package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.dto.CompetitorAdDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class SerpApiService {

    @Value("${serpapi.api.key:}")
    private String apiKey;

    @Value("${serpapi.base.url:https://serpapi.com}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SerpApiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if SerpAPI is configured and available
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.isBlank();
    }

    /**
     * Get remaining quota (estimated based on response)
     */
    public boolean hasQuota() {
        // Basic check - can be enhanced with actual quota API
        return isAvailable();
    }

    /**
     * Search Google Ads Transparency Center
     */
    public List<CompetitorAdDTO> searchGoogleAds(String brandName, String region, int limit) {
        if (!isAvailable()) {
            log.warn("SerpAPI not configured, cannot search Google Ads");
            return null;
        }

        try {
            String url = buildGoogleAdsUrl(brandName, region);
            log.info("Calling SerpAPI for Google Ads: {}", brandName);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (compatible; FBAdAutomation/1.0)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGoogleAdsResponse(response.getBody(), limit);
            } else {
                log.error("SerpAPI returned status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Error calling SerpAPI for Google Ads: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build Google Ads Transparency search URL
     */
    private String buildGoogleAdsUrl(String brandName, String region) {
        try {
            return UriComponentsBuilder.fromHttpUrl(baseUrl + "/search.json")
                .queryParam("engine", "google_ads_transparency_center")  // FIXED: Correct engine name
                .queryParam("text", brandName)  // FIXED: Use 'text' instead of 'q' for brand search
                .queryParam("region", region != null ? region.toUpperCase() : "US")
                .queryParam("num", "40")  // Results per page (max 100)
                .queryParam("api_key", apiKey)
                .build()
                .toUriString();
        } catch (Exception e) {
            log.error("Error building URL: {}", e.getMessage());
            throw new RuntimeException("Failed to build SerpAPI URL", e);
        }
    }

    /**
     * Parse Google Ads API response to CompetitorAdDTO
     */
    private List<CompetitorAdDTO> parseGoogleAdsResponse(String responseBody, int limit) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<CompetitorAdDTO> ads = new ArrayList<>();

            // SerpAPI returns ads in 'ads_results' field for google_ads_transparency_center engine
            if (!root.has("ads_results")) {
                log.warn("No 'ads_results' field in SerpAPI response, checking alternatives...");

                // Fallback: check 'organic_results' or 'ads'
                if (root.has("organic_results")) {
                    return parseAdsFromNode(root.get("organic_results"), limit);
                } else if (root.has("ads")) {
                    return parseAdsFromNode(root.get("ads"), limit);
                }

                // Log available fields for debugging
                StringBuilder keys = new StringBuilder();
                root.fieldNames().forEachRemaining(key -> {
                    if (keys.length() > 0) keys.append(", ");
                    keys.append(key);
                });
                log.warn("No ads data found in response. Response keys: {}",
                    keys.length() > 0 ? keys.toString() : "empty");
                return ads;
            }

            return parseAdsFromNode(root.get("ads_results"), limit);

        } catch (Exception e) {
            log.error("Error parsing SerpAPI response: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Helper method to parse ads from a JSON node
     */
    private List<CompetitorAdDTO> parseAdsFromNode(JsonNode adsNode, int limit) {
        List<CompetitorAdDTO> ads = new ArrayList<>();

        if (!adsNode.isArray()) {
            log.warn("Ads node is not an array");
            return ads;
        }

        // Parse each ad
        for (JsonNode adNode : adsNode) {
            try {
                CompetitorAdDTO ad = parseGoogleAd(adNode);
                if (ad != null) {
                    ads.add(ad);
                    if (ads.size() >= limit) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing individual ad: {}", e.getMessage());
            }
        }

        log.info("Parsed {} Google ads from SerpAPI", ads.size());
        return ads;
    }

    /**
     * Parse individual Google ad from JSON node
     *
     * SerpAPI Google Ads Transparency Center returns fields like:
     * - advertiser_name, creative_id, creative_format
     * - first_shown_date, last_shown_date
     * - images, videos, ad_text
     * - regions, platforms
     */
    private CompetitorAdDTO parseGoogleAd(JsonNode adNode) {
        CompetitorAdDTO.CompetitorAdDTOBuilder builder = CompetitorAdDTO.builder();

        // Ad ID - try multiple field names
        String adId = null;
        if (adNode.has("creative_id")) {
            adId = adNode.get("creative_id").asText();
        } else if (adNode.has("ad_id")) {
            adId = adNode.get("ad_id").asText();
        } else {
            adId = "google_" + UUID.randomUUID().toString().substring(0, 8);
        }
        builder.adId(adId);

        // Advertiser name
        if (adNode.has("advertiser_name")) {
            builder.advertiserName(adNode.get("advertiser_name").asText());
        } else if (adNode.has("advertiser")) {
            builder.advertiserName(adNode.get("advertiser").asText());
        }

        // Ad text/content
        String adText = null;
        if (adNode.has("ad_text")) {
            adText = adNode.get("ad_text").asText();
        } else if (adNode.has("text")) {
            adText = adNode.get("text").asText();
        } else if (adNode.has("description")) {
            adText = adNode.get("description").asText();
        }

        if (adText != null && !adText.isEmpty()) {
            // Use first line as headline, rest as primary text
            String[] lines = adText.split("\\n", 2);
            builder.headline(lines[0]);
            if (lines.length > 1) {
                builder.primaryText(lines[1]);
            } else {
                builder.primaryText(adText);
            }
        }

        // Creative format as description
        if (adNode.has("creative_format")) {
            String format = adNode.get("creative_format").asText();
            builder.description("Format: " + format);
        }

        // Images - multiple possible field names
        List<String> imageUrls = new ArrayList<>();
        if (adNode.has("image")) {
            imageUrls.add(adNode.get("image").asText());
        }
        if (adNode.has("images") && adNode.get("images").isArray()) {
            adNode.get("images").forEach(img -> imageUrls.add(img.asText()));
        }
        if (adNode.has("thumbnail")) {
            imageUrls.add(adNode.get("thumbnail").asText());
        }
        if (!imageUrls.isEmpty()) {
            builder.imageUrls(imageUrls);
        }

        // Video URL
        if (adNode.has("video")) {
            builder.videoUrl(adNode.get("video").asText());
        } else if (adNode.has("videos") && adNode.get("videos").isArray() && adNode.get("videos").size() > 0) {
            builder.videoUrl(adNode.get("videos").get(0).asText());
        }

        // Landing page URL
        if (adNode.has("advertiser_url")) {
            builder.landingPageUrl(adNode.get("advertiser_url").asText());
        } else if (adNode.has("link")) {
            builder.landingPageUrl(adNode.get("link").asText());
        }

        // Dates
        if (adNode.has("first_shown_date")) {
            try {
                String dateStr = adNode.get("first_shown_date").asText();
                builder.startDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                log.debug("Could not parse first_shown_date");
            }
        } else if (adNode.has("first_shown")) {
            try {
                builder.startDate(LocalDate.parse(adNode.get("first_shown").asText()));
            } catch (Exception e) {
                log.debug("Could not parse first_shown date");
            }
        }

        if (adNode.has("last_shown_date")) {
            try {
                String dateStr = adNode.get("last_shown_date").asText();
                builder.endDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                log.debug("Could not parse last_shown_date");
            }
        } else if (adNode.has("last_shown")) {
            try {
                builder.endDate(LocalDate.parse(adNode.get("last_shown").asText()));
            } catch (Exception e) {
                log.debug("Could not parse last_shown date");
            }
        }

        // Platforms/Regions
        if (adNode.has("platforms") && adNode.get("platforms").isArray()) {
            List<String> platforms = new ArrayList<>();
            adNode.get("platforms").forEach(p -> platforms.add(p.asText()));
            builder.platforms(platforms);
        } else if (adNode.has("regions") && adNode.get("regions").isArray()) {
            List<String> platforms = new ArrayList<>();
            adNode.get("regions").forEach(p -> platforms.add(p.asText()));
            builder.platforms(platforms);
        } else {
            builder.platforms(List.of("Google"));
        }

        // Data source
        builder.dataSource("GOOGLE_ADS_TRANSPARENCY");
        builder.isActive(true);

        // Ad Library URL
        if (adNode.has("advertiser_url")) {
            builder.adLibraryUrl(adNode.get("advertiser_url").asText());
        }

        // Metadata - store full raw response
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "serpapi");
        metadata.put("engine", "google_ads_transparency_center");
        metadata.put("raw", adNode.toString());
        builder.metadata(metadata);

        return builder.build();
    }
}
