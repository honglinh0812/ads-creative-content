package com.fbadsautomation.service;

import com.fbadsautomation.model.AdStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for building standardized image generation prompts across all AI providers.
 *
 * Implements a 6-component prompt structure:
 * a. Subject - User's original prompt content
 * b. Style - Visual keywords mapped from AdStyle
 * c. Mood and Scene - Professional advertising setting
 * d. Camera/Lens - Professional photography technical specs
 * e. Quality Tags - High-quality commercial photography attributes
 * f. Safety Guidance - Provider-specific content safety rules
 */
@Service
public class ImagePromptService {

    private static final Logger log = LoggerFactory.getLogger(ImagePromptService.class);

    /**
     * Build a standardized image generation prompt with 6 components
     *
     * @param userPrompt Original user input (Subject)
     * @param adStyle AdStyle to map to visual keywords
     * @param provider Provider name for safety guidance customization
     * @return Standardized prompt string
     */
    public String buildStandardizedPrompt(String userPrompt, AdStyle adStyle, String provider) {
        log.info("Building standardized image prompt - Style: {}, Provider: {}",
            adStyle != null ? adStyle.name() : "DEFAULT", provider);

        // Component a: Subject - Clean user prompt
        String subject = cleanSubject(userPrompt);

        // Component b: Style - AdStyle visual keywords
        String styleKeywords = getStyleKeywords(adStyle);

        // Component c: Mood and Scene - Fixed professional setting
        String moodAndScene = "professional advertising setting, well-composed scene";

        // Component d: Camera/Lens - Fixed technical specs
        String cameraSettings = "shot on professional camera, shallow depth of field, crisp focus";

        // Component e: Quality Tags - Fixed quality attributes
        String qualityTags = "high detail, 4k resolution, commercial photography, vibrant colors, professional composition";

        // Component f: Safety Guidance - Provider-specific
        String safetyGuidance = getSafetyGuidance(provider);

        // Build final prompt
        String standardizedPrompt = String.format(
            "Create a single high quality image: %s, %s, %s, %s, %s, %s",
            subject,
            styleKeywords,
            moodAndScene,
            cameraSettings,
            qualityTags,
            safetyGuidance
        );

        log.debug("Standardized prompt built (length: {}): {}",
            standardizedPrompt.length(),
            standardizedPrompt.length() > 150 ? standardizedPrompt.substring(0, 150) + "..." : standardizedPrompt);

        return standardizedPrompt;
    }

    /**
     * Clean and prepare the subject from user prompt
     * Removes instruction words and limits length
     */
    private String cleanSubject(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return "professional advertisement product";
        }

        // Remove common instruction words
        String cleaned = userPrompt
            .replaceAll("(?i)(advertise|promote|create ad for|generate content for|quảng cáo|tạo nội dung|giới thiệu)", "")
            .replaceAll("[^\\p{L}\\p{N}\\s\\-,.]", "") // Remove special chars except basic punctuation
            .trim();

        // Get first sentence or limit to 100 chars
        int endIndex = cleaned.indexOf('.');
        if (endIndex > 0 && endIndex < 100) {
            cleaned = cleaned.substring(0, endIndex);
        } else if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }

        return cleaned.isBlank() ? "professional advertisement product" : cleaned.trim();
    }

    /**
     * Map AdStyle to visual keywords for image generation
     * Each style has carefully curated keywords to match the tone
     */
    private String getStyleKeywords(AdStyle adStyle) {
        if (adStyle == null) {
            // Default professional style if not specified
            return "polished, corporate, clean, professional lighting, neutral background";
        }

        return switch (adStyle) {
            case PROFESSIONAL ->
                "polished, corporate, clean, professional lighting, neutral background";

            case CASUAL ->
                "friendly, relaxed, natural, warm, approachable, soft lighting, everyday setting";

            case HUMOROUS ->
                "playful, vibrant, fun, colorful, dynamic, bright lighting, whimsical scene";

            case URGENT ->
                "bold, high contrast, striking, attention-grabbing, dramatic lighting, energetic composition";

            case LUXURY ->
                "elegant, premium, sophisticated, high-end, minimalist, studio lighting, refined aesthetics";

            case EDUCATIONAL ->
                "clear, instructional, well-organized, bright even lighting, informative layout";

            case INSPIRATIONAL ->
                "uplifting, aspirational, hopeful, golden hour lighting, expansive scene, motivational atmosphere";

            case MINIMALIST ->
                "simple, uncluttered, clean lines, soft shadows, minimal elements, white space";
        };
    }

    /**
     * Get provider-specific safety guidance
     * Each provider has different content policies and restrictions
     */
    private String getSafetyGuidance(String provider) {
        if (provider == null || provider.isBlank()) {
            return "safe, brand friendly, suitable for advertising";
        }

        return switch (provider.toLowerCase()) {
            case "gemini" ->
                "safe for all audiences, brand friendly, no copyrighted characters, suitable for Facebook ads";

            case "openai", "dall-e", "dalle" ->
                "no real people, no logos, brand safe, suitable for commercial use";

            case "stablediffusion", "stable-diffusion" ->
                "brand friendly, commercial safe, no offensive content";

            case "falai", "fal-ai", "fal.ai" ->
                "safe, brand appropriate, no controversial imagery";

            case "huggingface", "hugging-face" ->
                "safe, brand friendly, suitable for advertising";

            default ->
                "safe, brand friendly, suitable for advertising";
        };
    }
}
