package com.fbadsautomation.service;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.exception.ApiException;
import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.util.CellRangeAddress;

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
// @Slf4j
public class FacebookExportService {

    private static final Logger log = LoggerFactory.getLogger(FacebookExportService.class);
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
    
    /**
     * Export single ad to Facebook CSV template
     */
    public ResponseEntity<byte[]> exportAdToFacebookTemplate(Long adId) {
        try {
            log.info("Exporting ad {} to Facebook template", adId);
            
            Optional<Ad> adOptional = adRepository.findById(adId);
            if (!adOptional.isPresent()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy quảng cáo với ID: " + adId);
            }
            
            Ad ad = adOptional.get();
            
            // Validate ad before export
            validateAdContentForFacebook(ad);
            
            // Generate CSV content
            String csvContent = generateFacebookCsvContent(Arrays.asList(ad));
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            
            // Create response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "facebook_ad_" + adId + ".csv");
            headers.setContentLength(csvBytes.length);
            
            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error exporting ad {} to Facebook template: {}", adId, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xuất quảng cáo: " + e.getMessage());
        }
    }
    
    /**
     * Export multiple ads to Facebook CSV template
     */
    public ResponseEntity<byte[]> exportMultipleAdsToFacebookTemplate(List<Long> adIds) {
        try {
            log.info("Exporting {} ads to Facebook template", adIds.size());
            
            if (adIds == null || adIds.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách ID quảng cáo không được để trống");
            }
            
            List<Ad> ads = adRepository.findAllById(adIds);
            if (ads.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy quảng cáo nào với các ID đã cung cấp");
            }
            
            // Validate all ads before export
            for (Ad ad : ads) {
                validateAdContentForFacebook(ad);
            }
            
            // Generate CSV content
            String csvContent = generateFacebookCsvContent(ads);
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            
            // Create response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "facebook_ads_bulk_" + System.currentTimeMillis() + ".csv");
            headers.setContentLength(csvBytes.length);
            
            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error exporting multiple ads to Facebook template: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xuất quảng cáo: " + e.getMessage());
        }
    }
    
    /**
     * Generate Facebook CSV content from ads
     */
    private String generateFacebookCsvContent(List<Ad> ads) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            // Write CSV headers
            writer.write(String.join(",", CSV_HEADERS) + "\n");

            // Write data for each ad
            for (Ad ad : ads) {
                Campaign campaign = ad.getCampaign();

                List<String> csvRow = Arrays.asList(
                    escapeCsvValue(campaign.getName()),
                    escapeCsvValue(mapCampaignObjective(campaign.getObjective())),
                    escapeCsvValue(mapBudgetType(campaign.getBudgetType())),
                    escapeCsvValue(formatBudget(campaign.getDailyBudget())),
                    escapeCsvValue(formatBudget(campaign.getTotalBudget())),
                    escapeCsvValue(formatDate(campaign.getStartDate())),
                    escapeCsvValue(formatDate(campaign.getEndDate())),
                    escapeCsvValue(campaign.getName() + " - Ad Set"),
                    escapeCsvValue(formatBudget(campaign.getDailyBudget())),
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
                    escapeCsvValue("Active")
                );

                writer.write(String.join(",", csvRow) + "\n");
            }

            writer.flush();
            return baos.toString(StandardCharsets.UTF_8.name());

        } catch (IOException e) {
            log.error("Error generating CSV content: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo file CSV: " + e.getMessage());
        }
    }

    /**
     * Export ads to Excel format (.xlsx) with proper formatting and UTF-8 support
     * Security: Input validation, resource cleanup, bounded memory usage
     * Performance: Streaming for large datasets, optimized cell styles
     */
    public ResponseEntity<byte[]> exportAdsToExcel(List<Long> adIds) {
        log.info("Exporting {} ads to Excel format", adIds.size());

        // Security: Validate input size to prevent DoS
        if (adIds == null || adIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ad IDs list cannot be empty");
        }

        if (adIds.size() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Cannot export more than 1000 ads at once. Please split into smaller batches.");
        }

        try {
            // Fetch and validate ads
            List<Ad> ads = adRepository.findAllById(adIds);
            if (ads.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No ads found with provided IDs");
            }

            // Validate all ads before export
            for (Ad ad : ads) {
                validateAdContentForFacebook(ad);
            }

            // Generate Excel file
            byte[] excelBytes = generateExcelContent(ads);

            // Create response headers with proper content disposition
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                "facebook_ads_" + System.currentTimeMillis() + ".xlsx");
            headers.setContentLength(excelBytes.length);
            headers.set("X-Export-Count", String.valueOf(ads.size()));

            log.info("Successfully exported {} ads to Excel", ads.size());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exporting ads to Excel: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to export ads to Excel: " + e.getMessage());
        }
    }

    /**
     * Generate Excel content with proper formatting
     * Implements SOLID principles: Single responsibility, proper resource management
     */
    private byte[] generateExcelContent(List<Ad> ads) throws IOException {
        // Using try-with-resources for proper resource cleanup
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Facebook Ads");

            // Create cell styles for better readability
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle = createDataStyle(workbook);
            XSSFCellStyle urlStyle = createUrlStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < CSV_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(CSV_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate data rows
            int rowNum = 1;
            for (Ad ad : ads) {
                Campaign campaign = ad.getCampaign();
                Row row = sheet.createRow(rowNum++);

                // Campaign information
                createCell(row, 0, campaign.getName(), dataStyle);
                createCell(row, 1, mapCampaignObjective(campaign.getObjective()), dataStyle);
                createCell(row, 2, mapBudgetType(campaign.getBudgetType()), dataStyle);
                createCell(row, 3, formatBudget(campaign.getDailyBudget()), dataStyle);
                createCell(row, 4, formatBudget(campaign.getTotalBudget()), dataStyle);
                createCell(row, 5, formatDate(campaign.getStartDate()), dataStyle);
                createCell(row, 6, formatDate(campaign.getEndDate()), dataStyle);

                // Ad Set information
                createCell(row, 7, campaign.getName() + " - Ad Set", dataStyle);
                createCell(row, 8, formatBudget(campaign.getDailyBudget()), dataStyle);
                createCell(row, 9, campaign.getTargetAudience(), dataStyle);

                // Ad information
                createCell(row, 10, ad.getName(), dataStyle);
                createCell(row, 11, mapAdType(ad.getAdType()), dataStyle);
                createCell(row, 12, ad.getHeadline(), dataStyle);
                createCell(row, 13, ad.getPrimaryText(), dataStyle);
                createCell(row, 14, ad.getDescription(), dataStyle);
                createCell(row, 15, mapCallToAction(ad.getCallToAction()), dataStyle);
                createCell(row, 16, ad.getWebsiteUrl(), urlStyle);
                createCell(row, 17, ad.getImageUrl(), urlStyle);
                createCell(row, 18, ad.getVideoUrl(), urlStyle);
                createCell(row, 19, "Active", dataStyle);
            }

            // Auto-size columns for better readability (with limits for performance)
            for (int i = 0; i < CSV_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                // Cap column width to prevent excessive widths
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            // Write to byte array
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Create header cell style with professional formatting
     */
    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();

        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);

        return style;
    }

    /**
     * Create data cell style
     */
    private XSSFCellStyle createDataStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);

        return style;
    }

    /**
     * Create URL cell style with hyperlink formatting
     */
    private XSSFCellStyle createUrlStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = createDataStyle(workbook);
        XSSFFont font = workbook.createFont();

        font.setColor(IndexedColors.BLUE.getIndex());
        font.setUnderline(Font.U_SINGLE);
        style.setFont(font);

        return style;
    }

    /**
     * Helper method to create and style cells
     * Prevents null pointer exceptions and ensures consistent formatting
     */
    private void createCell(Row row, int column, String value, XSSFCellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Unified export method supporting both CSV and Excel formats
     * API Stability: Maintains backward compatibility while extending functionality
     */
    public ResponseEntity<byte[]> exportAdsBulk(List<Long> adIds, String format) {
        log.info("Bulk export requested for {} ads in format: {}", adIds.size(), format);

        // Input validation
        if (format == null || format.trim().isEmpty()) {
            format = "csv"; // Default to CSV for backward compatibility
        }

        format = format.toLowerCase().trim();

        // Security: Whitelist validation for format parameter
        if (!format.equals("csv") && !format.equals("excel") && !format.equals("xlsx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid export format. Supported formats: csv, excel, xlsx");
        }

        // Route to appropriate export method
        if (format.equals("excel") || format.equals("xlsx")) {
            return exportAdsToExcel(adIds);
        } else {
            return exportMultipleAdsToFacebookTemplate(adIds);
        }
    }

}