package com.fbadsautomation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO used by the simplified optimization workflow. It bundles the scorecard
 * (driven by our quality scoring service) and the human-friendly suggestions
 * grouped per UX category so the frontend can render collapsible cards without
 * extra mapping logic.
 */
public class AdOptimizationInsightDTO {

    private Long adId;
    private String adName;
    private String campaignName;
    private String adType;
    private String callToAction;
    private boolean hasImage;
    private LocalDateTime createdDate;
    private LengthMetrics lengthMetrics;
    private Scorecard scorecard;
    private Map<String, List<String>> suggestions;
    private boolean saved;
    private AdCopyReviewDTO copyReview;
    private PersonaContext persona;
    private String language;

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

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LengthMetrics getLengthMetrics() {
        return lengthMetrics;
    }

    public void setLengthMetrics(LengthMetrics lengthMetrics) {
        this.lengthMetrics = lengthMetrics;
    }

    public Scorecard getScorecard() {
        return scorecard;
    }

    public void setScorecard(Scorecard scorecard) {
        this.scorecard = scorecard;
    }

    public Map<String, List<String>> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(Map<String, List<String>> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public AdCopyReviewDTO getCopyReview() {
        return copyReview;
    }

    public void setCopyReview(AdCopyReviewDTO copyReview) {
        this.copyReview = copyReview;
    }

    public PersonaContext getPersona() {
        return persona;
    }

    public void setPersona(PersonaContext persona) {
        this.persona = persona;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    // Inner DTOs -------------------------------------------------------------

    public static class Scorecard {
        private double compliance;
        private double linguistic;
        private double persuasiveness;
        private double completeness;
        private double total;
        private String grade;

        public double getCompliance() {
            return compliance;
        }

        public void setCompliance(double compliance) {
            this.compliance = compliance;
        }

        public double getLinguistic() {
            return linguistic;
        }

        public void setLinguistic(double linguistic) {
            this.linguistic = linguistic;
        }

        public double getPersuasiveness() {
            return persuasiveness;
        }

        public void setPersuasiveness(double persuasiveness) {
            this.persuasiveness = persuasiveness;
        }

        public double getCompleteness() {
            return completeness;
        }

        public void setCompleteness(double completeness) {
            this.completeness = completeness;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }
    }

    public static class LengthMetrics {
        private int headlineLength;
        private int descriptionLength;
        private int primaryTextLength;

        public LengthMetrics() {}

        public LengthMetrics(int headlineLength, int descriptionLength, int primaryTextLength) {
            this.headlineLength = headlineLength;
            this.descriptionLength = descriptionLength;
            this.primaryTextLength = primaryTextLength;
        }

        public int getHeadlineLength() {
            return headlineLength;
        }

        public void setHeadlineLength(int headlineLength) {
            this.headlineLength = headlineLength;
        }

        public int getDescriptionLength() {
            return descriptionLength;
        }

        public void setDescriptionLength(int descriptionLength) {
            this.descriptionLength = descriptionLength;
        }

        public int getPrimaryTextLength() {
            return primaryTextLength;
        }

        public void setPrimaryTextLength(int primaryTextLength) {
            this.primaryTextLength = primaryTextLength;
        }
    }

    public static class PersonaContext {
        private Long id;
        private String name;
        private Integer age;
        private String gender;
        private String tone;
        private List<String> interests;
        private List<String> painPoints;
        private String desiredOutcome;
        private String description;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getTone() {
            return tone;
        }

        public void setTone(String tone) {
            this.tone = tone;
        }

        public List<String> getInterests() {
            return interests;
        }

        public void setInterests(List<String> interests) {
            this.interests = interests;
        }

        public List<String> getPainPoints() {
            return painPoints;
        }

        public void setPainPoints(List<String> painPoints) {
            this.painPoints = painPoints;
        }

        public String getDesiredOutcome() {
            return desiredOutcome;
        }

        public void setDesiredOutcome(String desiredOutcome) {
            this.desiredOutcome = desiredOutcome;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
