package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdGenerationRequest {
    
    @NotNull(message = "Campaign ID is required")
    private Long campaignId;
    
    @NotBlank(message = "Ad type is required")
    private String adType; // PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD
    
    @NotBlank(message = "Prompt is required")
    private String prompt;
    
    private String name;
    
    private String mediaFileUrl; // URL của file media đã upload (nếu có)
    
    @NotBlank(message = "Text provider is required")
    private String textProvider; // openai, gemini
    
    private String imageProvider; // openai, huggingface (không bắt buộc nếu đã có mediaFileUrl)
    
    @NotNull(message = "Number of variations is required")
    private Integer numberOfVariations;
    
    private String language;
    
    private String callToAction;
}

