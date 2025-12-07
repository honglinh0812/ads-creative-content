package com.fbadsautomation.service;

import com.fbadsautomation.dto.PersonaRequest;
import com.fbadsautomation.dto.PersonaResponse;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Gender;
import com.fbadsautomation.model.Persona;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.PersonaRepository;
import com.fbadsautomation.repository.UserRepository;
import com.fbadsautomation.service.security.PromptSecurityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for Persona operations.
 * Implements business logic with security, validation, and error handling.
 *
 * Security: All operations validate user ownership before modifications.
 * Performance: Uses pagination and indexed queries for efficient data access.
 * Maintainability: Clear separation of concerns with comprehensive logging.
 */
@Service
@RequiredArgsConstructor
public class PersonaService {

    private static final Logger log = LoggerFactory.getLogger(PersonaService.class);
    private static final int MAX_PERSONAS_PER_USER = 100;

    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;
    private final PromptSecurityService promptSecurityService;

    /**
     * Create a new persona for a user.
     * Security: Validates user exists and enforces quota limits.
     * Performance: O(1) database operations with indexed queries.
     *
     * @param request Persona creation request
     * @param userId User ID
     * @return Created persona response
     * @throws ApiException if user not found
     * @throws BusinessException if quota exceeded or duplicate name
     */
    @Transactional
    public PersonaResponse createPersona(PersonaRequest request, Long userId) {
        log.info("Creating persona for user ID: {}", userId);

        // Validate and sanitize input
        request.sanitize();

        // Fetch user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Check quota
        long personaCount = personaRepository.countByUser(user);
        if (personaCount >= MAX_PERSONAS_PER_USER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, String.format(
                "Maximum persona limit reached (%d). Please delete existing personas before creating new ones.",
                MAX_PERSONAS_PER_USER
            ));
        }

        // Check for duplicate name
        if (personaRepository.existsByNameAndUser(request.getName(), user)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                String.format("A persona with name '%s' already exists", request.getName())
            );
        }

        // Map DTO to entity
        Persona persona = mapRequestToEntity(request, user);

        // Validate entity
        if (!persona.isValid()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid persona data");
        }

        // Save persona
        Persona saved = personaRepository.save(persona);
        log.info("Created persona ID: {} for user ID: {}", saved.getId(), userId);

        return PersonaResponse.fromEntity(saved);
    }

    /**
     * Get all personas for a user.
     * Security: Only returns personas owned by the user.
     * Performance: Ordered by creation date DESC for better UX.
     *
     * @param userId User ID
     * @return List of persona responses
     */
    @Transactional(readOnly = true)
    public List<PersonaResponse> getUserPersonas(Long userId) {
        log.info("Fetching all personas for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        List<Persona> personas = personaRepository.findByUserOrderByCreatedAtDesc(user);
        log.info("Found {} personas for user ID: {}", personas.size(), userId);

        return PersonaResponse.fromEntities(personas);
    }

    /**
     * Get personas with pagination.
     * Performance: Uses database pagination for efficient large dataset handling.
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of persona responses
     */
    @Transactional(readOnly = true)
    public Page<PersonaResponse> getUserPersonasPaged(Long userId, Pageable pageable) {
        log.info("Fetching paged personas for user ID: {} (page: {}, size: {})",
            userId, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Page<Persona> personas = personaRepository.findByUser(user, pageable);
        return personas.map(PersonaResponse::fromEntity);
    }

    /**
     * Get a specific persona by ID.
     * Security: Validates user ownership before returning data.
     * Performance: O(1) indexed query.
     *
     * @param personaId Persona ID
     * @param userId User ID
     * @return Persona response
     * @throws ApiException if persona not found or unauthorized
     */
    @Transactional(readOnly = true)
    public PersonaResponse getPersonaById(Long personaId, Long userId) {
        log.info("Fetching persona ID: {} for user ID: {}", personaId, userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Persona persona = personaRepository.findByIdAndUser(personaId, user)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Persona not found"));

        return PersonaResponse.fromEntity(persona);
    }

    /**
     * Update an existing persona.
     * Security: Validates ownership and checks for duplicate names.
     * Performance: Single database update with optimistic locking.
     *
     * @param personaId Persona ID
     * @param request Update request
     * @param userId User ID
     * @return Updated persona response
     */
    @Transactional
    public PersonaResponse updatePersona(Long personaId, PersonaRequest request, Long userId) {
        log.info("Updating persona ID: {} for user ID: {}", personaId, userId);

        // Validate and sanitize input
        request.sanitize();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Persona persona = personaRepository.findByIdAndUser(personaId, user)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Persona not found"));

        // Check for duplicate name (excluding current persona)
        if (!persona.getName().equals(request.getName()) &&
            personaRepository.existsByNameAndUser(request.getName(), user)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                String.format("A persona with name '%s' already exists", request.getName())
            );
        }

        // Update fields
        updateEntityFromRequest(persona, request);

        // Validate updated entity
        if (!persona.isValid()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid persona data");
        }

        Persona updated = personaRepository.save(persona);
        log.info("Updated persona ID: {}", updated.getId());

        return PersonaResponse.fromEntity(updated);
    }

    /**
     * Delete a persona.
     * Security: Validates ownership before deletion.
     * Performance: O(1) deletion with indexed lookup.
     *
     * @param personaId Persona ID
     * @param userId User ID
     */
    @Transactional
    public void deletePersona(Long personaId, Long userId) {
        log.info("Deleting persona ID: {} for user ID: {}", personaId, userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Persona persona = personaRepository.findByIdAndUser(personaId, user)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Persona not found"));

        personaRepository.delete(persona);
        log.info("Deleted persona ID: {}", personaId);
    }

    /**
     * Search personas by name.
     * Performance: Case-insensitive LIKE query with pagination.
     *
     * @param query Search query
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of matching personas
     */
    @Transactional(readOnly = true)
    public Page<PersonaResponse> searchPersonas(String query, Long userId, Pageable pageable) {
        log.info("Searching personas for user ID: {} with query: '{}'", userId, query);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Page<Persona> personas = personaRepository.searchByName(query, user, pageable);
        return personas.map(PersonaResponse::fromEntity);
    }

    /**
     * Maps request DTO to entity.
     * Performance: O(n) where n is list sizes.
     */
    private Persona mapRequestToEntity(PersonaRequest request, User user) {
        return Persona.builder()
            .name(promptSecurityService.sanitizeUserInput(request.getName()))
            .age(request.getAge())
            .gender(Gender.valueOf(request.getGender()))
            .interests(sanitizeList(request.getInterests()))
            .tone(promptSecurityService.sanitizeUserInput(request.getTone()))
            .painPoints(sanitizeList(request.getPainPoints()))
            .desiredOutcome(sanitizeNullable(request.getDesiredOutcome()))
            .description(sanitizeNullable(request.getDescription()))
            .user(user)
            .build();
    }

    /**
     * Updates entity fields from request.
     * Performance: O(n) where n is list sizes.
     */
    private void updateEntityFromRequest(Persona persona, PersonaRequest request) {
        persona.setName(promptSecurityService.sanitizeUserInput(request.getName()));
        persona.setAge(request.getAge());
        persona.setGender(Gender.valueOf(request.getGender()));
        persona.setInterests(sanitizeList(request.getInterests()));
        persona.setTone(promptSecurityService.sanitizeUserInput(request.getTone()));
        persona.setPainPoints(sanitizeList(request.getPainPoints()));
        persona.setDesiredOutcome(sanitizeNullable(request.getDesiredOutcome()));
        persona.setDescription(sanitizeNullable(request.getDescription()));
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(promptSecurityService::sanitizeUserInput)
            .toList();
    }

    private String sanitizeNullable(String value) {
        return value == null ? null : promptSecurityService.sanitizeUserInput(value);
    }

    /**
     * Phase 1: Find persona by ID and validate user ownership.
     * Used by AdService to fetch user-selected persona.
     *
     * @param personaId Persona ID
     * @param user User entity
     * @return Optional<Persona>
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Persona> findByIdAndUser(Long personaId, User user) {
        log.debug("Finding persona ID: {} for user ID: {}", personaId, user != null ? user.getId() : null);
        if (personaId == null || user == null) {
            return java.util.Optional.empty();
        }
        return personaRepository.findByIdAndUser(personaId, user);
    }

    /**
     * Enrich prompt with persona details for AI generation.
     * Security: Validates persona ownership before use.
     * Performance: O(n) string concatenation where n is persona fields.
     *
     * @param basePrompt Original prompt
     * @param personaId Persona ID (optional)
     * @param userId User ID
     * @return Enhanced prompt with persona context
     */
    @Transactional(readOnly = true)
    public String enrichPromptWithPersona(String basePrompt, Long personaId, Long userId) {
        // If no persona specified, return original prompt
        if (personaId == null) {
            return basePrompt;
        }

        log.info("Enriching prompt with persona ID: {} for user ID: {}", personaId, userId);

        try {
            // Fetch and validate persona ownership
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

            Persona persona = personaRepository.findByIdAndUser(personaId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                    "Persona not found or you don't have access to it"));

            // Build enriched prompt
            StringBuilder enriched = new StringBuilder(basePrompt != null ? basePrompt : "");

            // Append persona profile using the entity's built-in method
            enriched.append(persona.toPromptString());

            String result = enriched.toString();
            log.info("Enriched prompt length: {} characters", result.length());

            return result;

        } catch (ApiException e) {
            // Re-throw API exceptions
            throw e;
        } catch (Exception e) {
            // Log error but don't fail generation - use original prompt
            log.error("Failed to enrich prompt with persona {}: {}", personaId, e.getMessage(), e);
            log.warn("Continuing with original prompt without persona enhancement");
            return basePrompt;
        }
    }
}
