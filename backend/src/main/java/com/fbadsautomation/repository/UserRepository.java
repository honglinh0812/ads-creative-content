package com.fbadsautomation.repository;

import com.fbadsautomation.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long userId);
    Optional<User> findByEmail(String userEmail);
    Optional<User> findByUsername(String username);
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);
    // Removed findByFbUserId as fbUserId is no longer in User model
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    // Removed existsByFbUserId as fbUserId is no longer in User model;
    }
