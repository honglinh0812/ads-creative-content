package com.fbadsautomation.service;

import com.fbadsautomation.dto.TrendingKeyword;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Trends API Service using SerpApi
 * Requires: SERPAPI_KEY environment variable
 * Cost: $50/month for 5,000 searches
 * Free tier: 100 searches/month
 */
@Service
public class GoogleTrendsApiService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTrendsApiService.class);
    private static final String SERPAPI_BASE_URL = "https://serpapi.com/search";

    @Value("${serpapi.api.key:}")
    private String serpApiKey;

    private final RestTemplate restTemplate;

    public GoogleTrendsApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch trending keywords using SerpApi Google Trends API
     *
     * @param query Search query
     * @param region Target region (e.g., US, VN, UK)
     * @return List of trending keywords
     */
    public List<TrendingKeyword> fetchTrendsFromApi(String query, String region) {
        // Check if API key is configured
        if (serpApiKey == null || serpApiKey.isEmpty()) {
            log.warn("SerpApi key not configured. Using mock data fallback.");
            return null; // Will fallback to mock data
        }

        try {
            // Build SerpApi request URL
            String url = String.format(
                "%s?engine=google_trends&q=%s&geo=%s&api_key=%s",
                SERPAPI_BASE_URL,
                query,
                region,
                serpApiKey
            );

            log.info("Fetching Google Trends data from SerpApi: query={}, region={}", query, region);

            // Make API request
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Facebook Ads Automation/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            // Parse response
            return parseSerpApiResponse(response.getBody(), region);

        } catch (Exception e) {
            log.error("Error fetching trends from SerpApi: {}", e.getMessage(), e);
            return null; // Will fallback to mock data
        }
    }

    /**
     * Parse SerpApi response to extract trending keywords
     */
    private List<TrendingKeyword> parseSerpApiResponse(String responseBody, String region) {
        List<TrendingKeyword> trends = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(responseBody);

            // Extract interest over time data
            if (json.has("interest_over_time")) {
                JSONObject interestOverTime = json.getJSONObject("interest_over_time");
                if (interestOverTime.has("timeline_data")) {
                    JSONArray timelineData = interestOverTime.getJSONArray("timeline_data");

                    // Get latest data point
                    if (timelineData.length() > 0) {
                        JSONObject latest = timelineData.getJSONObject(timelineData.length() - 1);
                        if (latest.has("values")) {
                            JSONArray values = latest.getJSONArray("values");
                            for (int i = 0; i < values.length(); i++) {
                                JSONObject value = values.getJSONObject(i);
                                String keyword = value.optString("query", "");
                                int searchVolume = value.optInt("value", 0);

                                // Calculate growth (simplified)
                                int growth = Math.min(searchVolume, 100); // Normalize to percentage

                                trends.add(TrendingKeyword.builder()
                                    .keyword(keyword)
                                    .growth(growth)
                                    .region(region)
                                    .searchVolume((long) searchVolume * 1000)
                                    .category("Real-time")
                                    .build());
                            }
                        }
                    }
                }
            }

            // Extract related queries
            if (json.has("related_queries") && trends.isEmpty()) {
                JSONObject relatedQueries = json.getJSONObject("related_queries");
                if (relatedQueries.has("rising")) {
                    JSONArray rising = relatedQueries.getJSONArray("rising");
                    for (int i = 0; i < Math.min(rising.length(), 10); i++) {
                        JSONObject item = rising.getJSONObject(i);
                        String keyword = item.optString("query", "");
                        int growth = item.optInt("value", 0);

                        trends.add(TrendingKeyword.builder()
                            .keyword(keyword)
                            .growth(growth)
                            .region(region)
                            .searchVolume(100000L)
                            .category("Rising")
                            .build());
                    }
                }
            }

            log.info("Parsed {} trends from SerpApi response", trends.size());

        } catch (Exception e) {
            log.error("Error parsing SerpApi response: {}", e.getMessage(), e);
        }

        return trends;
    }

    /**
     * Check if API is configured and available
     */
    public boolean isApiAvailable() {
        return serpApiKey != null && !serpApiKey.isEmpty();
    }
}
