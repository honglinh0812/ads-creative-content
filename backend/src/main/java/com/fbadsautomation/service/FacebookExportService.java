package com.fbadsautomation.service;

import com.fbadsautomation.dto.FacebookAdPayload;
import com.fbadsautomation.dto.FacebookAutoExportResponse;
import com.fbadsautomation.dto.FacebookExportResponse;
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
import java.util.*;
import java.util.stream.Collectors;
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
    private final FacebookAdPayloadBuilder payloadBuilder;
    private final FacebookAutoUploadService facebookAutoUploadService;
    
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
        // Validate ad content for Facebook (this will throw exceptions if invalid)
        validateAdContentForFacebook(ad);
        FacebookAdPayload payload = payloadBuilder.buildPayload(ad);
        
        // Create preview data structure
        Map<String, Object> preview = new HashMap<>();
        
        // Campaign information
        Map<String, Object> campaignInfo = new HashMap<>();
        campaignInfo.put("name", payload.getCampaign().getName());
        campaignInfo.put("objective", payload.getCampaign().getObjective());
        campaignInfo.put("dailyBudget", payload.getCampaign().getDailyBudget());
        campaignInfo.put("lifetimeBudget", payload.getCampaign().getLifetimeBudget());
        campaignInfo.put("startDate", payload.getCampaign().getStartTime());
        campaignInfo.put("endDate", payload.getCampaign().getEndTime());
        campaignInfo.put("targetAudience", payload.getAdSet().getCountries());
        preview.put("campaign", campaignInfo);
        
        // Ad Set information (using campaign data as base)
        Map<String, Object> adSetInfo = new HashMap<>();
        adSetInfo.put("name", payload.getAdSet().getName());
        adSetInfo.put("budget", payload.getCampaign().getDailyBudget());
        adSetInfo.put("targetAudience", payload.getAdSet().getCountries());
        preview.put("adSet", adSetInfo);
        
        // Ad information
        Map<String, Object> adInfo = new HashMap<>();
        adInfo.put("name", payload.getAd().getName());
        adInfo.put("type", payload.getCreative().getType());
        adInfo.put("headline", payload.getCreative().getHeadline());
        adInfo.put("primaryText", payload.getCreative().getBody());
        adInfo.put("description", payload.getCreative().getDescription());
        adInfo.put("callToAction", payload.getCreative().getCallToAction());
        adInfo.put("websiteUrl", payload.getCreative().getDisplayLink());
        adInfo.put("imageUrl", payload.getCreative().getImageUrl());
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
            payload.getCampaign().getName(),
            payload.getCampaign().getStatus(),
            payload.getCampaign().getObjective(),
            payload.getCampaign().getBuyingType(),
            payload.getCampaign().getDailyBudget(),
            payload.getCampaign().getLifetimeBudget(),
            payload.getCampaign().getStartTime(),
            payload.getCampaign().getEndTime(),

            // Ad Set Level (12 fields - Ad Set budgets REMOVED)
            payload.getAdSet().getName(),
            payload.getAdSet().getStatus(),
            payload.getAdSet().getStartTime(),
            payload.getAdSet().getEndTime(),
            payload.getAdSet().getLink(),
            payload.getAdSet().getCountries(),
            payload.getAdSet().getGender(),
            payload.getAdSet().getAgeMin(),
            payload.getAdSet().getAgeMax(),
            payload.getAdSet().getPublisherPlatforms(),
            payload.getAdSet().getFacebookPositions(),
            payload.getAdSet().getInstagramPositions(),
            payload.getAdSet().getOptimizationGoal(),
            payload.getAdSet().getBillingEvent(),

            // Ad Level (9 fields)
            payload.getAd().getName(),
            payload.getAd().getStatus(),
            payload.getCreative().getType(),
            payload.getCreative().getHeadline(),
            payload.getCreative().getBody(),
            payload.getCreative().getDescription(),
            payload.getCreative().getDisplayLink(),
            payload.getCreative().getImageUrl(),
            payload.getCreative().getCallToAction(),
            payload.getCreative().getMarketingMessage()
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

            validateAdContentForFacebook(ad);
            FacebookAdPayload payload = payloadBuilder.buildPayload(ad);

            // Create individual preview
            Map<String, Object> adPreview = new HashMap<>();
            adPreview.put("adId", adId);
            adPreview.put("adName", payload.getAd().getName());
            adPreview.put("campaignName", payload.getCampaign().getName());
            adPreview.put("headline", payload.getCreative().getHeadline());
            adPreview.put("primaryText", payload.getCreative().getBody());
            adPreview.put("validation", Map.of(
                "headlineLength", payload.getCreative().getHeadline() != null ? payload.getCreative().getHeadline().length() : 0,
                "primaryTextLength", payload.getCreative().getBody() != null ? payload.getCreative().getBody().length() : 0
            ));
            adPreviews.add(adPreview);
            
            // Create CSV row
            List<String> csvRow = Arrays.asList(
                // Campaign Level (8 fields)
                payload.getCampaign().getName(),
                payload.getCampaign().getStatus(),
                payload.getCampaign().getObjective(),
                payload.getCampaign().getBuyingType(),
                payload.getCampaign().getDailyBudget(),
                payload.getCampaign().getLifetimeBudget(),
                payload.getCampaign().getStartTime(),
                payload.getCampaign().getEndTime(),

                // Ad Set Level (12 fields)
                payload.getAdSet().getName(),
                payload.getAdSet().getStatus(),
                payload.getAdSet().getStartTime(),
                payload.getAdSet().getEndTime(),
                payload.getAdSet().getLink(),
                payload.getAdSet().getCountries(),
                payload.getAdSet().getGender(),
                payload.getAdSet().getAgeMin(),
                payload.getAdSet().getAgeMax(),
                payload.getAdSet().getPublisherPlatforms(),
                payload.getAdSet().getFacebookPositions(),
                payload.getAdSet().getInstagramPositions(),
                payload.getAdSet().getOptimizationGoal(),
                payload.getAdSet().getBillingEvent(),

                // Ad Level (9 fields)
                payload.getAd().getName(),
                payload.getAd().getStatus(),
                payload.getCreative().getType(),
                payload.getCreative().getHeadline(),
                payload.getCreative().getBody(),
                payload.getCreative().getDescription(),
                payload.getCreative().getDisplayLink(),
                payload.getCreative().getImageUrl(),
                payload.getCreative().getCallToAction(),
                payload.getCreative().getMarketingMessage()
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

            List<PreparedAdExport> prepared = prepareAds(Collections.singletonList(adId));
            PreparedAdExport preparedAd = prepared.get(0);

            // Generate CSV content
            String csvContent = generateFacebookCsvContent(prepared);
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            
            // Create response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "facebook_ad_" + adId + ".csv");
            headers.setContentLength(csvBytes.length);

            // Mark campaign as EXPORTED (Issue #7)
            markCampaignAsExportedForAd(preparedAd.getAd());

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
            
            List<PreparedAdExport> prepared = prepareAds(adIds);
            
            // Generate CSV content
            String csvContent = generateFacebookCsvContent(prepared);
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            
            // Create response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "facebook_ads_bulk_" + System.currentTimeMillis() + ".csv");
            headers.setContentLength(csvBytes.length);

            // Mark campaigns as EXPORTED (Issue #7)
            for (PreparedAdExport preparedAd : prepared) {
                markCampaignAsExportedForAd(preparedAd.getAd());
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
    private String generateFacebookCsvContent(List<PreparedAdExport> preparedAds) {
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
            for (PreparedAdExport prepared : preparedAds) {
                FacebookAdPayload payload = prepared.getPayload();
                var campaign = payload.getCampaign();
                var adSet = payload.getAdSet();
                var creative = payload.getCreative();
                var adInfo = payload.getAd();

                List<String> csvRow = Arrays.asList(
                    // Campaign Level Fields (8 fields)
                    escapeCsvValue(campaign.getName()),
                    escapeCsvValue(campaign.getStatus()),
                    escapeCsvValue(campaign.getObjective()),
                    escapeCsvValue(campaign.getBuyingType()),
                    escapeCsvValue(campaign.getDailyBudget()),
                    escapeCsvValue(campaign.getLifetimeBudget()),
                    escapeCsvValue(campaign.getStartTime()),
                    escapeCsvValue(campaign.getEndTime()),

                    // Ad Set Level Fields
                    escapeCsvValue(adSet.getName()),
                    escapeCsvValue(adSet.getStatus()),
                    escapeCsvValue(adSet.getStartTime()),
                    escapeCsvValue(adSet.getEndTime()),
                    escapeCsvValue(adSet.getLink()),
                    escapeCsvValue(adSet.getCountries()),
                    escapeCsvValue(adSet.getGender()),
                    escapeCsvValue(adSet.getAgeMin()),
                    escapeCsvValue(adSet.getAgeMax()),
                    escapeCsvValue(adSet.getPublisherPlatforms()),
                    escapeCsvValue(adSet.getFacebookPositions()),
                    escapeCsvValue(adSet.getInstagramPositions()),
                    escapeCsvValue(adSet.getOptimizationGoal()),
                    escapeCsvValue(adSet.getBillingEvent()),

                    // Ad Level Fields
                    escapeCsvValue(adInfo.getName()),
                    escapeCsvValue(adInfo.getStatus()),
                    escapeCsvValue(creative.getType()),
                    escapeCsvValue(creative.getHeadline()),
                    escapeCsvValue(creative.getBody()),
                    escapeCsvValue(creative.getDescription()),
                    escapeCsvValue(creative.getDisplayLink()),
                    escapeCsvValue(creative.getImageUrl()),
                    escapeCsvValue(creative.getCallToAction()),
                    escapeCsvValue(creative.getMarketingMessage())
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
            List<PreparedAdExport> prepared = prepareAds(adIds);

            // Generate Excel file
            byte[] excelBytes = generateExcelContent(prepared);

            // Create response headers with proper content disposition
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                "facebook_ads_" + System.currentTimeMillis() + ".xlsx");
            headers.setContentLength(excelBytes.length);
            headers.set("X-Export-Count", String.valueOf(prepared.size()));

            // Mark campaigns as EXPORTED (Issue #7)
            for (PreparedAdExport preparedAd : prepared) {
                markCampaignAsExportedForAd(preparedAd.getAd());
            }

            log.info("Successfully exported {} ads to Excel", prepared.size());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exporting ads to Excel: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to export ads to Excel: " + e.getMessage());
        }
    }

    private List<PreparedAdExport> prepareAds(List<Long> adIds) {
        if (adIds == null || adIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ad IDs list cannot be empty");
        }

        List<Ad> ads = adRepository.findAllById(adIds);
        if (ads.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No ads found with provided IDs");
        }

        List<PreparedAdExport> prepared = new ArrayList<>();
        for (Ad ad : ads) {
            validateAdContentForFacebook(ad);
            prepared.add(new PreparedAdExport(ad, payloadBuilder.buildPayload(ad)));
        }

        return prepared;
    }

    private void enforceBudgetMinimums(List<PreparedAdExport> preparedAds) {
        double minimumBudget = payloadBuilder.getMinimumBudgetAmount();
        for (PreparedAdExport prepared : preparedAds) {
            Campaign campaign = prepared.getAd().getCampaign();
            if (campaign == null) {
                continue;
            }
            Double budget = resolveCampaignBudget(campaign);
            double normalizedBudget = payloadBuilder.normalizeBudgetForCurrency(budget);
            if (normalizedBudget < minimumBudget) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Campaign '" + campaign.getName() + "' daily budget (" + budget +
                        ") is below the required minimum of " + minimumBudget +
                        " " + payloadBuilder.getCurrency() + " for Facebook auto upload.");
            }
        }
    }

    private Double resolveCampaignBudget(Campaign campaign) {
        if (campaign.getDailyBudget() != null && campaign.getDailyBudget() > 0) {
            return campaign.getDailyBudget();
        }
        if (campaign.getBudgetType() == Campaign.BudgetType.DAILY && campaign.getBudget() != null) {
            return campaign.getBudget();
        }
        return campaign.getTotalBudget();
    }

    /**
     * Generate Excel content with proper formatting
     * Implements SOLID principles: Single responsibility, proper resource management
     */
    private byte[] generateExcelContent(List<PreparedAdExport> preparedAds) throws IOException {
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
            for (PreparedAdExport prepared : preparedAds) {
                FacebookAdPayload payload = prepared.getPayload();
                var campaign = payload.getCampaign();
                var adSet = payload.getAdSet();
                var creative = payload.getCreative();
                var ad = payload.getAd();
                Row row = sheet.createRow(rowNum++);

                int colNum = 0;

                // Campaign Level Fields (8 fields)
                createCell(row, colNum++, campaign.getName(), dataStyle);
                createCell(row, colNum++, campaign.getStatus(), dataStyle);
                createCell(row, colNum++, campaign.getObjective(), dataStyle);
                createCell(row, colNum++, campaign.getBuyingType(), dataStyle);
                createCell(row, colNum++, campaign.getDailyBudget(), dataStyle);
                createCell(row, colNum++, campaign.getLifetimeBudget(), dataStyle);
                createCell(row, colNum++, campaign.getStartTime(), dataStyle);
                createCell(row, colNum++, campaign.getEndTime(), dataStyle);

                // Ad Set Level Fields (12 fields - Ad Set budgets REMOVED)
                createCell(row, colNum++, adSet.getName(), dataStyle);
                createCell(row, colNum++, adSet.getStatus(), dataStyle);
                createCell(row, colNum++, adSet.getStartTime(), dataStyle);
                createCell(row, colNum++, adSet.getEndTime(), dataStyle);
                createCell(row, colNum++, adSet.getLink(), urlStyle);
                createCell(row, colNum++, adSet.getCountries(), dataStyle);
                createCell(row, colNum++, adSet.getGender(), dataStyle);
                createCell(row, colNum++, adSet.getAgeMin(), dataStyle);
                createCell(row, colNum++, adSet.getAgeMax(), dataStyle);
                createCell(row, colNum++, adSet.getPublisherPlatforms(), dataStyle);
                createCell(row, colNum++, adSet.getFacebookPositions(), dataStyle);
                createCell(row, colNum++, adSet.getInstagramPositions(), dataStyle);
                createCell(row, colNum++, adSet.getOptimizationGoal(), dataStyle);
                createCell(row, colNum++, adSet.getBillingEvent(), dataStyle);

                // Ad Level Fields (9 fields)
                createCell(row, colNum++, ad.getName(), dataStyle);
                createCell(row, colNum++, ad.getStatus(), dataStyle);
                createCell(row, colNum++, creative.getType(), dataStyle);
                createCell(row, colNum++, creative.getHeadline(), dataStyle);
                createCell(row, colNum++, creative.getBody(), dataStyle);
                createCell(row, colNum++, creative.getDescription(), dataStyle);
                createCell(row, colNum++, creative.getDisplayLink(), urlStyle);
                createCell(row, colNum++, creative.getImageUrl(), urlStyle);
                createCell(row, colNum++, creative.getCallToAction(), dataStyle);
                createCell(row, colNum++, creative.getMarketingMessage(), dataStyle);
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
     * Unified export method supporting both CSV and Excel formats with optional auto-upload.
     */
    public FacebookExportResponse exportAdsBulk(List<Long> adIds, String format, boolean autoUpload, String adAccountId) {
        log.info("Bulk export requested for {} ads in format: {} (autoUpload={})", adIds.size(), format, autoUpload);

        if (!StringUtils.hasText(format)) {
            format = "csv";
        }
        String normalizedFormat = format.toLowerCase().trim();
        if (!normalizedFormat.equals("csv") && !normalizedFormat.equals("excel") && !normalizedFormat.equals("xlsx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid export format. Supported formats: csv, excel, xlsx");
        }

        List<PreparedAdExport> prepared = prepareAds(adIds);
        enforceBudgetMinimums(prepared);
        byte[] fileContent;
        String filename;
        if (normalizedFormat.equals("excel") || normalizedFormat.equals("xlsx")) {
            try {
                fileContent = generateExcelContent(prepared);
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to export ads to Excel: " + e.getMessage());
            }
            filename = "facebook_ads_" + System.currentTimeMillis() + ".xlsx";
        } else {
            String csvContent = generateFacebookCsvContent(prepared);
            fileContent = csvContent.getBytes(StandardCharsets.UTF_8);
            filename = "facebook_ads_" + System.currentTimeMillis() + ".csv";
        }

        FacebookAutoExportResponse autoUploadResponse = autoUpload
            ? facebookAutoUploadService.autoUpload(
                prepared.stream().map(PreparedAdExport::getAd).collect(Collectors.toList()),
                adAccountId
            )
            : facebookAutoUploadService.skipped("Auto upload disabled");

        prepared.forEach(preparedAd -> markCampaignAsExportedForAd(preparedAd.getAd()));

        return FacebookExportResponse.builder()
            .filename(filename)
            .format(normalizedFormat)
            .fileContent(fileContent)
            .payloads(prepared.stream().map(PreparedAdExport::getPayload).collect(Collectors.toList()))
            .autoUpload(autoUploadResponse)
            .build();
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
                            campaign.getBidCap(),
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

    private static class PreparedAdExport {
        private final Ad ad;
        private final FacebookAdPayload payload;

        PreparedAdExport(Ad ad, FacebookAdPayload payload) {
            this.ad = ad;
            this.payload = payload;
        }

        public Ad getAd() {
            return ad;
        }

        public FacebookAdPayload getPayload() {
            return payload;
        }
    }
}
