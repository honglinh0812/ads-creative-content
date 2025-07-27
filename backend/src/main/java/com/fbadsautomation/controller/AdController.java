package com.fbadsautomation.controller;

import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdResponse;
import com.fbadsautomation.service.AIContentService;
import com.fbadsautomation.service.AdService;
import com.fbadsautomation.service.MetaAdLibraryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

public class AdController {

    private final AdService adService;
    private final AIContentService aiContentService;
    private final MetaAdLibraryService metaAdLibraryService;

    @GetMapping
    public ResponseEntity<Page<AdResponse>> getAllAds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
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

    @GetMapping("/{id}")
    public ResponseEntity<Ad> getAd(@PathVariable Long id, Authentication authentication) {
        log.info("Getting ad: {} for user: {}", id, authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adService.getAdByIdAndUser(id, userId));
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
                tempAd.setAdType(com.fbadsautomation.model.AdType.valueOf(request.getAdType()));
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
                    request.getCallToAction() // truyền call to action vào
                );
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
                    .toList();
                
                AdGenerationResponse response = AdGenerationResponse.builder()
                    .adId(null) // Không có adId vì chỉ là preview
                    .variations(variations)
                    .status("success")
                    .message("Ad preview generated successfully")
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
                    request.getCallToAction() // truyền call to action vào
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
                    .toList();
                AdGenerationResponse response = AdGenerationResponse.builder()
                    .adId(ad.getId())
                    .variations(variations)
                    .status("success")
                    .message("Ad created and saved successfully")
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

    @PostMapping("/{adId}/select-content")
    public ResponseEntity<Map<String, Object>> selectAdContent(
            @PathVariable Long adId,
            @RequestParam Long contentId,
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

    @GetMapping("/{adId}/contents")
    public ResponseEntity<List<AdContent>> getAdContents(
            @PathVariable Long adId,
            Authentication authentication) {
        
        log.info("Getting contents for ad {} by user: {}", adId, authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adService.getAdContents(adId, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAd(
            @PathVariable Long id,
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

    @PutMapping("/{id}")
    public ResponseEntity<Ad> updateAd(
            @PathVariable Long id,
            @RequestBody Ad updatedAd,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Ad ad = adService.updateAd(id, updatedAd, userId);
        return ResponseEntity.ok(ad);
    }
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
}
