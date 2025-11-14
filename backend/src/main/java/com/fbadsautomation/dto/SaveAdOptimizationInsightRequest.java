package com.fbadsautomation.dto;

import java.util.List;
import java.util.Map;

public class SaveAdOptimizationInsightRequest {
    private Long adId;
    private String adName;
    private String campaignName;
    private String language;
    private Map<String, List<String>> suggestions;
    private AdOptimizationInsightDTO.Scorecard scorecard;

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, List<String>> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(Map<String, List<String>> suggestions) {
        this.suggestions = suggestions;
    }

    public AdOptimizationInsightDTO.Scorecard getScorecard() {
        return scorecard;
    }

    public void setScorecard(AdOptimizationInsightDTO.Scorecard scorecard) {
        this.scorecard = scorecard;
    }
}
