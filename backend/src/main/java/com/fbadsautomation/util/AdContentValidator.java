package com.fbadsautomation.util;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Objects;

/**
 * Validator for AdContent to ensure compliance with Facebook advertising limits.
 * Automatically truncates content that exceeds Facebook's character limits.
 */
@Component
public class AdContentValidator {

    private static final Logger log = LoggerFactory.getLogger(AdContentValidator.class);

    // Facebook character limits
    private static final int HEADLINE_MAX = 40;
    private static final int DESCRIPTION_MAX = 125;
    private static final int PRIMARY_TEXT_MAX = 1000;

    /**
     * Validates and truncates AdContent fields to meet Facebook limits.
     * Sets needsReview flag if any truncation occurred.
     *
     * @param content The AdContent to validate and truncate
     */
    public void validateAndTruncate(AdContent content) {
        if (content == null) {
            return;
        }

        boolean needsReview = false;

        // Validate and truncate headline
        if (content.getHeadline() != null && content.getHeadline().length() > HEADLINE_MAX) {
            log.warn("Headline exceeds {} chars ({} chars), truncating: \"{}\"",
                HEADLINE_MAX, content.getHeadline().length(), content.getHeadline());
            content.setHeadline(truncateSmart(content.getHeadline(), HEADLINE_MAX));
            needsReview = true;
        }

        // Validate and truncate description
        if (content.getDescription() != null && content.getDescription().length() > DESCRIPTION_MAX) {
            log.warn("Description exceeds {} chars ({} chars), truncating",
                DESCRIPTION_MAX, content.getDescription().length());
            content.setDescription(truncateSmart(content.getDescription(), DESCRIPTION_MAX));
            needsReview = true;
        }

        // Validate and truncate primary text
        if (content.getPrimaryText() != null && content.getPrimaryText().length() > PRIMARY_TEXT_MAX) {
            log.warn("Primary text exceeds {} chars ({} chars), truncating",
                PRIMARY_TEXT_MAX, content.getPrimaryText().length());
            content.setPrimaryText(truncateSmart(content.getPrimaryText(), PRIMARY_TEXT_MAX));
            needsReview = true;
        }

        // Set needs review flag
        content.setNeedsReview(needsReview);
    }

    /**
     * Smart truncation that tries to cut at word boundaries when possible.
     *
     * @param text The text to truncate
     * @param maxLength The maximum length
     * @return Truncated text with "..." suffix if truncated
     */
    private String truncateSmart(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        // Reserve space for "..."
        int cutPoint = maxLength - 3;

        // Try to find last space near cut point (within 10 chars)
        int lastSpace = text.lastIndexOf(' ', cutPoint);

        if (lastSpace > cutPoint - 10 && lastSpace > 0) {
            // Cut at word boundary
            return text.substring(0, lastSpace).trim() + "...";
        } else {
            // Hard cut at character limit
            return text.substring(0, cutPoint).trim() + "...";
        }
    }

    /**
     * Applies the same Facebook limits to Ad entities before persistence.
     *
     * @param ad The Ad to validate and truncate
     * @return true if any field was truncated
     */
    public boolean enforceAdLimits(Ad ad) {
        if (ad == null) {
            return false;
        }

        AdContent mirror = new AdContent();
        mirror.setHeadline(ad.getHeadline());
        mirror.setDescription(ad.getDescription());
        mirror.setPrimaryText(ad.getPrimaryText());

        validateAndTruncate(mirror);

        boolean modified = !Objects.equals(ad.getHeadline(), mirror.getHeadline())
                || !Objects.equals(ad.getDescription(), mirror.getDescription())
                || !Objects.equals(ad.getPrimaryText(), mirror.getPrimaryText());

        ad.setHeadline(mirror.getHeadline());
        ad.setDescription(mirror.getDescription());
        ad.setPrimaryText(mirror.getPrimaryText());

        return modified;
    }

    /**
     * Validates content without modifying it. Returns true if valid.
     *
     * @param content The AdContent to validate
     * @return true if content meets all Facebook limits, false otherwise
     */
    public boolean isValid(AdContent content) {
        if (content == null) {
            return false;
        }

        if (content.getHeadline() != null && content.getHeadline().length() > HEADLINE_MAX) {
            return false;
        }

        if (content.getDescription() != null && content.getDescription().length() > DESCRIPTION_MAX) {
            return false;
        }

        if (content.getPrimaryText() != null && content.getPrimaryText().length() > PRIMARY_TEXT_MAX) {
            return false;
        }

        return true;
    }
}
