package com.fbadsautomation.repository;

import com.fbadsautomation.model.Persona;
import com.fbadsautomation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Persona entity.
 * Follows Spring Data JPA patterns with security-conscious queries.
 *
 * Security: All queries filtered by user to prevent unauthorized access.
 * Performance: Indexed queries for efficient lookups.
 */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Find all personas for a specific user.
     * Performance: O(n) with index on user_id.
     *
     * @param user The user
     * @return List of personas
     */
    List<Persona> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all personas for a user with pagination.
     * Performance: Uses limit/offset with indexed query.
     *
     * @param user The user
     * @param pageable Pagination params
     * @return Page of personas
     */
    Page<Persona> findByUser(User user, Pageable pageable);

    /**
     * Find persona by ID and user (security check).
     * Critical for authorization - ensures users can only access their own personas.
     *
     * @param id Persona ID
     * @param user The user
     * @return Optional persona
     */
    Optional<Persona> findByIdAndUser(Long id, User user);

    /**
     * Check if persona name exists for user (uniqueness check).
     * Prevents duplicate persona names per user.
     *
     * @param name Persona name
     * @param user The user
     * @return true if exists
     */
    boolean existsByNameAndUser(String name, User user);

    /**
     * Count personas for a user.
     * Used for quota enforcement and analytics.
     *
     * @param user The user
     * @return Count of personas
     */
    long countByUser(User user);

    /**
     * Find personas by tone for a user.
     * Allows users to filter personas by preferred tone.
     *
     * @param tone The tone
     * @param user The user
     * @return List of personas
     */
    List<Persona> findByToneAndUser(String tone, User user);

    /**
     * Search personas by name (case-insensitive).
     * Performance: Uses LIKE with index, may be slow on large datasets.
     *
     * @param name Name to search
     * @param user The user
     * @param pageable Pagination
     * @return Page of matching personas
     */
    @Query("SELECT p FROM Persona p WHERE p.user = :user AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Persona> searchByName(@Param("name") String name, @Param("user") User user, Pageable pageable);

    /**
     * Delete all personas for a user.
     * Used during user account deletion (cascade).
     *
     * @param user The user
     */
    void deleteByUser(User user);
}
