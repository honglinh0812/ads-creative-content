package com.fbadsautomation.repository;

import com.fbadsautomation.model.WebsiteConversionAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteConversionAdRepository extends JpaRepository<WebsiteConversionAd, Long> {
    List<WebsiteConversionAd> findByAdCampaignId(Long campaignId);
    Optional<WebsiteConversionAd> findByAdId(Long adId);
}
