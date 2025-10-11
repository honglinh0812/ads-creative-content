package com.fbadsautomation.controller;

import com.fbadsautomation.dto.TrendingKeyword;
import com.fbadsautomation.service.TrendingKeywordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trends")
@Tag(name = "Trending Keywords", description = "APIs for fetching trending keywords")
@SecurityRequirement(name = "Bearer Authentication")
public class TrendingKeywordsController {

    private static final Logger log = LoggerFactory.getLogger(TrendingKeywordsController.class);

    @Autowired
    private TrendingKeywordsService trendingKeywordsService;

    @GetMapping("/search")
    @Operation(summary = "Search trending keywords", description = "Fetch trending keywords for a query and region")
    public ResponseEntity<List<TrendingKeyword>> getTrends(
            @RequestParam String query,
            @RequestParam(defaultValue = "US") String region,
            Authentication authentication) {

        log.info("User {} searching trends for query: {} in region: {}",
                authentication.getName(), query, region);

        List<TrendingKeyword> trends = trendingKeywordsService.fetchTrends(query, region);

        return ResponseEntity.ok(trends);
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Clear trend cache", description = "Clear cached trends for a specific query and region")
    public ResponseEntity<String> clearCache(
            @RequestParam String query,
            @RequestParam(defaultValue = "US") String region,
            Authentication authentication) {

        log.info("User {} clearing trend cache for query: {} in region: {}",
                authentication.getName(), query, region);

        trendingKeywordsService.clearTrendCache(query, region);

        return ResponseEntity.ok("Cache cleared successfully");
    }
}
