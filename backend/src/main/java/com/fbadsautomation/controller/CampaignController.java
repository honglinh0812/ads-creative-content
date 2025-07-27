package com.fbadsautomation.controller;

import com.fbadsautomation.dto.CampaignCreateRequest;
import com.fbadsautomation.dto.CampaignDTO;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.service.CampaignService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

public class CampaignController {

    private final CampaignService campaignService;
    private CampaignDTO toDTO(Campaign campaign) {
        return new CampaignDTO(
            campaign.getId(),
            campaign.getName(),
            campaign.getStatus().toString(),
            campaign.getObjective() != null ? campaign.getObjective().toString() : null,
            campaign.getBudgetType() != null ? campaign.getBudgetType().toString() : null,
            campaign.getDailyBudget(),
            campaign.getTotalBudget(),
            campaign.getTargetAudience(),
            campaign.getStartDate(),
            campaign.getEndDate()
        );
    }

    @GetMapping
    public ResponseEntity<Page<CampaignDTO>> getAllCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        log.info("Getting all campaigns for user: {} with page {} and size {}", authentication.getName(), page, size);
        Long userId = Long.valueOf(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Campaign> campaigns = campaignService.getAllCampaignsByUserPaginated(userId, pageable);
        Page<CampaignDTO> dtos = campaigns.map(this::toDTO);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDTO> getCampaign(@PathVariable Long id, Authentication authentication) {
        log.info("Getting campaign: {} for user: {}", id, authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        Campaign campaign = campaignService.getCampaignByIdAndUser(id, userId);
        CampaignDTO dto = toDTO(campaign);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<CampaignDTO> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request, 
            Authentication authentication) {
        log.info("Creating campaign: {} for user: {}", request.getName(), authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        Campaign saved = campaignService.createCampaign(request, userId);
        CampaignDTO dto = toDTO(saved);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignDTO> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignCreateRequest request,
            Authentication authentication) {
        log.info("Updating campaign: {} for user: {}", id, authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        Campaign saved = campaignService.updateCampaign(id, request, userId);
        CampaignDTO dto = toDTO(saved);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id, Authentication authentication) {
        log.info("Deleting campaign: {} for user: {}", id, authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        campaignService.deleteCampaign(id, userId);
        return ResponseEntity.noContent().build();
    }
}
