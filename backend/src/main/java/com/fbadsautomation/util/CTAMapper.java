package com.fbadsautomation.util;

import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.util.ValidationMessages.Language;

/**
 * Utility class for mapping Facebook CTA enums to display labels
 * Supports bilingual (Vietnamese/English) label mapping
 */
public class CTAMapper {

    /**
     * Get display label for CTA enum based on language
     *
     * @param cta The FacebookCTA enum value
     * @param language The target language
     * @return Localized display label
     */
    public static String getDisplayLabel(FacebookCTA cta, Language language) {
        if (cta == null) {
            return null;
        }

        return language == Language.VIETNAMESE
                ? cta.getVietnameseLabel()
                : cta.getEnglishLabel();
    }

    /**
     * Get display label for CTA enum based on language code
     *
     * @param cta The FacebookCTA enum value
     * @param languageCode ISO language code (vi, en, etc.)
     * @return Localized display label
     */
    public static String getDisplayLabel(FacebookCTA cta, String languageCode) {
        if (cta == null) {
            return null;
        }

        return "vi".equalsIgnoreCase(languageCode)
                ? cta.getVietnameseLabel()
                : cta.getEnglishLabel();
    }

    /**
     * Get Vietnamese label (convenience method)
     */
    public static String getVietnameseLabel(FacebookCTA cta) {
        return cta != null ? cta.getVietnameseLabel() : null;
    }

    /**
     * Get English label (convenience method)
     */
    public static String getEnglishLabel(FacebookCTA cta) {
        return cta != null ? cta.getEnglishLabel() : null;
    }

    /**
     * Parse CTA enum from string (handles both enum name and labels)
     *
     * @param ctaString The CTA string (enum name or label)
     * @return FacebookCTA enum, or null if not found
     */
    public static FacebookCTA fromString(String ctaString) {
        if (ctaString == null || ctaString.trim().isEmpty()) {
            return null;
        }

        String trimmed = ctaString.trim();

        // Try parsing as enum name first
        try {
            return FacebookCTA.valueOf(trimmed.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Not an enum name, try matching labels
        }

        // Try matching by label
        for (FacebookCTA cta : FacebookCTA.values()) {
            if (trimmed.equalsIgnoreCase(cta.getEnglishLabel()) ||
                trimmed.equalsIgnoreCase(cta.getVietnameseLabel())) {
                return cta;
            }
        }

        return null;
    }
}
