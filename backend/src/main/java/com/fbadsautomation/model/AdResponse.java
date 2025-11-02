package com.fbadsautomation.model;

import java.time.LocalDateTime;

public class AdResponse {
    private Long id;
    private String name;
    private String adType;
    private String status;
    private String campaignName;
    private String imageUrl;
    private String videoUrl;
    private String headline;
    private String description;
    private String primaryText;
    private String callToAction;
    private LocalDateTime createdDate;
    private Long selectedContentId;
    private Long campaignId;

    public AdResponse() {}

    public AdResponse(Long id, String name, String adType, String status, String campaignName,
                     String imageUrl, String videoUrl, String headline, String description,
                     String primaryText, String callToAction, LocalDateTime createdDate,
                     Long selectedContentId, Long campaignId) {
        this.id = id;
        this.name = name;
        this.adType = adType;
        this.status = status;
        this.campaignName = campaignName;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.headline = headline;
        this.description = description;
        this.primaryText = primaryText;
        this.callToAction = callToAction;
        this.createdDate = createdDate;
        this.selectedContentId = selectedContentId;
        this.campaignId = campaignId;
    }

    public static AdResponseBuilder builder() {
        return new AdResponseBuilder();
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAdType() { return adType; }
    public String getStatus() { return status; }
    public String getCampaignName() { return campaignName; }
    public String getImageUrl() { return imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public String getHeadline() { return headline; }
    public String getDescription() { return description; }
    public String getPrimaryText() { return primaryText; }
    public String getCallToAction() { return callToAction; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Long getSelectedContentId() { return selectedContentId; }
    public Long getCampaignId() { return campaignId; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAdType(String adType) { this.adType = adType; }
    public void setStatus(String status) { this.status = status; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setHeadline(String headline) { this.headline = headline; }
    public void setDescription(String description) { this.description = description; }
    public void setPrimaryText(String primaryText) { this.primaryText = primaryText; }
    public void setCallToAction(String callToAction) { this.callToAction = callToAction; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public void setSelectedContentId(Long selectedContentId) { this.selectedContentId = selectedContentId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public static class AdResponseBuilder {
        private Long id;
        private String name;
        private String adType;
        private String status;
        private String campaignName;
        private String imageUrl;
        private String videoUrl;
        private String headline;
        private String description;
        private String primaryText;
        private String callToAction;
        private LocalDateTime createdDate;
        private Long selectedContentId;
        private Long campaignId;

        public AdResponseBuilder id(Long id) { this.id = id; return this; }
        public AdResponseBuilder name(String name) { this.name = name; return this; }
        public AdResponseBuilder adType(String adType) { this.adType = adType; return this; }
        public AdResponseBuilder status(String status) { this.status = status; return this; }
        public AdResponseBuilder campaignName(String campaignName) { this.campaignName = campaignName; return this; }
        public AdResponseBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public AdResponseBuilder videoUrl(String videoUrl) { this.videoUrl = videoUrl; return this; }
        public AdResponseBuilder headline(String headline) { this.headline = headline; return this; }
        public AdResponseBuilder description(String description) { this.description = description; return this; }
        public AdResponseBuilder primaryText(String primaryText) { this.primaryText = primaryText; return this; }
        public AdResponseBuilder callToAction(String callToAction) { this.callToAction = callToAction; return this; }
        public AdResponseBuilder createdDate(LocalDateTime createdDate) { this.createdDate = createdDate; return this; }
        public AdResponseBuilder selectedContentId(Long selectedContentId) { this.selectedContentId = selectedContentId; return this; }
        public AdResponseBuilder campaignId(Long campaignId) { this.campaignId = campaignId; return this; }

        public AdResponse build() {
            return new AdResponse(id, name, adType, status, campaignName, imageUrl, videoUrl,
                                headline, description, primaryText, callToAction, createdDate,
                                selectedContentId, campaignId);
        }
    }
}