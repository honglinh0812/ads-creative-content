package com.fbadsautomation.dto;

import com.fbadsautomation.dto.AdVariation;
import com.fbadsautomation.model.FacebookCTA;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;
public class AdGenerationRequest {
    
    @NotNull(message = "Campaign ID is required")
    @Positive(message = "Campaign ID must be positive")
    private Long campaignId;

    @NotBlank(message = "Ad type is required")
    @Pattern(regexp = "^(PAGE_POST_AD|WEBSITE_CONVERSION_AD|LEAD_FORM_AD)$",
             message = "Invalid ad type. Must be PAGE_POST_AD, WEBSITE_CONVERSION_AD, or LEAD_FORM_AD")
    private String adType;

    @Size(max = 5000, message = "Prompt cannot exceed 5000 characters")
    private String prompt;

    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,()]+$", message = "Name contains invalid characters")
    private String name;

    /**
     * URL của file media đã upload (nếu có). Nếu không có file upload trực tiếp, backend sẽ dùng giá trị này để set imageUrl cho ad.
     */
    @Pattern(regexp = "^(https?://.*|/api/images/.*)$", message = "Invalid media file URL format")
    private String mediaFileUrl;

    @NotBlank(message = "Text provider is required")
    @Pattern(regexp = "^(openai|gemini|anthropic|huggingface)$",
             message = "Invalid text provider. Must be openai, gemini, anthropic, or huggingface")
    private String textProvider;

    @Pattern(regexp = "^(openai|huggingface|stable-diffusion|fal-ai)$",
             message = "Invalid image provider. Must be openai, huggingface, stable-diffusion, or fal-ai")
    private String imageProvider;

    @NotNull(message = "Number of variations is required")
    @Min(value = 1, message = "Number of variations must be at least 1")
    @Max(value = 10, message = "Number of variations cannot exceed 10")
    private Integer numberOfVariations;

    @Pattern(regexp = "^(en|vi|es|fr|de|it|pt|ru|ja|ko|zh)$",
             message = "Invalid language code")
    private String language;

    @Size(max = 10, message = "Cannot exceed 10 ad links")
    private List<@Pattern(regexp = "^https?://.*", message = "Invalid URL format") String> adLinks;

    @Size(max = 100, message = "Prompt style cannot exceed 100 characters")
    private String promptStyle;

    @Size(max = 2000, message = "Custom prompt cannot exceed 2000 characters")
    private String customPrompt;

    private FacebookCTA callToAction;

    @Size(max = 10000, message = "Extracted content cannot exceed 10000 characters")
    private String extractedContent; // Content extracted from Meta Ad Library

    private Boolean isPreview; // Flag để phân biệt preview vs save thực sự

    @Valid
    private AdVariation selectedVariation; // Variation được chọn khi save

    private Boolean saveExistingContent; // Flag để backend biết chỉ lưu nội dung hiện tại

    // New fields for ad type specific data
    @Pattern(regexp = "^https?://.*", message = "Invalid website URL format")
    private String websiteUrl; // For WEBSITE_CONVERSION_AD

    @Valid
    @Size(max = 20, message = "Cannot exceed 20 lead form questions")
    private List<LeadFormQuestion> leadFormQuestions; // For LEAD_FORM_AD

    @Valid
    private AudienceSegmentRequest audienceSegment; // For audience targeting

    @Positive(message = "Persona ID must be positive")
    private Long personaId; // Optional: persona to use for prompt enhancement

    // Inner class for lead form questions
    public static class LeadFormQuestion {
        @NotBlank(message = "Question type is required")
        @Pattern(regexp = "^(FULL_NAME|EMAIL|PHONE|COMPANY|JOB_TITLE|CUSTOM)$",
                 message = "Invalid question type")
        private String type; // FULL_NAME, EMAIL, PHONE, COMPANY, JOB_TITLE, CUSTOM

        @Size(max = 200, message = "Custom text cannot exceed 200 characters")
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

    public AudienceSegmentRequest getAudienceSegment() { return audienceSegment; }
    public void setAudienceSegment(AudienceSegmentRequest audienceSegment) { this.audienceSegment = audienceSegment; }

    public Long getPersonaId() { return personaId; }
    public void setPersonaId(Long personaId) { this.personaId = personaId; }
}
