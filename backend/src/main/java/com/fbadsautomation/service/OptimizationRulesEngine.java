package com.fbadsautomation.service;

import com.fbadsautomation.dto.AnalyticsResponse;
import com.fbadsautomation.dto.OptimizationResponse.*;
import com.fbadsautomation.dto.OptimizationResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service

public class OptimizationRulesEngine {

    private static final Logger log = LoggerFactory.getLogger(OptimizationRulesEngine.class);

    // Configuration thresholds
    private static final double LOW_ROI_THRESHOLD = 10.0;
    private static final double HIGH_ROI_THRESHOLD = 50.0;
    private static final double LOW_CTR_THRESHOLD = 1.5;
    private static final double HIGH_CTR_THRESHOLD = 4.0;
    private static final double LOW_BUDGET_UTILIZATION_THRESHOLD = 50.0;
    private static final double HIGH_BUDGET_UTILIZATION_THRESHOLD = 90.0;
    private static final double LOW_SELECTION_RATE_THRESHOLD = 40.0;
    private static final double HIGH_CONVERSION_RATE_THRESHOLD = 10.0;
    private static final double LOW_CONVERSION_RATE_THRESHOLD = 2.0;
    private static final double HIGH_SELECTION_RATE_THRESHOLD = 70.0;
     /* Generate budget reallocation recommendations
     */
    public List<Recommendation> generateBudgetRecommendations(AnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            List<AnalyticsResponse.CampaignAnalytics> campaigns = analytics.getCampaignAnalytics();
            if (campaigns == null || campaigns.isEmpty()) return recommendations; // Find high and low performing campaigns
            List<AnalyticsResponse.CampaignAnalytics> highPerformers = campaigns.stream()
                    .filter(c -> c.getRoi() > HIGH_ROI_THRESHOLD && c.getBudgetUtilization() > HIGH_BUDGET_UTILIZATION_THRESHOLD)
                    .sorted((c1, c2) -> Double.compare(c2.getRoi(), c1.getRoi()))
                    .collect(Collectors.toList());
                    List<AnalyticsResponse.CampaignAnalytics> lowPerformers = campaigns.stream()
                    .filter(c -> c.getRoi() < LOW_ROI_THRESHOLD || c.getBudgetUtilization() < LOW_BUDGET_UTILIZATION_THRESHOLD)
                    .sorted((c1, c2) -> Double.compare(c1.getRoi(), c2.getRoi()))
                    .collect(Collectors.toList()); // Generate recommendations for high performers
            for (AnalyticsResponse.CampaignAnalytics campaign : highPerformers.stream().limit(3).collect(Collectors.toList())) {
                double suggestedIncrease = Math.min(campaign.getBudget() * 0.5, 1000.0); // Max 50% increase or $1000Recommendation recommendation = new Recommendation();
                Recommendation recommendation = new Recommendation();
                recommendation.setId(UUID.randomUUID().toString());
                recommendation.setType(RecommendationType.BUDGET_REALLOCATION);
                recommendation.setPriority(Priority.HIGH);
                recommendation.setTitle("Increase Budget for High-Performing Campaign");
                recommendation.setDescription(String.format(
                    "Campaign '%s' has excellent ROI (%.1f%%) and high budget utilization (%.1f%%). Consider increasing budget by $%.0f.",
                    campaign.getCampaignName(), campaign.getRoi(), campaign.getBudgetUtilization(), suggestedIncrease
                ));
                recommendation.setReasoning(String.format(
                    "This campaign demonstrates strong performance with ROI of %.1f%% (above %.1f%% threshold) and budget utilization of %.1f%%. " +
                    "Increasing budget could scale successful performance and drive additional conversions.",
                    campaign.getRoi(), HIGH_ROI_THRESHOLD, campaign.getBudgetUtilization()
                ));
                recommendation.setTargetEntity(campaign.getCampaignId().toString());
                recommendation.setTargetEntityName(campaign.getCampaignName());
                recommendation.setCategory("Budget Optimization");
                recommendation.setTags(Arrays.asList("budget", "scale", "high-roi")); // Set context
                Map<String, Object> context = new HashMap<>();
                context.put("currentBudget", campaign.getBudget());
                context.put("currentROI", campaign.getRoi());
                context.put("currentUtilization", campaign.getBudgetUtilization());
                context.put("suggestedBudget", campaign.getBudget() + suggestedIncrease);
                recommendation.setContext(context);

                // Set actions
                List<Action> actions = Arrays.asList(
                    new Action("increase_budget", 
                              String.format("Increase daily budget to $%.0f", campaign.getBudget() + suggestedIncrease),
                              Map.of("campaignId", campaign.getCampaignId(), "newBudget", campaign.getBudget() + suggestedIncrease),
                              true,
                              String.format("Are you sure you want to increase the budget for '%s' by $%.0f?", 
                                          campaign.getCampaignName(), suggestedIncrease))
                );
                recommendation.setActions(actions);

                // Set expected impact
                double expectedROIIncrease = Math.min(campaign.getRoi() * 0.1, 5.0); // Up to 10% ROI improvement or 5%
                Impact impact = new Impact("ROI", expectedROIIncrease, "increase", "7-14 days", 0.85,
                                         Map.of("revenue", suggestedIncrease * (campaign.getRoi() / 100)));
                recommendation.setExpectedImpact(impact);
                recommendation.setExpiresAt(LocalDateTime.now().plusDays(7));

                recommendations.add(recommendation);
            }

            // Generate recommendations for low performers
            for (AnalyticsResponse.CampaignAnalytics campaign : lowPerformers.stream().limit(2).collect(Collectors.toList())) {
                if (campaign.getRoi() < 0) {
                    // Suggest pausing negative ROI campaigns
                    Recommendation recommendation = new Recommendation();
                    recommendation.setId(UUID.randomUUID().toString());
                    recommendation.setType(RecommendationType.CAMPAIGN_PAUSE);
                    recommendation.setPriority(Priority.HIGH);
                    recommendation.setTitle("Pause Underperforming Campaign");
                    recommendation.setDescription(String.format(
                        "Campaign '%s' has negative ROI (%.1f%%) and is losing money. Consider pausing to prevent further losses.",
                        campaign.getCampaignName(), campaign.getRoi()
                    ));
                    recommendation.setReasoning(String.format(
                        "This campaign has a negative ROI of %.1f%%, meaning it's spending more than it's generating in revenue. " +
                        "Pausing will prevent further losses while you optimize the campaign strategy.",
                        campaign.getRoi()
                    ));
                    recommendation.setTargetEntity(campaign.getCampaignId().toString());
                    recommendation.setTargetEntityName(campaign.getCampaignName());
                    recommendation.setCategory("Risk Management");
                    recommendation.setTags(Arrays.asList("pause", "negative-roi", "loss-prevention"));

                    Map<String, Object> context = new HashMap<>();
                    context.put("currentROI", campaign.getRoi());
                    context.put("dailyLoss", campaign.getSpent() * (Math.abs(campaign.getRoi()) / 100));
                    recommendation.setContext(context);

                    List<Action> actions = Arrays.asList(
                        new Action("pause_campaign", 
                                  "Pause campaign to prevent further losses",
                                  Map.of("campaignId", campaign.getCampaignId()),
                                  true,
                                  String.format("Are you sure you want to pause '%s'? This will stop all ads in this campaign.", 
                                              campaign.getCampaignName()))
                    );
                    recommendation.setActions(actions);

                    Impact impact = new Impact("Cost Savings", Math.abs(campaign.getRoi()), "decrease", "Immediate", 0.95,
                                             Map.of("dailySavings", campaign.getSpent()));
                    recommendation.setExpectedImpact(impact);
                    recommendation.setExpiresAt(LocalDateTime.now().plusDays(3));

                    recommendations.add(recommendation);
                } else if (campaign.getBudgetUtilization() < LOW_BUDGET_UTILIZATION_THRESHOLD) {
                    // Suggest reducing budget for underutilized campaigns
                    double suggestedDecrease = campaign.getBudget() * 0.3; // 30% decrease
                    
                    Recommendation recommendation = new Recommendation();
                    recommendation.setId(UUID.randomUUID().toString());
                    recommendation.setType(RecommendationType.BUDGET_REALLOCATION);
                    recommendation.setPriority(Priority.MEDIUM);
                    recommendation.setTitle("Reduce Budget for Underutilized Campaign");
                    recommendation.setDescription(String.format(
                        "Campaign '%s' has low budget utilization (%.1f%%). Consider reducing budget by $%.0f to optimize allocation.",
                        campaign.getCampaignName(), campaign.getBudgetUtilization(), suggestedDecrease
                    ));
                    recommendation.setReasoning(String.format(
                        "This campaign is only utilizing %.1f%% of its budget, indicating limited audience reach or poor ad performance. " +
                        "Reducing budget allows reallocation to better-performing campaigns.",
                        campaign.getBudgetUtilization()
                    ));
                    recommendation.setTargetEntity(campaign.getCampaignId().toString());
                    recommendation.setTargetEntityName(campaign.getCampaignName());
                    recommendation.setCategory("Budget Optimization");
                    recommendation.setTags(Arrays.asList("budget", "underutilized", "reallocation"));

                    Map<String, Object> context = new HashMap<>();
                    context.put("currentBudget", campaign.getBudget());
                    context.put("currentUtilization", campaign.getBudgetUtilization());
                    context.put("suggestedBudget", campaign.getBudget() - suggestedDecrease);
                    recommendation.setContext(context);

                    List<Action> actions = Arrays.asList(
                        new Action("decrease_budget", 
                                  String.format("Reduce daily budget to $%.0f", campaign.getBudget() - suggestedDecrease),
                                  Map.of("campaignId", campaign.getCampaignId(), "newBudget", campaign.getBudget() - suggestedDecrease),
                                  true,
                                  String.format("Are you sure you want to reduce the budget for '%s' by $%.0f?", 
                                              campaign.getCampaignName(), suggestedDecrease))
                    );
                    recommendation.setActions(actions);

                    Impact impact = new Impact("Budget Efficiency", 30.0, "optimize", "3-7 days", 0.75,
                                             Map.of("budgetSavings", suggestedDecrease));
                    recommendation.setExpectedImpact(impact);
                    recommendation.setExpiresAt(LocalDateTime.now().plusDays(5));

                    recommendations.add(recommendation);
                };
    }

        } catch (Exception e) {
            log.error("Error generating budget recommendations: {}", e.getMessage(), e);
        }

        return recommendations;
    }

    /**
     * Generate AI provider switching recommendations
     */
    public List<Recommendation> generateAIProviderRecommendations(AnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            AnalyticsResponse.AIProviderAnalytics aiAnalytics = analytics.getAiProviderAnalytics();
            if (aiAnalytics == null || aiAnalytics.getProviderMetrics() == null) return recommendations;

            Map<String, AnalyticsResponse.AIProviderAnalytics.ProviderMetrics> providers = aiAnalytics.getProviderMetrics();

            // Find best and worst performing providers
            Optional<Map.Entry<String, AnalyticsResponse.AIProviderAnalytics.ProviderMetrics>> bestProvider = 
                providers.entrySet().stream()
                    .max(Map.Entry.comparingByValue((m1, m2) -> Double.compare(m1.getSelectionRate(), m2.getSelectionRate())));

            List<Map.Entry<String, AnalyticsResponse.AIProviderAnalytics.ProviderMetrics>> poorProviders = 
                providers.entrySet().stream()
                    .filter(entry -> entry.getValue().getSelectionRate() < LOW_SELECTION_RATE_THRESHOLD)
                    .sorted(Map.Entry.comparingByValue((m1, m2) -> Double.compare(m1.getSelectionRate(), m2.getSelectionRate())))
                    .collect(Collectors.toList());

            // Generate recommendations for poor performing providers
            for (Map.Entry<String, AnalyticsResponse.AIProviderAnalytics.ProviderMetrics> poorProvider : poorProviders.stream().limit(1).collect(Collectors.toList())) {
                String poorProviderName = poorProvider.getKey();
                String bestProviderName = bestProvider.get().getKey();
                double poorSelectionRate = poorProvider.getValue().getSelectionRate();
                double bestSelectionRate = bestProvider.get().getValue().getSelectionRate();
                
                if (bestProvider.isPresent() && bestProvider.get().getValue().getSelectionRate() - poorSelectionRate > 20.0) { // Significant difference
                    Recommendation recommendation = new Recommendation();
                    recommendation.setId(UUID.randomUUID().toString());
                    recommendation.setType(RecommendationType.AI_PROVIDER_SWITCH);
                    recommendation.setPriority(Priority.MEDIUM);
                    recommendation.setTitle("Switch to Better Performing AI Provider");
                    recommendation.setDescription(String.format(
                        "Consider switching from %s (%.1f%% selection rate) to %s (%.1f%% selection rate) for better content performance.",
                        formatProviderName(poorProviderName), poorSelectionRate,
                        formatProviderName(bestProviderName), bestSelectionRate
                    ));
                    recommendation.setReasoning(String.format(
                            "%s has a significantly higher content selection rate (%.1f%%) compared to %s (%.1f%%). " +
                            "Switching could improve content quality and campaign performance.",
                            formatProviderName(bestProviderName), bestSelectionRate,
                            formatProviderName(poorProviderName), poorSelectionRate
                        ));
                    recommendation.setTargetEntity(poorProviderName);
                    recommendation.setTargetEntityName(formatProviderName(poorProviderName));
                    recommendation.setCategory("AI Optimization");
                    recommendation.setTags(Arrays.asList("ai-provider", "content-quality", "performance"));

                    Map<String, Object> context = new HashMap<>();
                    context.put("currentProvider", poorProviderName);
                    context.put("currentSelectionRate", poorSelectionRate);
                    context.put("recommendedProvider", bestProviderName);
                    context.put("recommendedSelectionRate", bestSelectionRate);
                    context.put("improvementPotential", bestSelectionRate - poorSelectionRate);
                    recommendation.setContext(context);

                    List<Action> actions = Arrays.asList(
                        new Action("switch_ai_provider", 
                                  String.format("Switch default AI provider to %s", formatProviderName(bestProviderName)),
                                  Map.of("fromProvider", poorProviderName, "toProvider", bestProviderName),
                                  false,
                                  null)
                    );
                    recommendation.setActions(actions);

                        Impact impact = new Impact("Content Selection Rate", bestSelectionRate - poorSelectionRate, 
                                                 "increase", "1-2 weeks", 0.80,
                                                 Map.of("qualityImprovement", (bestSelectionRate - poorSelectionRate) / poorSelectionRate * 100));
                        recommendation.setExpectedImpact(impact);
                        recommendation.setExpiresAt(LocalDateTime.now().plusDays(14));

                        recommendations.add(recommendation);
                    };
    }
            }

        catch (Exception e) {
            log.error("Error generating AI provider recommendations: {}", e.getMessage(), e);
        }

        return recommendations;
    }

    /**
     * Generate campaign objective optimization recommendations
     */
    public List<Recommendation> generateCampaignObjectiveRecommendations(AnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            List<AnalyticsResponse.CampaignAnalytics> campaigns = analytics.getCampaignAnalytics();
            if (campaigns == null || campaigns.isEmpty()) return recommendations;

            for (AnalyticsResponse.CampaignAnalytics campaign : campaigns) {
                // Analyze campaign performance vs objective
                if ("TRAFFIC".equals(campaign.getObjective()) && campaign.getConversionRate() > HIGH_CONVERSION_RATE_THRESHOLD) {
                    // High converting traffic campaign should switch to conversions
                    Recommendation recommendation = new Recommendation();
                    recommendation.setId(UUID.randomUUID().toString());
                    recommendation.setType(RecommendationType.CAMPAIGN_OBJECTIVE_OPTIMIZATION);
                    recommendation.setPriority(Priority.MEDIUM);
                    recommendation.setTitle("Switch to Conversion Objective");
                    recommendation.setDescription(String.format(
                        "Campaign '%s' has high conversion rate (%.1f%%) with traffic objective. Consider switching to conversion objective for better optimization.",
                        campaign.getCampaignName(), campaign.getConversionRate()
                    ));
                    recommendation.setReasoning(String.format(
                        "This campaign achieves %.1f%% conversion rate, which is above the %.1f%% threshold. " +
                        "Switching to conversion objective will allow Facebook's algorithm to optimize for conversions rather than just traffic.",
                        campaign.getConversionRate(), HIGH_CONVERSION_RATE_THRESHOLD
                    ));
                    recommendation.setTargetEntity(campaign.getCampaignId().toString());
                    recommendation.setTargetEntityName(campaign.getCampaignName());
                    recommendation.setCategory("Campaign Strategy");
                    recommendation.setTags(Arrays.asList("objective", "conversions", "optimization"));

                    Map<String, Object> context = new HashMap<>();
                    context.put("currentObjective", campaign.getObjective());
                    context.put("currentConversionRate", campaign.getConversionRate());
                    context.put("recommendedObjective", "CONVERSIONS");
                    recommendation.setContext(context);

                    List<Action> actions = Arrays.asList(
                        new Action("change_objective", 
                                  "Change campaign objective to Conversions",
                                  Map.of("campaignId", campaign.getCampaignId(), "newObjective", "CONVERSIONS"),
                                  true,
                                  String.format("Changing objective will reset the campaign's learning phase. Continue with '%s'?", 
                                              campaign.getCampaignName()))
                    );
                    recommendation.setActions(actions);

                    Impact impact = new Impact("Conversion Rate", 15.0, "increase", "2-3 weeks", 0.70,
                                             Map.of("costPerConversion", -10.0));
                    recommendation.setExpectedImpact(impact);
                    recommendation.setExpiresAt(LocalDateTime.now().plusDays(10));

                    recommendations.add(recommendation);
                }
                
                else if ("CONVERSIONS".equals(campaign.getObjective()) && campaign.getConversionRate() < LOW_CONVERSION_RATE_THRESHOLD && campaign.getCtr() > HIGH_CTR_THRESHOLD) {
                    // Low converting but high CTR conversion campaign might work better with traffic objective
                    Recommendation recommendation = new Recommendation();
                    recommendation.setId(UUID.randomUUID().toString());
                    recommendation.setType(RecommendationType.CAMPAIGN_OBJECTIVE_OPTIMIZATION);
                    recommendation.setPriority(Priority.LOW);
                    recommendation.setTitle("Consider Traffic Objective");
                    recommendation.setDescription(String.format(
                        "Campaign '%s' has low conversion rate (%.1f%%) but high CTR (%.1f%%). Consider testing traffic objective.",
                        campaign.getCampaignName(), campaign.getConversionRate(), campaign.getCtr()
                    ));
                    recommendation.setReasoning(String.format(
                        "While this campaign has good engagement (%.1f%% CTR), the conversion rate is low (%.1f%%). " +
                        "Testing traffic objective might improve overall performance and reduce costs.",
                        campaign.getCtr(), campaign.getConversionRate()
                    ));
                    recommendation.setTargetEntity(campaign.getCampaignId().toString());
                    recommendation.setTargetEntityName(campaign.getCampaignName());
                    recommendation.setCategory("Campaign Strategy");
                    recommendation.setTags(Arrays.asList("objective", "traffic", "testing"));

                    Map<String, Object> context = new HashMap<>();
                    context.put("currentObjective", campaign.getObjective());
                    context.put("currentConversionRate", campaign.getConversionRate());
                    context.put("currentCTR", campaign.getCtr());
                    context.put("recommendedObjective", "TRAFFIC");
                    recommendation.setContext(context);

                    List<Action> actions = Arrays.asList(
                        new Action("test_objective", 
                                  "Create duplicate campaign with Traffic objective for testing",
                                  Map.of("campaignId", campaign.getCampaignId(), "testObjective", "TRAFFIC"),
                                  false,
                                  null)
                    );
                    recommendation.setActions(actions);

                    Impact impact = new Impact("Cost Per Click", -20.0, "decrease", "1-2 weeks", 0.60,
                                             Map.of("trafficIncrease", 25.0));
                    recommendation.setExpectedImpact(impact);
                    recommendation.setExpiresAt(LocalDateTime.now().plusDays(14));

                    recommendations.add(recommendation);
                };
    }

        } catch (Exception e) {
            log.error("Error generating campaign objective recommendations: {}", e.getMessage(), e);
        }

        return recommendations; }

    /**
     * Generate content type optimization recommendations
     */
    public List<Recommendation> generateContentTypeRecommendations(AnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        try {
            AnalyticsResponse.ContentAnalytics contentAnalytics = analytics.getContentAnalytics();
            if (contentAnalytics == null || contentAnalytics.getContentTypeDistribution() == null) return recommendations;
            Map<String, Integer> contentTypes = contentAnalytics.getContentTypeDistribution();
            Optional<Map.Entry<String, Integer>> mostUsedType = contentTypes.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            Optional<Map.Entry<String, Integer>> leastUsedType = contentTypes.entrySet().stream()
                    .min(Map.Entry.comparingByValue());
            if (mostUsedType.isPresent() && leastUsedType.isPresent()) {
                String mostUsed = mostUsedType.get().getKey();
                String leastUsed = leastUsedType.get().getKey();
                int mostUsedCount = mostUsedType.get().getValue();
                int leastUsedCount = leastUsedType.get().getValue();
        // If there's significant imbalance, recommend diversification
                if (mostUsedCount > leastUsedCount * 3 && contentAnalytics.getSelectionRate() <HIGH_SELECTION_RATE_THRESHOLD) {
                    Recommendation recommendation = new Recommendation();
                    recommendation.setId(UUID.randomUUID().toString());
                    recommendation.setType(RecommendationType.CONTENT_TYPE_OPTIMIZATION);
                    recommendation.setPriority(Priority.LOW);
                    recommendation.setTitle("Diversify Content Types");
                    recommendation.setDescription(String.format(
                        "You're heavily using %s content (%d pieces) but underutilizing %s (%d pieces). Consider diversifying for better performance.",
                        formatContentType(mostUsed), mostUsedCount, formatContentType(leastUsed), leastUsedCount
                    ));
                    recommendation.setReasoning(String.format(
                        "Content diversity can improve audience engagement and selection rates. Your current selection rate is %.1f%%, " +
                        "and diversifying content types might help reach the target of %.1f%%.",
                        contentAnalytics.getSelectionRate(), HIGH_SELECTION_RATE_THRESHOLD
                    ));
                    recommendation.setTargetEntity("content_strategy");
                    recommendation.setTargetEntityName("Content Strategy");
                    recommendation.setCategory("Content Optimization");
                    recommendation.setTags(Arrays.asList("content-type", "diversification", "strategy"));

                    Map<String, Object> context = new HashMap<>();
                    context.put("mostUsedType", mostUsed);
                    context.put("mostUsedCount", mostUsedCount);
                    context.put("leastUsedType", leastUsed);
                    context.put("leastUsedCount", leastUsedCount);
                    context.put("currentSelectionRate", contentAnalytics.getSelectionRate());
                    recommendation.setContext(context);

                    List<Action> actions = Arrays.asList(
                        new Action("generate_diverse_content", 
                                  String.format("Generate more %s content to balance portfolio", formatContentType(leastUsed)),
                                  Map.of("contentType", leastUsed, "targetCount", mostUsedCount / 2),
                                  false,
                                  null)
                    );
                    recommendation.setActions(actions);

                    Impact impact = new Impact("Content Selection Rate", 10.0, "increase", "2-4 weeks", 0.65,
                                             Map.of("contentDiversity", 25.0));
                    recommendation.setExpectedImpact(impact);
                    recommendation.setExpiresAt(LocalDateTime.now().plusDays(21));

                    recommendations.add(recommendation);
                };
    }

        } catch (Exception e) {
            log.error("Error generating content type recommendations: {}", e.getMessage(), e);
        }

        return recommendations;
    }

    // Helper methods
    private String formatProviderName(String provider) {
        return provider.replace("_", " ").toLowerCase()
                .replace("openai", "OpenAI")
                .replace("gemini", "Gemini")
                .replace("anthropic", "Anthropic")
                .replace("huggingface", "Hugging Face")
                .replace("fal ai", "Fal AI")
                .replace("stable diffusion", "Stable Diffusion"); }

    private String formatContentType(String type) {
        return type.replace("_", " ").toLowerCase()
                .replace("primary text", "Primary Text")
                .replace("call to action", "Call to Action"); };
    }
