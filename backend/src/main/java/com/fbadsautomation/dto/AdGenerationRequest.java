package com.fbadsautomation.dto;

import com.fbadsautomation.dto.AdVariation;
import com.fbadsautomation.model.FacebookCTA;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
public class AdGenerationRequest {
    
    @NotNull(message = "Campaign ID is required")
    private Long campaignId;
    @NotBlank(message = "Ad type is required")
    private String adType; // PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD
    
    private String prompt; // Remove @NotBlank to allow empty prompt when ad links are provided
    
    private String name;
    
    /**
     * URL của file media đã upload (nếu có). Nếu không có file upload trực tiếp, backend sẽ dùng giá trị này để set imageUrl cho ad.
     */
    private String mediaFileUrl;
    
    @NotBlank(message = "Text provider is required")
    private String textProvider; // openai, gemini
    
    private String imageProvider; // openai, huggingface (không bắt buộc nếu đã có mediaFileUrl)
    @NotNull(message = "Number of variations is required")
    private Integer numberOfVariations;
    private String language;

    private List<String> adLinks;
    
    private String promptStyle;
    
    private String customPrompt;
    
    private FacebookCTA callToAction;
    
    private String extractedContent; // Content extracted from Meta Ad Library
    
    private Boolean isPreview; // Flag để phân biệt preview vs save thực sự
    
    private AdVariation selectedVariation; // Variation được chọn khi save
    
    private Boolean saveExistingContent; // Flag để backend biết chỉ lưu nội dung hiện tại

    // New fields for ad type specific data
    private String websiteUrl; // For WEBSITE_CONVERSION_AD
    private List<LeadFormQuestion> leadFormQuestions; // For LEAD_FORM_AD
    
    // Inner class for lead form questions
    public static class LeadFormQuestion {
        private String type; // FULL_NAME, EMAIL, PHONE, COMPANY, JOB_TITLE, CUSTOM
        private String customText; // For CUSTOM type
        
        public LeadFormQuestion() {}
        
        public LeadFormQuestion(String type, String customText) {
            this.type = type;
            this.customText = customText;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCustomText() { return customText; }
        public void setCustomText(String customText) { this.customText = customText; }
    }
    
    @AssertTrue(message = "Could not create ads. Please check the ad prompt / ad link and try again.")
    public boolean isPromptOrAdLinksValid() {
        boolean hasPrompt = prompt != null && !prompt.trim().isEmpty();
        boolean hasAdLinks = adLinks != null && !adLinks.isEmpty() && adLinks.stream().anyMatch(link -> link != null && !link.trim().isEmpty());
        boolean hasExtractedContent = extractedContent != null && !extractedContent.trim().isEmpty();
        return hasPrompt || hasAdLinks || hasExtractedContent;
    }

    // Getter and Setter methods
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    
    public String getAdType() { return adType; }
    public void setAdType(String adType) { this.adType = adType; }
    
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getMediaFileUrl() { return mediaFileUrl; }
    public void setMediaFileUrl(String mediaFileUrl) { this.mediaFileUrl = mediaFileUrl; }
    
    public String getTextProvider() { return textProvider; }
    public void setTextProvider(String textProvider) { this.textProvider = textProvider; }
    
    public String getImageProvider() { return imageProvider; }
    public void setImageProvider(String imageProvider) { this.imageProvider = imageProvider; }
    
    public Integer getNumberOfVariations() { return numberOfVariations; }
    public void setNumberOfVariations(Integer numberOfVariations) { this.numberOfVariations = numberOfVariations; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public List<String> getAdLinks() { return adLinks; }
    public void setAdLinks(List<String> adLinks) { this.adLinks = adLinks; }
    
    public String getPromptStyle() { return promptStyle; }
    public void setPromptStyle(String promptStyle) { this.promptStyle = promptStyle; }
    
    public String getCustomPrompt() { return customPrompt; }
    public void setCustomPrompt(String customPrompt) { this.customPrompt = customPrompt; }
    
    public FacebookCTA getCallToAction() { return this.callToAction; }
    public void setCallToAction(FacebookCTA callToAction) { this.callToAction = callToAction; }
    
    public String getExtractedContent() { return extractedContent; }
    public void setExtractedContent(String extractedContent) { this.extractedContent = extractedContent; }
    
    public Boolean getIsPreview() { return isPreview; }
    public void setIsPreview(Boolean isPreview) { this.isPreview = isPreview; }
    
    public AdVariation getSelectedVariation() { return selectedVariation; }
    public void setSelectedVariation(AdVariation selectedVariation) { this.selectedVariation = selectedVariation; }
    
    public Boolean getSaveExistingContent() { return saveExistingContent; }
    public void setSaveExistingContent(Boolean saveExistingContent) { this.saveExistingContent = saveExistingContent; }
    
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    
    public List<LeadFormQuestion> getLeadFormQuestions() { return leadFormQuestions; }
    public void setLeadFormQuestions(List<LeadFormQuestion> leadFormQuestions) { this.leadFormQuestions = leadFormQuestions; }
}
