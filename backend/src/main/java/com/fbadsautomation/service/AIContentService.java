package com.fbadsautomation.service;

import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.ProviderResponse;
import com.fbadsautomation.integration.ai.AIContentServiceImpl;
import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.service.AIProviderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
 
 public class AIContentService {
    private static final Logger log = LoggerFactory.getLogger(AIContentService.class);
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
    * @param mediaFileUrl The uploaded media file URL from frontend (optional)
    * @param userSelectedPersona User-selected persona (Phase 1, optional)
    * @param trendingKeywords Trending keywords to incorporate (Phase 2, optional)
    * @return List of generated ad contents
    */
   public List<AdContent> generateAdContent(Ad ad,
                                           String prompt,
                                           org.springframework.web.multipart.MultipartFile mediaFile,
                                           String textProvider,
                                           String imageProvider,
                                           Integer numberOfVariations,
                                           String language,
                                           List<String> adLinks,
                                           String extractedContent,
                                           String mediaFileUrl,
                                           com.fbadsautomation.model.FacebookCTA callToAction,
                                           com.fbadsautomation.dto.AudienceSegmentRequest audienceSegment,
                                           com.fbadsautomation.model.Persona userSelectedPersona,
                                           List<String> trendingKeywords,
                                           List<AdGenerationRequest.VariationProviderConfig> variationConfigs,
                                           boolean enforceLengthLimits) {
       log.info("[Issue #9] Generating content for ad: {}, campaign: {}, mediaFileUrl: {}, persona: {}, trending keywords: {}",
                ad.getId(),
                ad.getCampaign() != null ? ad.getCampaign().getId() : "none",
                mediaFileUrl,
                userSelectedPersona != null ? userSelectedPersona.getName() : "auto-select",
                trendingKeywords != null ? trendingKeywords.size() : 0);

       // Determine content type based on ad type
       AdContent.ContentType contentType = determineContentType(ad.getAdType());

       // Issue #9: Get Campaign from Ad for campaign-level audience targeting
       com.fbadsautomation.model.Campaign campaign = ad.getCampaign();
       if (campaign != null && campaign.getTargetAudience() != null && !campaign.getTargetAudience().trim().isEmpty()) {
           log.info("[Issue #9] Using campaign-level target audience: {}", campaign.getTargetAudience());
       } else {
           log.warn("[Issue #9] Campaign has no target audience defined");
       }

       // Always use original prompt, extractedContent will be handled in buildFinalPrompt
       // Sử dụng CTA được truyền hoặc default nếu null
       com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;

       int effectiveVariations = numberOfVariations != null ? numberOfVariations : DEFAULT_VARIATIONS;
       if (variationConfigs != null && !variationConfigs.isEmpty()) {
           effectiveVariations = variationConfigs.size();
       }

       // Issue #9: Pass Campaign and AdStyle instead of AudienceSegment
       // Phase 1 & 2: Pass persona and trending keywords
       // Note: We still accept audienceSegment parameter for backward compatibility but prefer Campaign
       List<AdContent> generatedContents;
       if (variationConfigs != null && !variationConfigs.isEmpty()) {
           generatedContents = generatePerVariationContent(
               prompt,
               contentType,
               variationConfigs,
               textProvider,
               imageProvider,
               language,
               adLinks,
               extractedContent,
               mediaFileUrl,
               cta,
               campaign,
               ad.getAdStyle(),
               userSelectedPersona,
               trendingKeywords,
               enforceLengthLimits);
       } else {
           generatedContents = aiIntegrationService.generateContentWithCampaign(
               prompt,
               contentType,
               textProvider,
               imageProvider,
               effectiveVariations,
               language,
               adLinks,
               extractedContent,
               mediaFileUrl,
               cta,
               campaign,
               ad.getAdStyle(),
               userSelectedPersona,
               trendingKeywords,
               enforceLengthLimits);
       }
       
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

   private List<AdContent> generatePerVariationContent(String prompt,
                                                       AdContent.ContentType contentType,
                                                       List<AdGenerationRequest.VariationProviderConfig> variationConfigs,
                                                       String fallbackTextProvider,
                                                       String fallbackImageProvider,
                                                       String language,
                                                       List<String> adLinks,
                                                       String extractedContent,
                                                       String defaultMediaFileUrl,
                                                       com.fbadsautomation.model.FacebookCTA callToAction,
                                                       com.fbadsautomation.model.Campaign campaign,
                                                       com.fbadsautomation.model.AdStyle adStyle,
                                                       com.fbadsautomation.model.Persona userSelectedPersona,
                                                       List<String> trendingKeywords,
                                                       boolean enforceLengthLimits) {
       List<AdContent> perVariationContents = new ArrayList<>();

       for (int i = 0; i < variationConfigs.size(); i++) {
           AdGenerationRequest.VariationProviderConfig config = variationConfigs.get(i);
           String variationTextProvider = resolveTextProvider(config.getTextProvider(), fallbackTextProvider);

           String mediaFileToUse = defaultMediaFileUrl;
           String variationImageProvider = null;
           if (config.getUploadedFileUrl() != null && !config.getUploadedFileUrl().isBlank()) {
               mediaFileToUse = config.getUploadedFileUrl();
           } else {
               variationImageProvider = resolveImageProvider(config.getImageProvider(), fallbackImageProvider);
           }

           List<AdContent> generated = aiIntegrationService.generateContentWithCampaign(
               prompt,
               contentType,
               variationTextProvider,
               variationImageProvider,
               1,
               language,
               adLinks,
               extractedContent,
               mediaFileToUse,
               callToAction,
               campaign,
               adStyle,
               userSelectedPersona,
               trendingKeywords,
               enforceLengthLimits);

           if (!generated.isEmpty()) {
               perVariationContents.add(generated.get(0));
           } else {
               log.warn("No content returned for variation {} using provider {}", i + 1, variationTextProvider);
           }
       }

       return perVariationContents;
   }

   private String resolveTextProvider(String primary, String fallback) {
       if (primary != null && !primary.isBlank()) {
           return primary;
       }
       if (fallback != null && !fallback.isBlank()) {
           return fallback;
       }
       return "openai";
   }

   private String resolveImageProvider(String primary, String fallback) {
       if (primary != null && !primary.isBlank()) {
           return primary;
       }
       if (fallback != null && !fallback.isBlank()) {
           return fallback;
       }
       return null;
   }
   
   @org.springframework.transaction.annotation.Transactional
   private List<AdContent> saveAdContentsWithTransaction(List<AdContent> contents) {
       return adContentRepository.saveAll(contents);
   }

   // Overload cho AdContentService gọi
   public List<AdContent> generateContent(String prompt, AdContent.ContentType contentType, String provider, int numberOfVariations) {
       // Gọi hàm chính với các tham số còn lại là null/mặc định
       return aiIntegrationService.generateContent(prompt, contentType, provider, null, numberOfVariations, null, null, null, null, null, null, com.fbadsautomation.model.FacebookCTA.LEARN_MORE, null);
   }

   // Overload cho AdContentService gọi với default CTA
   public List<AdContent> generateContent(String prompt, AdContent.ContentType contentType, String provider, int numberOfVariations, com.fbadsautomation.model.FacebookCTA callToAction) {
       // Gọi hàm chính với các tham số còn lại là null/mặc định
       com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
       return aiIntegrationService.generateContent(prompt, contentType, provider, null, numberOfVariations, null, null, null, null, null, null, cta, null);
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
   public String enhanceImage(String imagePath, String enhancementType, String provider, java.util.Map<String, Object> params) throws Exception {
       AIProvider aiProvider = aiProviderService.getProvider(provider);
       if (aiProvider == null) {
           throw new Exception("Provider not found: " + provider);
       }
       return aiProvider.enhanceImage(imagePath, enhancementType, params);
   }

   private AdContent.ContentType determineContentType(AdType adType) {
       return switch (adType) {
           case PAGE_POST_AD -> AdContent.ContentType.PAGE_POST;
           case WEBSITE_CONVERSION_AD, LEAD_FORM_AD -> AdContent.ContentType.COMBINED;
       };
   }
}
