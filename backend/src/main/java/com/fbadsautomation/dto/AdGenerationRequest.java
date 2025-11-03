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
    @Pattern(regexp = "^(PAGE_POST_AD|WEBSITE_CONVERSION_AD|LEAD_FORM_AD|page_post|website_conversion|lead_generation)$",
             message = "Invalid ad type. Accepted values: page_post, website_conversion, lead_generation, PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD")
    private String adType;

    @Size(max = 5000, message = "Prompt cannot exceed 5000 characters")
    private String prompt;

    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,()]+$", message = "Name contains invalid characters. Only letters, numbers, spaces, and common punctuation allowed.")
    private String name;

    /**
     * URL của file media đã upload (nếu có). Nếu không có file upload trực tiếp, backend sẽ dùng giá trị này để set imageUrl cho ad.
     * Optional field - validation removed temporarily for Phase 1 (will be re-added with proper optional validator in Phase 3)
     */
    // @Pattern(regexp = "^(https?://.*|/api/images/.*)$", message = "Invalid media file URL format")
    private String mediaFileUrl;

    @NotBlank(message = "Text provider is required")
    @Pattern(regexp = "^(openai|gemini|anthropic|huggingface)$",
             message = "Invalid text provider. Must be openai, gemini, anthropic, or huggingface")
    private String textProvider;

    @Pattern(regexp = "^(gemini|openai|huggingface|stable-diffusion|fal-ai)$",
             message = "Invalid image provider. Must be gemini, openai, huggingface, stable-diffusion, or fal-ai")
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

    @Pattern(regexp = "^(PROFESSIONAL|CASUAL|HUMOROUS|URGENT|LUXURY|EDUCATIONAL|INSPIRATIONAL|MINIMALIST)$",
             message = "Invalid ad style. Must be one of: PROFESSIONAL, CASUAL, HUMOROUS, URGENT, LUXURY, EDUCATIONAL, INSPIRATIONAL, MINIMALIST")
    private String adStyle; // Optional: creative style/tone for ad content (Issue #8)

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

    @AssertTrue(message = "Website URL is required for WEBSITE_CONVERSION_AD type")
    public boolean isWebsiteUrlValidForAdType() {
        if (adType != null && (adType.equalsIgnoreCase("WEBSITE_CONVERSION_AD") || adType.toLowerCase().contains("website_conversion"))) {
            return websiteUrl != null && !websiteUrl.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Lead form questions are required for LEAD_FORM_AD type")
    public boolean isLeadFormQuestionsValidForAdType() {
        if (adType != null && (adType.equalsIgnoreCase("LEAD_FORM_AD") || adType.toLowerCase().contains("lead_generation"))) {
            return leadFormQuestions != null && !leadFormQuestions.isEmpty();
        }
        return true;
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

    public String getAdStyle() { return adStyle; }
    public void setAdStyle(String adStyle) { this.adStyle = adStyle; }
}
