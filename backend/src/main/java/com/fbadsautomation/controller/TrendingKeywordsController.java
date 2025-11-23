package com.fbadsautomation.controller;

import com.fbadsautomation.dto.TrendingKeyword;
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

    @GetMapping("/search")
    @Operation(summary = "Search trending keywords", description = "Fetch trending keywords for a query and region. Public endpoint - no authentication required.")
    public ResponseEntity<List<TrendingKeyword>> getTrends(
            @RequestParam String query,
            @RequestParam(defaultValue = "US") String region,
            @RequestParam(defaultValue = "en") String language,
            Authentication authentication) {

        // Authentication is optional - will be null for unauthenticated requests
        String username = authentication != null ? authentication.getName() : "anonymous";
        log.info("User {} searching trends for query: {} in region: {} (language={})", username, query, region, language);

        List<TrendingKeyword> trends = trendingKeywordsService.fetchTrends(query, region, language);

        return ResponseEntity.ok(trends);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Clear trend cache", description = "Clear cached trends for a specific query and region. Requires authentication.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<String> clearCache(
            @RequestParam String query,
            @RequestParam(defaultValue = "US") String region,
            @RequestParam(defaultValue = "en") String language,
            Authentication authentication) {

        // Cache clearing requires authentication
        if (authentication == null) {
            log.warn("Unauthenticated attempt to clear trend cache for query: {} in region: {}", query, region);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required to clear cache");
        }

        log.info("User {} clearing trend cache for query: {} in region: {} (language={})",
                authentication.getName(), query, region, language);

        trendingKeywordsService.clearTrendCache(query, region, language);

        return ResponseEntity.ok("Cache cleared successfully");
    }
}
