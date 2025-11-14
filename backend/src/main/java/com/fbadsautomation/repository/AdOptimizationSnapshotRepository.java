package com.fbadsautomation.repository;

import com.fbadsautomation.model.AdOptimizationSnapshot;
import com.fbadsautomation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdOptimizationSnapshotRepository extends JpaRepository<AdOptimizationSnapshot, Long> {
    Page<AdOptimizationSnapshot> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    boolean existsByUserAndAdId(User user, Long adId);
}
