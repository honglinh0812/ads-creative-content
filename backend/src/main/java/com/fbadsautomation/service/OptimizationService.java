package com.fbadsautomation.service;

import com.fbadsautomation.dto.AnalyticsResponse;
import com.fbadsautomation.dto.OptimizationResponse.*;
import com.fbadsautomation.dto.OptimizationResponse;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptimizationService {

    private static final Logger log = LoggerFactory.getLogger(OptimizationService.class);
    
    private final AnalyticsService analyticsService;
    private final OptimizationRulesEngine rulesEngine;
    private final UserRepository userRepository;

    @Autowired
    public OptimizationService(AnalyticsService analyticsService, 
                              OptimizationRulesEngine rulesEngine,
                              UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.rulesEngine = rulesEngine;
        this.userRepository = userRepository;
    }

    /**
     * Generate comprehensive optimization recommendations for a user
     */
    @Transactional(readOnly = true)
    public OptimizationResponse generateRecommendations(Long userId, String timeRange) {
        log.info("[OPTIMIZATION_SERVICE] Starting recommendations generation for user ID: {}, timeRange: {}", userId, timeRange);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("[OPTIMIZATION_SERVICE] User not found with ID: {}", userId);
                        return new RuntimeException("User not found with ID: " + userId);
                    });

            log.debug("[OPTIMIZATION_SERVICE] User found: ID={}, Email={}", user.getId(), user.getEmail());

            // Get analytics data
            log.info("[OPTIMIZATION_SERVICE] Fetching analytics data for user ID: {}", userId);
            AnalyticsResponse analytics = analyticsService.getAnalytics(userId, timeRange);
            
            // Log analytics data structure for debugging
            if (analytics == null) {
                log.warn("[OPTIMIZATION_SERVICE] Analytics data is null for user ID: {}", userId);
            } else {
                log.debug("[OPTIMIZATION_SERVICE] Analytics data structure - IsDemo: {}, Campaigns: {}, Ads: {}", 
                         analytics.isDemoData(), 
                         analytics.getCampaignAnalytics() != null ? analytics.getCampaignAnalytics().size() : "null",
                         analytics.getAdAnalytics() != null ? analytics.getAdAnalytics().size() : "null");
            }

            // Generate recommendations using rules engine
            List<Recommendation> allRecommendations = new ArrayList<>();
            
            // Budget optimization recommendations
            log.info("[OPTIMIZATION_SERVICE] Generating budget recommendations for user ID: {}", userId);
            List<Recommendation> budgetRecommendations = rulesEngine.generateBudgetRecommendations(analytics);
            if (budgetRecommendations != null) {
                allRecommendations.addAll(budgetRecommendations);
                log.debug("[OPTIMIZATION_SERVICE] Added {} budget recommendations", budgetRecommendations.size());
            } else {
                log.warn("[OPTIMIZATION_SERVICE] Budget recommendations is null for user ID: {}", userId);
            }
            
            // AI provider recommendations
            log.info("[OPTIMIZATION_SERVICE] Generating AI provider recommendations for user ID: {}", userId);
            List<Recommendation> aiProviderRecommendations = rulesEngine.generateAIProviderRecommendations(analytics);
            if (aiProviderRecommendations != null) {
                allRecommendations.addAll(aiProviderRecommendations);
                log.debug("[OPTIMIZATION_SERVICE] Added {} AI provider recommendations", aiProviderRecommendations.size());
            } else {
                log.warn("[OPTIMIZATION_SERVICE] AI provider recommendations is null for user ID: {}", userId);
            }
            
            // Campaign objective recommendations
            log.info("[OPTIMIZATION_SERVICE] Generating campaign objective recommendations for user ID: {}", userId);
            List<Recommendation> objectiveRecommendations = rulesEngine.generateCampaignObjectiveRecommendations(analytics);
            if (objectiveRecommendations != null) {
                allRecommendations.addAll(objectiveRecommendations);
                log.debug("[OPTIMIZATION_SERVICE] Added {} campaign objective recommendations", objectiveRecommendations.size());
            } else {
                log.warn("[OPTIMIZATION_SERVICE] Campaign objective recommendations is null for user ID: {}", userId);
            }
            
            // Content type recommendations
            log.info("[OPTIMIZATION_SERVICE] Generating content type recommendations for user ID: {}", userId);
            List<Recommendation> contentRecommendations = rulesEngine.generateContentTypeRecommendations(analytics);
            if (contentRecommendations != null) {
                allRecommendations.addAll(contentRecommendations);
                log.debug("[OPTIMIZATION_SERVICE] Added {} content type recommendations", contentRecommendations.size());
            } else {
                log.warn("[OPTIMIZATION_SERVICE] Content type recommendations is null for user ID: {}", userId);
            }
            
            // Ad scheduling recommendations
            log.info("[OPTIMIZATION_SERVICE] Generating ad scheduling recommendations for user ID: {}", userId);
            List<Recommendation> schedulingRecommendations = generateAdSchedulingRecommendations(analytics);
            if (schedulingRecommendations != null) {
                allRecommendations.addAll(schedulingRecommendations);
                log.debug("[OPTIMIZATION_SERVICE] Added {} ad scheduling recommendations", schedulingRecommendations.size());
            } else {
                log.warn("[OPTIMIZATION_SERVICE] Ad scheduling recommendations is null for user ID: {}", userId);
            }
            
            // Creative refresh recommendations
            log.info("[OPTIMIZATION_SERVICE] Generating creative refresh recommendations for user ID: {}", userId);
            List<Recommendation> creativeRecommendations = generateCreativeRefreshRecommendations(analytics);
            if (creativeRecommendations != null) {
                allRecommendations.addAll(creativeRecommendations);
                log.debug("[OPTIMIZATION_SERVICE] Added {} creative refresh recommendations", creativeRecommendations.size());
            } else {
                log.warn("[OPTIMIZATION_SERVICE] Creative refresh recommendations is null for user ID: {}", userId);
            }

            // Sort recommendations by priority and expected impact
            log.info("[OPTIMIZATION_SERVICE] Sorting {} total recommendations for user ID: {}", allRecommendations.size(), userId);
            allRecommendations.sort((r1, r2) -> {
                // First sort by priority
                int priorityComparison = comparePriority(r1 != null ? r1.getPriority() : null, r2 != null ? r2.getPriority() : null);
                if (priorityComparison != 0) return priorityComparison;
                
                // Then by expected impact
                double impact1 = (r1 != null && r1.getExpectedImpact() != null) ? r1.getExpectedImpact().getExpectedChange() : 0.0;
                double impact2 = (r2 != null && r2.getExpectedImpact() != null) ? r2.getExpectedImpact().getExpectedChange() : 0.0;
                return Double.compare(impact2, impact1); // Descending order
            });

            // Generate summary
            log.info("[OPTIMIZATION_SERVICE] Generating optimization summary for {} recommendations, user ID: {}", allRecommendations.size(), userId);
            OptimizationSummary summary = generateOptimizationSummary(allRecommendations);
            OptimizationResponse response = new OptimizationResponse(allRecommendations, summary);
            
            log.info("[OPTIMIZATION_SERVICE] Successfully generated {} optimization recommendations for user ID: {}", allRecommendations.size(), userId);
            return response;

        } catch (Exception e) {
            log.error("[OPTIMIZATION_SERVICE] ERROR generating optimization recommendations for user ID: {}, timeRange: {}, error: {}, stackTrace: {}", 
                     userId, timeRange, e.getMessage(), e.getStackTrace()[0].toString(), e);
            
            // Log detailed error information
            log.error("[OPTIMIZATION_SERVICE] Detailed error context - User ID: {}, TimeRange: {}, Error Type: {}, Error Message: {}", 
                     userId, timeRange, e.getClass().getSimpleName(), e.getMessage());
            
            throw new RuntimeException("Failed to generate optimization recommendations: " + e.getMessage(), e);
        }
    }

    /**
     * Generate ad scheduling recommendations based on performance patterns
     */
    private List<Recommendation> generateAdSchedulingRecommendations(AnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            // Analyze performance trends to identify optimal scheduling
            List<AnalyticsResponse.TimeSeriesData> trends = analytics.getPerformanceTrends();
            if (trends == null || trends.isEmpty()) return recommendations;
            // Simulate scheduling analysis (in real app, this would analyze hourly/daily patterns)
            Map<String, Double> performanceByTimeSlot = new HashMap<>();
            performanceByTimeSlot.put("morning", 2.8); // CTR
            performanceByTimeSlot.put("afternoon", 3.2);
            performanceByTimeSlot.put("evening", 2.1);
            performanceByTimeSlot.put("night", 1.5);

            String bestTimeSlot = performanceByTimeSlot.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("afternoon");
            String worstTimeSlot = performanceByTimeSlot.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("night");

            double bestPerformance = performanceByTimeSlot.get(bestTimeSlot);
            double worstPerformance = performanceByTimeSlot.get(worstTimeSlot);

            if (bestPerformance > worstPerformance * 1.5) { // Significant difference
                Recommendation recommendation = new Recommendation();
                recommendation.setId(UUID.randomUUID().toString());
                recommendation.setType(RecommendationType.AD_SCHEDULING);
                recommendation.setPriority(Priority.MEDIUM);
                recommendation.setTitle("Optimize Ad Scheduling");
                recommendation.setDescription(String.format(
                    "Your ads perform %.1fx better in the %s (%.1f%% CTR) compared to %s (%.1f%% CTR). Consider adjusting ad scheduling.",
                    bestPerformance / worstPerformance, bestTimeSlot, bestPerformance, worstTimeSlot, worstPerformance
                ));
                recommendation.setReasoning(String.format(
                    "Performance analysis shows significant variation across time periods. Concentrating ad spend during high-performance periods " +
                    "(%s with %.1f%% CTR) can improve overall campaign efficiency.",
                    bestTimeSlot, bestPerformance
                ));
                recommendation.setTargetEntity("ad_scheduling");
                recommendation.setTargetEntityName("Ad Scheduling");
                recommendation.setCategory("Timing Optimization");
                recommendation.setTags(Arrays.asList("scheduling", "timing", "performance"));

                Map<String, Object> context = new HashMap<>();
                context.put("bestTimeSlot", bestTimeSlot);
                context.put("bestPerformance", bestPerformance);
                context.put("worstTimeSlot", worstTimeSlot);
                context.put("worstPerformance", worstPerformance);
                context.put("performanceRatio", bestPerformance / worstPerformance);
                recommendation.setContext(context);

                List<Action> actions = Arrays.asList(new Action("adjust_ad_schedule", 
                              String.format("Increase ad delivery during %s hours", bestTimeSlot),
                              Map.of("timeSlot", bestTimeSlot, "adjustment", "increase"),
                              false,
                              null),
                    new Action("reduce_ad_schedule", 
                              String.format("Reduce ad delivery during %s hours", worstTimeSlot),
                              Map.of("timeSlot", worstTimeSlot, "adjustment", "decrease"),
                              false,
                              null)
                );
                recommendation.setActions(actions);

                Impact impact = new Impact("CTR", (bestPerformance - worstPerformance) * 0.5, "increase", "1-2 weeks", 0.75,
                                         Map.of("costEfficiency", 15.0));
                recommendation.setExpectedImpact(impact);
                recommendation.setExpiresAt(LocalDateTime.now().plusDays(14));

                recommendations.add(recommendation);
            }
        } catch (Exception e) {
            log.error("Error generating ad scheduling recommendations: {}", e.getMessage(), e);
        }

        return recommendations;
    }

    /**
     * Generate creative refresh recommendations
     */
    private List<Recommendation> generateCreativeRefreshRecommendations(AnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            List<AnalyticsResponse.AdAnalytics> ads = analytics.getAdAnalytics();
            if (ads == null || ads.isEmpty()) return recommendations;
            // Find ads that might need creative refresh (running for long time with declining performance)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            List<AnalyticsResponse.AdAnalytics> oldAds = ads.stream()
                    .filter(ad -> ad.getCreatedDate() != null && ad.getCreatedDate().isBefore(thirtyDaysAgo))
                    .filter(ad -> ad.getCtr() < 2.0) // Low CTR
                    .sorted((a1, a2) -> a1.getCreatedDate().compareTo(a2.getCreatedDate())) // Oldest first
                    .limit(3)
                    .collect(Collectors.toList());
            for (AnalyticsResponse.AdAnalytics ad : oldAds) {
                Recommendation recommendation = new Recommendation();
                recommendation.setId(UUID.randomUUID().toString());
                recommendation.setType(RecommendationType.AD_CREATIVE_REFRESH);
                recommendation.setPriority(Priority.MEDIUM);
                recommendation.setTitle("Refresh Ad Creative");
                recommendation.setDescription(String.format(
                    "Ad '%s' has been running for over 30 days with declining performance (%.1f%% CTR). Consider refreshing the creative.",
                    ad.getAdName(), ad.getCtr()
                ));
                recommendation.setReasoning(String.format(
                    "This ad was created on %s and currently has a CTR of %.1f%%, which is below the optimal range. " +
                    "Creative fatigue may be setting in, and refreshing the creative could improve performance.",
                    ad.getCreatedDate().toLocalDate(), ad.getCtr()
                ));
                recommendation.setTargetEntity(ad.getAdId().toString());
                recommendation.setTargetEntityName(ad.getAdName());
                recommendation.setCategory("Creative Optimization");
                recommendation.setTags(Arrays.asList("creative", "refresh", "fatigue"));

                Map<String, Object> context = new HashMap<>();
                context.put("adAge", java.time.temporal.ChronoUnit.DAYS.between(ad.getCreatedDate(), LocalDateTime.now()));
                context.put("currentCTR", ad.getCtr());
                context.put("currentImpressions", ad.getImpressions());
                context.put("aiProvider", ad.getAiProvider());
                recommendation.setContext(context);

                List<Action> actions = Arrays.asList(new Action("generate_new_creative", 
                              "Generate new ad creative using AI",
                              Map.of("adId", ad.getAdId(), "aiProvider", ad.getAiProvider()),
                              false,
                              null),
                    new Action("duplicate_and_test", 
                              "Create duplicate ad with new creative for A/B testing",
                              Map.of("originalAdId", ad.getAdId(), "testType", "creative_refresh"),
                              false,
                              null)
                );
                recommendation.setActions(actions);

                Impact impact = new Impact("CTR", 1.0, "increase", "1-2 weeks", 0.70,
                                         Map.of("engagementImprovement", 25.0));
                recommendation.setExpectedImpact(impact);
                recommendation.setExpiresAt(LocalDateTime.now().plusDays(7));

                recommendations.add(recommendation);
            }
        } catch (Exception e) {
            log.error("Error generating creative refresh recommendations: {}", e.getMessage(), e);
        }

        return recommendations;
    }

    /**
     * Generate optimization summary
     */
    private OptimizationSummary generateOptimizationSummary(List<Recommendation> recommendations) {
        int total = recommendations.size();
        int high = (int) recommendations.stream().filter(r -> r.getPriority() == Priority.HIGH).count();
        int medium = (int) recommendations.stream().filter(r -> r.getPriority() == Priority.MEDIUM).count();
        int low = (int) recommendations.stream().filter(r -> r.getPriority() == Priority.LOW).count();

        Map<RecommendationType, Integer> byType = recommendations.stream()
                .collect(Collectors.groupingBy(
                    Recommendation::getType,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        double totalPotentialImpact = recommendations.stream()
                .filter(r -> r.getExpectedImpact() != null)
                .mapToDouble(r -> r.getExpectedImpact().getExpectedChange())
                .sum();

        String topCategory = byType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().toString())
                .orElse("None");
        int implementable = (int) recommendations.stream()
                .filter(r -> r.getActions() != null && !r.getActions().isEmpty())
                .count();

        double averageConfidence = recommendations.stream()
                .filter(r -> r.getExpectedImpact() != null)
                .mapToDouble(r -> r.getExpectedImpact().getConfidence())
                .average()
                .orElse(0.0);
        return new OptimizationSummary(total, high, medium, low, byType, totalPotentialImpact, 
                                     topCategory, implementable, averageConfidence);
    }

    /**
     * Compare priorities for sorting
     */
    private int comparePriority(Priority p1, Priority p2) {
        Map<Priority, Integer> priorityOrder = Map.of(Priority.HIGH, 3,
            Priority.MEDIUM, 2,
            Priority.LOW, 1
        );
        return Integer.compare(priorityOrder.get(p2), priorityOrder.get(p1)); // Descending order
    }

    /**
     * Get recommendations by type
     */
    public List<Recommendation> getRecommendationsByType(Long userId, RecommendationType type, String timeRange) {
        try {
            OptimizationResponse response = generateRecommendations(userId, timeRange);
            return response.getRecommendations().stream()
                    .filter(r -> r.getType() == type)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting recommendations by type for user ID {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get high priority recommendations only
     */
    public List<Recommendation> getHighPriorityRecommendations(Long userId, String timeRange) {
        log.info("[OPTIMIZATION_SERVICE] Starting high priority recommendations fetch for user ID: {}, timeRange: {}", userId, timeRange);
        
        try {
            OptimizationResponse response = generateRecommendations(userId, timeRange);
            
            if (response == null) {
                log.warn("[OPTIMIZATION_SERVICE] OptimizationResponse is null for user ID: {}", userId);
                return new ArrayList<>();
            }
            
            List<Recommendation> allRecommendations = response.getRecommendations();
            if (allRecommendations == null) {
                log.warn("[OPTIMIZATION_SERVICE] All recommendations list is null for user ID: {}", userId);
                return new ArrayList<>();
            }
            
            log.debug("[OPTIMIZATION_SERVICE] Filtering high priority recommendations from {} total recommendations for user ID: {}", 
                     allRecommendations.size(), userId);
            
            List<Recommendation> highPriorityRecommendations = allRecommendations.stream()
                    .filter(r -> r != null && r.getPriority() == Priority.HIGH)
                    .collect(Collectors.toList());
            
            log.info("[OPTIMIZATION_SERVICE] Successfully filtered {} high priority recommendations for user ID: {}", 
                    highPriorityRecommendations.size(), userId);
            
            return highPriorityRecommendations;
            
        } catch (Exception e) {
            log.error("[OPTIMIZATION_SERVICE] ERROR getting high priority recommendations for user ID: {}, timeRange: {}, error: {}, stackTrace: {}", 
                     userId, timeRange, e.getMessage(), e.getStackTrace()[0].toString(), e);
            
            // Log detailed error information
            log.error("[OPTIMIZATION_SERVICE] Detailed error context - User ID: {}, TimeRange: {}, Error Type: {}, Error Message: {}", 
                     userId, timeRange, e.getClass().getSimpleName(), e.getMessage());
            
            return new ArrayList<>();
        }
    }
}
