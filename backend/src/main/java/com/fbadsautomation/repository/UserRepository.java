package com.fbadsautomation.repository;

import com.fbadsautomation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long userId);
    Optional<User> findByEmail(String userEmail);
    // Removed findByFbUserId as fbUserId is no longer in User model
    boolean existsByEmail(String email);
    // Removed existsByFbUserId as fbUserId is no longer in User model
}