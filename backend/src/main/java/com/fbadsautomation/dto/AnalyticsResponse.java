package com.fbadsautomation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class AnalyticsResponse {
    private KPIMetrics kpiMetrics;
    private List<TimeSeriesData> performanceTrends;
    private List<CampaignAnalytics> campaignAnalytics;
    private List<AdAnalytics> adAnalytics;
    private AIProviderAnalytics aiProviderAnalytics;
    private BudgetAnalytics budgetAnalytics;
    private ContentAnalytics contentAnalytics;
    private LocalDateTime generatedAt;
    private boolean isDemoData;

    // Constructors
    public AnalyticsResponse() {
        this.generatedAt = LocalDateTime.now();
        this.isDemoData = false;
    }

    public AnalyticsResponse(KPIMetrics kpiMetrics, List<TimeSeriesData> performanceTrends,
                           List<CampaignAnalytics> campaignAnalytics, List<AdAnalytics> adAnalytics,
                           AIProviderAnalytics aiProviderAnalytics, BudgetAnalytics budgetAnalytics,
                           ContentAnalytics contentAnalytics) {
        this.kpiMetrics = kpiMetrics;
        this.performanceTrends = performanceTrends;
        this.campaignAnalytics = campaignAnalytics;
        this.adAnalytics = adAnalytics;
        this.aiProviderAnalytics = aiProviderAnalytics;
        this.budgetAnalytics = budgetAnalytics;
        this.contentAnalytics = contentAnalytics;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public KPIMetrics getKpiMetrics() { return kpiMetrics; }
    public void setKpiMetrics(KPIMetrics kpiMetrics) { this.kpiMetrics = kpiMetrics;
    }

    public List<TimeSeriesData> getPerformanceTrends() { return performanceTrends; }
    public void setPerformanceTrends(List<TimeSeriesData> performanceTrends) { this.performanceTrends = performanceTrends;
    }

    public List<CampaignAnalytics> getCampaignAnalytics() { return campaignAnalytics; }
    public void setCampaignAnalytics(List<CampaignAnalytics> campaignAnalytics) { this.campaignAnalytics = campaignAnalytics;
    }

    public List<AdAnalytics> getAdAnalytics() { return adAnalytics; }
    public void setAdAnalytics(List<AdAnalytics> adAnalytics) { this.adAnalytics = adAnalytics;
    }

    public AIProviderAnalytics getAiProviderAnalytics() { return aiProviderAnalytics; }
    public void setAiProviderAnalytics(AIProviderAnalytics aiProviderAnalytics) { this.aiProviderAnalytics = aiProviderAnalytics;
    }

    public BudgetAnalytics getBudgetAnalytics() { return budgetAnalytics; }
    public void setBudgetAnalytics(BudgetAnalytics budgetAnalytics) { this.budgetAnalytics = budgetAnalytics;
    }

    public ContentAnalytics getContentAnalytics() { return contentAnalytics; }
    public void setContentAnalytics(ContentAnalytics contentAnalytics) { this.contentAnalytics = contentAnalytics;
    }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt;
    }

    public boolean isDemoData() { return isDemoData; }
    public void setDemoData(boolean demoData) { this.isDemoData = demoData; }
    public boolean getIsDemoData() { return isDemoData; }
    public void setIsDemoData(boolean demoData) { this.isDemoData = demoData; }

    // Inner Classes
    public static class KPIMetrics {
        private long totalCampaigns;
        private long totalAds;
        private long activeCampaigns;
        private long activeAds;
        private double totalBudget;
        private double totalSpent;
        private double averageCTR;
        private double averageCPC;
        private long totalImpressions;
        private long totalClicks;
        private double conversionRate;
        private double roi;
        private int contentGenerated;
        private double aiCostSavings;

        // Growth metrics (compared to previous period)
        private double campaignGrowth;
        private double adGrowth;
        private double budgetGrowth;
        private double impressionGrowth;

        public KPIMetrics() {
        }

        public KPIMetrics(long totalCampaigns, long totalAds, long activeCampaigns, long activeAds,
                         double totalBudget, double totalSpent, double averageCTR, double averageCPC,
                         long totalImpressions, long totalClicks, double conversionRate, double roi,
                         int contentGenerated, double aiCostSavings) {
            this.totalCampaigns = totalCampaigns;
            this.totalAds = totalAds;
            this.activeCampaigns = activeCampaigns;
            this.activeAds = activeAds;
            this.totalBudget = totalBudget;
            this.totalSpent = totalSpent;
            this.averageCTR = averageCTR;
            this.averageCPC = averageCPC;
            this.totalImpressions = totalImpressions;
            this.totalClicks = totalClicks;
            this.conversionRate = conversionRate;
            this.roi = roi;
            this.contentGenerated = contentGenerated;
            this.aiCostSavings = aiCostSavings;
        }

        // Getters and Setters
        public long getTotalCampaigns() { return totalCampaigns; }
        public void setTotalCampaigns(long totalCampaigns) { this.totalCampaigns = totalCampaigns; }

        public long getTotalAds() { return totalAds; }
        public void setTotalAds(long totalAds) { this.totalAds = totalAds; }

        public long getActiveCampaigns() { return activeCampaigns; }
        public void setActiveCampaigns(long activeCampaigns) { this.activeCampaigns = activeCampaigns; }

        public long getActiveAds() { return activeAds; }
        public void setActiveAds(long activeAds) { this.activeAds = activeAds; }

        public double getTotalBudget() { return totalBudget; }
        public void setTotalBudget(double totalBudget) { this.totalBudget = totalBudget; }

        public double getTotalSpent() { return totalSpent; }
        public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

        public double getAverageCTR() { return averageCTR; }
        public void setAverageCTR(double averageCTR) { this.averageCTR = averageCTR; }

        public double getAverageCPC() { return averageCPC; }
        public void setAverageCPC(double averageCPC) { this.averageCPC = averageCPC; }

        public long getTotalImpressions() { return totalImpressions; }
        public void setTotalImpressions(long totalImpressions) { this.totalImpressions = totalImpressions; }

        public long getTotalClicks() { return totalClicks; }
        public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }

        public double getConversionRate() { return conversionRate; }
        public void setConversionRate(double conversionRate) { this.conversionRate = conversionRate; }

        public double getRoi() { return roi; }
        public void setRoi(double roi) { this.roi = roi; }

        public int getContentGenerated() { return contentGenerated; }
        public void setContentGenerated(int contentGenerated) { this.contentGenerated = contentGenerated; }

        public double getAiCostSavings() { return aiCostSavings; }
        public void setAiCostSavings(double aiCostSavings) { this.aiCostSavings = aiCostSavings; }

        public double getCampaignGrowth() { return campaignGrowth; }
        public void setCampaignGrowth(double campaignGrowth) { this.campaignGrowth = campaignGrowth; }

        public double getAdGrowth() { return adGrowth; }
        public void setAdGrowth(double adGrowth) { this.adGrowth = adGrowth; }

        public double getBudgetGrowth() { return budgetGrowth; }
        public void setBudgetGrowth(double budgetGrowth) { this.budgetGrowth = budgetGrowth; }

        public double getImpressionGrowth() { return impressionGrowth; }
        public void setImpressionGrowth(double impressionGrowth) { this.impressionGrowth = impressionGrowth; }
    }

    public static class TimeSeriesData {
        private LocalDateTime timestamp;
        private String period; // "day", "week", "month"
        private Map<String, Double> metrics;

        public TimeSeriesData() {
        }

        public TimeSeriesData(LocalDateTime timestamp, String period, Map<String, Double> metrics) {
            this.timestamp = timestamp;
            this.period = period;
            this.metrics = metrics;
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }

        public Map<String, Double> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Double> metrics) { this.metrics = metrics; }
    }

    public static class CampaignAnalytics {
        private Long campaignId;
        private String campaignName;
        private String status;
        private String objective;
        private double budget;
        private double spent;
        private double budgetUtilization;
        private int adCount;
        private int activeAdCount;
        private long impressions;
        private long clicks;
        private double ctr;
        private double cpc;
        private double conversions;
        private double conversionRate;
        private double roi;
        private LocalDateTime createdDate;
        private LocalDateTime lastActive;

        public CampaignAnalytics() {;
    }

        // Getters and Setters
        public Long getCampaignId() { return campaignId; }
        public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getObjective() { return objective; }
        public void setObjective(String objective) { this.objective = objective; }

        public double getBudget() { return budget; }
        public void setBudget(double budget) { this.budget = budget; }

        public double getSpent() { return spent; }
        public void setSpent(double spent) { this.spent = spent; }

        public double getBudgetUtilization() { return budgetUtilization; }
        public void setBudgetUtilization(double budgetUtilization) { this.budgetUtilization = budgetUtilization; }

        public int getAdCount() { return adCount; }
        public void setAdCount(int adCount) { this.adCount = adCount; }

        public int getActiveAdCount() { return activeAdCount; }
        public void setActiveAdCount(int activeAdCount) { this.activeAdCount = activeAdCount; }

        public long getImpressions() { return impressions; }
        public void setImpressions(long impressions) { this.impressions = impressions; }

        public long getClicks() { return clicks; }
        public void setClicks(long clicks) { this.clicks = clicks; }

        public double getCtr() { return ctr; }
        public void setCtr(double ctr) { this.ctr = ctr; }

        public double getCpc() { return cpc; }
        public void setCpc(double cpc) { this.cpc = cpc; }

        public double getConversions() { return conversions; }
        public void setConversions(double conversions) { this.conversions = conversions; }

        public double getConversionRate() { return conversionRate; }
        public void setConversionRate(double conversionRate) { this.conversionRate = conversionRate; }

        public double getRoi() { return roi; }
        public void setRoi(double roi) { this.roi = roi; }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

        public LocalDateTime getLastActive() { return lastActive; }
        public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; };
    }

    public static class AdAnalytics {
        private Long adId;
        private String adName;
        private String campaignName;
        private String status;
        private String adType;
        private long impressions;
        private long clicks;
        private double ctr;
        private double cpc;
        private double conversions;
        private double conversionRate;
        private String aiProvider;
        private int contentVariations;
        private LocalDateTime createdDate;
        private LocalDateTime lastActive;

        public AdAnalytics() {;
    }

        // Getters and Setters
        public Long getAdId() { return adId; }
        public void setAdId(Long adId) { this.adId = adId; }

        public String getAdName() { return adName; }
        public void setAdName(String adName) { this.adName = adName; }

        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getAdType() { return adType; }
        public void setAdType(String adType) { this.adType = adType; }

        public long getImpressions() { return impressions; }
        public void setImpressions(long impressions) { this.impressions = impressions; }

        public long getClicks() { return clicks; }
        public void setClicks(long clicks) { this.clicks = clicks; }

        public double getCtr() { return ctr; }
        public void setCtr(double ctr) { this.ctr = ctr; }

        public double getCpc() { return cpc; }
        public void setCpc(double cpc) { this.cpc = cpc; }

        public double getConversions() { return conversions; }
        public void setConversions(double conversions) { this.conversions = conversions; }

        public double getConversionRate() { return conversionRate; }
        public void setConversionRate(double conversionRate) { this.conversionRate = conversionRate; }

        public String getAiProvider() { return aiProvider; }
        public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }

        public int getContentVariations() { return contentVariations; }
        public void setContentVariations(int contentVariations) { this.contentVariations = contentVariations; }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

        public LocalDateTime getLastActive() { return lastActive; }
        public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; };
    }

    public static class AIProviderAnalytics {
        private Map<String, ProviderMetrics> providerMetrics;
        private String mostUsedProvider;
        private String bestPerformingProvider;
        private double totalAICost;
        private double estimatedSavings;
        private int totalContentGenerated;

        public AIProviderAnalytics() {;
    }

        public AIProviderAnalytics(Map<String, ProviderMetrics> providerMetrics, String mostUsedProvider,
                                 String bestPerformingProvider, double totalAICost, double estimatedSavings,
                                 int totalContentGenerated) {
            this.providerMetrics = providerMetrics;
            this.mostUsedProvider = mostUsedProvider;
            this.bestPerformingProvider = bestPerformingProvider;
            this.totalAICost = totalAICost;
            this.estimatedSavings = estimatedSavings;
            this.totalContentGenerated = totalContentGenerated;
        }

        // Getters and Setters
        public Map<String, ProviderMetrics> getProviderMetrics() { return providerMetrics; }
        public void setProviderMetrics(Map<String, ProviderMetrics> providerMetrics) { this.providerMetrics = providerMetrics; }

        public String getMostUsedProvider() { return mostUsedProvider; }
        public void setMostUsedProvider(String mostUsedProvider) { this.mostUsedProvider = mostUsedProvider; }

        public String getBestPerformingProvider() { return bestPerformingProvider; }
        public void setBestPerformingProvider(String bestPerformingProvider) { this.bestPerformingProvider = bestPerformingProvider; }

        public double getTotalAICost() { return totalAICost; }
        public void setTotalAICost(double totalAICost) { this.totalAICost = totalAICost; }

        public double getEstimatedSavings() { return estimatedSavings; }
        public void setEstimatedSavings(double estimatedSavings) { this.estimatedSavings = estimatedSavings; }

        public int getTotalContentGenerated() { return totalContentGenerated; }
        public void setTotalContentGenerated(int totalContentGenerated) { this.totalContentGenerated = totalContentGenerated; }

        public static class ProviderMetrics {
            private String providerName;
            private int contentCount;
            private double successRate;
            private double averageResponseTime;
            private double totalCost;
            private double selectionRate;

            public ProviderMetrics() {;
    }

            public ProviderMetrics(String providerName, int contentCount, double successRate,
                                 double averageResponseTime, double totalCost, double selectionRate) {
                this.providerName = providerName;
                this.contentCount = contentCount;
                this.successRate = successRate;
                this.averageResponseTime = averageResponseTime;
                this.totalCost = totalCost;
                this.selectionRate = selectionRate;
            }

            // Getters and Setters
            public String getProviderName() { return providerName; }
            public void setProviderName(String providerName) { this.providerName = providerName; }

            public int getContentCount() { return contentCount; }
            public void setContentCount(int contentCount) { this.contentCount = contentCount; }

            public double getSuccessRate() { return successRate; }
            public void setSuccessRate(double successRate) { this.successRate = successRate; }

            public double getAverageResponseTime() { return averageResponseTime; }
            public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }

            public double getTotalCost() { return totalCost; }
            public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

            public double getSelectionRate() { return selectionRate; }
            public void setSelectionRate(double selectionRate) { this.selectionRate = selectionRate; };
    }
    }

    public static class BudgetAnalytics {
        private double totalBudgetAllocated;
        private double totalBudgetSpent;
        private double budgetUtilizationRate;
        private double averageDailySpend;
        private double projectedMonthlySpend;
        private List<BudgetBreakdown> budgetBreakdown;
        private List<SpendingTrend> spendingTrends;

        public BudgetAnalytics() {;
    }

        // Getters and Setters
        public double getTotalBudgetAllocated() { return totalBudgetAllocated; }
        public void setTotalBudgetAllocated(double totalBudgetAllocated) { this.totalBudgetAllocated = totalBudgetAllocated; }

        public double getTotalBudgetSpent() { return totalBudgetSpent; }
        public void setTotalBudgetSpent(double totalBudgetSpent) { this.totalBudgetSpent = totalBudgetSpent; }

        public double getBudgetUtilizationRate() { return budgetUtilizationRate; }
        public void setBudgetUtilizationRate(double budgetUtilizationRate) { this.budgetUtilizationRate = budgetUtilizationRate; }

        public double getAverageDailySpend() { return averageDailySpend; }
        public void setAverageDailySpend(double averageDailySpend) { this.averageDailySpend = averageDailySpend; }

        public double getProjectedMonthlySpend() { return projectedMonthlySpend; }
        public void setProjectedMonthlySpend(double projectedMonthlySpend) { this.projectedMonthlySpend = projectedMonthlySpend; }

        public List<BudgetBreakdown> getBudgetBreakdown() { return budgetBreakdown; }
        public void setBudgetBreakdown(List<BudgetBreakdown> budgetBreakdown) { this.budgetBreakdown = budgetBreakdown; }

        public List<SpendingTrend> getSpendingTrends() { return spendingTrends; }
        public void setSpendingTrends(List<SpendingTrend> spendingTrends) { this.spendingTrends = spendingTrends; }

        public static class BudgetBreakdown {
            private String category;
            private double amount;
            private double percentage;

            public BudgetBreakdown() {;
    }

            public BudgetBreakdown(String category, double amount, double percentage) {
                this.category = category;
                this.amount = amount;
                this.percentage = percentage;
            }

            // Getters and Setters
            public String getCategory() { return category; }
            public void setCategory(String category) { this.category = category; }

            public double getAmount() { return amount; }
            public void setAmount(double amount) { this.amount = amount; }

            public double getPercentage() { return percentage; }
            public void setPercentage(double percentage) { this.percentage = percentage; };
    }

        public static class SpendingTrend {
            private LocalDateTime date;
            private double amount;
            private String category;

            public SpendingTrend() {;
    }

            public SpendingTrend(LocalDateTime date, double amount, String category) {
                this.date = date;
                this.amount = amount;
                this.category = category;
            }

            // Getters and Setters
            public LocalDateTime getDate() { return date; }
            public void setDate(LocalDateTime date) { this.date = date; }

            public double getAmount() { return amount; }
            public void setAmount(double amount) { this.amount = amount; }

            public String getCategory() { return category; }
            public void setCategory(String category) { this.category = category; };
    }
    }

    public static class ContentAnalytics {
        private int totalContentGenerated;
        private int selectedContent;
        private double selectionRate;
        private Map<String, Integer> contentTypeDistribution;
        private Map<String, Double> aiProviderPerformance;
        private List<TopPerformingContent> topPerformingContent;

        public ContentAnalytics() {;
    }

        // Getters and Setters
        public int getTotalContentGenerated() { return totalContentGenerated; }
        public void setTotalContentGenerated(int totalContentGenerated) { this.totalContentGenerated = totalContentGenerated; }

        public int getSelectedContent() { return selectedContent; }
        public void setSelectedContent(int selectedContent) { this.selectedContent = selectedContent; }

        public double getSelectionRate() { return selectionRate; }
        public void setSelectionRate(double selectionRate) { this.selectionRate = selectionRate; }

        public Map<String, Integer> getContentTypeDistribution() { return contentTypeDistribution; }
        public void setContentTypeDistribution(Map<String, Integer> contentTypeDistribution) { this.contentTypeDistribution = contentTypeDistribution; }

        public Map<String, Double> getAiProviderPerformance() { return aiProviderPerformance; }
        public void setAiProviderPerformance(Map<String, Double> aiProviderPerformance) { this.aiProviderPerformance = aiProviderPerformance; }

        public List<TopPerformingContent> getTopPerformingContent() { return topPerformingContent; }
        public void setTopPerformingContent(List<TopPerformingContent> topPerformingContent) { this.topPerformingContent = topPerformingContent; }

        public static class TopPerformingContent {
            private Long contentId;
            private String headline;
            private String aiProvider;
            private double performanceScore;
            private long impressions;
            private double ctr;

            public TopPerformingContent() {;
    }

            public TopPerformingContent(Long contentId, String headline, String aiProvider,
                                      double performanceScore, long impressions, double ctr) {
                this.contentId = contentId;
                this.headline = headline;
                this.aiProvider = aiProvider;
                this.performanceScore = performanceScore;
                this.impressions = impressions;
                this.ctr = ctr;
            }

            // Getters and Setters
            public Long getContentId() { return contentId; }
            public void setContentId(Long contentId) { this.contentId = contentId; }

            public String getHeadline() { return headline; }
            public void setHeadline(String headline) { this.headline = headline; }

            public String getAiProvider() { return aiProvider; }
            public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }

            public double getPerformanceScore() { return performanceScore; }
            public void setPerformanceScore(double performanceScore) { this.performanceScore = performanceScore; }

            public long getImpressions() { return impressions; }
            public void setImpressions(long impressions) { this.impressions = impressions; }

            public double getCtr() { return ctr; }
            public void setCtr(double ctr) { this.ctr = ctr; };
    }
    };
    }
