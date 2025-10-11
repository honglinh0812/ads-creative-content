package com.fbadsautomation.service;

import com.fbadsautomation.dto.TrendingKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TrendingKeywordsService {

    private static final Logger log = LoggerFactory.getLogger(TrendingKeywordsService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GoogleTrendsApiService googleTrendsApiService;

    /**
     * Fetch trending keywords for a query and region
     * Results are cached for 60 minutes
     *
     * @param query Search query
     * @param region Target region (e.g., US, VN, UK)
     * @return List of trending keywords
     */
    @Cacheable(value = "trends", key = "#query + '_' + #region", unless = "#result == null")
    public List<TrendingKeyword> fetchTrends(String query, String region) {
        log.info("Fetching trends for query: {} in region: {}", query, region);

        // Check cache first
        String cacheKey = "trends:" + query + ":" + region;
        @SuppressWarnings("unchecked")
        List<TrendingKeyword> cachedTrends = (List<TrendingKeyword>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedTrends != null && !cachedTrends.isEmpty()) {
            log.info("Returning cached trends for {}", cacheKey);
            return cachedTrends;
        }

        // Try to fetch from real API first
        List<TrendingKeyword> trends = null;
        if (googleTrendsApiService.isApiAvailable()) {
            log.info("Fetching trends from Google Trends API via SerpApi");
            trends = googleTrendsApiService.fetchTrendsFromApi(query, region);
        }

        // Fallback to mock data if API unavailable or fails
        if (trends == null || trends.isEmpty()) {
            log.info("Using mock trends data (API not available or failed)");
            trends = generateMockTrends(query, region);
        }

        // Cache for 60 minutes
        redisTemplate.opsForValue().set(cacheKey, trends, 60, TimeUnit.MINUTES);
        log.info("Cached {} trends for {}", trends.size(), cacheKey);

        return trends;
    }

    /**
     * Generate mock trending keywords
     * This should be replaced with actual API integration
     */
    private List<TrendingKeyword> generateMockTrends(String query, String region) {
        List<TrendingKeyword> trends = new ArrayList<>();

        // Generate contextual mock keywords based on query
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("tech") || lowerQuery.contains("gadget") || lowerQuery.contains("phone")) {
            trends.add(TrendingKeyword.builder()
                    .keyword("AI smartphone")
                    .growth(85)
                    .region(region)
                    .searchVolume(125000L)
                    .category("Technology")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("5G connectivity")
                    .growth(72)
                    .region(region)
                    .searchVolume(98000L)
                    .category("Technology")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("wireless charging")
                    .growth(65)
                    .region(region)
                    .searchVolume(87000L)
                    .category("Technology")
                    .build());
        } else if (lowerQuery.contains("fashion") || lowerQuery.contains("clothing") || lowerQuery.contains("apparel")) {
            trends.add(TrendingKeyword.builder()
                    .keyword("sustainable fashion")
                    .growth(92)
                    .region(region)
                    .searchVolume(145000L)
                    .category("Fashion")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("vintage style")
                    .growth(78)
                    .region(region)
                    .searchVolume(112000L)
                    .category("Fashion")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("minimalist wardrobe")
                    .growth(68)
                    .region(region)
                    .searchVolume(95000L)
                    .category("Fashion")
                    .build());
        } else if (lowerQuery.contains("food") || lowerQuery.contains("restaurant") || lowerQuery.contains("cooking")) {
            trends.add(TrendingKeyword.builder()
                    .keyword("plant-based meals")
                    .growth(88)
                    .region(region)
                    .searchVolume(134000L)
                    .category("Food")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("meal prep")
                    .growth(75)
                    .region(region)
                    .searchVolume(106000L)
                    .category("Food")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("healthy recipes")
                    .growth(70)
                    .region(region)
                    .searchVolume(98000L)
                    .category("Food")
                    .build());
        } else {
            // Generic trending keywords
            trends.add(TrendingKeyword.builder()
                    .keyword("new arrival " + query)
                    .growth(80)
                    .region(region)
                    .searchVolume(100000L)
                    .category("General")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("best " + query)
                    .growth(75)
                    .region(region)
                    .searchVolume(95000L)
                    .category("General")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("affordable " + query)
                    .growth(70)
                    .region(region)
                    .searchVolume(88000L)
                    .category("General")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("premium " + query)
                    .growth(65)
                    .region(region)
                    .searchVolume(82000L)
                    .category("General")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("trending " + query)
                    .growth(60)
                    .region(region)
                    .searchVolume(76000L)
                    .category("General")
                    .build());
        }

        return trends;
    }

    /**
     * Clear trend cache for a specific query and region
     */
    public void clearTrendCache(String query, String region) {
        String cacheKey = "trends:" + query + ":" + region;
        redisTemplate.delete(cacheKey);
        log.info("Cleared trend cache for {}", cacheKey);
    }
}
