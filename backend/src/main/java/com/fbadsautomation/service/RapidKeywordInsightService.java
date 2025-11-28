package com.fbadsautomation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.dto.KeywordLanguage;
import com.fbadsautomation.dto.KeywordLocation;
import com.fbadsautomation.dto.TrendingKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RapidKeywordInsightService {

    private static final Logger log = LoggerFactory.getLogger(RapidKeywordInsightService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rapidapi.keyword.insight.key:}")
    private String apiKey;

    @Value("${rapidapi.keyword.insight.host:google-keyword-insight1.p.rapidapi.com}")
    private String apiHost;

    @Value("${rapidapi.keyword.insight.base-url:https://google-keyword-insight1.p.rapidapi.com}")
    private String baseUrl;

    public RapidKeywordInsightService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return restTemplate != null
            && objectMapper != null
            && apiKey != null
            && !apiKey.isBlank()
            && baseUrl != null
            && !baseUrl.isBlank();
    }

    @Cacheable(
        value = "rapidLocations",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<KeywordLocation> fetchSupportedLocations() {
        if (!isConfigured()) {
            return Collections.emptyList();
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                buildUrl("/locations/"),
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                String.class
            );

            if (!response.hasBody() || response.getBody() == null) {
                return Collections.emptyList();
            }

            return objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<KeywordLocation>>() {}
            );
        } catch (Exception ex) {
            log.error("Failed to fetch RapidAPI locations: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @Cacheable(
        value = "rapidLanguages",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<KeywordLanguage> fetchSupportedLanguages() {
        if (!isConfigured()) {
            return Collections.emptyList();
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                buildUrl("/languages/"),
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                String.class
            );

            if (!response.hasBody() || response.getBody() == null) {
                return Collections.emptyList();
            }

            return objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<KeywordLanguage>>() {}
            );
        } catch (Exception ex) {
            log.error("Failed to fetch RapidAPI languages: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<TrendingKeyword> fetchTopKeywords(String query, String location, String language, int limit) {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(buildUrl("/topkeys/"))
                .queryParam("keyword", query)
                .queryParam("location", location)
                .queryParam("lang", language)
                .queryParam("num", limit);

            ResponseEntity<String> response = restTemplate.exchange(
                builder.build(true).toUri(),
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                String.class
            );

            if (!response.hasBody() || response.getBody() == null) {
                log.warn("RapidAPI returned empty body for query {}", query);
                return Collections.emptyList();
            }

            return parseTopKeywords(response.getBody(), location);
        } catch (Exception ex) {
            log.error("Failed to fetch top keywords from RapidAPI: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<TrendingKeyword> parseTopKeywords(String body, String region) throws Exception {
        var node = objectMapper.readTree(body);
        var resultsNode = node.has("result") ? node.get("result")
            : node.has("results") ? node.get("results")
            : node;
        if (resultsNode == null || !resultsNode.isArray()) {
            return Collections.emptyList();
        }

        List<TrendingKeyword> keywords = new ArrayList<>();
        for (var item : resultsNode) {
            String keywordValue = item.has("keyword") ? item.get("keyword").asText() : null;
            if (keywordValue == null || keywordValue.isBlank()) {
                continue;
            }

            Integer growth = item.has("trend") ? item.get("trend").asInt()
                : item.has("score") ? item.get("score").asInt()
                : 60;
            Long volume = item.has("volume") ? item.get("volume").asLong()
                : item.has("search_volume") ? item.get("search_volume").asLong()
                : 50000L;

            keywords.add(TrendingKeyword.builder()
                .keyword(keywordValue)
                .growth(growth)
                .region(region)
                .searchVolume(volume)
                .category(item.has("category") ? item.get("category").asText("RapidAPI") : "RapidAPI")
                .source("RapidAPI - Google Keyword Insight")
                .build());
        }
        return keywords;
    }

    private String buildUrl(String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + path;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey.trim());
        headers.set("x-rapidapi-host", apiHost.trim());
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "FBAdsAutomation/1.0");
        return headers;
    }
}
