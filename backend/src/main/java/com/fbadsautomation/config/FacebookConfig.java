package com.fbadsautomation.config;

import com.fbadsautomation.integration.facebook.FacebookProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration

public class FacebookConfig {

    // RestTemplate bean moved to RestTemplateConfig to avoid conflicts
    
    @Bean
    public FacebookProperties facebookProperties() {
        FacebookProperties properties = new FacebookProperties();
        properties.setApiUrl("https://graph.facebook.com");
        properties.setApiVersion("18.0");
        properties.setAppId("${spring.security.oauth2.client.registration.facebook.client-id}");
        properties.setAppSecret("${spring.security.oauth2.client.registration.facebook.client-secret}");
        properties.setScope("email, public_profile");
        properties.setRedirectUri("${spring.security.oauth2.client.registration.facebook.redirect-uri}");
        return properties;
    }
}
