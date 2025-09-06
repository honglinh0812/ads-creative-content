package com.fbadsautomation.service;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Campaign;
// Corrected import: Use the AIContentService that returns List<AdContent>
import com.fbadsautomation.service.AIContentService;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdContentService {

    private static final Logger log = LoggerFactory.getLogger(AdContentService.class);

    private final AdRepository adRepository;
    private final AdContentRepository adContentRepository;
    private final CampaignRepository campaignRepository;
    private final AIContentService aiContentService; // Service layer for AI interaction
    private static final int DEFAULT_VARIATIONS_FOR_AD = 1; // Default variations when generating for an ad

    @Autowired
    public AdContentService(AdRepository adRepository, AdContentRepository adContentRepository, 
                           CampaignRepository campaignRepository, AIContentService aiContentService) {
        this.adRepository = adRepository;
        this.adContentRepository = adContentRepository;
        this.campaignRepository = campaignRepository;
        this.aiContentService = aiContentService;
    }

    /**
     * Generate content variations for an ad using AI.
     * Saves all generated variations and associates them with the ad.
     *
     * @param campaignId The campaign ID
     * @param adId The ad ID
     * @param prompt The prompt to generate content from
     * @param provider The AI provider to use (optional, defaults if null/empty)
     * @param numberOfVariations Number of variations to generate
     * @return List of generated and saved AdContent variations
     */
    @Transactional
    // Corrected return type to List<AdContent>
    public List<AdContent> generateContent(Long campaignId, Long adId, String prompt, String provider, int numberOfVariations) {
        log.info("Generating {} content variations for ad: {} in campaign: {} with prompt: {}", numberOfVariations, adId, campaignId, prompt);

        // Verify campaign exists
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        // Verify ad exists and belongs to campaign
        Ad ad = adRepository.findByIdAndCampaignId(adId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found in campaign"));

        try {
            // Generate content variations using AI service
            List<AdContent> generatedContents;
            int variationsToGenerate = numberOfVariations > 0 ? numberOfVariations : DEFAULT_VARIATIONS_FOR_AD;
            // Assuming AdContent.ContentType.COMBINED is appropriate when generating full ad content
            AdContent.ContentType contentType = AdContent.ContentType.COMBINED; 

            if (provider != null && !provider.trim().isEmpty()) {
                generatedContents = aiContentService.generateContent(prompt, contentType, provider, variationsToGenerate);
            } else {
                // If no provider specified, use the default logic in AIContentService (which might pick the first available)
                generatedContents = aiContentService.generateContent(prompt, contentType, null, variationsToGenerate); 
            }

            if (generatedContents == null || generatedContents.isEmpty()) {
                log.warn("AI service returned no content for ad: {}", adId);
                return new ArrayList<>(); // Return empty list if nothing was generated
            }

            // Associate content with ad and save each variation
            List<AdContent> savedContents = new ArrayList<>();
            for (AdContent content : generatedContents) {
                content.setAd(ad);
                content.setIsSelected(false); // None are selected initially
                savedContents.add(adContentRepository.save(content));
            }

            log.info("Successfully generated and saved {} content variations for ad: {}", savedContents.size(), adId);
            return savedContents;
        } catch (ApiException ae) {
            log.error("API Error generating content for ad {}: {}", adId, ae.getMessage(), ae);
            throw ae; // Re-throw API exception
        } catch (Exception e) {
            log.error("Error generating content for ad {}: {}", adId, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate content: " + e.getMessage());
        }
    }

    /**
     * Select content for an ad
     * @param campaignId The campaign ID
     * @param adId The ad ID
     * @param contentId The content ID to select
     * @return The selected ad content
     */
    @Transactional
    public AdContent selectContent(Long campaignId, Long adId, Long contentId) {
        log.info("Selecting content: {} for ad: {} in campaign: {}", contentId, adId, campaignId);

        // Verify campaign exists
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        // Verify ad exists and belongs to campaign
        Ad ad = adRepository.findByIdAndCampaignId(adId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found in campaign"));

        // Verify content exists and belongs to ad
        AdContent contentToSelect = adContentRepository.findByIdAndAdId(contentId, adId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Content not found in ad"));
        // Deselect all other content for this ad
        List<AdContent> allContents = adContentRepository.findByAdId(adId);
        for (AdContent otherContent : allContents) {
            if (!otherContent.getId().equals(contentId) && otherContent.getIsSelected()) {
                otherContent.setIsSelected(false);
                adContentRepository.save(otherContent);
            }
        }

        // Select the requested content
        contentToSelect.setIsSelected(true);

        // Update the parent Ad object with the selected content details
        // This assumes the Ad object itself stores the currently active/selected content details
        ad.setHeadline(contentToSelect.getHeadline());
        ad.setPrimaryText(contentToSelect.getPrimaryText());
        ad.setDescription(contentToSelect.getDescription());
        ad.setCallToAction(contentToSelect.getCallToAction()); // Corrected from setCta to setCallToAction
        ad.setImageUrl(contentToSelect.getImageUrl()); // Update image URL as well
        adRepository.save(ad);

        // Save and return the selected content object
        return adContentRepository.save(contentToSelect);
    }

    /**
     * Get all content for an ad
     * @param campaignId The campaign ID
     * @param adId The ad ID
     * @return List of ad content
     */
    public List<AdContent> getAllContentForAd(Long campaignId, Long adId) {
        log.info("Getting all content for ad: {} in campaign: {}", adId, campaignId);

        // Verify campaign exists
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        // Verify ad exists and belongs to campaign
        Ad ad = adRepository.findByIdAndCampaignId(adId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ad not found in campaign"));

        return adContentRepository.findByAdId(adId);
    }
}
