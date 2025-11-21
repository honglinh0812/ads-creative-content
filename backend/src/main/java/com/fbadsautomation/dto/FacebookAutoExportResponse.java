package com.fbadsautomation.dto;

import com.fbadsautomation.integration.facebook.FacebookMarketingApiClient;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Response returned after automatically exporting ads to Facebook.
 * Includes the structured payload that was sent, API upload results, and the redirect URL for Ads Manager.
 */
@Value
@Builder
public class FacebookAutoExportResponse {
    List<FacebookAdPayload> payloads;
    List<FacebookMarketingApiClient.UploadResult> results;
    String adsManagerUrl;
}
