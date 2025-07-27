package com.fbadsautomation.service;

import com.fbadsautomation.model.AdContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service

public class AIContentValidationService {

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
    ); // Maximum lengths for different content types
    private static final int MAX_HEADLINE_LENGTH = 40;
    private static final int MAX_DESCRIPTION_LENGTH = 125;
    private static final int MAX_PRIMARY_TEXT_LENGTH = 10000; // Tăng lên rất cao để bỏ giới hạn thực tế
    // Minimum quality thresholds
    private static final int MIN_WORD_COUNT = 3;
    private static final double MIN_READABILITY_SCORE = 0.3;

    /**
     * Validate and filter AI-generated content
     */
    public List<AdContent> validateAndFilterContent(List<AdContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return contents;
        }
        
        List<AdContent> validatedContents = new ArrayList<>();
        
        for (AdContent content : contents) {
            ValidationResult result = validateContent(content);
            if (result.isValid()) {
                // Apply automatic fixes if needed
                AdContent cleanedContent = applyAutomaticFixes(content);
                validatedContents.add(cleanedContent);
                log.debug("Content validated successfully: {}", content.getHeadline());
            } else {
                log.warn("Content validation failed: {} - Reasons: {}", 
                        content.getHeadline(), result.getViolations());
                // Try to fix the content automatically
                AdContent fixedContent = attemptContentFix(content, result);
                if (fixedContent != null) {
                    validatedContents.add(fixedContent);
                    log.info("Content automatically fixed: {}", fixedContent.getHeadline());
                } else {
                    log.error("Content could not be fixed and will be excluded: {}", content.getHeadline());
                }
            }
        }
        
        return validatedContents;
    }
    
    /**
     * Validate individual content piece
     */
    public ValidationResult validateContent(AdContent content) {
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
            violations.addAll(validateText(content.getHeadline(), "headline", MAX_HEADLINE_LENGTH));
        } else {
            violations.add("Headline is missing");
        }
        
        // Validate description
        if (content.getDescription() != null) {
            violations.addAll(validateText(content.getDescription(), "description", MAX_DESCRIPTION_LENGTH));
        }
        
        // Validate primary text
        if (content.getPrimaryText() != null) {
            violations.addAll(validateText(content.getPrimaryText(), "primary text", MAX_PRIMARY_TEXT_LENGTH));
        }
        
        // Check overall content quality
        violations.addAll(validateContentQuality(content));
        
        result.setValid(violations.isEmpty());
        result.setViolations(violations);
        result.setSeverity(calculateSeverity(violations));
        
        return result;
    }
    
    /**
     * Validate text content
     */
    private List<String> validateText(String text, String fieldName, int maxLength) {
        List<String> violations = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            violations.add(fieldName + " is empty");
            return violations;
        }
        
        String cleanText = text.trim().toLowerCase(); // Check length
        if (text.length() > maxLength) {
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
        // Check for coherence between fields
        if (content.getHeadline() != null && content.getPrimaryText() != null) {
            double similarity = calculateTextSimilarity(content.getHeadline(), content.getPrimaryText());
            if (similarity < 0.1) {
                violations.add("Content fields seem unrelated");
            }
        }
        // Check readability
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
     * Calculate readability score (simplified)
     */
    private double calculateReadabilityScore(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        String[] sentences = text.split("[.!?]+");
        String[] words = text.split("\\s+");
        if (sentences.length == 0 || words.length == 0) return 0.0;
        double avgWordsPerSentence = (double) words.length / sentences.length; // Simple readability: prefer shorter sentences
        return Math.max(0.0, 1.0 - (avgWordsPerSentence - 10) / 20);
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
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> violations = new ArrayList<>();
        private int severity;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getViolations() { return violations; }
        public void setViolations(List<String> violations) { this.violations = violations; }
        
        public int getSeverity() { return severity; }
        public void setSeverity(int severity) { this.severity = severity; }
    }
}
