package com.fbadsautomation.integration.facebook;

import lombok.Data;

/**
 * Configuration properties for Facebook integration
 */
@Data
public class FacebookProperties {
    private String apiUrl;
    private String apiVersion;
    private String appId;
    private String appSecret;
    private String redirectUri;
    private String scope;
}
