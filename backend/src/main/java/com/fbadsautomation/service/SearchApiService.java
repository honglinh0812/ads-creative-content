package com.fbadsautomation.service;

import com.fbadsautomation.dto.search.SearchApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Lightweight wrapper around SearchAPI.io.
 * Supports multiple engines (google, youtube, tiktok, etc.) through a single entry point.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${searchapi.base-url:https://www.searchapi.io/api/v1/search}")
    private String baseUrl;

    @Value("${searchapi.api-key:}")
    private String apiKey;

    public SearchApiResponse search(String engine, String query, Map<String, String> extraParams, int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("SearchAPI key is not configured. Set SEARCH_API_KEY env variable.");
        }
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("engine", engine);
            if (query != null && !query.isBlank()) {
                params.add("q", query);
            }
            params.add("api_key", apiKey);
            if (extraParams != null) {
                extraParams.forEach((key, value) -> {
                    if (value != null && !value.isBlank()) {
                        params.add(key, value);
                    }
                });
            }

            URI uri = new URI(baseUrl + buildQuery(params));
            ResponseEntity<SearchApiResponse> response = restTemplate.getForEntity(uri, SearchApiResponse.class);
            SearchApiResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response from SearchAPI");
            }
            body.trimResults(limit);
            return body;
        } catch (HttpClientErrorException e) {
            HttpStatus status = e.getStatusCode();
            log.error("SearchAPI returned error status {}: {}", status.value(), e.getResponseBodyAsString());
            throw new IllegalStateException("SearchAPI error: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException | URISyntaxException e) {
            log.error("Failed to call SearchAPI", e);
            throw new IllegalStateException("Unable to reach SearchAPI: " + e.getMessage(), e);
        }
    }

    private String buildQuery(MultiValueMap<String, String> params) {
        StringBuilder builder = new StringBuilder("?");
        params.forEach((key, values) -> {
            for (String value : values) {
                if (builder.length() > 1) {
                    builder.append('&');
                }
                builder.append(key).append('=').append(value.replace(" ", "+"));
            }
        });
        return builder.toString();
    }
}
