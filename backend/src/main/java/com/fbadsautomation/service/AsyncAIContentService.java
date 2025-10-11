package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AsyncJobStatus;
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
    private final AIProviderService aiProviderService;
    private final AIContentValidationService validationService;
    private final AsyncErrorHandlingService errorHandlingService;

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
            String promptStyle,
            String customPrompt,
            String extractedContent,
            FacebookCTA callToAction) {

        try {
            errorHandlingService.validateJobExecution(jobId, "content-generation");
            asyncJobService.startJob(jobId, "Starting AI content generation");

            String providerId = (textProvider == null || textProvider.isBlank()) ? "openai" : textProvider;
            AIProvider textAI = aiProviderService.getProvider(providerId);
            if (textAI == null) {
                throw new RuntimeException("Unsupported AI provider: " + providerId);
            }

            asyncJobService.updateJobProgress(jobId, 10, "Preparing content generation");

            String finalPrompt = buildFinalPrompt(prompt, adLinks, promptStyle, customPrompt, extractedContent);
            String enhancedPrompt = enhancePromptForAdType(finalPrompt, convertContentTypeToAdType(contentType));

            asyncJobService.updateJobProgress(jobId, 30, "Generating text content");

            FacebookCTA cta = callToAction != null ? callToAction : FacebookCTA.LEARN_MORE;
            CompletableFuture<List<AdContent>> textGeneration = textAI.generateAdContentAsync(
                    enhancedPrompt, numberOfVariations, language, cta);

            CompletableFuture<List<AdContent>> resilientTextGeneration =
                errorHandlingService.generateContentWithResilience(
                    jobId, textGeneration, enhancedPrompt, numberOfVariations, cta);

            List<AdContent> contents = resilientTextGeneration.get();

            asyncJobService.updateJobProgress(jobId, 70, "Processing generated content");

            if (imageProvider != null && !imageProvider.isBlank()) {
                AIProvider imageAI = aiProviderService.getProvider(imageProvider);
                if (imageAI != null) {
                    asyncJobService.updateJobProgress(jobId, 80, "Generating images");

                    for (int i = 0; i < contents.size(); i++) {
                        errorHandlingService.validateJobExecution(jobId, "image-generation");
                        AdContent content = contents.get(i);
                        String imagePrompt = content.getPrimaryText();
                        CompletableFuture<String> imageGeneration = imageAI.generateImageAsync(imagePrompt);

                        CompletableFuture<String> resilientImageGeneration =
                            errorHandlingService.generateImageWithResilience(
                                jobId, imageGeneration, imagePrompt);

                        String imageUrl = resilientImageGeneration.get();
                        content.setImageUrl(imageUrl);

                        int progress = 80 + (i + 1) * 10 / contents.size();
                        asyncJobService.updateJobProgress(jobId, progress, "Generated image " + (i + 1) + "/" + contents.size());
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

    private String buildFinalPrompt(String prompt, List<String> adLinks, String promptStyle, String customPrompt, String extractedContent) {
        StringBuilder finalPrompt = new StringBuilder();

        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            finalPrompt.append(customPrompt).append("\n\n");
        }

        if (extractedContent != null && !extractedContent.trim().isEmpty()) {
            finalPrompt.append("Reference content: ").append(extractedContent).append("\n\n");
        }

        finalPrompt.append(prompt);

        if (adLinks != null && !adLinks.isEmpty()) {
            finalPrompt.append("\n\nReference ads: ");
            for (String link : adLinks) {
                finalPrompt.append(link).append(" ");
            }
        }

        if (promptStyle != null && !promptStyle.trim().isEmpty()) {
            finalPrompt.append("\n\nStyle: ").append(promptStyle);
        }

        return finalPrompt.toString();
    }

    private String enhancePromptForAdType(String prompt, com.fbadsautomation.model.AdType adType) {
        String enhancement = switch (adType) {
            case PAGE_POST_AD -> "Create engaging social media content that encourages likes, comments, and shares. ";
            case WEBSITE_CONVERSION_AD -> "Create compelling content that drives visitors to take action on a website. ";
            case LEAD_FORM_AD -> "Create persuasive content that encourages users to fill out a form and provide their contact information. ";
        };
        return enhancement + prompt;
    }

    private com.fbadsautomation.model.AdType convertContentTypeToAdType(AdContent.ContentType contentType) {
        return switch (contentType) {
            case PAGE_POST -> com.fbadsautomation.model.AdType.PAGE_POST_AD;
            case COMBINED -> com.fbadsautomation.model.AdType.WEBSITE_CONVERSION_AD;
            default -> com.fbadsautomation.model.AdType.WEBSITE_CONVERSION_AD;
        };
    }
}