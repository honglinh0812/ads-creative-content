package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.dto.OptimizationResponse.*;
import com.fbadsautomation.dto.OptimizationResponse;
import com.fbadsautomation.service.OptimizationService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/optimization")
public class OptimizationController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationController.class);
    
    private final OptimizationService optimizationService;

    @Autowired
    public OptimizationController(OptimizationService optimizationService) {
        this.optimizationService = optimizationService;
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
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Fetching high priority recommendations for user ID: {} with time range: {}", userId, timeRange);
            List<Recommendation> recommendations = optimizationService.getHighPriorityRecommendations(userId, timeRange);
            return ResponseEntity.ok(ApiResponse.success("High priority recommendations retrieved successfully", recommendations));
            
        } catch (Exception e) {
            log.error("Error fetching high priority recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch high priority recommendations"));
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
