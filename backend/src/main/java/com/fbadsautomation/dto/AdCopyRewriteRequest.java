package com.fbadsautomation.dto;

/**
 * Request payload for rewriting a specific ad copy section.
 */
public class AdCopyRewriteRequest {
    private String section;
    private String additionalGuidance;
    private String language;

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getAdditionalGuidance() {
        return additionalGuidance;
    }

    public void setAdditionalGuidance(String additionalGuidance) {
        this.additionalGuidance = additionalGuidance;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
