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
                .queryParam("engine", "google_ads_transparency")
                .queryParam("q", brandName)
                .queryParam("region", region.toUpperCase())
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

            // Check for ads in response
            if (!root.has("ads")) {
                log.warn("No 'ads' field in SerpAPI response");
                return ads;
            }

            JsonNode adsNode = root.get("ads");
            if (!adsNode.isArray()) {
                log.warn("'ads' field is not an array");
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

        } catch (Exception e) {
            log.error("Error parsing SerpAPI response: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse individual Google ad from JSON node
     */
    private CompetitorAdDTO parseGoogleAd(JsonNode adNode) {
        CompetitorAdDTO.CompetitorAdDTOBuilder builder = CompetitorAdDTO.builder();

        // Ad ID
        if (adNode.has("ad_id")) {
            builder.adId(adNode.get("ad_id").asText());
        } else {
            builder.adId("google_" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Headline/Title
        if (adNode.has("title")) {
            builder.headline(adNode.get("title").asText());
        }

        // Description
        if (adNode.has("description")) {
            builder.primaryText(adNode.get("description").asText());
        }

        // Advertiser
        if (adNode.has("advertiser")) {
            builder.advertiserName(adNode.get("advertiser").asText());
        }

        // Images
        List<String> imageUrls = new ArrayList<>();
        if (adNode.has("thumbnail")) {
            imageUrls.add(adNode.get("thumbnail").asText());
        }
        if (adNode.has("image_url")) {
            imageUrls.add(adNode.get("image_url").asText());
        }
        if (!imageUrls.isEmpty()) {
            builder.imageUrls(imageUrls);
        }

        // Landing page URL
        if (adNode.has("link")) {
            builder.landingPageUrl(adNode.get("link").asText());
        }

        // Dates
        if (adNode.has("first_shown")) {
            try {
                builder.startDate(LocalDate.parse(adNode.get("first_shown").asText()));
            } catch (Exception e) {
                log.debug("Could not parse first_shown date");
            }
        }

        if (adNode.has("last_shown")) {
            try {
                builder.endDate(LocalDate.parse(adNode.get("last_shown").asText()));
            } catch (Exception e) {
                log.debug("Could not parse last_shown date");
            }
        }

        // Platforms
        if (adNode.has("platforms")) {
            JsonNode platformsNode = adNode.get("platforms");
            if (platformsNode.isArray()) {
                List<String> platforms = new ArrayList<>();
                platformsNode.forEach(p -> platforms.add(p.asText()));
                builder.platforms(platforms);
            }
        } else {
            builder.platforms(List.of("Google"));
        }

        // Data source
        builder.dataSource("GOOGLE_ADS_TRANSPARENCY");
        builder.isActive(true);

        // Ad Library URL (construct from advertiser)
        if (adNode.has("advertiser_url")) {
            builder.adLibraryUrl(adNode.get("advertiser_url").asText());
        }

        // Metadata - store full raw response
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "serpapi");
        metadata.put("engine", "google_ads_transparency");
        metadata.put("raw", adNode.toString());
        builder.metadata(metadata);

        return builder.build();
    }
}
