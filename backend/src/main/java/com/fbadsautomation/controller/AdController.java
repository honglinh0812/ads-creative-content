package com.fbadsautomation.controller;

import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdResponse;
import com.fbadsautomation.service.AIContentService;
import com.fbadsautomation.service.AdService;
import com.fbadsautomation.service.MetaAdLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ads")
@CrossOrigin(origins = "*")
@Tag(name = "Ads", description = "API endpoints for advertisement management and AI content generation")
@SecurityRequirement(name = "Bearer Authentication")
public class AdController {

    private static final Logger log = LoggerFactory.getLogger(AdController.class);
    
    private final AdService adService;
    private final AIContentService aiContentService;
    private final MetaAdLibraryService metaAdLibraryService;

    @Autowired
    public AdController(AdService adService, AIContentService aiContentService, MetaAdLibraryService metaAdLibraryService) {
        this.adService = adService;
        this.aiContentService = aiContentService;
        this.metaAdLibraryService = metaAdLibraryService;
    }

    @Operation(summary = "Get all ads", description = "Retrieve paginated list of ads for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ads retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Page<AdResponse>> getAllAds(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        log.info("Getting all ads for user: {} with page {} and size {}", authentication.getName(), page, size);
        Long userId = Long.parseLong(authentication.getName());

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<AdResponse> ads = adService.getAllAdResponsesByUserPaginated(userId, pageable);
            return ResponseEntity.ok(ads);
        } catch (Exception e) {
            log.error("🔥 Failed to load ads for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get ad by ID", description = "Retrieve a specific ad by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Ad not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Ad> getAd(
            @Parameter(description = "Ad ID") @PathVariable Long id, 
            Authentication authentication) {
        log.info("Getting ad: {} for user: {}", id, authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adService.getAdByIdAndUser(id, userId));
    }

    @Operation(summary = "Generate ad content", description = "Generate AI-powered ad content based on the provided request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad content generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "500", description = "Internal server error during generation")
    })
    @PostMapping("/enhance-image")
    public ResponseEntity<Map<String, String>> enhanceImage(
            @RequestBody Map<String, Object> requestBody,
            Authentication authentication) {
        log.info("Enhancing image for user: {}", authentication.getName());
        String imageUrl = (String) requestBody.get("imageUrl");
        String providerName = (String) requestBody.get("provider");
        List<String> enhancementTypes = (List<String>) requestBody.getOrDefault("enhancementTypes", new ArrayList<>());

        if (imageUrl == null || providerName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
        }

        try {
            String enhancedUrl = imageUrl;
            for (String type : enhancementTypes) {
                enhancedUrl = aiContentService.enhanceImage(enhancedUrl, providerName, type, Map.of());
            }
            return ResponseEntity.ok(Map.of("enhancedUrl", enhancedUrl));
        } catch (Exception e) {
            log.error("Error enhancing image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to enhance image"));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<AdGenerationResponse> generateAdContent(
            @Valid @RequestBody AdGenerationRequest request,
            Authentication authentication) {
        
        log.info("Generating ad content for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        // Validate input: check if both prompt and ad links are empty
        String prompt = request.getPrompt();
        List<String> adLinks = request.getAdLinks();
        String extractedContent = request.getExtractedContent();
        
        boolean isPromptEmpty = (prompt == null || prompt.trim().isEmpty());
        boolean isAdLinksEmpty = (adLinks == null || adLinks.isEmpty() || adLinks.stream().allMatch(link -> link == null || link.trim().isEmpty()));
        boolean isExtractedContentEmpty = (extractedContent == null || extractedContent.trim().isEmpty());
        // If both prompt and ad links are empty, return error
        if (isPromptEmpty && isAdLinksEmpty && isExtractedContentEmpty) {
            log.warn("Both prompt and ad links are empty for user: {}", authentication.getName());
            AdGenerationResponse errorResponse = AdGenerationResponse.builder()
                .status("error")
                .message("Could not create ads. Please check the ad prompt / ad link and try again.")
                .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // Kiểm tra xem có phải là preview hay không
            Boolean isPreview = request.getIsPreview();
            if (isPreview == null) {
                isPreview = false; // Mặc định là save thực sự;
            }
            
            if (isPreview) {
                // Chỉ tạo preview, không lưu vào database
                log.info("Creating preview for user: {}", authentication.getName());
                
                // Tạo ad tạm thời để generate content
                Ad tempAd = new Ad();
                tempAd.setName(request.getName());
                tempAd.setAdType(adService.mapFrontendAdTypeToEnum(request.getAdType()));
                tempAd.setPrompt(request.getPrompt());
                
                // Generate AI content mà không lưu vào database
                List<AdContent> contents = adService.generatePreviewContent(tempAd,
                    request.getPrompt(),
                    null, // mediaFile
                    request.getTextProvider(),
                    request.getImageProvider(),
                    request.getNumberOfVariations(),
                    request.getLanguage(),
                    request.getAdLinks(),
                    request.getPromptStyle(),
                    request.getCustomPrompt(),
                    request.getExtractedContent(),
                    request.getMediaFileUrl(), // truyền mediaFileUrl vào
                    request.getCallToAction(), // truyền call to action vào
                    request.getWebsiteUrl(), // truyền website URL vào
                    request.getLeadFormQuestions(), // truyền lead form questions vào
                    request.getAudienceSegment() // truyền audience segment vào
                );
                // Convert to AdGenerationResponse with quality metrics
                List<AdGenerationResponse.AdVariation> variations = contents.stream()
                    .map(this::convertToAdVariation)
                    .toList();

                // Build validation report
                AdGenerationResponse.ValidationReport validationReport = buildValidationReport(contents);

                AdGenerationResponse response = AdGenerationResponse.builder()
                    .adId(null) // Không có adId vì chỉ là preview
                    .variations(variations)
                    .status("success")
                    .message("Ad preview generated successfully")
                    .validationReport(validationReport)
                    .build();
                return ResponseEntity.ok(response);
            } else {
                // Tạo quảng cáo thực sự và lưu vào database
                log.info("Creating actual ad for user: {}", authentication.getName());
                
                // Create ad and generate AI content in one step
                Map<String, Object> adResult = adService.createAdWithAIContent(request.getCampaignId(),
                    request.getAdType(),
                    request.getPrompt(),
                    request.getName(),
                    null, // mediaFile will be handled by the upload endpoint
                    userId,
                    request.getTextProvider(),
                    request.getImageProvider(),
                    request.getNumberOfVariations(),
                    request.getLanguage(),
                    request.getAdLinks(),
                    request.getPromptStyle(),
                    request.getCustomPrompt(),
                    request.getExtractedContent(),
                    request.getMediaFileUrl(), // truyền mediaFileUrl vào
                    request.getCallToAction(), // truyền call to action vào
                    request.getWebsiteUrl(), // truyền website URL vào
                    request.getLeadFormQuestions(), // truyền lead form questions vào
                    request.getAudienceSegment() // truyền audience segment vào
                );
                List<AdContent> contents = (List<AdContent>) adResult.get("contents");
                Ad ad = (Ad) adResult.get("ad");
                // Nếu có selectedVariation, mark nó là selected
                if (request.getSelectedVariation() != null) {
                    try {
                        // Tìm content được chọn và mark là selected
                        for (AdContent content : contents) {
                            content.setIsSelected(false); // Reset tất cả;
                        }
                        
                        // Mark content được chọn
                        // Logic này có thể cần điều chỉnh tuỳ theo cấu trúc selectedVariation
                        if (contents.size() > 0) {
                            contents.get(0).setIsSelected(true); // Tạm thời chọn content đầu tiên;
                        }
                        
                        // Save lại contents
                        adService.saveAdContents(contents);
                    } catch (Exception e) {
                        log.warn("Failed to mark selected variation: {}", e.getMessage());
                    }
                }
                
                // Convert to AdGenerationResponse with quality metrics
                List<AdGenerationResponse.AdVariation> variations = contents.stream()
                    .map(this::convertToAdVariation)
                    .toList();

                AdGenerationResponse.ValidationReport validationReport = buildValidationReport(contents);

                AdGenerationResponse response = AdGenerationResponse.builder()
                    .adId(ad.getId())
                    .variations(variations)
                    .status("success")
                    .message("Ad created and saved successfully")
                    .validationReport(validationReport)
                    .build();

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error generating ad content: {}", e.getMessage(), e);
            AdGenerationResponse errorResponse = AdGenerationResponse.builder()
                .status("error")
                .message("Failed to generate ad content: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Save existing ad content without regenerating
     */
    @Operation(summary = "Save existing ad", description = "Save an existing ad with updated content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping("/save-existing")
    public ResponseEntity<AdGenerationResponse> saveExistingAd(@RequestBody AdGenerationRequest request, Authentication authentication) {
        log.info("Saving existing ad content for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        try {
            // Tạo ad với nội dung đã có (không generate mới)
            Map<String, Object> adResult = adService.createAdWithExistingContent(request.getCampaignId(),
                request.getAdType(),
                request.getPrompt(),
                request.getName(),
                null, // mediaFile
                userId,
                request.getSelectedVariation(), // Sử dụng nội dung đã chọn
                request.getAdLinks(),
                request.getPromptStyle(),
                request.getCustomPrompt(),
                request.getMediaFileUrl() // truyền mediaFileUrl vào
            );

            List<AdContent> contents = (List<AdContent>) adResult.get("contents");
            Ad ad = (Ad) adResult.get("ad");
            
            // Convert to AdGenerationResponse
            List<AdGenerationResponse.AdVariation> variations = contents.stream()
                .map(content -> AdGenerationResponse.AdVariation.builder()
                    .id(content.getId())
                    .headline(content.getHeadline())
                    .description(content.getDescription())
                    .primaryText(content.getPrimaryText())
                    .callToAction(content.getCallToAction() != null ? content.getCallToAction().name() : null)
                    .imageUrl(content.getImageUrl())
                    .order(content.getPreviewOrder())
                    .build())
                .collect(java.util.stream.Collectors.toList());
            AdGenerationResponse response = AdGenerationResponse.builder()
                .adId(ad.getId())
                .variations(variations)
                .status("success")
                .message("Ad saved successfully")
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving existing ad: {}", e.getMessage(), e);
            AdGenerationResponse errorResponse = AdGenerationResponse.builder()
                .status("error")
                .message("Failed to save ad: " + e.getMessage())
                .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "Select ad content", description = "Select specific content for an ad")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Content selected successfully"),
        @ApiResponse(responseCode = "404", description = "Ad or content not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping("/{adId}/select-content")
    public ResponseEntity<Map<String, Object>> selectAdContent(
            @Parameter(description = "Ad ID") @PathVariable Long adId,
            @Parameter(description = "Content ID") @RequestParam Long contentId,
            Authentication authentication) {
        
        log.info("Selecting content {} for ad {} by user: {}", contentId, adId, authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        try {
            Ad updatedAd = adService.selectAdContent(adId, contentId, userId);
            
            Map<String, Object> response = Map.of("success", true,
                "message", "Ad content selected successfully",
                "ad", updatedAd
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error selecting ad content", e);
            Map<String, Object> errorResponse = Map.of("success", false,
                "message", "Failed to select ad content: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Get ad contents", description = "Retrieve all content variations for a specific ad")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad contents retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Ad not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{adId}/contents")
    public ResponseEntity<List<AdContent>> getAdContents(
            @Parameter(description = "Ad ID") @PathVariable Long adId,
            Authentication authentication) {
        
        log.info("Getting contents for ad {} by user: {}", adId, authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adService.getAdContents(adId, userId));
    }

    @Operation(summary = "Delete ad", description = "Delete a specific ad by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Ad not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAd(
            @Parameter(description = "Ad ID") @PathVariable Long id,
            Authentication authentication) {
        
        log.info("Deleting ad: {} for user: {}", id, authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        try {
            adService.deleteAd(id, userId);
            
            Map<String, Object> response = Map.of("success", true,
                "message", "Ad deleted successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting ad", e);
            Map<String, Object> errorResponse = Map.of("success", false,
                "message", "Failed to delete ad: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Save selected ad", description = "Save the selected ad content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Selected ad saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping("/save-selected")
    public ResponseEntity<Map<String, Object>> saveSelectedAd(@RequestBody AdContent adContent, Authentication authentication) {
        log.info("Saving selected ad content for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        try {
            // The adContent object received here will have the ID of the selected AdContent
            // and the adId of the parent Ad. We need to use these to update the Ad.
            Ad updatedAd = adService.selectAdContent(adContent.getAd().getId(), adContent.getId(), userId);

            Map<String, Object> response = Map.of("success", true,
                "message", "Ad content saved successfully",
                "ad", updatedAd
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving selected ad content", e);
            Map<String, Object> errorResponse = Map.of("success", false,
                "message", "Failed to save ad content: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "Create ad", description = "Create a new advertisement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ad created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Ad> createAd(
            @RequestBody Ad adData,
            Authentication authentication) {
        log.info("Creating new ad for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        try {
            Ad createdAd = adService.createAd(adData, userId);
            return ResponseEntity.ok(createdAd);
        } catch (Exception e) {
            log.error("Failed to create ad for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Update ad", description = "Update an existing advertisement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad updated successfully"),
        @ApiResponse(responseCode = "404", description = "Ad not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Ad> updateAd(
            @Parameter(description = "Ad ID") @PathVariable Long id,
            @RequestBody Ad updatedAd,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Ad ad = adService.updateAd(id, updatedAd, userId);
        return ResponseEntity.ok(ad);
    }
    @Operation(summary = "Extract from Meta Ad Library", description = "Extract ad data from Meta Ad Library")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data extracted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "500", description = "External service error")
    })
    @PostMapping("/extract-from-library")
    public ResponseEntity<List<Map<String, Object>>> extractFromMetaAdLibrary(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        log.info("Extracting content from Meta Ad Library for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        try {
            @SuppressWarnings("unchecked")
            List<String> adLinks = (List<String>) request.get("adLinks");
            if (adLinks == null || adLinks.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            List<Map<String, Object>> result = metaAdLibraryService.extractAdTextAndImages(adLinks);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error extracting from Meta Ad Library", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Helper method to convert AdContent to AdVariation with quality metrics
     */
    private AdGenerationResponse.AdVariation convertToAdVariation(AdContent content) {
        // Parse warnings from JSON if available
        List<String> warnings = null;
        if (content.getValidationWarnings() != null && !content.getValidationWarnings().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                warnings = objectMapper.readValue(content.getValidationWarnings(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } catch (Exception e) {
                // If JSON parsing fails, treat as comma-separated string
                warnings = java.util.Arrays.asList(content.getValidationWarnings().split(","));
            }
        }

        return AdGenerationResponse.AdVariation.builder()
                .id(content.getId())
                .headline(content.getHeadline())
                .description(content.getDescription())
                .primaryText(content.getPrimaryText())
                .callToAction(content.getCallToAction() != null ? content.getCallToAction().name() : null)
                .imageUrl(content.getImageUrl())
                .order(content.getPreviewOrder())
                .qualityScore(content.getQualityScore())
                .hasWarnings(content.getHasWarnings())
                .warnings(warnings)
                .build();
    }

    /**
     * Build validation report from list of AdContent
     */
    private AdGenerationResponse.ValidationReport buildValidationReport(List<AdContent> contents) {
        int total = contents.size();
        int passed = (int) contents.stream()
                .filter(c -> c.getHasWarnings() == null || !c.getHasWarnings())
                .count();
        int withWarnings = (int) contents.stream()
                .filter(c -> c.getHasWarnings() != null && c.getHasWarnings())
                .count();
        int failed = total - passed - withWarnings; // Should be 0 with new logic

        double avgScore = contents.stream()
                .filter(c -> c.getQualityScore() != null)
                .mapToInt(AdContent::getQualityScore)
                .average()
                .orElse(0.0);

        return new AdGenerationResponse.ValidationReport(total, passed, failed, withWarnings, avgScore);
    }
}
