package com.fbadsautomation.model;

import java.util.List;

public class AdGenerationResponse {
    private List<AdContent> adVariations;
    private String status;
    private String message;
    
    public AdGenerationResponse() {}
    
    public AdGenerationResponse(List<AdContent> adVariations, String status, String message) {
        this.adVariations = adVariations;
        this.status = status;
        this.message = message;
    }
    
    // Getters and Setters
    public List<AdContent> getAdVariations() { return adVariations; }
    public void setAdVariations(List<AdContent> adVariations) { this.adVariations = adVariations; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public static AdGenerationResponseBuilder builder() {
        return new AdGenerationResponseBuilder();
    }
    
    public static class AdGenerationResponseBuilder {
        private List<AdContent> adVariations;
        private String status;
        private String message;
        
        public AdGenerationResponseBuilder adVariations(List<AdContent> adVariations) {
            this.adVariations = adVariations;
            return this;
        }
        
        public AdGenerationResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public AdGenerationResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public AdGenerationResponse build() {
            return new AdGenerationResponse(adVariations, status, message);
        }
    }
}
