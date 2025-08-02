package com.fbadsautomation.integration.ai;

import com.fbadsautomation.ai.AIProvider; // Import the main interface
import com.fbadsautomation.ai.GeminiProvider;
import com.fbadsautomation.ai.HuggingFaceProvider;
import com.fbadsautomation.ai.OpenAIProvider;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.AdContent; // Import AdContent
import com.fbadsautomation.model.AdType; // Import AdType
import com.fbadsautomation.service.AIContentValidationService;
import com.fbadsautomation.service.AIProviderService;
import com.fbadsautomation.service.MetaAdLibraryService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service

/**
 * Service for generating ad content using AI based on ad type
 */
public class AIContentServiceImpl {

    private final AIProviderService aiProviderService;
    private final MetaAdLibraryService metaAdLibraryService;
    private final AIContentValidationService validationService;

    @Autowired
    public AIContentServiceImpl(AIProviderService aiProviderService,
                               MetaAdLibraryService metaAdLibraryService,
                               AIContentValidationService validationService) {
        this.aiProviderService = aiProviderService;
        this.metaAdLibraryService = metaAdLibraryService;
        this.validationService = validationService;
    }

    public List<AdContent> generateContent(String prompt,
                                           AdContent.ContentType contentType,
                                           String textProvider,
                                           String imageProvider,
                                           int numberOfVariations,
                                           String language,
                                           List<String> adLinks,
                                           String promptStyle,
                                           String customPrompt,
                                           String extractedContent,
                                           com.fbadsautomation.model.FacebookCTA callToAction) {
        
        String providerId = (textProvider == null || textProvider.isBlank())
                ? "openai" : textProvider;
        log.info("🔍 Requested text provider: '{}', normalized to: '{}'", textProvider, providerId);
        AIProvider textAI = aiProviderService.getProvider(providerId);
        if (textAI == null) {
            log.error("Unsupported text AI provider: {}", providerId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported AI provider: " + providerId);
        }
        log.info("✅ Found text AI provider: {} ({})", textAI.getProviderName(), providerId);

        AIProvider imageAI = null;
        if (imageProvider != null && !imageProvider.isBlank()) {
            imageAI = aiProviderService.getProvider(imageProvider);
            if (imageAI == null) {
                log.error("Unsupported image AI provider: {}", imageProvider);
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported image AI provider: " + imageProvider);
            }
        }

        // Build final prompt based on available inputs
        String finalPrompt = buildFinalPrompt(prompt, adLinks, promptStyle, customPrompt, extractedContent);
        try {
            AdType adType = convertContentTypeToAdType(contentType);
            String enhancedPrompt = enhancePromptForAdType(finalPrompt, adType);
            log.info("Generating {} text variations using enhanced reliability features", numberOfVariations);

            // Use enhanced AI provider service with caching, circuit breaker, and fallback
            // Sử dụng CTA được truyền hoặc default nếu null
            com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
            List<AdContent> contents = aiProviderService.generateContentWithReliability(
                enhancedPrompt, textProvider, numberOfVariations, language, adLinks, cta); // Generate images with reliability features if image provider is specified
            if (imageProvider != null && !imageProvider.isBlank()) {
                for (AdContent content : contents) {
                    String imagePrompt = content.getPrimaryText();
                    String imageUrl = aiProviderService.generateImageWithReliability(imagePrompt, imageProvider);
                    content.setImageUrl(imageUrl);
                }
                log.info("Images added using enhanced reliability features");
            }

            // Validate and filter generated content
            List<AdContent> validatedContents = validationService.validateAndFilterContent(contents);
            if (validatedContents.isEmpty()) {
                log.warn("All generated content was filtered out due to validation failures");
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Generated content did not meet quality standards. Please try with a different prompt.");
            }

            log.info("Successfully generated and validated {} content variations", validatedContents.size());
            return validatedContents;

        } catch (Exception e) {
            log.error("Failed to generate content with {}: {}", textAI.getProviderName(), e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate content: " + e.getMessage(), e);
        }
    }

    /**
     * Build final prompt by combining original prompt and ad link content
     */
    private String buildFinalPrompt(String originalPrompt, List<String> adLinks, String promptStyle, String customPrompt, String extractedContent) {
        StringBuilder finalPrompt = new StringBuilder(); // Check if we have extracted content from Meta Ad Library
        boolean hasExtractedContent = (extractedContent != null && !extractedContent.trim().isEmpty());
        boolean hasAdLinks = (adLinks != null && !adLinks.isEmpty());
        boolean hasOriginalPrompt = (originalPrompt != null && !originalPrompt.trim().isEmpty());
        
        if (hasExtractedContent) {
            // Use extracted content from Meta Ad Library
            log.info("Using extracted content from Meta Ad Library");
            if (hasOriginalPrompt) {
                // Both prompt and extracted content available
                log.info("Combining original prompt with extracted content");
                finalPrompt.append("Yêu cầu từ người dùng: ").append(originalPrompt).append("\n\n");
                finalPrompt.append(extractedContent);
            } else {
                // Only extracted content available
                log.info("Using only extracted content as prompt is empty");
                finalPrompt.append(extractedContent);
            }
        } else if (hasAdLinks) {
            // Extract content from ad links
            List<Map<String, Object>> adContents = metaAdLibraryService.extractAdsByAdIds(adLinks); // Lấy nội dung body của từng ad (nếu có)
            StringBuilder adLinkContentBuilder = new StringBuilder();
            for (int i = 0; i < adContents.size(); i++) {
                Map<String, Object> ad = adContents.get(i);
                String body = null;
                if (ad.get("body") != null) {
                    body = String.valueOf(ad.get("body")); } else if (ad.get("snapshot") instanceof Map) {
                    Object snapshotBody = ((Map<?, ?>) ad.get("snapshot")).get("body");
                    if (snapshotBody != null) body = String.valueOf(snapshotBody); }
                if (body != null && !body.trim().isEmpty()) {
                    adLinkContentBuilder.append("Quảng cáo ").append(i + 1).append(": ").append(body).append("\n");
                }
            }
            String adLinkContent = adLinkContentBuilder.toString().trim();
            boolean hasAdLinkContent = (adLinkContent != null && !adLinkContent.isEmpty());
            if (hasOriginalPrompt) {
                if (hasAdLinkContent) {
                    // Both prompt and ad links are available - combine them
                    log.info("Combining original prompt with ad link content");
                    finalPrompt.append("Yêu cầu từ người dùng: ").append(originalPrompt).append("\n\n");
                    finalPrompt.append("Nội dung tham khảo từ quảng cáo:\n").append(adLinkContent);
                } else {
                    // Ad links provided but no content extracted - use original prompt only
                    log.info("Ad links provided but no content extracted, using original prompt only");
                    finalPrompt.append(originalPrompt);
                }
            } else {
                if (hasAdLinkContent) {
                    // Only ad links available - use extracted content
                    log.info("Using only ad link content as prompt is empty");
                    finalPrompt.append(adLinkContent);
                } else {
                    // Neither prompt nor ad link content available - throw error
                    log.error("No prompt and no content extracted from ad links");
                    throw new ApiException(HttpStatus.BAD_REQUEST, 
                        "Could not generate content: Please provide a valid prompt or ad link");
                }
            }
        } else {
            // Only original prompt available
            log.info("Using only original prompt as no ad links or extracted content provided");
            finalPrompt.append(originalPrompt);
        }
        
        log.info("Final prompt built: {}", finalPrompt.toString());
        return finalPrompt.toString();
    }

    /**
     * Convert ContentType to AdType for backward compatibility or prompt enhancement.
     * Corrected to use actual values from AdContent.ContentType enum.
     */
    private AdType convertContentTypeToAdType(AdContent.ContentType contentType) {
        if (contentType == null) {
            return AdType.PAGE_POST_AD;
        // Default if null;
        }
        // Corrected switch cases to use actual enum constants from AdContent.ContentType
        switch (contentType) {
            case TEXT:
            case IMAGE:
            case COMBINED:
            case PAGE_POST:
            default:
                return AdType.PAGE_POST_AD;
        }
    }

    /**
     * Enhance user prompt based on ad type to potentially get better AI-generated content.
     */
    private String enhancePromptForAdType(String userPrompt, AdType adType) {
        // Bổ sung hướng dẫn kiểm duyệt để AI sinh nội dung đạt chuẩn
        StringBuilder enhancedPrompt = new StringBuilder(userPrompt);
        enhancedPrompt.append("\n\nLưu ý: Nội dung quảng cáo phải tuân thủ các tiêu chí sau:\n");
        enhancedPrompt.append("- Tiêu đề (headline) tối đa 40 ký tự, mô tả (description) tối đa 125 ký tự, nội dung chính (primaryText) tối đa 10.000 ký tự.\n");
        enhancedPrompt.append("- Mỗi trường phải có ít nhất 3 từ.\n");
        enhancedPrompt.append("- Không chứa từ ngữ vi phạm chính sách, spam, hoặc nội dung không phù hợp (ví dụ: 'hate', 'violence', 'drugs', 'miracle', 'guaranteed results', 'FREE!!!', 'ACT NOW!!!', ...).\n");
        enhancedPrompt.append("- Nội dung phải rõ ràng, dễ đọc, không viết hoa toàn bộ, không lặp lại từ ngữ vô nghĩa.\n");
        enhancedPrompt.append("- Nếu không chắc chắn, hãy ưu tiên an toàn và tuân thủ chính sách quảng cáo Facebook.\n");
        return enhancedPrompt.toString();
    }
}
