package com.fbadsautomation.service;

import com.fbadsautomation.dto.PromptValidationResponse;
import com.fbadsautomation.dto.PromptValidationResponse.ValidationIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromptValidationService {

    private static final Logger log = LoggerFactory.getLogger(PromptValidationService.class);

    private static final int MIN_EXCELLENT_SCORE = 80;
    private static final int MIN_GOOD_SCORE = 60;
    private static final int MIN_FAIR_SCORE = 40;

    @Cacheable(value = "promptValidation", key = "#prompt + '_' + #adType + '_' + #language + '_' + #targetAudience + '_' + #industry", unless = "#result == null")
    public PromptValidationResponse validatePrompt(
            String prompt,
            String adType,
            String language,
            String targetAudience,
            String industry) {

        PromptValidationResponse response = new PromptValidationResponse();
        List<ValidationIssue> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int score = 100;

        // Validation: Length
        if (prompt.length() < 20) {
            score -= 30;
            issues.add(new ValidationIssue(
                "length",
                "warning",
                "Prompt is too short",
                "Add more details about your product, target audience, and desired outcome. Aim for at least 50 characters."
            ));
        } else if (prompt.length() > 500) {
            score -= 10;
            issues.add(new ValidationIssue(
                "length",
                "info",
                "Prompt is quite long",
                "Consider focusing on key points. Very long prompts may dilute the main message."
            ));
        }

        // Validation: Specificity
        if (!containsProductInfo(prompt)) {
            score -= 20;
            issues.add(new ValidationIssue(
                "specificity",
                "warning",
                "Missing product/service information",
                "Include what you're selling or promoting."
            ));
        }

        if (targetAudience == null || targetAudience.trim().isEmpty()) {
            score -= 15;
            suggestions.add("Specify your target audience for better ad personalization");
        }

        // Validation: Clarity
        if (containsUnclearTerms(prompt)) {
            score -= 10;
            issues.add(new ValidationIssue(
                "clarity",
                "info",
                "Vague terms detected",
                "Replace generic terms like 'great', 'amazing', 'best' with specific benefits or metrics."
            ));
        }

        // Validation: Call-to-action intent
        if (!containsCTAIntent(prompt)) {
            score -= 15;
            suggestions.add("Mention desired user action (e.g., 'encourage sign-ups', 'drive purchases')");
        }

        // Validation: Emotional appeal
        if (!containsEmotionalElement(prompt)) {
            suggestions.add("Consider adding emotional triggers or benefits that resonate with your audience");
        }

        // Additional suggestions based on ad type
        if ("WEBSITE_CONVERSION_AD".equals(adType)) {
            suggestions.add("For conversion ads, emphasize urgency and clear value proposition");
        } else if ("LEAD_FORM_AD".equals(adType)) {
            suggestions.add("For lead generation, highlight what users will receive and keep form friction low");
        }

        // Generate improved prompt (basic version)
        String improvedPrompt = generateImprovedPrompt(prompt, issues, adType, targetAudience);

        // Set quality level
        String qualityLevel;
        if (score >= MIN_EXCELLENT_SCORE) {
            qualityLevel = "excellent";
        } else if (score >= MIN_GOOD_SCORE) {
            qualityLevel = "good";
        } else if (score >= MIN_FAIR_SCORE) {
            qualityLevel = "fair";
        } else {
            qualityLevel = "poor";
        }

        response.setValid(score >= MIN_FAIR_SCORE);
        response.setQualityScore(Math.max(0, score));
        response.setQualityLevel(qualityLevel);
        response.setIssues(issues);
        response.setSuggestions(suggestions);
        response.setImprovedPrompt(improvedPrompt);

        log.debug("Prompt validation completed: score={}, level={}, issues={}",
                  score, qualityLevel, issues.size());

        return response;
    }

    private boolean containsProductInfo(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("product") || lower.contains("service") ||
               lower.contains("app") || lower.contains("software") ||
               lower.contains("sell") || lower.contains("offer") ||
               lower.contains("game") || lower.contains("course") ||
               lower.contains("book") || lower.contains("tool");
    }

    private boolean containsUnclearTerms(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("great") || lower.contains("amazing") ||
               lower.contains("best") || lower.contains("awesome");
    }

    private boolean containsCTAIntent(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("sign up") || lower.contains("buy") ||
               lower.contains("purchase") || lower.contains("download") ||
               lower.contains("subscribe") || lower.contains("learn more") ||
               lower.contains("get started") || lower.contains("try") ||
               lower.contains("register") || lower.contains("join");
    }

    private boolean containsEmotionalElement(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("feel") || lower.contains("love") ||
               lower.contains("enjoy") || lower.contains("happy") ||
               lower.contains("excited") || lower.contains("relief") ||
               lower.contains("success") || lower.contains("achieve") ||
               lower.contains("dream") || lower.contains("passion");
    }

    private String generateImprovedPrompt(String original, List<ValidationIssue> issues,
                                         String adType, String targetAudience) {
        // Enhanced rule-based improvement with AI-ready structure
        StringBuilder improved = new StringBuilder();

        // Start with original content
        improved.append(original);

        // Add punctuation if missing
        if (!original.endsWith(".") && !original.endsWith("!") && !original.endsWith("?")) {
            improved.append(".");
        }

        // Add target audience context if missing and provided
        if (targetAudience != null && !targetAudience.trim().isEmpty() &&
            !original.toLowerCase().contains("target") &&
            !original.toLowerCase().contains("audience")) {
            improved.append(" Targeting: ").append(targetAudience).append(".");
        }

        // Add CTA suggestion if missing
        if (!containsCTAIntent(original)) {
            improved.append(" Encourage users to take action.");
        }

        // Add specificity if missing product info
        if (!containsProductInfo(original)) {
            improved.append(" Include specific product/service benefits.");
        }

        // Only return improved version if meaningfully different
        String improvedStr = improved.toString();
        if (improvedStr.length() > original.length() + 10) {
            return improvedStr;
        }

        return null; // No significant improvement needed
    }
}
