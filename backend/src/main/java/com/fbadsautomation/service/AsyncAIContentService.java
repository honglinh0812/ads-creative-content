package com.fbadsautomation.service;

import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AudienceSegmentRequest;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAIContentService {

    private final AsyncJobService asyncJobService;
    private final AIContentValidationService validationService;
    private final AsyncErrorHandlingService errorHandlingService;
    private final AIContentService aiContentService;
    private final com.fbadsautomation.repository.UserRepository userRepository;
    private final com.fbadsautomation.repository.CampaignRepository campaignRepository;
    private final PersonaService personaService;

    @Async("aiProcessingExecutor")
    public CompletableFuture<Void> generateContentAsync(
            String jobId,
            Long userId,
            Long campaignId,               // Issue #6: Campaign ID
            String prompt,
            AdContent.ContentType contentType,
            String textProvider,
            String imageProvider,
            int numberOfVariations,
            String language,
            List<String> adLinks,
            String extractedContent,
            FacebookCTA callToAction,
            String mediaFileUrl,
            String websiteUrl,
            List<AdGenerationRequest.LeadFormQuestion> leadFormQuestions,
            AudienceSegmentRequest audienceSegment,
            Long personaId,
            List<String> trendingKeywords,
            String adStyle,
            List<AdGenerationRequest.VariationProviderConfig> variationConfigs) {              // Issue #6: Ad style

        try {
            errorHandlingService.validateJobExecution(jobId, "content-generation");
            asyncJobService.startJob(jobId, "Starting AI content generation");

            asyncJobService.updateJobProgress(jobId, 10, "Preparing content generation");

            // Create temporary Ad object for generation (not saved to DB)
            Ad tempAd = new Ad();
            tempAd.setAdType(convertContentTypeToAdType(contentType));
            tempAd.setPrompt(prompt);
            // Issue #6: Set adStyle on tempAd so it can be used in prompt generation
            if (adStyle != null && !adStyle.isEmpty()) {
                try {
                    com.fbadsautomation.model.AdStyle styleEnum =
                        com.fbadsautomation.model.AdStyle.valueOf(adStyle.toUpperCase());
                    tempAd.setAdStyle(styleEnum);
                    log.info("[Async Issue #6] Set ad style: {}", styleEnum);
                } catch (IllegalArgumentException e) {
                    log.warn("[Async Issue #6] Invalid ad style '{}', ignoring", adStyle);
                }
            }

            // Fetch user first
            com.fbadsautomation.model.User user = userRepository.findById(userId)
                .orElse(null);

            // Issue #6: Fetch campaign for context
            com.fbadsautomation.model.Campaign campaign = null;
            if (campaignId != null && user != null) {
                try {
                    campaign = campaignRepository.findByIdAndUser(campaignId, user)
                        .orElse(null);
                    if (campaign != null) {
                        log.info("[Async Issue #6] Using campaign: {} with target audience: {}",
                            campaign.getName(),
                            campaign.getTargetAudience() != null ? campaign.getTargetAudience() : "none");
                        // Set campaign on tempAd so AIContentService can use it
                        tempAd.setCampaign(campaign);
                    } else {
                        log.warn("[Async Issue #6] Campaign ID {} not found for user {}", campaignId, userId);
                    }
                } catch (Exception e) {
                    log.warn("[Async Issue #6] Failed to fetch campaign {}: {}", campaignId, e.getMessage());
                }
            }

            // Phase 1: Fetch user-selected persona if provided
            com.fbadsautomation.model.Persona userSelectedPersona = null;
            if (personaId != null && user != null) {
                try {
                    userSelectedPersona = personaService.findByIdAndUser(personaId, user)
                        .orElse(null);
                    if (userSelectedPersona != null) {
                        log.info("[Async] Using user-selected persona: {} (ID: {})",
                            userSelectedPersona.getName(), personaId);
                    } else {
                        log.warn("[Async] Persona ID {} not found for user {}, will use auto-selection",
                            personaId, userId);
                    }
                } catch (Exception e) {
                    log.warn("[Async] Failed to fetch persona {}, falling back to auto-selection: {}",
                        personaId, e.getMessage());
                }
            }

            asyncJobService.updateJobProgress(jobId, 20, "Generating content with AI");

            // Use the same sync service logic to ensure consistency
            // Phase 1&2: Pass persona and trending keywords
            List<AdContent> contents = aiContentService.generateAdContent(
                tempAd,
                prompt,
                null, // mediaFile - not used in preview
                textProvider,
                imageProvider,
                numberOfVariations,
                language,
                adLinks,
                extractedContent,
                mediaFileUrl,
                callToAction,
                audienceSegment,
                userSelectedPersona, // Phase 1: User-selected persona
                trendingKeywords,     // Phase 2: Trending keywords
                variationConfigs
            );

            asyncJobService.updateJobProgress(jobId, 90, "Processing generated content");

            // Set temporary IDs for preview (same as sync mode)
            for (int i = 0; i < contents.size(); i++) {
                AdContent content = contents.get(i);
                content.setId((long) (i + 1));
                content.setPreviewOrder(i + 1);
                content.setIsSelected(false);
            }

            // Ensure imageUrl is set from mediaFileUrl if needed
            if (mediaFileUrl != null && !mediaFileUrl.isEmpty()) {
                for (AdContent content : contents) {
                    if (content.getImageUrl() == null || content.getImageUrl().isEmpty()) {
                        content.setImageUrl(mediaFileUrl);
                    }
                }
            }

            asyncJobService.updateJobProgress(jobId, 95, "Validating content");

            List<AdContent> validatedContents = validationService.validateAndFilterContent(contents);
            if (validatedContents.isEmpty()) {
                throw new RuntimeException("All generated content was filtered out due to validation failures");
            }

            // Convert to DTOs to avoid Hibernate lazy loading serialization issues
            List<Map<String, Object>> contentDTOs = validatedContents.stream()
                .map(content -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", content.getId());
                    dto.put("headline", content.getHeadline());
                    dto.put("description", content.getDescription());
                    dto.put("primaryText", content.getPrimaryText());
                    dto.put("callToAction", content.getCallToAction() != null ? content.getCallToAction().name() : null);
                    dto.put("imageUrl", content.getImageUrl());
                    dto.put("contentType", content.getContentType() != null ? content.getContentType().name() : null);
                    dto.put("previewOrder", content.getPreviewOrder());
                    dto.put("isSelected", content.getIsSelected());
                    dto.put("needsReview", content.getNeedsReview());
                    dto.put("qualityScore", content.getQualityScore());
                    dto.put("hasWarnings", content.getHasWarnings());
                    dto.put("validationWarnings", content.getValidationWarnings());
                    dto.put("aiProvider", content.getAiProvider());
                    dto.put("createdDate", content.getCreatedDate());
                    // Don't include ad/user relationships to avoid lazy loading
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

            asyncJobService.completeJob(jobId, contentDTOs);
            log.info("Successfully completed async content generation for job: {}", jobId);

        } catch (Exception e) {
            log.error("Error in async content generation for job: {}", jobId, e);
            errorHandlingService.handleAsyncException(jobId, "content-generation", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private com.fbadsautomation.model.AdType convertContentTypeToAdType(AdContent.ContentType contentType) {
        return switch (contentType) {
            case PAGE_POST -> com.fbadsautomation.model.AdType.PAGE_POST_AD;
            case COMBINED -> com.fbadsautomation.model.AdType.WEBSITE_CONVERSION_AD;
            default -> com.fbadsautomation.model.AdType.WEBSITE_CONVERSION_AD;
        };
    }
}
