package com.fbadsautomation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persona entity representing target audience profiles for ad generation.
 * Follows existing entity patterns with proper validation and security considerations.
 *
 * Security: All fields are sanitized in service layer before persistence.
 * Performance: Uses @ElementCollection with proper fetch strategies.
 * Maintainability: Clear field names and comprehensive validation.
 */
@Entity
@Table(name = "personas", indexes = {
    @Index(name = "idx_persona_user", columnList = "user_id"),
    @Index(name = "idx_persona_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Persona name is required")
    @Size(min = 2, max = 100, message = "Persona name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 13, message = "Age must be at least 13")
    @Max(value = 120, message = "Age must be at most 120")
    @Column(nullable = false)
    private Integer age;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @ElementCollection(fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SELECT)
    @CollectionTable(
        name = "persona_interests",
        joinColumns = @JoinColumn(name = "persona_id"),
        indexes = @Index(name = "idx_persona_interests", columnList = "persona_id")
    )
    @Column(name = "interest", length = 100)
    @Size(max = 20, message = "Maximum 20 interests allowed")
    @Builder.Default
    private List<String> interests = new ArrayList<>();

    @NotBlank(message = "Tone is required")
    @Pattern(
        regexp = "^(professional|funny|casual|friendly|formal|enthusiastic)$",
        message = "Tone must be one of: professional, funny, casual, friendly, formal, enthusiastic"
    )
    @Column(nullable = false, length = 20)
    private String tone;

    @ElementCollection(fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SELECT)
    @CollectionTable(
        name = "persona_pain_points",
        joinColumns = @JoinColumn(name = "persona_id"),
        indexes = @Index(name = "idx_persona_pain_points", columnList = "persona_id")
    )
    @Column(name = "pain_point", length = 500)
    @Size(max = 10, message = "Maximum 10 pain points allowed")
    @Builder.Default
    private List<String> painPoints = new ArrayList<>();

    @Size(max = 500, message = "Desired outcome must be at most 500 characters")
    @Column(name = "desired_outcome", length = 500)
    private String desiredOutcome;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Formats persona details for AI prompt injection.
     * Performance: O(n) where n is total items in lists.
     *
     * @return Formatted string for prompt enhancement
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nTarget Persona Profile:");
        sb.append("\n- Name: ").append(name);
        sb.append("\n- Age: ").append(age);
        sb.append("\n- Gender: ").append(gender.getDisplayName());

        if (interests != null && !interests.isEmpty()) {
            sb.append("\n- Interests: ").append(String.join(", ", interests));
        }

        sb.append("\n- Preferred Tone: ").append(tone);

        if (painPoints != null && !painPoints.isEmpty()) {
            sb.append("\n- Pain Points: ");
            for (int i = 0; i < painPoints.size(); i++) {
                sb.append("\n  ").append(i + 1).append(". ").append(painPoints.get(i));
            }
        }

        if (desiredOutcome != null && !desiredOutcome.isEmpty()) {
            sb.append("\n- Desired Outcome: ").append(desiredOutcome);
        }

        return sb.toString();
    }

    /**
     * Formats persona details for AI prompt injection (Vietnamese version).
     * Phase 3: Added for ChainOfThoughtPromptBuilder bilingual support.
     *
     * @return Formatted string in Vietnamese for prompt enhancement
     */
    public String toPromptStringVietnamese() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nHồ sơ Persona mục tiêu:");
        sb.append("\n- Tên: ").append(name);
        sb.append("\n- Tuổi: ").append(age);
        sb.append("\n- Giới tính: ").append(gender.getDisplayNameVietnamese());

        if (interests != null && !interests.isEmpty()) {
            sb.append("\n- Sở thích: ").append(String.join(", ", interests));
        }

        sb.append("\n- Giọng điệu ưa thích: ").append(tone);

        if (painPoints != null && !painPoints.isEmpty()) {
            sb.append("\n- Vấn đề nhức nhối (Pain Points): ");
            for (int i = 0; i < painPoints.size(); i++) {
                sb.append("\n  ").append(i + 1).append(". ").append(painPoints.get(i));
            }
        }

        if (desiredOutcome != null && !desiredOutcome.isEmpty()) {
            sb.append("\n- Kết quả mong muốn: ").append(desiredOutcome);
        }

        return sb.toString();
    }

    /**
     * Validates persona data integrity.
     * Called before save operations in service layer.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
            && age != null && age >= 13 && age <= 120
            && gender != null
            && tone != null && !tone.trim().isEmpty();
    }
}
