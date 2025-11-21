package com.fbadsautomation.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Normalized Facebook ad payload used for either CSV export or direct Marketing API uploads.
 * Keeps campaign/ad set/ad creative properties grouped for simpler integrations.
 */
@Value
@Builder
public class FacebookAdPayload {
    CampaignPayload campaign;
    AdSetPayload adSet;
    CreativePayload creative;
    AdPayload ad;

    @Value
    @Builder
    public static class CampaignPayload {
        String name;
        String status;
        String objective;
        String buyingType;
        String dailyBudget;
        String lifetimeBudget;
        String startTime;
        String endTime;
    }

    @Value
    @Builder
    public static class AdSetPayload {
        String name;
        String status;
        String startTime;
        String endTime;
        String link;
        String countries;
        String gender;
        String ageMin;
        String ageMax;
        String publisherPlatforms;
        String facebookPositions;
        String instagramPositions;
        String optimizationGoal;
        String billingEvent;
    }

    @Value
    @Builder
    public static class CreativePayload {
        String type;
        String headline;
        String body;
        String description;
        String displayLink;
        String imageUrl;
        String callToAction;
        String marketingMessage;
    }

    @Value
    @Builder
    public static class AdPayload {
        Long adId;
        String name;
        String status;
        String websiteUrl;
    }
}
