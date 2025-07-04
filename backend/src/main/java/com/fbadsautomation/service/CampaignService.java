package com.fbadsautomation.service;

import com.fbadsautomation.dto.CampaignCreateRequest;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    /**
     * Get campaign by ID and user
     * @param id The campaign ID
     * @param userEmail The user email
     * @return The campaign
     */
    public Campaign getCampaignByIdAndUser(Long id, Long userId) {
        log.info("Getting campaign: {} for user ID: {}", id, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        return campaignRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    /**
     * Create a new campaign
     * @param request The campaign creation request
     * @param userEmail The user email
     * @return The created campaign
     */
    @Transactional
    public Campaign createCampaign(CampaignCreateRequest request, Long userId) {
        log.info("Creating campaign: {} for user ID: {}", request.getName(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .objective(request.getObjective() != null ? 
                    Campaign.CampaignObjective.valueOf(request.getObjective()) : null)
                .budgetType(request.getBudgetType() != null ? 
                    Campaign.BudgetType.valueOf(request.getBudgetType()) : Campaign.BudgetType.DAILY)
                .dailyBudget(request.getDailyBudget())
                .totalBudget(request.getTotalBudget())
                .targetAudience(request.getTargetAudience())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .user(user)
                .status(Campaign.CampaignStatus.DRAFT)
                .build();
        
        return campaignRepository.save(campaign);
    }

    /**
     * Update an existing campaign
     * @param id The campaign ID
     * @param request The updated campaign details
     * @param userEmail The user email
     * @return The updated campaign
     */
    @Transactional
    public Campaign updateCampaign(Long id, CampaignCreateRequest request, Long userId) {
        log.info("Updating campaign: {} for user ID: {}", id, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        Campaign campaign = campaignRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        
        // Update fields
        campaign.setName(request.getName());
        campaign.setObjective(request.getObjective() != null ? 
            Campaign.CampaignObjective.valueOf(request.getObjective()) : null);
        campaign.setBudgetType(request.getBudgetType() != null ? 
            Campaign.BudgetType.valueOf(request.getBudgetType()) : null);
        campaign.setDailyBudget(request.getDailyBudget());
        campaign.setTotalBudget(request.getTotalBudget());
        campaign.setTargetAudience(request.getTargetAudience());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        
        return campaignRepository.save(campaign);
    }

    /**
     * Delete a campaign
     * @param id The campaign ID
     * @param userEmail The user email
     */
    @Transactional
    public void deleteCampaign(Long id, Long userId) {
        log.info("Deleting campaign: {} for user ID: {}", id, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        Campaign campaign = campaignRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        
        campaignRepository.delete(campaign);
    }

    public Page<Campaign> getAllCampaignsByUserPaginated(Long userId, Pageable pageable) {
        log.info("Getting campaigns for user ID: {} with pageable: {}", userId, pageable);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        return campaignRepository.findAllByUser(user, pageable);
    }
}


