package com.fbadsautomation.service;

import com.fbadsautomation.dto.CompetitorAdDTO;
import com.fbadsautomation.dto.PlatformSearchErrorCode;
import com.fbadsautomation.dto.PlatformSearchMode;
import com.fbadsautomation.dto.PlatformSearchResult;
import com.fbadsautomation.exception.ResourceException;
import com.fbadsautomation.model.CompetitorSearch;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.CompetitorSearchRepository;
import com.fbadsautomation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing competitor ad searches and analysis
 *
 * Provides functionality to:
 * - Search for competitor ads via Facebook Ad Library
 * - Store and retrieve search history
 * - Parse and structure competitor ad data
 * - Cache results for performance
 *
 * @author AI Panel - Senior Engineers
 * @version 1.0
 * @security All operations validate user ownership and input data
 * @performance Uses Redis cache with 24-hour TTL, indexed queries
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitorService {

    private final MetaAdLibraryService metaAdLibraryService;
    private final ScrapeCreatorsService scrapeCreatorsService;
    private final CompetitorSearchRepository competitorSearchRepository;
    private final UserRepository userRepository;
    private final SerpApiService serpApiService;

    @Autowired(required = false)
    private ApifyService apifyService;

    /**
     * Search for competitor ads by brand name
     *
     * Security: Validates user exists and sanitizes brand name
     * Performance: Results cached for 24 hours with key: "competitor:search:{brandName}:{region}"
     *
     * @param brandName Brand name to search (sanitized)
     * @param region Target region code (US, UK, VN, etc.)
     * @param userId User performing the search
     * @param limit Maximum number of ads to return (default 5, max 50)
     * @return List of competitor ads
     * @throws ResourceException if user not found or search fails
     */
    @Cacheable(value = "competitorSearches", key = "#brandName + ':' + #region", unless = "#result == null || #result.isEmpty()")
    public List<CompetitorAdDTO> searchCompetitorAds(
            @NotBlank String brandName,
            String region,
            @NotNull Long userId,
            int limit) {

        log.info("Searching competitor ads for brand: {}, region: {}, limit: {}", brandName, region, limit);

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.fbadsautomation.exception.ResourceException.notFound("User", String.valueOf(userId)));

        // Sanitize and validate inputs
        String sanitizedBrandName = sanitizeBrandName(brandName);
        int safeLimit = Math.min(Math.max(limit, 1), 50); // Between 1 and 50

        // Save search history
        saveSearchHistory(sanitizedBrandName, region, user);

        try {
            // Use ScrapeCreators API to search for competitor ads
            log.info("Calling ScrapeCreators API to search for brand: {}", sanitizedBrandName);

            // Try direct company name search first
            Map<String, Object> apiResponse = scrapeCreatorsService.searchAdsByCompanyName(
                    sanitizedBrandName,
                    region != null ? region : "ALL",
                    safeLimit
            );

            // Check for errors - if direct search fails, try two-step approach
            if (apiResponse.containsKey("error")) {
                log.warn("Direct search failed, trying two-step approach for brand: {}", sanitizedBrandName);
                apiResponse = scrapeCreatorsService.searchAdsByBrandTwoStep(
                        sanitizedBrandName,
                        region != null ? region : "ALL",
                        safeLimit
                );
            }

            // Check again for errors
            if (apiResponse.containsKey("error")) {
                String errorType = String.valueOf(apiResponse.get("error"));
                String errorMessage = String.valueOf(apiResponse.get("message"));
                log.error("ScrapeCreators API error for brand {}: {} - {}",
                         sanitizedBrandName, errorType, errorMessage);
                return Collections.emptyList();
            }

            // Extract ads array from response
            Object adsObj = apiResponse.get("ads");
            if (!(adsObj instanceof List)) {
                log.warn("No ads array in response for brand: {}", sanitizedBrandName);
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> adsData = (List<Map<String, Object>>) adsObj;

            // Convert to DTOs
            List<CompetitorAdDTO> results = adsData.stream()
                    .map(this::convertScrapeCreatorsAdToDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} competitor ads for brand: {}", results.size(), sanitizedBrandName);
            return results;

        } catch (Exception e) {
            log.error("Error searching competitor ads for brand {}: {}", sanitizedBrandName, e.getMessage(), e);
            throw com.fbadsautomation.exception.ResourceException.operationFailed(
                "CompetitorAds",
                sanitizedBrandName,
                "search",
                e
            );
        }
    }

    /**
     * Fetch competitor ads from specific ad URLs
     *
     * This method extracts ad IDs from Facebook Ad Library URLs and retrieves ad content
     *
     * Security: URLs validated before processing, results sanitized
     * Performance: Each ad fetch may take 500ms-1s due to rate limiting
     *
     * @param adUrls List of Facebook Ad Library URLs
     * @param userId User performing the fetch
     * @return List of structured competitor ad DTOs
     * @throws ResourceException if fetching fails
     */
    @Transactional(readOnly = true)
    public List<CompetitorAdDTO> fetchCompetitorAdsByUrls(
            @NotNull List<String> adUrls,
            @NotNull Long userId) {

        log.info("Fetching {} competitor ads by URLs for user {}", adUrls.size(), userId);

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.fbadsautomation.exception.ResourceException.notFound("User", String.valueOf(userId)));

        // Validate URL limit (prevent DoS)
        if (adUrls.size() > 10) {
            throw com.fbadsautomation.exception.ResourceException.operationFailed(
                "CompetitorAds",
                "batch",
                "fetch",
                new IllegalArgumentException("Cannot fetch more than 10 ads at once. Please reduce the number of URLs.")
            );
        }

        List<CompetitorAdDTO> results = new ArrayList<>();

        for (String url : adUrls) {
            try {
                // Validate URL
                if (!isValidAdLibraryUrl(url)) {
                    log.warn("Invalid Ad Library URL: {}", url);
                    continue;
                }

                // Extract ad content
                CompetitorAdDTO adDTO = fetchSingleCompetitorAd(url);
                if (adDTO != null) {
                    adDTO.sanitize(); // Security: XSS prevention
                    results.add(adDTO);
                }

            } catch (Exception e) {
                log.error("Error fetching ad from URL {}: {}", url, e.getMessage());
                // Continue processing other URLs
            }
        }

        log.info("Successfully fetched {} out of {} competitor ads", results.size(), adUrls.size());
        return results;
    }

    /**
     * Fetch a single competitor ad from URL
     *
     * @param adUrl Facebook Ad Library URL
     * @return CompetitorAdDTO or null if fetch fails
     */
    private CompetitorAdDTO fetchSingleCompetitorAd(String adUrl) {
        // Extract ad ID from URL
        String adId = metaAdLibraryService.extractAdIdFromUrl(adUrl);
        if (adId == null) {
            log.warn("Could not extract ad ID from URL: {}", adUrl);
            return null;
        }

        // Fetch ad content using MetaAdLibraryService
        List<Map<String, Object>> adData = metaAdLibraryService.extractAdTextAndImages(Collections.singletonList(adUrl));

        if (adData == null || adData.isEmpty()) {
            log.warn("No ad data returned for URL: {}", adUrl);
            return null;
        }

        // Convert to DTO
        return convertToDTO(adData.get(0), adId, adUrl);
    }

    /**
     * Convert raw ad data map to structured DTO
     *
     * @param adData Raw ad data from scraping service
     * @param adId Ad ID
     * @param adUrl Ad Library URL
     * @return CompetitorAdDTO
     */
    private CompetitorAdDTO convertToDTO(Map<String, Object> adData, String adId, String adUrl) {
        try {
            CompetitorAdDTO dto = CompetitorAdDTO.builder()
                    .adId(adId)
                    .adLibraryUrl(adUrl)
                    .dataSource("SCRAPE_CREATORS_API")
                    .isActive(true) // Assume active if found
                    .build();

            // Extract text content
            if (adData.containsKey("text")) {
                String text = String.valueOf(adData.get("text"));
                // Split text into headline and primary text (heuristic)
                if (text != null && text.length() > 0) {
                    if (text.length() > 100) {
                        dto.setHeadline(text.substring(0, 97) + "...");
                        dto.setPrimaryText(text);
                    } else {
                        dto.setHeadline(text);
                        dto.setPrimaryText(text);
                    }
                }
            }

            // Extract images
            if (adData.containsKey("images")) {
                Object imagesObj = adData.get("images");
                if (imagesObj instanceof List) {
                    List<String> imageUrls = ((List<?>) imagesObj).stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    dto.setImageUrls(imageUrls);
                }
            }

            // Set metadata
            dto.setMetadata(adData);

            return dto;

        } catch (Exception e) {
            log.error("Error converting ad data to DTO: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert ScrapeCreators API response to CompetitorAdDTO
     *
     * Maps ScrapeCreators data structure to internal DTO format
     * Handles various date formats and missing fields gracefully
     *
     * @param apiResponse Raw response from ScrapeCreators API
     * @return CompetitorAdDTO or null if conversion fails
     */
    private CompetitorAdDTO convertScrapeCreatorsAdToDTO(Map<String, Object> apiResponse) {
        try {
            CompetitorAdDTO dto = CompetitorAdDTO.builder()
                    .dataSource("SCRAPE_CREATORS_API")
                    .isActive(true)
                    .build();

            // Extract ad ID (try both formats)
            String adId = extractString(apiResponse, "adId", "ad_id", "id");
            dto.setAdId(adId);

            // Build Ad Library URL if we have ad ID
            if (adId != null) {
                dto.setAdLibraryUrl("https://www.facebook.com/ads/library/?id=" + adId);
            }

            // Extract snapshot data (main ad content)
            Map<String, Object> snapshot = extractMap(apiResponse, "snapshot");
            if (snapshot != null) {
                // Extract text content
                dto.setHeadline(extractString(snapshot, "title", "headline"));
                dto.setPrimaryText(extractString(snapshot, "body", "text", "body_text"));
                dto.setDescription(extractString(snapshot, "link_description", "description"));

                // Extract images
                List<String> imageUrls = extractStringList(snapshot, "images", "image_url", "image_urls");
                dto.setImageUrls(imageUrls);

                // Extract video - DTO has single videoUrl field
                List<String> videoUrls = extractStringList(snapshot, "videos", "video_url", "video_urls");
                if (!videoUrls.isEmpty()) {
                    dto.setVideoUrl(videoUrls.get(0)); // Use first video
                }

                // Extract CTA
                dto.setCallToAction(extractString(snapshot, "cta_text", "call_to_action"));

                // Extract landing page
                dto.setLandingPageUrl(extractString(snapshot, "link_url", "landing_page_url", "website_url"));
            }

            // Extract advertiser info
            dto.setAdvertiserName(extractString(apiResponse, "page_name", "advertiser_name", "pageName"));

            // Extract dates
            dto.setStartDate(parseDate(extractString(apiResponse, "start_date", "startDate")));
            dto.setEndDate(parseDate(extractString(apiResponse, "end_date", "endDate")));

            // Extract platforms
            List<String> platforms = extractStringList(apiResponse, "platforms", "publisher_platforms");
            dto.setPlatforms(platforms);

            // Extract regions - DTO uses targetRegions field
            List<String> regions = extractStringList(apiResponse, "regions", "target_countries");
            dto.setTargetRegions(regions);

            // Extract impressions - DTO uses estimatedImpressions field
            dto.setEstimatedImpressions(extractString(apiResponse, "impressions", "spend", "delivery"));

            // Store full metadata
            dto.setMetadata(apiResponse);

            // Sanitize for XSS
            dto.sanitize();

            return dto;

        } catch (Exception e) {
            log.error("Error converting ScrapeCreators data to DTO: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract string from map with fallback keys
     */
    private String extractString(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * Extract map from parent map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    /**
     * Extract list of strings with fallback keys
     */
    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> map, String... keys) {
        if (map == null) return new ArrayList<>();

        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .collect(Collectors.toList());
            } else if (value instanceof String) {
                return Collections.singletonList((String) value);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Parse date string to LocalDate
     * Supports multiple formats: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Try ISO format first (yyyy-MM-dd)
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e1) {
            try {
                // Try dd/MM/yyyy
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e2) {
                try {
                    // Try MM/dd/yyyy
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                } catch (Exception e3) {
                    log.warn("Could not parse date: {}", dateStr);
                    return null;
                }
            }
        }
    }

    /**
     * Get search history for user
     *
     * Security: Only returns searches for the authenticated user
     * Performance: Indexed query on user_id and search_date
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of search history
     */
    @Transactional(readOnly = true)
    public Page<CompetitorSearch> getSearchHistory(@NotNull Long userId, Pageable pageable) {
        log.info("Fetching search history for user {}", userId);

        List<CompetitorSearch> searches = competitorSearchRepository.findByUserId(userId);

        // Sort by date descending
        searches.sort(Comparator.comparing(CompetitorSearch::getSearchDate).reversed());

        // Apply pagination manually (for simplicity)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searches.size());

        List<CompetitorSearch> pageContent = start < searches.size()
                ? searches.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, searches.size());
    }

    /**
     * Get autocomplete suggestions for brand names based on search history
     *
     * @param userId User ID
     * @param prefix Brand name prefix
     * @param limit Maximum suggestions
     * @return List of brand name suggestions
     */
    @Transactional(readOnly = true)
    public List<String> getBrandNameSuggestions(@NotNull Long userId, String prefix, int limit) {
        log.info("Getting brand name suggestions for user {} with prefix: {}", userId, prefix);

        List<CompetitorSearch> searches = competitorSearchRepository.findByUserId(userId);

        return searches.stream()
                .map(CompetitorSearch::getBrandName)
                .filter(Objects::nonNull)
                .filter(name -> prefix == null || name.toLowerCase().startsWith(prefix.toLowerCase()))
                .distinct()
                .limit(Math.min(limit, 10))
                .collect(Collectors.toList());
    }

    /**
     * Search Google Ads using SerpAPI and return structured/fallback response.
     */
    public PlatformSearchResult searchGoogleAds(String brandName, String region, Long userId, int limit) {
        log.info("Searching Google Ads for brand: {}, region: {}", brandName, region);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceException.notFound("User", String.valueOf(userId)));

        String sanitizedBrand = sanitizeBrandName(brandName);
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        PlatformSearchResult result = serpApiService.searchGoogleAds(sanitizedBrand, region, safeLimit);
        boolean hasStructuredData = result != null
                && result.getMode() == PlatformSearchMode.DATA
                && result.getAds() != null
                && !result.getAds().isEmpty();

        saveSearchHistory(
            sanitizedBrand,
            region,
            user,
            "GOOGLE",
            hasStructuredData && result.getTotalResults() != null ? result.getTotalResults() : 0,
            hasStructuredData
        );

        return result;
    }

    /**
     * Search TikTok ads via Apify Creative Center scraper
     *
     * @param brandName Brand name to search
     * @param region Target region code
     * @param userId User performing search
     * @param limit Maximum number of ads
     * @return List of competitor ads or null for iframe fallback
     */
    public PlatformSearchResult searchTikTokAds(String brandName, String region, Long userId, int limit) {
        log.info("TikTok search requested for: {}", brandName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceException.notFound("User", String.valueOf(userId)));

        String sanitizedBrand = sanitizeBrandName(brandName);

        PlatformSearchResult result;
        if (apifyService == null) {
            log.warn("Apify service bean not available, returning iframe fallback for TikTok");
            result = PlatformSearchResult.builder()
                .success(false)
                .platform(com.fbadsautomation.dto.AdPlatform.TIKTOK)
                .brandName(sanitizedBrand)
                .region(region)
                .mode(PlatformSearchMode.IFRAME)
                .errorCode(PlatformSearchErrorCode.CONFIG_MISSING)
                .message("TikTok Ads search chưa khả dụng trên hệ thống này.")
                .iframeUrl(buildTikTokIframeUrl(sanitizedBrand))
                .friendlySuggestion("Cấu hình Apify để nhận dữ liệu chi tiết hoặc dùng iframe.")
                .build();
        } else {
            result = apifyService.searchTikTokAds(sanitizedBrand, region, limit);
        }

        boolean hasStructuredData = result != null
                && result.getMode() == PlatformSearchMode.DATA
                && result.getAds() != null
                && !result.getAds().isEmpty();

        saveSearchHistory(
            sanitizedBrand,
            region,
            user,
            "TIKTOK",
            hasStructuredData && result.getTotalResults() != null ? result.getTotalResults() : 0,
            hasStructuredData
        );

        return result;
    }

    private String buildTikTokIframeUrl(String brandName) {
        String encoded = java.net.URLEncoder.encode(brandName != null ? brandName : "", java.nio.charset.StandardCharsets.UTF_8);
        return "https://ads.tiktok.com/business/creativecenter/inspiration/topads/pc/en?keyword=" + encoded;
    }

    /**
     * Save search history entry with platform information
     *
     * @param brandName Brand name searched
     * @param region Region code
     * @param user User who performed search
     * @param platform Platform (FACEBOOK, GOOGLE, TIKTOK)
     * @param resultCount Number of results
     * @param success Whether search was successful
     */
    @Transactional
    private void saveSearchHistory(String brandName, String region, User user, String platform, int resultCount, boolean success) {
        try {
            CompetitorSearch search = new CompetitorSearch();
            search.setBrandName(brandName);
            search.setIndustry(region); // Reusing industry field for region
            search.setUser(user);
            search.setSearchDate(LocalDateTime.now());
            search.setSearchType(platform); // Store platform in searchType field
            search.setResultCount(resultCount);
            search.setSuccess(success);

            competitorSearchRepository.save(search);
            log.debug("Saved search history: brand={}, region={}, platform={}, user={}",
                     brandName, region, platform, user.getId());

        } catch (Exception e) {
            // Don't fail the main operation if history save fails
            log.error("Failed to save search history: {}", e.getMessage());
        }
    }

    /**
     * Save search history entry (legacy method for backward compatibility)
     *
     * @param brandName Brand name searched
     * @param region Region code
     * @param user User who performed search
     */
    @Transactional
    private void saveSearchHistory(String brandName, String region, User user) {
        saveSearchHistory(brandName, region, user, "FACEBOOK", 0, true);
    }

    /**
     * Sanitize brand name input
     * Security: Prevents injection attacks and XSS
     *
     * @param brandName Raw brand name
     * @return Sanitized brand name
     */
    private String sanitizeBrandName(String brandName) {
        if (brandName == null) return "";

        // Remove special characters that could be used for injection
        String sanitized = brandName.replaceAll("[<>\"'%;)(&+]", "")
                                     .trim();

        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    /**
     * Validate if URL is a valid Facebook Ad Library URL
     *
     * @param url URL to validate
     * @return true if valid
     */
    private boolean isValidAdLibraryUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        return url.contains("facebook.com/ads/library") && url.contains("id=");
    }

    /**
     * Delete old search history entries (cleanup job)
     * Removes searches older than 90 days
     *
     * Performance: Should be run as scheduled job during off-peak hours
     *
     * @return Number of deleted entries
     */
    @Transactional
    public int cleanupOldSearchHistory() {
        log.info("Cleaning up old competitor search history");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        List<CompetitorSearch> allSearches = competitorSearchRepository.findAll();

        List<CompetitorSearch> oldSearches = allSearches.stream()
                .filter(search -> search.getSearchDate().isBefore(cutoffDate))
                .collect(Collectors.toList());

        if (!oldSearches.isEmpty()) {
            competitorSearchRepository.deleteAll(oldSearches);
            log.info("Deleted {} old search history entries", oldSearches.size());
            return oldSearches.size();
        }

        return 0;
    }
}
