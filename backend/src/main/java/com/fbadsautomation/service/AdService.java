package com.fbadsautomation.service;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import com.fbadsautomation.model.AdResponse;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.CampaignRepository;
import com.fbadsautomation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdService {

    private final AdRepository adRepository;
    private final CampaignRepository campaignRepository;
    private final AdContentRepository adContentRepository;
    private final UserRepository userRepository;
    private final AIContentService aiContentService;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStorageLocation;

    /**
     * Get all ads for a user
     * @param userId The user ID
     * @return List of ads
     */
    @Transactional(readOnly = true)
    public List<AdResponse> getAllAdResponsesByUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return adRepository.findByUserOrderByCreatedDateDesc(user).stream()
            .map(ad -> {
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
                            .createdDate(ad.getCreatedDate())
                            .build();
                } catch (Exception e) {
                    log.error("❌ Error mapping ad {}: {}", ad.getId(), e.getMessage(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

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
                                                     String name, MultipartFile mediaFile, Long userId, String textProvider, String imageProvider, Integer numberOfVariations, String language) {
        log.info("Creating ad with AI content for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        
        Campaign campaign = campaignRepository.findByIdAndUser(campaignId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        
        // Create ad
        Ad ad = new Ad();
        ad.setName(name);
        ad.setAdType(Ad.AdType.valueOf(adType.toUpperCase()));
        ad.setPrompt(prompt);
        ad.setCampaign(campaign);
        ad.setUser(user);
        ad.setStatus("GENERATING");
        ad.setCreatedBy(user.getId().toString());
        ad.setCreatedDate(LocalDateTime.now());
        
        // Save media file if provided
        if (mediaFile != null && !mediaFile.isEmpty()) {
            String mediaPath = saveMediaFile(mediaFile);
            ad.setImageUrl(mediaPath);
        }
        
        ad = adRepository.save(ad);
        
        // Generate AI content
        List<AdContent> contents = aiContentService.generateAdContent(ad, prompt, mediaFile, textProvider, imageProvider, numberOfVariations, language);
        
        // Set preview order for contents
        for (int i = 0; i < contents.size(); i++) {
            contents.get(i).setPreviewOrder(i + 1);
            contents.get(i).setUser(user);
            contents.get(i).setAd(ad);
        }
        
        contents = adContentRepository.saveAll(contents);
        
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
        ad.setUpdatedDate(LocalDateTime.now());
        
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
        
        adRepository.delete(ad);
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
}







