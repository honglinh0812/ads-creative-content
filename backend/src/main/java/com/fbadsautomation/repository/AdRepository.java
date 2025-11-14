package com.fbadsautomation.repository;

import com.fbadsautomation.model.Ad;
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
public interface AdRepository extends JpaRepository<Ad, Long> {
    List<Ad> findByCampaign(Campaign campaign);
    List<Ad> findByCampaignOrderByCreatedDateDesc(Campaign campaign);
    Optional<Ad> findByIdAndCampaign(Long id, Campaign campaign);
    Optional<Ad> findByIdAndCampaignId(Long id, Long campaignId);
    List<Ad> findByCampaignId(Long campaignId);

    // Count ads by campaign ID - for campaign status updates (Issue #3)
    long countByCampaignId(Long campaignId);

    // Thêm các phương thức truy vấn theo user để đảm bảo cô lập dữ liệu
    List<Ad> findByUser(User user);
    List<Ad> findByUserOrderByCreatedDateDesc(User user);
    Optional<Ad> findByIdAndUser(Long id, User user);
    List<Ad> findByCampaignAndUser(Campaign campaign, User user);

    // Query by user ID for performance report matching
    @Query("SELECT a FROM Ad a WHERE a.user.id = :userId")
    List<Ad> findByUserId(@Param("userId") Long userId);
    
    List<Ad> findTop10ByOrderByCreatedDateDesc();
    List<Ad> findTop10ByUserOrderByCreatedDateDesc(User user);
    
    @Query("SELECT COUNT(a) FROM Ad a WHERE a.campaign.user = :user")
    long countByCampaignUser(@Param("user") User user);
    
    @Query("SELECT COUNT(a) FROM Ad a WHERE a.campaign.user = :user AND a.status = :status")
    long countByCampaignUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    // Thêm phương thức đếm theo user trực tiếp
    long countByUser(User user);
    long countByUserAndStatus(User user, String status);

    List<Ad> findTop5ByUserOrderByCreatedDateDesc(User user);
    Page<Ad> findAllByUser(User user, Pageable pageable);
    
    // Debug method for foreign key constraint issue
    List<Ad> findBySelectedContentId(Long selectedContentId);

    // =====================================================
    // OPTIMIZED QUERIES WITH JOIN FETCH TO PREVENT N+1
    // =====================================================

    /**
     * Find ads with campaign eagerly loaded to prevent N+1 queries
     */
    @Query("SELECT a FROM Ad a JOIN FETCH a.campaign WHERE a.user = :user ORDER BY a.createdDate DESC")
    List<Ad> findByUserWithCampaign(@Param("user") User user);

    /**
     * Find top ads with campaign for dashboard
     */
    @Query(value = "SELECT a FROM Ad a JOIN FETCH a.campaign WHERE a.user = :user ORDER BY a.createdDate DESC",
           countQuery = "SELECT COUNT(a) FROM Ad a WHERE a.user = :user")
    Page<Ad> findTop5ByUserWithCampaignOrderByCreatedDateDesc(@Param("user") User user, Pageable pageable);

    /**
     * Find ads by campaign with all related data
     */
    @Query("SELECT a FROM Ad a JOIN FETCH a.campaign c WHERE a.campaign = :campaign AND a.user = :user ORDER BY a.createdDate DESC")
    List<Ad> findByCampaignAndUserWithCampaign(@Param("campaign") Campaign campaign, @Param("user") User user);

    /**
     * Find ad by ID with campaign and user eagerly loaded
     */
    @Query("SELECT a FROM Ad a JOIN FETCH a.campaign JOIN FETCH a.user WHERE a.id = :id AND a.user = :user")
    Optional<Ad> findByIdAndUserWithRelations(@Param("id") Long id, @Param("user") User user);

    /**
     * Find ads with statistics for dashboard
     */
    @Query("SELECT a, c.name as campaignName, COUNT(ac) as contentCount " +
           "FROM Ad a JOIN a.campaign c LEFT JOIN AdContent ac ON ac.ad = a " +
           "WHERE a.user = :user " +
           "GROUP BY a, c.name " +
           "ORDER BY a.createdDate DESC")
    List<Object[]> findAdsWithStatsByUser(@Param("user") User user);

    /**
     * Find ads by status with campaign information
     */
    @Query("SELECT a FROM Ad a JOIN FETCH a.campaign WHERE a.user = :user AND a.status = :status ORDER BY a.createdDate DESC")
    List<Ad> findByUserAndStatusWithCampaign(@Param("user") User user, @Param("status") String status);

    /**
     * Find ads with content count for performance monitoring
     */
    @Query("SELECT a, COUNT(ac) as contentCount, " +
           "SUM(CASE WHEN ac.isSelected = true THEN 1 ELSE 0 END) as selectedContentCount " +
           "FROM Ad a LEFT JOIN AdContent ac ON ac.ad = a " +
           "WHERE a.user = :user " +
           "GROUP BY a " +
           "ORDER BY a.createdDate DESC")
    List<Object[]> findAdsWithContentStats(@Param("user") User user);

    /**
     * Count ads by user and date range
     */
    long countByUserAndCreatedDateBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Fetch a batch of ads for a user (including campaign) to support bulk optimization.
     */
    @Query("SELECT a FROM Ad a LEFT JOIN FETCH a.campaign WHERE a.user = :user AND a.id IN :adIds")
    List<Ad> findByUserAndIdInWithCampaign(@Param("user") User user, @Param("adIds") List<Long> adIds);
}
