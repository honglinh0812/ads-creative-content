package com.fbadsautomation.integration.facebook;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j

/**
 * Configuration properties for Facebook integration
 */
public class FacebookProperties {
    private String apiUrl;
    private String apiVersion;
    private String appId;
    private String appSecret;
    private String redirectUri;
    private String scope;
}
