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
     * This is the NEW method that accepts Campaign and AdStyle
     */
    public List<AdContent> generateContentWithCampaign(String prompt,
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
                                                        com.fbadsautomation.model.Campaign campaign,
                                                        com.fbadsautomation.model.AdStyle adStyle) {

        String providerId = (textProvider == null || textProvider.isBlank())
                ? "openai" : textProvider;
        log.info("🔍 [Issue #9] Using campaign-level audience. Provider: '{}', campaign: {}, style: {}",
                 providerId,
                 campaign != null ? campaign.getId() : "none",
                 adStyle != null ? adStyle.name() : "none");

        AIProvider textAI = aiProviderService.getProvider(providerId);
        if (textAI == null) {
            log.error("Unsupported text AI provider: {}", providerId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported AI provider: " + providerId);
        }

        // Build final prompt
        String finalPrompt = buildFinalPrompt(prompt, adLinks, promptStyle, customPrompt, extractedContent);

        try {
            AdType adType = convertContentTypeToAdType(contentType);
            // Issue #9: Use new enhancePromptWithCampaign method
            String enhancedPrompt = enhancePromptWithCampaign(finalPrompt, adType, campaign, adStyle);
            log.info("[Issue #9] Generating {} variations with campaign audience", numberOfVariations);

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

                log.info("Generating images using AI provider: {}", effectiveImageProvider);

                for (AdContent content : contents) {
                    try {
                        String imageUrl = aiProviderService.generateImageWithReliability(
                            content.getPrimaryText(), effectiveImageProvider);

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
                    } catch (Exception e) {
                        log.error("Failed to generate/store image with provider {}: {}",
                            effectiveImageProvider, e.getMessage());
                        content.setImageUrl("/img/placeholder.png");
                    }
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
        String finalPrompt = buildFinalPrompt(prompt, adLinks, promptStyle, customPrompt, extractedContent);

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

                log.info("Generating images for {} variations using provider: {}", contents.size(), effectiveImageProvider);

                for (AdContent content : contents) {
                    try {
                        String imagePrompt = content.getPrimaryText();
                        String imageUrl = aiProviderService.generateImageWithReliability(
                            imagePrompt, effectiveImageProvider);

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
                    } catch (Exception e) {
                        log.error("Failed to generate/store image for variation with provider {}: {}",
                            effectiveImageProvider, e.getMessage());
                        // Keep default placeholder on error
                        content.setImageUrl("/img/placeholder.png");
                    }
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
     * Issue #9: Enhance prompt with Campaign-level audience
     * This is the NEW method for Issue #9 that uses Campaign instead of AudienceSegment
     */
    private String enhancePromptWithCampaign(String userPrompt, AdType adType,
                                             com.fbadsautomation.model.Campaign campaign,
                                             com.fbadsautomation.model.AdStyle adStyle) {
        // Detect language for bilingual support
        Language detectedLanguage = ValidationMessages.detectLanguage(userPrompt);
        log.info("[Issue #9] Detected language: {}, campaign: {}", detectedLanguage,
                 campaign != null ? campaign.getId() : "none");

        // Try multi-stage persona-based prompting with campaign (Issue #9)
        if (personaSelectorService != null && multiStagePromptBuilder != null) {
            try {
                log.info("[Issue #9] Using multi-stage prompting with campaign-level audience");

                // Select appropriate persona based on product/service
                com.fbadsautomation.model.AdPersona persona = personaSelectorService.selectPersona(
                    userPrompt,
                    detectedLanguage
                );

                // Build enhanced prompt with persona, campaign audience, and style (Issue #8 + #9)
                String enhancedPrompt = multiStagePromptBuilder.buildEnhancedPrompt(
                    userPrompt,
                    persona,
                    adType,
                    detectedLanguage,
                    campaign,  // Use Campaign instead of AudienceSegment (Issue #9)
                    adStyle    // Include AdStyle (Issue #8)
                );

                log.info("[Issue #9] Multi-stage prompt built with campaign audience, persona: {}", persona.name());
                return enhancedPrompt;

            } catch (Exception e) {
                log.warn("[Issue #9] Multi-stage prompting failed, falling back: {}", e.getMessage());
            }
        }

        // Fallback to legacy prompting
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
}
