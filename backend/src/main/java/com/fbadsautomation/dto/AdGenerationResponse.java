package com.fbadsautomation.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdGenerationResponse {
    private Long adId; // ID của ad đã tạo
    private List<AdVariation> variations;
    private String status;
    private String message;
    private ValidationReport validationReport; // Detailed validation feedback

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

    public ValidationReport getValidationReport() { return validationReport; }
    public void setValidationReport(ValidationReport validationReport) { this.validationReport = validationReport; }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long adId;
        private List<AdVariation> variations;
        private String status;
        private String message;
        private ValidationReport validationReport;

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

        public Builder validationReport(ValidationReport validationReport) {
            this.validationReport = validationReport;
            return this;
        }

        public AdGenerationResponse build() {
            AdGenerationResponse response = new AdGenerationResponse(adId, variations, status, message);
            response.setValidationReport(validationReport);
            return response;
        }
    }
    
    public static class AdVariation {
        private Long id; // ID của AdContent
        private String headline;
        private String primaryText;
        private String description;
        private String callToAction; // Enum value (e.g., "LEARN_MORE")
        private String callToActionLabel; // Display label (e.g., "Tìm hiểu thêm")
        private String imageUrl;
        private Integer order;
        private Integer qualityScore; // Quality score 0-100
        private Boolean hasWarnings; // Flag for warnings
        private List<String> warnings; // List of validation warnings
        private Boolean needsReview; // Flag for content truncated to meet Facebook limits
        private QualityDetails qualityDetails; // Detailed quality breakdown

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

        public String getCallToActionLabel() { return callToActionLabel; }
        public void setCallToActionLabel(String callToActionLabel) { this.callToActionLabel = callToActionLabel; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }

        public Integer getQualityScore() { return qualityScore; }
        public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

        public Boolean getHasWarnings() { return hasWarnings; }
        public void setHasWarnings(Boolean hasWarnings) { this.hasWarnings = hasWarnings; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }

        public Boolean getNeedsReview() { return needsReview; }
        public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }

        public QualityDetails getQualityDetails() { return qualityDetails; }
        public void setQualityDetails(QualityDetails qualityDetails) { this.qualityDetails = qualityDetails; }

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
            private String callToActionLabel;
            private String imageUrl;
            private Integer order;
            private Integer qualityScore;
            private Boolean hasWarnings;
            private List<String> warnings;
            private QualityDetails qualityDetails;

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

            public Builder callToActionLabel(String callToActionLabel) {
                this.callToActionLabel = callToActionLabel;
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

            public Builder qualityScore(Integer qualityScore) {
                this.qualityScore = qualityScore;
                return this;
            }

            public Builder hasWarnings(Boolean hasWarnings) {
                this.hasWarnings = hasWarnings;
                return this;
            }

            public Builder warnings(List<String> warnings) {
                this.warnings = warnings;
                return this;
            }

            public Builder qualityDetails(QualityDetails qualityDetails) {
                this.qualityDetails = qualityDetails;
                return this;
            }

            public AdVariation build() {
                AdVariation variation = new AdVariation(id, headline, primaryText, description, callToAction, imageUrl, order);
                variation.setCallToActionLabel(callToActionLabel);
                variation.setQualityScore(qualityScore);
                variation.setHasWarnings(hasWarnings);
                variation.setWarnings(warnings);
                variation.setQualityDetails(qualityDetails);
                return variation;
            }
        }
    }

    /**
     * Validation report for aggregated quality feedback
     */
    public static class ValidationReport {
        private int totalVariations;
        private int passedVariations;
        private int failedVariations;
        private int variationsWithWarnings;
        private double averageQualityScore;

        public ValidationReport() {}

        public ValidationReport(int totalVariations, int passedVariations, int failedVariations,
                               int variationsWithWarnings, double averageQualityScore) {
            this.totalVariations = totalVariations;
            this.passedVariations = passedVariations;
            this.failedVariations = failedVariations;
            this.variationsWithWarnings = variationsWithWarnings;
            this.averageQualityScore = averageQualityScore;
        }

        // Getters and setters
        public int getTotalVariations() { return totalVariations; }
        public void setTotalVariations(int totalVariations) { this.totalVariations = totalVariations; }

        public int getPassedVariations() { return passedVariations; }
        public void setPassedVariations(int passedVariations) { this.passedVariations = passedVariations; }

        public int getFailedVariations() { return failedVariations; }
        public void setFailedVariations(int failedVariations) { this.failedVariations = failedVariations; }

        public int getVariationsWithWarnings() { return variationsWithWarnings; }
        public void setVariationsWithWarnings(int variationsWithWarnings) { this.variationsWithWarnings = variationsWithWarnings; }

        public double getAverageQualityScore() { return averageQualityScore; }
        public void setAverageQualityScore(double averageQualityScore) { this.averageQualityScore = averageQualityScore; }
    }

    public static class QualityDetails {
        private double complianceScore;
        private double linguisticScore;
        private double persuasivenessScore;
        private double completenessScore;
        private double totalScore;
        private String grade;
        private List<String> suggestions;
        private List<String> strengths;

        public double getComplianceScore() { return complianceScore; }
        public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

        public double getLinguisticScore() { return linguisticScore; }
        public void setLinguisticScore(double linguisticScore) { this.linguisticScore = linguisticScore; }

        public double getPersuasivenessScore() { return persuasivenessScore; }
        public void setPersuasivenessScore(double persuasivenessScore) { this.persuasivenessScore = persuasivenessScore; }

        public double getCompletenessScore() { return completenessScore; }
        public void setCompletenessScore(double completenessScore) { this.completenessScore = completenessScore; }

        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

        public List<String> getStrengths() { return strengths; }
        public void setStrengths(List<String> strengths) { this.strengths = strengths; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("complianceScore", complianceScore);
            map.put("linguisticScore", linguisticScore);
            map.put("persuasivenessScore", persuasivenessScore);
            map.put("completenessScore", completenessScore);
            map.put("totalScore", totalScore);
            map.put("grade", grade);
            map.put("suggestions", suggestions);
            map.put("strengths", strengths);
            return map;
        }
    }
}
