package com.fbadsautomation.service;

import com.fbadsautomation.dto.ProviderResponse;
import com.fbadsautomation.integration.ai.AIContentServiceImpl;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.service.AIProviderService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor

public class AIContentService {

    private final AIContentServiceImpl aiIntegrationService;
    private final AIProviderService aiProviderService;
    private final AdContentRepository adContentRepository;
    private final AdRepository adRepository;
    private static final int DEFAULT_VARIATIONS = 1; // Default number of variations

    /**
     * Generate ad content for an ad
     * @param ad The ad to generate content for
     * @param prompt The content prompt
     * @param mediaFile The media file (optional)
     * @return List of generated ad contents
     */
    public List<AdContent> generateAdContent(Ad ad, String prompt, org.springframework.web.multipart.MultipartFile mediaFile, String textProvider, String imageProvider, Integer numberOfVariations, String language, List<String> adLinks, String promptStyle, String customPrompt, String extractedContent, com.fbadsautomation.model.FacebookCTA callToAction) {
        log.info("Generating content for ad: {}", ad.getId());
        
        // Determine content type based on ad type
        AdContent.ContentType contentType = determineContentType(ad.getAdType());
        
        // Always use original prompt, extractedContent will be handled in buildFinalPrompt
        // Sử dụng CTA được truyền hoặc default nếu null
        com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
        List<AdContent> generatedContents = aiIntegrationService.generateContent(
                prompt, contentType, textProvider, imageProvider, numberOfVariations != null ? numberOfVariations : DEFAULT_VARIATIONS, language, adLinks, promptStyle, customPrompt, extractedContent, cta);
        
        // Set ad reference and preview order for each content
        for (int i = 0; i < generatedContents.size(); i++) {
            AdContent content = generatedContents.get(i);
        // Kiểm tra xem có phải là preview hay không (ad.getId() == null)
            if (ad.getId() == null) {
                // Đây là preview, không cần lưu vào database
                log.info("Preview mode: setting temporary ad reference for content {}", i);
                content.setAd(ad);
                content.setUser(ad.getUser());
                content.setPreviewOrder(i + 1);
                content.setIsSelected(false);
            } else {
                // Đây là save thực sự, cần đảm bảo ad đã được lưu
                log.info("Setting ad reference for content {}: adId= {}, userId={}", i, ad.getId(), ad.getUser().getId());
        // Lấy ad đã được quản lý từ repository
                Ad managedAd = adRepository.findById(ad.getId()).orElseThrow(() -> new RuntimeException("Ad not found"));
                content.setAd(managedAd);
                content.setUser(managedAd.getUser());
                content.setPreviewOrder(i + 1);
                content.setIsSelected(false);
            };
    }
        
        // Save all generated contents (only if not in preview mode)
        if (ad.getId() != null) {
            return saveAdContentsWithTransaction(generatedContents);
        } else {
            // Preview mode: return without saving
            log.info("Preview mode: returning {} contents without saving to database", generatedContents.size());
            return generatedContents;
        }
    }
    
    @org.springframework.transaction.annotation.Transactional
    private List<AdContent> saveAdContentsWithTransaction(List<AdContent> contents) {
        return adContentRepository.saveAll(contents);
    }

    // Overload cho AdContentService gọi
    public List<AdContent> generateContent(String prompt, AdContent.ContentType contentType, String provider, int numberOfVariations) {
        // Gọi hàm chính với các tham số còn lại là null/mặc định
        return aiIntegrationService.generateContent(prompt, contentType, provider, null, numberOfVariations, null, null, null, null, null, com.fbadsautomation.model.FacebookCTA.LEARN_MORE);
    }
    
    // Overload cho AdContentService gọi với default CTA
    public List<AdContent> generateContent(String prompt, AdContent.ContentType contentType, String provider, int numberOfVariations, com.fbadsautomation.model.FacebookCTA callToAction) {
        // Gọi hàm chính với các tham số còn lại là null/mặc định
        com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
        return aiIntegrationService.generateContent(prompt, contentType, provider, null, numberOfVariations, null, null, null, null, null, cta);
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
    private AdContent.ContentType determineContentType(AdType adType) {
        return switch (adType) {
            case PAGE_POST_AD -> AdContent.ContentType.PAGE_POST;
            case WEBSITE_CONVERSION_AD, LEAD_FORM_AD -> AdContent.ContentType.COMBINED;
        };
    }
}
