package com.fbadsautomation.service;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookExportService {
    
    private final AdRepository adRepository;
    
    private static final String[] CSV_HEADERS = {
        "Campaign Name",
        "Campaign Objective", 
        "Campaign Budget Type",
        "Campaign Daily Budget",
        "Campaign Lifetime Budget",
        "Campaign Start Date",
        "Campaign End Date",
        "Ad Set Name",
        "Ad Set Budget",
        "Target Audience",
        "Ad Name",
        "Ad Type",
        "Headline",
        "Primary Text",
        "Description",
        "Call to Action",
        "Website URL",
        "Image URL",
        "Video URL",
        "Status"
    };
    
    public ResponseEntity<byte[]> exportAdToFacebookTemplate(Long adId) {
        try {
            Optional<Ad> adOptional = adRepository.findById(adId);
            if (!adOptional.isPresent()) {
                throw new RuntimeException("Ad not found with id: " + adId);
            }
            
            Ad ad = adOptional.get();
            Campaign campaign = ad.getCampaign();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            
            // Write BOM for Excel UTF-8 support
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            
            // Write CSV headers
            writer.write(String.join(",", CSV_HEADERS));
            writer.write("\n");
            
            // Write ad data
            String[] rowData = {
                escapeCsvValue(campaign.getName()),
                escapeCsvValue(mapCampaignObjective(campaign.getObjective())),
                escapeCsvValue(mapBudgetType(campaign.getBudgetType())),
                formatBudget(campaign.getDailyBudget()),
                formatBudget(campaign.getTotalBudget()),
                formatDate(campaign.getStartDate()),
                formatDate(campaign.getEndDate()),
                escapeCsvValue(ad.getName() + "_AdSet"),
                formatBudget(campaign.getDailyBudget()),
                escapeCsvValue(campaign.getTargetAudience()),
                escapeCsvValue(ad.getName()),
                escapeCsvValue(mapAdType(ad.getAdType())),
                escapeCsvValue(ad.getHeadline()),
                escapeCsvValue(ad.getPrimaryText()),
                escapeCsvValue(ad.getDescription()),
                escapeCsvValue(mapCallToAction(ad.getCallToAction())),
                escapeCsvValue(ad.getWebsiteUrl()),
                escapeCsvValue(ad.getImageUrl()),
                escapeCsvValue(ad.getVideoUrl()),
                escapeCsvValue("PAUSED")
            };
            
            writer.write(String.join(",", rowData));
            writer.write("\n");
            writer.flush();
            writer.close();
            
            byte[] csvData = outputStream.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", 
                "facebook_ads_template_" + ad.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".csv");
            headers.setContentLength(csvData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
                
        } catch (IOException e) {
            log.error("Error creating CSV export for ad {}: {}", adId, e.getMessage());
            throw new RuntimeException("Failed to create CSV export", e);
        }
    }
    
    public ResponseEntity<byte[]> exportMultipleAdsToFacebookTemplate(List<Long> adIds) {
        try {
            List<Ad> ads = adRepository.findAllById(adIds);
            if (ads.isEmpty()) {
                throw new RuntimeException("No ads found for the provided IDs");
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            
            // Write BOM for Excel UTF-8 support
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            
            // Write CSV headers
            writer.write(String.join(",", CSV_HEADERS));
            writer.write("\n");
            
            // Write data for each ad
            for (Ad ad : ads) {
                Campaign campaign = ad.getCampaign();
                
                String[] rowData = {
                    escapeCsvValue(campaign.getName()),
                    escapeCsvValue(mapCampaignObjective(campaign.getObjective())),
                    escapeCsvValue(mapBudgetType(campaign.getBudgetType())),
                    formatBudget(campaign.getDailyBudget()),
                    formatBudget(campaign.getTotalBudget()),
                    formatDate(campaign.getStartDate()),
                    formatDate(campaign.getEndDate()),
                    escapeCsvValue(ad.getName() + "_AdSet"),
                    formatBudget(campaign.getDailyBudget()),
                    escapeCsvValue(campaign.getTargetAudience()),
                    escapeCsvValue(ad.getName()),
                    escapeCsvValue(mapAdType(ad.getAdType())),
                    escapeCsvValue(ad.getHeadline()),
                    escapeCsvValue(ad.getPrimaryText()),
                    escapeCsvValue(ad.getDescription()),
                    escapeCsvValue(mapCallToAction(ad.getCallToAction())),
                    escapeCsvValue(ad.getWebsiteUrl()),
                    escapeCsvValue(ad.getImageUrl()),
                    escapeCsvValue(ad.getVideoUrl()),
                    escapeCsvValue("PAUSED")
                };
                
                writer.write(String.join(",", rowData));
                writer.write("\n");
            }
            
            writer.flush();
            writer.close();
            
            byte[] csvData = outputStream.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", 
                "facebook_ads_template_bulk_export.csv");
            headers.setContentLength(csvData.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
                
        } catch (IOException e) {
            log.error("Error creating bulk CSV export: {}", e.getMessage());
            throw new RuntimeException("Failed to create bulk CSV export", e);
        }
    }
    
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
    
    private String formatBudget(Double budget) {
        return budget != null ? String.valueOf(budget) : "";
    }
    
    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
    }
    
    private String mapCampaignObjective(Campaign.CampaignObjective objective) {
        if (objective == null) return "TRAFFIC";
        
        switch (objective) {
            case BRAND_AWARENESS: return "BRAND_AWARENESS";
            case REACH: return "REACH";
            case TRAFFIC: return "TRAFFIC";
            case ENGAGEMENT: return "ENGAGEMENT";
            case APP_INSTALLS: return "APP_INSTALLS";
            case VIDEO_VIEWS: return "VIDEO_VIEWS";
            case LEAD_GENERATION: return "LEAD_GENERATION";
            case CONVERSIONS: return "CONVERSIONS";
            case CATALOG_SALES: return "CATALOG_SALES";
            case STORE_TRAFFIC: return "STORE_TRAFFIC";
            default: return "TRAFFIC";
        }
    }
    
    private String mapBudgetType(Campaign.BudgetType budgetType) {
        if (budgetType == null) return "DAILY";
        
        switch (budgetType) {
            case DAILY: return "DAILY";
            case LIFETIME: return "LIFETIME";
            default: return "DAILY";
        }
    }
    
    private String mapAdType(AdType adType) {
        if (adType == null) return "SINGLE_IMAGE";
        
        switch (adType) {
            case PAGE_POST: return "SINGLE_IMAGE";
            case LEAD_FORM: return "LEAD_GENERATION";
            case WEBSITE_CONVERSION: return "SINGLE_IMAGE";
            default: return "SINGLE_IMAGE";
        }
    }
    
    private String mapCallToAction(com.fbadsautomation.model.FacebookCTA cta) {
        if (cta == null) return "LEARN_MORE";
        
        switch (cta) {
            case LEARN_MORE: return "LEARN_MORE";
            case SHOP_NOW: return "SHOP_NOW";
            case SIGN_UP: return "SIGN_UP";
            case DOWNLOAD: return "DOWNLOAD";
            case BOOK_TRAVEL: return "BOOK_TRAVEL";
            case CONTACT_US: return "CONTACT_US";
            case CALL_NOW: return "CALL_NOW";
            case APPLY_NOW: return "APPLY_NOW";
            case GET_QUOTE: return "GET_QUOTE";
            case SUBSCRIBE: return "SUBSCRIBE";
            default: return "LEARN_MORE";
        }
    }
}