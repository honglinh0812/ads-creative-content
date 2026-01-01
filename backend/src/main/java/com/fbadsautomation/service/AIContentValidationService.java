package com.fbadsautomation.service;

import com.fbadsautomation.config.ValidationConfig;
import com.fbadsautomation.model.AdContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIContentValidationService {

    private static final Logger log = LoggerFactory.getLogger(AIContentValidationService.class);

    private final ValidationConfig config;
    private final ObjectMapper objectMapper;

    @Autowired
    public AIContentValidationService(ValidationConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    // Inappropriate content patterns
    private static final List<String> INAPPROPRIATE_KEYWORDS = Arrays.asList("hate", "violence", "discrimination", "illegal", "drugs", "weapons",
        "gambling", "adult", "explicit", "offensive", "scam", "fraud"
    ); // Facebook advertising policy violations
    private static final List<String> POLICY_VIOLATIONS = Arrays.asList("before and after", "miracle", "guaranteed results", "lose weight fast",
        "get rich quick", "work from home", "make money fast", "free money",
        "click here", "act now", "limited time", "urgent"
    );
    // Spam indicators
    private static final List<String> SPAM_INDICATORS = Arrays.asList("!!!", "???", "FREE!!!", "URGENT!!!", "ACT NOW!!!", "LIMITED TIME!!!",
        "CLICK HERE NOW", "AMAZING OFFER", "DON'T MISS OUT"
    );     // Maximum lengths for different content types - Updated to match Facebook actual limits
    private static final int MAX_HEADLINE_LENGTH = 40; // Facebook limit: 40 characters
    private static final int MAX_DESCRIPTION_LENGTH = 125; // Facebook limit: 125 characters (not 30!)
    private static final int MAX_PRIMARY_TEXT_LENGTH = 1000; // Facebook limit: 1000+ characters (not 125!)
    // Minimum quality thresholds - Relaxed for better content acceptance
    private static final int MIN_WORD_COUNT = 2; // Reduced from 3 to 2
    private static final double MIN_READABILITY_SCORE = 0.1; // Reduced from 0.3 to 0.1

    /**
     * Validate and filter AI-generated content with quality scoring
     */
    public List<AdContent> validateAndFilterContent(List<AdContent> contents) {
        return validateAndFilterContent(contents, true);
    }

    public List<AdContent> validateAndFilterContent(List<AdContent> contents, boolean enforceLengthLimits) {
        if (contents == null || contents.isEmpty()) {
            return contents;
        }

        List<AdContent> validatedContents = new ArrayList<>();

        for (AdContent content : contents) {
            ValidationResult result = validateContent(content, enforceLengthLimits);

            // Attach quality metrics to content
            content.setQualityScore(result.getQualityScore());
            content.setHasWarnings(!result.getViolations().isEmpty());

            // Convert violations list to JSON string for storage
            if (!result.getViolations().isEmpty()) {
                try {
                    String warningsJson = objectMapper.writeValueAsString(result.getViolations());
                    content.setValidationWarnings(warningsJson);
                } catch (Exception e) {
                    log.error("Failed to serialize validation warnings", e);
                    content.setValidationWarnings(String.join(", ", result.getViolations()));
                }
            }

            if (result.isValid()) {
                // Apply automatic fixes if needed
                AdContent cleanedContent = applyAutomaticFixes(content);
                cleanedContent.setQualityScore(result.getQualityScore());
                cleanedContent.setHasWarnings(false);
                validatedContents.add(cleanedContent);
                log.debug("Content validated successfully: {} (score: {})", content.getHeadline(), result.getQualityScore());
            } else {
                // Check if quality score meets minimum threshold
                if (config.isAllowPartialResults() && result.getQualityScore() >= config.getMinQualityScore()) {
                    // Accept content with warnings if it meets minimum quality
                    log.warn("Content has validation warnings but meets minimum quality (score: {}): {}",
                            result.getQualityScore(), content.getHeadline());
                    validatedContents.add(content);
                } else {
                    // Try to fix the content automatically
                    log.warn("Content validation failed: {} (score: {}) - Reasons: {}",
                            content.getHeadline(), result.getQualityScore(), result.getViolations());

                    AdContent fixedContent = attemptContentFix(content, result);
                    if (fixedContent != null) {
                        // Re-validate fixed content
                        ValidationResult fixedResult = validateContent(fixedContent, enforceLengthLimits);
                        fixedContent.setQualityScore(fixedResult.getQualityScore());
                        fixedContent.setHasWarnings(!fixedResult.getViolations().isEmpty());

                        if (!fixedResult.getViolations().isEmpty()) {
                            try {
                                String warningsJson = objectMapper.writeValueAsString(fixedResult.getViolations());
                                fixedContent.setValidationWarnings(warningsJson);
                            } catch (Exception e) {
                                fixedContent.setValidationWarnings(String.join(", ", fixedResult.getViolations()));
                            }
                        }

                        validatedContents.add(fixedContent);
                        log.info("Content automatically fixed: {} (score: {})", fixedContent.getHeadline(), fixedResult.getQualityScore());
                    } else {
                        // If configured to allow partial results, include even failed content
                        if (config.isAllowPartialResults() && !config.isStrictMode()) {
                            log.warn("Content could not be fixed but including due to allow-partial-results: {}", content.getHeadline());
                            validatedContents.add(content);
                        } else {
                            log.error("Content could not be fixed and will be excluded: {}", content.getHeadline());
                        }
                    }
                }
            }
        }

        return validatedContents;
    }
    
    /**
     * Validate individual content piece
     */
    public ValidationResult validateContent(AdContent content) {
        return validateContent(content, true);
    }

    public ValidationResult validateContent(AdContent content, boolean enforceLengthLimits) {
        ValidationResult result = new ValidationResult();
        List<String> violations = new ArrayList<>();
        
        // Check for null or empty content
        if (content == null) {
            violations.add("Content is null");
            result.setValid(false);
            result.setViolations(violations);
            return result;
        }
        
        // Validate headline
        if (content.getHeadline() != null) {
            violations.addAll(validateText(content.getHeadline(), "headline", MAX_HEADLINE_LENGTH, enforceLengthLimits));
        } else {
            violations.add("Headline is missing");
        }
        
        // Validate description
        if (content.getDescription() != null) {
            violations.addAll(validateText(content.getDescription(), "description", MAX_DESCRIPTION_LENGTH, enforceLengthLimits));
        }
        
        // Validate primary text
        if (content.getPrimaryText() != null) {
            violations.addAll(validateText(content.getPrimaryText(), "primary text", MAX_PRIMARY_TEXT_LENGTH, enforceLengthLimits));
        }
        
        // Check overall content quality
        violations.addAll(validateContentQuality(content));

        result.setValid(violations.isEmpty());
        result.setViolations(violations);
        result.setSeverity(calculateSeverity(violations));
        result.setQualityScore(calculateQualityScore(violations, result.getSeverity()));
        result.setSuggestions(generateSuggestions(violations));
        result.setIssues(convertViolationsToIssues(violations));

        return result;
    }
    
    /**
     * Validate text content
     */
    private List<String> validateText(String text, String fieldName, int maxLength, boolean enforceLengthLimits) {
        List<String> violations = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            violations.add(fieldName + " is empty");
            return violations;
        }
        
        String cleanText = text.trim().toLowerCase(); // Check length
        if (enforceLengthLimits && text.length() > maxLength) {
            violations.add(fieldName + " exceeds maximum length of " + maxLength + " characters");
        }
        
        // Check minimum word count
        String[] words = text.trim().split("\\s+");
        if (words.length < MIN_WORD_COUNT) {
            violations.add(fieldName + " has too few words (minimum " + MIN_WORD_COUNT + ")");
        }
        
        // Check for inappropriate content
        for (String keyword : INAPPROPRIATE_KEYWORDS) {
            if (cleanText.contains(keyword.toLowerCase())) {
                violations.add(fieldName + " contains inappropriate content: " + keyword);
            }
        }
        
        // Check for policy violations
        for (String violation : POLICY_VIOLATIONS) {
            if (cleanText.contains(violation.toLowerCase())) {
                violations.add(fieldName + " may violate Facebook advertising policies: " + violation);
            }
        }
        
        // Check for spam indicators
        for (String spam : SPAM_INDICATORS) {
            if (text.contains(spam)) {
                violations.add(fieldName + " contains spam-like content: " + spam);
            }
        }
        
        // Check for excessive punctuation
        if (text.matches(".*[!?]{3,}.*")) {
            violations.add(fieldName + " contains excessive punctuation");
        }
        
        // Check for all caps (more than 50% of text)
        long upperCaseCount = text.chars().filter(Character::isUpperCase).count();
        long letterCount = text.chars().filter(Character::isLetter).count();
        if (letterCount > 0 && (double) upperCaseCount / letterCount > 0.5) {
            violations.add(fieldName + " contains too much uppercase text");
        }
        
        return violations;
    }
    
    /**
     * Validate overall content quality
     */
    private List<String> validateContentQuality(AdContent content) {
        List<String> violations = new ArrayList<>(); // Check for duplicate content across fields
        if (content.getHeadline() != null && content.getDescription() != null &&
            content.getHeadline().equals(content.getDescription())) {
            violations.add("Headline and description are identical");
        }
        // Check for coherence between fields - Relaxed threshold
        if (content.getHeadline() != null && content.getPrimaryText() != null) {
            double similarity = calculateTextSimilarity(content.getHeadline(), content.getPrimaryText());
            if (similarity < 0.05) { // Reduced from 0.1 to 0.05 - more lenient
                violations.add("Content fields seem unrelated");
            }
        }
        // Check readability - More lenient
        if (content.getPrimaryText() != null) {
            double readabilityScore = calculateReadabilityScore(content.getPrimaryText());
            if (readabilityScore < MIN_READABILITY_SCORE) {
                violations.add("Content readability is too low");
            }
        }
        return violations;
    }
    
    /**
     * Apply automatic fixes to content
     */
    private AdContent applyAutomaticFixes(AdContent content) {
        AdContent fixedContent = new AdContent(); // Copy all fields
        fixedContent.setHeadline(cleanText(content.getHeadline()));
        fixedContent.setDescription(cleanText(content.getDescription()));
        fixedContent.setPrimaryText(cleanText(content.getPrimaryText()));
        fixedContent.setImageUrl(content.getImageUrl());
        fixedContent.setCallToAction(content.getCallToAction());
        fixedContent.setCta(content.getCta());
        fixedContent.setAiProvider(content.getAiProvider());
        
        return fixedContent;
    }
    
    /**
     * Attempt to fix content automatically
     */
    private AdContent attemptContentFix(AdContent content, ValidationResult result) {
        AdContent fixedContent = new AdContent(); // Copy original content
        fixedContent.setHeadline(content.getHeadline());
        fixedContent.setDescription(content.getDescription());
        fixedContent.setPrimaryText(content.getPrimaryText());
        fixedContent.setImageUrl(content.getImageUrl());
        fixedContent.setCallToAction(content.getCallToAction());
        fixedContent.setCta(content.getCta());
        fixedContent.setAiProvider(content.getAiProvider());
        
        boolean wasFixed = false; // Fix length issues by truncating
        if (fixedContent.getHeadline() != null && fixedContent.getHeadline().length() > MAX_HEADLINE_LENGTH) {
            fixedContent.setHeadline(truncateText(fixedContent.getHeadline(), MAX_HEADLINE_LENGTH));
            wasFixed = true;
        }
        
        if (fixedContent.getDescription() != null && fixedContent.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            fixedContent.setDescription(truncateText(fixedContent.getDescription(), MAX_DESCRIPTION_LENGTH));
            wasFixed = true;
        }
        
        if (fixedContent.getPrimaryText() != null && fixedContent.getPrimaryText().length() > MAX_PRIMARY_TEXT_LENGTH) {
            fixedContent.setPrimaryText(truncateText(fixedContent.getPrimaryText(), MAX_PRIMARY_TEXT_LENGTH));
            wasFixed = true;
        }
        
        // Remove excessive punctuation
        if (fixedContent.getHeadline() != null) {
            String cleaned = fixedContent.getHeadline().replaceAll("[!?]{3,}", "!");
            if (!cleaned.equals(fixedContent.getHeadline())) {
                fixedContent.setHeadline(cleaned);
                wasFixed = true;
            }
        }
        
        // Convert excessive uppercase to title case
        if (fixedContent.getHeadline() != null) {
            String cleaned = convertExcessiveUppercase(fixedContent.getHeadline());
            if (!cleaned.equals(fixedContent.getHeadline())) {
                fixedContent.setHeadline(cleaned);
                wasFixed = true;
            }
        }
        
        // If we made fixes, validate again
        if (wasFixed) {
            ValidationResult newResult = validateContent(fixedContent);
            if (newResult.isValid() || newResult.getSeverity() < result.getSeverity()) {
                return fixedContent;
            }
        }
        
        return null; // Could not fix;
    }
    
    /**
     * Clean text by removing extra whitespace and normalizing
     */
    private String cleanText(String text) {
        if (text == null) return null;
        return text.trim()
                  .replaceAll("\\s+", " ") // Replace multiple spaces with single space
                  .replaceAll("([.!?])\\1+", "$1"); // Remove repeated punctuation
    }
    
    /**
     * Truncate text while preserving word boundaries
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        String truncated = text.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxLength * 0.8) { // If we can preserve most of the text
            return truncated.substring(0, lastSpace) + "...";
        } else {
            return truncated + "...";
        }
    }
    
    /**
     * Convert excessive uppercase to title case
     */
    private String convertExcessiveUppercase(String text) {
        if (text == null) return null;
        long upperCaseCount = text.chars().filter(Character::isUpperCase).count();
        long letterCount = text.chars().filter(Character::isLetter).count();
        if (letterCount > 0 && (double) upperCaseCount / letterCount > 0.5) {
            // Convert to title case
            String[] words = text.toLowerCase().split("\\s+");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1))
                          .append(" ");
                }
            }
            
            return result.toString().trim();
        }
        
        return text;
    }
    
    /**
     * Calculate text similarity (simple implementation)
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
    
    /**
     * Calculate readability score (simplified) - More lenient calculation
     */
    private double calculateReadabilityScore(String text) {
        if (text == null || text.trim().isEmpty()) return 0.5; // Default to acceptable score
        String[] sentences = text.split("[.!?]+");
        String[] words = text.split("\\s+");
        if (sentences.length == 0 || words.length == 0) return 0.5; // Default to acceptable score
        
        double avgWordsPerSentence = (double) words.length / sentences.length;
        // More lenient readability calculation - accept longer sentences
        double score = Math.max(0.0, 1.0 - (avgWordsPerSentence - 15) / 30); // Increased threshold from 10 to 15
        return Math.max(0.1, score); // Ensure minimum score of 0.1
    }
    
    /**
     * Calculate violation severity
     */
    private int calculateSeverity(List<String> violations) {
        int severity = 0;
        for (String violation : violations) {
            if (violation.contains("inappropriate") || violation.contains("policy")) {
                severity += 3; // High severity
            } else if (violation.contains("spam") || violation.contains("excessive")) {
                severity += 2; // Medium severity
            } else {
                severity += 1; // Low severity
            }
        }
        return severity;
    }

    /**
     * Calculate quality score (0-100) based on violations and severity
     */
    private int calculateQualityScore(List<String> violations, int severity) {
        if (violations.isEmpty()) {
            return 100; // Perfect score
        }

        // Start with 100 and deduct points based on severity
        int score = 100;
        score -= Math.min(severity * 10, 80); // Max deduction: 80 points
        score -= violations.size() * 2; // 2 points per violation

        return Math.max(0, score); // Ensure score is never negative
    }

    /**
     * Generate helpful suggestions based on violations
     */
    private List<String> generateSuggestions(List<String> violations) {
        List<String> suggestions = new ArrayList<>();

        for (String violation : violations) {
            if (violation.contains("exceeds maximum length")) {
                suggestions.add("Shorten the text to meet Facebook's character limits");
            } else if (violation.contains("inappropriate content")) {
                suggestions.add("Remove or rephrase content that may violate advertising policies");
            } else if (violation.contains("policy")) {
                suggestions.add("Avoid claims like 'guaranteed results' or 'miracle' that violate Facebook policies");
            } else if (violation.contains("spam")) {
                suggestions.add("Reduce excessive punctuation and all-caps text");
            } else if (violation.contains("too few words")) {
                suggestions.add("Add more descriptive content to improve engagement");
            } else if (violation.contains("unrelated")) {
                suggestions.add("Ensure all content fields are coherent and related to your ad message");
            }
        }

        // Remove duplicates
        return new ArrayList<>(new java.util.LinkedHashSet<>(suggestions));
    }

    /**
     * Convert violations to detailed ValidationIssue objects
     */
    private List<ValidationIssue> convertViolationsToIssues(List<String> violations) {
        List<ValidationIssue> issues = new ArrayList<>();

        for (String violation : violations) {
            String field = extractField(violation);
            String type = categorizeViolationType(violation);
            String severity = determineSeverity(violation);
            String suggestion = generateSuggestionForViolation(violation);

            issues.add(new ValidationIssue(field, type, severity, violation, suggestion));
        }

        return issues;
    }

    /**
     * Extract field name from violation message
     */
    private String extractField(String violation) {
        if (violation.contains("headline")) return "headline";
        if (violation.contains("description")) return "description";
        if (violation.contains("primary text")) return "primaryText";
        return "general";
    }

    /**
     * Categorize violation type
     */
    private String categorizeViolationType(String violation) {
        if (violation.contains("length") || violation.contains("too few words")) return "length";
        if (violation.contains("inappropriate")) return "inappropriate";
        if (violation.contains("policy")) return "policy";
        if (violation.contains("spam") || violation.contains("excessive")) return "spam";
        if (violation.contains("readability") || violation.contains("unrelated")) return "quality";
        return "other";
    }

    /**
     * Determine issue severity level
     */
    private String determineSeverity(String violation) {
        if (violation.contains("inappropriate") || violation.contains("policy")) {
            return "error";
        } else if (violation.contains("spam") || violation.contains("excessive")) {
            return "warning";
        } else {
            return "info";
        }
    }

    /**
     * Generate specific suggestion for a violation
     */
    private String generateSuggestionForViolation(String violation) {
        if (violation.contains("exceeds maximum length")) {
            return "Shorten text while preserving key message";
        } else if (violation.contains("inappropriate")) {
            return "Remove or rephrase prohibited content";
        } else if (violation.contains("policy")) {
            return "Avoid superlatives and unverifiable claims";
        } else if (violation.contains("spam")) {
            return "Use standard punctuation and mixed case";
        } else if (violation.contains("too few words")) {
            return "Add more descriptive details";
        } else if (violation.contains("unrelated")) {
            return "Align content across all fields";
        } else {
            return "Review and improve content quality";
        }
    }

    /**
     * Validation result class with detailed feedback
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> violations = new ArrayList<>();
        private List<ValidationIssue> issues = new ArrayList<>();
        private int severity;
        private int qualityScore; // 0-100
        private List<String> suggestions = new ArrayList<>();

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }

        public List<ValidationIssue> getIssues() { return issues; }
        public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }

        public int getSeverity() { return severity; }
        public void setSeverity(int severity) { this.severity = severity; }

        public int getQualityScore() { return qualityScore; }
        public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }

    /**
     * Detailed validation issue
     */
    public static class ValidationIssue {
        private String field; // headline, description, primaryText
        private String type; // length, policy, spam, quality
        private String severity; // error, warning, info
        private String message;
        private String suggestion;

        public ValidationIssue(String field, String type, String severity, String message, String suggestion) {
            this.field = field;
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.suggestion = suggestion;
        }

        // Getters
        public String getField() { return field; }
        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
    }
}
