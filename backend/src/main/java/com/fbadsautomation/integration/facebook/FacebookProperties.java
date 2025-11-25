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
    private String marketingAccessToken;
    private String pageId;
    private String pixelId;
    private String defaultAdAccountId;
    private String defaultLinkUrl;
    private String accountCurrency;
    private boolean adsetBudgetSharingEnabled;
    private boolean debugPayloads;
    
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

    public String getMarketingAccessToken() {
        return marketingAccessToken;
    }

    public String getPageId() {
        return pageId;
    }

    public String getPixelId() {
        return pixelId;
    }

    public String getDefaultAdAccountId() {
        return defaultAdAccountId;
    }
    public String getDefaultLinkUrl() { return defaultLinkUrl; }
    public String getAccountCurrency() { return accountCurrency; }

    public boolean isAdsetBudgetSharingEnabled() {
        return adsetBudgetSharingEnabled;
    }

    public boolean isDebugPayloads() {
        return debugPayloads;
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

    public void setMarketingAccessToken(String marketingAccessToken) {
        this.marketingAccessToken = marketingAccessToken;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public void setPixelId(String pixelId) {
        this.pixelId = pixelId;
    }

    public void setDefaultAdAccountId(String defaultAdAccountId) {
        this.defaultAdAccountId = defaultAdAccountId;
    }
    public void setDefaultLinkUrl(String defaultLinkUrl) { this.defaultLinkUrl = defaultLinkUrl; }
    public void setAccountCurrency(String accountCurrency) { this.accountCurrency = accountCurrency; }

    public void setAdsetBudgetSharingEnabled(boolean adsetBudgetSharingEnabled) {
        this.adsetBudgetSharingEnabled = adsetBudgetSharingEnabled;
    }

    public void setDebugPayloads(boolean debugPayloads) {
        this.debugPayloads = debugPayloads;
    }
}
