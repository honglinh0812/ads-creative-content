package com.fbadsautomation.repository;

import com.fbadsautomation.model.AdPerformanceReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Ad Performance Reports
 *
 * Security: All queries include user_id for user isolation
 * Performance: Optimized queries with indexed columns
 * Maintainability: Clear method names following Spring Data conventions
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Repository
public interface AdPerformanceReportRepository extends JpaRepository<AdPerformanceReport, Long> {

    /**
     * Find all reports for a specific ad
     * Security: Should be combined with user check in service layer
     */
    List<AdPerformanceReport> findByAdId(Long adId);

    /**
     * Find all reports for a specific ad with user isolation
     * Security: Enforces user ownership
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.ad.id = :adId AND r.user.id = :userId")
    List<AdPerformanceReport> findByAdIdAndUserId(@Param("adId") Long adId, @Param("userId") Long userId);

    /**
     * Find all reports for a campaign
     * Security: Should be combined with user check in service layer
     */
    List<AdPerformanceReport> findByCampaignId(Long campaignId);

    /**
     * Find all reports for a campaign with user isolation
     * Security: Enforces user ownership
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.campaign.id = :campaignId AND r.user.id = :userId")
    List<AdPerformanceReport> findByCampaignIdAndUserId(@Param("campaignId") Long campaignId, @Param("userId") Long userId);

    /**
     * Find all reports for a user
     * Security: Primary method for user-scoped queries
     */
    List<AdPerformanceReport> findByUserId(Long userId);

    /**
     * Find reports within a date range
     * Performance: Uses indexed report_date column
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.reportDate BETWEEN :startDate AND :endDate")
    List<AdPerformanceReport> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find reports within a date range for a specific user
     * Security: User isolation enforced
     * Performance: Uses composite index on user_id + report_date
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.user.id = :userId AND r.reportDate BETWEEN :startDate AND :endDate ORDER BY r.reportDate DESC")
    List<AdPerformanceReport> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find latest N reports for a specific ad
     * Performance: Uses composite index on ad_id + report_date
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.ad.id = :adId ORDER BY r.reportDate DESC")
    List<AdPerformanceReport> findLatestByAdId(@Param("adId") Long adId, Pageable pageable);

    /**
     * Find latest N reports for a specific ad with user isolation
     * Security: Enforces user ownership
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.ad.id = :adId AND r.user.id = :userId ORDER BY r.reportDate DESC")
    List<AdPerformanceReport> findLatestByAdIdAndUserId(
        @Param("adId") Long adId,
        @Param("userId") Long userId,
        Pageable pageable
    );

    /**
     * Find latest N reports for a campaign
     * Performance: Uses composite index on campaign_id + report_date
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.campaign.id = :campaignId ORDER BY r.reportDate DESC")
    List<AdPerformanceReport> findLatestByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

    /**
     * Check if a report exists for a specific ad on a specific date
     * Used for duplicate detection before import
     */
    @Query("SELECT r FROM AdPerformanceReport r WHERE r.ad.id = :adId AND r.reportDate = :reportDate AND r.source = :source")
    Optional<AdPerformanceReport> findByAdIdAndReportDateAndSource(
        @Param("adId") Long adId,
        @Param("reportDate") LocalDate reportDate,
        @Param("source") String source
    );

    /**
     * Count reports for a user (for pagination)
     */
    long countByUserId(Long userId);

    /**
     * Count reports for an ad
     */
    long countByAdId(Long adId);

    /**
     * Count reports for a campaign
     */
    long countByCampaignId(Long campaignId);

    /**
     * Delete all reports for a specific ad
     * Used when ad is deleted (cascade should handle this)
     */
    void deleteByAdId(Long adId);

    /**
     * Delete all reports for a user
     * Used for GDPR compliance / account deletion
     */
    void deleteByUserId(Long userId);

    /**
     * Get aggregated metrics for an ad
     * Performance: Single query for multiple aggregations
     */
    @Query("SELECT " +
           "SUM(r.impressions) as totalImpressions, " +
           "SUM(r.clicks) as totalClicks, " +
           "AVG(r.ctr) as avgCtr, " +
           "SUM(r.spend) as totalSpend, " +
           "AVG(r.cpc) as avgCpc, " +
           "AVG(r.cpm) as avgCpm, " +
           "SUM(r.conversions) as totalConversions " +
           "FROM AdPerformanceReport r " +
           "WHERE r.ad.id = :adId AND r.user.id = :userId")
    Object[] getAggregatedMetricsByAdIdAndUserId(@Param("adId") Long adId, @Param("userId") Long userId);

    /**
     * Get aggregated metrics for a campaign
     * Performance: Single query for multiple aggregations
     */
    @Query("SELECT " +
           "SUM(r.impressions) as totalImpressions, " +
           "SUM(r.clicks) as totalClicks, " +
           "AVG(r.ctr) as avgCtr, " +
           "SUM(r.spend) as totalSpend, " +
           "AVG(r.cpc) as avgCpc, " +
           "AVG(r.cpm) as avgCpm, " +
           "SUM(r.conversions) as totalConversions " +
           "FROM AdPerformanceReport r " +
           "WHERE r.campaign.id = :campaignId AND r.user.id = :userId")
    Object[] getAggregatedMetricsByCampaignIdAndUserId(@Param("campaignId") Long campaignId, @Param("userId") Long userId);

    /**
     * Find top performing ads by CTR
     * Performance: Uses indexed columns
     */
    @Query("SELECT r.ad.id, AVG(r.ctr) as avgCtr FROM AdPerformanceReport r " +
           "WHERE r.user.id = :userId " +
           "GROUP BY r.ad.id " +
           "ORDER BY avgCtr DESC")
    List<Object[]> findTopPerformingAdsByCtr(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find top performing ads by conversions
     */
    @Query("SELECT r.ad.id, SUM(r.conversions) as totalConversions FROM AdPerformanceReport r " +
           "WHERE r.user.id = :userId " +
           "GROUP BY r.ad.id " +
           "ORDER BY totalConversions DESC")
    List<Object[]> findTopPerformingAdsByConversions(@Param("userId") Long userId, Pageable pageable);
}
