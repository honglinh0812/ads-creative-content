package com.fbadsautomation.dto;

import com.fbadsautomation.dto.AdVariation;
import com.fbadsautomation.model.FacebookCTA;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j

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
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadFormQuestion {
        private String type; // FULL_NAME, EMAIL, PHONE, COMPANY, JOB_TITLE, CUSTOM
        private String customText; // For CUSTOM type
    }
    
    @AssertTrue(message = "Could not create ads. Please check the ad prompt / ad link and try again.")
    public boolean isPromptOrAdLinksValid() {
        boolean hasPrompt = prompt != null && !prompt.trim().isEmpty();
        boolean hasAdLinks = adLinks != null && !adLinks.isEmpty() && adLinks.stream().anyMatch(link -> link != null && !link.trim().isEmpty());
        boolean hasExtractedContent = extractedContent != null && !extractedContent.trim().isEmpty();
        return hasPrompt || hasAdLinks || hasExtractedContent;
    }

    public FacebookCTA getCallToAction() {
        return this.callToAction;
    }

    public void setCallToAction(FacebookCTA callToAction) {
        this.callToAction = callToAction;
    }
}
