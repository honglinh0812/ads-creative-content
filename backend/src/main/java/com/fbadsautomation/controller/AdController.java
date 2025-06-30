package com.fbadsautomation.controller;

import com.fbadsautomation.dto.AdCreateRequest;
import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdResponse;
import com.fbadsautomation.service.AdService;
import com.fbadsautomation.service.AIContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdController {

    private final AdService adService;
    private final AIContentService aiContentService;

    @GetMapping
    public ResponseEntity<List<AdResponse>> getAllAds(Authentication authentication) {
        log.info("Getting all ads for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());

        try {
            List<AdResponse> ads = adService.getAllAdResponsesByUser(userId);
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
            @RequestBody AdGenerationRequest request,
            Authentication authentication) {
        
        log.info("Generating ad content for user: {}", authentication.getName());
        Long userId = Long.parseLong(authentication.getName());
        
        try {
            log.info("AdController: Text provider: {}, Image provider: {}", request.getTextProvider(), request.getImageProvider());
            // Tạo ad với AI content generation
            Map<String, Object> result = adService.createAdWithAIContent(
                request.getCampaignId(),
                request.getAdType(),
                request.getPrompt(),
                request.getName(),
                null, // mediaFile sẽ được xử lý riêng qua upload endpoint
                userId,
                request.getTextProvider(),
                request.getImageProvider(),
                request.getNumberOfVariations(),
                request.getLanguage()
            );
            
            Ad ad = (Ad) result.get("ad");
            List<AdContent> contents = (List<AdContent>) result.get("contents");
            
            // Chuyển đổi sang AdGenerationResponse
            List<AdGenerationResponse.AdVariation> variations = contents.stream()
                .map(content -> AdGenerationResponse.AdVariation.builder()
                    .id(content.getId())
                    .headline(content.getHeadline())
                    .description(content.getDescription())
                    .primaryText(content.getPrimaryText())
                    .callToAction(content.getCallToAction())
                    .imageUrl(content.getImageUrl())
                    .order(content.getPreviewOrder())
                    .build())
                .toList();
            
            AdGenerationResponse response = AdGenerationResponse.builder()
                .adId(ad.getId())
                .variations(variations)
                .status("success")
                .message("Ad variations generated successfully")
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating ad content", e);
            AdGenerationResponse errorResponse = AdGenerationResponse.builder()
                .status("error")
                .message("Failed to generate ad content: " + e.getMessage())
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
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Ad content selected successfully",
                "ad", updatedAd
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error selecting ad content", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
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
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Ad deleted successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting ad", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "Failed to delete ad: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

