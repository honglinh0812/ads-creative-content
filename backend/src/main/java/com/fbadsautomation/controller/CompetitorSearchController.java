package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.dto.search.SearchApiResponse;
import com.fbadsautomation.service.CompetitorService;
import com.fbadsautomation.service.SearchApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/competitors/search")
@RequiredArgsConstructor
@Tag(name = "Competitor Search", description = "Search competitor footprint across the web using SearchAPI.io")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class CompetitorSearchController {

    private final SearchApiService searchApiService;
    private final CompetitorService competitorService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Search competitor ads/content across the web")
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(@RequestBody SearchRequest request,
                                                                    Authentication authentication) {
        String engine = StringUtils.hasText(request.getEngine()) ? request.getEngine() : "linkedin_ad_library";
        int limit = request.getLimit() != null ? request.getLimit() : 10;

        boolean isTikTok = "tiktok_ads_library".equalsIgnoreCase(engine);
        Map<String, String> options = new HashMap<>();
        String resolvedCountry = resolveCountry(engine, request.getCountry());
        options.put("country", resolvedCountry);
        options.put("time_period", request.getTimePeriod());
        options.put("next_page_token", request.getNextPageToken());
        String queryParam;
        String historyBrand;

        if (isTikTok) {
            String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword() : request.getQuery();
            if (!StringUtils.hasText(keyword) && !StringUtils.hasText(request.getAdvertiserId())) {
                throw new IllegalArgumentException("Keyword or advertiser ID is required for TikTok Ads Library search.");
            }
            options.put("sort_by", request.getSortBy());
            options.put("advertiser_id", request.getAdvertiserId());
            queryParam = keyword;
            historyBrand = StringUtils.hasText(request.getAdvertiser())
                    ? request.getAdvertiser()
                    : (StringUtils.hasText(keyword) ? keyword : request.getAdvertiserId());
        } else {
            String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword() : request.getQuery();
            String advertiser = StringUtils.hasText(request.getAdvertiser()) ? request.getAdvertiser() : request.getQuery();
            if (!StringUtils.hasText(keyword) && !StringUtils.hasText(advertiser)) {
                throw new IllegalArgumentException("Advertiser or keyword is required for LinkedIn Ads Library search.");
            }
            options.put("advertiser", advertiser);
            queryParam = StringUtils.hasText(keyword) ? keyword : advertiser;
            historyBrand = StringUtils.hasText(advertiser) ? advertiser : keyword;
        }
        SearchApiResponse apiResponse = searchApiService.search(engine, queryParam, options, limit);
        SearchResultResponse response = SearchResultResponse.from(apiResponse, engine);

        Long userId = getUserIdFromAuthentication(authentication);
        if (userId != null) {
            int count = (apiResponse.getAds() != null ? apiResponse.getAds().size() : 0)
                    + (apiResponse.getVideos() != null ? apiResponse.getVideos().size() : 0)
                    + (apiResponse.getContent() != null ? apiResponse.getContent().size() : 0);
            competitorService.recordSearchHistory(
                historyBrand,
                resolvedCountry,
                userId,
                engine.toUpperCase(),
                count,
                count > 0
            );
        }

        return ResponseEntity.ok(ApiResponse.success("Search completed", response));
    }

    @Data
    public static class SearchRequest {
        private String query;
        private String engine;
        private String keyword;
        private String advertiser;
        private String country;
        private String timePeriod;
        private String nextPageToken;
        private String advertiserId;
        private String sortBy;
        @Min(1)
        @Max(50)
        private Integer limit;
    }

    @Data
    public static class SearchResultResponse {
        @Schema(description = "Engine used for the search")
        private String engine;
        private SearchApiResponse.SearchAdResult[] ads;
        private SearchApiResponse.SearchVideoResult[] videos;
        private SearchApiResponse.SearchContentResult[] content;

        static SearchResultResponse from(SearchApiResponse body, String engine) {
            SearchResultResponse response = new SearchResultResponse();
            response.engine = engine;
            response.ads = body.getAds() != null
                ? body.getAds().toArray(new SearchApiResponse.SearchAdResult[0]) : new SearchApiResponse.SearchAdResult[0];
            response.videos = body.getVideos() != null
                ? body.getVideos().toArray(new SearchApiResponse.SearchVideoResult[0]) : new SearchApiResponse.SearchVideoResult[0];
            response.content = body.getContent() != null
                ? body.getContent().toArray(new SearchApiResponse.SearchContentResult[0]) : new SearchApiResponse.SearchContentResult[0];
            return response;
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.warn("Unable to parse user ID from username: {}", username);
                return null;
            }
        } else if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                log.warn("Unable to parse user ID from principal string: {}", principal);
                return null;
            }
        }
        log.warn("Unsupported authentication principal type: {}", principal.getClass().getName());
        return null;
    }

    private String resolveCountry(String engine, String requestedCountry) {
        if ("tiktok_ads_library".equalsIgnoreCase(engine)) {
            return StringUtils.hasText(requestedCountry) ? requestedCountry : "ALL";
        }
        return requestedCountry;
    }
}
