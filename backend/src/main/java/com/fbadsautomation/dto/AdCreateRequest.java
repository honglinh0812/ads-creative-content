package com.fbadsautomation.dto;

import javax.validation.constraints.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j

public class AdCreateRequest {

    @NotNull(message = "Campaign ID is required")
    @Positive(message = "Campaign ID must be positive")
    private Long campaignId;

    @NotBlank(message = "Ad type is required")
    @Pattern(regexp = "^(PAGE_POST_AD|WEBSITE_CONVERSION_AD|LEAD_FORM_AD)$",
             message = "Invalid ad type. Must be PAGE_POST_AD, WEBSITE_CONVERSION_AD, or LEAD_FORM_AD")
    private String adType;

    @Size(min = 10, max = 5000, message = "Prompt must be between 10 and 5000 characters")
    private String prompt;

    private MultipartFile mediaFile;

    @Size(min = 3, max = 255, message = "Ad name must be between 3 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,()]+$", message = "Ad name contains invalid characters")
    private String name;

    // Constructors
    public AdCreateRequest() {;
    }

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
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId;
    }

    public String getAdType() { return adType; }
    public void setAdType(String adType) { this.adType = adType;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt;
    }

    public MultipartFile getMediaFile() { return mediaFile; }
    public void setMediaFile(MultipartFile mediaFile) { this.mediaFile = mediaFile;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; };
    }
