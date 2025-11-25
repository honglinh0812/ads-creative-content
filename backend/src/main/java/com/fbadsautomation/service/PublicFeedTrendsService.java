package com.fbadsautomation.service;

import com.fbadsautomation.dto.TrendingKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight provider that consumes public RSS/CSV feeds (Google Trends RSS by default)
 * to extract trending keywords without relying on cookie-based scrapers.
 */
@Service
public class PublicFeedTrendsService {

    private static final Logger log = LoggerFactory.getLogger(PublicFeedTrendsService.class);
    private final RestTemplate restTemplate;
    private final Map<String, String> feedByRegion;

    public PublicFeedTrendsService(
        RestTemplate restTemplate,
        @Value("${trends.feed.vn:https://trends.google.com/trends/trendingsearches/daily/rss?geo=VN}") String vietnamFeed,
        @Value("${trends.feed.us:https://trends.google.com/trends/trendingsearches/daily/rss?geo=US}") String usFeed,
        @Value("${trends.feed.sg:https://trends.google.com/trends/trendingsearches/daily/rss?geo=SG}") String singaporeFeed
    ) {
        this.restTemplate = restTemplate;
        this.feedByRegion = new HashMap<>();
        registerFeed("VN", vietnamFeed);
        registerFeed("US", usFeed);
        registerFeed("SG", singaporeFeed);
    }

    private void registerFeed(String region, String url) {
        if (region == null || url == null) {
            return;
        }
        String trimmed = url.trim();
        if (!trimmed.isEmpty()) {
            feedByRegion.put(region.toUpperCase(), trimmed);
        }
    }

    public boolean isAvailable() {
        return restTemplate != null && !feedByRegion.isEmpty();
    }

    /**
     * Fetches keywords from configured public feeds. If a specific region feed is unavailable,
     * it will try fallbacks (US, SG) to avoid returning empty results.
     */
    public List<TrendingKeyword> fetchFromFeeds(String region, String language, int maxResults) {
        if (!isAvailable()) {
            return List.of();
        }

        String normalizedRegion = sanitizeRegion(region);
        List<String> candidateFeeds = buildFeedPriority(normalizedRegion);
        List<TrendingKeyword> collected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String feedUrl : candidateFeeds) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(feedUrl, HttpMethod.GET, null, String.class);
                if (!response.hasBody() || response.getBody() == null) {
                    continue;
                }
                List<TrendingKeyword> parsed = parseRss(response.getBody(), normalizedRegion, language);
                for (TrendingKeyword keyword : parsed) {
                    if (keyword == null || keyword.getKeyword() == null) {
                        continue;
                    }
                    String canonical = keyword.getKeyword().trim().toLowerCase();
                    if (canonical.isEmpty() || seen.contains(canonical)) {
                        continue;
                    }
                    collected.add(keyword);
                    seen.add(canonical);
                    if (collected.size() >= maxResults) {
                        return collected;
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to read RSS feed {}: {}", feedUrl, ex.getMessage());
            }
        }

        return collected;
    }

    private List<String> buildFeedPriority(String region) {
        List<String> feeds = new ArrayList<>();
        if (feedByRegion.containsKey(region)) {
            feeds.add(feedByRegion.get(region));
        }
        // fallback order: VN -> SG -> US
        if (!"SG".equals(region) && feedByRegion.containsKey("SG") && !feeds.contains(feedByRegion.get("SG"))) {
            feeds.add(feedByRegion.get("SG"));
        }
        if (!"US".equals(region) && feedByRegion.containsKey("US") && !feeds.contains(feedByRegion.get("US"))) {
            feeds.add(feedByRegion.get("US"));
        }
        return feeds;
    }

    private List<TrendingKeyword> parseRss(String xmlBody, String region, String language) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource source = new InputSource(new StringReader(xmlBody));
        org.w3c.dom.Document document = builder.parse(source);
        org.w3c.dom.NodeList items = document.getElementsByTagName("item");

        List<TrendingKeyword> keywords = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            org.w3c.dom.Node node = items.item(i);
            if (!(node instanceof org.w3c.dom.Element element)) {
                continue;
            }
            String title = getChildText(element, "title");
            String description = getChildText(element, "description");
            String keywordText = sanitizeKeywordText(title, description);
            if (keywordText == null) {
                continue;
            }

            int growthScore = Math.max(45, 100 - (i * 5));
            long estimatedVolume = Math.max(20000L, 90000L - (i * 2500L));

            keywords.add(TrendingKeyword.builder()
                .keyword(keywordText)
                .growth(growthScore)
                .region(region)
                .searchVolume(estimatedVolume)
                .category("Public Feed")
                .source("Google Trends RSS")
                .build());
        }
        return keywords;
    }

    private String getChildText(org.w3c.dom.Element element, String tagName) {
        org.w3c.dom.NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        org.w3c.dom.Node node = nodes.item(0);
        return node != null ? node.getTextContent() : null;
    }

    private String sanitizeKeywordText(String title, String description) {
        String candidate = title != null ? title : description;
        if (candidate == null) {
            return null;
        }
        String trimmed = candidate.replaceAll("\\s+", " ").trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Remove timestamps or trailing info often added in RSS titles
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            if (parts.length > 1) {
                trimmed = parts[0].trim();
            }
        }
        return trimmed;
    }

    private String sanitizeRegion(String region) {
        return (region == null || region.isBlank()) ? "US" : region.trim().toUpperCase();
    }
}
