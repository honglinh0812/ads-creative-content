package com.fbadsautomation.service;

import com.fbadsautomation.dto.TrendingKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RapidKeywordInsightService rapidKeywordInsightService;

    public static String buildCacheKey(String query, String location, String language, Integer limit) {
        return sanitizeQuery(query).toLowerCase()
            + "_" + sanitizeLocation(location)
            + "_" + sanitizeLanguage(language)
            + "_" + sanitizeLimit(limit);
    }

    public List<TrendingKeyword> fetchTrends(String query, String location, String language, Integer limit) {
        String normalizedQuery = sanitizeQuery(query);
        String normalizedLocation = sanitizeLocation(location);
        String normalizedLanguage = sanitizeLanguage(language);
        int normalizedLimit = sanitizeLimit(limit);

        log.info("Fetching RapidAPI trends for query: {} in location: {} (language={}, limit={})",
            normalizedQuery, normalizedLocation, normalizedLanguage, normalizedLimit);

        String cacheKey = "trends:" + buildCacheKey(query, location, language, limit);
        @SuppressWarnings("unchecked")
        List<TrendingKeyword> cachedTrends = (List<TrendingKeyword>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedTrends != null && !cachedTrends.isEmpty()) {
            log.info("Returning cached trends for {}", cacheKey);
            return cachedTrends;
        }

        List<TrendingKeyword> rapidKeywords = rapidKeywordInsightService.fetchTopKeywords(
            normalizedQuery,
            normalizedLocation,
            normalizedLanguage,
            normalizedLimit
        );

        List<TrendingKeyword> finalTrends;
        if (rapidKeywords == null || rapidKeywords.isEmpty()) {
            log.warn("RapidAPI returned no keywords. Falling back to mock data.");
            finalTrends = generateMockTrends(normalizedQuery, normalizedLocation, normalizedLanguage, normalizedLimit);
        } else if (rapidKeywords.size() < normalizedLimit) {
            finalTrends = enrichWithFallbacks(rapidKeywords, normalizedQuery, normalizedLocation, normalizedLanguage, normalizedLimit);
        } else {
            finalTrends = rapidKeywords;
        }

        redisTemplate.opsForValue().set(cacheKey, finalTrends, 60, TimeUnit.MINUTES);
        log.info("Cached {} trends for {}", finalTrends.size(), cacheKey);
        return finalTrends;
    }

    private List<TrendingKeyword> enrichWithFallbacks(List<TrendingKeyword> base,
                                                      String query,
                                                      String location,
                                                      String language,
                                                      int limit) {
        List<TrendingKeyword> enriched = new ArrayList<>(base);
        List<TrendingKeyword> fallback = generateMockTrends(query, location, language, limit);

        for (TrendingKeyword keyword : fallback) {
            boolean exists = enriched.stream()
                .anyMatch(existing -> existing.getKeyword() != null
                    && existing.getKeyword().trim().equalsIgnoreCase(keyword.getKeyword()));
            if (!exists) {
                enriched.add(keyword);
            }
            if (enriched.size() >= limit) {
                break;
            }
        }
        return enriched;
    }

    private List<TrendingKeyword> generateMockTrends(String query, String location, String language, int limit) {
        List<TrendingKeyword> trends = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        boolean prefersVietnamese = "VN".equalsIgnoreCase(location) || "vi".equalsIgnoreCase(language);

        if (prefersVietnamese) {
            trends.addAll(generateVietnameseFallbackTrends(query, location));
        } else if (lowerQuery.contains("tech") || lowerQuery.contains("gadget") || lowerQuery.contains("phone")) {
            trends.add(TrendingKeyword.builder()
                .keyword("AI smartphone")
                .growth(85)
                .region(location)
                .searchVolume(125000L)
                .category("Technology")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("5G connectivity")
                .growth(72)
                .region(location)
                .searchVolume(98000L)
                .category("Technology")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("wireless charging")
                .growth(65)
                .region(location)
                .searchVolume(87000L)
                .category("Technology")
                .source("System Fallback")
                .build());
        } else if (lowerQuery.contains("fashion") || lowerQuery.contains("clothing") || lowerQuery.contains("apparel")) {
            trends.add(TrendingKeyword.builder()
                .keyword("sustainable fashion")
                .growth(92)
                .region(location)
                .searchVolume(145000L)
                .category("Fashion")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("vintage style")
                .growth(78)
                .region(location)
                .searchVolume(112000L)
                .category("Fashion")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("minimalist wardrobe")
                .growth(68)
                .region(location)
                .searchVolume(95000L)
                .category("Fashion")
                .source("System Fallback")
                .build());
        } else if (lowerQuery.contains("food") || lowerQuery.contains("restaurant") || lowerQuery.contains("cooking")) {
            trends.add(TrendingKeyword.builder()
                .keyword("plant-based meals")
                .growth(88)
                .region(location)
                .searchVolume(134000L)
                .category("Food")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("meal prep")
                .growth(75)
                .region(location)
                .searchVolume(106000L)
                .category("Food")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("healthy recipes")
                .growth(70)
                .region(location)
                .searchVolume(98000L)
                .category("Food")
                .source("System Fallback")
                .build());
        } else {
            trends.add(TrendingKeyword.builder()
                .keyword("new arrival " + query)
                .growth(80)
                .region(location)
                .searchVolume(100000L)
                .category("General")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("best " + query)
                .growth(75)
                .region(location)
                .searchVolume(95000L)
                .category("General")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("affordable " + query)
                .growth(70)
                .region(location)
                .searchVolume(88000L)
                .category("General")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("premium " + query)
                .growth(65)
                .region(location)
                .searchVolume(82000L)
                .category("General")
                .source("System Fallback")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("trending " + query)
                .growth(60)
                .region(location)
                .searchVolume(76000L)
                .category("General")
                .source("System Fallback")
                .build());
        }

        if (trends.size() > limit) {
            return trends.subList(0, limit);
        }
        return trends;
    }

    private List<TrendingKeyword> generateVietnameseFallbackTrends(String query, String location) {
        List<TrendingKeyword> trends = new ArrayList<>();
        String normalizedQuery = query == null ? "" : query.trim();
        String topic = normalizedQuery.isEmpty() ? "sản phẩm" : normalizedQuery;

        trends.add(TrendingKeyword.builder()
            .keyword("xu hướng " + topic)
            .growth(82)
            .region(location)
            .searchVolume(95000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword(topic + " giá tốt")
            .growth(77)
            .region(location)
            .searchVolume(88000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword(topic + " khuyến mãi")
            .growth(74)
            .region(location)
            .searchVolume(82000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword("mẫu " + topic + " hot")
            .growth(69)
            .region(location)
            .searchVolume(76000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword(topic + " chính hãng")
            .growth(64)
            .region(location)
            .searchVolume(69000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());

        String lower = topic.toLowerCase();
        if (lower.contains("tech") || lower.contains("gadget") || lower.contains("phone")) {
            trends.add(TrendingKeyword.builder()
                .keyword("điện thoại AI mới")
                .growth(90)
                .region(location)
                .searchVolume(120000L)
                .category("Công nghệ")
                .source("System Fallback (vi)")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("kết nối 5G Việt Nam")
                .growth(76)
                .region(location)
                .searchVolume(91000L)
                .category("Công nghệ")
                .source("System Fallback (vi)")
                .build());
        } else if (lower.contains("fashion") || lower.contains("clothing") || lower.contains("apparel")) {
            trends.add(TrendingKeyword.builder()
                .keyword("thời trang bền vững")
                .growth(88)
                .region(location)
                .searchVolume(105000L)
                .category("Thời trang")
                .source("System Fallback (vi)")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("phong cách vintage")
                .growth(72)
                .region(location)
                .searchVolume(87000L)
                .category("Thời trang")
                .source("System Fallback (vi)")
                .build());
        } else if (lower.contains("food") || lower.contains("restaurant") || lower.contains("cooking")) {
            trends.add(TrendingKeyword.builder()
                .keyword("ẩm thực healthy")
                .growth(86)
                .region(location)
                .searchVolume(99000L)
                .category("Ẩm thực")
                .source("System Fallback (vi)")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("thực đơn eat clean")
                .growth(70)
                .region(location)
                .searchVolume(91000L)
                .category("Ẩm thực")
                .source("System Fallback (vi)")
                .build());
        }

        return trends;
    }

    public void clearTrendCache(String query, String location, String language, Integer limit) {
        String cacheKey = "trends:" + buildCacheKey(query, location, language, limit);
        redisTemplate.delete(cacheKey);
        log.info("Cleared trend cache for {}", cacheKey);
    }

    public static String sanitizeQuery(String query) {
        return (query == null || query.isBlank()) ? "general" : query.trim();
    }

    private static String sanitizeLocation(String location) {
        return (location == null || location.isBlank()) ? "US" : location.trim().toUpperCase();
    }

    private static String sanitizeLanguage(String language) {
        return (language == null || language.isBlank()) ? "en" : language.trim().toLowerCase();
    }

    private static int sanitizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 50);
    }
}
