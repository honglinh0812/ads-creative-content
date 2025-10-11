package com.fbadsautomation.controller;

import com.fbadsautomation.dto.PersonaRequest;
import com.fbadsautomation.dto.PersonaResponse;
import com.fbadsautomation.service.PersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for Persona management.
 * Provides endpoints for CRUD operations on user personas.
 *
 * Security: All endpoints require authentication via Bearer token.
 * Performance: Uses pagination for list endpoints to handle large datasets.
 * API Design: RESTful with standard HTTP methods and status codes.
 */
@RestController
@RequestMapping("/api/personas")
@CrossOrigin(origins = "*")
@Tag(name = "Personas", description = "API endpoints for managing target audience personas")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class PersonaController {

    private static final Logger log = LoggerFactory.getLogger(PersonaController.class);

    private final PersonaService personaService;

    /**
     * Create a new persona.
     * Security: Validates request and user authentication.
     * Performance: O(1) database operation.
     *
     * @param request Persona creation request
     * @param authentication User authentication
     * @return Created persona
     */
    @Operation(
        summary = "Create persona",
        description = "Create a new target audience persona for ad generation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Persona created successfully",
            content = @Content(schema = @Schema(implementation = PersonaResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "409", description = "Persona with same name already exists")
    })
    @PostMapping
    public ResponseEntity<PersonaResponse> createPersona(
            @Valid @RequestBody PersonaRequest request,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Creating persona for user: {}", userId);

        try {
            PersonaResponse response = personaService.createPersona(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create persona for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all personas for the authenticated user.
     * Performance: Paginated to handle large datasets efficiently.
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param authentication User authentication
     * @return Page of personas
     */
    @Operation(
        summary = "Get all personas",
        description = "Retrieve all personas for the authenticated user with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Personas retrieved successfully"
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<PersonaResponse>> getAllPersonas(
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Getting all personas for user: {}", userId);

        try {
            List<PersonaResponse> personas = personaService.getUserPersonas(userId);
            return ResponseEntity.ok(personas);
        } catch (Exception e) {
            log.error("Failed to get personas for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get personas with pagination.
     * Performance: Database-level pagination for large datasets.
     *
     * @param page Page number
     * @param size Page size
     * @param authentication User authentication
     * @return Page of personas
     */
    @Operation(
        summary = "Get personas (paginated)",
        description = "Retrieve personas with pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Personas retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/paged")
    public ResponseEntity<Page<PersonaResponse>> getPersonasPaged(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Getting paged personas for user: {} (page: {}, size: {})", userId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<PersonaResponse> personas = personaService.getUserPersonasPaged(userId, pageable);
            return ResponseEntity.ok(personas);
        } catch (Exception e) {
            log.error("Failed to get paged personas for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get a specific persona by ID.
     * Security: Validates user ownership of persona.
     *
     * @param id Persona ID
     * @param authentication User authentication
     * @return Persona details
     */
    @Operation(
        summary = "Get persona by ID",
        description = "Retrieve a specific persona by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Persona retrieved successfully",
            content = @Content(schema = @Schema(implementation = PersonaResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Persona not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponse> getPersonaById(
            @Parameter(description = "Persona ID") @PathVariable Long id,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Getting persona {} for user: {}", id, userId);

        try {
            PersonaResponse persona = personaService.getPersonaById(id, userId);
            return ResponseEntity.ok(persona);
        } catch (Exception e) {
            log.error("Failed to get persona {} for user {}: {}", id, userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update an existing persona.
     * Security: Validates ownership and request data.
     *
     * @param id Persona ID
     * @param request Update request
     * @param authentication User authentication
     * @return Updated persona
     */
    @Operation(
        summary = "Update persona",
        description = "Update an existing persona"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Persona updated successfully",
            content = @Content(schema = @Schema(implementation = PersonaResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Persona not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate persona name")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PersonaResponse> updatePersona(
            @Parameter(description = "Persona ID") @PathVariable Long id,
            @Valid @RequestBody PersonaRequest request,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Updating persona {} for user: {}", id, userId);

        try {
            PersonaResponse response = personaService.updatePersona(id, request, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update persona {} for user {}: {}", id, userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete a persona.
     * Security: Validates ownership before deletion.
     *
     * @param id Persona ID
     * @param authentication User authentication
     * @return No content on success
     */
    @Operation(
        summary = "Delete persona",
        description = "Delete a persona by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Persona deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Persona not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePersona(
            @Parameter(description = "Persona ID") @PathVariable Long id,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Deleting persona {} for user: {}", id, userId);

        try {
            personaService.deletePersona(id, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete persona {} for user {}: {}", id, userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Search personas by name.
     * Performance: Uses database LIKE query with pagination.
     *
     * @param query Search query
     * @param page Page number
     * @param size Page size
     * @param authentication User authentication
     * @return Page of matching personas
     */
    @Operation(
        summary = "Search personas",
        description = "Search personas by name (case-insensitive)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<PersonaResponse>> searchPersonas(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("Searching personas for user: {} with query: '{}'", userId, query);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<PersonaResponse> personas = personaService.searchPersonas(query, userId, pageable);
            return ResponseEntity.ok(personas);
        } catch (Exception e) {
            log.error("Failed to search personas for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
