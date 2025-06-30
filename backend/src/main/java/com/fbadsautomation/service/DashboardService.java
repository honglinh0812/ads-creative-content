package com.fbadsautomation.service;

import com.fbadsautomation.dto.DashboardResponse;
import com.fbadsautomation.model.User;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.repository.UserRepository;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.AdRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdRepository adRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Long userId) {
        try {
            log.info("DashboardService: Getting dashboard data for user ID: {}", userId);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            log.info("DashboardService: Found user: {} with ID: {}", user.getEmail(), user.getId());

            // Get campaigns summary
            log.info("DashboardService: Fetching campaigns for user: {}", user.getId());
            List<DashboardResponse.CampaignSummary> campaigns = campaignRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .map(campaign -> {
                        log.debug("DashboardService: Processing campaign: {}", campaign.getId());
                        try {
                            return new DashboardResponse.CampaignSummary(
                                    campaign.getId(),
                                    campaign.getName(),
                                    campaign.getObjective() != null ? campaign.getObjective().toString() : null,
                                    campaign.getStatus() != null ? campaign.getStatus().toString() : "DRAFT",
                                    campaign.getDailyBudget() != null ? campaign.getDailyBudget() : 
                                        (campaign.getTotalBudget() != null ? campaign.getTotalBudget() : 0.0),
                                    campaign.getCreatedAt(),
                                    campaign.getAds() != null ? campaign.getAds().size() : 0
                            );
                        } catch (Exception e) {
                            log.error("DashboardService: Error mapping campaign {}: {}", campaign.getId(), e.getMessage(), e);
                            return new DashboardResponse.CampaignSummary(
                                    campaign.getId(),
                                    campaign.getName(),
                                    null,
                                    "DRAFT",
                                    0.0,
                                    campaign.getCreatedAt(),
                                    0
                            );
                        }
                    })
                    .collect(Collectors.toList());
            log.info("DashboardService: Finished fetching {} campaigns for user: {}", campaigns.size(), user.getId());

            // Get recent ads summary - simplified to avoid potential issues
            log.info("DashboardService: Fetching recent ads for user: {}", user.getId());
            List<DashboardResponse.AdSummary> recentAds;
            try {
                recentAds = adRepository.findTop10ByOrderByCreatedDateDesc()
                        .stream()
                        .filter(ad -> {
                            log.debug("DashboardService: Filtering ad: {}", ad.getId());
                            try {
                                return ad.getCampaign() != null && 
                                       ad.getCampaign().getUser() != null && 
                                       ad.getCampaign().getUser().getId().equals(user.getId());
                            } catch (Exception e) {
                                log.warn("DashboardService: Error filtering ad {}: {}", ad.getId(), e.getMessage(), e); // Log full stack trace
                                return false;
                            }
                        })
                        .map(ad -> {
                            log.debug("DashboardService: Processing ad: {}", ad.getId());
                            try {
                                return new DashboardResponse.AdSummary(
                                        ad.getId(),
                                        ad.getName(),
                                        ad.getAdType() != null ? ad.getAdType().toString() : null,
                                        ad.getStatus() != null ? ad.getStatus() : "DRAFT",
                                        ad.getCampaign() != null ? ad.getCampaign().getName() : "Unknown",
                                        ad.getCreatedDate(),
                                        ad.getImageUrl()
                                );
                            } catch (Exception e) {
                                log.error("DashboardService: Error mapping ad {}: {}", ad.getId(), e.getMessage(), e); // Log full stack trace
                                return new DashboardResponse.AdSummary(
                                        ad.getId(),
                                        ad.getName(),
                                        null,
                                        "DRAFT",
                                        "Unknown",
                                        ad.getCreatedDate(),
                                        null
                                );
                            }
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("DashboardService: Error getting recent ads: {}", e.getMessage(), e); // Log full stack trace
                recentAds = List.of(); // Empty list as fallback
            }
            log.info("DashboardService: Finished fetching {} recent ads for user: {}", recentAds.size(), user.getId());

            // Get dashboard stats with error handling
            log.info("DashboardService: Calculating dashboard stats for user: {}", user.getId());
            long totalCampaigns = 0;
            long totalAds = 0;
            long activeCampaigns = 0;
            long activeAds = 0;

            try {
                totalCampaigns = campaignRepository.countByUser(user);
                log.info("DashboardService: Total campaigns count: {}", totalCampaigns);
            } catch (Exception e) {
                log.error("DashboardService: Error counting campaigns: {}", e.getMessage(), e);
            }

            try {
                totalAds = adRepository.countByCampaignUser(user);
                log.info("DashboardService: Total ads count: {}", totalAds);
            } catch (Exception e) {
                log.error("DashboardService: Error counting ads: {}", e.getMessage(), e);
            }

            try {
                activeCampaigns = campaignRepository.countByUserAndStatus(user, Campaign.CampaignStatus.ACTIVE);
                log.info("DashboardService: Active campaigns count: {}", activeCampaigns);
            } catch (Exception e) {
                log.error("DashboardService: Error counting active campaigns: {}", e.getMessage(), e);
            }

            try {
                activeAds = adRepository.countByCampaignUserAndStatus(user, "ACTIVE");
                log.info("DashboardService: Active ads count: {}", activeAds);
            } catch (Exception e) {
                log.error("DashboardService: Error counting active ads: {}", e.getMessage(), e);
            }
            log.info("DashboardService: Finished calculating dashboard stats.");

            DashboardResponse.DashboardStats stats = new DashboardResponse.DashboardStats(
                    totalCampaigns, totalAds, activeCampaigns, activeAds
            );

            DashboardResponse response = new DashboardResponse(campaigns, recentAds, stats);
            log.info("DashboardService: Dashboard data successfully prepared for user ID: {}", userId);
            
            return response;
            
        } catch (Exception e) {
            log.error("DashboardService: Critical error in getDashboardData for user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get dashboard data: " + e.getMessage(), e);
        }
    }
}