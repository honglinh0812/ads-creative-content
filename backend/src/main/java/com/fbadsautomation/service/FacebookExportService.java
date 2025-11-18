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
    private final MinIOStorageService minioStorageService;
    private final com.fbadsautomation.util.AdContentValidator adContentValidator;
    private final com.fbadsautomation.integration.facebook.FacebookMarketingApiClient marketingApiClient;
    private final com.fbadsautomation.integration.facebook.FacebookProperties facebookProperties;
    
    // Enhanced URL pattern supporting:
    // - HTTP/HTTPS protocols (case-insensitive)
    // - Subdomains and complex domains
    // - IP addresses (IPv4)
    // - Port numbers
    // - Query parameters and fragments
    // - Storage URLs (MinIO, S3, CDN)
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(?i)(https?://)" +                                    // Protocol (case-insensitive, required for remote URLs)
        "(" +                                                    // Domain or IP
            "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*" + // Subdomains (optional)
            "[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +       // Domain name
            "\\.[a-zA-Z]{2,}" +                                    // TLD (.com, .org, etc.)
            "|" +                                                  // OR
            "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +    // IPv4 address
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +            // Last octet
            "|" +                                                  // OR
            "localhost" +                                          // localhost
        ")" +
        "(:[0-9]{1,5})?" +                                       // Port (optional)
        "(/.*)?" +                                                // Path, query, fragment (optional)
        "$"
    );
    
    // Facebook Ads Import Template Headers
    // Matches Facebook's official bulk import template format
    // Total: 29 columns (Campaign: 8, Ad Set: 12, Ad: 9)
    private static final String[] CSV_HEADERS = {
        // Campaign Level Fields (8 fields)
        "Campaign Name",
        "Campaign Status",
        "Campaign Objective",
        "Buying Type",
        "Campaign Daily Budget",
        "Campaign Lifetime Budget",
        "Campaign Start Time",
        "Campaign Stop Time",

        // Ad Set Level Fields (12 fields)
        // NOTE: Ad Set budgets removed - Facebook doesn't allow both campaign and ad set budgets
        "Ad Set Name",
        "Ad Set Run Status",
        "Ad Set Time Start",
        "Ad Set Time Stop",
        "Link",
        "Countries",
        "Gender",
        "Age Min",
        "Age Max",
        "Publisher Platforms",
        "Facebook Positions",
        "Instagram Positions",
        "Optimization Goal",
        "Billing Event",

        // Ad Level Fields (9 fields)
        "Ad Name",
        "Ad Status",
        "Creative Type",                 // NEW: Required by Facebook to identify ad format
        "Title",
        "Body",
        "Link Description",
        "Display Link",
        "Image URL",                     // CHANGED: From "Image File Name" to support URL import
        "Call to Action",
        "Marketing Message Primary Text"
    };
    
    /**
     * Validate ad content for Facebook export
     */
    private void validateAdContentForFacebook(Ad ad) {
        log.info("Validating ad content for Facebook export: {}", ad.getId());

        // Check if ad status is ready
        if (!"READY".equals(ad.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Ad must be in READY status to export to Facebook");
        }

        // Validate required content fields
        // Note: We validate actual content instead of just selectedContentId
        // This is more reliable as it checks what's actually exportable
        if (!StringUtils.hasText(ad.getHeadline())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Headline is required for Facebook export");
        }

        boolean normalized = adContentValidator.enforceAdLimits(ad);
        if (normalized) {
            log.warn("Headline/body exceeded Facebook limits for ad {}. Values were auto-truncated during export validation.", ad.getId());
            adRepository.save(ad);
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
        if (ad.getPrimaryText().length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Primary text must be 1000 characters or less for Facebook");
        }
        
        // Validate description length if present (Facebook limit: 125 characters)
        if (StringUtils.hasText(ad.getDescription()) && ad.getDescription().length() > 125) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Description must be 125 characters or less for Facebook");
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
        // Validate input is not null or empty
        if (!StringUtils.hasText(imageUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Image URL is required");
        }

        // Trim whitespace
        imageUrl = imageUrl.trim();

        // Handle API endpoint URLs (stored in database but served via ImageController)
        if (imageUrl.startsWith("/api/images/")) {
            log.debug("Validating MinIO-backed image URL: {}", imageUrl);
            String filename = extractFilenameFromApiUrl(imageUrl, "/api/images/");
            validateMinIOImageFile(filename);
            return;
        }

        // Handle static placeholder images (no validation needed)
        if (imageUrl.startsWith("/img/") || imageUrl.equals("placeholder.png")) {
            log.debug("Skipping validation for static placeholder: {}", imageUrl);
            return;
        }

        // Check if it's a local file path (legacy storage)
        // Local paths: /uploads/..., uploads/..., ./uploads/..., ../uploads/...
        if (imageUrl.startsWith("/uploads/") ||
            imageUrl.startsWith("uploads/") ||
            imageUrl.startsWith("./") ||
            imageUrl.startsWith("../")) {
            validateLocalImageFile(imageUrl);
        } else if (isValidUrl(imageUrl)) {
            // If it's a valid URL, validate remote URL requirements
            validateRemoteImageUrl(imageUrl);
        } else {
            // If it's neither a local path nor a valid URL, throw error
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid image URL format. Must be a valid URL or local file path");
        }
    }
    
    /**
     * Validate video requirements for Facebook
     */
    private void validateVideoRequirements(String videoUrl) {
        // Validate input is not null or empty
        if (!StringUtils.hasText(videoUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Video URL is required");
        }

        // Trim whitespace
        videoUrl = videoUrl.trim();

        // Handle API endpoint URLs (stored in database but served via ImageController)
        if (videoUrl.startsWith("/api/videos/") || videoUrl.startsWith("/api/images/")) {
            log.debug("Validating MinIO-backed video URL: {}", videoUrl);
            String prefix = videoUrl.startsWith("/api/videos/") ? "/api/videos/" : "/api/images/";
            String filename = extractFilenameFromApiUrl(videoUrl, prefix);
            validateMinIOVideoFile(filename);
            return;
        }

        // Handle static placeholder videos (no validation needed)
        if (videoUrl.startsWith("/video/") || videoUrl.startsWith("/videos/")) {
            log.debug("Skipping validation for static video: {}", videoUrl);
            return;
        }

        // Check if it's a local file path (legacy storage)
        // Local paths: /uploads/..., uploads/..., ./uploads/..., ../uploads/...
        if (videoUrl.startsWith("/uploads/") ||
            videoUrl.startsWith("uploads/") ||
            videoUrl.startsWith("./") ||
            videoUrl.startsWith("../")) {
            validateLocalVideoFile(videoUrl);
        } else if (isValidUrl(videoUrl)) {
            // If it's a valid URL, validate remote URL requirements
            validateRemoteVideoUrl(videoUrl);
        } else {
            // If it's neither a local path nor a valid URL, throw error
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid video URL format. Must be a valid URL or local file path");
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
     * Extract filename from API URL
     * Example: "/api/images/abc123.png" -> "abc123.png"
     */
    private String extractFilenameFromApiUrl(String url, String prefix) {
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        return url;
    }

    /**
     * Validate image file stored in MinIO
     */
    private void validateMinIOImageFile(String filename) {
        try {
            log.debug("Validating MinIO image file: {}", filename);

            // Check if file exists in MinIO
            if (!minioStorageService.fileExists(filename)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Image file not found in storage: " + filename);
            }

            // Get file metadata from MinIO
            io.minio.StatObjectResponse fileInfo = minioStorageService.getFileInfo(filename);
            long fileSize = fileInfo.size();
            String contentType = fileInfo.contentType();

            log.debug("MinIO image file found: {} (size: {} bytes, type: {})",
                filename, fileSize, contentType);

            // Validate file size (Facebook limit: 30MB for images)
            long maxSizeBytes = 30L * 1024 * 1024; // 30MB
            if (fileSize > maxSizeBytes) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Image file size exceeds Facebook limit of 30MB");
            }

            // Validate file format based on extension
            String lowerFilename = filename.toLowerCase();
            if (!lowerFilename.endsWith(".jpg") && !lowerFilename.endsWith(".jpeg") &&
                !lowerFilename.endsWith(".png") && !lowerFilename.endsWith(".gif")) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Image format not supported. Facebook accepts: JPG, JPEG, PNG, GIF");
            }

            log.debug("MinIO image validation passed: {}", filename);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating MinIO image file: {}", filename, e);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Error validating image file from storage: " + e.getMessage());
        }
    }

    /**
     * Validate video file stored in MinIO
     */
    private void validateMinIOVideoFile(String filename) {
        try {
            log.debug("Validating MinIO video file: {}", filename);

            // Check if file exists in MinIO
            if (!minioStorageService.fileExists(filename)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Video file not found in storage: " + filename);
            }

            // Get file metadata from MinIO
            io.minio.StatObjectResponse fileInfo = minioStorageService.getFileInfo(filename);
            long fileSize = fileInfo.size();
            String contentType = fileInfo.contentType();

            log.debug("MinIO video file found: {} (size: {} bytes, type: {})",
                filename, fileSize, contentType);

            // Validate file size (Facebook limit: 4GB for videos)
            long maxSizeBytes = 4L * 1024 * 1024 * 1024; // 4GB
            if (fileSize > maxSizeBytes) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Video file size exceeds Facebook limit of 4GB");
            }

            // Validate file format based on extension
            String lowerFilename = filename.toLowerCase();
            if (!lowerFilename.endsWith(".mp4") && !lowerFilename.endsWith(".mov") &&
                !lowerFilename.endsWith(".avi") && !lowerFilename.endsWith(".mkv") &&
                !lowerFilename.endsWith(".webm") && !lowerFilename.endsWith(".flv")) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Video format not supported. Facebook accepts: MP4, MOV, AVI, MKV, WEBM, FLV");
            }

            log.debug("MinIO video validation passed: {}", filename);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating MinIO video file: {}", filename, e);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Error validating video file from storage: " + e.getMessage());
        }
    }

    /**
     * Validate URL format
     * Supports:
     * - HTTP/HTTPS URLs with protocol
     * - Subdomains and complex domain structures
     * - IPv4 addresses and localhost
     * - Port numbers
     * - Query parameters and URL fragments
     * - Storage service URLs (MinIO, S3, CDN)
     *
     * Note: This does NOT validate local file paths - use separate validation for those
     */
    private boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }

        // Trim whitespace
        url = url.trim();

        // Match against pattern
        return URL_PATTERN.matcher(url).matches();
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

    private String mapCampaignObjective(Campaign.CampaignObjective objective) {
        if (objective == null) return "Traffic";

        switch (objective) {
            case BRAND_AWARENESS: return "Brand Awareness";
            case REACH: return "Reach";
            case TRAFFIC: return "Traffic";
            case ENGAGEMENT: return "Post Engagement";
            case APP_INSTALLS: return "App Installs";
            case VIDEO_VIEWS: return "Video Views";
            case LEAD_GENERATION: return "Lead Generation";
            case CONVERSIONS: return "Conversions";
            case CATALOG_SALES: return "Catalog Sales";
            case STORE_TRAFFIC: return "Store Traffic";
            default: return "Traffic";
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
        campaignInfo.put("startDate", formatDateTime(campaign.getStartDate()));
        campaignInfo.put("endDate", formatDateTime(campaign.getEndDate()));
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
            // Campaign Level (8 fields)
            campaign.getName(),
            "ACTIVE",
            mapCampaignObjective(campaign.getObjective()),
            "AUCTION",
            formatBudgetForFacebook(campaign.getDailyBudget()),     // UPDATED
            formatBudgetForFacebook(campaign.getTotalBudget()),     // UPDATED
            formatDateTime(campaign.getStartDate()),
            formatDateTime(campaign.getEndDate()),

            // Ad Set Level (12 fields - Ad Set budgets REMOVED)
            campaign.getName() + " - Ad Set",
            "ACTIVE",
            formatDateTime(campaign.getStartDate()),
            formatDateTime(campaign.getEndDate()),
            ad.getWebsiteUrl(),
            extractCountriesFromAudience(campaign.getTargetAudience()),
            extractGenderFromAudience(campaign.getTargetAudience()),
            extractAgeMinFromAudience(campaign.getTargetAudience()),
            extractAgeMaxFromAudience(campaign.getTargetAudience()),
            "facebook,instagram",
            "feed",
            "stream",
            mapOptimizationGoal(campaign.getObjective()),
            "IMPRESSIONS",

            // Ad Level (9 fields)
            ad.getName(),
            "ACTIVE",
            mapCreativeType(ad.getAdType(), ad.getImageUrl(), ad.getVideoUrl()),  // NEW
            ad.getHeadline(),
            ad.getPrimaryText(),
            ad.getDescription(),
            ad.getWebsiteUrl(),
            getImageUrlForFacebook(ad.getImageUrl()),                            // UPDATED
            mapCallToAction(ad.getCallToAction()),
            ad.getPrimaryText()
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
                // Campaign Level (8 fields)
                campaign.getName(),
                "ACTIVE",
                mapCampaignObjective(campaign.getObjective()),
                "AUCTION",
                formatBudgetForFacebook(campaign.getDailyBudget()),     // UPDATED
                formatBudgetForFacebook(campaign.getTotalBudget()),     // UPDATED
                formatDateTime(campaign.getStartDate()),
                formatDateTime(campaign.getEndDate()),

                // Ad Set Level (12 fields - Ad Set budgets REMOVED)
                campaign.getName() + " - Ad Set",
                "ACTIVE",
                formatDateTime(campaign.getStartDate()),
                formatDateTime(campaign.getEndDate()),
                ad.getWebsiteUrl(),
                extractCountriesFromAudience(campaign.getTargetAudience()),
                extractGenderFromAudience(campaign.getTargetAudience()),
                extractAgeMinFromAudience(campaign.getTargetAudience()),
                extractAgeMaxFromAudience(campaign.getTargetAudience()),
                "facebook,instagram",
                "feed",
                "stream",
                mapOptimizationGoal(campaign.getObjective()),
                "IMPRESSIONS",

                // Ad Level (9 fields)
                ad.getName(),
                "ACTIVE",
                mapCreativeType(ad.getAdType(), ad.getImageUrl(), ad.getVideoUrl()),  // NEW
                ad.getHeadline(),
                ad.getPrimaryText(),
                ad.getDescription(),
                ad.getWebsiteUrl(),
                getImageUrlForFacebook(ad.getImageUrl()),                            // UPDATED
                mapCallToAction(ad.getCallToAction()),
                ad.getPrimaryText()
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

            // Mark campaign as EXPORTED (Issue #7)
            markCampaignAsExportedForAd(ad);

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

            // Mark campaigns as EXPORTED (Issue #7)
            for (Ad ad : ads) {
                markCampaignAsExportedForAd(ad);
            }

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error exporting multiple ads to Facebook template: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xuất quảng cáo: " + e.getMessage());
        }
    }
    
    /**
     * Generate Facebook CSV content from ads
     * Maps ad data to Facebook's official import template format
     * Includes UTF-8 BOM for Excel compatibility
     */
    private String generateFacebookCsvContent(List<Ad> ads) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            // Add UTF-8 BOM (Byte Order Mark) for Excel to recognize UTF-8 encoding
            // This ensures Vietnamese characters display correctly when opened in Excel
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);

            // Write CSV headers
            writer.write(String.join(",", CSV_HEADERS) + "\n");

            // Write data for each ad
            for (Ad ad : ads) {
                Campaign campaign = ad.getCampaign();

                List<String> csvRow = Arrays.asList(
                    // Campaign Level Fields (8 fields)
                    escapeCsvValue(campaign.getName()),
                    escapeCsvValue("ACTIVE"),
                    escapeCsvValue(mapCampaignObjective(campaign.getObjective())),
                    escapeCsvValue("AUCTION"),
                    escapeCsvValue(formatBudgetForFacebook(campaign.getDailyBudget())),     // UPDATED: Format for FB
                    escapeCsvValue(formatBudgetForFacebook(campaign.getTotalBudget())),     // UPDATED: Format for FB
                    escapeCsvValue(formatDateTime(campaign.getStartDate())),
                    escapeCsvValue(formatDateTime(campaign.getEndDate())),

                    // Ad Set Level Fields (12 fields - Ad Set budgets REMOVED)
                    escapeCsvValue(campaign.getName() + " - Ad Set"),
                    escapeCsvValue("ACTIVE"),
                    escapeCsvValue(formatDateTime(campaign.getStartDate())),
                    escapeCsvValue(formatDateTime(campaign.getEndDate())),
                    // NO Ad Set budgets - Facebook doesn't allow both campaign and ad set budgets
                    escapeCsvValue(ad.getWebsiteUrl()),
                    escapeCsvValue(extractCountriesFromAudience(campaign.getTargetAudience())),
                    escapeCsvValue(extractGenderFromAudience(campaign.getTargetAudience())),
                    escapeCsvValue(extractAgeMinFromAudience(campaign.getTargetAudience())),
                    escapeCsvValue(extractAgeMaxFromAudience(campaign.getTargetAudience())),
                    escapeCsvValue("facebook,instagram"),
                    escapeCsvValue("feed"),
                    escapeCsvValue("stream"),
                    escapeCsvValue(mapOptimizationGoal(campaign.getObjective())),
                    escapeCsvValue("IMPRESSIONS"),

                    // Ad Level Fields (9 fields)
                    escapeCsvValue(ad.getName()),
                    escapeCsvValue("ACTIVE"),
                    escapeCsvValue(mapCreativeType(ad.getAdType(), ad.getImageUrl(), ad.getVideoUrl())),  // NEW: Creative Type
                    escapeCsvValue(ad.getHeadline()),                                                      // Title
                    escapeCsvValue(ad.getPrimaryText()),                                                   // Body
                    escapeCsvValue(ad.getDescription()),                                                   // Link Description
                    escapeCsvValue(ad.getWebsiteUrl()),                                                    // Display Link
                    escapeCsvValue(getImageUrlForFacebook(ad.getImageUrl())),                            // UPDATED: Public URL
                    escapeCsvValue(mapCallToAction(ad.getCallToAction())),
                    escapeCsvValue(ad.getPrimaryText())                                                    // Marketing Message
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
     * Format date-time for Facebook import (format: MM/DD/YYYY HH:mm)
     */
    private String formatDateTime(java.time.LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) + " 00:00";
    }

    /**
     * Extract country codes from target audience string
     * Example: "US,UK,VN" or defaults to "US"
     */
    private String extractCountriesFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "US";
        }
        // Simple extraction - can be enhanced based on actual audience format
        if (targetAudience.contains("Vietnam") || targetAudience.contains("VN")) {
            return "VN";
        }
        return "US";
    }

    /**
     * Extract gender from target audience
     * Returns: "All", "Male", or "Female"
     */
    private String extractGenderFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "All";
        }
        String lower = targetAudience.toLowerCase();
        if (lower.contains("male") && !lower.contains("female")) {
            return "Male";
        } else if (lower.contains("female")) {
            return "Female";
        }
        return "All";
    }

    /**
     * Extract minimum age from target audience (default: 18)
     */
    private String extractAgeMinFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "18";
        }
        // Try to extract age range like "18-65" or "25+"
        if (targetAudience.matches(".*\\b(\\d{2})\\s*-\\s*\\d{2}.*")) {
            String age = targetAudience.replaceAll(".*\\b(\\d{2})\\s*-\\s*\\d{2}.*", "$1");
            return age;
        }
        return "18";
    }

    /**
     * Extract maximum age from target audience (default: 65)
     */
    private String extractAgeMaxFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "65";
        }
        // Try to extract age range like "18-65"
        if (targetAudience.matches(".*\\b\\d{2}\\s*-\\s*(\\d{2}).*")) {
            String age = targetAudience.replaceAll(".*\\b\\d{2}\\s*-\\s*(\\d{2}).*", "$1");
            return age;
        }
        return "65";
    }

    /**
     * Map campaign objective to Facebook optimization goal
     */
    private String mapOptimizationGoal(Campaign.CampaignObjective objective) {
        if (objective == null) return "LINK_CLICKS";

        switch (objective) {
            case CONVERSIONS: return "CONVERSIONS";
            case LEAD_GENERATION: return "LEAD_GENERATION";
            case TRAFFIC: return "LINK_CLICKS";
            case ENGAGEMENT: return "POST_ENGAGEMENT";
            case VIDEO_VIEWS: return "VIDEO_VIEWS";
            case APP_INSTALLS: return "APP_INSTALLS";
            case REACH: return "REACH";
            case BRAND_AWARENESS: return "BRAND_AWARENESS";
            default: return "LINK_CLICKS";
        }
    }

    /**
     * Get publicly accessible image URL for Facebook import
     * Facebook will download the image from this URL during import
     *
     * @param imageUrl Internal image URL/path from database
     * @return Public URL that Facebook can access, or empty string if unavailable
     */
    private String getImageUrlForFacebook(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return "";
        }

        // Handle API URLs - convert to public MinIO URL
        if (imageUrl.startsWith("/api/images/")) {
            String filename = imageUrl.substring("/api/images/".length());
            try {
                // Get presigned URL valid for 7 days (Facebook import window)
                String publicUrl = minioStorageService.getPublicUrl(filename);
                log.debug("Converted API image URL to public URL: {} -> {}", imageUrl, publicUrl);
                return publicUrl;
            } catch (Exception e) {
                log.error("Failed to get public URL for image: {}", filename, e);
                return "";
            }
        }

        // Already a full URL - return as-is (external CDN, etc.)
        if (isValidUrl(imageUrl)) {
            return imageUrl;
        }

        // Local file path - cannot be used for Facebook import
        log.warn("Cannot export local file path to Facebook (not accessible): {}", imageUrl);
        return "";
    }

    /**
     * Map internal ad type to Facebook Creative Type
     * Facebook requires this field to determine how to process the ad creative
     *
     * @param adType Internal ad type from database
     * @param imageUrl Image URL (to detect image-based ads)
     * @param videoUrl Video URL (to detect video-based ads)
     * @return Facebook Creative Type value
     */
    private String mapCreativeType(AdType adType, String imageUrl, String videoUrl) {
        // Check for video content
        boolean hasVideo = StringUtils.hasText(videoUrl);

        // Lead form ads have specific creative type
        if (adType == AdType.LEAD_FORM_AD) {
            return "Lead Ad";
        }

        // Video ads (prioritize video if both image and video exist)
        if (hasVideo) {
            return "Video Page Post Ad";
        }

        // Default to standard image ad (most common)
        // This covers PAGE_POST_AD, WEBSITE_CONVERSION_AD, etc.
        return "Page Post Ad";
    }

    /**
     * Format budget amount for Facebook import
     * Facebook expects budget in smallest currency unit (cents for USD, đồng for VND)
     *
     * @param budget Budget amount in local currency (e.g., 10.00 USD or 100000 VND)
     * @return Budget as string in smallest currency unit, or empty string if null/invalid
     */
    private String formatBudgetForFacebook(Double budget) {
        if (budget == null || budget <= 0) {
            return "";
        }

        // Convert to smallest currency unit
        // USD: multiply by 100 (10.00 USD -> "1000" cents)
        // VND: multiply by 1 (100000 VND -> "100000" đồng)
        // Note: budgetMultiplier is injected from application.properties
        long budgetInSmallestUnit = Math.round(budget * getBudgetMultiplier());

        return String.valueOf(budgetInSmallestUnit);
    }

    /**
     * Get budget multiplier for currency conversion
     * USD: 100 (convert dollars to cents)
     * VND: 1 (already in smallest unit)
     *
     * @return Multiplier value
     */
    private int getBudgetMultiplier() {
        // Default to 100 for USD (cents conversion)
        // Can be overridden in application.properties: facebook.ads.budget.multiplier
        return 100;
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

            // Mark campaigns as EXPORTED (Issue #7)
            for (Ad ad : ads) {
                markCampaignAsExportedForAd(ad);
            }

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
     * Upload ads directly to Facebook Marketing API (best effort).
     * Requires facebook.marketing.access-token and ad account id (act_...)
     */
    public List<com.fbadsautomation.integration.facebook.FacebookMarketingApiClient.UploadResult> uploadAdsToFacebook(
        List<Long> adIds,
        String adAccountId
    ) {
        String resolvedAdAccountId = StringUtils.hasText(adAccountId)
            ? adAccountId
            : facebookProperties.getDefaultAdAccountId();

        log.info("Uploading {} ads to Facebook ad account {}", adIds.size(), resolvedAdAccountId);

        if (!StringUtils.hasText(facebookProperties.getMarketingAccessToken())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Facebook Marketing access token is missing. Set FACEBOOK_MARKETING_ACCESS_TOKEN env.");
        }
        if (!StringUtils.hasText(resolvedAdAccountId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Ad account id is required. Provide act_... or set FACEBOOK_DEFAULT_AD_ACCOUNT_ID env.");
        }
        if (adIds == null || adIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ad IDs list cannot be empty");
        }

        List<Ad> ads = adRepository.findAllById(adIds);
        if (ads.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No ads found with provided IDs");
        }

        List<com.fbadsautomation.integration.facebook.FacebookMarketingApiClient.UploadResult> results = new ArrayList<>();

        for (Ad ad : ads) {
            validateAdContentForFacebook(ad);
            var result = marketingApiClient.uploadAdToAccount(ad, resolvedAdAccountId, facebookProperties.getMarketingAccessToken());
            results.add(result);
            markCampaignAsExportedForAd(ad);
        }

        return results;
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

                int colNum = 0;

                // Campaign Level Fields (8 fields)
                createCell(row, colNum++, campaign.getName(), dataStyle);
                createCell(row, colNum++, "ACTIVE", dataStyle);
                createCell(row, colNum++, mapCampaignObjective(campaign.getObjective()), dataStyle);
                createCell(row, colNum++, "AUCTION", dataStyle);
                createCell(row, colNum++, formatBudgetForFacebook(campaign.getDailyBudget()), dataStyle);     // UPDATED: Format for FB
                createCell(row, colNum++, formatBudgetForFacebook(campaign.getTotalBudget()), dataStyle);     // UPDATED: Format for FB
                createCell(row, colNum++, formatDateTime(campaign.getStartDate()), dataStyle);
                createCell(row, colNum++, formatDateTime(campaign.getEndDate()), dataStyle);

                // Ad Set Level Fields (12 fields - Ad Set budgets REMOVED)
                createCell(row, colNum++, campaign.getName() + " - Ad Set", dataStyle);
                createCell(row, colNum++, "ACTIVE", dataStyle);
                createCell(row, colNum++, formatDateTime(campaign.getStartDate()), dataStyle);
                createCell(row, colNum++, formatDateTime(campaign.getEndDate()), dataStyle);
                // NO Ad Set budgets - Facebook doesn't allow both campaign and ad set budgets
                createCell(row, colNum++, ad.getWebsiteUrl(), urlStyle);
                createCell(row, colNum++, extractCountriesFromAudience(campaign.getTargetAudience()), dataStyle);
                createCell(row, colNum++, extractGenderFromAudience(campaign.getTargetAudience()), dataStyle);
                createCell(row, colNum++, extractAgeMinFromAudience(campaign.getTargetAudience()), dataStyle);
                createCell(row, colNum++, extractAgeMaxFromAudience(campaign.getTargetAudience()), dataStyle);
                createCell(row, colNum++, "facebook,instagram", dataStyle);
                createCell(row, colNum++, "feed", dataStyle);
                createCell(row, colNum++, "stream", dataStyle);
                createCell(row, colNum++, mapOptimizationGoal(campaign.getObjective()), dataStyle);
                createCell(row, colNum++, "IMPRESSIONS", dataStyle);

                // Ad Level Fields (9 fields)
                createCell(row, colNum++, ad.getName(), dataStyle);
                createCell(row, colNum++, "ACTIVE", dataStyle);
                createCell(row, colNum++, mapCreativeType(ad.getAdType(), ad.getImageUrl(), ad.getVideoUrl()), dataStyle);  // NEW: Creative Type
                createCell(row, colNum++, ad.getHeadline(), dataStyle);                                                      // Title
                createCell(row, colNum++, ad.getPrimaryText(), dataStyle);                                                   // Body
                createCell(row, colNum++, ad.getDescription(), dataStyle);                                                   // Link Description
                createCell(row, colNum++, ad.getWebsiteUrl(), urlStyle);                                                    // Display Link
                createCell(row, colNum++, getImageUrlForFacebook(ad.getImageUrl()), urlStyle);                             // UPDATED: Public URL
                createCell(row, colNum++, mapCallToAction(ad.getCallToAction()), dataStyle);
                createCell(row, colNum++, ad.getPrimaryText(), dataStyle);                                                   // Marketing Message
            }

            // Optimize column widths for better readability
            optimizeColumnWidths(sheet, CSV_HEADERS.length);

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
     * Optimize column widths based on content type
     * Balances readability with file size and performance
     *
     * @param sheet Excel sheet to optimize
     * @param headerCount Number of columns to optimize
     */
    private void optimizeColumnWidths(XSSFSheet sheet, int headerCount) {
        // Define optimal widths for specific column types (in Excel units: 1 unit ≈ 1/256 char width)
        // Vietnamese text and long URLs need more space than English
        Map<String, Integer> columnWidthRules = Map.ofEntries(
            Map.entry("Campaign Name", 8000),
            Map.entry("Ad Set Name", 8000),
            Map.entry("Ad Name", 8000),
            Map.entry("Title", 12000),              // Headline - longer text
            Map.entry("Body", 20000),               // Primary text - longest text field
            Map.entry("Link Description", 15000),
            Map.entry("Image URL", 15000),
            Map.entry("Link", 15000),
            Map.entry("Display Link", 15000),
            Map.entry("Marketing Message Primary Text", 20000),
            Map.entry("Campaign Objective", 6000),
            Map.entry("Optimization Goal", 6000),
            Map.entry("Creative Type", 6000),
            Map.entry("Call to Action", 5000),
            Map.entry("Default", 5000)              // Status, dates, short fields
        );

        for (int i = 0; i < headerCount; i++) {
            // Auto-size based on content
            sheet.autoSizeColumn(i);

            // Get header name to apply specific rules
            String headerName = sheet.getRow(0).getCell(i).getStringCellValue();
            int maxWidth = columnWidthRules.getOrDefault(headerName, columnWidthRules.get("Default"));

            // Apply max width constraint (prevent extremely wide columns)
            if (sheet.getColumnWidth(i) > maxWidth) {
                sheet.setColumnWidth(i, maxWidth);
            }

            // Apply min width constraint (prevent too narrow columns)
            int minWidth = 3000;  // Minimum readable width
            if (sheet.getColumnWidth(i) < minWidth) {
                sheet.setColumnWidth(i, minWidth);
            }
        }
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

    /**
     * Helper method to mark campaign as EXPORTED after ad export (Issue #7)
     * @param ad The ad that was exported
     */
    private void markCampaignAsExportedForAd(Ad ad) {
        try {
            if (ad.getCampaign() != null) {
                Campaign campaign = ad.getCampaign();
                if (campaign.getStatus() == Campaign.CampaignStatus.READY) {
                    campaign.setStatus(Campaign.CampaignStatus.EXPORTED);
                    campaignService.updateCampaign(campaign.getId(),
                        new com.fbadsautomation.dto.CampaignCreateRequest(
                            campaign.getName(),
                            campaign.getObjective() != null ? campaign.getObjective().toString() : null,
                            campaign.getBudgetType() != null ? campaign.getBudgetType().toString() : null,
                            campaign.getDailyBudget(),
                            campaign.getTotalBudget(),
                            campaign.getTargetAudience(),
                            campaign.getStartDate(),
                            campaign.getEndDate()
                        ),
                        campaign.getUser().getId()
                    );
                    log.info("Campaign {} marked as EXPORTED after ad {} export", campaign.getId(), ad.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to mark campaign as EXPORTED for ad {}: {}", ad.getId(), e.getMessage());
        }
    }

}
