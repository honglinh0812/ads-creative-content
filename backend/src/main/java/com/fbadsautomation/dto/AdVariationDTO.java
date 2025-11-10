package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Data Transfer Object for A/B Test Ad Variation
 * Represents a single variation of an ad generated for A/B testing
 *
 * @author AI Panel - Senior Engineers
 * @version 1.0
 * @security AI-generated content, sanitized before display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A/B test ad variation with structured fields")
public class AdVariationDTO {

    /**
     * Variation number (1-5)
     */
    @Schema(description = "Variation number", example = "1", required = true)
    private int variationNumber;

    /**
     * Ad headline
     */
    @Schema(description = "Ad headline (max 40 chars for optimal display)", example = "Limited Time Offer - 50% Off")
    @NotBlank(message = "Headline is required")
    @Size(max = 255, message = "Headline must not exceed 255 characters")
    private String headline;

    /**
     * Primary ad text
     */
    @Schema(description = "Primary ad text (max 125 chars for optimal display)", example = "Get the best deals on premium products. Shop now and save big!")
    @NotBlank(message = "Primary text is required")
    @Size(max = 5000, message = "Primary text must not exceed 5000 characters")
    private String primaryText;

    /**
     * Call-to-action text
     */
    @Schema(description = "Call-to-action text", example = "Shop Now")
    @Size(max = 50, message = "CTA must not exceed 50 characters")
    private String callToAction;

    /**
     * Testing focus (what this variation tests)
     */
    @Schema(description = "What aspect this variation tests", example = "Emotional headline vs. rational approach")
    private String testingFocus;

    /**
     * Raw variation text from AI (fallback)
     */
    @Schema(description = "Raw variation text if parsing fails", example = "VARIATION 1: ...")
    private String rawVariation;
}
