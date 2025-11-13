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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final AdRepository adRepository;
    private final AdContentRepository adContentRepository;

    @Autowired
    public AnalyticsService(UserRepository userRepository, CampaignRepository campaignRepository,
                           AdRepository adRepository, AdContentRepository adContentRepository) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.adRepository = adRepository;
        this.adContentRepository = adContentRepository;
    }

    /**
     * Get comprehensive analytics for a user
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long userId, String timeRange) {
        log.info("[ANALYTICS_SERVICE] Starting analytics generation for user ID: {}, timeRange: {}", userId, timeRange);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("[ANALYTICS_SERVICE] User not found with ID: {}", userId);
                        return new RuntimeException("User not found with ID: " + userId);
                    });

            log.debug("[ANALYTICS_SERVICE] User found: ID={}, Email={}", user.getId(), user.getEmail());

            LocalDateTime startDate = calculateStartDate(timeRange);
            LocalDateTime endDate = LocalDateTime.now();
            
            log.debug("[ANALYTICS_SERVICE] Calculated date range: {} to {}", startDate, endDate);

            // Check if user has any real data
            long campaignCount = 0;
            long adCount = 0;
            
            try {
                log.debug("[ANALYTICS_SERVICE] Counting campaigns and ads for user ID: {}", userId);
                campaignCount = campaignRepository.countByUser(user);
                adCount = adRepository.countByUser(user);
                log.debug("[ANALYTICS_SERVICE] User data counts - Campaigns: {}, Ads: {}", campaignCount, adCount);
            } catch (Exception e) {
                log.warn("[ANALYTICS_SERVICE] Error counting campaigns/ads for user {}, returning demo data: {}", userId, e.getMessage());
                log.error("[ANALYTICS_SERVICE] Database error details - Error Type: {}, Error Message: {}", 
                         e.getClass().getSimpleName(), e.getMessage());
                // If we can't count due to database issues, return demo data
                AnalyticsResponse demoResponse = generateDemoAnalytics(user, timeRange, startDate, endDate);
                demoResponse.setIsDemoData(true);
                log.info("[ANALYTICS_SERVICE] Returning demo data due to database error for user ID: {}", userId);
                return demoResponse;
            }

            if (campaignCount == 0 && adCount == 0) {
                log.info("[ANALYTICS_SERVICE] User {} has no campaigns/ads, returning demo analytics", userId);
                AnalyticsResponse demoResponse = generateDemoAnalytics(user, timeRange, startDate, endDate);
                demoResponse.setIsDemoData(true);
                log.debug("[ANALYTICS_SERVICE] Generated demo analytics for user ID: {}", userId);
                return demoResponse;
            }

            // Generate all analytics components with real data
            log.info("[ANALYTICS_SERVICE] Generating real analytics data for user ID: {}", userId);
            
            log.debug("[ANALYTICS_SERVICE] Generating KPI metrics for user ID: {}", userId);
            KPIMetrics kpiMetrics = generateKPIMetrics(user, startDate, endDate);
            
            log.debug("[ANALYTICS_SERVICE] Generating performance trends for user ID: {}", userId);
            List<TimeSeriesData> performanceTrends = generatePerformanceTrends(user, startDate, endDate, timeRange);
            
            log.debug("[ANALYTICS_SERVICE] Generating campaign analytics for user ID: {}", userId);
            List<CampaignAnalytics> campaignAnalytics = generateCampaignAnalytics(user, startDate, endDate);
            
            log.debug("[ANALYTICS_SERVICE] Generating ad analytics for user ID: {}", userId);
            List<AdAnalytics> adAnalytics = generateAdAnalytics(user, startDate, endDate);
            
            log.debug("[ANALYTICS_SERVICE] Generating AI provider analytics for user ID: {}", userId);
            AIProviderAnalytics aiProviderAnalytics = generateAIProviderAnalytics(user, startDate, endDate);
            
            log.debug("[ANALYTICS_SERVICE] Generating budget analytics for user ID: {}", userId);
            BudgetAnalytics budgetAnalytics = generateBudgetAnalytics(user, startDate, endDate);
            
            log.debug("[ANALYTICS_SERVICE] Generating content analytics for user ID: {}", userId);
            ContentAnalytics contentAnalytics = generateContentAnalytics(user, startDate, endDate);

            AnalyticsResponse response = new AnalyticsResponse(
                kpiMetrics, performanceTrends, campaignAnalytics, adAnalytics,
                aiProviderAnalytics, budgetAnalytics, contentAnalytics
            );
            response.setIsDemoData(false);

            log.info("[ANALYTICS_SERVICE] Successfully generated analytics for user ID: {} with {} campaigns and {} ads", 
                    userId, campaignCount, adCount);
            return response;

        } catch (Exception e) {
            log.error("[ANALYTICS_SERVICE] ERROR generating analytics for user ID: {}, timeRange: {}, error: {}, stackTrace: {}", 
                     userId, timeRange, e.getMessage(), e.getStackTrace()[0].toString(), e);
            
            // Log detailed error information
            log.error("[ANALYTICS_SERVICE] Detailed error context - User ID: {}, TimeRange: {}, Error Type: {}, Error Message: {}", 
                     userId, timeRange, e.getClass().getSimpleName(), e.getMessage());
            
            try {
                // Try to return demo data as fallback
                log.info("[ANALYTICS_SERVICE] Returning demo data as fallback for user ID: {}", userId);
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> {
                            log.error("[ANALYTICS_SERVICE] User not found during fallback for ID: {}", userId);
                            return new RuntimeException("User not found with ID: " + userId);
                        });
                LocalDateTime startDate = calculateStartDate(timeRange);
                LocalDateTime endDate = LocalDateTime.now();
                AnalyticsResponse demoResponse = generateDemoAnalytics(user, timeRange, startDate, endDate);
                demoResponse.setIsDemoData(true);
                log.info("[ANALYTICS_SERVICE] Successfully returned demo data as fallback for user ID: {}", userId);
                return demoResponse;
            } catch (Exception fallbackException) {
                log.error("[ANALYTICS_SERVICE] ERROR generating fallback demo analytics for user ID: {}, error: {}, stackTrace: {}", 
                         userId, fallbackException.getMessage(), fallbackException.getStackTrace()[0].toString(), fallbackException);
                throw new RuntimeException("Failed to generate analytics: " + e.getMessage(), e);
            }
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

    /**
     * Generate demo analytics for users with no campaigns/ads yet
     */
    private AnalyticsResponse generateDemoAnalytics(User user, String timeRange, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating demo analytics for user: {}", user.getId());

        // Create demo KPI metrics
        KPIMetrics demoKPIs = new KPIMetrics(
            3L, // totalCampaigns
            12L, // totalAds
            2L, // activeCampaigns
            8L, // activeAds
            5000.0, // totalBudget
            3400.0, // totalSpent (68% utilization)
            3.25, // averageCTR (3.25%)
            1.12, // averageCPC ($1.12)
            195000L, // totalImpressions
            6337L, // totalClicks
            8.2, // conversionRate (8.2%)
            156.0, // roi (156%)
            28, // contentGenerated
            700.0 // aiCostSavings ($700)
        );
        demoKPIs.setCampaignGrowth(18.5);
        demoKPIs.setAdGrowth(22.3);
        demoKPIs.setBudgetGrowth(15.2);
        demoKPIs.setImpressionGrowth(24.7);

        // Generate demo performance trends
        List<TimeSeriesData> demoTrends = generateDemoPerformanceTrends(startDate, endDate, timeRange);

        // Generate demo campaign analytics
        List<CampaignAnalytics> demoCampaigns = generateDemoCampaigns();

        // Generate demo ad analytics
        List<AdAnalytics> demoAds = generateDemoAds();

        // Generate demo AI provider analytics
        AIProviderAnalytics demoAIProvider = generateDemoAIProviderAnalytics();

        // Generate demo budget analytics
        BudgetAnalytics demoBudget = generateDemoBudgetAnalytics(startDate, endDate);

        // Generate demo content analytics
        ContentAnalytics demoContent = generateDemoContentAnalytics();

        return new AnalyticsResponse(
            demoKPIs, demoTrends, demoCampaigns, demoAds,
            demoAIProvider, demoBudget, demoContent
        );
    }

    private List<TimeSeriesData> generateDemoPerformanceTrends(LocalDateTime startDate, LocalDateTime endDate, String timeRange) {
        List<TimeSeriesData> trends = new ArrayList<>();
        String period = determinePeriod(timeRange);
        List<LocalDateTime> timePoints = generateTimePoints(startDate, endDate, period);

        double baseImpressions = 10000;
        for (int i = 0; i < timePoints.size(); i++) {
            Map<String, Double> metrics = new HashMap<>();
            double growthFactor = 1.0 + (i * 0.05); // 5% growth per period
            double impressions = baseImpressions * growthFactor * (0.9 + Math.random() * 0.2);
            double ctr = 2.8 + Math.random() * 1.2;
            double cpc = 0.95 + Math.random() * 0.4;

            metrics.put("impressions", impressions);
            metrics.put("clicks", impressions * (ctr / 100));
            metrics.put("ctr", ctr);
            metrics.put("cpc", cpc);
            metrics.put("conversions", impressions * (ctr / 100) * 0.082);
            metrics.put("spend", impressions * (ctr / 100) * cpc);

            trends.add(new TimeSeriesData(timePoints.get(i), period, metrics));
        }

        return trends;
    }

    private List<CampaignAnalytics> generateDemoCampaigns() {
        List<CampaignAnalytics> campaigns = new ArrayList<>();

        // Demo Campaign 1: High Performer
        CampaignAnalytics campaign1 = new CampaignAnalytics();
        campaign1.setCampaignId(999001L);
        campaign1.setCampaignName("Summer Sale 2024 (Demo)");
        campaign1.setStatus("ACTIVE");
        campaign1.setObjective("CONVERSIONS");
        campaign1.setBudget(2000.0);
        campaign1.setSpent(1450.0);
        campaign1.setBudgetUtilization(72.5);
        campaign1.setAdCount(5);
        campaign1.setActiveAdCount(4);
        campaign1.setImpressions(85000L);
        campaign1.setClicks(2890L);
        campaign1.setCtr(3.4);
        campaign1.setCpc(0.95);
        campaign1.setConversions(245.0);
        campaign1.setConversionRate(8.5);
        campaign1.setRoi(178.0);
        campaign1.setCreatedDate(LocalDateTime.now().minusDays(15));
        campaign1.setLastActive(LocalDateTime.now().minusHours(2));
        campaigns.add(campaign1);

        // Demo Campaign 2: Average Performer
        CampaignAnalytics campaign2 = new CampaignAnalytics();
        campaign2.setCampaignId(999002L);
        campaign2.setCampaignName("Product Launch Q3 (Demo)");
        campaign2.setStatus("ACTIVE");
        campaign2.setObjective("TRAFFIC");
        campaign2.setBudget(1800.0);
        campaign2.setSpent(1200.0);
        campaign2.setBudgetUtilization(66.7);
        campaign2.setAdCount(4);
        campaign2.setActiveAdCount(3);
        campaign2.setImpressions(65000L);
        campaign2.setClicks(2050L);
        campaign2.setCtr(3.15);
        campaign2.setCpc(1.18);
        campaign2.setConversions(156.0);
        campaign2.setConversionRate(7.6);
        campaign2.setRoi(142.0);
        campaign2.setCreatedDate(LocalDateTime.now().minusDays(22));
        campaign2.setLastActive(LocalDateTime.now().minusHours(5));
        campaigns.add(campaign2);

        // Demo Campaign 3: Paused Campaign
        CampaignAnalytics campaign3 = new CampaignAnalytics();
        campaign3.setCampaignId(999003L);
        campaign3.setCampaignName("Brand Awareness Test (Demo)");
        campaign3.setStatus("PAUSED");
        campaign3.setObjective("BRAND_AWARENESS");
        campaign3.setBudget(1200.0);
        campaign3.setSpent(750.0);
        campaign3.setBudgetUtilization(62.5);
        campaign3.setAdCount(3);
        campaign3.setActiveAdCount(0);
        campaign3.setImpressions(45000L);
        campaign3.setClicks(1397L);
        campaign3.setCtr(3.1);
        campaign3.setCpc(1.32);
        campaign3.setConversions(98.0);
        campaign3.setConversionRate(7.0);
        campaign3.setRoi(118.0);
        campaign3.setCreatedDate(LocalDateTime.now().minusDays(30));
        campaign3.setLastActive(LocalDateTime.now().minusDays(3));
        campaigns.add(campaign3);

        return campaigns;
    }

    private List<AdAnalytics> generateDemoAds() {
        List<AdAnalytics> ads = new ArrayList<>();
        String[] adNames = {
            "Summer Sale - Image Ad (Demo)", "Product Launch - Video (Demo)",
            "Brand Story - Carousel (Demo)", "Limited Offer - Static (Demo)",
            "New Arrivals - Dynamic (Demo)", "Flash Sale - Animated (Demo)",
            "Customer Testimonial (Demo)", "Feature Highlight (Demo)",
            "Seasonal Promotion (Demo)", "Holiday Special (Demo)",
            "Weekend Deal (Demo)", "Exclusive Access (Demo)"
        };
        String[] campaigns = {"Summer Sale 2024 (Demo)", "Product Launch Q3 (Demo)", "Brand Awareness Test (Demo)"};
        String[] providers = {"OPENAI", "GEMINI", "STABLE_DIFFUSION"};

        for (int i = 0; i < 12; i++) {
            AdAnalytics ad = new AdAnalytics();
            ad.setAdId(999100L + i);
            ad.setAdName(adNames[i]);
            ad.setCampaignName(campaigns[i % 3]);
            ad.setStatus(i < 8 ? "ACTIVE" : "PAUSED");
            ad.setAdType("SINGLE_IMAGE");
            ad.setImpressions(8000L + (long)(Math.random() * 12000));
            ad.setClicks((long)(ad.getImpressions() * (0.025 + Math.random() * 0.02)));
            ad.setCtr(ad.getImpressions() > 0 ? (double) ad.getClicks() / ad.getImpressions() * 100 : 0.0);
            ad.setCpc(0.85 + Math.random() * 0.6);
            ad.setConversions(ad.getClicks() * (0.07 + Math.random() * 0.04));
            ad.setConversionRate(ad.getClicks() > 0 ? ad.getConversions() / ad.getClicks() * 100 : 0.0);
            ad.setAiProvider(providers[i % 3]);
            ad.setContentVariations(2 + (i % 4));
            ad.setCreatedDate(LocalDateTime.now().minusDays(30 - i * 2));
            ad.setLastActive(LocalDateTime.now().minusHours(i * 3));
            ads.add(ad);
        }

        return ads;
    }

    private AIProviderAnalytics generateDemoAIProviderAnalytics() {
        Map<String, AIProviderAnalytics.ProviderMetrics> providerMetrics = new HashMap<>();

        providerMetrics.put("OPENAI", new AIProviderAnalytics.ProviderMetrics(
            "OPENAI", 15, 92.5, 1650.0, 12.50, 73.3
        ));
        providerMetrics.put("GEMINI", new AIProviderAnalytics.ProviderMetrics(
            "GEMINI", 8, 88.2, 1850.0, 6.40, 62.5
        ));
        providerMetrics.put("STABLE_DIFFUSION", new AIProviderAnalytics.ProviderMetrics(
            "STABLE_DIFFUSION", 5, 85.0, 2100.0, 4.00, 60.0
        ));

        return new AIProviderAnalytics(
            providerMetrics,
            "OPENAI",
            "OPENAI",
            22.90,
            700.0,
            28
        );
    }

    private BudgetAnalytics generateDemoBudgetAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        BudgetAnalytics analytics = new BudgetAnalytics();
        analytics.setTotalBudgetAllocated(5000.0);
        analytics.setTotalBudgetSpent(3400.0);
        analytics.setBudgetUtilizationRate(68.0);
        analytics.setAverageDailySpend(113.3);
        analytics.setProjectedMonthlySpend(3400.0);

        List<BudgetAnalytics.BudgetBreakdown> breakdown = Arrays.asList(
            new BudgetAnalytics.BudgetBreakdown("Campaign Ads", 2890.0, 85.0),
            new BudgetAnalytics.BudgetBreakdown("AI Content Generation", 340.0, 10.0),
            new BudgetAnalytics.BudgetBreakdown("Platform Fees", 170.0, 5.0)
        );
        analytics.setBudgetBreakdown(breakdown);

        List<BudgetAnalytics.SpendingTrend> trends = new ArrayList<>();
        LocalDateTime current = startDate;
        while (current.isBefore(endDate)) {
            double dailySpend = 100.0 + Math.random() * 50.0;
            trends.add(new BudgetAnalytics.SpendingTrend(current, dailySpend, "Campaign Ads"));
            current = current.plusDays(1);
        }
        analytics.setSpendingTrends(trends);

        return analytics;
    }

    private ContentAnalytics generateDemoContentAnalytics() {
        ContentAnalytics analytics = new ContentAnalytics();
        analytics.setTotalContentGenerated(28);
        analytics.setSelectedContent(18);
        analytics.setSelectionRate(64.3);

        Map<String, Integer> typeDistribution = new HashMap<>();
        typeDistribution.put("SINGLE_IMAGE", 15);
        typeDistribution.put("CAROUSEL", 8);
        typeDistribution.put("VIDEO", 5);
        analytics.setContentTypeDistribution(typeDistribution);

        Map<String, Double> providerPerformance = new HashMap<>();
        providerPerformance.put("OPENAI", 73.3);
        providerPerformance.put("GEMINI", 62.5);
        providerPerformance.put("STABLE_DIFFUSION", 60.0);
        analytics.setAiProviderPerformance(providerPerformance);

        List<ContentAnalytics.TopPerformingContent> topContent = Arrays.asList(
            new ContentAnalytics.TopPerformingContent(999201L, "Summer Sale - 50% Off Everything (Demo)", "OPENAI", 8500.0, 12500L, 3.8),
            new ContentAnalytics.TopPerformingContent(999202L, "New Product Launch - Limited Edition (Demo)", "GEMINI", 7200.0, 10800L, 3.5),
            new ContentAnalytics.TopPerformingContent(999203L, "Flash Sale - Today Only (Demo)", "OPENAI", 6800.0, 9500L, 3.6)
        );
        analytics.setTopPerformingContent(topContent);

        return analytics;
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
