package com.fbadsautomation.controller;

import com.fbadsautomation.dto.CampaignCreateRequest;
import com.fbadsautomation.dto.CampaignDTO;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campaigns")
@CrossOrigin(origins = "*")
@Tag(name = "Campaigns", description = "API endpoints for campaign management")
@SecurityRequirement(name = "Bearer Authentication")

public class CampaignController {

    private static final Logger log = LoggerFactory.getLogger(CampaignController.class);
    
    private final CampaignService campaignService;

    @Autowired
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }
    private CampaignDTO toDTO(Campaign campaign) {
        // Calculate totalAds from ads collection
        int totalAds = campaign.getAds() != null ? campaign.getAds().size() : 0;

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
            campaign.getPerformanceGoal(),
            campaign.getStartDate(),
            campaign.getEndDate(),
            totalAds,
            campaign.getCreatedDate()
        );
    }

    @Operation(summary = "Get all campaigns", description = "Retrieve paginated list of campaigns for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Page<CampaignDTO>> getAllCampaigns(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        log.info("Getting all campaigns for user: {} with page {} and size {}", authentication.getName(), page, size);
        Long userId = Long.valueOf(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        // Lấy tất cả các campaign trong hệ thống
        Page<CampaignDTO> dtos = campaignService.getAllCampaignsWithStatsByUserPaginated(userId, pageable);
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get campaign by ID", description = "Retrieve a specific campaign by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Campaign retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Campaign not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CampaignDTO> getCampaign(
        @Parameter(description = "Campaign ID") @PathVariable Long id, 
        Authentication authentication) {
        log.info("Getting campaign: {} for user: {}", id, authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        Campaign campaign = campaignService.getCampaignByIdAndUser(id, userId);
        CampaignDTO dto = toDTO(campaign);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Create new campaign", description = "Create a new advertising campaign")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Campaign created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid campaign data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<CampaignDTO> createCampaign(
            @Parameter(description = "Campaign creation data") @Valid @RequestBody CampaignCreateRequest request, 
            Authentication authentication) {
        log.info("Creating campaign: {} for user: {}", request.getName(), authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        Campaign saved = campaignService.createCampaign(request, userId);
        CampaignDTO dto = toDTO(saved);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update campaign", description = "Update an existing campaign")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Campaign updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid campaign data"),
        @ApiResponse(responseCode = "404", description = "Campaign not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CampaignDTO> updateCampaign(
            @Parameter(description = "Campaign ID") @PathVariable Long id,
            @Parameter(description = "Updated campaign data") @Valid @RequestBody CampaignCreateRequest request,
            Authentication authentication) {
        log.info("Updating campaign: {} for user: {}", id, authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        Campaign saved = campaignService.updateCampaign(id, request, userId);
        CampaignDTO dto = toDTO(saved);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Delete campaign", description = "Delete a campaign by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Campaign deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Campaign not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
        @Parameter(description = "Campaign ID") @PathVariable Long id, 
        Authentication authentication) {
        log.info("Deleting campaign: {} for user: {}", id, authentication.getName());
        Long userId = Long.valueOf(authentication.getName());
        campaignService.deleteCampaign(id, userId);
        return ResponseEntity.noContent().build();
    }
}
