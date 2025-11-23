package com.fbadsautomation.service;

import com.fbadsautomation.dto.CompetitorAdDTO;
import com.fbadsautomation.dto.PlatformSearchResult;
import com.fbadsautomation.dto.TrendingKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TrendingKeywordsService {

    private static final Logger log = LoggerFactory.getLogger(TrendingKeywordsService.class);
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#[\\p{L}\\p{N}_]{3,}");
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}\\-]{2,}");
    private static final Set<String> STOP_WORDS = new HashSet<>(Set.of(
        "the", "and", "with", "for", "your", "from", "that", "this", "have", "will", "just", "into", "free",
        "you", "are", "our", "all", "how", "why", "can", "now", "more", "best", "get", "made", "new",
        "các", "một", "và", "cho", "khi", "trong", "cùng", "những", "này", "đang", "được", "là", "từ",
        "sản", "phẩm", "dịch", "vụ", "giúp", "bạn", "chỉ", "với", "mỗi", "đều", "đến"
    ));

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GoogleTrendsApiService googleTrendsApiService;

    @Autowired(required = false)
    private ApifyService apifyService;

    /**
     * Build deterministic Redis cache key (also used by SpEL)
     */
    public static String buildCacheKey(String query, String region, String language) {
        return sanitizeQuery(query).toLowerCase()
            + "_" + sanitizeRegion(region)
            + "_" + sanitizeLanguage(language);
    }

    /**
     * Fetch trending keywords for a query and region
     * Results are cached for 60 minutes
     *
     * @param query Search query
     * @param region Target region (e.g., US, VN, UK)
     * @return List of trending keywords
     */
    @Cacheable(
        value = "trends",
        key = "T(com.fbadsautomation.service.TrendingKeywordsService).buildCacheKey(#query, #region, #language)",
        unless = "#result == null"
    )
    public List<TrendingKeyword> fetchTrends(String query, String region, String language) {
        String normalizedQuery = sanitizeQuery(query);
        String normalizedRegion = sanitizeRegion(region);
        String normalizedLanguage = sanitizeLanguage(language);

        log.info("Fetching trends for query: {} in region: {} (language={})",
            normalizedQuery, normalizedRegion, normalizedLanguage);

        String cacheKey = "trends:" + buildCacheKey(query, region, language);
        @SuppressWarnings("unchecked")
        List<TrendingKeyword> cachedTrends = (List<TrendingKeyword>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedTrends != null && !cachedTrends.isEmpty()) {
            log.info("Returning cached trends for {}", cacheKey);
            return cachedTrends;
        }

        List<TrendingKeyword> googleTrends = fetchFromGoogleTrends(normalizedQuery, normalizedRegion, normalizedLanguage);
        List<TrendingKeyword> tiktokTrends = extractTikTokTrends(normalizedQuery, normalizedRegion);

        List<TrendingKeyword> mergedTrends = mergeTrendSources(googleTrends, tiktokTrends);

        if (mergedTrends == null || mergedTrends.isEmpty()) {
            log.info("Using mock trends data (API not available or failed)");
            mergedTrends = generateMockTrends(normalizedQuery, normalizedRegion, normalizedLanguage);
        } else if (mergedTrends.size() < 5) {
            mergedTrends = enrichWithFallbacks(mergedTrends, normalizedQuery, normalizedRegion, normalizedLanguage);
        }

        redisTemplate.opsForValue().set(cacheKey, mergedTrends, 60, TimeUnit.MINUTES);
        log.info("Cached {} trends for {}", mergedTrends.size(), cacheKey);

        return mergedTrends;
    }

    private List<TrendingKeyword> fetchFromGoogleTrends(String query, String region, String language) {
        if (googleTrendsApiService == null || !googleTrendsApiService.isApiAvailable()) {
            log.debug("Google Trends API not configured. Skipping real-time data.");
            return Collections.emptyList();
        }

        try {
            List<TrendingKeyword> results = googleTrendsApiService.fetchTrendsFromApi(query, region, language);
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching Google Trends data: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<TrendingKeyword> extractTikTokTrends(String query, String region) {
        // TODO: TikTok (Apify) integration only supports region filtering today. Add language-aware filtering once upstream supports it.
        if (apifyService == null || !apifyService.isAvailable()) {
            log.debug("Apify/TikTok integration not configured. Skipping TikTok trends.");
            return Collections.emptyList();
        }

        try {
            PlatformSearchResult result = apifyService.searchTikTokAds(query, region, 40);
            if (result == null || !result.isSuccess() || result.getAds() == null || result.getAds().isEmpty()) {
                log.info("TikTok search returned no usable ads for query {}", query);
                return Collections.emptyList();
            }

            Map<String, KeywordStats> keywordStats = new HashMap<>();
            for (CompetitorAdDTO ad : result.getAds()) {
                List<KeywordCandidate> candidates = extractKeywordCandidates(ad);
                if (candidates.isEmpty()) {
                    continue;
                }

                int engagementScore = calculateEngagementScore(ad);
                for (KeywordCandidate candidate : candidates) {
                    keywordStats
                        .computeIfAbsent(candidate.canonical(), key -> new KeywordStats(candidate.display()))
                        .increment(engagementScore);
                }
            }

            return keywordStats.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue().score(), a.getValue().score()))
                .limit(12)
                .map(entry -> TrendingKeyword.builder()
                    .keyword(entry.getValue().display())
                    .growth(entry.getValue().growthScore())
                    .region(region)
                    .searchVolume(entry.getValue().estimateSearchVolume())
                    .category("TikTok Creative Center")
                    .source("TikTok Creative Center")
                    .build())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to build TikTok trend insights: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<TrendingKeyword> mergeTrendSources(List<TrendingKeyword>... sources) {
        if (sources == null) {
            return Collections.emptyList();
        }

        Map<String, TrendingKeyword> merged = new LinkedHashMap<>();
        for (List<TrendingKeyword> source : sources) {
            if (source == null) continue;

            for (TrendingKeyword keyword : source) {
                if (keyword == null || keyword.getKeyword() == null) continue;
                String canonical = keyword.getKeyword().trim().toLowerCase();
                if (canonical.isEmpty()) continue;

                TrendingKeyword existing = merged.get(canonical);
                if (existing == null || prefers(keyword, existing)) {
                    merged.put(canonical, keyword);
                } else if (existing.getSource() == null && keyword.getSource() != null) {
                    existing.setSource(keyword.getSource());
                }
            }
        }

        return merged.values()
            .stream()
            .sorted(Comparator.comparingInt(this::scoreForSorting).reversed())
            .collect(Collectors.toList());
    }

    private boolean prefers(TrendingKeyword candidate, TrendingKeyword existing) {
        Integer candidateGrowth = candidate.getGrowth();
        Integer existingGrowth = existing.getGrowth();
        if (candidateGrowth != null && existingGrowth != null) {
            return candidateGrowth > existingGrowth;
        }
        return existingGrowth == null && candidateGrowth != null;
    }

    private int scoreForSorting(TrendingKeyword keyword) {
        int growthScore = keyword.getGrowth() != null ? keyword.getGrowth() : 50;
        long volume = keyword.getSearchVolume() != null ? keyword.getSearchVolume() : 0L;
        int volumeScore = (int) Math.min(40, Math.log10(volume + 1) * 10);
        return growthScore + volumeScore;
    }

    private List<TrendingKeyword> enrichWithFallbacks(List<TrendingKeyword> base, String query, String region, String language) {
        List<TrendingKeyword> enriched = new ArrayList<>(base);
        List<TrendingKeyword> fallback = generateMockTrends(query, region, language);

        for (TrendingKeyword keyword : fallback) {
            if (keyword.getKeyword() == null) continue;
            String canonical = keyword.getKeyword().trim().toLowerCase();
            boolean exists = enriched.stream()
                .anyMatch(existing -> existing.getKeyword() != null
                    && existing.getKeyword().trim().equalsIgnoreCase(keyword.getKeyword()));
            if (!exists) {
                enriched.add(keyword);
            }
            if (enriched.size() >= 10) {
                break;
            }
        }
        return enriched;
    }

    /**
     * Generate mock trending keywords used as a safety net when real sources fail
     */
    private List<TrendingKeyword> generateMockTrends(String query, String region, String language) {
        List<TrendingKeyword> trends = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        boolean prefersVietnamese = "VN".equalsIgnoreCase(region) || "vi".equalsIgnoreCase(language);

        if (prefersVietnamese) {
            return generateVietnameseFallbackTrends(query, region);
        }

        if (lowerQuery.contains("tech") || lowerQuery.contains("gadget") || lowerQuery.contains("phone")) {
            trends.add(TrendingKeyword.builder()
                    .keyword("AI smartphone")
                    .growth(85)
                    .region(region)
                    .searchVolume(125000L)
                    .category("Technology")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("5G connectivity")
                    .growth(72)
                    .region(region)
                    .searchVolume(98000L)
                    .category("Technology")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("wireless charging")
                    .growth(65)
                    .region(region)
                    .searchVolume(87000L)
                    .category("Technology")
                    .source("System Fallback")
                    .build());
        } else if (lowerQuery.contains("fashion") || lowerQuery.contains("clothing") || lowerQuery.contains("apparel")) {
            trends.add(TrendingKeyword.builder()
                    .keyword("sustainable fashion")
                    .growth(92)
                    .region(region)
                    .searchVolume(145000L)
                    .category("Fashion")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("vintage style")
                    .growth(78)
                    .region(region)
                    .searchVolume(112000L)
                    .category("Fashion")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("minimalist wardrobe")
                    .growth(68)
                    .region(region)
                    .searchVolume(95000L)
                    .category("Fashion")
                    .source("System Fallback")
                    .build());
        } else if (lowerQuery.contains("food") || lowerQuery.contains("restaurant") || lowerQuery.contains("cooking")) {
            trends.add(TrendingKeyword.builder()
                    .keyword("plant-based meals")
                    .growth(88)
                    .region(region)
                    .searchVolume(134000L)
                    .category("Food")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("meal prep")
                    .growth(75)
                    .region(region)
                    .searchVolume(106000L)
                    .category("Food")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("healthy recipes")
                    .growth(70)
                    .region(region)
                    .searchVolume(98000L)
                    .category("Food")
                    .source("System Fallback")
                    .build());
        } else {
            trends.add(TrendingKeyword.builder()
                    .keyword("new arrival " + query)
                    .growth(80)
                    .region(region)
                    .searchVolume(100000L)
                    .category("General")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("best " + query)
                    .growth(75)
                    .region(region)
                    .searchVolume(95000L)
                    .category("General")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("affordable " + query)
                    .growth(70)
                    .region(region)
                    .searchVolume(88000L)
                    .category("General")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("premium " + query)
                    .growth(65)
                    .region(region)
                    .searchVolume(82000L)
                    .category("General")
                    .source("System Fallback")
                    .build());
            trends.add(TrendingKeyword.builder()
                    .keyword("trending " + query)
                    .growth(60)
                    .region(region)
                    .searchVolume(76000L)
                    .category("General")
                    .source("System Fallback")
                    .build());
        }

        return trends;
    }

    private List<TrendingKeyword> generateVietnameseFallbackTrends(String query, String region) {
        List<TrendingKeyword> trends = new ArrayList<>();
        String normalizedQuery = query == null ? "" : query.trim();
        String topic = normalizedQuery.isEmpty() ? "sản phẩm" : normalizedQuery;
        String regionCode = sanitizeRegion(region);

        trends.add(TrendingKeyword.builder()
            .keyword("xu hướng " + topic)
            .growth(82)
            .region(regionCode)
            .searchVolume(95000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword(topic + " giá tốt")
            .growth(77)
            .region(regionCode)
            .searchVolume(88000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword(topic + " khuyến mãi")
            .growth(74)
            .region(regionCode)
            .searchVolume(82000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword("mẫu " + topic + " hot")
            .growth(69)
            .region(regionCode)
            .searchVolume(76000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());
        trends.add(TrendingKeyword.builder()
            .keyword(topic + " chính hãng")
            .growth(64)
            .region(regionCode)
            .searchVolume(69000L)
            .category("Bản địa hóa")
            .source("System Fallback (vi)")
            .build());

        String lower = topic.toLowerCase();
        if (lower.contains("tech") || lower.contains("gadget") || lower.contains("phone")) {
            trends.add(TrendingKeyword.builder()
                .keyword("điện thoại AI mới")
                .growth(90)
                .region(regionCode)
                .searchVolume(120000L)
                .category("Công nghệ")
                .source("System Fallback (vi)")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("kết nối 5G Việt Nam")
                .growth(76)
                .region(regionCode)
                .searchVolume(91000L)
                .category("Công nghệ")
                .source("System Fallback (vi)")
                .build());
        } else if (lower.contains("fashion") || lower.contains("clothing") || lower.contains("apparel")) {
            trends.add(TrendingKeyword.builder()
                .keyword("thời trang bền vững")
                .growth(88)
                .region(regionCode)
                .searchVolume(105000L)
                .category("Thời trang")
                .source("System Fallback (vi)")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("phong cách vintage")
                .growth(72)
                .region(regionCode)
                .searchVolume(87000L)
                .category("Thời trang")
                .source("System Fallback (vi)")
                .build());
        } else if (lower.contains("food") || lower.contains("restaurant") || lower.contains("cooking")) {
            trends.add(TrendingKeyword.builder()
                .keyword("ẩm thực healthy")
                .growth(86)
                .region(regionCode)
                .searchVolume(99000L)
                .category("Ẩm thực")
                .source("System Fallback (vi)")
                .build());
            trends.add(TrendingKeyword.builder()
                .keyword("thực đơn eat clean")
                .growth(70)
                .region(regionCode)
                .searchVolume(91000L)
                .category("Ẩm thực")
                .source("System Fallback (vi)")
                .build());
        }

        return trends;
    }

    /**
     * Clear trend cache for a specific query and region
     */
    public void clearTrendCache(String query, String region, String language) {
        String cacheKey = "trends:" + buildCacheKey(query, region, language);
        redisTemplate.delete(cacheKey);
        log.info("Cleared trend cache for {}", cacheKey);
    }

    private static String sanitizeQuery(String query) {
        return (query == null || query.isBlank()) ? "general" : query.trim();
    }

    private static String sanitizeRegion(String region) {
        return (region == null || region.isBlank()) ? "US" : region.trim().toUpperCase();
    }

    private static String sanitizeLanguage(String language) {
        return (language == null || language.isBlank()) ? "en" : language.trim().toLowerCase();
    }

    private List<KeywordCandidate> extractKeywordCandidates(CompetitorAdDTO ad) {
        if (ad == null) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, ad.getPrimaryText());
        appendIfNotBlank(sb, ad.getHeadline());
        appendIfNotBlank(sb, ad.getDescription());

        String content = sb.toString();
        if (content.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, KeywordCandidate> candidates = new LinkedHashMap<>();

        Matcher hashtagMatcher = HASHTAG_PATTERN.matcher(content);
        while (hashtagMatcher.find()) {
            KeywordCandidate candidate = buildCandidate(hashtagMatcher.group(), true);
            if (candidate != null) {
                candidates.putIfAbsent(candidate.canonical(), candidate);
            }
        }

        if (candidates.size() < 3) {
            List<String> fallbackWords = extractKeyPhrases(content);
            for (String word : fallbackWords) {
                KeywordCandidate candidate = buildCandidate(word, false);
                if (candidate != null) {
                    candidates.putIfAbsent(candidate.canonical(), candidate);
                }
            }
        }

        return new ArrayList<>(candidates.values());
    }

    private KeywordCandidate buildCandidate(String raw, boolean isHashtag) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (isHashtag) {
            String canonical = trimmed.toLowerCase();
            if (canonical.length() < 4) {
                return null;
            }
            return new KeywordCandidate(canonical, canonical);
        }

        String cleaned = trimmed
            .replaceAll("[^\\p{L}\\p{N}\\s\\-]", " ")
            .toLowerCase()
            .trim();

        if (cleaned.length() < 3 || STOP_WORDS.contains(cleaned)) {
            return null;
        }

        String display = Arrays.stream(cleaned.split("\\s+"))
            .filter(part -> !part.isBlank())
            .map(this::capitalizeWord)
            .collect(Collectors.joining(" "));

        if (display.isBlank()) {
            return null;
        }

        return new KeywordCandidate(display.toLowerCase(), display);
    }

    private void appendIfNotBlank(StringBuilder sb, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(text);
    }

    private List<String> extractKeyPhrases(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase());

        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() < 4 || STOP_WORDS.contains(word)) {
                continue;
            }
            frequency.merge(word, 1, Integer::sum);
        }

        return frequency.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private int calculateEngagementScore(CompetitorAdDTO ad) {
        if (ad == null || ad.getMetadata() == null) {
            return 35;
        }

        Object metadata = ad.getMetadata();
        if (!(metadata instanceof Map<?, ?> dataMap)) {
            return 35;
        }

        int likes = parseMetric(dataMap.get("likes"));
        int shares = parseMetric(dataMap.get("shares"));
        int comments = parseMetric(dataMap.get("comments"));

        int total = likes + (shares * 3) + (comments * 2);
        double normalized = Math.min(100d, Math.log1p(total) * 12 + 25);
        return (int) Math.round(normalized);
    }

    private int parseMetric(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String capitalizeWord(String word) {
        if (word == null || word.isBlank()) {
            return "";
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private record KeywordCandidate(String canonical, String display) {}

    private static class KeywordStats {
        private final String display;
        private int frequency = 0;
        private int maxEngagement = 0;

        KeywordStats(String display) {
            this.display = display;
        }

        void increment(int engagementScore) {
            frequency++;
            maxEngagement = Math.max(maxEngagement, engagementScore);
        }

        int score() {
            return (frequency * 12) + maxEngagement;
        }

        int growthScore() {
            return Math.min(100, score());
        }

        long estimateSearchVolume() {
            return 60000L + (long) frequency * 8000L + (long) maxEngagement * 500L;
        }

        String display() {
            return display;
        }
    }
}
