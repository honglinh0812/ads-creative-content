package com.fbadsautomation.controller;

import com.fbadsautomation.dto.KeywordLanguage;
import com.fbadsautomation.dto.KeywordLocation;
import com.fbadsautomation.dto.TrendingKeyword;
import com.fbadsautomation.service.RapidKeywordInsightService;
import com.fbadsautomation.service.TrendingKeywordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trends")
@Tag(name = "Trending Keywords", description = "Public APIs for fetching trending keywords - no authentication required")
public class TrendingKeywordsController {

    private static final Logger log = LoggerFactory.getLogger(TrendingKeywordsController.class);

    @Autowired
    private TrendingKeywordsService trendingKeywordsService;

    @Autowired
    private RapidKeywordInsightService rapidKeywordInsightService;

    @GetMapping("/search")
    @Operation(summary = "Search trending keywords", description = "Fetch trending keywords for a query and region. Public endpoint - no authentication required.")
    public ResponseEntity<List<TrendingKeyword>> getTrends(
            @RequestParam String query,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "region", required = false) String legacyRegion,
            @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestParam(name = "num", defaultValue = "10") Integer limit,
            Authentication authentication) {

        // Authentication is optional - will be null for unauthenticated requests
        String username = authentication != null ? authentication.getName() : "anonymous";
        String resolvedLocation = location != null ? location : (legacyRegion != null ? legacyRegion : "US");
        log.info("User {} searching trends for query: {} in location: {} (language={}, limit={})",
            username, query, resolvedLocation, language, limit);

        List<TrendingKeyword> trends = trendingKeywordsService.fetchTrends(query, resolvedLocation, language, limit);

        return ResponseEntity.ok(trends);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Clear trend cache", description = "Clear cached trends for a specific query and region. Requires authentication.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<String> clearCache(
            @RequestParam String query,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "region", required = false) String legacyRegion,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(name = "num", defaultValue = "10") Integer limit,
            Authentication authentication) {

        // Cache clearing requires authentication
        if (authentication == null) {
            log.warn("Unauthenticated attempt to clear trend cache for query: {} in region: {}", query, legacyRegion);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required to clear cache");
        }

        String resolvedLocation = location != null ? location : (legacyRegion != null ? legacyRegion : "US");
        log.info("User {} clearing trend cache for query: {} in location: {} (language={}, limit={})",
                authentication.getName(), query, resolvedLocation, language, limit);

        trendingKeywordsService.clearTrendCache(query, resolvedLocation, language, limit);

        return ResponseEntity.ok("Cache cleared successfully");
    }

    @GetMapping("/locations")
    @Operation(summary = "Get supported locations", description = "List locations supported by RapidAPI Google Keyword Insight.")
    public ResponseEntity<List<KeywordLocation>> getSupportedLocations() {
        return ResponseEntity.ok(rapidKeywordInsightService.fetchSupportedLocations());
    }

    @GetMapping("/languages")
    @Operation(summary = "Get supported languages", description = "List languages supported by RapidAPI Google Keyword Insight.")
    public ResponseEntity<List<KeywordLanguage>> getSupportedLanguages() {
        return ResponseEntity.ok(rapidKeywordInsightService.fetchSupportedLanguages());
    }
}
