package com.fbadsautomation.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchApiResponse {

    @JsonProperty("ads")
    private List<SearchAdResult> ads = Collections.emptyList();

    @JsonProperty("videos")
    private List<SearchVideoResult> videos = Collections.emptyList();

    @JsonProperty("results")
    private List<SearchContentResult> content = Collections.emptyList();

    public void trimResults(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        if (ads != null && ads.size() > safeLimit) {
            ads = ads.subList(0, safeLimit);
        }
        if (videos != null && videos.size() > safeLimit) {
            videos = videos.subList(0, safeLimit);
        }
        if (content != null && content.size() > safeLimit) {
            content = content.subList(0, safeLimit);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchAdResult {
        private String title;
        private String description;
        @JsonProperty("displayed_link")
        private String displayedLink;
        private String link;
        @JsonProperty("tracking_link")
        private String trackingLink;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchVideoResult {
        private String title;
        private String link;
        private String thumbnail;
        private String channel;
        private String views;
        @JsonProperty("published_date")
        private String publishedDate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchContentResult {
        private String title;
        private String link;
        private String thumbnail;
        private String author;
        private String stats;
    }
}
