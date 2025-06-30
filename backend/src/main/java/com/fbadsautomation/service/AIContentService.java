package com.fbadsautomation.service;

import com.fbadsautomation.integration.ai.AIContentServiceImpl;
import com.fbadsautomation.service.AIProviderService;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.dto.ProviderResponse;
import com.fbadsautomation.repository.AdContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIContentService {

    private final AIContentServiceImpl aiIntegrationService;
    private final AIProviderService aiProviderService;
    private final AdContentRepository adContentRepository;
    private static final int DEFAULT_VARIATIONS = 1; // Default number of variations

    /**
     * Generate ad content for an ad
     * @param ad The ad to generate content for
     * @param prompt The content prompt
     * @param mediaFile The media file (optional)
     * @return List of generated ad contents
     */
    public List<AdContent> generateAdContent(Ad ad, String prompt, org.springframework.web.multipart.MultipartFile mediaFile, String textProvider, String imageProvider, Integer numberOfVariations, String language) {
        log.info("Generating content for ad: {}", ad.getId());
        
        // Determine content type based on ad type
        AdContent.ContentType contentType = determineContentType(ad.getAdType());
        
        List<AdContent> generatedContents = aiIntegrationService.generateContent(
                prompt, contentType, textProvider, imageProvider, numberOfVariations != null ? numberOfVariations : DEFAULT_VARIATIONS, language);
        
        // Set ad reference and preview order for each content
        for (int i = 0; i < generatedContents.size(); i++) {
            AdContent content = generatedContents.get(i);
            content.setAd(ad);
            content.setUser(ad.getUser());
            content.setPreviewOrder(i + 1);
            content.setIsSelected(false);
        }
        
        // Save all generated contents
        return adContentRepository.saveAll(generatedContents);
    }

    /**
     * Gets all available AI providers and their capabilities.
     *
     * @return Map of provider names to a list of their capabilities (e.g., ["TEXT", "IMAGE"])
     */
    public Map<String, List<String>> getAvailableProviders() {
        return aiProviderService.getAllProviders().stream()
                .collect(Collectors.toMap(
                        ProviderResponse::getId,
                        ProviderResponse::getCapabilities
                ));
    }

    /**
     * Determine content type based on ad type
     * @param adType The ad type
     * @return The content type
     */
    private AdContent.ContentType determineContentType(Ad.AdType adType) {
        return switch (adType) {
            case PAGE_POST_AD -> AdContent.ContentType.PAGE_POST;
            case WEBSITE_CONVERSION_AD, LEAD_FORM_AD -> AdContent.ContentType.COMBINED;
        };
    }
}

