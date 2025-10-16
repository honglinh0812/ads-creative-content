package com.fbadsautomation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PromptValidationRequest {

    @NotBlank(message = "Prompt is required")
    @Size(min = 10, max = 5000, message = "Prompt must be between 10 and 5000 characters")
    private String prompt;

    private String adType;
    private String language;
    private String targetAudience;
    private String industry;

    // Constructors
    public PromptValidationRequest() {}

    public PromptValidationRequest(String prompt, String adType, String language, String targetAudience, String industry) {
        this.prompt = prompt;
        this.adType = adType;
        this.language = language;
        this.targetAudience = targetAudience;
        this.industry = industry;
    }

    // Getters and Setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getAdType() { return adType; }
    public void setAdType(String adType) { this.adType = adType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}
