package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.dto.AdPlatform;
import com.fbadsautomation.dto.CompetitorAdDTO;
import com.fbadsautomation.dto.PlatformSearchErrorCode;
import com.fbadsautomation.dto.PlatformSearchMode;
import com.fbadsautomation.dto.PlatformSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * Search Google Ads Transparency Center using SerpAPI and return unified result.
     */
    public PlatformSearchResult searchGoogleAds(String brandName, String region, int limit) {
        String sanitizedBrand = brandName != null ? brandName.trim() : "";
        String normalizedRegion = normalizeRegion(region);
        String location = mapRegionToLocation(normalizedRegion);

        PlatformSearchResult.PlatformSearchResultBuilder builder = PlatformSearchResult.builder()
            .platform(AdPlatform.GOOGLE)
            .brandName(sanitizedBrand)
            .region(normalizedRegion)
            .mode(PlatformSearchMode.IFRAME)
            .iframeUrl(buildTransparencyCenterUrl(normalizedRegion, sanitizedBrand))
            .fallbackRegions(buildFallbackRegions(normalizedRegion));

        if (!isAvailable()) {
            log.warn("SerpAPI not configured, cannot search Google Ads");
            return builder
                .success(false)
                .errorCode(PlatformSearchErrorCode.CONFIG_MISSING)
                .message("Google Ads search is not configured. Please add a SerpAPI API key in settings.")
                .friendlySuggestion("Mở Google Ads Transparency Center bằng iframe hoặc thử lại sau khi cấu hình SerpAPI.")
                .build();
        }

        try {
            String url = buildGoogleAdsUrl(sanitizedBrand, location);
            log.info("Calling SerpAPI for Google Ads: {} (region={}, location={})", sanitizedBrand, normalizedRegion, location);

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
                return parseGoogleAdsResponse(response.getBody(), builder, sanitizedBrand, normalizedRegion, location, limit);
            }

            log.error("SerpAPI returned status: {}", response.getStatusCode());
            return builder
                .success(false)
                .mode(PlatformSearchMode.ERROR)
                .errorCode(PlatformSearchErrorCode.PROVIDER_ERROR)
                .message("SerpAPI trả về trạng thái " + response.getStatusCode() + ".")
                .friendlySuggestion("Thử lại sau ít phút hoặc mở Transparency Center trực tiếp.")
                .retryable(true)
                .build();

        } catch (Exception e) {
            log.error("Error calling SerpAPI for Google Ads: {}", e.getMessage(), e);
            return builder
                .success(false)
                .mode(PlatformSearchMode.ERROR)
                .errorCode(PlatformSearchErrorCode.TEMPORARY_ERROR)
                .message("Google Ads search gặp sự cố: " + e.getMessage())
                .friendlySuggestion("Vui lòng thử lại sau hoặc dùng chế độ iframe.")
                .retryable(true)
                .build();
        }
    }

    /**
     * Build Google Ads Transparency search URL
     */
    private String buildGoogleAdsUrl(String brandName, String location) {
        try {
            return UriComponentsBuilder.fromHttpUrl(baseUrl + "/search.json")
                .queryParam("engine", "google_ads_transparency_center")
                .queryParam("text", brandName)
                .queryParam("location", location)
                .queryParam("num", "40")
                .queryParam("api_key", apiKey)
                .build()
                .toUriString();
        } catch (Exception e) {
            log.error("Error building URL: {}", e.getMessage());
            throw new RuntimeException("Failed to build SerpAPI URL", e);
        }
    }

    private String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return "US";
        }
        return region.trim().toUpperCase();
    }

    /**
     * Map region codes to location names for SerpAPI.
     */
    private String mapRegionToLocation(String region) {
        return switch (region) {
            case "US" -> "United States";
            case "CA" -> "Canada";
            case "GB", "UK" -> "United Kingdom";
            case "AU" -> "Australia";
            case "DE" -> "Germany";
            case "FR" -> "France";
            case "IT" -> "Italy";
            case "ES" -> "Spain";
            case "JP" -> "Japan";
            case "KR" -> "South Korea";
            case "SG" -> "Singapore";
            case "VN" -> {
                log.warn("Vietnam (VN) not supported. Using Singapore as regional fallback.");
                yield "Singapore";
            }
            case "NZ" -> "New Zealand";
            case "IN" -> "India";
            default -> {
                log.warn("Unknown region '{}', defaulting to United States", region);
                yield "United States";
            }
        };
    }

    private List<String> buildFallbackRegions(String region) {
        List<String> fallbacks = new ArrayList<>();
        if (!"US".equals(region)) {
            fallbacks.add("US");
        }
        if ("VN".equals(region)) {
            fallbacks.add("SG");
        }
        if (fallbacks.isEmpty()) {
            fallbacks.add("GLOBAL");
        }
        return fallbacks;
    }

    private String buildTransparencyCenterUrl(String region, String brandName) {
        String encodedBrand = URLEncoder.encode(brandName != null ? brandName : "", StandardCharsets.UTF_8);
        String regionParam = region != null ? region : "anywhere";
        return String.format("https://adstransparency.google.com/?region=%s&q=%s", regionParam, encodedBrand);
    }

    /**
     * Parse Google Ads API response to PlatformSearchResult
     */
    private PlatformSearchResult parseGoogleAdsResponse(
            String responseBody,
            PlatformSearchResult.PlatformSearchResultBuilder builder,
            String brandName,
            String region,
            String location,
            int limit) {

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("location", location);

            if (root.has("search_metadata")) {
                JsonNode meta = root.get("search_metadata");
                meta.fieldNames().forEachRemaining(key -> metadata.put("search_metadata." + key, meta.get(key).asText("")));
            }
            if (root.has("search_information")) {
                JsonNode info = root.get("search_information");
                info.fieldNames().forEachRemaining(key -> metadata.put("search_information." + key, info.get(key).asText("")));
            }

            builder.metadata(metadata);

            if (root.has("error")) {
                String errorMsg = root.get("error").asText();
                String errorDetails = root.has("error_message") ? root.get("error_message").asText() : errorMsg;

                log.error("❌ SerpAPI returned error: {}", errorMsg);
                log.error("Error details: {}", errorDetails);
                log.debug("SerpAPI error response: {}", responseBody);

                PlatformSearchErrorCode errorCode = mapSerpApiErrorCode(errorMsg, errorDetails);
                String userMessage = mapSerpApiError(errorMsg, errorDetails);
                boolean retryable = isRetryableError(errorMsg, errorDetails);

                return builder
                    .success(false)
                    .mode(errorCode == PlatformSearchErrorCode.NO_RESULTS ? PlatformSearchMode.EMPTY : PlatformSearchMode.ERROR)
                    .errorCode(errorCode)
                    .message(userMessage)
                    .friendlySuggestion("Thử lại sau hoặc sử dụng khu vực khác như " + String.join("/", buildFallbackRegions(region)))
                    .retryable(retryable)
                    .build();
            }

            JsonNode adsNode = null;
            if (root.has("ads_results")) {
                adsNode = root.get("ads_results");
            } else if (root.has("organic_results")) {
                adsNode = root.get("organic_results");
            } else if (root.has("ads")) {
                adsNode = root.get("ads");
            }

            List<CompetitorAdDTO> ads = adsNode != null
                ? parseAdsFromNode(adsNode, limit)
                : Collections.emptyList();

            if (!ads.isEmpty()) {
                return builder
                    .success(true)
                    .mode(PlatformSearchMode.DATA)
                    .errorCode(PlatformSearchErrorCode.NONE)
                    .totalResults(ads.size())
                    .ads(ads)
                    .message(String.format("Found %d Google ads for %s", ads.size(), brandName))
                    .build();
            }

            String resultsState = metadata.getOrDefault("search_information.results_state", "").toString();
            String suggestion = "Không tìm thấy quảng cáo cho " + brandName + " tại " + region + ".";

            return builder
                .success(false)
                .mode(PlatformSearchMode.EMPTY)
                .errorCode(PlatformSearchErrorCode.NO_RESULTS)
                .message(resultsState.equalsIgnoreCase("fully empty")
                    ? suggestion + " Google Ads Transparency Center không trả về kết quả."
                    : suggestion)
                .friendlySuggestion("Thử tìm với khu vực khác: " + String.join("/", buildFallbackRegions(region)))
                .build();

        } catch (Exception e) {
            log.error("Error parsing SerpAPI response: {}", e.getMessage(), e);
            return builder
                .success(false)
                .mode(PlatformSearchMode.ERROR)
                .errorCode(PlatformSearchErrorCode.PROVIDER_ERROR)
                .message("Không thể phân tích kết quả Google Ads: " + e.getMessage())
                .friendlySuggestion("Thử mở Google Ads Transparency Center trực tiếp.")
                .retryable(true)
                .build();
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

    /**
     * Phase 4: Determine if a SerpAPI error is retryable
     */
    private boolean isRetryableError(String error, String details) {
        if (error == null) return false;

        String errorLower = error.toLowerCase();
        String detailsLower = details != null ? details.toLowerCase() : "";

        // Non-retryable: API key errors, quota exceeded, invalid parameters, engine errors
        if (errorLower.contains("api key") || errorLower.contains("invalid key") ||
            errorLower.contains("quota") || errorLower.contains("parameter") ||
            errorLower.contains("engine") || errorLower.contains("location")) {
            return false;
        }

        // Retryable: Rate limiting, temporary errors, search errors
        if (errorLower.contains("rate") || errorLower.contains("too many") ||
            errorLower.contains("timeout") || errorLower.contains("unavailable")) {
            return true;
        }

        // Default: assume retryable for unknown errors
        return true;
    }

    /**
     * Map SerpAPI error messages to canonical error code
     */
    private PlatformSearchErrorCode mapSerpApiErrorCode(String error, String details) {
        if (error == null) {
            return PlatformSearchErrorCode.UNKNOWN;
        }

        String errorLower = error.toLowerCase();
        String detailsLower = details != null ? details.toLowerCase() : "";

        if (errorLower.contains("api key")) {
            return PlatformSearchErrorCode.CONFIG_MISSING;
        }
        if (errorLower.contains("quota") || detailsLower.contains("quota")) {
            return PlatformSearchErrorCode.QUOTA_EXCEEDED;
        }
        if (errorLower.contains("parameter") || errorLower.contains("invalid") && detailsLower.contains("parameter")) {
            return PlatformSearchErrorCode.VALIDATION_ERROR;
        }
        if (errorLower.contains("engine")) {
            return PlatformSearchErrorCode.PROVIDER_ERROR;
        }
        if (errorLower.contains("location")) {
            return PlatformSearchErrorCode.REGION_UNSUPPORTED;
        }
        if (errorLower.contains("rate") || errorLower.contains("too many")) {
            return PlatformSearchErrorCode.RATE_LIMITED;
        }
        if (errorLower.contains("search error") || errorLower.contains("failed")) {
            return PlatformSearchErrorCode.PROVIDER_ERROR;
        }

        return PlatformSearchErrorCode.UNKNOWN;
    }

    /**
     * Phase 4: Map SerpAPI error messages to user-friendly messages
     */
    private String mapSerpApiError(String error, String details) {
        if (error == null) {
            return "Google Ads search failed due to an unknown error. Please try again later.";
        }

        String errorLower = error.toLowerCase();
        String detailsLower = details != null ? details.toLowerCase() : "";

        // API Key errors
        if (errorLower.contains("api key") || errorLower.contains("invalid key") || errorLower.contains("unauthorized")) {
            return "Google Ads search failed: Invalid or missing SerpAPI API key. Please check your API configuration.";
        }

        // Quota errors
        if (errorLower.contains("quota") || errorLower.contains("limit") || detailsLower.contains("searches remaining")) {
            return "Google Ads search failed: SerpAPI monthly quota exceeded. Please upgrade your plan or wait until next month.";
        }

        // Parameter errors
        if (errorLower.contains("parameter") || errorLower.contains("invalid") && detailsLower.contains("parameter")) {
            return "Google Ads search failed: Invalid search parameters. Please check your region and brand name.";
        }

        // Engine errors
        if (errorLower.contains("engine") || errorLower.contains("not found")) {
            return "Google Ads search failed: SerpAPI engine unavailable. The google_ads_transparency_center engine may not be supported in your plan.";
        }

        // Location/Region errors
        if (errorLower.contains("location") || detailsLower.contains("location")) {
            return "Google Ads search failed: Unsupported region. Please try a different region (US, UK, CA, AU, etc.).";
        }

        // Rate limiting
        if (errorLower.contains("rate") || errorLower.contains("too many")) {
            return "Google Ads search failed: Too many requests. Please wait a few minutes and try again.";
        }

        // Generic search error
        if (errorLower.contains("search error") || errorLower.contains("failed")) {
            return String.format("Google Ads search failed: %s. Please try again or contact support.", details);
        }

        // Default fallback
        return String.format("Google Ads search encountered an error: %s. Please try again or contact support.",
            details != null && !details.isEmpty() ? details : error);
    }
}
