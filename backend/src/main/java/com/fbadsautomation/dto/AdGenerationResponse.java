package com.fbadsautomation.dto;

import java.util.List;

public class AdGenerationResponse {
    private Long adId; // ID của ad đã tạo
    private List<AdVariation> variations;
    private String status;
    private String message;
    
    public AdGenerationResponse() {}
    
    public AdGenerationResponse(Long adId, List<AdVariation> variations, String status, String message) {
        this.adId = adId;
        this.variations = variations;
        this.status = status;
        this.message = message;
    }
    
    // Getter and Setter methods
    public Long getAdId() { return adId; }
    public void setAdId(Long adId) { this.adId = adId; }
    
    public List<AdVariation> getVariations() { return variations; }
    public void setVariations(List<AdVariation> variations) { this.variations = variations; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long adId;
        private List<AdVariation> variations;
        private String status;
        private String message;
        
        public Builder adId(Long adId) {
            this.adId = adId;
            return this;
        }
        
        public Builder variations(List<AdVariation> variations) {
            this.variations = variations;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public AdGenerationResponse build() {
            return new AdGenerationResponse(adId, variations, status, message);
        }
    }
    
    public static class AdVariation {
        private Long id; // ID của AdContent
        private String headline;
        private String primaryText;
        private String description;
        private String callToAction;
        private String imageUrl;
        private Integer order;
        
        public AdVariation() {}
        
        public AdVariation(Long id, String headline, String primaryText, String description, 
                          String callToAction, String imageUrl, Integer order) {
            this.id = id;
            this.headline = headline;
            this.primaryText = primaryText;
            this.description = description;
            this.callToAction = callToAction;
            this.imageUrl = imageUrl;
            this.order = order;
        }
        
        // Getter and Setter methods
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getHeadline() { return headline; }
        public void setHeadline(String headline) { this.headline = headline; }
        
        public String getPrimaryText() { return primaryText; }
        public void setPrimaryText(String primaryText) { this.primaryText = primaryText; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCallToAction() { return callToAction; }
        public void setCallToAction(String callToAction) { this.callToAction = callToAction; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        
        // Builder pattern for AdVariation
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Long id;
            private String headline;
            private String primaryText;
            private String description;
            private String callToAction;
            private String imageUrl;
            private Integer order;
            
            public Builder id(Long id) {
                this.id = id;
                return this;
            }
            
            public Builder headline(String headline) {
                this.headline = headline;
                return this;
            }
            
            public Builder primaryText(String primaryText) {
                this.primaryText = primaryText;
                return this;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder callToAction(String callToAction) {
                this.callToAction = callToAction;
                return this;
            }
            
            public Builder imageUrl(String imageUrl) {
                this.imageUrl = imageUrl;
                return this;
            }
            
            public Builder order(Integer order) {
                this.order = order;
                return this;
            }
            
            public AdVariation build() {
                return new AdVariation(id, headline, primaryText, description, callToAction, imageUrl, order);
            }
        }
    }
}
