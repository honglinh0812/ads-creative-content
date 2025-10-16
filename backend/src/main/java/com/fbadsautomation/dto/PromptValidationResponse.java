package com.fbadsautomation.dto;

import java.util.List;

public class PromptValidationResponse {

    private boolean isValid;
    private int qualityScore; // 0-100
    private String qualityLevel; // excellent, good, fair, poor
    private List<ValidationIssue> issues;
    private List<String> suggestions;
    private String improvedPrompt;

    // Constructor
    public PromptValidationResponse() {}

    public static class ValidationIssue {
        private String type; // length, specificity, clarity, tone, etc.
        private String severity; // error, warning, info
        private String message;
        private String suggestion;

        // Constructor
        public ValidationIssue() {}

        public ValidationIssue(String type, String severity, String message, String suggestion) {
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.suggestion = suggestion;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }

    // Getters and Setters
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

    public String getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(String qualityLevel) { this.qualityLevel = qualityLevel; }

    public List<ValidationIssue> getIssues() { return issues; }
    public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getImprovedPrompt() { return improvedPrompt; }
    public void setImprovedPrompt(String improvedPrompt) { this.improvedPrompt = improvedPrompt; }
}
