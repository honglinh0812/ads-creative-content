package com.fbadsautomation.repository;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdContentRepository extends JpaRepository<AdContent, Long> {
    List<AdContent> findByAd(Ad ad);
    List<AdContent> findByAdOrderByCreatedDateDesc(Ad ad);
    List<AdContent> findByAdOrderByPreviewOrder(Ad ad);
    Optional<AdContent> findByIdAndAd(Long id, Ad ad);
    List<AdContent> findByAdAndIsSelected(Ad ad, Boolean isSelected);
    Optional<AdContent> findByIdAndAdId(Long id, Long adId);
    List<AdContent> findByAdId(Long adId);
    
    // Thêm các phương thức truy vấn theo user để đảm bảo cô lập dữ liệu
    List<AdContent> findByUser(User user);
    List<AdContent> findByUserOrderByCreatedDateDesc(User user);
    Optional<AdContent> findByIdAndUser(Long id, User user);
    List<AdContent> findByAdAndUser(Ad ad, User user);
    List<AdContent> findByAdAndUserOrderByPreviewOrder(Ad ad, User user);

    /**
     * Count total content by user
     */
    long countByUser(User user);

    /**
     * Find selected content by user with relations
     */
    @Query("SELECT ac FROM AdContent ac JOIN FETCH ac.ad a JOIN FETCH a.campaign c WHERE c.user = :user AND ac.isSelected = true")
    List<AdContent> findSelectedByUserWithRelations(@Param("user") User user);

    /**
     * Get AI provider statistics by user
     */
    @Query("SELECT ac.aiProvider, COUNT(ac), " +
           "CAST(SUM(CASE WHEN ac.isSelected = true THEN 1 ELSE 0 END) AS double) / COUNT(ac) as selectionRate " +
           "FROM AdContent ac JOIN ac.ad a JOIN a.campaign c " +
           "WHERE c.user = :user AND ac.aiProvider IS NOT NULL " +
           "GROUP BY ac.aiProvider " +
           "ORDER BY COUNT(ac) DESC")
    List<Object[]> findAIProviderStatsByUser(@Param("user") User user);

    /**
     * Get content type statistics by user
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN ac.headline IS NOT NULL THEN 'headline' " +
           "  WHEN ac.primaryText IS NOT NULL THEN 'primary_text' " +
           "  WHEN ac.description IS NOT NULL THEN 'description' " +
           "  WHEN ac.callToAction IS NOT NULL THEN 'call_to_action' " +
           "  ELSE 'other' " +
           "END as contentType, " +
           "COUNT(ac) " +
           "FROM AdContent ac JOIN ac.ad a JOIN a.campaign c " +
           "WHERE c.user = :user " +
           "GROUP BY " +
           "CASE " +
           "  WHEN ac.headline IS NOT NULL THEN 'headline' " +
           "  WHEN ac.primaryText IS NOT NULL THEN 'primary_text' " +
           "  WHEN ac.description IS NOT NULL THEN 'description' " +
           "  WHEN ac.callToAction IS NOT NULL THEN 'call_to_action' " +
           "  ELSE 'other' " +
           "END " +
           "ORDER BY COUNT(ac) DESC")
    List<Object[]> findContentTypeStatsByUser(@Param("user") User user);
    // =====================================================
    // OPTIMIZED QUERIES WITH JOIN FETCH TO PREVENT N+1
    // =====================================================

    /**
     * Find ad contents with ad and campaign eagerly loaded
     */
    @Query("SELECT ac FROM AdContent ac JOIN FETCH ac.ad a JOIN FETCH a.campaign WHERE ac.user = :user ORDER BY ac.createdDate DESC")
    List<AdContent> findByUserWithAdAndCampaign(@Param("user") User user);

    /**
     * Find ad contents by ad with all relations loaded
     */
    @Query("SELECT ac FROM AdContent ac JOIN FETCH ac.ad a JOIN FETCH a.campaign WHERE ac.ad = :ad AND ac.user = :user ORDER BY ac.previewOrder")
    List<AdContent> findByAdAndUserWithRelations(@Param("ad") Ad ad, @Param("user") User user);
    /**
     * Find ad contents with performance metrics
     */
    @Query("SELECT ac, a.name as adName, c.name as campaignName " +
           "FROM AdContent ac JOIN ac.ad a JOIN a.campaign c " +
           "WHERE ac.user = :user " +
           "ORDER BY ac.createdDate DESC")
    List<Object[]> findContentWithPerformanceMetrics(@Param("user") User user);

    /**
     * Batch update selected status for ad contents
     */
    @Query("UPDATE AdContent ac SET ac.isSelected = :selected WHERE ac.ad = :ad AND ac.user = :user")
    void updateSelectedStatusByAdAndUser(@Param("ad") Ad ad, @Param("user") User user, @Param("selected") Boolean selected);
}
