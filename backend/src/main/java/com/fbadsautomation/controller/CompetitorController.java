package com.fbadsautomation.controller;

import com.fbadsautomation.dto.CompetitorAdDTO;
import com.fbadsautomation.model.CompetitorSearch;
import com.fbadsautomation.service.ComparisonService;
import com.fbadsautomation.service.CompetitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Competitor Insights functionality
 *
 * Provides endpoints for:
 * - Searching competitor ads by brand name
 * - Fetching specific competitor ads by URL
 * - Viewing search history
 * - AI-powered ad comparison and suggestions
 *
 * @author AI Panel - Senior Engineers
 * @version 1.0
 * @security All endpoints require authentication, enforce user ownership
 */
@RestController
@RequestMapping("/competitors")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Competitor Insights", description = "APIs for competitor ad analysis and comparison")
@SecurityRequirement(name = "bearerAuth")
public class CompetitorController {

    private final CompetitorService competitorService;
    private final ComparisonService comparisonService;


    /**
     * Fetch specific competitor ads by Facebook Ad Library URLs
     *
     * Endpoint: POST /api/competitors/ads/fetch
     *
     * @param request Request containing list of Ad Library URLs
     * @param authentication Spring Security authentication
     * @return List of fetched competitor ads
     */
    @Operation(
        summary = "Fetch competitor ads by URLs",
        description = "Fetch specific ads from Facebook Ad Library using their URLs. Maximum 10 URLs per request."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fetch successful"),
        @ApiResponse(responseCode = "400", description = "Invalid URLs or too many URLs (max 10)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/ads/fetch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> fetchCompetitorAdsByUrls(
            @Valid @RequestBody FetchAdsRequest request,
            Authentication authentication) {

        log.info("Fetching {} competitor ads by URLs for user: {}",
                 request.getAdUrls().size(), authentication.getName());

        try {
            Long userId = getUserIdFromAuthentication(authentication);

            List<CompetitorAdDTO> ads = competitorService.fetchCompetitorAdsByUrls(
                request.getAdUrls(),
                userId
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRequested", request.getAdUrls().size());
            response.put("totalFetched", ads.size());
            response.put("ads", ads);
            response.put("message", String.format("Successfully fetched %d out of %d ads",
                                                   ads.size(), request.getAdUrls().size()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching competitor ads: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


    /**
     * Get search history for current user
     *
     * Endpoint: GET /api/competitors/history
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param authentication Spring Security authentication
     * @return Paginated search history
     */
    @Operation(
        summary = "Get competitor search history",
        description = "Retrieve user's competitor search history with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CompetitorSearch>> getSearchHistory(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.info("Fetching search history for user: {}", authentication.getName());

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            Pageable pageable = PageRequest.of(page, size);

            Page<CompetitorSearch> history = competitorService.getSearchHistory(userId, pageable);

            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("Error fetching search history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get brand name autocomplete suggestions
     *
     * Endpoint: GET /api/competitors/suggestions
     *
     * @param prefix Brand name prefix for autocomplete
     * @param limit Maximum suggestions
     * @param authentication Spring Security authentication
     * @return List of brand name suggestions
     */
    @Operation(
        summary = "Get brand name suggestions",
        description = "Get autocomplete suggestions for brand names based on search history"
    )
    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<String>> getBrandNameSuggestions(
            @Parameter(description = "Brand name prefix") @RequestParam(required = false) String prefix,
            @Parameter(description = "Max suggestions") @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit,
            Authentication authentication) {

        log.info("Getting brand suggestions for prefix: {} by user: {}", prefix, authentication.getName());

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            List<String> suggestions = competitorService.getBrandNameSuggestions(userId, prefix, limit);

            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            log.error("Error getting suggestions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate AI-powered suggested ad variation based on competitor analysis
     *
     * Endpoint: POST /api/competitors/comparison/suggest
     *
     * @param request Comparison request with competitor ad and user's ad
     * @param authentication Spring Security authentication
     * @return AI-generated suggested variation
     */
    @Operation(
        summary = "Generate ad suggestion based on competitor",
        description = "Use AI to analyze competitor ad and suggest improvements for user's ad"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Suggestion generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    @PostMapping("/comparison/suggest")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> generateSuggestedVariation(
            @Valid @RequestBody ComparisonRequest request,
            Authentication authentication) {

        log.info("Generating ad suggestion for user: {}", authentication.getName());

        try {
            String suggestion = comparisonService.generateSuggestedVariation(
                request.getCompetitorAd(),
                request.getMyAd(),
                request.getAiProvider()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestedAd", suggestion);
            response.put("originalAd", request.getMyAd());
            response.put("competitorAdId", request.getCompetitorAd().getAdId());
            response.put("aiProvider", request.getAiProvider());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating suggestion: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate suggestion: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Analyze competitor ad and extract insights
     *
     * Endpoint: POST /api/competitors/analyze
     *
     * @param request Analysis request
     * @param authentication Spring Security authentication
     * @return Structured insights about the ad
     */
    @Operation(
        summary = "Analyze competitor ad",
        description = "Use AI to extract insights about competitor ad (strengths, weaknesses, patterns)"
    )
    @PostMapping("/analyze")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> analyzeCompetitorAd(
            @Valid @RequestBody AnalysisRequest request,
            Authentication authentication) {

        log.info("Analyzing competitor ad: {} for user: {}",
                 request.getCompetitorAd().getAdId(), authentication.getName());

        try {
            Map<String, Object> insights = comparisonService.analyzeCompetitorAd(
                request.getCompetitorAd(),
                request.getAiProvider()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("adId", request.getCompetitorAd().getAdId());
            response.put("insights", insights);
            response.put("aiProvider", request.getAiProvider());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing ad: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Identify common patterns across multiple competitor ads
     *
     * Endpoint: POST /api/competitors/patterns
     *
     * @param request Request containing list of competitor ads
     * @param authentication Spring Security authentication
     * @return Common patterns and trends
     */
    @Operation(
        summary = "Identify common patterns in competitor ads",
        description = "Analyze multiple competitor ads to identify common patterns and trends"
    )
    @PostMapping("/patterns")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> identifyCommonPatterns(
            @Valid @RequestBody PatternAnalysisRequest request,
            Authentication authentication) {

        log.info("Identifying patterns across {} ads for user: {}",
                 request.getCompetitorAds().size(), authentication.getName());

        try {
            Map<String, Object> patterns = comparisonService.identifyCommonPatterns(
                request.getCompetitorAds(),
                request.getAiProvider()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalAdsAnalyzed", request.getCompetitorAds().size());
            response.put("patterns", patterns);
            response.put("aiProvider", request.getAiProvider());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error identifying patterns: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Generate A/B test variations based on competitor insights
     *
     * Endpoint: POST /api/competitors/ab-test
     *
     * @param request A/B test generation request
     * @param authentication Spring Security authentication
     * @return List of ad variations
     */
    @Operation(
        summary = "Generate A/B test variations",
        description = "Generate multiple ad variations for A/B testing based on competitor insights"
    )
    @PostMapping("/ab-test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> generateABTestVariations(
            @Valid @RequestBody ABTestRequest request,
            Authentication authentication) {

        log.info("Generating {} A/B test variations for user: {}",
                 request.getNumberOfVariations(), authentication.getName());

        try {
            List<com.fbadsautomation.dto.AdVariationDTO> variations = comparisonService.generateABTestVariations(
                request.getBaseAd(),
                request.getCompetitorInsights(),
                request.getNumberOfVariations(),
                request.getAiProvider()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("baseAd", request.getBaseAd());
            response.put("variations", variations);
            response.put("count", variations.size());
            response.put("aiProvider", request.getAiProvider());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating A/B variations: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Extract user ID from Spring Security authentication
     *
     * @param authentication Spring Security authentication object
     * @return User ID
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // Assuming authentication.getName() returns user ID as string
        // Adjust based on your authentication implementation
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // If name is username, need to look up user ID
            log.warn("Authentication name is not a numeric ID: {}", authentication.getName());
            // Fallback: return a default or throw exception
            throw new RuntimeException("Invalid user ID in authentication");
        }
    }

    // ==================== Request/Response DTOs ====================

    /**
     * Request DTO for fetching ads by URLs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request for fetching competitor ads by URLs")
    public static class FetchAdsRequest {

        @NotNull(message = "Ad URLs are required")
        @javax.validation.constraints.Size(min = 1, max = 10, message = "Must provide 1-10 ad URLs")
        @Schema(description = "List of Facebook Ad Library URLs",
                example = "[\"https://www.facebook.com/ads/library/?id=123\"]",
                required = true)
        private List<String> adUrls;
    }

    /**
     * Request DTO for ad comparison
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request for generating ad suggestion based on competitor")
    public static class ComparisonRequest {

        @NotNull(message = "Competitor ad is required")
        @Schema(description = "Competitor's ad to analyze", required = true)
        private CompetitorAdDTO competitorAd;

        @NotBlank(message = "Your ad text is required")
        @Schema(description = "Your current ad text", example = "Check out our amazing products!", required = true)
        private String myAd;

        @Schema(description = "AI provider to use", example = "openai", defaultValue = "openai",
                allowableValues = {"openai", "anthropic", "gemini"})
        private String aiProvider = "openai";
    }

    /**
     * Request DTO for ad analysis
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request for analyzing a competitor ad")
    public static class AnalysisRequest {

        @NotNull(message = "Competitor ad is required")
        @Schema(description = "Competitor's ad to analyze", required = true)
        private CompetitorAdDTO competitorAd;

        @Schema(description = "AI provider to use", example = "openai", defaultValue = "openai")
        private String aiProvider = "openai";
    }

    /**
     * Request DTO for pattern analysis
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request for pattern analysis across multiple ads")
    public static class PatternAnalysisRequest {

        @NotNull(message = "Competitor ads are required")
        @javax.validation.constraints.Size(min = 2, max = 20, message = "Must provide 2-20 ads for pattern analysis")
        @Schema(description = "List of competitor ads to analyze", required = true)
        private List<CompetitorAdDTO> competitorAds;

        @Schema(description = "AI provider to use", example = "openai", defaultValue = "openai")
        private String aiProvider = "openai";
    }

    /**
     * Request DTO for A/B test generation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request for generating A/B test variations")
    public static class ABTestRequest {

        @NotBlank(message = "Base ad is required")
        @Schema(description = "Base ad to create variations from", example = "Shop now and save!", required = true)
        private String baseAd;

        @Schema(description = "Insights from competitor analysis to apply")
        private Map<String, Object> competitorInsights;

        @Min(1)
        @Max(5)
        @Schema(description = "Number of variations to generate", example = "3", defaultValue = "3")
        private int numberOfVariations = 3;

        @Schema(description = "AI provider to use", example = "openai", defaultValue = "openai")
        private String aiProvider = "openai";
    }
}
