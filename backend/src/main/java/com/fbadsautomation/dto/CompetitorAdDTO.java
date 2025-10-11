package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for Competitor Ad information
 * Represents a competitor's ad retrieved from Facebook Ad Library
 *
 * @author AI Panel - Senior Engineers
 * @version 1.0
 * @security Public data from Facebook Ad Library, no sensitive information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Competitor ad information from Facebook Ad Library")
public class CompetitorAdDTO {

    /**
     * Facebook Ad Library ID
     */
    @Schema(description = "Facebook Ad Library ID", example = "702369045530963")
    private String adId;

    /**
     * Ad headline/title
     * Security: Sanitized on frontend display to prevent XSS
     */
    @Schema(description = "Ad headline", example = "Limited Time Offer - 50% Off")
    @Size(max = 255, message = "Headline must not exceed 255 characters")
    private String headline;

    /**
     * Primary ad text/body
     * Security: Sanitized on frontend display
     */
    @Schema(description = "Primary ad text", example = "Get the best deals on premium products...")
    @Size(max = 5000, message = "Primary text must not exceed 5000 characters")
    private String primaryText;

    /**
     * Ad description
     */
    @Schema(description = "Ad description", example = "Shop now and save big!")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Call to action text
     */
    @Schema(description = "Call to action", example = "SHOP_NOW", allowableValues = {
        "SHOP_NOW", "LEARN_MORE", "SIGN_UP", "DOWNLOAD", "BOOK_NOW", "CONTACT_US"
    })
    private String callToAction;

    /**
     * Date when ad started running
     */
    @Schema(description = "Ad start date", example = "2025-10-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * Date when ad stopped running (null if still active)
     */
    @Schema(description = "Ad end date (null if active)", example = "2025-10-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * List of image URLs used in the ad
     * Security: URLs validated to be from Facebook CDN
     */
    @Schema(description = "Image URLs", example = "[\"https://scontent.xx.fbcdn.net/...\"]")
    private List<String> imageUrls;

    /**
     * Video URL if ad contains video
     */
    @Schema(description = "Video URL", example = "https://video.xx.fbcdn.net/...")
    private String videoUrl;

    /**
     * Landing page URL
     * Security: URL validation applied
     */
    @Schema(description = "Landing page URL", example = "https://example.com/product")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String landingPageUrl;

    /**
     * Advertiser/Page name
     */
    @Schema(description = "Advertiser name", example = "Nike")
    @Size(max = 255, message = "Advertiser name must not exceed 255 characters")
    private String advertiserName;

    /**
     * Ad Library URL for reference
     */
    @Schema(description = "Facebook Ad Library URL", example = "https://www.facebook.com/ads/library/?id=702369045530963")
    private String adLibraryUrl;

    /**
     * Geographic regions where ad is shown
     */
    @Schema(description = "Target regions", example = "[\"US\", \"CA\", \"UK\"]")
    private List<String> targetRegions;

    /**
     * Estimated impressions (if available)
     */
    @Schema(description = "Estimated impressions", example = "100000-500000")
    private String estimatedImpressions;

    /**
     * Ad platform (Facebook, Instagram, Messenger, etc.)
     */
    @Schema(description = "Ad platforms", example = "[\"Facebook\", \"Instagram\"]")
    private List<String> platforms;

    /**
     * Whether ad is still active
     */
    @Schema(description = "Is ad currently active", example = "true")
    private Boolean isActive;

    /**
     * Source from which this ad was retrieved
     */
    @Schema(description = "Data source", example = "SCRAPE_CREATORS_API", allowableValues = {
        "SCRAPE_CREATORS_API", "META_AD_LIBRARY_API", "MANUAL"
    })
    private String dataSource;

    /**
     * Validation errors if any
     */
    @Schema(description = "Validation errors", example = "[\"Missing headline\"]")
    private List<String> validationErrors;

    /**
     * Additional metadata from the scraping service
     */
    @Schema(description = "Additional metadata")
    private Object metadata;

    /**
     * Checks if the ad has all required fields for export
     *
     * @return true if ad is valid for export
     */
    public boolean isValidForExport() {
        return (headline != null && !headline.trim().isEmpty())
                && (primaryText != null && !primaryText.trim().isEmpty())
                && callToAction != null;
    }

    /**
     * Checks if the ad has media (images or video)
     *
     * @return true if ad has at least one image or video
     */
    public boolean hasMedia() {
        return (imageUrls != null && !imageUrls.isEmpty()) || (videoUrl != null && !videoUrl.trim().isEmpty());
    }

    /**
     * Get a summary string for display
     *
     * @return Summary string (headline + truncated text)
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        if (headline != null) {
            summary.append(headline);
        }
        if (primaryText != null) {
            String truncated = primaryText.length() > 100
                ? primaryText.substring(0, 97) + "..."
                : primaryText;
            if (summary.length() > 0) {
                summary.append(" - ");
            }
            summary.append(truncated);
        }
        return summary.toString();
    }

    /**
     * Sanitize text content to prevent XSS
     * Note: This is a basic sanitization. Frontend should also sanitize before rendering.
     *
     * @param text Text to sanitize
     * @return Sanitized text
     */
    public static String sanitizeText(String text) {
        if (text == null) return null;
        return text.replaceAll("<script[^>]*>.*?</script>", "")
                   .replaceAll("<[^>]+>", "")
                   .trim();
    }

    /**
     * Apply sanitization to all text fields
     */
    public void sanitize() {
        this.headline = sanitizeText(this.headline);
        this.primaryText = sanitizeText(this.primaryText);
        this.description = sanitizeText(this.description);
        this.advertiserName = sanitizeText(this.advertiserName);
    }
}
