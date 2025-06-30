package com.fbadsautomation.repository;

import com.fbadsautomation.model.PagePostAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagePostAdRepository extends JpaRepository<PagePostAd, Long> {
    List<PagePostAd> findByAdCampaignId(Long campaignId);
    Optional<PagePostAd> findByAdId(Long adId);
}
