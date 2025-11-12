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
import com.fbadsautomation.service.MinIOStorageService;
import com.fbadsautomation.util.ByteArrayMultipartFile;
import com.fbadsautomation.util.ValidationMessages;
import com.fbadsautomation.util.ValidationMessages.Language;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
/**
 * Service for generating ad content using AI based on ad type
 */
public class AIContentServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(AIContentServiceImpl.class);

    private final AIProviderService aiProviderService;
    private final MetaAdLibraryService metaAdLibraryService;
    private final AIContentValidationService validationService;
    private final MinIOStorageService minIOStorageService;

    @Value("${ai.default.image-provider:gemini}")
    private String defaultImageProvider;

    @Autowired(required = false)
    private com.fbadsautomation.service.PersonaSelectorService personaSelectorService;

    @Autowired(required = false)
    private com.fbadsautomation.service.MultiStagePromptBuilder multiStagePromptBuilder;

    @Autowired(required = false)
    private com.fbadsautomation.service.ChainOfThoughtPromptBuilder chainOfThoughtPromptBuilder;

    @Autowired
    public AIContentServiceImpl(AIProviderService aiProviderService,
                               MetaAdLibraryService metaAdLibraryService,
                               AIContentValidationService validationService,
                               MinIOStorageService minIOStorageService) {
        this.aiProviderService = aiProviderService;
        this.metaAdLibraryService = metaAdLibraryService;
        this.validationService = validationService;
        this.minIOStorageService = minIOStorageService;
    }

    /**
     * Determines if a URL is external (needs downloading) or already local
     *
     * @param url The URL to check
     * @return true if URL is external and needs downloading, false if local or invalid
     */
    private boolean isExternalUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Local paths don't need downloading
        if (url.startsWith("/api/images/") || url.startsWith("/img/")) {
            return false;
        }

        // Check if it's a valid external HTTP/HTTPS URL
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String protocol = urlObj.getProtocol();
            return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
        } catch (java.net.MalformedURLException e) {
            // Invalid URL format, treat as local path
            return false;
        }
    }

    /**
     * Issue #9: Generate content with Campaign-level audience
     * Phase 1 & 2: Accept persona and trending keywords
     */
    public List<AdContent> generateContentWithCampaign(String prompt,
                                                        AdContent.ContentType contentType,
                                                        String textProvider,
                                                        String imageProvider,
                                                        int numberOfVariations,
                                                        String language,
                                                        List<String> adLinks,
                                                        String extractedContent,
                                                        String mediaFileUrl,
                                                        com.fbadsautomation.model.FacebookCTA callToAction,
                                                        com.fbadsautomation.model.Campaign campaign,
                                                        com.fbadsautomation.model.AdStyle adStyle,
                                                        com.fbadsautomation.model.Persona userSelectedPersona,
                                                        List<String> trendingKeywords) {

        String providerId = (textProvider == null || textProvider.isBlank())
                ? "openai" : textProvider;
        log.info("🔍 [Phase 1&2] Provider: '{}', campaign: {}, style: {}, persona: {}, keywords: {}",
                 providerId,
                 campaign != null ? campaign.getId() : "none",
                 adStyle != null ? adStyle.name() : "none",
                 userSelectedPersona != null ? userSelectedPersona.getName() : "auto-select",
                 trendingKeywords != null ? trendingKeywords.size() : 0);

        AIProvider textAI = aiProviderService.getProvider(providerId);
        if (textAI == null) {
            log.error("Unsupported text AI provider: {}", providerId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported AI provider: " + providerId);
        }

        // Build final prompt
        String finalPrompt = buildFinalPrompt(prompt, adLinks, extractedContent);

        try {
            AdType adType = convertContentTypeToAdType(contentType);
            // Issue #9: Use new enhancePromptWithCampaign method
            // Phase 1 & 2: Pass persona and trending keywords
            String enhancedPrompt = enhancePromptWithCampaign(finalPrompt, adType, campaign, adStyle, userSelectedPersona, trendingKeywords);
            log.info("[Phase 1&2] Generating {} variations with campaign audience, persona, and trending keywords", numberOfVariations);

            // Generate content
            com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
            List<AdContent> contents = aiProviderService.generateContentWithReliability(
                enhancedPrompt, textProvider, numberOfVariations, language, adLinks, cta);

            // Handle images
            if (mediaFileUrl != null && !mediaFileUrl.isBlank()) {
                // Use provided media file URL
                for (AdContent content : contents) {
                    content.setImageUrl(mediaFileUrl);
                }
            } else {
                // Use specified provider or default to Gemini
                String effectiveImageProvider = (imageProvider != null && !imageProvider.isBlank())
                    ? imageProvider
                    : defaultImageProvider;

                log.info("🎨 Generating images for {} variations using provider: {}", contents.size(), effectiveImageProvider);

                // Smart fallback: Track provider state across variations to reduce API calls
                String workingProvider = effectiveImageProvider;
                boolean primaryProviderFailed = false;
                int totalApiCalls = 0;
                int successfulGenerations = 0;

                for (int i = 0; i < contents.size(); i++) {
                    AdContent content = contents.get(i);
                    boolean retryWithFallback = false;

                    try {
                        totalApiCalls++;
                        log.debug("[VARIATION {}/{}] Generating image with provider: {}", i + 1, contents.size(), workingProvider);

                        // Issue #9: Extract concise image prompt instead of using full primaryText
                        String imagePrompt = extractImagePrompt(prompt, content);
                        String imageUrl = aiProviderService.generateImageWithReliability(
                            imagePrompt, workingProvider);

                        // Only download if it's truly an external URL
                        if (isExternalUrl(imageUrl)) {
                            log.debug("External image URL detected, downloading: {}", imageUrl);
                            String storedImageUrl = downloadAndStoreImage(imageUrl);
                            content.setImageUrl(storedImageUrl);
                        } else {
                            // Already local, use as-is
                            log.debug("Local image path detected, using directly: {}", imageUrl);
                            content.setImageUrl(imageUrl);
                        }

                        successfulGenerations++;
                        log.debug("✅ [VARIATION {}/{}] Image generated successfully with {}", i + 1, contents.size(), workingProvider);

                    } catch (Exception e) {
                        // Smart fallback: If primary provider fails on first variation, switch for all remaining
                        if (!primaryProviderFailed && workingProvider.equals(effectiveImageProvider) && i == 0) {
                            primaryProviderFailed = true;
                            String fallbackProvider = getFallbackProvider(effectiveImageProvider);
                            log.warn("⚠️ [SMART FALLBACK] Primary provider '{}' failed on first variation. " +
                                "Switching to '{}' for all remaining {} variations to reduce API calls.",
                                effectiveImageProvider, fallbackProvider, contents.size() - 1);
                            workingProvider = fallbackProvider;
                            retryWithFallback = true;
                        }

                        // Retry current variation with fallback provider if it's the first failure
                        if (retryWithFallback) {
                            try {
                                totalApiCalls++;
                                log.debug("[VARIATION {}/{}] Retrying with fallback provider: {}", i + 1, contents.size(), workingProvider);

                                // Issue #9: Extract concise image prompt instead of using full primaryText
                                String imagePrompt = extractImagePrompt(prompt, content);
                                String imageUrl = aiProviderService.generateImageWithReliability(
                                    imagePrompt, workingProvider);

                                if (isExternalUrl(imageUrl)) {
                                    String storedImageUrl = downloadAndStoreImage(imageUrl);
                                    content.setImageUrl(storedImageUrl);
                                } else {
                                    content.setImageUrl(imageUrl);
                                }

                                successfulGenerations++;
                                log.info("✅ [VARIATION {}/{}] Image generated successfully after fallback to {}",
                                    i + 1, contents.size(), workingProvider);

                            } catch (Exception retryException) {
                                log.error("❌ [VARIATION {}/{}] Failed to generate/store image even with fallback provider {}: {}",
                                    i + 1, contents.size(), workingProvider, retryException.getMessage());
                                content.setImageUrl("/img/placeholder.png");
                            }
                        } else {
                            log.error("❌ [VARIATION {}/{}] Failed to generate/store image with provider {}: {}",
                                i + 1, contents.size(), workingProvider, e.getMessage());
                            content.setImageUrl("/img/placeholder.png");
                        }
                    }
                }

                // Summary logging
                log.info("📊 [IMAGE GENERATION SUMMARY] Provider: {} | Variations: {} | Successful: {} | Total API calls: {} | Avg calls/variation: {.2f}",
                    effectiveImageProvider, contents.size(), successfulGenerations, totalApiCalls,
                    (double) totalApiCalls / contents.size());

                if (totalApiCalls > contents.size() * 2) {
                    log.warn("⚠️ [HIGH API USAGE] Total API calls ({}) exceeds 2x variations ({}). Check provider reliability.",
                        totalApiCalls, contents.size());
                }
            }

            // Validate
            List<AdContent> validatedContents = validationService.validateAndFilterContent(contents);
            return validatedContents.isEmpty() ? contents : validatedContents;

        } catch (Exception e) {
            log.error("[Issue #9] Failed to generate content: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate ad content: " + e.getMessage());
        }
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
        return generateContent(prompt, contentType, textProvider, imageProvider, numberOfVariations,
                               language, adLinks, promptStyle, customPrompt, extractedContent, null, callToAction, null);
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
                                           String mediaFileUrl,
                                           com.fbadsautomation.model.FacebookCTA callToAction,
                                           com.fbadsautomation.dto.AudienceSegmentRequest audienceSegment) {
        
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
        String finalPrompt = buildFinalPrompt(prompt, adLinks, extractedContent);

        try {
            AdType adType = convertContentTypeToAdType(contentType);
            // Pass audienceSegment to enhancePromptForAdType for multi-stage prompting
            String enhancedPrompt = enhancePromptForAdType(finalPrompt, adType, audienceSegment);
            log.info("Generating {} text variations using enhanced reliability features", numberOfVariations);

            // Use enhanced AI provider service with caching, circuit breaker, and fallback
            // Sử dụng CTA được truyền hoặc default nếu null
            com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
            List<AdContent> contents = aiProviderService.generateContentWithReliability(
                enhancedPrompt, textProvider, numberOfVariations, language, adLinks, cta);

            // Handle image URL assignment
            // Priority 1: Use uploaded image from frontend (mediaFileUrl)
            // Priority 2: Generate image with AI provider (specified or default to Gemini)
            if (mediaFileUrl != null && !mediaFileUrl.isBlank()) {
                // User uploaded an image - use it for all variations
                log.info("Using uploaded image for all {} variations: {}", contents.size(), mediaFileUrl);
                for (AdContent content : contents) {
                    content.setImageUrl(mediaFileUrl);
                }
            } else {
                // Use specified provider or default to Gemini
                String effectiveImageProvider = (imageProvider != null && !imageProvider.isBlank())
                    ? imageProvider
                    : defaultImageProvider;

                log.info("🎨 Generating images for {} variations using provider: {}", contents.size(), effectiveImageProvider);

                // Smart fallback: Track provider state across variations to reduce API calls
                String workingProvider = effectiveImageProvider;
                boolean primaryProviderFailed = false;
                int totalApiCalls = 0;
                int successfulGenerations = 0;

                for (int i = 0; i < contents.size(); i++) {
                    AdContent content = contents.get(i);
                    boolean retryWithFallback = false;

                    try {
                        totalApiCalls++;
                        // Issue #9: Extract concise image prompt instead of using full primaryText
                        String imagePrompt = extractImagePrompt(prompt, content);
                        log.debug("[VARIATION {}/{}] Generating image with provider: {}", i + 1, contents.size(), workingProvider);

                        String imageUrl = aiProviderService.generateImageWithReliability(
                            imagePrompt, workingProvider);

                        // Only download if it's truly an external URL
                        if (isExternalUrl(imageUrl)) {
                            log.debug("External image URL detected, downloading: {}", imageUrl);
                            String storedImageUrl = downloadAndStoreImage(imageUrl);
                            content.setImageUrl(storedImageUrl);
                            log.info("Generated and stored image for variation: {}", storedImageUrl);
                        } else {
                            // Already local, use as-is
                            log.debug("Local image path detected, using directly: {}", imageUrl);
                            content.setImageUrl(imageUrl);
                            log.info("Using local image for variation: {}", imageUrl);
                        }

                        successfulGenerations++;
                        log.debug("✅ [VARIATION {}/{}] Image generated successfully with {}", i + 1, contents.size(), workingProvider);

                    } catch (Exception e) {
                        // Smart fallback: If primary provider fails on first variation, switch for all remaining
                        if (!primaryProviderFailed && workingProvider.equals(effectiveImageProvider) && i == 0) {
                            primaryProviderFailed = true;
                            String fallbackProvider = getFallbackProvider(effectiveImageProvider);
                            log.warn("⚠️ [SMART FALLBACK] Primary provider '{}' failed on first variation. " +
                                "Switching to '{}' for all remaining {} variations to reduce API calls.",
                                effectiveImageProvider, fallbackProvider, contents.size() - 1);
                            workingProvider = fallbackProvider;
                            retryWithFallback = true;
                        }

                        // Retry current variation with fallback provider if it's the first failure
                        if (retryWithFallback) {
                            try {
                                totalApiCalls++;
                                // Issue #9: Extract concise image prompt instead of using full primaryText
                                String imagePrompt = extractImagePrompt(prompt, content);
                                log.debug("[VARIATION {}/{}] Retrying with fallback provider: {}", i + 1, contents.size(), workingProvider);

                                String imageUrl = aiProviderService.generateImageWithReliability(
                                    imagePrompt, workingProvider);

                                if (isExternalUrl(imageUrl)) {
                                    String storedImageUrl = downloadAndStoreImage(imageUrl);
                                    content.setImageUrl(storedImageUrl);
                                    log.info("Generated and stored image after fallback: {}", storedImageUrl);
                                } else {
                                    content.setImageUrl(imageUrl);
                                    log.info("Using local image after fallback: {}", imageUrl);
                                }

                                successfulGenerations++;
                                log.info("✅ [VARIATION {}/{}] Image generated successfully after fallback to {}",
                                    i + 1, contents.size(), workingProvider);

                            } catch (Exception retryException) {
                                log.error("❌ [VARIATION {}/{}] Failed to generate/store image even with fallback provider {}: {}",
                                    i + 1, contents.size(), workingProvider, retryException.getMessage());
                                content.setImageUrl("/img/placeholder.png");
                            }
                        } else {
                            log.error("❌ [VARIATION {}/{}] Failed to generate/store image with provider {}: {}",
                                i + 1, contents.size(), workingProvider, e.getMessage());
                            // Keep default placeholder on error
                            content.setImageUrl("/img/placeholder.png");
                        }
                    }
                }

                // Summary logging
                log.info("📊 [IMAGE GENERATION SUMMARY] Provider: {} | Variations: {} | Successful: {} | Total API calls: {} | Avg calls/variation: {.2f}",
                    effectiveImageProvider, contents.size(), successfulGenerations, totalApiCalls,
                    (double) totalApiCalls / contents.size());

                if (totalApiCalls > contents.size() * 2) {
                    log.warn("⚠️ [HIGH API USAGE] Total API calls ({}) exceeds 2x variations ({}). Check provider reliability.",
                        totalApiCalls, contents.size());
                }

                log.info("Images added and stored using enhanced reliability features");
            }

            // Validate and filter generated content
            List<AdContent> validatedContents = validationService.validateAndFilterContent(contents);

            if (validatedContents.isEmpty()) {
                log.error("All {} generated content variations were filtered out due to validation failures", contents.size());

                // Provide detailed feedback about what went wrong
                StringBuilder errorMessage = new StringBuilder("Generated content did not meet quality standards. ");

                // Analyze why all content failed
                if (contents.size() > 0) {
                    errorMessage.append("Issues found: ");
                    // This will be logged, but we return at least something if possible
                    log.error("All variations rejected. This should rarely happen with new validation logic.");
                }

                errorMessage.append("Please try with a different prompt or adjust your requirements.");
                throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage.toString());
            }

            // Log validation results with details
            long contentWithWarnings = validatedContents.stream()
                    .filter(c -> c.getHasWarnings() != null && c.getHasWarnings())
                    .count();

            if (contentWithWarnings > 0) {
                log.warn("Successfully generated {} content variations, {} have quality warnings",
                        validatedContents.size(), contentWithWarnings);
            } else {
                log.info("Successfully generated and validated {} content variations with no warnings",
                        validatedContents.size());
            }

            return validatedContents;

        } catch (Exception e) {
            log.error("Failed to generate content with {}: {}", textAI.getProviderName(), e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate content: " + e.getMessage(), e);
        }
    }

    /**
     * Build final prompt by combining original prompt and ad link content
     */
    private String buildFinalPrompt(String originalPrompt, List<String> adLinks, String extractedContent) {
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
     * Issue #9: Enhance prompt with Campaign-level audience
     * Phase 1 & 2: Accept persona and trending keywords
     */
    private String enhancePromptWithCampaign(String userPrompt, AdType adType,
                                             com.fbadsautomation.model.Campaign campaign,
                                             com.fbadsautomation.model.AdStyle adStyle,
                                             com.fbadsautomation.model.Persona userSelectedPersona,
                                             List<String> trendingKeywords) {
        // Detect language for bilingual support
        Language detectedLanguage = ValidationMessages.detectLanguage(userPrompt);
        log.info("[Phase 3] Detected language: {}, campaign: {}, persona: {}, keywords: {}",
                 detectedLanguage,
                 campaign != null ? campaign.getId() : "none",
                 userSelectedPersona != null ? userSelectedPersona.getName() : "auto-select",
                 trendingKeywords != null ? trendingKeywords.size() : 0);

        // Phase 3: Try unified Chain-of-Thought prompting first
        if (chainOfThoughtPromptBuilder != null) {
            try {
                log.info("[Phase 3] Using unified Chain-of-Thought prompt builder");

                // Get campaign target audience
                String targetAudience = (campaign != null && campaign.getTargetAudience() != null)
                    ? campaign.getTargetAudience()
                    : "General audience";

                // Build CoT prompt with all parameters (persona can be null - will be handled by builder)
                String cotPrompt = chainOfThoughtPromptBuilder.buildCoTPrompt(
                    userPrompt,
                    userSelectedPersona,  // Pass user Persona directly (can be null)
                    adStyle,
                    targetAudience,
                    trendingKeywords,
                    detectedLanguage,
                    com.fbadsautomation.model.FacebookCTA.LEARN_MORE,  // Default CTA for prompt
                    adType,
                    1  // numberOfVariations for prompt context
                );

                log.info("[Phase 3] CoT prompt built successfully with {} stages",
                        userSelectedPersona != null ? "user persona" : "no persona");
                return cotPrompt;

            } catch (Exception e) {
                log.warn("[Phase 3] CoT prompting failed, falling back to multi-stage: {}", e.getMessage());
            }
        }

        // Fallback to Phase 1&2: multi-stage persona-based prompting with campaign (Issue #9)
        if (personaSelectorService != null && multiStagePromptBuilder != null) {
            try {
                log.info("[Phase 1&2 Fallback] Using multi-stage prompting with campaign-level audience");

                // Phase 1: Use user-selected persona if provided, otherwise auto-select
                com.fbadsautomation.model.AdPersona persona;
                if (userSelectedPersona != null) {
                    // Convert user Persona to AdPersona (map tone to closest match)
                    persona = mapUserPersonaToAdPersona(userSelectedPersona);
                    log.info("[Phase 1] Using user-selected persona: {} (mapped to AdPersona: {})",
                            userSelectedPersona.getName(), persona.name());
                } else {
                    // Auto-select persona based on product/service
                    persona = personaSelectorService.selectPersona(userPrompt, detectedLanguage);
                    log.info("[Phase 1] Auto-selected persona: {}", persona.name());
                }

                // Phase 2: Add trending keywords to prompt if provided
                String finalUserPrompt = userPrompt;
                if (trendingKeywords != null && !trendingKeywords.isEmpty()) {
                    finalUserPrompt = enrichPromptWithTrendingKeywords(userPrompt, trendingKeywords, detectedLanguage);
                    log.info("[Phase 2] Added {} trending keywords to prompt", trendingKeywords.size());
                }

                // Build enhanced prompt with persona, campaign audience, and style (Issue #8 + #9)
                String enhancedPrompt = multiStagePromptBuilder.buildEnhancedPrompt(
                    finalUserPrompt,  // Use enriched prompt with trending keywords
                    persona,
                    adType,
                    detectedLanguage,
                    campaign,  // Use Campaign instead of AudienceSegment (Issue #9)
                    adStyle    // Include AdStyle (Issue #8)
                );

                log.info("[Phase 1&2 Fallback] Multi-stage prompt built with campaign audience, persona: {}, keywords: {}",
                        persona.name(), trendingKeywords != null ? trendingKeywords.size() : 0);
                return enhancedPrompt;

            } catch (Exception e) {
                log.warn("[Issue #9] Multi-stage prompting failed, falling back to legacy: {}", e.getMessage());
            }
        }

        // Final fallback to legacy prompting
        log.info("[Issue #9] Using legacy prompt (no campaign audience integration)");
        return buildLegacyPromptForAdType(userPrompt, adType, detectedLanguage);
    }

    /**
     * Legacy: Enhance user prompt based on ad type with validation constraints
     * This method uses AudienceSegment (deprecated in favor of Campaign)
     * @deprecated Use enhancePromptWithCampaign instead (Issue #9)
     */
    @Deprecated
    private String enhancePromptForAdType(String userPrompt, AdType adType, com.fbadsautomation.dto.AudienceSegmentRequest audienceSegment) {
        // Detect language for bilingual support
        Language detectedLanguage = ValidationMessages.detectLanguage(userPrompt);
        log.info("Detected language for prompt enrichment: {}", detectedLanguage);

        // Try multi-stage persona-based prompting first (new approach)
        if (personaSelectorService != null && multiStagePromptBuilder != null) {
            try {
                log.info("Using multi-stage persona-based prompting for natural ad generation");

                // Select appropriate persona based on product/service
                com.fbadsautomation.model.AdPersona persona = personaSelectorService.selectPersona(
                    userPrompt,
                    detectedLanguage
                );

                // Build enhanced prompt with persona, examples, and constraints
                String enhancedPrompt = multiStagePromptBuilder.buildEnhancedPrompt(
                    userPrompt,
                    persona,
                    adType,
                    detectedLanguage,
                    audienceSegment, // Now properly passed from caller
                    null  // AdStyle - will be passed from Ad object in Issue #9 refactor
                );

                log.info("Multi-stage prompt built successfully with persona: {}", persona.name());
                return enhancedPrompt;

            } catch (Exception e) {
                log.warn("Multi-stage prompting failed, falling back to legacy prompt: {}", e.getMessage());
            }
        }

        // Fallback to legacy prompting (old approach)
        log.info("Using legacy prompt enhancement (audience segment handled separately)");
        String legacyPrompt = buildLegacyPromptForAdType(userPrompt, adType, detectedLanguage);

        // Inject audience segment for legacy approach
        if (audienceSegment != null) {
            legacyPrompt = injectAudienceSegment(legacyPrompt, audienceSegment);
        }

        return legacyPrompt;
    }

    /**
     * Legacy prompt builder (fallback when multi-stage prompting unavailable)
     */
    private String buildLegacyPromptForAdType(String userPrompt, AdType adType, Language detectedLanguage) {
        StringBuilder enhanced = new StringBuilder(userPrompt);

        // Add validation constraints to guide AI generation in appropriate language
        if (detectedLanguage == Language.VIETNAMESE) {
            enhanced.append("\n\n📋 YÊU CẦU NỘI DUNG:\n");
            enhanced.append("✓ Tiêu đề: Tối đa 40 ký tự\n");
            enhanced.append("✓ Mô tả: Tối đa 125 ký tự\n");
            enhanced.append("✓ Văn bản chính: Tối đa 1000 ký tự\n");
            enhanced.append("✓ Giọng điệu: Chuyên nghiệp, rõ ràng, hướng tới hành động\n");
            enhanced.append("✓ Phong cách: Tránh dấu chấm than/hỏi quá nhiều (!!!, ???), chữ in hoa toàn bộ, hoặc ngôn ngữ spam\n");
            enhanced.append("✓ Tuân thủ: Tuân theo chính sách quảng cáo Facebook - tránh các tuyên bố như 'kết quả đảm bảo', 'thần kỳ', 'làm giàu nhanh'\n");
            enhanced.append("✓ Chất lượng: Đảm bảo nội dung mạch lạc, hấp dẫn và nhất quán trên tất cả các trường\n");
            enhanced.append("\n💡 Tạo nội dung đáp ứng các tiêu chuẩn này để đảm bảo được duyệt.\n");
        } else {
            enhanced.append("\n\n📋 CONTENT REQUIREMENTS:\n");
            enhanced.append("✓ Headline: Maximum 40 characters\n");
            enhanced.append("✓ Description: Maximum 125 characters\n");
            enhanced.append("✓ Primary Text: Maximum 1000 characters\n");
            enhanced.append("✓ Tone: Professional, clear, action-oriented\n");
            enhanced.append("✓ Style: Avoid excessive punctuation (!!!, ???), all caps, or spam-like language\n");
            enhanced.append("✓ Compliance: Follow Facebook advertising policies - avoid claims like 'guaranteed results', 'miracle', 'get rich quick'\n");
            enhanced.append("✓ Quality: Ensure content is coherent, engaging, and aligned across all fields\n");
            enhanced.append("\n💡 Generate content that meets these standards to ensure approval.\n");
        }

        return enhanced.toString();
    }

    /**
     * Inject audience segment variables into the prompt for personalized ad targeting
     */
    private String injectAudienceSegment(String basePrompt, com.fbadsautomation.dto.AudienceSegmentRequest audienceSegment) {
        StringBuilder enhanced = new StringBuilder(basePrompt);
        enhanced.append("\n\n🎯 TARGET AUDIENCE PROFILE:\n");

        if (audienceSegment.getGender() != null) {
            enhanced.append("👥 Gender: ").append(audienceSegment.getGender().getDisplayName()).append("\n");
        }

        if (audienceSegment.getMinAge() != null && audienceSegment.getMaxAge() != null) {
            enhanced.append("📅 Age Range: ").append(audienceSegment.getMinAge())
                    .append(" - ").append(audienceSegment.getMaxAge()).append(" years old\n");
        } else if (audienceSegment.getMinAge() != null) {
            enhanced.append("📅 Minimum Age: ").append(audienceSegment.getMinAge()).append("+ years old\n");
        }

        if (audienceSegment.getLocation() != null && !audienceSegment.getLocation().trim().isEmpty()) {
            enhanced.append("📍 Location: ").append(audienceSegment.getLocation()).append("\n");
        }

        if (audienceSegment.getInterests() != null && !audienceSegment.getInterests().trim().isEmpty()) {
            enhanced.append("❤️ Interests: ").append(audienceSegment.getInterests()).append("\n");
        }

        enhanced.append("\n💡 Please create ad content that resonates with this specific audience demographic.");

        log.info("Audience segment injected into prompt: gender={}, age={}-{}, location={}",
                audienceSegment.getGender(),
                audienceSegment.getMinAge(),
                audienceSegment.getMaxAge(),
                audienceSegment.getLocation());

        return enhanced.toString();
    }

    /**
     * Get first fallback provider from the chain when primary provider fails
     * This helps reduce redundant API calls by switching providers early
     *
     * @param primaryProvider The primary provider that failed
     * @return The first fallback provider from the chain
     */
    private String getFallbackProvider(String primaryProvider) {
        // Return first fallback from provider chain
        return switch (primaryProvider.toLowerCase()) {
            case "gemini" -> "openai";
            case "openai" -> "gemini";
            case "fal-ai" -> "stable-diffusion";
            case "stable-diffusion" -> "fal-ai";
            default -> "gemini"; // Default fallback
        };
    }

    /**
     * Download external AI-generated image and store it in MinIO to prevent expiration
     * @param externalImageUrl The external image URL (e.g., from OpenAI, Stability AI)
     * @return The stored image URL accessible via /api/images/{filename}
     */
    private String downloadAndStoreImage(String externalImageUrl) throws Exception {
        try {
            log.info("Downloading external image: {}", externalImageUrl);

            // Download image from external URL
            URL url = new URL(externalImageUrl);
            byte[] imageBytes = url.openStream().readAllBytes();

            // Generate unique filename
            String extension = externalImageUrl.contains(".png") ? "png" : "jpg";
            String filename = "ai-gen-" + UUID.randomUUID().toString() + "." + extension;

            // Create MultipartFile wrapper for byte array
            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                imageBytes,
                filename,
                "image/" + extension,
                filename
            );

            // Store in MinIO
            String storedFilename = minIOStorageService.uploadFile(multipartFile);

            // Return API gateway URL instead of MinIO direct URL
            String apiImageUrl = "/api/images/" + storedFilename;
            log.info("Successfully stored external image as: {}", apiImageUrl);

            return apiImageUrl;
        } catch (Exception e) {
            log.error("Failed to download and store external image: {}", e.getMessage(), e);
            throw new Exception("Failed to download and store image: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 1: Maps user-created Persona to predefined AdPersona enum.
     * Mapping is based on tone field matching.
     *
     * @param userPersona User-created persona entity
     * @return Mapped AdPersona enum value
     */
    private com.fbadsautomation.model.AdPersona mapUserPersonaToAdPersona(com.fbadsautomation.model.Persona userPersona) {
        if (userPersona == null || userPersona.getTone() == null) {
            log.warn("[Phase 1] User persona is null or has no tone, defaulting to GENERAL_FRIENDLY");
            return com.fbadsautomation.model.AdPersona.GENERAL_FRIENDLY;
        }

        String tone = userPersona.getTone().toLowerCase();

        // Map user persona tone to closest AdPersona
        return switch (tone) {
            case "professional", "formal" -> {
                log.info("[Phase 1] Mapping tone '{}' to PROFESSIONAL_TRUSTWORTHY", tone);
                yield com.fbadsautomation.model.AdPersona.PROFESSIONAL_TRUSTWORTHY;
            }
            case "funny" -> {
                log.info("[Phase 1] Mapping tone '{}' to GEN_Z_GAMER", tone);
                yield com.fbadsautomation.model.AdPersona.GEN_Z_GAMER;
            }
            case "enthusiastic" -> {
                log.info("[Phase 1] Mapping tone '{}' to HEALTH_WELLNESS", tone);
                yield com.fbadsautomation.model.AdPersona.HEALTH_WELLNESS;
            }
            case "casual", "friendly" -> {
                log.info("[Phase 1] Mapping tone '{}' to GENERAL_FRIENDLY", tone);
                yield com.fbadsautomation.model.AdPersona.GENERAL_FRIENDLY;
            }
            default -> {
                log.warn("[Phase 1] Unknown tone '{}', defaulting to GENERAL_FRIENDLY", tone);
                yield com.fbadsautomation.model.AdPersona.GENERAL_FRIENDLY;
            }
        };
    }

    /**
     * Phase 2: Enriches user prompt with trending keywords in a natural way.
     * Supports bilingual output (Vietnamese/English).
     *
     * @param userPrompt Original user prompt
     * @param trendingKeywords List of trending keywords to incorporate
     * @param language Detected language
     * @return Enriched prompt with trending keywords section
     */
    private String enrichPromptWithTrendingKeywords(String userPrompt, List<String> trendingKeywords, Language language) {
        if (trendingKeywords == null || trendingKeywords.isEmpty()) {
            return userPrompt;
        }

        boolean isVietnamese = (language == Language.VIETNAMESE);
        StringBuilder enrichedPrompt = new StringBuilder(userPrompt);
        enrichedPrompt.append("\n\n");

        if (isVietnamese) {
            enrichedPrompt.append("🔥 TỪ KHÓA TRENDING CẦN KẾT HỢP:\n");
            enrichedPrompt.append("Vui lòng tự nhiên kết hợp các từ khóa trending sau vào nội dung quảng cáo (nếu phù hợp):\n");
        } else {
            enrichedPrompt.append("🔥 TRENDING KEYWORDS TO INCORPORATE:\n");
            enrichedPrompt.append("Please naturally incorporate the following trending keywords into the ad content (where appropriate):\n");
        }

        for (int i = 0; i < trendingKeywords.size(); i++) {
            enrichedPrompt.append((i + 1)).append(". ").append(trendingKeywords.get(i)).append("\n");
        }

        if (isVietnamese) {
            enrichedPrompt.append("\nLưu ý: Chỉ sử dụng những từ khóa phù hợp với ngữ cảnh và giọng điệu của quảng cáo. Không nhồi nhét từ khóa một cách máy móc.");
        } else {
            enrichedPrompt.append("\nNote: Only use keywords that fit naturally with the context and tone of the ad. Avoid keyword stuffing.");
        }

        log.info("[Phase 2] Enriched prompt with {} trending keywords in {} language",
                trendingKeywords.size(), language.name());

        return enrichedPrompt.toString();
    }

    /**
     * Issue #9: Extract concise image prompt from text content or original prompt.
     * Image generators work best with short, descriptive prompts (< 200 chars)
     * focusing on visual elements, not full ad copy.
     *
     * @param basePrompt Original user prompt (e.g., "Advertise iPhone 15 Pro")
     * @param adContent Generated ad content (headline + description)
     * @return Concise image prompt suitable for image generation
     */
    private String extractImagePrompt(String basePrompt, AdContent adContent) {
        StringBuilder imagePrompt = new StringBuilder();

        // Strategy 1: Use headline if available (most concise)
        if (adContent != null && adContent.getHeadline() != null && !adContent.getHeadline().isEmpty()) {
            String headline = adContent.getHeadline();
            // Remove emojis and special characters, keep only descriptive text
            headline = headline.replaceAll("[^\\p{L}\\p{N}\\s\\-,.]", "").trim();
            // Limit to first 100 chars
            if (headline.length() > 100) {
                headline = headline.substring(0, 100);
            }
            imagePrompt.append(headline);
        }
        // Strategy 2: Extract first sentence from base prompt
        else if (basePrompt != null && !basePrompt.isEmpty()) {
            // Remove instruction words
            String cleaned = basePrompt
                .replaceAll("(?i)(advertise|promote|create ad for|generate content for|quảng cáo|tạo nội dung|giới thiệu)", "")
                .trim();

            // Get first sentence or first 100 chars
            int endIndex = cleaned.indexOf('.');
            if (endIndex > 0 && endIndex < 100) {
                cleaned = cleaned.substring(0, endIndex);
            } else if (cleaned.length() > 100) {
                cleaned = cleaned.substring(0, 100);
            }
            imagePrompt.append(cleaned.trim());
        }
        // Fallback
        else {
            imagePrompt.append("Professional advertisement product");
        }

        // Add visual style keywords
        imagePrompt.append(", professional advertising photography, high quality, vibrant colors, eye-catching");

        String result = imagePrompt.toString();
        log.info("[Issue #9] Extracted image prompt (length: {}): {}", result.length(),
            result.length() > 100 ? result.substring(0, 100) + "..." : result);

        return result;
    }
}
