package com.fbadsautomation.service;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookExportService {
    
    private final AdRepository adRepository;
    private final CampaignService campaignService;
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z]{2,}(/.*)?$"
    );
    
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
    
    /**
     * Validate ad content for Facebook export
     */
    private void validateAdContentForFacebook(Ad ad) {
        log.info("Validating ad content for Facebook export: {}", ad.getId());
        
        // Check if ad has selected content
        if (ad.getSelectedContentId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Ad content must be selected before exporting to Facebook");
        }
        
        // Check if ad status is ready
        if (!"READY".equals(ad.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Ad must be in READY status to export to Facebook");
        }
        
        // Validate required content fields
        if (!StringUtils.hasText(ad.getHeadline())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Headline is required for Facebook export");
        }
        
        if (!StringUtils.hasText(ad.getPrimaryText())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Primary text is required for Facebook export");
        }
        
        // Validate headline length (Facebook limit: 40 characters)
        if (ad.getHeadline().length() > 40) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Headline must be 40 characters or less for Facebook");
        }
        
        // Validate primary text length (Facebook limit: 125 characters)
        if (ad.getPrimaryText().length() > 12500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Primary text must be 12500 characters or less for Facebook");
        }
        
        // Validate description length if present (Facebook limit: 30 characters)
        if (StringUtils.hasText(ad.getDescription()) && ad.getDescription().length() > 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Description must be 30 characters or less for Facebook");
        }
        
        // Validate ad type specific requirements
        validateAdTypeSpecificRequirements(ad);
        
        // Validate media requirements
        validateMediaRequirements(ad);
        
        // Validate campaign is ready for Facebook
        campaignService.isCampaignReadyForFacebookExport(ad.getCampaign().getId(), ad.getCampaign().getUser().getId());
    }
    
    /**
     * Validate ad type specific requirements
     */
    private void validateAdTypeSpecificRequirements(Ad ad) {
        if (ad.getAdType() == AdType.WEBSITE_CONVERSION_AD) {
            if (!StringUtils.hasText(ad.getWebsiteUrl())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Website URL is required for Website Conversion Ad");
            }
            if (!isValidUrl(ad.getWebsiteUrl())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Invalid website URL format");
            }
        }
        
        if (ad.getAdType() == AdType.LEAD_FORM_AD) {
            if (!StringUtils.hasText(ad.getLeadFormQuestions())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Lead form questions are required for Lead Form Ad");
            }
        }
        
        // Validate Call to Action is set
        if (ad.getCallToAction() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Call to Action is required for Facebook export");
        }
    }
    
    /**
     * Validate media files according to Facebook requirements
     */
    private void validateMediaRequirements(Ad ad) {
        boolean hasImage = StringUtils.hasText(ad.getImageUrl());
        boolean hasVideo = StringUtils.hasText(ad.getVideoUrl());
        
        if (!hasImage && !hasVideo) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "At least one media file (image or video) is required for Facebook export");
        }
        
        // Validate image requirements
        if (hasImage) {
            validateImageRequirements(ad.getImageUrl());
        }
        
        // Validate video requirements
        if (hasVideo) {
            validateVideoRequirements(ad.getVideoUrl());
        }
    }
    
    /**
     * Validate image requirements for Facebook
     */
    private void validateImageRequirements(String imageUrl) {
        if (!isValidUrl(imageUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Invalid image URL format");
        }
        
        // Check if it's a local file path or URL
        if (imageUrl.startsWith("/") || imageUrl.startsWith("uploads/")) {
            validateLocalImageFile(imageUrl);
        } else {
            validateRemoteImageUrl(imageUrl);
        }
    }
    
    /**
     * Validate video requirements for Facebook
     */
    private void validateVideoRequirements(String videoUrl) {
        if (!isValidUrl(videoUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Invalid video URL format");
        }
        
        // Check if it's a local file path or URL
        if (videoUrl.startsWith("/") || videoUrl.startsWith("uploads/")) {
            validateLocalVideoFile(videoUrl);
        } else {
            validateRemoteVideoUrl(videoUrl);
        }
    }
    
    /**
     * Validate local image file
     */
    private void validateLocalImageFile(String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Image file not found: " + imagePath);
            }
            
            // Check file size (Facebook limit: 30MB for images)
            long fileSize = Files.size(path);
            long maxSizeBytes = 30 * 1024 * 1024; // 30MB
            if (fileSize > maxSizeBytes) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Image file size exceeds Facebook limit of 30MB");
            }
            
            // Check file format
            String fileName = path.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && 
                !fileName.endsWith(".png") && !fileName.endsWith(".gif")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Image format not supported. Facebook accepts: JPG, JPEG, PNG, GIF");
            }
            
            // Check image dimensions using ImageIO
            validateImageDimensions(path);
            
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Error validating image file: " + e.getMessage());
        }
    }
    
    /**
     * Validate local video file
     */
    private void validateLocalVideoFile(String videoPath) {
        try {
            Path path = Paths.get(videoPath);
            if (!Files.exists(path)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Video file not found: " + videoPath);
            }
            
            // Check file size (Facebook limit: 4GB for videos)
            long fileSize = Files.size(path);
            long maxSizeBytes = 4L * 1024 * 1024 * 1024; // 4GB
            if (fileSize > maxSizeBytes) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Video file size exceeds Facebook limit of 4GB");
            }
            
            // Check file format
            String fileName = path.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".mp4") && !fileName.endsWith(".mov") && 
                !fileName.endsWith(".avi") && !fileName.endsWith(".mkv") &&
                !fileName.endsWith(".webm") && !fileName.endsWith(".flv")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Video format not supported. Facebook accepts: MP4, MOV, AVI, MKV, WEBM, FLV");
            }
            
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Error validating video file: " + e.getMessage());
        }
    }
    
    /**
     * Validate remote image URL
     */
    private void validateRemoteImageUrl(String imageUrl) {
        // Check URL format
        if (!imageUrl.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)(\\?.*)?$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Remote image URL must end with supported format: .jpg, .jpeg, .png, .gif");
        }
        
        // Check if URL is accessible (optional - can be resource intensive)
        // validateUrlAccessibility(imageUrl);
    }
    
    /**
     * Validate remote video URL
     */
    private void validateRemoteVideoUrl(String videoUrl) {
        // Check URL format
        if (!videoUrl.toLowerCase().matches(".*\\.(mp4|mov|avi|mkv|webm|flv)(\\?.*)?$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Remote video URL must end with supported format: .mp4, .mov, .avi, .mkv, .webm, .flv");
        }
        
        // Check if URL is accessible (optional - can be resource intensive)
        // validateUrlAccessibility(videoUrl);
    }
    
    /**
     * Validate image dimensions
     */
    private void validateImageDimensions(Path imagePath) {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Unable to read image file or unsupported format");
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Facebook minimum dimensions: 600x600 pixels
            if (width < 600 || height < 600) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Image dimensions too small. Facebook requires minimum 600x600 pixels");
            }
            
            // Facebook maximum dimensions: 1936x1936 pixels for square images
            if (width > 1936 || height > 1936) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Image dimensions too large. Facebook recommends maximum 1936x1936 pixels");
            }
            
            // Check aspect ratio (Facebook recommends 1:1 for most ad types)
            double aspectRatio = (double) width / height;
            if (aspectRatio < 0.8 || aspectRatio > 1.91) {
                log.warn("Image aspect ratio {} may not be optimal for Facebook ads. Recommended: 1:1 to 1.91:1", aspectRatio);
            }
            
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Error reading image dimensions: " + e.getMessage());
        }
    }
    
    /**
     * Optional: Validate URL accessibility (can be resource intensive)
     */
    private void validateUrlAccessibility(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "Media URL is not accessible: " + url);
            }
            
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Unable to verify media URL accessibility: " + url);
        }
    }
    
    /**
     * URL format
     */
    private boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }
    
    public ResponseEntity<byte[]> exportAdToFacebookTemplate(Long adId) {
        try {
            Optional<Ad> adOpt = adRepository.findById(adId);
            if (adOpt.isPresent()) {
                Ad ad = adOpt.get();
                
                // Validate ad content before export
                validateAdContentForFacebook(ad);
                
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
            } else {
                throw new ApiException(HttpStatus.NOT_FOUND, "Ad not found with id: " + adId);
            }
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
            case PAGE_POST_AD: return "SINGLE_IMAGE";
            case LEAD_FORM_AD: return "LEAD_GENERATION";
            case WEBSITE_CONVERSION_AD: return "SINGLE_IMAGE";
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
            case BOOK_NOW: return "BOOK_TRAVEL";
            case CONTACT_US: return "CONTACT_US";
            case APPLY_NOW: return "APPLY_NOW";
            case GET_OFFER: return "GET_QUOTE";
            case SUBSCRIBE: return "SUBSCRIBE";
            default: return "LEARN_MORE";
        }
    }
    
    /**
     * Preview Facebook format for ad before export
     */
    public Map<String, Object> previewFacebookFormat(Long adId) {
        log.info("Previewing Facebook format for ad: {}", adId);
        
        Optional<Ad> adOpt = adRepository.findById(adId);
        if (!adOpt.isPresent()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Ad not found");
        }
        
        Ad ad = adOpt.get();
        Campaign campaign = ad.getCampaign();
        
        // Validate ad content for Facebook (this will throw exceptions if invalid)
        validateAdContentForFacebook(ad);
        
        // Create preview data structure
        Map<String, Object> preview = new HashMap<>();
        
        // Campaign information
        Map<String, Object> campaignInfo = new HashMap<>();
        campaignInfo.put("name", campaign.getName());
        campaignInfo.put("objective", mapCampaignObjective(campaign.getObjective()));
        campaignInfo.put("budgetType", mapBudgetType(campaign.getBudgetType()));
        campaignInfo.put("dailyBudget", formatBudget(campaign.getDailyBudget()));
        campaignInfo.put("lifetimeBudget", formatBudget(campaign.getTotalBudget()));
        campaignInfo.put("startDate", formatDate(campaign.getStartDate()));
        campaignInfo.put("endDate", formatDate(campaign.getEndDate()));
        campaignInfo.put("targetAudience", campaign.getTargetAudience());
        preview.put("campaign", campaignInfo);
        
        // Ad Set information (using campaign data as base)
        Map<String, Object> adSetInfo = new HashMap<>();
        adSetInfo.put("name", campaign.getName() + " - Ad Set");
        adSetInfo.put("budget", formatBudget(campaign.getDailyBudget()));
        adSetInfo.put("targetAudience", campaign.getTargetAudience());
        preview.put("adSet", adSetInfo);
        
        // Ad information
        Map<String, Object> adInfo = new HashMap<>();
        adInfo.put("name", ad.getName());
        adInfo.put("type", mapAdType(ad.getAdType()));
        adInfo.put("headline", ad.getHeadline());
        adInfo.put("primaryText", ad.getPrimaryText());
        adInfo.put("description", ad.getDescription());
        adInfo.put("callToAction", mapCallToAction(ad.getCallToAction()));
        adInfo.put("websiteUrl", ad.getWebsiteUrl());
        adInfo.put("imageUrl", ad.getImageUrl());
        adInfo.put("videoUrl", ad.getVideoUrl());
        adInfo.put("status", "Active");
        preview.put("ad", adInfo);
        
        // Character count validation info
        Map<String, Object> validation = new HashMap<>();
        validation.put("headlineLength", ad.getHeadline().length());
        validation.put("headlineLimit", 40);
        validation.put("primaryTextLength", ad.getPrimaryText().length());
        validation.put("primaryTextLimit", 125);
        if (StringUtils.hasText(ad.getDescription())) {
            validation.put("descriptionLength", ad.getDescription().length());
            validation.put("descriptionLimit", 30);
        }
        preview.put("validation", validation);
        
        // Facebook format preview (as it would appear in CSV)
        List<String> csvRow = Arrays.asList(
            campaign.getName(),
            mapCampaignObjective(campaign.getObjective()),
            mapBudgetType(campaign.getBudgetType()),
            formatBudget(campaign.getDailyBudget()),
            formatBudget(campaign.getTotalBudget()),
            formatDate(campaign.getStartDate()),
            formatDate(campaign.getEndDate()),
            campaign.getName() + " - Ad Set",
            formatBudget(campaign.getDailyBudget()),
            campaign.getTargetAudience(),
            ad.getName(),
            mapAdType(ad.getAdType()),
            ad.getHeadline(),
            ad.getPrimaryText(),
            ad.getDescription(),
            mapCallToAction(ad.getCallToAction()),
            ad.getWebsiteUrl(),
            ad.getImageUrl(),
            ad.getVideoUrl(),
            "Active"
        );
        preview.put("csvPreview", csvRow);
        preview.put("csvHeaders", Arrays.asList(CSV_HEADERS));
        
        return preview;
    }
    
    /**
     * Preview Facebook format for multiple ads
     */
    public Map<String, Object> previewMultipleFacebookFormat(List<Long> adIds) {
        log.info("Previewing Facebook format for {} ads", adIds.size());
        
        List<Map<String, Object>> adPreviews = new ArrayList<>();
        List<List<String>> csvRows = new ArrayList<>();
        
        for (Long adId : adIds) {
            Optional<Ad> adOpt = adRepository.findById(adId);
            if (!adOpt.isPresent()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Ad not found: " + adId);
            }
            
            Ad ad = adOpt.get();
            Campaign campaign = ad.getCampaign();
            
            // Validate each ad
            validateAdContentForFacebook(ad);
            
            // Create individual preview
            Map<String, Object> adPreview = new HashMap<>();
            adPreview.put("adId", adId);
            adPreview.put("adName", ad.getName());
            adPreview.put("campaignName", campaign.getName());
            adPreview.put("headline", ad.getHeadline());
            adPreview.put("primaryText", ad.getPrimaryText());
            adPreview.put("validation", Map.of(
                "headlineLength", ad.getHeadline().length(),
                "primaryTextLength", ad.getPrimaryText().length()
            ));
            adPreviews.add(adPreview);
            
            // Create CSV row
            List<String> csvRow = Arrays.asList(
                campaign.getName(),
                mapCampaignObjective(campaign.getObjective()),
                mapBudgetType(campaign.getBudgetType()),
                formatBudget(campaign.getDailyBudget()),
                formatBudget(campaign.getTotalBudget()),
                formatDate(campaign.getStartDate()),
                formatDate(campaign.getEndDate()),
                campaign.getName() + " - Ad Set",
                formatBudget(campaign.getDailyBudget()),
                campaign.getTargetAudience(),
                ad.getName(),
                mapAdType(ad.getAdType()),
                ad.getHeadline(),
                ad.getPrimaryText(),
                ad.getDescription(),
                mapCallToAction(ad.getCallToAction()),
                ad.getWebsiteUrl(),
                ad.getImageUrl(),
                ad.getVideoUrl(),
                "Active"
            );
            csvRows.add(csvRow);
        }
        
        Map<String, Object> preview = new HashMap<>();
        preview.put("ads", adPreviews);
        preview.put("csvPreview", csvRows);
        preview.put("csvHeaders", Arrays.asList(CSV_HEADERS));
        preview.put("totalAds", adIds.size());
        
        return preview;
    }
}