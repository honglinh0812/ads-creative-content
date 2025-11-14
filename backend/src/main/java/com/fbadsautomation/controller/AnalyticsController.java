package com.fbadsautomation.controller;
import com.fbadsautomation.dto.AnalyticsResponse;
import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.dto.ContentInsightsResponse;
import com.fbadsautomation.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics", description = "Analytics and reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    
    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Get analytics dashboard", description = "Retrieves comprehensive analytics dashboard data for the authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analytics dashboard data retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalyticsDashboard(
            @Parameter(description = "Time range for analytics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching analytics dashboard for user ID: {} with time range: {}", userId, timeRange);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("Analytics dashboard retrieved successfully", analytics));
        } catch (Exception e) {
            log.error("Error fetching analytics dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch analytics dashboard"));
        }
    }

    @GetMapping("/content-insights")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ContentInsightsResponse>> getContentInsights(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            ContentInsightsResponse response = analyticsService.getContentInsights(userId);
            return ResponseEntity.ok(ApiResponse.success("Content insights retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching content insights", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to fetch content insights"));
        }
    }

    @Operation(summary = "Get KPI metrics", description = "Retrieves key performance indicator metrics for the authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KPI metrics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.KPIMetrics>> getKPIMetrics(
            @Parameter(description = "Time range for KPI metrics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching KPI metrics for user ID: {} with time range: {}", userId, timeRange);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("KPI metrics retrieved successfully", analytics.getKpiMetrics()));
        } catch (Exception e) {
            log.error("Error fetching KPI metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch KPI metrics"));
        }
    }

    @Operation(summary = "Get performance trends", description = "Retrieves performance trends over time for the authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Performance trends retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/trends")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<java.util.List<AnalyticsResponse.TimeSeriesData>>> getPerformanceTrends(
            @Parameter(description = "Time range for trends (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            @Parameter(description = "Specific metric to analyze") @RequestParam(required = false) String metric,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching performance trends for user ID: {} with time range: {} and metric: {}", 
                    userId, timeRange, metric);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("Performance trends retrieved successfully", 
                    analytics.getPerformanceTrends()));
        } catch (Exception e) {
            log.error("Error fetching performance trends: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch performance trends"));
        }
    }

    @Operation(summary = "Get campaign analytics", description = "Retrieves detailed analytics for campaigns")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Campaign analytics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/campaigns")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<java.util.List<AnalyticsResponse.CampaignAnalytics>>> getCampaignAnalytics(
            @Parameter(description = "Time range for campaign analytics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            @Parameter(description = "Filter by campaign status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by campaign objective") @RequestParam(required = false) String objective,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching campaign analytics for user ID: {} with filters - timeRange: {}, status: {}, objective: {}", 
                    userId, timeRange, status, objective);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            java.util.List<AnalyticsResponse.CampaignAnalytics> campaignAnalytics = analytics.getCampaignAnalytics();
            // Apply filters
            if (status != null && !status.isEmpty()) {
                campaignAnalytics = campaignAnalytics.stream()
                        .filter(ca -> status.equalsIgnoreCase(ca.getStatus()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (objective != null && !objective.isEmpty()) {
                campaignAnalytics = campaignAnalytics.stream()
                        .filter(ca -> objective.equalsIgnoreCase(ca.getObjective()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return ResponseEntity.ok(ApiResponse.success("Campaign analytics retrieved successfully", campaignAnalytics));
        } catch (Exception e) {
            log.error("Error fetching campaign analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch campaign analytics"));
        }
    }

    @Operation(summary = "Get ad analytics", description = "Retrieves detailed analytics for ads")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ad analytics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/ads")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<java.util.List<AnalyticsResponse.AdAnalytics>>> getAdAnalytics(
            @Parameter(description = "Time range for ad analytics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            @Parameter(description = "Filter by ad status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by ad type") @RequestParam(required = false) String adType,
            @Parameter(description = "Filter by AI provider") @RequestParam(required = false) String aiProvider,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching ad analytics for user ID: {} with filters - timeRange: {}, status: {}, adType: {}, aiProvider: {}", 
                    userId, timeRange, status, adType, aiProvider);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            java.util.List<AnalyticsResponse.AdAnalytics> adAnalytics = analytics.getAdAnalytics();
            // Apply filters
            if (status != null && !status.isEmpty()) {
                adAnalytics = adAnalytics.stream()
                        .filter(aa -> status.equalsIgnoreCase(aa.getStatus()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (adType != null && !adType.isEmpty()) {
                adAnalytics = adAnalytics.stream()
                        .filter(aa -> adType.equalsIgnoreCase(aa.getAdType()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (aiProvider != null && !aiProvider.isEmpty()) {
                adAnalytics = adAnalytics.stream()
                        .filter(aa -> aiProvider.equalsIgnoreCase(aa.getAiProvider()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            return ResponseEntity.ok(ApiResponse.success("Ad analytics retrieved successfully", adAnalytics));
        } catch (Exception e) {
            log.error("Error fetching ad analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch ad analytics"));
        }
    }

    @Operation(summary = "Get AI provider analytics", description = "Retrieves analytics for AI providers usage and performance")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI provider analytics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/ai-providers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.AIProviderAnalytics>> getAIProviderAnalytics(
            @Parameter(description = "Time range for AI provider analytics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching AI provider analytics for user ID: {} with time range: {}", userId, timeRange);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("AI provider analytics retrieved successfully", 
                    analytics.getAiProviderAnalytics()));
        } catch (Exception e) {
            log.error("Error fetching AI provider analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch AI provider analytics"));
        }
    }

    @Operation(summary = "Get budget analytics", description = "Retrieves budget utilization and spending analytics")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Budget analytics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/budget")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.BudgetAnalytics>> getBudgetAnalytics(
            @Parameter(description = "Time range for budget analytics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching budget analytics for user ID: {} with time range: {}", userId, timeRange);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("Budget analytics retrieved successfully", 
                    analytics.getBudgetAnalytics()));
        } catch (Exception e) {
            log.error("Error fetching budget analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch budget analytics"));
        }
    }

    @Operation(summary = "Get content analytics", description = "Retrieves analytics for content performance and engagement")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content analytics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/content")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.ContentAnalytics>> getContentAnalytics(
            @Parameter(description = "Time range for content analytics (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching content analytics for user ID: {} with time range: {}", userId, timeRange);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("Content analytics retrieved successfully", 
                    analytics.getContentAnalytics()));
        } catch (Exception e) {
            log.error("Error fetching content analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch content analytics"));
        }
    }

    @Operation(summary = "Get analytics summary", description = "Retrieves a comprehensive summary of all analytics data")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analytics summary retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsSummary(
            @Parameter(description = "Time range for analytics summary (e.g., 7d, 30d, 90d)") @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching analytics summary for user ID: {} with time range: {}", userId, timeRange);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            AnalyticsResponse.KPIMetrics kpis = analytics.getKpiMetrics();
            Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("totalCampaigns", kpis.getTotalCampaigns());
            summary.put("totalAds", kpis.getTotalAds());
            summary.put("totalBudget", kpis.getTotalBudget());
            summary.put("totalSpent", kpis.getTotalSpent());
            summary.put("totalImpressions", kpis.getTotalImpressions());
            summary.put("totalClicks", kpis.getTotalClicks());
            summary.put("averageCTR", kpis.getAverageCTR());
            summary.put("roi", kpis.getRoi());
            summary.put("contentGenerated", kpis.getContentGenerated());
            summary.put("aiCostSavings", kpis.getAiCostSavings());
            summary.put("campaignGrowth", kpis.getCampaignGrowth());
            summary.put("adGrowth", kpis.getAdGrowth());
            summary.put("timeRange", timeRange);
            summary.put("generatedAt", analytics.getGeneratedAt());
            return ResponseEntity.ok(ApiResponse.success("Analytics summary retrieved successfully", summary));
        } catch (Exception e) {
            log.error("Error fetching analytics summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch analytics summary"));
        }
    }

    @Operation(summary = "Get filter options", description = "Retrieves available filter options for analytics endpoints")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filter options retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/filters")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFilterOptions(Authentication authentication) {
        
        try {
            Map<String, Object> filters = Map.of("timeRanges", java.util.Arrays.asList("7d", "30d", "90d", "1y"),
                "campaignStatuses", java.util.Arrays.asList("ACTIVE", "PAUSED", "DRAFT", "COMPLETED"),
                "campaignObjectives", java.util.Arrays.asList("BRAND_AWARENESS", "REACH", "TRAFFIC", "ENGAGEMENT", 
                                                            "APP_INSTALLS", "VIDEO_VIEWS", "LEAD_GENERATION", "CONVERSIONS"),
                "adStatuses", java.util.Arrays.asList("ACTIVE", "PAUSED", "DRAFT"),
                "adTypes", java.util.Arrays.asList("SINGLE_IMAGE", "SINGLE_VIDEO", "CAROUSEL", "COLLECTION"),
                "aiProviders", java.util.Arrays.asList("OPENAI", "GEMINI", "ANTHROPIC", "HUGGINGFACE", "FAL_AI", "STABLE_DIFFUSION"),
                "metrics", java.util.Arrays.asList("impressions", "clicks", "ctr", "cpc", "conversions", "spend", "roi")
            );
            return ResponseEntity.ok(ApiResponse.success("Filter options retrieved successfully", filters));
        } catch (Exception e) {
            log.error("Error fetching filter options: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch filter options"));
        }
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        // Extract user ID from Spring Security's UserDetails
        // The CustomUserDetailsService sets username as user.getId().toString()
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) principal;
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                log.error("Failed to parse user ID from username: {}", userDetails.getUsername());
                throw new RuntimeException("Invalid user ID format");
            }
        } else if (principal instanceof String) {
            // JWT token might provide user ID directly as string
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                log.error("Failed to parse user ID from principal: {}", principal);
                throw new RuntimeException("Invalid user ID format");
            }
        }

        log.error("Unable to extract user ID from authentication principal type: {}",
                 principal.getClass().getName());
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}
