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

import java.util.List;
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
    private final PersonaService personaService;

    @Async("aiProcessingExecutor")
    public CompletableFuture<Void> generateContentAsync(
            String jobId,
            Long userId,
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
            List<String> trendingKeywords) {

        try {
            errorHandlingService.validateJobExecution(jobId, "content-generation");
            asyncJobService.startJob(jobId, "Starting AI content generation");

            asyncJobService.updateJobProgress(jobId, 10, "Preparing content generation");

            // Create temporary Ad object for generation (not saved to DB)
            Ad tempAd = new Ad();
            tempAd.setAdType(convertContentTypeToAdType(contentType));
            tempAd.setPrompt(prompt);

            // Phase 1: Fetch user-selected persona if provided
            com.fbadsautomation.model.Persona userSelectedPersona = null;
            if (personaId != null && userId != null) {
                try {
                    com.fbadsautomation.model.User user = userRepository.findById(userId)
                        .orElse(null);
                    if (user != null) {
                        userSelectedPersona = personaService.findByIdAndUser(personaId, user)
                            .orElse(null);
                        if (userSelectedPersona != null) {
                            log.info("[Async] Using user-selected persona: {} (ID: {})",
                                userSelectedPersona.getName(), personaId);
                        } else {
                            log.warn("[Async] Persona ID {} not found for user {}, will use auto-selection",
                                personaId, userId);
                        }
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
                trendingKeywords     // Phase 2: Trending keywords
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

            asyncJobService.completeJob(jobId, validatedContents);
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