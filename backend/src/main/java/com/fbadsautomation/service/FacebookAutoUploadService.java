package com.fbadsautomation.service;

import com.fbadsautomation.dto.FacebookAutoExportResponse;
import com.fbadsautomation.integration.facebook.FacebookMarketingApiClient;
import com.fbadsautomation.integration.facebook.FacebookProperties;
import com.fbadsautomation.model.Ad;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacebookAutoUploadService {
    private static final Logger log = LoggerFactory.getLogger(FacebookAutoUploadService.class);
    private static final String ADS_MANAGER_BASE_URL = "https://business.facebook.com/adsmanager/manage/ads";

    private final FacebookProperties facebookProperties;
    private final FacebookMarketingApiClient marketingApiClient;

    public FacebookAutoExportResponse autoUpload(List<Ad> ads, String adAccountId) {
        if (ads == null || ads.isEmpty()) {
            return skipped("No ads provided for auto upload");
        }

        if (!StringUtils.hasText(facebookProperties.getMarketingAccessToken())) {
            return skipped("Facebook Marketing access token is missing. Set FACEBOOK_MARKETING_ACCESS_TOKEN env.");
        }

        String resolvedAdAccountId = resolveAdAccountId(adAccountId);
        if (!StringUtils.hasText(resolvedAdAccountId)) {
            return skipped("Ad account id is required. Provide act_... or set FACEBOOK_DEFAULT_AD_ACCOUNT_ID env.");
        }

        List<FacebookMarketingApiClient.UploadResult> results = new ArrayList<>();
        try {
            // TODO: Confirm if Facebook Marketing API supports importing ads from files for this use case.
            for (Ad ad : ads) {
                var result = marketingApiClient.uploadAdToAccount(
                    ad,
                    resolvedAdAccountId,
                    facebookProperties.getMarketingAccessToken()
                );
                results.add(result);
            }

            return FacebookAutoExportResponse.builder()
                .status(FacebookAutoExportResponse.AutoUploadStatus.UPLOADED)
                .message("Uploaded via Marketing API")
                .results(results)
                .adsManagerUrl(buildAdsManagerUrl(resolvedAdAccountId))
                .build();

        } catch (Exception e) {
            log.error("Auto upload failed: {}", e.getMessage(), e);
            return FacebookAutoExportResponse.builder()
                .status(FacebookAutoExportResponse.AutoUploadStatus.FAILED)
                .message(e.getMessage())
                .results(results)
                .adsManagerUrl(buildAdsManagerUrl(resolvedAdAccountId))
                .build();
        }
    }

    public FacebookAutoExportResponse skipped(String reason) {
        return FacebookAutoExportResponse.builder()
            .status(FacebookAutoExportResponse.AutoUploadStatus.SKIPPED)
            .message(reason)
            .adsManagerUrl(ADS_MANAGER_BASE_URL)
            .build();
    }

    private String resolveAdAccountId(String requestAccountId) {
        if (StringUtils.hasText(requestAccountId)) {
            return requestAccountId;
        }
        return facebookProperties.getDefaultAdAccountId();
    }

    private String buildAdsManagerUrl(String adAccountId) {
        if (!StringUtils.hasText(adAccountId)) {
            return ADS_MANAGER_BASE_URL;
        }
        String normalized = adAccountId.startsWith("act_") ? adAccountId.substring(4) : adAccountId;
        return ADS_MANAGER_BASE_URL + "?act=" + normalized;
    }
}
