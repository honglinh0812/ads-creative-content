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

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class ApifyService {

    @Value("${apify.api.key:}")
    private String apiKey;

    @Value("${apify.base.url:https://api.apify.com/v2}")
    private String baseUrl;

    @Value("${apify.tiktok.actor:doliz/tiktok-creative-center-scraper}")
    private String tiktokActorId;

    @Value("${apify.tiktok.cookie:}")
    private String tiktokCookie;

    @Value("${apify.poll.max.attempts:30}")
    private int pollMaxAttempts;

    @Value("${apify.poll.interval.ms:2000}")
    private long pollIntervalMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ApifyService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return "US";
        }
        return region.trim().toUpperCase();
    }

    private List<String> buildTikTokFallbackRegions(String region) {
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

    private String buildTikTokIframeUrl(String brandName) {
        String encoded = java.net.URLEncoder.encode(brandName != null ? brandName : "", java.nio.charset.StandardCharsets.UTF_8);
        return "https://ads.tiktok.com/business/creativecenter/inspiration/topads/pc/en?keyword=" + encoded;
    }

    private PlatformSearchResult detectDatasetIssue(JsonNode dataset, PlatformSearchResult.PlatformSearchResultBuilder builder) {
        if (dataset == null || !dataset.isArray() || dataset.size() == 0) {
            return null;
        }

        JsonNode first = dataset.get(0);
        if (first.has("code") && first.has("msg")) {
            String msg = first.get("msg").asText("Apify error");
            int code = first.get("code").asInt();
            log.warn("Apify dataset reported code {} with message: {}", code, msg);
            String lower = msg.toLowerCase();

            if (lower.contains("valid cookies")) {
                return builder
                    .success(false)
                    .mode(PlatformSearchMode.ERROR)
                    .errorCode(PlatformSearchErrorCode.CONFIG_MISSING)
                    .message("TikTok yêu cầu cookie hợp lệ để lấy dữ liệu (" + msg + ")")
                    .friendlySuggestion("Cập nhật cookie TikTok trong cấu hình Apify hoặc dùng iframe.")
                    .retryable(false)
                    .build();
            }

            return builder
                .success(false)
                .mode(PlatformSearchMode.ERROR)
                .errorCode(PlatformSearchErrorCode.PROVIDER_ERROR)
                .message("Apify trả về lỗi: " + msg)
                .friendlySuggestion("Thử lại sau hoặc kiểm tra actor TikTok Creative Center.")
                .retryable(code != 0)
                .build();
        }

        return null;
    }

    /**
     * Check if Apify is configured and available
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.isBlank();
    }

    /**
     * Search TikTok Creative Center for competitor ads and return unified result
     */
    public PlatformSearchResult searchTikTokAds(String brandName, String region, int limit) {
        String sanitizedBrand = brandName != null ? brandName.trim() : "";
        String normalizedRegion = normalizeRegion(region);

        PlatformSearchResult.PlatformSearchResultBuilder builder = PlatformSearchResult.builder()
            .platform(AdPlatform.TIKTOK)
            .brandName(sanitizedBrand)
            .region(normalizedRegion)
            .mode(PlatformSearchMode.IFRAME)
            .iframeUrl(buildTikTokIframeUrl(sanitizedBrand))
            .fallbackRegions(buildTikTokFallbackRegions(normalizedRegion));

        if (!isAvailable()) {
            log.warn("Apify not configured, cannot search TikTok ads");
            return builder
                .success(false)
                .errorCode(PlatformSearchErrorCode.CONFIG_MISSING)
                .message("TikTok Ads search chưa được cấu hình. Vui lòng thêm Apify API key/cookie.")
                .friendlySuggestion("Sử dụng iframe Creative Center hoặc cấu hình Apify để có dữ liệu chi tiết.")
                .build();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("actorId", tiktokActorId);
        metadata.put("region", normalizedRegion);
        metadata.put("cookieConfigured", tiktokCookie != null && !tiktokCookie.isBlank());

        if (tiktokCookie == null || tiktokCookie.isBlank()) {
            log.warn("TikTok cookie not configured. Apify actor may fail for protected regions.");
        }

        try {
            log.info("Starting Apify actor for TikTok search: {}", sanitizedBrand);

            String runId = startActorRun(sanitizedBrand, normalizedRegion, limit);
            metadata.put("runId", runId);

            if (runId == null) {
                log.error("Failed to start Apify actor run");
                builder.metadata(metadata);
                return builder
                    .success(false)
                    .mode(PlatformSearchMode.ERROR)
                    .errorCode(PlatformSearchErrorCode.PROVIDER_ERROR)
                    .message("Không thể khởi chạy Apify actor cho TikTok.")
                    .friendlySuggestion("Kiểm tra lại API key hoặc giới hạn run của Apify.")
                    .retryable(true)
                    .build();
            }

            JsonNode dataset = pollForResults(runId);
            if (dataset == null) {
                log.error("Failed to get results from Apify actor");
                builder.metadata(metadata);
                return builder
                    .success(false)
                    .mode(PlatformSearchMode.ERROR)
                    .errorCode(PlatformSearchErrorCode.TEMPORARY_ERROR)
                    .message("Không lấy được kết quả từ Apify. Vui lòng thử lại.")
                    .friendlySuggestion("Đợi vài phút và tìm lại hoặc mở iframe.")
                    .retryable(true)
                    .build();
            }

            metadata.put("datasetSize", dataset.size());
            builder.metadata(metadata);

            PlatformSearchResult datasetIssue = detectDatasetIssue(dataset, builder);
            if (datasetIssue != null) {
                return datasetIssue;
            }

            List<CompetitorAdDTO> ads = parseApifyResults(dataset, limit);
            if (!ads.isEmpty()) {
                return builder
                    .success(true)
                    .mode(PlatformSearchMode.DATA)
                    .errorCode(PlatformSearchErrorCode.NONE)
                    .ads(ads)
                    .totalResults(ads.size())
                    .message(String.format("Found %d TikTok ads for %s", ads.size(), sanitizedBrand))
                    .build();
            }

            return builder
                .success(false)
                .mode(PlatformSearchMode.EMPTY)
                .errorCode(PlatformSearchErrorCode.NO_RESULTS)
                .message(String.format("Không tìm thấy quảng cáo TikTok cho %s tại %s", sanitizedBrand, normalizedRegion))
                .friendlySuggestion("Thử từ khóa rộng hơn hoặc xem trực tiếp bằng iframe.")
                .build();

        } catch (Exception e) {
            log.error("Error calling Apify for TikTok ads: {}", e.getMessage(), e);
            return builder
                .success(false)
                .mode(PlatformSearchMode.ERROR)
                .errorCode(PlatformSearchErrorCode.PROVIDER_ERROR)
                .message("TikTok Ads search gặp lỗi: " + e.getMessage())
                .friendlySuggestion("Kiểm tra lại thông tin đăng nhập Apify hoặc dùng iframe.")
                .retryable(true)
                .build();
        }
    }

    /**
     * Start an Apify actor run
     *
     * @return Run ID if successful, null otherwise
     */
    private String startActorRun(String brandName, String region, int limit) {
        try {
            // Issue #3: Properly encode actor ID and use correct Apify API v2 format
            // Actor ID format: username/actor-name or username~actor-name
            // API endpoint: /v2/acts/{username~actor-name}/runs
            String encodedActorId = tiktokActorId.replace("/", "~");
            String url = baseUrl + "/acts/" + encodedActorId + "/runs";
            log.info("Apify API URL: {}", url);

            // Build input for TikTok Creative Center scraper
            Map<String, Object> input = new HashMap<>();
            input.put("keyword", brandName);
            input.put("region", region != null ? region.toUpperCase() : "US");
            input.put("maxResults", Math.min(limit, 100)); // Limit to 100 per API constraints
            input.put("adType", "all"); // Search all ad types

            if (tiktokCookie != null && !tiktokCookie.isBlank()) {
                input.put("cookie", tiktokCookie);
                input.put("cookies", tiktokCookie);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "Mozilla/5.0 (compatible; FBAdAutomation/1.0)");
            // Issue #3: Send API token in Authorization header (more secure)
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(input, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("data") && root.get("data").has("id")) {
                    String runId = root.get("data").get("id").asText();
                    log.info("Apify actor run started: {}", runId);
                    return runId;
                }
            }

            log.error("Apify returned status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error starting Apify actor: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Poll for actor run results with exponential backoff
     *
     * @param runId The actor run ID
     * @return JsonNode with results if successful, null otherwise
     */
    private JsonNode pollForResults(String runId) {
        try {
            String url = baseUrl + "/actor-runs/" + runId + "?token=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (compatible; FBAdAutomation/1.0)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            int attempts = 0;
            long currentInterval = pollIntervalMs;

            while (attempts < pollMaxAttempts) {
                ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    if (root.has("data") && root.get("data").has("status")) {
                        String status = root.get("data").get("status").asText();

                        log.debug("Apify run status: {} (attempt {}/{})", status, attempts + 1, pollMaxAttempts);

                        if ("SUCCEEDED".equals(status)) {
                            // Get dataset ID
                            if (root.get("data").has("defaultDatasetId")) {
                                String datasetId = root.get("data").get("defaultDatasetId").asText();
                                return fetchDatasetItems(datasetId);
                            } else {
                                log.error("No dataset ID in successful run");
                                return null;
                            }
                        } else if ("FAILED".equals(status) || "ABORTED".equals(status) || "TIMED-OUT".equals(status)) {
                            log.error("Apify run failed with status: {}", status);
                            return null;
                        }
                        // Status is RUNNING or READY - continue polling
                    }
                }

                // Wait before next poll (exponential backoff, max 60 seconds)
                Thread.sleep(currentInterval);
                currentInterval = Math.min(currentInterval * 2, 60000);
                attempts++;
            }

            log.error("Apify polling timed out after {} attempts", pollMaxAttempts);
            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Apify polling interrupted: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error polling Apify results: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetch dataset items from Apify
     *
     * @param datasetId The dataset ID
     * @return JsonNode with items
     */
    private JsonNode fetchDatasetItems(String datasetId) {
        try {
            String url = baseUrl + "/datasets/" + datasetId + "/items?token=" + apiKey;

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
                return objectMapper.readTree(response.getBody());
            }

            log.error("Failed to fetch dataset items, status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error fetching dataset items: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse Apify results to CompetitorAdDTO list
     *
     * TikTok Creative Center data structure (from doliz/tiktok-creative-center-scraper):
     * - adId: Unique ad identifier
     * - title: Ad title/headline
     * - caption: Ad description text
     * - imageUrl: Thumbnail image
     * - videoUrl: Video URL if available
     * - likes, shares, comments: Engagement metrics
     * - advertiserName: Brand/advertiser name
     * - ctaText: Call-to-action text
     * - landingPage: Destination URL
     * - publishTime: When ad was published
     */
    private List<CompetitorAdDTO> parseApifyResults(JsonNode root, int limit) {
        List<CompetitorAdDTO> ads = new ArrayList<>();

        try {
            if (!root.isArray()) {
                log.warn("Apify results is not an array");
                return ads;
            }

            // Phase 1.2: Log first item structure for debugging
            if (root.size() > 0) {
                JsonNode firstItem = root.get(0);

                // Log available fields
                List<String> fieldNames = new ArrayList<>();
                firstItem.fieldNames().forEachRemaining(fieldNames::add);
                log.info("📋 TikTok ad available fields: {}", String.join(", ", fieldNames));

                // Log sample data (debug level to avoid spam)
                log.debug("First TikTok ad raw data: {}", firstItem.toPrettyString());
            }

            for (JsonNode item : root) {
                try {
                    CompetitorAdDTO ad = parseTikTokAd(item);
                    if (ad != null) {
                        ads.add(ad);
                        if (ads.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error parsing individual TikTok ad: {}", e.getMessage());
                }
            }

            log.info("Parsed {} TikTok ads from Apify", ads.size());
            return ads;

        } catch (Exception e) {
            log.error("Error parsing Apify results: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse individual TikTok ad from Apify result
     */
    private CompetitorAdDTO parseTikTokAd(JsonNode adNode) {
        CompetitorAdDTO.CompetitorAdDTOBuilder builder = CompetitorAdDTO.builder();

        // Ad ID
        if (adNode.has("adId")) {
            builder.adId("tiktok_" + adNode.get("adId").asText());
        } else if (adNode.has("id")) {
            builder.adId("tiktok_" + adNode.get("id").asText());
        } else {
            builder.adId("tiktok_" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Advertiser name
        if (adNode.has("advertiserName")) {
            builder.advertiserName(adNode.get("advertiserName").asText());
        } else if (adNode.has("brandName")) {
            builder.advertiserName(adNode.get("brandName").asText());
        }

        // Headline - try multiple field names
        if (adNode.has("title")) {
            builder.headline(adNode.get("title").asText());
        } else if (adNode.has("adTitle")) {
            builder.headline(adNode.get("adTitle").asText());
        }

        // Primary text - caption/description
        if (adNode.has("caption")) {
            builder.primaryText(adNode.get("caption").asText());
        } else if (adNode.has("description")) {
            builder.primaryText(adNode.get("description").asText());
        } else if (adNode.has("adText")) {
            builder.primaryText(adNode.get("adText").asText());
        }

        // Call-to-action
        if (adNode.has("ctaText")) {
            builder.description("CTA: " + adNode.get("ctaText").asText());
        } else if (adNode.has("callToAction")) {
            builder.description("CTA: " + adNode.get("callToAction").asText());
        }

        // Image URL
        List<String> imageUrls = new ArrayList<>();
        if (adNode.has("imageUrl")) {
            imageUrls.add(adNode.get("imageUrl").asText());
        } else if (adNode.has("thumbnail")) {
            imageUrls.add(adNode.get("thumbnail").asText());
        } else if (adNode.has("coverImage")) {
            imageUrls.add(adNode.get("coverImage").asText());
        }
        if (!imageUrls.isEmpty()) {
            builder.imageUrls(imageUrls);
        }

        // Video URL
        if (adNode.has("videoUrl")) {
            builder.videoUrl(adNode.get("videoUrl").asText());
        } else if (adNode.has("video")) {
            builder.videoUrl(adNode.get("video").asText());
        }

        // Landing page
        if (adNode.has("landingPage")) {
            builder.landingPageUrl(adNode.get("landingPage").asText());
        } else if (adNode.has("clickUrl")) {
            builder.landingPageUrl(adNode.get("clickUrl").asText());
        } else if (adNode.has("url")) {
            builder.landingPageUrl(adNode.get("url").asText());
        }

        // Publish date
        if (adNode.has("publishTime")) {
            try {
                String dateStr = adNode.get("publishTime").asText();
                // Handle different date formats
                if (dateStr.contains("T")) {
                    dateStr = dateStr.split("T")[0]; // Extract date part from ISO format
                }
                builder.startDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                log.debug("Could not parse publishTime");
            }
        } else if (adNode.has("createdAt")) {
            try {
                String dateStr = adNode.get("createdAt").asText();
                if (dateStr.contains("T")) {
                    dateStr = dateStr.split("T")[0];
                }
                builder.startDate(LocalDate.parse(dateStr));
            } catch (Exception e) {
                log.debug("Could not parse createdAt");
            }
        }

        // Engagement metrics as metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "apify");
        metadata.put("scraper", "tiktok-creative-center");

        if (adNode.has("likes")) {
            metadata.put("likes", adNode.get("likes").asInt());
        }
        if (adNode.has("shares")) {
            metadata.put("shares", adNode.get("shares").asInt());
        }
        if (adNode.has("comments")) {
            metadata.put("comments", adNode.get("comments").asInt());
        }

        metadata.put("raw", adNode.toString());
        builder.metadata(metadata);

        // Platform
        builder.platforms(List.of("TikTok"));
        builder.dataSource("TIKTOK_CREATIVE_CENTER");
        builder.isActive(true);

        CompetitorAdDTO ad = builder.build();

        // Phase 3: Validate before returning
        if (!isAdUsable(ad)) {
            log.warn("❌ Skipping TikTok ad with insufficient data: {}", ad.getAdId());
            return null;
        }

        log.debug("✅ Parsed usable TikTok ad: headline={}, image={}, video={}",
            ad.getHeadline() != null,
            ad.getImageUrls() != null && !ad.getImageUrls().isEmpty(),
            ad.getVideoUrl() != null);

        return ad;
    }

    /**
     * Phase 3: Validate if a TikTok ad has sufficient data to be useful
     */
    private boolean isAdUsable(CompetitorAdDTO ad) {
        if (ad == null) {
            return false;
        }

        // Require at least headline OR primary text
        boolean hasText = (ad.getHeadline() != null && !ad.getHeadline().trim().isEmpty())
                       || (ad.getPrimaryText() != null && !ad.getPrimaryText().trim().isEmpty());

        // Require at least image OR video
        boolean hasMedia = (ad.getImageUrls() != null && !ad.getImageUrls().isEmpty())
                        || (ad.getVideoUrl() != null && !ad.getVideoUrl().trim().isEmpty());

        // Ad is usable if it has text OR media (at least one)
        boolean usable = hasText || hasMedia;

        if (!usable) {
            log.warn("⚠️ Ad {} has insufficient data - headline: {}, primaryText: {}, images: {}, video: {}",
                ad.getAdId(),
                ad.getHeadline() != null,
                ad.getPrimaryText() != null,
                ad.getImageUrls() != null && !ad.getImageUrls().isEmpty(),
                ad.getVideoUrl() != null);
        }

        return usable;
    }
}
