package com.fbadsautomation.service;

import com.fbadsautomation.dto.CampaignCreateRequest;
import com.fbadsautomation.dto.CampaignDTO;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
 
 public class CampaignService {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
   
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
               .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
       return campaignRepository.findByIdAndUser(id, user)
               .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Campaign not found"));
   }

   /**
    * Validate campaign for Facebook export requirements
    * @param campaign The campaign to validate
    * @return List of validation errors
    */
   public List<String> validateCampaignForFacebook(Campaign campaign) {
       List<String> errors = new ArrayList<>();
       
       // Validate campaign name
       if (!StringUtils.hasText(campaign.getName()) || campaign.getName().trim().length() < 3) {
           errors.add("Tên chiến dịch phải có ít nhất 3 ký tự");
       }
       
       if (campaign.getName() != null && campaign.getName().length() > 100) {
           errors.add("Tên chiến dịch không được vượt quá 100 ký tự");
       }
       
       // Validate objective
       if (campaign.getObjective() == null) {
           errors.add("Mục tiêu chiến dịch là bắt buộc cho Facebook");
       } else {
           // Check if objective is supported by Facebook
           switch (campaign.getObjective()) {
               case BRAND_AWARENESS:
               case REACH:
               case TRAFFIC:
               case ENGAGEMENT:
               case APP_INSTALLS:
               case VIDEO_VIEWS:
               case LEAD_GENERATION:
               case CONVERSIONS:
               case CATALOG_SALES:
               case STORE_TRAFFIC:
                   // These are valid Facebook objectives
                   break;
               default:
                   errors.add("Mục tiêu chiến dịch không được hỗ trợ bởi Facebook: " + campaign.getObjective());
           }
       }
       
       // Validate budget
       if (campaign.getBudgetType() == null) {
           errors.add("Loại ngân sách là bắt buộc cho Facebook");
       }
       
       if (campaign.getBudgetType() == Campaign.BudgetType.DAILY) {
           if (campaign.getDailyBudget() == null || campaign.getDailyBudget() <= 0) {
               errors.add("Ngân sách hàng ngày phải lớn hơn 0");
           } else if (campaign.getDailyBudget() < 1.0) {
               errors.add("Ngân sách hàng ngày tối thiểu cho Facebook là $1.00");
           }
       } else if (campaign.getBudgetType() == Campaign.BudgetType.LIFETIME) {
           if (campaign.getTotalBudget() == null || campaign.getTotalBudget() <= 0) {
               errors.add("Tổng ngân sách phải lớn hơn 0");
           } else if (campaign.getTotalBudget() < 1.0) {
               errors.add("Tổng ngân sách tối thiểu cho Facebook là $1.00");
           }
       }
       
       // Validate dates
       if (campaign.getStartDate() == null) {
           errors.add("Ngày bắt đầu là bắt buộc cho Facebook");
       } else if (campaign.getStartDate().isBefore(LocalDateTime.now().toLocalDate())) {
           errors.add("Ngày bắt đầu không thể là ngày trong quá khứ");
       }
       
       if (campaign.getEndDate() != null && campaign.getStartDate() != null) {
           if (campaign.getEndDate().isBefore(campaign.getStartDate())) {
               errors.add("Ngày kết thúc phải sau ngày bắt đầu");
           }
           
           // Facebook requires at least 1 day campaign duration
           if (campaign.getEndDate().equals(campaign.getStartDate())) {
               errors.add("Chiến dịch phải chạy ít nhất 1 ngày");
           }
       }
       
       // Validate target audience
       if (!StringUtils.hasText(campaign.getTargetAudience())) {
           errors.add("Đối tượng mục tiêu là bắt buộc cho Facebook");
       }

       if (campaign.getBidCap() != null && campaign.getBidCap() <= 0) {
           errors.add("Bid cap phải lớn hơn 0");
       }
       
       return errors;
   }
   
   /**
    * Check if campaign is ready for Facebook export
    * @param campaignId The campaign ID
    * @param userId The user ID
    * @return true if campaign is ready for export
    * @throws ApiException if campaign is not ready with validation errors
    */
   public boolean isCampaignReadyForFacebookExport(Long campaignId, Long userId) {
       Campaign campaign = getCampaignByIdAndUser(campaignId, userId);
       List<String> errors = validateCampaignForFacebook(campaign);
       
       if (!errors.isEmpty()) {
           String errorMessage = "Chiến dịch chưa sẵn sàng cho Facebook export:\n" + String.join("\n", errors);
           throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
       }
       
       return true;
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
               .objective(request.getObjective() != null ? Campaign.CampaignObjective.valueOf(request.getObjective()) : null)
               .budgetType(request.getBudgetType() != null ? Campaign.BudgetType.valueOf(request.getBudgetType()) : Campaign.BudgetType.DAILY)
               .dailyBudget(request.getDailyBudget())
               .totalBudget(request.getTotalBudget())
               .targetAudience(request.getTargetAudience())
               .bidCap(request.getBidCap())
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
       campaign.setObjective(request.getObjective() != null ? Campaign.CampaignObjective.valueOf(request.getObjective()) : null);
       campaign.setBudgetType(request.getBudgetType() != null ? Campaign.BudgetType.valueOf(request.getBudgetType()) : null);
       campaign.setDailyBudget(request.getDailyBudget());
       campaign.setTotalBudget(request.getTotalBudget());
       campaign.setTargetAudience(request.getTargetAudience());
       campaign.setBidCap(request.getBidCap());
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

   /**
    * Get campaigns with ad count and created date for display (Issue #7 fix)
    * Returns CampaignDTO with totalAds and createdDate populated
    * @param userId The user ID
    * @param pageable Pagination parameters
    * @return Page of CampaignDTO with all fields
    */
   public Page<CampaignDTO> getAllCampaignsWithStatsByUserPaginated(Long userId, Pageable pageable) {
       log.info("Getting campaigns with stats for user ID: {} with pageable: {}", userId, pageable);

       User user = userRepository.findById(userId)
               .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

       // Use optimized query with ad count
       Page<Object[]> results = campaignRepository.findAllByUserWithAdCount(user, pageable);

       // Map Object[] to CampaignDTO
       return results.map(row -> {
           Campaign campaign = (Campaign) row[0];
           Long adCount = (Long) row[1];

           return new CampaignDTO(
               campaign.getId(),
               campaign.getName(),
               campaign.getStatus() != null ? campaign.getStatus().toString() : null,
               campaign.getObjective() != null ? campaign.getObjective().toString() : null,
               campaign.getBudgetType() != null ? campaign.getBudgetType().toString() : null,
               campaign.getDailyBudget(),
               campaign.getTotalBudget(),
               campaign.getTargetAudience(),
               campaign.getBidCap(),
               campaign.getStartDate(),
               campaign.getEndDate(),
               adCount.intValue(),
               campaign.getCreatedDate()
           );
       });
   }

   /**
    * DEPRECATED: Campaign status is now automatically updated by database trigger (V24)
    *
    * This method is no longer needed as the campaign_status_update_trigger handles:
    * - DRAFT -> READY when ads exist
    * - READY -> DRAFT when no ads remain
    * - EXPORTED -> READY when ads are added
    *
    * Keeping this method for backward compatibility but it's not called anywhere.
    * Consider removing in future cleanup.
    *
    * @deprecated Use database trigger (V24) instead
    * @param campaignId The campaign ID
    * @param userId The user ID
    */
   @Deprecated
   @Transactional
   public void updateCampaignStatusBasedOnAds(Long campaignId, Long userId) {
       log.warn("DEPRECATED: updateCampaignStatusBasedOnAds called for campaign {}. Status is auto-updated by DB trigger (V24)", campaignId);
       // Campaign status is automatically handled by database trigger
       // No manual update needed
   }

   /**
    * Mark campaign as exported after successful Facebook export (Issue #7)
    * @param campaignId The campaign ID
    * @param userId The user ID
    */
   @Transactional
   public void markCampaignAsExported(Long campaignId, Long userId) {
       log.info("Marking campaign {} as EXPORTED for user: {}", campaignId, userId);

       Campaign campaign = getCampaignByIdAndUser(campaignId, userId);

       if (campaign.getStatus() == Campaign.CampaignStatus.READY) {
           campaign.setStatus(Campaign.CampaignStatus.EXPORTED);
           campaignRepository.save(campaign);
           log.info("Campaign {} status changed: READY -> EXPORTED", campaignId);
       } else {
           log.warn("Cannot mark campaign {} as EXPORTED: current status is {}",
                    campaignId, campaign.getStatus());
       }
   }
}
