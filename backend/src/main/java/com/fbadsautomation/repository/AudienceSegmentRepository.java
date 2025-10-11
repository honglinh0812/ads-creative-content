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

    List<AudienceSegment> findByAdId(Long adId);

    List<AudienceSegment> findByUserId(Long userId);

    Optional<AudienceSegment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT a FROM AudienceSegment a WHERE a.ad.id = :adId AND a.user.id = :userId")
    List<AudienceSegment> findByAdIdAndUserId(@Param("adId") Long adId, @Param("userId") Long userId);

    void deleteByAdId(Long adId);
}
