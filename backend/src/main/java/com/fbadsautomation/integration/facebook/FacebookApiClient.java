package com.fbadsautomation.integration.facebook;

import com.fbadsautomation.exception.ApiException;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Component;

import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor

public class FacebookApiClient {

    private final RestTemplate restTemplate;
    private final FacebookProperties facebookProperties;

    public String getAccessToken(String code, String redirectUri) {
        String tokenUrl = String.format("%s/v%s/oauth/access_token?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s",
            facebookProperties.getApiUrl(),
            facebookProperties.getApiVersion(),
            facebookProperties.getAppId(),
            redirectUri,
            facebookProperties.getAppSecret(),
            code
        );
        try {
            Map<String, Object> response = restTemplate.getForObject(tokenUrl, Map.class);
            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            } else {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get access token from Facebook");
            }
        } catch (Exception e) {
            log.error("Error getting access token from Facebook: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting access token from Facebook: " + e.getMessage());
        }
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = String.format("%s/v%s/me?fields = id,name,email,picture&access_token=%s",
            facebookProperties.getApiUrl(),
            facebookProperties.getApiVersion(),
            accessToken
        );
        try {
            Map<String, Object> response = restTemplate.getForObject(userInfoUrl, Map.class);
            if (response != null && response.containsKey("id")) {
                return response;
            } else {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get user info from Facebook");
            }
        } catch (Exception e) {
            log.error("Error getting user info from Facebook: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting user info from Facebook: " + e.getMessage());
        }
    }
}
