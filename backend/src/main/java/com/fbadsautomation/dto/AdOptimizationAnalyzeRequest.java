package com.fbadsautomation.dto;

import java.util.List;

public class AdOptimizationAnalyzeRequest {
    private List<Long> adIds;
    private String language;

    public List<Long> getAdIds() {
        return adIds;
    }

    public void setAdIds(List<Long> adIds) {
        this.adIds = adIds;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
