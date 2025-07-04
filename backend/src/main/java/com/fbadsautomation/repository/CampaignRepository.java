package com.fbadsautomation.repository;

import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByUser(User user);
    List<Campaign> findByUserOrderByCreatedAtDesc(User user);
    Optional<Campaign> findByIdAndUser(Long id, User user);
    
    long countByUser(User user);
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.user = :user AND c.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") Campaign.CampaignStatus status);

    List<Campaign> findTop5ByUserOrderByCreatedAtDesc(User user);
    Page<Campaign> findAllByUser(User user, Pageable pageable);
}


