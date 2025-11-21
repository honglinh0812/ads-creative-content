package com.fbadsautomation.service;

import com.fbadsautomation.dto.FacebookAdPayload;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.FacebookCTA;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FacebookAdPayloadBuilder {
    private static final Logger log = LoggerFactory.getLogger(FacebookAdPayloadBuilder.class);
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(?i)(https?://)" +
            "(" +
            "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*" +
            "[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
            "\\.[a-zA-Z]{2,}" +
            "|" +
            "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
            "|" +
            "localhost" +
            ")" +
            "(:[0-9]{1,5})?" +
            "(/.*)?" +
            "$"
    );
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final MinIOStorageService minioStorageService;

    public FacebookAdPayload buildPayload(Ad ad) {
        Campaign campaign = ad.getCampaign();

        return FacebookAdPayload.builder()
            .campaign(FacebookAdPayload.CampaignPayload.builder()
                .name(campaign.getName())
                .status("ACTIVE")
                .objective(mapCampaignObjective(campaign.getObjective()))
                .buyingType("AUCTION")
                .dailyBudget(formatBudgetForFacebook(campaign.getDailyBudget()))
                .lifetimeBudget(formatBudgetForFacebook(campaign.getTotalBudget()))
                .startTime(formatDateTime(campaign.getStartDate()))
                .endTime(formatDateTime(campaign.getEndDate()))
                .build())
            .adSet(FacebookAdPayload.AdSetPayload.builder()
                .name(campaign.getName() + " - Ad Set")
                .status("ACTIVE")
                .startTime(formatDateTime(campaign.getStartDate()))
                .endTime(formatDateTime(campaign.getEndDate()))
                .link(ad.getWebsiteUrl())
                .countries(extractCountriesFromAudience(campaign.getTargetAudience()))
                .gender(extractGenderFromAudience(campaign.getTargetAudience()))
                .ageMin(extractAgeMinFromAudience(campaign.getTargetAudience()))
                .ageMax(extractAgeMaxFromAudience(campaign.getTargetAudience()))
                .publisherPlatforms("facebook,instagram")
                .facebookPositions("feed")
                .instagramPositions("stream")
                .optimizationGoal(mapOptimizationGoal(campaign.getObjective()))
                .billingEvent("IMPRESSIONS")
                .build())
            .creative(FacebookAdPayload.CreativePayload.builder()
                .type(mapCreativeType(ad.getAdType(), ad.getImageUrl(), ad.getVideoUrl()))
                .headline(ad.getHeadline())
                .body(ad.getPrimaryText())
                .description(ad.getDescription())
                .displayLink(ad.getWebsiteUrl())
                .imageUrl(getImageUrlForFacebook(ad.getImageUrl()))
                .callToAction(mapCallToAction(ad.getCallToAction()))
                .marketingMessage(ad.getPrimaryText())
                .build())
            .ad(FacebookAdPayload.AdPayload.builder()
                .adId(ad.getId())
                .name(ad.getName())
                .status("ACTIVE")
                .websiteUrl(ad.getWebsiteUrl())
                .build())
            .build();
    }

    public List<FacebookAdPayload> buildPayloads(List<Ad> ads) {
        return ads.stream()
            .map(this::buildPayload)
            .collect(java.util.stream.Collectors.toList());
    }

    private String mapCampaignObjective(Campaign.CampaignObjective objective) {
        if (objective == null) return "Traffic";

        return switch (objective) {
            case BRAND_AWARENESS -> "Brand Awareness";
            case REACH -> "Reach";
            case TRAFFIC -> "Traffic";
            case ENGAGEMENT -> "Post Engagement";
            case APP_INSTALLS -> "App Installs";
            case VIDEO_VIEWS -> "Video Views";
            case LEAD_GENERATION -> "Lead Generation";
            case CONVERSIONS -> "Conversions";
            case CATALOG_SALES -> "Catalog Sales";
            case STORE_TRAFFIC -> "Store Traffic";
            default -> "Traffic";
        };
    }

    private String mapOptimizationGoal(Campaign.CampaignObjective objective) {
        if (objective == null) return "LINK_CLICKS";

        return switch (objective) {
            case CONVERSIONS -> "CONVERSIONS";
            case LEAD_GENERATION -> "LEAD_GENERATION";
            case TRAFFIC -> "LINK_CLICKS";
            case ENGAGEMENT -> "POST_ENGAGEMENT";
            case VIDEO_VIEWS -> "VIDEO_VIEWS";
            case APP_INSTALLS -> "APP_INSTALLS";
            case REACH -> "REACH";
            case BRAND_AWARENESS -> "BRAND_AWARENESS";
            default -> "LINK_CLICKS";
        };
    }

    private String mapCreativeType(AdType adType, String imageUrl, String videoUrl) {
        boolean hasVideo = StringUtils.hasText(videoUrl);

        if (adType == AdType.LEAD_FORM_AD) {
            return "Lead Ad";
        }

        if (hasVideo) {
            return "Video Page Post Ad";
        }

        return "Page Post Ad";
    }

    private String mapCallToAction(FacebookCTA cta) {
        if (cta == null) return "LEARN_MORE";
        return cta.name();
    }

    private String formatDateTime(java.time.LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER) + " 00:00";
    }

    private String formatBudgetForFacebook(Double budget) {
        if (budget == null || budget <= 0) {
            return "";
        }
        long budgetInSmallestUnit = Math.round(budget * getBudgetMultiplier());
        return String.valueOf(budgetInSmallestUnit);
    }

    private int getBudgetMultiplier() {
        return 100;
    }

    private String extractCountriesFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "US";
        }
        if (targetAudience.contains("Vietnam") || targetAudience.contains("VN")) {
            return "VN";
        }
        return "US";
    }

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

    private String extractAgeMinFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "18";
        }
        if (targetAudience.matches(".*\\b(\\d{2})\\s*-\\s*\\d{2}.*")) {
            return targetAudience.replaceAll(".*\\b(\\d{2})\\s*-\\s*\\d{2}.*", "$1");
        }
        return "18";
    }

    private String extractAgeMaxFromAudience(String targetAudience) {
        if (!StringUtils.hasText(targetAudience)) {
            return "65";
        }
        if (targetAudience.matches(".*\\b\\d{2}\\s*-\\s*(\\d{2}).*")) {
            return targetAudience.replaceAll(".*\\b\\d{2}\\s*-\\s*(\\d{2}).*", "$1");
        }
        return "65";
    }

    private String getImageUrlForFacebook(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return "";
        }

        if (imageUrl.startsWith("/api/images/")) {
            String filename = imageUrl.substring("/api/images/".length());
            try {
                String publicUrl = minioStorageService.getPublicUrl(filename);
                log.debug("Converted API image URL to public URL: {} -> {}", imageUrl, publicUrl);
                return publicUrl;
            } catch (Exception e) {
                log.error("Failed to get public URL for image: {}", filename, e);
                return "";
            }
        }

        if (isValidUrl(imageUrl)) {
            return imageUrl;
        }

        log.warn("Cannot export local file path to Facebook (not accessible): {}", imageUrl);
        return "";
    }

    private boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }
}
