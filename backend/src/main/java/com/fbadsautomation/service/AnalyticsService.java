package com.fbadsautomation.service;

import com.fbadsautomation.dto.AnalyticsResponse.*;
import com.fbadsautomation.dto.AnalyticsResponse;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final AdRepository adRepository;
    private final AdContentRepository adContentRepository;

    /**
     * Get comprehensive analytics for a user
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long userId, String timeRange) {
        try {
            log.info("Generating analytics for user ID: {} with time range: {}", userId, timeRange);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            LocalDateTime startDate = calculateStartDate(timeRange);
            LocalDateTime endDate = LocalDateTime.now();

            // Generate all analytics components
            KPIMetrics kpiMetrics = generateKPIMetrics(user, startDate, endDate);
            List<TimeSeriesData> performanceTrends = generatePerformanceTrends(user, startDate, endDate, timeRange);
            List<CampaignAnalytics> campaignAnalytics = generateCampaignAnalytics(user, startDate, endDate);
            List<AdAnalytics> adAnalytics = generateAdAnalytics(user, startDate, endDate);
            AIProviderAnalytics aiProviderAnalytics = generateAIProviderAnalytics(user, startDate, endDate);
            BudgetAnalytics budgetAnalytics = generateBudgetAnalytics(user, startDate, endDate);
            ContentAnalytics contentAnalytics = generateContentAnalytics(user, startDate, endDate);

            AnalyticsResponse response = new AnalyticsResponse(
                kpiMetrics, performanceTrends, campaignAnalytics, adAnalytics,
                aiProviderAnalytics, budgetAnalytics, contentAnalytics
            );

            log.info("Analytics generated successfully for user ID: {}", userId);
            return response;

        } catch (Exception e) {
            log.error("Error generating analytics for user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate analytics: " + e.getMessage(), e);
        }
    }

    /**
     * Generate KPI metrics
     */
    private KPIMetrics generateKPIMetrics(User user, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Basic counts
            long totalCampaigns = campaignRepository.countByUser(user);
            long totalAds = adRepository.countByUser(user);
            long activeCampaigns = campaignRepository.countByUserAndStatus(user, Campaign.CampaignStatus.ACTIVE);
            long activeAds = adRepository.countByUserAndStatus(user, "ACTIVE");

            // Budget calculations
            List<Object[]> budgetData = campaignRepository.findCampaignsWithBudgetAnalysis(user);
            double totalBudget = budgetData.stream()
                    .mapToDouble(row -> {
                        Double dailyBudget = (Double) row[1];
                        Double totalBudgetValue = (Double) row[2];
                        return Math.max(dailyBudget != null ? dailyBudget : 0.0, 
                                       totalBudgetValue != null ? totalBudgetValue : 0.0);
                    })
                    .sum();

            // Simulated performance metrics (in real app, these would come from Facebook API)
            double totalSpent = totalBudget * 0.65; // 65% budget utilization;
            long totalImpressions = totalAds * 15000; // Average 15k impressions per ad;
            long totalClicks = (long) (totalImpressions * 0.025); // 2.5% CTR;
            double averageCTR = totalImpressions > 0 ? (double) totalClicks / totalImpressions * 100 : 0.0;
            double averageCPC = totalClicks > 0 ? totalSpent / totalClicks : 0.0;
            double conversions = totalClicks * 0.08; // 8% conversion rate;
            double conversionRate = totalClicks > 0 ? conversions / totalClicks * 100 : 0.0;
            double roi = totalSpent > 0 ? (conversions * 50 - totalSpent) / totalSpent * 100 : 0.0; // $50 avg order value;

            // AI-specific metrics
            int contentGenerated = (int) adContentRepository.countByUser(user);
            double aiCostSavings = contentGenerated * 25.0; // $25 saved per AI-generated content;

            // Growth calculations (compared to previous period)
            LocalDateTime previousPeriodStart = startDate.minus(ChronoUnit.DAYS.between(startDate, endDate), ChronoUnit.DAYS);
            double campaignGrowth = calculateGrowthRate(totalCampaigns, 
                    campaignRepository.countByUserAndCreatedDateBetween(user, previousPeriodStart, startDate));
            double adGrowth = calculateGrowthRate(totalAds,
                    adRepository.countByUserAndCreatedDateBetween(user, previousPeriodStart, startDate));

            KPIMetrics metrics = new KPIMetrics(
                totalCampaigns, totalAds, activeCampaigns, activeAds,
                totalBudget, totalSpent, averageCTR, averageCPC,
                totalImpressions, totalClicks, conversionRate, roi,
                contentGenerated, aiCostSavings
            );

            metrics.setCampaignGrowth(campaignGrowth);
            metrics.setAdGrowth(adGrowth);
            metrics.setBudgetGrowth(15.2); // Simulated growth
            metrics.setImpressionGrowth(23.5); // Simulated growth

            return metrics;

        } catch (Exception e) {
            log.error("Error generating KPI metrics: {}", e.getMessage(), e);
            return new KPIMetrics();
        }
    }

    /**
     * Generate performance trends over time
     */
    private List<TimeSeriesData> generatePerformanceTrends(User user, LocalDateTime startDate, 
                                                          LocalDateTime endDate, String timeRange) {
        List<TimeSeriesData> trends = new ArrayList<>();
        
        try {
            String period = determinePeriod(timeRange);
            List<LocalDateTime> timePoints = generateTimePoints(startDate, endDate, period);

            for (LocalDateTime timePoint : timePoints) {
                Map<String, Double> metrics = new HashMap<>();
                
                // Simulate trend data (in real app, this would query actual performance data)
                double baseImpressions = 10000 + (Math.random() * 5000);
                double baseCTR = 2.0 + (Math.random() * 1.5);
                double baseCPC = 0.8 + (Math.random() * 0.4);
                double baseConversions = baseImpressions * (baseCTR / 100) * 0.08;
                
                metrics.put("impressions", baseImpressions);
                metrics.put("clicks", baseImpressions * (baseCTR / 100));
                metrics.put("ctr", baseCTR);
                metrics.put("cpc", baseCPC);
                metrics.put("conversions", baseConversions);
                metrics.put("spend", baseImpressions * (baseCTR / 100) * baseCPC);

                trends.add(new TimeSeriesData(timePoint, period, metrics));
            }
        } catch (Exception e) {
            log.error("Error generating performance trends: {}", e.getMessage(), e);
        }

        return trends;
    }

    /**
     * Generate campaign analytics
     */
    private List<CampaignAnalytics> generateCampaignAnalytics(User user, LocalDateTime startDate, LocalDateTime endDate) {
        List<CampaignAnalytics> analytics = new ArrayList<>();
        
        try {
            List<Object[]> campaignData = campaignRepository.findCampaignsWithStatsByUser(user);
            
            for (Object[] row : campaignData) {
                Campaign campaign = (Campaign) row[0];
                Long adCount = (Long) row[1];
                Long activeAdCount = (Long) row[2];
                
                CampaignAnalytics campaignAnalytic = new CampaignAnalytics();
                campaignAnalytic.setCampaignId(campaign.getId());
                campaignAnalytic.setCampaignName(campaign.getName());
                campaignAnalytic.setStatus(campaign.getStatus().toString());
                campaignAnalytic.setObjective(campaign.getObjective() != null ? campaign.getObjective().toString() : "");
                
                double budget = Math.max(
                    campaign.getDailyBudget() != null ? campaign.getDailyBudget() : 0.0,
                    campaign.getTotalBudget() != null ? campaign.getTotalBudget() : 0.0
                );
                campaignAnalytic.setBudget(budget);
                
                // Simulate performance data
                double spent = budget * (0.5 + Math.random() * 0.4); // 50-90% utilization;
                long impressions = adCount.intValue() * (10000 + (int)(Math.random() * 10000));
                long clicks = (long) (impressions * (0.015 + Math.random() * 0.02)); // 1.5-3.5% CTR;
                double ctr = impressions > 0 ? (double) clicks / impressions * 100 : 0.0;
                double cpc = clicks > 0 ? spent / clicks : 0.0;
                double conversions = clicks * (0.05 + Math.random() * 0.1); // 5-15% conversion rate;
                double conversionRate = clicks > 0 ? conversions / clicks * 100 : 0.0;
                double roi = spent > 0 ? (conversions * 45 - spent) / spent * 100 : 0.0;
                
                campaignAnalytic.setSpent(spent);
                campaignAnalytic.setBudgetUtilization(budget > 0 ? spent / budget * 100 : 0.0);
                campaignAnalytic.setAdCount(adCount.intValue());
                campaignAnalytic.setActiveAdCount(activeAdCount.intValue());
                campaignAnalytic.setImpressions(impressions);
                campaignAnalytic.setClicks(clicks);
                campaignAnalytic.setCtr(ctr);
                campaignAnalytic.setCpc(cpc);
                campaignAnalytic.setConversions(conversions);
                campaignAnalytic.setConversionRate(conversionRate);
                campaignAnalytic.setRoi(roi);
                campaignAnalytic.setCreatedDate(campaign.getCreatedDate());
                campaignAnalytic.setLastActive(campaign.getUpdatedAt());
                
                analytics.add(campaignAnalytic);
            }
        } catch (Exception e) {
            log.error("Error generating campaign analytics: {}", e.getMessage(), e);
        }

        return analytics;
    }

    /**
     * Generate ad analytics
     */
    private List<AdAnalytics> generateAdAnalytics(User user, LocalDateTime startDate, LocalDateTime endDate) {
        List<AdAnalytics> analytics = new ArrayList<>();
        
        try {
            List<Object[]> adData = adRepository.findAdsWithContentStats(user);
            
            for (Object[] row : adData) {
                Ad ad = (Ad) row[0];
                Long contentCount = (Long) row[1];
                Long selectedContentCount = (Long) row[2];
                
                AdAnalytics adAnalytic = new AdAnalytics();
                adAnalytic.setAdId(ad.getId());
                adAnalytic.setAdName(ad.getName());
                adAnalytic.setCampaignName(ad.getCampaign().getName());
                adAnalytic.setStatus(ad.getStatus());
                adAnalytic.setAdType(ad.getAdType() != null ? ad.getAdType().toString() : "");
                
                // Simulate performance data
                long impressions = 8000 + (long)(Math.random() * 12000);
                long clicks = (long) (impressions * (0.02 + Math.random() * 0.03));
                double ctr = impressions > 0 ? (double) clicks / impressions * 100 : 0.0;
                double cpc = 0.6 + Math.random() * 0.8;
                double conversions = clicks * (0.06 + Math.random() * 0.08);
                double conversionRate = clicks > 0 ? conversions / clicks * 100 : 0.0;
                
                adAnalytic.setImpressions(impressions);
                adAnalytic.setClicks(clicks);
                adAnalytic.setCtr(ctr);
                adAnalytic.setCpc(cpc);
                adAnalytic.setConversions(conversions);
                adAnalytic.setConversionRate(conversionRate);
                
                // Get AI provider from selected content
                List<AdContent> adContents = adContentRepository.findByAdAndUserOrderByPreviewOrder(ad, user);
                String aiProvider = adContents.stream()
                        .filter(AdContent::getIsSelected)
                        .findFirst()
                        .map(content -> content.getAiProvider() != null ? content.getAiProvider().toString() : "Unknown")
                        .orElse("Unknown");
                
                adAnalytic.setAiProvider(aiProvider);
                adAnalytic.setContentVariations(contentCount.intValue());
                adAnalytic.setCreatedDate(ad.getCreatedDate());
                adAnalytic.setLastActive(ad.getUpdatedAt());
                
                analytics.add(adAnalytic);
            }
        } catch (Exception e) {
            log.error("Error generating ad analytics: {}", e.getMessage(), e);
        }

        return analytics;
    }

    /**
     * Generate AI provider analytics
     */
    private AIProviderAnalytics generateAIProviderAnalytics(User user, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> providerStats = adContentRepository.findAIProviderStatsByUser(user);
            Map<String, AIProviderAnalytics.ProviderMetrics> providerMetrics = new HashMap<>();
            
            double totalCost = 0.0;
            int totalContent = 0;
            String mostUsedProvider = "";
            int maxContentCount = 0;
            
            for (Object[] row : providerStats) {
                String providerName = row[0].toString();
                Long contentCount = (Long) row[1];
                Double selectionRate = (Double) row[2];
                
                // Simulate additional metrics
                double successRate = 85.0 + Math.random() * 10.0; // 85-95%;
                double avgResponseTime = 1500 + Math.random() * 1000; // 1.5-2.5 seconds;
                double cost = contentCount * (0.01 + Math.random() * 0.02); // $0.01-0.03 per content;
                
                AIProviderAnalytics.ProviderMetrics metrics = new AIProviderAnalytics.ProviderMetrics(
                    providerName, contentCount.intValue(), successRate, avgResponseTime, cost, selectionRate * 100
                );
                
                providerMetrics.put(providerName, metrics);
                totalCost += cost;
                totalContent += contentCount.intValue();
                
                if (contentCount.intValue() > maxContentCount) {
                    maxContentCount = contentCount.intValue();
                    mostUsedProvider = providerName;
                };
            }
            
            // Determine best performing provider (highest selection rate)
            String bestPerformingProvider = providerMetrics.entrySet().stream()
                    .max(Map.Entry.comparingByValue((m1, m2) -> Double.compare(m1.getSelectionRate(), m2.getSelectionRate())))
                    .map(Map.Entry::getKey)
                    .orElse("");
            
            double estimatedSavings = totalContent * 25.0; // $25 saved per AI-generated content;
            
            return new AIProviderAnalytics(providerMetrics, mostUsedProvider, bestPerformingProvider, 
                                         totalCost, estimatedSavings, totalContent);

        } catch (Exception e) {
            log.error("Error generating AI provider analytics: {}", e.getMessage(), e);
            return new AIProviderAnalytics();
        }
    }

    /**
     * Generate budget analytics
     */
    private BudgetAnalytics generateBudgetAnalytics(User user, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> budgetData = campaignRepository.findCampaignsWithBudgetAnalysis(user);
            
            double totalBudgetAllocated = budgetData.stream()
                    .mapToDouble(row -> {
                        Double dailyBudget = (Double) row[1];
                        Double totalBudget = (Double) row[2];
                        return Math.max(dailyBudget != null ? dailyBudget : 0.0, 
                                       totalBudget != null ? totalBudget : 0.0);
                    })
                    .sum();
            
            double totalBudgetSpent = totalBudgetAllocated * 0.68; // 68% utilization;
            double budgetUtilizationRate = totalBudgetAllocated > 0 ? totalBudgetSpent / totalBudgetAllocated * 100 : 0.0;
            
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            double averageDailySpend = daysBetween > 0 ? totalBudgetSpent / daysBetween : 0.0;
            double projectedMonthlySpend = averageDailySpend * 30;
            
            // Generate budget breakdown
            List<BudgetAnalytics.BudgetBreakdown> budgetBreakdown = Arrays.asList(
                new BudgetAnalytics.BudgetBreakdown("Campaign Ads", totalBudgetSpent * 0.85, 85.0),
                new BudgetAnalytics.BudgetBreakdown("AI Content Generation", totalBudgetSpent * 0.10, 10.0),
                new BudgetAnalytics.BudgetBreakdown("Platform Fees", totalBudgetSpent * 0.05, 5.0)
            );
            
            // Generate spending trends (simplified)
            List<BudgetAnalytics.SpendingTrend> spendingTrends = new ArrayList<>();
            LocalDateTime current = startDate;
            while (current.isBefore(endDate)) {
                double dailySpend = averageDailySpend * (0.8 + Math.random() * 0.4); // ±20% variation;
                spendingTrends.add(new BudgetAnalytics.SpendingTrend(current, dailySpend, "Campaign Ads"));
                current = current.plusDays(1);
            }
            
            BudgetAnalytics analytics = new BudgetAnalytics();
            analytics.setTotalBudgetAllocated(totalBudgetAllocated);
            analytics.setTotalBudgetSpent(totalBudgetSpent);
            analytics.setBudgetUtilizationRate(budgetUtilizationRate);
            analytics.setAverageDailySpend(averageDailySpend);
            analytics.setProjectedMonthlySpend(projectedMonthlySpend);
            analytics.setBudgetBreakdown(budgetBreakdown);
            analytics.setSpendingTrends(spendingTrends);
            
            return analytics;

        } catch (Exception e) {
            log.error("Error generating budget analytics: {}", e.getMessage(), e);
            return new BudgetAnalytics();
        }
    }

    /**
     * Generate content analytics
     */
    private ContentAnalytics generateContentAnalytics(User user, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            int totalContentGenerated = (int) adContentRepository.countByUser(user);
            List<AdContent> selectedContents = adContentRepository.findSelectedByUserWithRelations(user);
            int selectedContent = selectedContents.size();
            double selectionRate = totalContentGenerated > 0 ? (double) selectedContent / totalContentGenerated * 100 : 0.0;
            
            // Content type distribution
            List<Object[]> contentTypeStats = adContentRepository.findContentTypeStatsByUser(user);
            Map<String, Integer> contentTypeDistribution = contentTypeStats.stream()
                    .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> ((Long) row[1]).intValue()
                    ));
            
            // AI provider performance
            List<Object[]> providerStats = adContentRepository.findAIProviderStatsByUser(user);
            Map<String, Double> aiProviderPerformance = providerStats.stream()
                    .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Double) row[2] * 100 // Selection rate as percentage
                    ));
            
            // Top performing content (simulated)
            List<ContentAnalytics.TopPerformingContent> topPerformingContent = selectedContents.stream()
                    .limit(10)
                    .map(content -> {
                        long impressions = 5000 + (long)(Math.random() * 10000);
                        double ctr = 2.0 + Math.random() * 2.0;
                        double performanceScore = impressions * (ctr / 100) * 10; // Simple scoring;
                        
                        return new ContentAnalytics.TopPerformingContent(
                            content.getId(),
                            content.getHeadline(),
                            content.getAiProvider() != null ? content.getAiProvider().toString() : "Unknown",
                            performanceScore,
                            impressions,
                            ctr
                        );
                    })
                    .sorted((c1, c2) -> Double.compare(c2.getPerformanceScore(), c1.getPerformanceScore()))
                    .collect(Collectors.toList());
            
            ContentAnalytics analytics = new ContentAnalytics();
            analytics.setTotalContentGenerated(totalContentGenerated);
            analytics.setSelectedContent(selectedContent);
            analytics.setSelectionRate(selectionRate);
            analytics.setContentTypeDistribution(contentTypeDistribution);
            analytics.setAiProviderPerformance(aiProviderPerformance);
            analytics.setTopPerformingContent(topPerformingContent);
            
            return analytics;

        } catch (Exception e) {
            log.error("Error generating content analytics: {}", e.getMessage(), e);
            return new ContentAnalytics();
        }
    }

    // Helper methods
    private LocalDateTime calculateStartDate(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeRange.toLowerCase()) {
            case "7d": return now.minusDays(7);
            case "30d": return now.minusDays(30);
            case "90d": return now.minusDays(90);
            case "1y": return now.minusYears(1);
            default: return now.minusDays(30);
        }
    }

    private String determinePeriod(String timeRange) {
        switch (timeRange.toLowerCase()) {
            case "7d": return "day";
            case "30d": return "day";
            case "90d": return "week";
            case "1y": return "month";
            default: return "day";
        }
    }

    private List<LocalDateTime> generateTimePoints(LocalDateTime start, LocalDateTime end, String period) {
        List<LocalDateTime> points = new ArrayList<>();
        LocalDateTime current = start;
        
        while (current.isBefore(end)) {
            points.add(current);
            switch (period) {
                case "day": current = current.plusDays(1); break;
                case "week": current = current.plusWeeks(1); break;
                case "month": current = current.plusMonths(1); break;
                default: current = current.plusDays(1);
            }
        }
        
        return points;
    }

    private double calculateGrowthRate(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((double) (current - previous) / previous) * 100.0;
    }
}
