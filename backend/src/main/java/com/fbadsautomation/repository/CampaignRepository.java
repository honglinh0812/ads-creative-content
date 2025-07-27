package com.fbadsautomation.repository;

import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByUser(User user);
    List<Campaign> findByUserOrderByCreatedDateDesc(User user);
    Optional<Campaign> findByIdAndUser(Long id, User user);
    
    long countByUser(User user);
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.user = :user AND c.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") Campaign.CampaignStatus status);

    List<Campaign> findTop5ByUserOrderByCreatedDateDesc(User user);
    Page<Campaign> findAllByUser(User user, Pageable pageable);

    // ====================================================
    // OPTIMIZED QUERIES WITH JOIN FETCH TO PREVENT N+1
    // ====================================================

    /**
     * Find campaigns with ads eagerly loaded to prevent N+1 queries
     */
    @Query("SELECT DISTINCT c FROM Campaign c LEFT JOIN FETCH c.ads WHERE c.user = :user ORDER BY c.createdDate DESC")
    List<Campaign> findByUserWithAds(@Param("user") User user);

    /**
     * Find top campaigns with ads for dashboard
     */
    @Query("SELECT DISTINCT c FROM Campaign c LEFT JOIN FETCH c.ads WHERE c.user = :user ORDER BY c.createdDate DESC")
    List<Campaign> findTop5ByUserWithAdsOrderByCreatedDateDesc(@Param("user") User user, Pageable pageable);

    /**
     * Find campaign by ID with ads eagerly loaded
     */
    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.ads WHERE c.id = :id AND c.user = :user")
    Optional<Campaign> findByIdAndUserWithAds(@Param("id") Long id, @Param("user") User user);

    /**
     * Find campaigns with statistics for dashboard
     */
    @Query("SELECT c, COUNT(a) as adCount, " +
           "SUM(CASE WHEN a.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeAdCount " +
           "FROM Campaign c LEFT JOIN c.ads a " +
           "WHERE c.user = :user " +
           "GROUP BY c " +
           "ORDER BY c.createdDate DESC")
    List<Object[]> findCampaignsWithStatsByUser(@Param("user") User user);

    /**
     * Find campaigns by status with pagination and statistics
     */
    @Query("SELECT c, COUNT(a) as adCount " +
           "FROM Campaign c LEFT JOIN c.ads a " +
           "WHERE c.user = :user AND c.status = :status " +
           "GROUP BY c " +
           "ORDER BY c.createdDate DESC")
    Page<Object[]> findByUserAndStatusWithStats(@Param("user") User user,
                                               @Param("status") Campaign.CampaignStatus status,
                                               Pageable pageable);

    /**
     * Find campaigns with budget analysis
     */
    @Query("SELECT c, " +
           "COALESCE(c.dailyBudget, 0) as dailyBudget, " +
           "COALESCE(c.totalBudget, 0) as totalBudget, " +
           "COUNT(a) as adCount " +
           "FROM Campaign c LEFT JOIN c.ads a " +
           "WHERE c.user = :user " +
           "GROUP BY c, c.dailyBudget, c.totalBudget " +
           "ORDER BY c.createdDate DESC")
    List<Object[]> findCampaignsWithBudgetAnalysis(@Param("user") User user);

    /**
     * Count campaigns by user and date range
     */
    long countByUserAndCreatedDateBetween(User user, LocalDateTime startDate, LocalDateTime endDate);
}
