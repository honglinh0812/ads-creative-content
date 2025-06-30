package com.fbadsautomation.dto;

import org.springframework.web.multipart.MultipartFile;

public class AdCreateRequest {
    private Long campaignId;
    private String adType; // PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD
    private String prompt;
    private MultipartFile mediaFile;
    private String name;

    // Constructors
    public AdCreateRequest() {}

    public AdCreateRequest(Long campaignId, String adType, String prompt, 
                          MultipartFile mediaFile, String name) {
        this.campaignId = campaignId;
        this.adType = adType;
        this.prompt = prompt;
        this.mediaFile = mediaFile;
        this.name = name;
    }

    // Getters and Setters
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }

    public String getAdType() { return adType; }
    public void setAdType(String adType) { this.adType = adType; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public MultipartFile getMediaFile() { return mediaFile; }
    public void setMediaFile(MultipartFile mediaFile) { this.mediaFile = mediaFile; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

