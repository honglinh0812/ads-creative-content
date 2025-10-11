package com.fbadsautomation.dto;

import com.fbadsautomation.model.Gender;
import com.fbadsautomation.model.Persona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for persona data.
 * Excludes sensitive information and provides clean API contract.
 *
 * Security: Does not expose user entity details.
 * Performance: Minimal data transfer with only essential fields.
 * Maintainability: Clear mapping from entity to DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaResponse {

    private Long id;
    private String name;
    private Integer age;
    private String gender;
    private List<String> interests;
    private String tone;
    private List<String> painPoints;
    private String desiredOutcome;
    private String description;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Maps Persona entity to Response DTO.
     * Performance: O(1) for field mapping, O(n) for list copies.
     * Security: Excludes sensitive user information.
     *
     * @param persona The persona entity
     * @return PersonaResponse DTO
     */
    public static PersonaResponse fromEntity(Persona persona) {
        if (persona == null) {
            return null;
        }

        return PersonaResponse.builder()
            .id(persona.getId())
            .name(persona.getName())
            .age(persona.getAge())
            .gender(persona.getGender() != null ? persona.getGender().name() : null)
            .interests(persona.getInterests() != null ? List.copyOf(persona.getInterests()) : List.of())
            .tone(persona.getTone())
            .painPoints(persona.getPainPoints() != null ? List.copyOf(persona.getPainPoints()) : List.of())
            .desiredOutcome(persona.getDesiredOutcome())
            .description(persona.getDescription())
            .userId(persona.getUser() != null ? persona.getUser().getId() : null)
            .createdAt(persona.getCreatedAt())
            .updatedAt(persona.getUpdatedAt())
            .build();
    }

    /**
     * Maps list of entities to list of response DTOs.
     * Performance: O(n) where n is list size.
     *
     * @param personas List of persona entities
     * @return List of PersonaResponse DTOs
     */
    public static List<PersonaResponse> fromEntities(List<Persona> personas) {
        if (personas == null) {
            return List.of();
        }
        return personas.stream()
            .map(PersonaResponse::fromEntity)
            .toList();
    }

    /**
     * Gets a summary string for display purposes.
     *
     * @return Human-readable summary
     */
    public String getSummary() {
        return String.format("%s, %d, %s - %s tone",
            name, age, gender, tone);
    }
}
