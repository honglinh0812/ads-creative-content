package com.fbadsautomation.repository;

import com.fbadsautomation.model.AudienceSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AudienceSegmentRepository extends JpaRepository<AudienceSegment, Long> {

    // Issue #9: Changed from Ad to Campaign
    List<AudienceSegment> findByCampaignId(Long campaignId);

    List<AudienceSegment> findByUserId(Long userId);

    Optional<AudienceSegment> findByIdAndUserId(Long id, Long userId);

    // Issue #9: Updated query to use campaign instead of ad
    @Query("SELECT a FROM AudienceSegment a WHERE a.campaign.id = :campaignId AND a.user.id = :userId")
    List<AudienceSegment> findByCampaignIdAndUserId(@Param("campaignId") Long campaignId, @Param("userId") Long userId);

    // Issue #9: Updated to delete by campaign
    void deleteByCampaignId(Long campaignId);

    // Deprecated methods (for backward compatibility during migration)
    @Deprecated
    default List<AudienceSegment> findByAdId(Long adId) {
        throw new UnsupportedOperationException("findByAdId is deprecated - use findByCampaignId instead (Issue #9)");
    }

    @Deprecated
    default void deleteByAdId(Long adId) {
        throw new UnsupportedOperationException("deleteByAdId is deprecated - use deleteByCampaignId instead (Issue #9)");
    }
}
