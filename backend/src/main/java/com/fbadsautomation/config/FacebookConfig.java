package com.fbadsautomation.config;

import com.fbadsautomation.integration.facebook.FacebookProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class FacebookConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
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
