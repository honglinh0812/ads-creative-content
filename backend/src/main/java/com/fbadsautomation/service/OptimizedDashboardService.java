package com.fbadsautomation.service;

import com.fbadsautomation.dto.DashboardResponse;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service

public class OptimizedDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdRepository adRepository;

    /**
     * Get optimized dashboard data with minimal database queries
     * This method uses JOIN FETCH queries to prevent N+1 query problems
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardDataOptimized(Long userId) {
        try {
            log.info("OptimizedDashboardService: Getting dashboard data for user ID: {}", userId);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            log.info("OptimizedDashboardService: Found user: {} with ID: {}", user.getEmail(), user.getId());

            // Single query to get campaigns with statistics
            log.info("OptimizedDashboardService: Fetching campaigns with statistics");
            List<Object[]> campaignStats = campaignRepository.findCampaignsWithStatsByUser(user);
            
            List<DashboardResponse.CampaignSummary> campaigns = campaignStats.stream()
                    .limit(5) // Take top 5
                    .map(row -> {
                        Campaign campaign = (Campaign) row[0];
                        Long adCount = (Long) row[1];
                        Long activeAdCount = (Long) row[2];
                        
                        log.debug("OptimizedDashboardService: Processing campaign: {} with {} ads ({} active)", campaign.getId(), adCount, activeAdCount);
                        
                        return new DashboardResponse.CampaignSummary(
                                campaign.getId(),
                                campaign.getName(),
                                campaign.getObjective() != null ? campaign.getObjective().toString() : null,
                                campaign.getStatus() != null ? campaign.getStatus().toString() : "DRAFT",
                                campaign.getDailyBudget() != null ? campaign.getDailyBudget() : 
                                    (campaign.getTotalBudget() != null ? campaign.getTotalBudget() : 0.0),
                                campaign.getCreatedDate(),
                                adCount.intValue()
                        );
                    })
                    .collect(Collectors.toList());

            // Single query to get ads with campaign information (prevents N+1)
            log.info("OptimizedDashboardService: Fetching recent ads with campaign info");
            Pageable top5Ads = PageRequest.of(0, 5);
            List<Ad> recentAdsWithCampaign = adRepository.findTop5ByUserWithCampaignOrderByCreatedDateDesc(user, top5Ads)
                    .getContent();
            
            List<DashboardResponse.AdSummary> recentAds = recentAdsWithCampaign.stream()
                    .map(ad -> {
                        log.debug("OptimizedDashboardService: Processing ad: {} from campaign: {}", ad.getId(), ad.getCampaign().getName());
                        
                        return new DashboardResponse.AdSummary(
                                ad.getId(),
                                ad.getName(),
                                ad.getAdType() != null ? ad.getAdType().toString() : null,
                                ad.getStatus() != null ? ad.getStatus() : "DRAFT",
                                ad.getCampaign().getName(), // No additional query needed!
                                ad.getCreatedDate(),
                                ad.getImageUrl(),
                                ad.getVideoUrl(),
                                ad.getHeadline(),
                                ad.getPrimaryText(),
                                ad.getDescription(),
                                ad.getCallToAction() != null ? ad.getCallToAction().getLabel() : null
                        );
                    })
                    .collect(Collectors.toList());

            // Get dashboard statistics with optimized queries
            log.info("OptimizedDashboardService: Calculating dashboard statistics");
            DashboardResponse.DashboardStats stats = calculateOptimizedStats(user);
            DashboardResponse response = new DashboardResponse(campaigns, recentAds, stats);
            log.info("OptimizedDashboardService: Dashboard data successfully prepared for user ID: {}", userId);
            
            return response;
            
        } catch (Exception e) {
            log.error("OptimizedDashboardService: Critical error in getDashboardDataOptimized for user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get dashboard data: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate dashboard statistics with optimized queries
     */
    private DashboardResponse.DashboardStats calculateOptimizedStats(User user) {
        try {
            // Use batch queries to get all statistics at once
            long totalCampaigns = campaignRepository.countByUser(user);
            long totalAds = adRepository.countByUser(user);
            long activeCampaigns = campaignRepository.countByUserAndStatus(user, Campaign.CampaignStatus.ACTIVE);
            long activeAds = adRepository.countByUserAndStatus(user, "ACTIVE");

            log.info("OptimizedDashboardService: Statistics - Campaigns: {} (Active: {}), Ads: {} (Active: {})", totalCampaigns, activeCampaigns, totalAds, activeAds);

            return new DashboardResponse.DashboardStats(
                    totalCampaigns, totalAds, activeCampaigns, activeAds
            );
            
        } catch (Exception e) {
            log.error("OptimizedDashboardService: Error calculating statistics: {}", e.getMessage(), e);
            return new DashboardResponse.DashboardStats(0, 0, 0, 0);
        }
    }

    /**
     * Get campaign performance metrics with optimized queries
     */
    @Transactional(readOnly = true)
    public List<CampaignPerformanceMetric> getCampaignPerformanceMetrics(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            List<Object[]> budgetAnalysis = campaignRepository.findCampaignsWithBudgetAnalysis(user);
            
            return budgetAnalysis.stream()
                    .map(row -> {
                        Campaign campaign = (Campaign) row[0];
                        Double dailyBudget = (Double) row[1];
                        Double totalBudget = (Double) row[2];
                        Long adCount = (Long) row[3];
                        
                        return new CampaignPerformanceMetric(
                                campaign.getId(),
                                campaign.getName(),
                                dailyBudget,
                                totalBudget,
                                adCount.intValue(),
                                campaign.getStatus().toString()
                        );
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("OptimizedDashboardService: Error getting campaign performance metrics: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get ad performance statistics with optimized queries
     */
    @Transactional(readOnly = true)
    public List<AdPerformanceMetric> getAdPerformanceMetrics(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            List<Object[]> adStats = adRepository.findAdsWithContentStats(user);
            
            return adStats.stream()
                    .map(row -> {
                        Ad ad = (Ad) row[0];
                        Long contentCount = (Long) row[1];
                        Long selectedContentCount = (Long) row[2];
                        
                        return new AdPerformanceMetric(
                                ad.getId(),
                                ad.getName(),
                                ad.getCampaign().getName(),
                                contentCount.intValue(),
                                selectedContentCount.intValue(),
                                ad.getStatus()
                        );
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("OptimizedDashboardService: Error getting ad performance metrics: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // Performance metric classes
    public static class CampaignPerformanceMetric {
        private Long id;
        private String name;
        private Double dailyBudget;
        private Double totalBudget;
        private Integer adCount;
        private String status;

        public CampaignPerformanceMetric(Long id, String name, Double dailyBudget, 
                                       Double totalBudget, Integer adCount, String status) {
            this.id = id;
            this.name = name;
            this.dailyBudget = dailyBudget;
            this.totalBudget = totalBudget;
            this.adCount = adCount;
            this.status = status;
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public Double getDailyBudget() { return dailyBudget; }
        public Double getTotalBudget() { return totalBudget; }
        public Integer getAdCount() { return adCount; }
        public String getStatus() { return status; }
    }

    public static class AdPerformanceMetric {
        private Long id;
        private String name;
        private String campaignName;
        private Integer contentCount;
        private Integer selectedContentCount;
        private String status;

        public AdPerformanceMetric(Long id, String name, String campaignName, 
                                 Integer contentCount, Integer selectedContentCount, String status) {
            this.id = id;
            this.name = name;
            this.campaignName = campaignName;
            this.contentCount = contentCount;
            this.selectedContentCount = selectedContentCount;
            this.status = status;
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getCampaignName() { return campaignName; }
        public Integer getContentCount() { return contentCount; }
        public Integer getSelectedContentCount() { return selectedContentCount; }
        public String getStatus() { return status; }
    }
}
