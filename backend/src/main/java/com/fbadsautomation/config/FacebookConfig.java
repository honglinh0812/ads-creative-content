package com.fbadsautomation.config;

import com.fbadsautomation.integration.facebook.FacebookProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration

public class FacebookConfig {

    @Autowired
    private Environment environment;
    
    @Bean
    public FacebookProperties facebookProperties() {
        FacebookProperties properties = new FacebookProperties();
        properties.setApiUrl(environment.getProperty("facebook.api-url", "https://graph.facebook.com"));
        properties.setApiVersion(environment.getProperty("facebook.api-version", "24.0"));
        properties.setAppId(environment.getProperty("spring.security.oauth2.client.registration.facebook.client-id"));
        properties.setAppSecret(environment.getProperty("spring.security.oauth2.client.registration.facebook.client-secret"));
        properties.setScope(environment.getProperty("facebook.scope", "email,public_profile,ads_management,business_management"));
        properties.setRedirectUri(environment.getProperty("spring.security.oauth2.client.registration.facebook.redirect-uri"));
        properties.setMarketingAccessToken(environment.getProperty("facebook.marketing.access-token", ""));
        properties.setPageId(environment.getProperty("facebook.page-id", ""));
        properties.setPixelId(environment.getProperty("facebook.pixel-id", ""));
        properties.setDefaultAdAccountId(environment.getProperty("facebook.default-ad-account-id", ""));
        properties.setDefaultLinkUrl(environment.getProperty("facebook.default-link-url", ""));
        properties.setAdsetBudgetSharingEnabled(Boolean.parseBoolean(
            environment.getProperty("facebook.adset-budget-sharing-enabled", "false")));
        properties.setDebugPayloads(Boolean.parseBoolean(
            environment.getProperty("facebook.debug-payloads", "false")));
        return properties;
    }
}
