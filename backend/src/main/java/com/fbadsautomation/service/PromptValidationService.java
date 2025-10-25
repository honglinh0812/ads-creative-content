package com.fbadsautomation.service;

import com.fbadsautomation.dto.PromptValidationResponse;
import com.fbadsautomation.dto.PromptValidationResponse.ValidationIssue;
import com.fbadsautomation.util.ValidationMessages;
import com.fbadsautomation.util.ValidationMessages.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromptValidationService {

    private static final Logger log = LoggerFactory.getLogger(PromptValidationService.class);

    @Autowired(required = false)
    private AIPromptImprovementService aiPromptImprovementService;

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

        // Detect language from prompt text for bilingual support
        Language detectedLanguage = ValidationMessages.detectLanguage(prompt);
        log.info("Detected language for prompt validation: {}", detectedLanguage);

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
                ValidationMessages.getMessage("too_short", detectedLanguage),
                ValidationMessages.getSuggestion("add_product_details", detectedLanguage)
            ));
        } else if (prompt.length() > 500) {
            score -= 10;
            issues.add(new ValidationIssue(
                "length",
                "info",
                ValidationMessages.getMessage("too_long", detectedLanguage),
                ValidationMessages.getSuggestion("simplify", detectedLanguage)
            ));
        }

        // Validation: Specificity
        if (!containsProductInfo(prompt)) {
            score -= 20;
            issues.add(new ValidationIssue(
                "specificity",
                "warning",
                ValidationMessages.getMessage("missing_product", detectedLanguage),
                ValidationMessages.getSuggestion("add_product_details", detectedLanguage)
            ));
        }

        if (targetAudience == null || targetAudience.trim().isEmpty()) {
            score -= 15;
            suggestions.add(ValidationMessages.getSuggestion("add_target_audience", detectedLanguage));
        }

        // Validation: Clarity
        if (containsUnclearTerms(prompt)) {
            score -= 10;
            issues.add(new ValidationIssue(
                "clarity",
                "info",
                ValidationMessages.getMessage("too_vague", detectedLanguage),
                ValidationMessages.getSuggestion("be_specific", detectedLanguage)
            ));
        }

        // Validation: Call-to-action intent
        if (!containsCTAIntent(prompt)) {
            score -= 15;
            suggestions.add(ValidationMessages.getSuggestion("add_action", detectedLanguage));
        }

        // Validation: Emotional appeal
        if (!containsEmotionalElement(prompt)) {
            suggestions.add(ValidationMessages.getSuggestion("highlight_benefits", detectedLanguage));
        }

        // Additional suggestions based on ad type
        if ("WEBSITE_CONVERSION_AD".equals(adType)) {
            suggestions.add(ValidationMessages.getSuggestion("add_urgency", detectedLanguage));
        } else if ("LEAD_FORM_AD".equals(adType)) {
            suggestions.add(ValidationMessages.getSuggestion("highlight_benefits", detectedLanguage));
        }

        // Generate improved prompt using AI (if available) or fallback to rule-based
        String improvedPrompt = null;
        if (aiPromptImprovementService != null) {
            try {
                improvedPrompt = aiPromptImprovementService.generateImprovedPrompt(prompt, adType, targetAudience);
                log.debug("AI-powered improvement generated successfully");
            } catch (Exception e) {
                log.warn("AI prompt improvement failed, falling back to rule-based: {}", e.getMessage());
            }
        }

        // Fallback to rule-based if AI unavailable or failed
        if (improvedPrompt == null) {
            improvedPrompt = generateImprovedPrompt(prompt, issues, adType, targetAudience);
        }

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
        // English keywords
        boolean hasEnglish = lower.contains("product") || lower.contains("service") ||
               lower.contains("app") || lower.contains("software") ||
               lower.contains("sell") || lower.contains("offer") ||
               lower.contains("game") || lower.contains("course") ||
               lower.contains("book") || lower.contains("tool");

        // Vietnamese keywords
        boolean hasVietnamese = lower.contains("sản phẩm") || lower.contains("dịch vụ") ||
               lower.contains("ứng dụng") || lower.contains("phần mềm") ||
               lower.contains("bán") || lower.contains("cung cấp") ||
               lower.contains("trò chơi") || lower.contains("khóa học") ||
               lower.contains("sách") || lower.contains("công cụ");

        return hasEnglish || hasVietnamese;
    }

    private boolean containsUnclearTerms(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("great") || lower.contains("amazing") ||
               lower.contains("best") || lower.contains("awesome");
    }

    private boolean containsCTAIntent(String prompt) {
        String lower = prompt.toLowerCase();
        // English keywords
        boolean hasEnglish = lower.contains("sign up") || lower.contains("buy") ||
               lower.contains("purchase") || lower.contains("download") ||
               lower.contains("subscribe") || lower.contains("learn more") ||
               lower.contains("get started") || lower.contains("try") ||
               lower.contains("register") || lower.contains("join");

        // Vietnamese keywords
        boolean hasVietnamese = lower.contains("đăng ký") || lower.contains("mua") ||
               lower.contains("tải xuống") || lower.contains("tải về") ||
               lower.contains("tìm hiểu") || lower.contains("bắt đầu") ||
               lower.contains("dùng thử") || lower.contains("tham gia");

        return hasEnglish || hasVietnamese;
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
