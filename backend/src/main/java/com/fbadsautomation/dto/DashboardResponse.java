package com.fbadsautomation.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class DashboardResponse {
    private List<CampaignSummary> campaigns;
    private List<AdSummary> recentAds;
    private DashboardStats stats;

    // Constructors
    public DashboardResponse() {;
    }

    public DashboardResponse(List<CampaignSummary> campaigns, List<AdSummary> recentAds, DashboardStats stats) {
        this.campaigns = campaigns;
        this.recentAds = recentAds;
        this.stats = stats;
    }

    // Getters and Setters
    public List<CampaignSummary> getCampaigns() { return campaigns; }
    public void setCampaigns(List<CampaignSummary> campaigns) { this.campaigns = campaigns; }

    public List<AdSummary> getRecentAds() { return recentAds; }
    public void setRecentAds(List<AdSummary> recentAds) { this.recentAds = recentAds; }

    public DashboardStats getStats() { return stats; }
    public void setStats(DashboardStats stats) { this.stats = stats; }

    // Inner classes
    public static class CampaignSummary {
        private Long id;
        private String name;
        private String objective;
        private String status;
        private Double budget;
        private LocalDateTime createdDate;
        private int adCount;

        // Constructors
        public CampaignSummary() {;
    }

        public CampaignSummary(Long id, String name, String objective, String status, 
                             Double budget, LocalDateTime createdDate, int adCount) {
            this.id = id;
            this.name = name;
            this.objective = objective;
            this.status = status;
            this.budget = budget;
            this.createdDate = createdDate;
            this.adCount = adCount;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getObjective() { return objective; }
        public void setObjective(String objective) { this.objective = objective; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Double getBudget() { return budget; }
        public void setBudget(Double budget) { this.budget = budget; }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

        public int getAdCount() { return adCount; }
        public void setAdCount(int adCount) { this.adCount = adCount; };
    }

    public static class AdSummary {
        private Long id;
        private String name;
        private String adType;
        private String status;
        private String campaignName;
        private LocalDateTime createdDate;
        private String imageUrl;
        private String videoUrl;
        private String headline;
        private String primaryText;
        private String description;
        private String callToAction;

        // Constructors
        public AdSummary() {;
    }

        public AdSummary(Long id, String name, String adType, String status, 
                        String campaignName, LocalDateTime createdDate, String imageUrl, String videoUrl,
                        String headline, String primaryText, String description, String callToAction) {
            this.id = id;
            this.name = name;
            this.adType = adType;
            this.status = status;
            this.campaignName = campaignName;
            this.createdDate = createdDate;
            this.imageUrl = imageUrl;
            this.videoUrl = videoUrl;
            this.headline = headline;
            this.primaryText = primaryText;
            this.description = description;
            this.callToAction = callToAction;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAdType() { return adType; }
        public void setAdType(String adType) { this.adType = adType; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

        public String getHeadline() { return headline; }
        public void setHeadline(String headline) { this.headline = headline; }

        public String getPrimaryText() { return primaryText; }
        public void setPrimaryText(String primaryText) { this.primaryText = primaryText; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCallToAction() { return callToAction; }
        public void setCallToAction(String callToAction) { this.callToAction = callToAction; };
    }

    public static class DashboardStats {
        private long totalCampaigns;
        private long totalAds;
        private long activeCampaigns;
        private long activeAds;

        // Constructors
        public DashboardStats() {;
    }

        public DashboardStats(long totalCampaigns, long totalAds, long activeCampaigns, long activeAds) {
            this.totalCampaigns = totalCampaigns;
            this.totalAds = totalAds;
            this.activeCampaigns = activeCampaigns;
            this.activeAds = activeAds;
        }

        // Getters and Setters
        public long getTotalCampaigns() { return totalCampaigns; }
        public void setTotalCampaigns(long totalCampaigns) { this.totalCampaigns = totalCampaigns; }

        public long getTotalAds() { return totalAds; }
        public void setTotalAds(long totalAds) { this.totalAds = totalAds; }

        public long getActiveCampaigns() { return activeCampaigns; }
        public void setActiveCampaigns(long activeCampaigns) { this.activeCampaigns = activeCampaigns; }

        public long getActiveAds() { return activeAds; }
        public void setActiveAds(long activeAds) { this.activeAds = activeAds; };
    }
}
