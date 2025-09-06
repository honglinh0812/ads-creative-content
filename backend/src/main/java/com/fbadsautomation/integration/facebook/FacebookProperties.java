package com.fbadsautomation.integration.facebook;

import lombok.Data;

@Data
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
    
    // Getter methods
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public String getAppSecret() {
        return appSecret;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public String getScope() {
        return scope;
    }
    
    // Setter methods
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
    
    public void setAppId(String appId) {
        this.appId = appId;
    }
    
    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
    
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
}
