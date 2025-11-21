package com.fbadsautomation.dto;

import com.fbadsautomation.integration.facebook.FacebookMarketingApiClient;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Response returned after automatically exporting ads to Facebook.
 * Captures status, message, API upload results, and the redirect URL for Ads Manager.
 */
@Value
@Builder
public class FacebookAutoExportResponse {
    AutoUploadStatus status;
    String message;
    List<FacebookMarketingApiClient.UploadResult> results;
    String adsManagerUrl;

    public enum AutoUploadStatus {
        SKIPPED,
        UPLOADED,
        FAILED
    }
}
