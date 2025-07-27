package com.fbadsautomation.controller;
import com.fbadsautomation.dto.AnalyticsResponse;
import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.service.AnalyticsService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor

public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get comprehensive analytics dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalyticsDashboard(
            @RequestParam(defaultValue = "30d") String timeRange,
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

    /**
     * Get KPI metrics only
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.KPIMetrics>> getKPIMetrics(
            @RequestParam(defaultValue = "30d") String timeRange,
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

    /**
     * Get performance trends
     */
    @GetMapping("/trends")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<java.util.List<AnalyticsResponse.TimeSeriesData>>> getPerformanceTrends(
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(required = false) String metric,
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

    /**
     * Get campaign analytics
     */
    @GetMapping("/campaigns")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<java.util.List<AnalyticsResponse.CampaignAnalytics>>> getCampaignAnalytics(
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String objective,
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

    /**
     * Get ad analytics
     */
    @GetMapping("/ads")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<java.util.List<AnalyticsResponse.AdAnalytics>>> getAdAnalytics(
            @RequestParam(defaultValue = "30d") String timeRange,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String adType,
            @RequestParam(required = false) String aiProvider,
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

    /**
     * Get AI provider analytics
     */
    @GetMapping("/ai-providers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.AIProviderAnalytics>> getAIProviderAnalytics(
            @RequestParam(defaultValue = "30d") String timeRange,
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

    /**
     * Get budget analytics
     */
    @GetMapping("/budget")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.BudgetAnalytics>> getBudgetAnalytics(
            @RequestParam(defaultValue = "30d") String timeRange,
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

    /**
     * Get content analytics
     */
    @GetMapping("/content")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AnalyticsResponse.ContentAnalytics>> getContentAnalytics(
            @RequestParam(defaultValue = "30d") String timeRange,
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

    /**
     * Get analytics summary for quick overview
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsSummary(
            @RequestParam(defaultValue = "30d") String timeRange,
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

    /**
     * Get available filter options
     */
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
        // This is a simplified implementation
        // In a real application, you would extract the user ID from the JWT token or user details
        return 1L; // Placeholder - replace with actual user ID extraction logic
    }
}
