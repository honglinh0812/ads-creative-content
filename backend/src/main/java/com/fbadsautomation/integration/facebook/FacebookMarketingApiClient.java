package com.fbadsautomation.integration.facebook;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.service.MinIOStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Base64;
import org.springframework.util.StringUtils;

/**
 * Lightweight Facebook Marketing API client to create campaign/adset/ad for a given ad account.
 * Note: Requires access token with ads_management (dev mode is fine for single tester).
 * This is intentionally minimal and best-effort; it returns detailed error messages for frontend fallback.
 */
@Component
@RequiredArgsConstructor
public class FacebookMarketingApiClient {

    private static final Logger log = LoggerFactory.getLogger(FacebookMarketingApiClient.class);
    // TODO: Keep these field lists in sync with Facebook Marketing API docs for the current version.
    private static final Set<String> CAMPAIGN_ALLOWED_FIELDS = Set.of(
        "name", "objective", "status", "special_ad_categories", "is_adset_budget_sharing_enabled", "access_token"
    );
    private static final Set<String> ADSET_ALLOWED_FIELDS = Set.of(
        "name", "campaign_id", "is_adset_budget_sharing_enabled", "daily_budget", "start_time",
        "end_time", "billing_event", "optimization_goal", "status", "promoted_object", "targeting",
        "access_token"
    );
    private static final Set<String> CREATIVE_ALLOWED_FIELDS = Set.of(
        "name", "object_story_spec", "access_token"
    );
    private static final Set<String> AD_ALLOWED_FIELDS = Set.of(
        "name", "adset_id", "creative", "status", "access_token"
    );

    private final RestTemplate restTemplate;
    private final FacebookProperties facebookProperties;
    private final MinIOStorageService minIOStorageService;
    private final FacebookAccountMetadataService facebookAccountMetadataService;

    private static final double MIN_VND_DAILY_BUDGET = 26481d;
    private static final double DEFAULT_LEGACY_USD_TO_VND_RATE = 25000d;
    private static final long MAX_BID_VND = 26_508_324L;
    private static final long MAX_BID_USD_CENTS = 10_000_000L; // $100,000.00

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Map<String, String> COUNTRY_CODE_ALIASES = Map.ofEntries(
        Map.entry("UNITED STATES", "US"),
        Map.entry("USA", "US"),
        Map.entry("US", "US"),
        Map.entry("VIETNAM", "VN"),
        Map.entry("VIỆT NAM", "VN"),
        Map.entry("VN", "VN"),
        Map.entry("SINGAPORE", "SG"),
        Map.entry("SG", "SG"),
        Map.entry("THAILAND", "TH"),
        Map.entry("TH", "TH"),
        Map.entry("INDONESIA", "ID"),
        Map.entry("ID", "ID"),
        Map.entry("PHILIPPINES", "PH"),
        Map.entry("PH", "PH")
    );

    public UploadResult uploadAdToAccount(Ad ad, String adAccountId, String accessToken) {
        facebookAccountMetadataService.ensureCurrencyLoaded(false);
        validateInputs(adAccountId, accessToken);

        try {
            Campaign campaign = ad.getCampaign();

            String campaignId = createCampaign(adAccountId, campaign, accessToken);
            String adSetId = createAdSet(adAccountId, campaign, ad, campaignId, accessToken);
            String creativeId = createAdCreative(adAccountId, ad, accessToken);
            String adId = createAd(adAccountId, ad, adSetId, creativeId, accessToken);

            UploadResult result = new UploadResult();
            result.setCampaignId(campaignId);
            result.setAdSetId(adSetId);
            result.setCreativeId(creativeId);
            result.setAdId(adId);
            result.setMessage("Uploaded via Marketing API");
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload ad {} to Meta: {}", ad.getId(), e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Failed to upload to Facebook: " + e.getMessage());
        }
    }

    private void validateInputs(String adAccountId, String accessToken) {
        if (adAccountId == null || adAccountId.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ad account ID is required");
        }
        if (!adAccountId.startsWith("act_")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ad account ID must start with act_");
        }
        if (accessToken == null || accessToken.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Facebook access token is required");
        }
    }

    private String createCampaign(String adAccountId, Campaign campaign, String accessToken) {
        String url = String.format("%s/v%s/%s/campaigns",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", campaign.getName());
        payload.put("objective", mapObjective(campaign.getObjective()));
        payload.put("status", "PAUSED");
        payload.put("special_ad_categories", "[]");
        payload.put("is_adset_budget_sharing_enabled", facebookProperties.isAdsetBudgetSharingEnabled());
        payload.put("access_token", accessToken);
        logPayload("campaign", payload, CAMPAIGN_ALLOWED_FIELDS);

        Map<String, Object> response = postForm(url, convertToForm(payload));
        return extractIdOrThrow(response, "campaign");
    }

    private String createAdSet(String adAccountId, Campaign campaign, Ad ad, String campaignId, String accessToken) {
        String url = String.format("%s/v%s/%s/adsets",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", campaign.getName() + " - Ad Set");
        payload.put("campaign_id", campaignId);
        boolean adsetBudgetSharing = facebookProperties.isAdsetBudgetSharingEnabled();
        payload.put("is_adset_budget_sharing_enabled", adsetBudgetSharing);

        if (!adsetBudgetSharing) {
            payload.put("daily_budget", formatBudgetForMeta(resolveAdsetBudget(campaign)));
        }
        Long bidAmount = resolveBidAmount(campaign);
        if (bidAmount != null && bidAmount > 0) {
            payload.put("bid_amount", bidAmount.toString());
        }

        payload.put("start_time", formatDateTime(campaign.getStartDate()));
        if (campaign.getEndDate() != null) {
            payload.put("end_time", formatDateTime(campaign.getEndDate()));
        }
        payload.put("billing_event", "IMPRESSIONS");
        payload.put("optimization_goal", resolveOptimizationGoal(campaign));
        payload.put("status", "PAUSED");
        payload.put("promoted_object", buildPromotedObject(ad));
        payload.put("targeting", buildTargeting(campaign));
        payload.put("access_token", accessToken);
        logPayload("adset", payload, ADSET_ALLOWED_FIELDS);

        Map<String, Object> response = postForm(url, convertToForm(payload));
        return extractIdOrThrow(response, "ad set");
    }

    private String createAdCreative(String adAccountId, Ad ad, String accessToken) {
        String url = String.format("%s/v%s/%s/adcreatives",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        Map<String, Object> linkData = new HashMap<>();
        linkData.put("name", ad.getHeadline());
        linkData.put("message", ad.getPrimaryText());
        linkData.put("link", resolveWebsiteUrl(ad));
        linkData.put("description", ad.getDescription());
        String resolvedImageUrl = resolveAbsoluteImageUrl(ad.getImageUrl());
        String imageHash = uploadImageToAdAccount(adAccountId, resolvedImageUrl, accessToken);
        if (StringUtils.hasText(imageHash)) {
            linkData.put("image_hash", imageHash);
        } else if (StringUtils.hasText(resolvedImageUrl)) {
            linkData.put("picture", resolvedImageUrl);
        }
        Map<String, Object> ctaMap = new HashMap<>();
        ctaMap.put("type", mapCallToAction(ad.getCallToAction()));
        linkData.put("call_to_action", ctaMap);

        Map<String, Object> objectStorySpec = new HashMap<>();
        if (StringUtils.hasText(facebookProperties.getPageId())) {
            objectStorySpec.put("page_id", facebookProperties.getPageId()); // optional if you have a page
        }
        objectStorySpec.put("link_data", linkData);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", ad.getName());
        payload.put("object_story_spec", JsonUtils.toJson(objectStorySpec));
        payload.put("access_token", accessToken);
        logPayload("creative", payload, CREATIVE_ALLOWED_FIELDS);

        Map<String, Object> response = postForm(url, convertToForm(payload));
        return extractIdOrThrow(response, "creative");
    }

    private String createAd(String adAccountId, Ad ad, String adSetId, String creativeId, String accessToken) {
        String url = String.format("%s/v%s/%s/ads",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", ad.getName());
        payload.put("adset_id", adSetId);
        payload.put("creative", String.format("{\"creative_id\":\"%s\"}", creativeId));
        payload.put("status", "PAUSED");
        payload.put("access_token", accessToken);
        logPayload("ad", payload, AD_ALLOWED_FIELDS);

        Map<String, Object> response = postForm(url, convertToForm(payload));
        return extractIdOrThrow(response, "ad");
    }

    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw buildFacebookApiException(response.getBody(), response.getStatusCode());
            }
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw parseFacebookApiException(ex);
        }
    }


    private String extractIdOrThrow(Map<String, Object> response, String entityName) {
        Object id = response.get("id");
        if (id == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Facebook API did not return " + entityName + " id: " + response);
        }
        return id.toString();
    }

    private MultiValueMap<String, String> convertToForm(Map<String, Object> payload) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        payload.forEach((key, value) -> {
            if (value != null) {
                form.add(key, value.toString());
            }
        });
        return form;
    }

    private void logPayload(String entityType, Map<String, Object> payload, Set<String> allowedFields) {
        if (!facebookProperties.isDebugPayloads()) {
            return;
        }
        try {
            String json = JsonUtils.toJson(payload);
            Set<String> unknown = new HashSet<>(payload.keySet());
            unknown.removeAll(allowedFields);
            log.info("[FB Export][{}] Payload sent: {}", entityType, json);
            if (!unknown.isEmpty()) {
                log.warn("[FB Export][{}] Unknown/unsupported fields detected: {}. Allowed fields: {}", entityType, unknown, allowedFields);
            }
        } catch (Exception e) {
            log.warn("[FB Export][{}] Failed to log payload: {}", entityType, e.getMessage());
        }
    }

    private String mapObjective(Campaign.CampaignObjective objective) {
        if (objective == null) return "OUTCOME_TRAFFIC";
        switch (objective) {
            case BRAND_AWARENESS:
            case REACH:
            case STORE_TRAFFIC:
                return "OUTCOME_AWARENESS";
            case TRAFFIC:
                return "OUTCOME_TRAFFIC";
            case ENGAGEMENT:
            case VIDEO_VIEWS:
                return "OUTCOME_ENGAGEMENT";
            case APP_INSTALLS:
                return "OUTCOME_APP_PROMOTION";
            case LEAD_GENERATION:
                return "OUTCOME_LEADS";
            case CONVERSIONS:
            case CATALOG_SALES:
                return "OUTCOME_SALES";
            default:
                return "OUTCOME_TRAFFIC";
        }
    }

    private String mapOptimizationGoal(Campaign.CampaignObjective objective) {
        if (objective == null) return "LINK_CLICKS";
        switch (objective) {
            case TRAFFIC:
                return "LINK_CLICKS";
            case LEAD_GENERATION:
                return "LEAD_GENERATION";
            case CONVERSIONS:
            case CATALOG_SALES:
                return "OFFSITE_CONVERSIONS";
            default:
                return "LINK_CLICKS";
        }
    }

    private String resolveOptimizationGoal(Campaign campaign) {
        if (campaign != null && StringUtils.hasText(campaign.getPerformanceGoal())) {
            return campaign.getPerformanceGoal().trim().toUpperCase(Locale.ROOT);
        }
        return mapOptimizationGoal(campaign != null ? campaign.getObjective() : null);
    }

    private String mapCallToAction(FacebookCTA cta) {
        if (cta == null) return "LEARN_MORE";
        return cta.name();
    }

    private String formatBudgetForMeta(Double budget) {
        if (budget == null || budget <= 0) {
            return "0";
        }
        long smallest = Math.round(budget * getBudgetMultiplier());
        return String.valueOf(smallest);
    }

    private Double resolveAdsetBudget(Campaign campaign) {
        Double budget = null;
        if (campaign.getDailyBudget() != null && campaign.getDailyBudget() > 0) {
            budget = campaign.getDailyBudget();
        } else if (campaign.getBudgetType() == Campaign.BudgetType.DAILY && campaign.getBudget() != null) {
            budget = campaign.getBudget();
        } else if (campaign.getTotalBudget() != null) {
            budget = campaign.getTotalBudget();
        }
        budget = normalizeBudgetForCurrency(budget);
        return budget != null ? budget : 0.0;
    }

    private Long resolveBidAmount(Campaign campaign) {
        Double manualBid = campaign.getBidCap();
        if (manualBid != null && manualBid > 0) {
            double normalizedManual = normalizeBudgetForCurrency(manualBid);
            long manualSmallest = Math.round(normalizedManual * getBudgetMultiplier());
            if (manualSmallest > 0) {
                return clampBid(manualSmallest);
            }
        }

        Double source = resolveAdsetBudget(campaign);
        if (source == null || source <= 0) {
            source = campaign.getDailyBudget();
        }
        if (source == null || source <= 0) {
            source = campaign.getTotalBudget();
        }
        if (source == null || source <= 0) {
            return null;
        }
        source = normalizeBudgetForCurrency(source);
        int multiplier = getBudgetMultiplier();
        double minBidCurrency = getMinBidAmount() / (double) multiplier;
        double baseBid = Math.max(source * 0.1, minBidCurrency);
        long smallest = Math.round(baseBid * multiplier);
        if (smallest <= 0) {
            return null;
        }
        return clampBid(smallest);
    }

    private long clampBid(long smallest) {
        long minBid = getMinBidAmount();
        long maxBid = getMaxBidAmount();
        long clamped = smallest;
        if (smallest < minBid) {
            clamped = minBid;
        } else if (smallest > maxBid) {
            log.warn("Bid amount {} exceeds Meta cap {} for currency {}. Clamping to allowed maximum.",
                smallest,
                maxBid,
                facebookProperties.getAccountCurrency());
            clamped = maxBid;
        }
        return clamped;
    }

    private int getBudgetMultiplier() {
        String currency = facebookProperties.getAccountCurrency();
        if ("VND".equalsIgnoreCase(currency)) {
            return 1;
        }
        return 100;
    }

    private long getMinBidAmount() {
        String currency = facebookProperties.getAccountCurrency();
        if ("VND".equalsIgnoreCase(currency)) {
            return 1000;
        }
        return 100;
    }

    private long getMaxBidAmount() {
        String currency = facebookProperties.getAccountCurrency();
        if ("VND".equalsIgnoreCase(currency)) {
            return MAX_BID_VND;
        }
        return MAX_BID_USD_CENTS;
    }

    private Double normalizeBudgetForCurrency(Double budget) {
        if (budget == null || budget <= 0) {
            return budget;
        }
        if ("VND".equalsIgnoreCase(facebookProperties.getAccountCurrency()) && budget < MIN_VND_DAILY_BUDGET) {
            double rate = facebookProperties.getLegacyUsdToVndRate() > 0
                ? facebookProperties.getLegacyUsdToVndRate()
                : DEFAULT_LEGACY_USD_TO_VND_RATE;
            double converted = budget * rate;
            log.debug("Normalized legacy USD budget {} using rate {} -> {}", budget, rate, converted);
            return converted;
        }
        return budget;
    }

    private String formatDateTime(java.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return ISO_FORMATTER.format(date.atStartOfDay());
    }

    private String buildPromotedObject(Ad ad) {
        Map<String, Object> promoted = new HashMap<>();
        if (StringUtils.hasText(facebookProperties.getPageId())) {
            promoted.put("page_id", facebookProperties.getPageId());
        }
        promoted.put("custom_event_type", "OTHER");
        if (StringUtils.hasText(facebookProperties.getPixelId())) {
            promoted.put("pixel_id", facebookProperties.getPixelId());
        }
        return JsonUtils.toJson(promoted);
    }

    private String buildTargeting(Campaign campaign) {
        Map<String, Object> targeting = new HashMap<>();
        Map<String, Object> geo = new HashMap<>();
        geo.put("countries", getCountries(campaign));
        targeting.put("geo_locations", geo);
        return JsonUtils.toJson(targeting);
    }

    private ApiException buildFacebookApiException(Map body, HttpStatus status) {
        String serialized = null;
        if (body != null) {
            try {
                serialized = objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException ignored) {
            }
        }
        String message = extractFacebookErrorMessage(serialized, status);
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private ApiException parseFacebookApiException(HttpStatusCodeException exception) {
        String responseBody = exception.getResponseBodyAsString();
        String message = extractFacebookErrorMessage(responseBody, exception.getStatusCode());
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private String extractFacebookErrorMessage(String responseBody, HttpStatus status) {
        String defaultMessage = "Facebook API error: " + status;
        if (!StringUtils.hasText(responseBody)) {
            return defaultMessage;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (errorNode.isMissingNode()) {
                return defaultMessage;
            }
            String userMessage = textOrNull(errorNode, "error_user_msg");
            String userTitle = textOrNull(errorNode, "error_user_title");
            String message = textOrNull(errorNode, "message");
            if (StringUtils.hasText(userMessage)) {
                return userMessage;
            }
            if (StringUtils.hasText(userTitle) && StringUtils.hasText(message)) {
                return userTitle + ": " + message;
            }
            if (StringUtils.hasText(message)) {
                return message;
            }
        } catch (Exception e) {
            log.warn("Failed to parse Facebook error response: {}", e.getMessage());
        }
        return defaultMessage;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : null;
    }

    private String uploadImageToAdAccount(String adAccountId, String imageUrl, String accessToken) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        try {
            byte[] bytes = downloadImageBytes(imageUrl);
            if (bytes == null || bytes.length == 0) {
                log.warn("Image download failed for {}", imageUrl);
                return null;
            }

            String url = String.format("%s/v%s/%s/adimages",
                facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("access_token", accessToken);
            form.add("bytes", Base64.getEncoder().encodeToString(bytes));

            Map<String, Object> response = postForm(url, form);
            Object imagesObj = response.get("images");
            if (imagesObj instanceof Map) {
                Object hash = ((Map<?, ?>) imagesObj).get("hash");
                return hash != null ? hash.toString() : null;
            }
            if (imagesObj instanceof List) {
                List<?> list = (List<?>) imagesObj;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    Object hash = ((Map<?, ?>) list.get(0)).get("hash");
                    return hash != null ? hash.toString() : null;
                }
            }
            log.warn("Unexpected response from adimages endpoint: {}", response);
            return null;
        } catch (Exception e) {
            log.warn("Failed to upload image for ad account {}: {}", adAccountId, e.getMessage());
            return null;
        }
    }

    private String resolveAbsoluteImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/api/images/")) {
            String filename = trimmed.substring("/api/images/".length());
            try {
                return minIOStorageService.getPublicUrl(filename);
            } catch (Exception e) {
                log.warn("Failed to get public URL for {}: {}", filename, e.getMessage());
                return null;
            }
        }
        if (trimmed.startsWith("uploads/")) {
            try {
                return minIOStorageService.getPublicUrl(trimmed.substring("uploads/".length()));
            } catch (Exception e) {
                log.warn("Failed to map uploads path {}: {}", trimmed, e.getMessage());
                return null;
            }
        }
        // TODO: handle other storage schemes if needed (e.g., relative paths or base64)
        return trimmed;
    }

    /**
     * Extract a list of country codes from campaign targetAudience.
     * Accepts JSON array (["US","VN"]) or comma/semicolon separated text.
     */
    private List<String> getCountries(Campaign campaign) {
        if (campaign == null || !StringUtils.hasText(campaign.getTargetAudience())) {
            return List.of("US");
        }

        String targetAudience = campaign.getTargetAudience();
        ObjectMapper mapper = new ObjectMapper();
        List<String> codes = new ArrayList<>();

        try {
            JsonNode node = mapper.readTree(targetAudience);
            if (node.isArray()) {
                node.forEach(item -> {
                    if (item.isTextual()) {
                        String code = resolveCountryCode(item.asText());
                        if (code != null) {
                            codes.add(code);
                        }
                    }
                });
            }
        } catch (JsonProcessingException ignored) {
            // Fall back to parsing as delimited string
        }

        if (codes.isEmpty()) {
            for (String part : targetAudience.split("[,;\\n]")) {
                String code = resolveCountryCode(part);
                if (code != null && !codes.contains(code)) {
                    codes.add(code);
                }
            }
        }

        return codes.isEmpty() ? List.of("US") : codes;
    }

    private String resolveCountryCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.matches("^[A-Z]{2}$")) {
            return upper;
        }
        return COUNTRY_CODE_ALIASES.getOrDefault(upper, null);
    }

    private byte[] downloadImageBytes(String url) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            log.warn("Failed to download image {} status {}", url, response.getStatusCode());
        } catch (Exception e) {
            log.warn("Error downloading image {}: {}", url, e.getMessage());
        }
        return null;
    }

    private String resolveWebsiteUrl(Ad ad) {
        if (StringUtils.hasText(ad.getWebsiteUrl())) {
            return ad.getWebsiteUrl();
        }
        if (StringUtils.hasText(facebookProperties.getDefaultLinkUrl())) {
            log.warn("Ad {} missing websiteUrl. Falling back to facebook.default-link-url.", ad.getId());
            return facebookProperties.getDefaultLinkUrl();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
            "Website URL is required for Facebook auto upload");
    }

    @Data
    public static class UploadResult {
        private String campaignId;
        private String adSetId;
        private String creativeId;
        private String adId;
        private String message;
    }
}
