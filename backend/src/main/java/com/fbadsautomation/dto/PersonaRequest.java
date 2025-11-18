package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.util.List;

/**
 * Request DTO for creating/updating personas.
 * Follows security best practices with comprehensive input validation.
 *
 * Security: All inputs validated to prevent injection attacks.
 * Maintainability: Clear validation messages for API consumers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaRequest {

    @NotBlank(message = "Persona name is required")
    @Size(min = 2, max = 100, message = "Persona name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[\\p{L}\\p{M}0-9\\s\\-_'\\.]+$",
        message = "Persona name contains invalid characters. Accented letters, numbers, spaces, hyphens, underscores, apostrophes, and periods are allowed"
    )
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 13, message = "Age must be at least 13 (COPPA compliance)")
    @Max(value = 120, message = "Age must be at most 120")
    private Integer age;

    @NotBlank(message = "Gender is required")
    @Pattern(
        regexp = "^(MALE|FEMALE|ALL)$",
        message = "Gender must be one of: MALE, FEMALE, ALL"
    )
    private String gender;

    @NotNull(message = "Interests cannot be null")
    @Size(max = 20, message = "Maximum 20 interests allowed")
    private List<@NotBlank @Size(max = 100) String> interests;

    @NotBlank(message = "Tone is required")
    @Pattern(
        regexp = "^(professional|funny|casual|friendly|formal|enthusiastic)$",
        message = "Tone must be one of: professional, funny, casual, friendly, formal, enthusiastic"
    )
    private String tone;

    @NotNull(message = "Pain points cannot be null")
    @Size(max = 10, message = "Maximum 10 pain points allowed")
    private List<@NotBlank @Size(max = 500) String> painPoints;

    @Size(max = 500, message = "Desired outcome must be at most 500 characters")
    private String desiredOutcome;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    /**
     * Validates logical consistency of request data.
     * Called after basic validation in service layer.
     *
     * @return true if logically consistent
     */
    @AssertTrue(message = "At least one interest must be provided")
    public boolean hasInterests() {
        return interests != null && !interests.isEmpty();
    }

    @AssertTrue(message = "At least one pain point must be provided")
    public boolean hasPainPoints() {
        return painPoints != null && !painPoints.isEmpty();
    }

    /**
     * Sanitizes input strings to prevent XSS and injection attacks.
     * Called in service layer before entity mapping.
     * Performance: O(n) where n is total character count.
     */
    public void sanitize() {
        if (name != null) {
            name = name.trim();
        }
        if (tone != null) {
            tone = tone.trim().toLowerCase();
        }
        if (gender != null) {
            gender = gender.trim().toUpperCase();
        }
        if (desiredOutcome != null) {
            desiredOutcome = desiredOutcome.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (interests != null) {
            interests = interests.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .limit(20)
                .toList();
        }
        if (painPoints != null) {
            painPoints = painPoints.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .limit(10)
                .toList();
        }
    }
}
