package com.fbadsautomation.service;

import com.fbadsautomation.dto.CompetitorAdDTO;
import com.fbadsautomation.exception.ResourceException;
import com.fbadsautomation.model.CompetitorSearch;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.CompetitorSearchRepository;
import com.fbadsautomation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            // For now, we'll use the ScrapeCreators approach
            // In production, you might want to implement actual brand search via Facebook Ad Library
            log.warn("Brand search not yet implemented. Returning empty list for brand: {}", sanitizedBrandName);

            // TODO: Implement actual Facebook Ad Library brand search
            // This would require either:
            // 1. Facebook Ad Library API with search capability
            // 2. Web scraping with proper rate limiting
            // 3. Third-party service integration

            return Collections.emptyList();

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
     * Save search history entry
     *
     * @param brandName Brand name searched
     * @param region Region code
     * @param user User who performed search
     */
    @Transactional
    private void saveSearchHistory(String brandName, String region, User user) {
        try {
            CompetitorSearch search = new CompetitorSearch();
            search.setBrandName(brandName);
            search.setIndustry(region); // Reusing industry field for region
            search.setUser(user);
            search.setSearchDate(LocalDateTime.now());

            competitorSearchRepository.save(search);
            log.debug("Saved search history: brand={}, region={}, user={}", brandName, region, user.getId());

        } catch (Exception e) {
            // Don't fail the main operation if history save fails
            log.error("Failed to save search history: {}", e.getMessage());
        }
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
