package com.fbadsautomation.repository;

import com.fbadsautomation.model.LeadFormAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadFormAdRepository extends JpaRepository<LeadFormAd, Long> {
    List<LeadFormAd> findByAdCampaignId(Long campaignId);
    Optional<LeadFormAd> findByAdId(Long adId);
}
