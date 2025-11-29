package com.fbadsautomation.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        private String advertiserName;
        private String advertiserThumbnail;
        private String advertiserId;
        private String adType;
        private String headline;
        private String image;
        private String coverImage;
        private String videoLink;
        private String cta;
        private Integer position;
        private String adId;
        private List<String> headlineLinks = new ArrayList<>();
        @JsonProperty("first_shown_datetime")
        private String firstShown;
        @JsonProperty("last_shown_datetime")
        private String lastShown;
        @JsonProperty("estimated_audience")
        private String estimatedAudience;
        @JsonProperty("estimated_audience_min")
        private Integer estimatedAudienceMin;
        @JsonProperty("estimated_audience_max")
        private Integer estimatedAudienceMax;

        @JsonProperty("advertiser")
        public void unpackAdvertiser(Object advertiserNode) {
            if (advertiserNode == null) {
                this.advertiserName = null;
                this.advertiserThumbnail = null;
                return;
            }
            if (advertiserNode instanceof Map) {
                Map<?, ?> advertiser = (Map<?, ?>) advertiserNode;
                Object name = advertiser.get("name");
                Object thumbnail = advertiser.get("thumbnail");
                this.advertiserName = name != null ? name.toString().trim() : null;
                this.advertiserThumbnail = thumbnail != null ? thumbnail.toString() : null;
            } else {
                this.advertiserName = advertiserNode.toString().trim();
                this.advertiserThumbnail = null;
            }
        }

        @JsonProperty("advertiser_id")
        public void setAdvertiserId(String advertiserId) {
            this.advertiserId = advertiserId;
        }

        @JsonProperty("content")
        public void unpackContent(Map<String, Object> content) {
            if (content == null) {
                return;
            }
            Object headline = content.get("headline");
            if (headline != null) {
                this.headline = headline.toString();
            }
            Object image = content.get("image");
            if (image != null) {
                this.image = image.toString();
            }
            Object cta = content.get("cta");
            if (cta != null) {
                this.cta = cta.toString();
            }
            Object headlineLinks = content.get("headline_links");
            if (headlineLinks instanceof List) {
                List<?> links = (List<?>) headlineLinks;
                this.headlineLinks = new ArrayList<>();
                for (Object value : links) {
                    if (value != null) {
                        this.headlineLinks.add(value.toString());
                    }
                }
            }
        }

        @JsonProperty("ad_type")
        public void setAdType(String adType) {
            this.adType = adType;
        }

        @JsonProperty("position")
        public void setPosition(Integer position) {
            this.position = position;
        }

        @JsonProperty("id")
        public void setAdId(String adId) {
            this.adId = adId;
        }

        @JsonProperty("cover_image")
        public void setCoverImage(String coverImage) {
            this.coverImage = coverImage;
        }

        @JsonProperty("video_link")
        public void setVideoLink(String videoLink) {
            this.videoLink = videoLink;
            if (this.link == null) {
                this.link = videoLink;
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchVideoResult {
        private String title;
        private String link;
        private String thumbnail;
        private String channel;
        private String channelLink;
        private String views;
        @JsonProperty("published_date")
        private String publishedDate;

        @JsonProperty("channel")
        public void unpackChannel(Object channelNode) {
            if (channelNode == null) {
                this.channel = null;
                this.channelLink = null;
                return;
            }
            if (channelNode instanceof String) {
                this.channel = (String) channelNode;
                this.channelLink = null;
                return;
            }
            if (channelNode instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) channelNode;
                Object name = map.get("name");
                Object link = map.get("link");
                this.channel = name != null ? name.toString() : null;
                this.channelLink = link != null ? link.toString() : null;
            }
        }
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
