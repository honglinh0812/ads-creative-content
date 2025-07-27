package com.fbadsautomation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class OptimizationResponse {
    private List<Recommendation> recommendations;
    private OptimizationSummary summary;
    private LocalDateTime generatedAt;

    // Constructors
    public OptimizationResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    public OptimizationResponse(List<Recommendation> recommendations, OptimizationSummary summary) {
        this.recommendations = recommendations;
        this.summary = summary;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations;
    }

    public OptimizationSummary getSummary() { return summary; }
    public void setSummary(OptimizationSummary summary) { this.summary = summary;
    }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt;
    }

    // Inner Classes
    public static class Recommendation {
        private String id;
        private RecommendationType type;
        private Priority priority;
        private String title;
        private String description;
        private String reasoning;
        private Map<String, Object> context;
        private List<Action> actions;
        private Impact expectedImpact;
        private String targetEntity; // Campaign ID, Ad ID, etc.
        private String targetEntityName;
        private LocalDateTime createdDate;
        private LocalDateTime expiresAt;
        private RecommendationStatus status;
        private String category;
        private List<String> tags;

        public Recommendation() {
            this.createdDate = LocalDateTime.now();
        this.status = RecommendationStatus.PENDING;
        }

        public Recommendation(String id, RecommendationType type, Priority priority, String title, 
                            String description, String reasoning, Map<String, Object> context,
                            List<Action> actions, Impact expectedImpact, String targetEntity, 
                            String targetEntityName) {
            this();
            this.id = id;
            this.type = type;
            this.priority = priority;
            this.title = title;
            this.description = description;
            this.reasoning = reasoning;
            this.context = context;
            this.actions = actions;
            this.expectedImpact = expectedImpact;
            this.targetEntity = targetEntity;
            this.targetEntityName = targetEntityName;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id;
    }

        public RecommendationType getType() { return type; }
        public void setType(RecommendationType type) { this.type = type;
    }

        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority;
    }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title;
    }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description;
    }

        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning;
    }

        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context;
    }

        public List<Action> getActions() { return actions; }
        public void setActions(List<Action> actions) { this.actions = actions;
    }

        public Impact getExpectedImpact() { return expectedImpact; }
        public void setExpectedImpact(Impact expectedImpact) { this.expectedImpact = expectedImpact;
    }

        public String getTargetEntity() { return targetEntity; }
        public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity;
    }

        public String getTargetEntityName() { return targetEntityName; }
        public void setTargetEntityName(String targetEntityName) { this.targetEntityName = targetEntityName;
    }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate;
    }

        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt;
    }

        public RecommendationStatus getStatus() { return status; }
        public void setStatus(RecommendationStatus status) { this.status = status;
    }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category;
    }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags;
    }
    }

    public static class Action {
        private String type;
        private String description;
        private Map<String, Object> parameters;
        private boolean requiresConfirmation;
        private String confirmationMessage;

        public Action() {;
    }

        public Action(String type, String description, Map<String, Object> parameters, 
                     boolean requiresConfirmation, String confirmationMessage) {
            this.type = type;
            this.description = description;
            this.parameters = parameters;
            this.requiresConfirmation = requiresConfirmation;
            this.confirmationMessage = confirmationMessage;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type;
    }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description;
    }

        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters;
    }

        public boolean isRequiresConfirmation() { return requiresConfirmation; }
        public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation;
    }

        public String getConfirmationMessage() { return confirmationMessage; }
        public void setConfirmationMessage(String confirmationMessage) { this.confirmationMessage = confirmationMessage;
    }
    }

    public static class Impact {
        private String metric;
        private double expectedChange;
        private String changeType; // "increase", "decrease", "optimize"
        private String timeframe;
        private double confidence;
        private Map<String, Double> additionalMetrics;

        public Impact() {;
    }

        public Impact(String metric, double expectedChange, String changeType, 
                     String timeframe, double confidence, Map<String, Double> additionalMetrics) {
            this.metric = metric;
            this.expectedChange = expectedChange;
            this.changeType = changeType;
            this.timeframe = timeframe;
            this.confidence = confidence;
            this.additionalMetrics = additionalMetrics;
        }

        // Getters and Setters
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric;
    }

        public double getExpectedChange() { return expectedChange; }
        public void setExpectedChange(double expectedChange) { this.expectedChange = expectedChange;
    }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType;
    }

        public String getTimeframe() { return timeframe; }
        public void setTimeframe(String timeframe) { this.timeframe = timeframe;
    }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence;
    }

        public Map<String, Double> getAdditionalMetrics() { return additionalMetrics; }
        public void setAdditionalMetrics(Map<String, Double> additionalMetrics) { this.additionalMetrics = additionalMetrics;
    }
    }

    public static class OptimizationSummary {
        private int totalRecommendations;
        private int highPriorityRecommendations;
        private int mediumPriorityRecommendations;
        private int lowPriorityRecommendations;
        private Map<RecommendationType, Integer> recommendationsByType;
        private double totalPotentialImpact;
        private String topRecommendationCategory;
        private int implementableRecommendations;
        private double averageConfidence;

        public OptimizationSummary() {;
    }

        public OptimizationSummary(int totalRecommendations, int highPriorityRecommendations,
                                 int mediumPriorityRecommendations, int lowPriorityRecommendations,
                                 Map<RecommendationType, Integer> recommendationsByType,
                                 double totalPotentialImpact, String topRecommendationCategory,
                                 int implementableRecommendations, double averageConfidence) {
            this.totalRecommendations = totalRecommendations;
            this.highPriorityRecommendations = highPriorityRecommendations;
            this.mediumPriorityRecommendations = mediumPriorityRecommendations;
            this.lowPriorityRecommendations = lowPriorityRecommendations;
            this.recommendationsByType = recommendationsByType;
            this.totalPotentialImpact = totalPotentialImpact;
            this.topRecommendationCategory = topRecommendationCategory;
            this.implementableRecommendations = implementableRecommendations;
            this.averageConfidence = averageConfidence;
        }

        // Getters and Setters
        public int getTotalRecommendations() { return totalRecommendations; }
        public void setTotalRecommendations(int totalRecommendations) { this.totalRecommendations = totalRecommendations;
    }

        public int getHighPriorityRecommendations() { return highPriorityRecommendations; }
        public void setHighPriorityRecommendations(int highPriorityRecommendations) { this.highPriorityRecommendations = highPriorityRecommendations;
    }

        public int getMediumPriorityRecommendations() { return mediumPriorityRecommendations; }
        public void setMediumPriorityRecommendations(int mediumPriorityRecommendations) { this.mediumPriorityRecommendations = mediumPriorityRecommendations;
    }

        public int getLowPriorityRecommendations() { return lowPriorityRecommendations; }
        public void setLowPriorityRecommendations(int lowPriorityRecommendations) { this.lowPriorityRecommendations = lowPriorityRecommendations;
    }

        public Map<RecommendationType, Integer> getRecommendationsByType() { return recommendationsByType; }
        public void setRecommendationsByType(Map<RecommendationType, Integer> recommendationsByType) { this.recommendationsByType = recommendationsByType;
    }

        public double getTotalPotentialImpact() { return totalPotentialImpact; }
        public void setTotalPotentialImpact(double totalPotentialImpact) { this.totalPotentialImpact = totalPotentialImpact;
    }

        public String getTopRecommendationCategory() { return topRecommendationCategory; }
        public void setTopRecommendationCategory(String topRecommendationCategory) { this.topRecommendationCategory = topRecommendationCategory;
    }

        public int getImplementableRecommendations() { return implementableRecommendations; }
        public void setImplementableRecommendations(int implementableRecommendations) { this.implementableRecommendations = implementableRecommendations;
    }

        public double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; };
    }

    // Enums
    public enum RecommendationType {
        BUDGET_REALLOCATION,
        AI_PROVIDER_SWITCH,
        CAMPAIGN_OBJECTIVE_OPTIMIZATION,
        AD_SCHEDULING,
        CONTENT_TYPE_OPTIMIZATION,
        CAMPAIGN_PAUSE,
        CAMPAIGN_SCALE,
        AD_CREATIVE_REFRESH,
        TARGETING_OPTIMIZATION,
        BID_STRATEGY_OPTIMIZATION;
    }

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW;
    }

    public enum RecommendationStatus {
        PENDING,
        ACCEPTED,
        DISMISSED,
        IMPLEMENTED,
        SCHEDULED,
        EXPIRED;
    }
}
