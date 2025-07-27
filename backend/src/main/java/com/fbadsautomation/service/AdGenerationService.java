package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdGenerationResponse;
import com.fbadsautomation.model.AdRequest;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service

public class AdGenerationService {

    private final AIProviderService aiProviderService;
    private final AdRepository adRepository;
    private final AdContentRepository adContentRepository;

    public AdGenerationService(
            AIProviderService aiProviderService,
            AdRepository adRepository,
            AdContentRepository adContentRepository) {
        this.aiProviderService = aiProviderService;
        this.adRepository = adRepository;
        this.adContentRepository = adContentRepository;
    }

    public AdGenerationResponse generateAd(AdRequest request) {
        // 1. Get the Text Provider
        String textProviderId = request.getTextProvider();
        if (textProviderId == null || textProviderId.isEmpty()) {
            log.error("Missing or empty textProvider in request.");
            return createErrorResponse("Text provider ID is required.");
        }
        AIProvider textProvider = aiProviderService.getProvider(textProviderId);
        if (textProvider == null) {
            log.error("Text provider ID " + textProviderId + " not found. Cannot generate ad content.");
            // Handle error appropriately, maybe return an error response or throw exception
            // For now, return empty response or mock data
            return createErrorResponse("Text provider not found.");
        }
        log.info("Using text provider: {}", textProvider.getProviderName());

        // 2. Generate Ad Content (Text)
        List<AdContent> adContents = textProvider.generateAdContent(
                request.getPrompt(),
                request.getNumberOfVariations(),
                request.getLanguage(),
                request.getCallToAction()
        );
        // 3. Get the Image Provider (if specified)
        AIProvider imageProvider = null;
        if (request.getImageProvider() != null && !request.getImageProvider().isEmpty()) {
            imageProvider = aiProviderService.getProvider(request.getImageProvider());
            if (imageProvider == null) {
                log.warn("Image provider ID " + request.getImageProvider() + " not found. Proceeding without image generation from this provider.");
            } else {
                log.info("Using image provider: {}", imageProvider.getProviderName());
            }
        }

        // 4. Generate Images (using the selected image provider)
        for (AdContent adContent : adContents) {
            adContent.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE); // Create temp ID

            String imageUrl = "/img/placeholder.png"; // Default placeholder
            if (imageProvider != null && imageProvider.supportsImageGeneration()) {
                try {
                    // Generate image using the specifically selected image provider
                    String imagePrompt = "Advertisement visual for: " + adContent.getHeadline(); // Use headline as prompt
                    log.debug("Requesting image generation from {} with prompt: {}", imageProvider.getProviderName(), imagePrompt);
                    imageUrl = imageProvider.generateImage(imagePrompt);
                    log.info("Generated image URL from {}: {}", imageProvider.getProviderName(), imageUrl);
                } catch (Exception e) {
                    log.error("Error generating image with provider {}: {}", imageProvider.getProviderName(), e.getMessage());
                    // Keep placeholder on error
                }
            } else if (imageProvider != null) {
                 log.warn("Selected image provider {} does not support image generation.", imageProvider.getProviderName());
            } else {
                 log.info("No image provider selected or found. Using placeholder image.");
            }
            adContent.setImageUrl(imageUrl);
        }

        // Note: Video generation is handled separately via VideoGenerationController/Service
        // based on request.getGenerateVideo() and request.getVideoProvider()

        // 5. Create response
        AdGenerationResponse response = AdGenerationResponse.builder()
        .adVariations(adContents)
        .status("success")
        .message("Ad content generated successfully")
        .build();
        return response;
    }

    private AdGenerationResponse createErrorResponse(String errorMessage) {
        AdGenerationResponse response = new AdGenerationResponse();
        // You might want a dedicated error field in AdResponse
        // For now, returning empty variations
        log.error(errorMessage);
        return response;
    }
}
