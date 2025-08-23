package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdResponse;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor

public class AdService {

    private final AdRepository adRepository;
    private final CampaignRepository campaignRepository;
    private final AdContentRepository adContentRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final AIContentService aiContentService;
    private final List<AIProvider> aiProviders;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStorageLocation;

    /**
     * Get ad by ID and user
     * @param adId The ad ID
     * @param userId The user ID
     * @return The ad
     */
    public Ad getAdByIdAndUser(Long adId, Long userId) {
        log.info("Getting ad: {} for user ID: {}", adId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return adRepository.findByIdAndUser(adId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found"));
    }

    /**
     * Create ad with AI content generation
     * @param campaignId The campaign ID
     * @param adType The ad type
     * @param prompt The prompt for AI generation
     * @param name The ad name
     * @param mediaFile The media file (optional)
     * @param userId The user ID
     * @return Map containing ad and generated contents
     */
    @Transactional
    public Map<String, Object> createAdWithAIContent(Long campaignId, String adType, String prompt, 
                                                     String name, MultipartFile mediaFile, Long userId, String textProvider, String imageProvider, Integer numberOfVariations, String language, List<String> adLinks, String promptStyle, String customPrompt, String extractedContent, String mediaFileUrl, com.fbadsautomation.model.FacebookCTA callToAction, String websiteUrl, List<AdGenerationRequest.LeadFormQuestion> leadFormQuestions) {
        log.info("Creating ad with AI content for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Campaign campaign = campaignRepository.findByIdAndUser(campaignId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        
        // Create ad
        Ad ad = new Ad();
        ad.setName(name);
        ad.setAdType(com.fbadsautomation.model.AdType.valueOf(adType));
        ad.setPrompt(prompt);
        ad.setCampaign(campaign);
        ad.setUser(user);
        ad.setStatus("GENERATING");
        ad.setCreatedBy(user.getId().toString());
        ad.setCreatedDate(LocalDateTime.now());
        
        // Set ad type specific fields
        if (websiteUrl != null && !websiteUrl.trim().isEmpty()) {
            ad.setWebsiteUrl(websiteUrl);
        }
        
        if (leadFormQuestions != null && !leadFormQuestions.isEmpty()) {
            // Convert lead form questions to JSON string
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String leadFormQuestionsJson = objectMapper.writeValueAsString(leadFormQuestions);
                ad.setLeadFormQuestions(leadFormQuestionsJson);
            } catch (Exception e) {
                log.warn("Failed to serialize lead form questions: {}", e.getMessage());
            }
        }
        
        // Save media file if provided
        if (mediaFile != null && !mediaFile.isEmpty()) {
            String mediaPath = saveMediaFile(mediaFile);
            ad.setImageUrl(mediaPath);
        } else if (mediaFileUrl != null && !mediaFileUrl.isEmpty()) {
            ad.setImageUrl(mediaFileUrl);
        }
        
        // Lưu ad trước để đảm bảo có ID
        ad = adRepository.save(ad);
        log.info("Saved ad with ID: {}", ad.getId());
        
        // Flush để đảm bảo ad được lưu hoàn toàn
        adRepository.flush();
        
        // Đảm bảo ad được quản lý bởi Hibernate và merge với persistence context
        ad = adRepository.findById(ad.getId()).orElseThrow(() -> new RuntimeException("Ad not found after save"));
        log.info("Retrieved managed ad with ID: {}", ad.getId());
        
        // Generate AI content
        List<AdContent> contents = aiContentService.generateAdContent(ad, prompt, mediaFile, textProvider, imageProvider, numberOfVariations, language, adLinks, promptStyle, customPrompt, extractedContent, callToAction);
        // Nếu có contents được tạo, copy nội dung đầu tiên vào ad
        if (!contents.isEmpty()) {
            AdContent firstContent = contents.get(0);
            ad.setHeadline(firstContent.getHeadline());
            ad.setPrimaryText(firstContent.getPrimaryText());
            ad.setDescription(firstContent.getDescription());
            ad.setCallToAction(firstContent.getCallToAction());
            ad.setImageUrl(firstContent.getImageUrl());
            ad.setStatus("READY");
            
            // Mark first content as selected
            firstContent.setIsSelected(true);
            adContentRepository.save(firstContent);
            
            // Save updated ad
            ad = adRepository.save(ad);
        }
        
        // Khi tạo AdContent, nếu imageUrl rỗng thì set từ mediaFileUrl
        log.info("Setting imageUrl for {} contents, mediaFileUrl: {}", contents.size(), mediaFileUrl);
        for (AdContent content : contents) {
            if ((content.getImageUrl() == null || content.getImageUrl().isEmpty()) && mediaFileUrl != null && !mediaFileUrl.isEmpty()) {
                content.setImageUrl(mediaFileUrl);
                log.info("Set imageUrl for content '{}' to: {}", content.getHeadline(), mediaFileUrl);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("ad", ad);
        result.put("contents", contents);
        
        return result;
    }

    /**
     * Select ad content
     * @param adId The ad ID
     * @param contentId The content ID
     * @param userId The user ID
     * @return The updated ad
     */
    @Transactional
    public Ad selectAdContent(Long adId, Long contentId, Long userId) {
        log.info("Selecting content {} for ad {} by user ID: {}", contentId, adId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Ad ad = adRepository.findByIdAndUser(adId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found"));
        
        AdContent selectedContent = adContentRepository.findByIdAndUser(contentId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad content not found"));
        // Update ad with selected content
        ad.setHeadline(selectedContent.getHeadline());
        ad.setPrimaryText(selectedContent.getPrimaryText());
        ad.setDescription(selectedContent.getDescription());
        ad.setCallToAction(selectedContent.getCallToAction());
        ad.setImageUrl(selectedContent.getImageUrl());
        ad.setSelectedContentId(contentId);
        ad.setStatus("READY");
        ad.setUpdatedBy(user.getId().toString());
        ad.setUpdatedAt(LocalDateTime.now());
        
        // Mark selected content
        selectedContent.setIsSelected(true);
        adContentRepository.save(selectedContent);
        return adRepository.save(ad);
    }

    /**
     * Get ad contents
     * @param adId The ad ID
     * @param userId The user ID
     * @return List of ad contents
     */
    public List<AdContent> getAdContents(Long adId, Long userId) {
        log.info("Getting contents for ad {} by user ID: {}", adId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Ad ad = adRepository.findByIdAndUser(adId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found"));
        
        return adContentRepository.findByAdAndUserOrderByPreviewOrder(ad, user);
    }

    /**
     * Delete an ad
     * @param adId The ad ID
     * @param userId The user ID
     */
    @Transactional
    public void deleteAd(Long adId, Long userId) {
        log.info("Deleting ad: {} for user ID: {}", adId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Ad ad = adRepository.findByIdAndUser(adId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found"));
        
        log.info("Ad {} has selectedContentId: {}", adId, ad.getSelectedContentId());
        
        // Clear selected_content_id reference first to avoid foreign key constraint violation
        if (ad.getSelectedContentId() != null) {
            ad.setSelectedContentId(null);
            adRepository.save(ad);
            log.info("Cleared selected_content_id reference for ad: {}", adId);
            // Verify the update
            Ad updatedAd = adRepository.findById(adId).orElse(null);
            log.info("After clear, ad {} has selectedContentId: {}", adId, updatedAd != null ? updatedAd.getSelectedContentId() : "null");
        }
        
        // Check if there are other ads referencing the same content
        List<Ad> adsWithSameContent = adRepository.findBySelectedContentId(ad.getSelectedContentId());
        if (!adsWithSameContent.isEmpty()) {
            log.warn("Found {} other ads referencing the same content: {}", adsWithSameContent.size(), adsWithSameContent.stream().map(Ad::getId).collect(Collectors.toList()));
            // Clear all references to avoid foreign key constraint violation
            for (Ad otherAd : adsWithSameContent) {
                otherAd.setSelectedContentId(null);
                adRepository.save(otherAd);
                log.info("Cleared selectedContentId for ad: {}", otherAd.getId());
            }
        }
        
        // Then, delete all ad contents
        List<AdContent> adContents = adContentRepository.findByAdAndUserOrderByPreviewOrder(ad, user);
        if (!adContents.isEmpty()) {
            adContentRepository.deleteAll(adContents);
            log.info("Deleted {} ad contents for ad: {}", adContents.size(), adId);
        }
        
        // Finally, delete the ad
        adRepository.delete(ad);
        log.info("Successfully deleted ad: {} for user ID: {}", adId, userId);
    }

    /**
     * Update an ad
     * @param adId The ad ID
     * @param updatedAd The updated ad object
     * @param userId The user ID
     * @return The updated ad
     */
    @Transactional
    public Ad updateAd(Long adId, Ad updatedAd, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
            Ad ad = adRepository.findByIdAndUser(adId, user)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found"));

            ad.setHeadline(updatedAd.getHeadline());
            ad.setPrimaryText(updatedAd.getPrimaryText());
            ad.setDescription(updatedAd.getDescription());
            ad.setCallToAction(updatedAd.getCallToAction());
            ad.setImageUrl(updatedAd.getImageUrl());
            ad.setUpdatedBy(user.getId().toString());
            ad.setUpdatedAt(LocalDateTime.now());

            return adRepository.save(ad);
        } catch (Exception e) {
            log.error("[updateAd] Error updating ad {}: {}", adId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Save uploaded media file
     * @param mediaFile The media file
     * @return The file path
     */
    private String saveMediaFile(MultipartFile mediaFile) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + mediaFile.getOriginalFilename();
            Path uploadPath = Paths.get(imageStorageLocation);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(mediaFile.getInputStream(), filePath);
            
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save media file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save media file");
        }
    }

    public Page<AdResponse> getAllAdResponsesByUserPaginated(Long userId, Pageable pageable) {
        log.info("Getting ads for user ID: {} with pageable: {}", userId, pageable);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return adRepository.findAllByUser(user, pageable).map(ad -> {
            try {
                String campaignName = (ad.getCampaign() != null && ad.getCampaign().getName() != null)
                        ? ad.getCampaign().getName()
                        : "Unknown";

                return AdResponse.builder()
                        .id(ad.getId())
                        .name(ad.getName())
                        .adType(ad.getAdType() != null ? ad.getAdType().toString() : null)
                        .status(ad.getStatus())
                        .campaignName(campaignName)
                        .imageUrl(ad.getImageUrl())
                        .videoUrl(ad.getVideoUrl())
                        .headline(ad.getHeadline())
                        .description(ad.getDescription())
                        .primaryText(ad.getPrimaryText())
                        .callToAction(ad.getCallToAction() != null ? ad.getCallToAction().name() : null)
                        .createdDate(ad.getCreatedDate())
                        .build();
            } catch (Exception e) {
                log.error("Error mapping ad {}: {}", ad.getId(), e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Generate preview content without saving to database
     * @param tempAd Temporary ad object for content generation
     * @param prompt The prompt for AI generation
     * @param mediaFile The media file (optional)
     * @param textProvider The text AI provider
     * @param imageProvider The image AI provider
     * @param numberOfVariations Number of variations to generate
     * @param language The language for generation
     * @param adLinks List of ad links
     * @param promptStyle The prompt style
     * @param customPrompt Custom prompt addition
     * @param extractedContent Extracted content from Meta Ad Library
     * @return List of generated content (not saved to database)
     */
    public List<AdContent> generatePreviewContent(Ad tempAd, String prompt, MultipartFile mediaFile, 
                                                 String textProvider, String imageProvider, Integer numberOfVariations, 
                                                 String language, List<String> adLinks, String promptStyle, 
                                                 String customPrompt, String extractedContent, String mediaFileUrl,
                                                 com.fbadsautomation.model.FacebookCTA callToAction, String websiteUrl, List<AdGenerationRequest.LeadFormQuestion> leadFormQuestions) {
        log.info("Generating preview content for ad: {}", tempAd.getName());
        
        // Generate AI content without saving to database
        List<AdContent> contents = aiContentService.generateAdContent(tempAd, prompt, mediaFile, textProvider, 
                                                                     imageProvider, numberOfVariations, language, 
                                                                     adLinks, promptStyle, customPrompt, extractedContent, callToAction);
        // Set temporary IDs for preview
        for (int i = 0; i < contents.size(); i++) {
            AdContent content = contents.get(i);
            content.setId((long) (i + 1)); // Temporary ID for preview
            content.setPreviewOrder(i + 1);
            content.setIsSelected(false);
        }
        
        // Khi tạo AdContent, nếu imageUrl rỗng thì set từ mediaFileUrl
        log.info("Setting imageUrl for {} contents in generatePreviewContent, mediaFileUrl: {}", contents.size(), mediaFileUrl);
        for (AdContent content : contents) {
            if ((content.getImageUrl() == null || content.getImageUrl().isEmpty()) && mediaFileUrl != null && !mediaFileUrl.isEmpty()) {
                content.setImageUrl(mediaFileUrl);
                log.info("Set imageUrl for preview content '{}' to: {}", content.getHeadline(), mediaFileUrl);
            }
        }
        
        return contents;
    }
    
    /**
     * Save ad contents to database
     * @param contents List of ad contents to save
     * @return Saved contents
     */
    @Transactional
    public List<AdContent> saveAdContents(List<AdContent> contents) {
        log.info("Saving {} ad contents to database", contents.size());
        return adContentRepository.saveAll(contents);
    }

    /**
     * Create ad with existing content (without regenerating)
     * @param campaignId The campaign ID
     * @param adType The ad type
     * @param prompt The prompt
     * @param name The ad name
     * @param mediaFile The media file (optional)
     * @param userId The user ID
     * @param selectedVariation The selected variation content
     * @param adLinks List of ad links
     * @param promptStyle The prompt style
     * @param customPrompt Custom prompt addition
     * @return Map containing ad and contents
     */
    @Transactional
    public Map<String, Object> createAdWithExistingContent(Long campaignId, String adType, String prompt, 
                                                          String name, MultipartFile mediaFile, Long userId,
                                                          com.fbadsautomation.dto.AdVariation selectedVariation,
                                                          List<String> adLinks, String promptStyle, String customPrompt, String mediaFileUrl) {
        log.info("Creating ad with existing content for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Campaign campaign = campaignRepository.findByIdAndUser(campaignId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        
        // Create ad
        Ad ad = new Ad();
        ad.setName(name);
        ad.setAdType(com.fbadsautomation.model.AdType.valueOf(adType));
        ad.setPrompt(prompt);
        ad.setCampaign(campaign);
        ad.setUser(user);
        ad.setStatus("READY");
        ad.setCreatedBy(user.getId().toString());
        ad.setCreatedDate(LocalDateTime.now());
        
        // Save media file if provided
        if (mediaFile != null && !mediaFile.isEmpty()) {
            String mediaPath = saveMediaFile(mediaFile);
            ad.setImageUrl(mediaPath);
        } else if (mediaFileUrl != null && !mediaFileUrl.isEmpty()) {
            ad.setImageUrl(mediaFileUrl);
        }
        
        // Lưu ad trước để đảm bảo có ID
        ad = adRepository.save(ad);
        log.info("Saved ad with ID: {}", ad.getId());
        
        // Tạo AdContent từ selectedVariation
        AdContent content = new AdContent();
        content.setAd(ad);
        content.setUser(user);
        content.setHeadline(selectedVariation.getHeadline());
        content.setDescription(selectedVariation.getDescription());
        content.setPrimaryText(selectedVariation.getPrimaryText());
        content.setCallToAction(selectedVariation.getCallToAction() != null ? com.fbadsautomation.model.FacebookCTA.valueOf(selectedVariation.getCallToAction()) : null);
        // Set imageUrl from selectedVariation or mediaFileUrl if available
        String contentImageUrl = selectedVariation.getImageUrl();
        if ((contentImageUrl == null || contentImageUrl.isEmpty()) && mediaFileUrl != null && !mediaFileUrl.isEmpty()) {
            contentImageUrl = mediaFileUrl;
        }
        content.setImageUrl(contentImageUrl);
        content.setPreviewOrder(1);
        content.setIsSelected(true);
        content.setCreatedDate(LocalDateTime.now());
        
        // Save content
        content = adContentRepository.save(content);
        // Copy content to ad
        ad.setHeadline(content.getHeadline());
        ad.setPrimaryText(content.getPrimaryText());
        ad.setDescription(content.getDescription());
        ad.setCallToAction(content.getCallToAction());
        ad.setImageUrl(content.getImageUrl());
        
        // Save updated ad
        ad = adRepository.save(ad);
        Map<String, Object> result = new HashMap<>();
        result.put("ad", ad);
        result.put("contents", List.of(content));
        
        return result;
    }

    private AIProvider getProvider(String providerName) {
        return aiProviders.stream()
                .filter(p -> p.getName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid AI provider: " + providerName));
    }

    private Ad createAdFromRequest(AdGenerationRequest request, User user, Campaign campaign) {
        Ad ad = new Ad();
        ad.setUser(user);
        ad.setCampaign(campaign);
        ad.setName(request.getName());
        ad.setPrompt(request.getPrompt());
        ad.setAdType(com.fbadsautomation.model.AdType.valueOf(request.getAdType()));
        ad.setCallToAction(request.getCallToAction()); // Gán CTA từ request
        ad.setMediaFilePath(request.getMediaFileUrl());
        ad.setCreatedDate(LocalDateTime.now());
        ad.setUpdatedAt(LocalDateTime.now());
        ad.setCreatedBy(user.getId().toString());
        ad.setUpdatedBy(user.getId().toString());
        ad.setStatus("DRAFT");
        return ad;
    }

    @Transactional
    public AdGenerationResponse generateAd(AdGenerationRequest request, Long userId) {
        log.info("Starting ad generation with request: {}", request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));

        AIProvider textProvider = getProvider(request.getTextProvider());
        List<AdContent> generatedContents = textProvider.generateAdContent(request.getPrompt(),
                request.getNumberOfVariations(),
                request.getLanguage(),
                request.getCallToAction() // Truyền CTA xuống provider
        );
        log.info("Generated {} ad contents from provider {}", generatedContents.size(), textProvider.getName());

        List<com.fbadsautomation.dto.AdVariation> variations = new ArrayList<>();
        String imageUrl = request.getMediaFileUrl();

        if (imageUrl == null || imageUrl.isEmpty()) {
            if (request.getImageProvider() != null && !request.getImageProvider().isEmpty()) {
                AIProvider imageProvider = getProvider(request.getImageProvider());
                imageUrl = imageProvider.generateImage(request.getPrompt());
                log.info("Generated image URL {} from provider {}", imageUrl, imageProvider.getName());
            }
        }

        for (int i = 0; i < generatedContents.size(); i++) {
            AdContent content = generatedContents.get(i);
            com.fbadsautomation.dto.AdVariation variation = new com.fbadsautomation.dto.AdVariation();
            variation.setId(UUID.randomUUID().toString()); // Temporary ID for frontend
            variation.setHeadline(content.getHeadline());
            variation.setPrimaryText(content.getPrimaryText());
            variation.setDescription(content.getDescription());
            variation.setCallToAction(content.getCallToAction() != null ? content.getCallToAction().name() : null);
            variation.setImageUrl(imageUrl);
            variations.add(variation);
        }

        // Chỉ khai báo 1 lần ở đây
        List<AdGenerationResponse.AdVariation> responseVariations = variations.stream().map(v -> {
            AdGenerationResponse.AdVariation rv = new AdGenerationResponse.AdVariation();
            try {
                rv.setId(v.getId() != null ? Long.parseLong(v.getId()) : null);
            } catch (NumberFormatException e) {
                rv.setId(null);
            }
            rv.setHeadline(v.getHeadline());
            rv.setPrimaryText(v.getPrimaryText());
            rv.setDescription(v.getDescription());
            rv.setCallToAction(v.getCallToAction());
            rv.setImageUrl(v.getImageUrl());
            rv.setOrder(null);
            return rv;
        }).collect(Collectors.toList());

        if (request.getIsPreview() != null && request.getIsPreview()) {
            return AdGenerationResponse.builder()
                    .status("success")
                    .variations(responseVariations)
                    .build();
        }

        Ad ad = createAdFromRequest(request, user, campaign);
        ad = adRepository.save(ad);
        log.info("Saved new ad with ID: {}", ad.getId());

        for (AdContent content : generatedContents) {
            content.setAd(ad);
            content.setUser(user);
            content.setImageUrl(imageUrl);
            content.setIsSelected(false);
            adContentRepository.save(content);
        }
        log.info("Saved {} ad contents for ad ID: {}", generatedContents.size(), ad.getId());

        return AdGenerationResponse.builder()
                .status("success")
                .adId(ad.getId())
                .variations(responseVariations)
                .build();
    }
}


