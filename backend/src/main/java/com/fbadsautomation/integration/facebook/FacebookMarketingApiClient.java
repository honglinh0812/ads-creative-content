package com.fbadsautomation.integration.facebook;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.FacebookCTA;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight Facebook Marketing API client to create campaign/adset/ad for a given ad account.
 * Note: Requires access token with ads_management (dev mode is fine for single tester).
 * This is intentionally minimal and best-effort; it returns detailed error messages for frontend fallback.
 */
@Component
@RequiredArgsConstructor
public class FacebookMarketingApiClient {

    private static final Logger log = LoggerFactory.getLogger(FacebookMarketingApiClient.class);

    private final RestTemplate restTemplate;
    private final FacebookProperties facebookProperties;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public UploadResult uploadAdToAccount(Ad ad, String adAccountId, String accessToken) {
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

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", campaign.getName());
        form.add("objective", mapObjective(campaign.getObjective()));
        form.add("status", "PAUSED"); // create in paused state for safety
        form.add("special_ad_categories", "[]");
        form.add("access_token", accessToken);

        Map<String, Object> response = postForm(url, form);
        return extractIdOrThrow(response, "campaign");
    }

    private String createAdSet(String adAccountId, Campaign campaign, Ad ad, String campaignId, String accessToken) {
        String url = String.format("%s/v%s/%s/adsets",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", campaign.getName() + " - Ad Set");
        form.add("campaign_id", campaignId);
        boolean adsetBudgetSharing = facebookProperties.isAdsetBudgetSharingEnabled();
        form.add("is_adset_budget_sharing_enabled", String.valueOf(adsetBudgetSharing));

        if (!adsetBudgetSharing) {
            String dailyBudget = formatBudgetForMeta(resolveAdsetBudget(campaign));
            form.add("daily_budget", dailyBudget);
        }

        form.add("start_time", ISO_FORMATTER.format(campaign.getStartDate()));
        if (campaign.getEndDate() != null) {
            form.add("end_time", ISO_FORMATTER.format(campaign.getEndDate()));
        }
        form.add("billing_event", "IMPRESSIONS");
        form.add("optimization_goal", mapOptimizationGoal(campaign.getObjective()));
        form.add("status", "PAUSED");
        form.add("promoted_object", buildPromotedObject(ad));
        form.add("targeting", buildTargeting(campaign));
        form.add("access_token", accessToken);

        Map<String, Object> response = postForm(url, form);
        return extractIdOrThrow(response, "ad set");
    }

    private String createAdCreative(String adAccountId, Ad ad, String accessToken) {
        String url = String.format("%s/v%s/%s/adcreatives",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        Map<String, Object> linkData = new HashMap<>();
        linkData.put("name", ad.getHeadline());
        linkData.put("message", ad.getPrimaryText());
        linkData.put("link", ad.getWebsiteUrl());
        linkData.put("description", ad.getDescription());
        Map<String, Object> ctaMap = new HashMap<>();
        ctaMap.put("type", mapCallToAction(ad.getCallToAction()));
        linkData.put("call_to_action", ctaMap);
        if (ad.getImageUrl() != null) {
            linkData.put("image_url", ad.getImageUrl());
        }

        Map<String, Object> objectStorySpec = new HashMap<>();
        objectStorySpec.put("page_id", facebookProperties.getPageId()); // optional if you have a page
        objectStorySpec.put("link_data", linkData);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", ad.getName());
        form.add("object_story_spec", JsonUtils.toJson(objectStorySpec));
        form.add("access_token", accessToken);

        Map<String, Object> response = postForm(url, form);
        return extractIdOrThrow(response, "creative");
    }

    private String createAd(String adAccountId, Ad ad, String adSetId, String creativeId, String accessToken) {
        String url = String.format("%s/v%s/%s/ads",
            facebookProperties.getApiUrl(), facebookProperties.getApiVersion(), adAccountId);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", ad.getName());
        form.add("adset_id", adSetId);
        form.add("creative", String.format("{\"creative_id\":\"%s\"}", creativeId));
        form.add("status", "PAUSED");
        form.add("access_token", accessToken);

        Map<String, Object> response = postForm(url, form);
        return extractIdOrThrow(response, "ad");
    }

    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Facebook API error: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private String extractIdOrThrow(Map<String, Object> response, String entityName) {
        Object id = response.get("id");
        if (id == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Facebook API did not return " + entityName + " id: " + response);
        }
        return id.toString();
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
            case BRAND_AWARENESS:
            case REACH:
            case STORE_TRAFFIC:
                return "REACH";
            case TRAFFIC:
                return "LINK_CLICKS";
            case ENGAGEMENT:
                return "POST_ENGAGEMENT";
            case VIDEO_VIEWS:
                return "THRUPLAY";
            case APP_INSTALLS:
                return "APP_INSTALLS";
            case LEAD_GENERATION:
                return "LEAD_GENERATION";
            case CONVERSIONS:
            case CATALOG_SALES:
                return "OFFSITE_CONVERSIONS";
            default:
                return "LINK_CLICKS";
        }
    }

    private String mapCallToAction(FacebookCTA cta) {
        if (cta == null) return "LEARN_MORE";
        return cta.name();
    }

    private String formatBudgetForMeta(Double budget) {
        if (budget == null || budget <= 0) {
            return "0";
        }
        // convert to smallest currency unit (assume USD cents or VND unit depending on account currency)
        long smallest = Math.round(budget * 100);
        return String.valueOf(smallest);
    }

    private Double resolveAdsetBudget(Campaign campaign) {
        if (campaign.getDailyBudget() != null && campaign.getDailyBudget() > 0) {
            return campaign.getDailyBudget();
        }
        if (campaign.getBudgetType() == Campaign.BudgetType.DAILY && campaign.getBudget() != null) {
            return campaign.getBudget();
        }
        if (campaign.getTotalBudget() != null) {
            return campaign.getTotalBudget();
        }
        return 0.0;
    }

    private String buildPromotedObject(Ad ad) {
        Map<String, Object> promoted = new HashMap<>();
        promoted.put("page_id", facebookProperties.getPageId());
        promoted.put("custom_event_type", "OTHER");
        promoted.put("pixel_id", facebookProperties.getPixelId());
        return JsonUtils.toJson(promoted);
    }

    private String buildTargeting(Campaign campaign) {
        Map<String, Object> targeting = new HashMap<>();
        Map<String, Object> geo = new HashMap<>();
        geo.put("countries", getCountries(campaign));
        targeting.put("geo_locations", geo);
        return JsonUtils.toJson(targeting);
    }

    /**
     * Extract a list of country codes from campaign targetAudience.
     * Accepts JSON array (["US","VN"]) or comma/semicolon separated text.
     */
    private List<String> getCountries(Campaign campaign) {
        if (campaign == null || campaign.getTargetAudience() == null || campaign.getTargetAudience().isBlank()) {
            return List.of("VN");
        }

        String targetAudience = campaign.getTargetAudience();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode node = mapper.readTree(targetAudience);
            if (node.isArray()) {
                List<String> countries = new ArrayList<>();
                node.forEach(item -> {
                    if (item.isTextual()) {
                        String code = item.asText().trim();
                        if (!code.isEmpty()) {
                            countries.add(code.toUpperCase());
                        }
                    }
                });
                if (!countries.isEmpty()) {
                    return countries;
                }
            }
        } catch (JsonProcessingException ignored) {
            
        }

        List<String> countries = new ArrayList<>();
        for (String part : targetAudience.split("[,;]")) {
            String code = part.trim();
            if (!code.isEmpty()) {
                countries.add(code.toUpperCase());
            }
        }

        return countries.isEmpty() ? List.of("US") : countries;
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
