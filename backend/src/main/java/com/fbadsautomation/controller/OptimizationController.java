package com.fbadsautomation.controller;

import com.fbadsautomation.dto.AdCopyRewriteRequest;
import com.fbadsautomation.dto.AdCopyRewriteResponse;
import com.fbadsautomation.dto.AdOptimizationAnalyzeRequest;
import com.fbadsautomation.dto.AdOptimizationInsightDTO;
import com.fbadsautomation.dto.AdOptimizationSnapshotDTO;
import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.dto.OptimizationResponse.*;
import com.fbadsautomation.dto.OptimizationResponse;
import com.fbadsautomation.dto.SaveAdOptimizationInsightRequest;
import com.fbadsautomation.service.AdOptimizationInsightService;
import com.fbadsautomation.service.OptimizationService;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/optimization")
public class OptimizationController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationController.class);
    
    private final OptimizationService optimizationService;
    private final AdOptimizationInsightService adOptimizationInsightService;

    @Autowired
    public OptimizationController(OptimizationService optimizationService,
                                  AdOptimizationInsightService adOptimizationInsightService) {
        this.optimizationService = optimizationService;
        this.adOptimizationInsightService = adOptimizationInsightService;
    }

    /**
     * Get comprehensive optimization recommendations
     */
    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OptimizationResponse>> getOptimizationRecommendations(
            @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching optimization recommendations for user ID: {} with time range: {}", userId, timeRange);
            OptimizationResponse recommendations = optimizationService.generateRecommendations(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("Optimization recommendations retrieved successfully", recommendations));
        } catch (Exception e) {
            log.error("Error fetching optimization recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch optimization recommendations"));
        }
    }

    /**
     * Simplified ad-level analysis used by the streamlined Optimization view.
     */
    @PostMapping("/ad-insights/analyze")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<AdOptimizationInsightDTO>>> analyzeAds(
            @RequestBody AdOptimizationAnalyzeRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            List<AdOptimizationInsightDTO> insights = adOptimizationInsightService.analyzeAds(userId, request);
            return ResponseEntity.ok(ApiResponse.success("Ad insights generated", insights));
        } catch (Exception e) {
            log.error("Error analyzing ads", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to analyze ads"));
        }
    }

    /**
     * Persist an optimization snapshot so the user can revisit it later.
     */
    @PostMapping("/ad-insights/save")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AdOptimizationSnapshotDTO>> saveAdInsight(
            @RequestBody SaveAdOptimizationInsightRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            AdOptimizationSnapshotDTO snapshot = adOptimizationInsightService.saveSnapshot(userId, request);
            return ResponseEntity.ok(ApiResponse.success("Optimization snapshot saved", snapshot));
        } catch (Exception e) {
            log.error("Error saving optimization snapshot", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to save optimization snapshot"));
        }
    }

    /**
     * List previously saved optimization snapshots.
     */
    @GetMapping("/ad-insights/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<AdOptimizationSnapshotDTO>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            Page<AdOptimizationSnapshotDTO> history = adOptimizationInsightService.history(
                userId, PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success("Optimization history retrieved", history));
        } catch (Exception e) {
            log.error("Error fetching optimization history", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to fetch optimization history"));
        }
    }

    @PostMapping("/ad-insights/{adId}/rewrite")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<AdCopyRewriteResponse>> rewriteAdCopy(
            @PathVariable Long adId,
            @RequestBody AdCopyRewriteRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            return adOptimizationInsightService.rewriteSection(userId, adId, request)
                .map(response -> ResponseEntity.ok(ApiResponse.success("Rewrite generated", response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error("Unable to generate rewrite right now")));
        } catch (Exception e) {
            log.error("Error rewriting ad copy", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to rewrite ad copy"));
        }
    }

    /**
     * Get recommendations by type
     */
    @GetMapping("/recommendations/type/{type}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Recommendation>>> getRecommendationsByType(
            @PathVariable RecommendationType type,
            @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching {} recommendations for user ID: {} with time range: {}", type, userId, timeRange);
            List<Recommendation> recommendations = optimizationService.getRecommendationsByType(userId, type, timeRange);
            return ResponseEntity.ok(ApiResponse.success(
                String.format("%s recommendations retrieved successfully", type), recommendations));
            
        } catch (Exception e) {
            log.error("Error fetching recommendations by type {}: {}", type, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch recommendations by type"));
        }
    }

    /**
     * Get high priority recommendations only
     */
    @GetMapping("/recommendations/priority/high")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Recommendation>>> getHighPriorityRecommendations(
            @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        Long userId = null;
        try {
            userId = getUserIdFromAuthentication(authentication);
            log.info("[OPTIMIZATION] Starting high priority recommendations fetch for user ID: {}, timeRange: {}", userId, timeRange);
            
            List<Recommendation> recommendations = optimizationService.getHighPriorityRecommendations(userId, timeRange);
            
            // Validate recommendations list
            if (recommendations == null) {
                log.warn("[OPTIMIZATION] Recommendations list is null for user ID: {}, returning empty list", userId);
                recommendations = new ArrayList<>();
            }
            
            log.info("[OPTIMIZATION] Successfully fetched {} high priority recommendations for user ID: {}", 
                    recommendations.size(), userId);
            
            return ResponseEntity.ok(ApiResponse.success("High priority recommendations retrieved successfully", recommendations));
            
        } catch (Exception e) {
            log.error("[OPTIMIZATION] ERROR fetching high priority recommendations for user ID: {}, timeRange: {}, error: {}, stackTrace: {}", 
                     userId, timeRange, e.getMessage(), e.getStackTrace()[0].toString(), e);
            
            // Log detailed error information
            log.error("[OPTIMIZATION] Detailed error context - User ID: {}, TimeRange: {}, Error Type: {}, Error Message: {}", 
                     userId, timeRange, e.getClass().getSimpleName(), e.getMessage());
            
            // Return success with empty list to prevent frontend crashes
            log.info("[OPTIMIZATION] Returning empty recommendations list as fallback for user ID: {}", userId);
            return ResponseEntity.ok(ApiResponse.success("No high priority recommendations available", new ArrayList<>()));
        }
    }

    /**
     * Accept a recommendation (mark as accepted for implementation)
     */
    @PostMapping("/recommendations/{recommendationId}/accept")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<String>> acceptRecommendation(
            @PathVariable String recommendationId,
            @RequestBody(required = false) Map<String, Object> parameters,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("User ID: {} accepting recommendation: {}", userId, recommendationId);
            
            // In a real implementation, this would:
            // 1. Update recommendation status to ACCEPTED
            // 2. Schedule implementation if automated
            // 3. Log the acceptance for tracking
            // 4. Potentially trigger automated actions
            
            // For now, we'll simulate the acceptance
            log.info("Recommendation {} accepted by user {}", recommendationId, userId);
            
            return ResponseEntity.ok(ApiResponse.success("Recommendation accepted successfully", 
                "Recommendation " + recommendationId + " has been accepted and will be implemented."));
            
        } catch (Exception e) {
            log.error("Error accepting recommendation {}: {}", recommendationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to accept recommendation"));
        }
    }

    /**
     * Dismiss a recommendation
     */
    @PostMapping("/recommendations/{recommendationId}/dismiss")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<String>> dismissRecommendation(
            @PathVariable String recommendationId,
            @RequestBody(required = false) Map<String, String> reason,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            String dismissalReason = reason != null ? reason.get("reason") : "No reason provided";
            log.info("User ID: {} dismissing recommendation: {} with reason: {}", userId, recommendationId, dismissalReason);
            
            // In a real implementation, this would:
            // 1. Update recommendation status to DISMISSED
            // 2. Log the dismissal reason for learning
            // 3. Update recommendation engine rules if needed
            
            log.info("Recommendation {} dismissed by user {} - Reason: {}", recommendationId, userId, dismissalReason);
            
            return ResponseEntity.ok(ApiResponse.success("Recommendation dismissed successfully", 
                "Recommendation " + recommendationId + " has been dismissed."));
            
        } catch (Exception e) {
            log.error("Error dismissing recommendation {}: {}", recommendationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to dismiss recommendation"));
        }
    }

    /**
     * Schedule a recommendation for later implementation
     */
    @PostMapping("/recommendations/{recommendationId}/schedule")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<String>> scheduleRecommendation(
            @PathVariable String recommendationId,
            @RequestBody Map<String, Object> scheduleData,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            String scheduledTime = scheduleData.get("scheduledTime").toString();
            log.info("User ID: {} scheduling recommendation: {} for: {}", userId, recommendationId, scheduledTime);
            
            // In a real implementation, this would:
            // 1. Update recommendation status to SCHEDULED
            // 2. Create a scheduled task for implementation
            // 3. Set up notifications/reminders
            
            log.info("Recommendation {} scheduled by user {} for {}", recommendationId, userId, scheduledTime);
            
            return ResponseEntity.ok(ApiResponse.success("Recommendation scheduled successfully", 
                "Recommendation " + recommendationId + " has been scheduled for " + scheduledTime));
            
        } catch (Exception e) {
            log.error("Error scheduling recommendation {}: {}", recommendationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to schedule recommendation"));
        }
    }

    /**
     * Get recommendation implementation status
     */
    @GetMapping("/recommendations/{recommendationId}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationStatus(
            @PathVariable String recommendationId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching status for recommendation: {} by user: {}", recommendationId, userId);
            
            // In a real implementation, this would fetch actual status from database
            Map<String, Object> status = Map.of("recommendationId", recommendationId,
                "status", "PENDING",
                "createdDate", "2024-01-15T10:30:00",
                "lastUpdated", "2024-01-15T10:30:00",
                "implementationProgress", 0,
                "estimatedCompletion", "Not started"
            );
            return ResponseEntity.ok(ApiResponse.success("Recommendation status retrieved successfully", status));
            
        } catch (Exception e) {
            log.error("Error fetching recommendation status {}: {}", recommendationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch recommendation status"));
        }
    }

    /**
     * Get optimization summary statistics
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOptimizationSummary(
            @RequestParam(defaultValue = "30d") String timeRange,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching optimization summary for user ID: {} with time range: {}", userId, timeRange);
            OptimizationResponse recommendations = optimizationService.generateRecommendations(userId, timeRange);
            OptimizationSummary summary = recommendations.getSummary();
            
            Map<String, Object> summaryData = Map.of("totalRecommendations", summary.getTotalRecommendations(),
                "highPriorityCount", summary.getHighPriorityRecommendations(),
                "mediumPriorityCount", summary.getMediumPriorityRecommendations(),
                "lowPriorityCount", summary.getLowPriorityRecommendations(),
                "implementableCount", summary.getImplementableRecommendations(),
                "totalPotentialImpact", summary.getTotalPotentialImpact(),
                "averageConfidence", summary.getAverageConfidence(),
                "topCategory", summary.getTopRecommendationCategory(),
                "recommendationsByType", summary.getRecommendationsByType(),
                "generatedAt", recommendations.getGeneratedAt()
            );
            return ResponseEntity.ok(ApiResponse.success("Optimization summary retrieved successfully", summaryData));
            
        } catch (Exception e) {
            log.error("Error fetching optimization summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch optimization summary"));
        }
    }

    /**
     * Get available recommendation types and their descriptions
     */
    @GetMapping("/types")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationTypes() {
        
        try {
            Map<String, Object> types = Map.of("BUDGET_REALLOCATION", Map.of(
                    "name", "Budget Reallocation",
                    "description", "Optimize budget distribution across campaigns based on performance",
                    "category", "Budget Optimization"
                ),
                "AI_PROVIDER_SWITCH", Map.of(
                    "name", "AI Provider Switch",
                    "description", "Switch to better performing AI providers for content generation",
                    "category", "AI Optimization"
                ),
                "CAMPAIGN_OBJECTIVE_OPTIMIZATION", Map.of(
                    "name", "Campaign Objective Optimization",
                    "description", "Optimize campaign objectives based on performance patterns",
                    "category", "Campaign Strategy"
                ),
                "AD_SCHEDULING", Map.of(
                    "name", "Ad Scheduling",
                    "description", "Optimize ad delivery timing based on performance patterns",
                    "category", "Timing Optimization"
                ),
                "CONTENT_TYPE_OPTIMIZATION", Map.of(
                    "name", "Content Type Optimization",
                    "description", "Optimize content type mix for better engagement",
                    "category", "Content Strategy"
                ),
                "AD_CREATIVE_REFRESH", Map.of(
                    "name", "Ad Creative Refresh",
                    "description", "Refresh ad creatives to combat fatigue and improve performance",
                    "category", "Creative Optimization"
                )
            );
            return ResponseEntity.ok(ApiResponse.success("Recommendation types retrieved successfully", types));
            
        } catch (Exception e) {
            log.error("Error fetching recommendation types: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch recommendation types"));
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
