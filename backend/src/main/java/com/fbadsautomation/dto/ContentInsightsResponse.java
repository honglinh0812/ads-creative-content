package com.fbadsautomation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Lightweight analytics payload that focuses solely on insights derived from
 * the ads the user already generated inside the platform. This deliberately
 * removes any dependence on external performance metrics (CTR, spend, etc.)
 * so the frontend can render a very simple analytics view.
 */
public class ContentInsightsResponse {

    private Summary summary;
    private CopyMetrics copyMetrics;
    private List<CTAUsage> ctaUsage;
    private List<RecentAd> recentAds;
    private Map<String, Integer> adTypeBreakdown;
    private LocalDateTime generatedAt;

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public CopyMetrics getCopyMetrics() {
        return copyMetrics;
    }

    public void setCopyMetrics(CopyMetrics copyMetrics) {
        this.copyMetrics = copyMetrics;
    }

    public List<CTAUsage> getCtaUsage() {
        return ctaUsage;
    }

    public void setCtaUsage(List<CTAUsage> ctaUsage) {
        this.ctaUsage = ctaUsage;
    }

    public List<RecentAd> getRecentAds() {
        return recentAds;
    }

    public void setRecentAds(List<RecentAd> recentAds) {
        this.recentAds = recentAds;
    }

    public Map<String, Integer> getAdTypeBreakdown() {
        return adTypeBreakdown;
    }

    public void setAdTypeBreakdown(Map<String, Integer> adTypeBreakdown) {
        this.adTypeBreakdown = adTypeBreakdown;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    // Inner DTOs -------------------------------------------------------------

    public static class Summary {
        private int totalAds;
        private int textAds;
        private int mediaAds;
        private int uniqueCampaigns;

        public Summary() {}

        public Summary(int totalAds, int textAds, int mediaAds, int uniqueCampaigns) {
            this.totalAds = totalAds;
            this.textAds = textAds;
            this.mediaAds = mediaAds;
            this.uniqueCampaigns = uniqueCampaigns;
        }

        public int getTotalAds() {
            return totalAds;
        }

        public void setTotalAds(int totalAds) {
            this.totalAds = totalAds;
        }

        public int getTextAds() {
            return textAds;
        }

        public void setTextAds(int textAds) {
            this.textAds = textAds;
        }

        public int getMediaAds() {
            return mediaAds;
        }

        public void setMediaAds(int mediaAds) {
            this.mediaAds = mediaAds;
        }

        public int getUniqueCampaigns() {
            return uniqueCampaigns;
        }

        public void setUniqueCampaigns(int uniqueCampaigns) {
            this.uniqueCampaigns = uniqueCampaigns;
        }
    }

    public static class CopyMetrics {
        private double averageHeadlineLength;
        private double averagePrimaryTextLength;
        private double averageDescriptionLength;
        private List<KeywordHighlight> keywordHighlights;

        public double getAverageHeadlineLength() {
            return averageHeadlineLength;
        }

        public void setAverageHeadlineLength(double averageHeadlineLength) {
            this.averageHeadlineLength = averageHeadlineLength;
        }

        public double getAveragePrimaryTextLength() {
            return averagePrimaryTextLength;
        }

        public void setAveragePrimaryTextLength(double averagePrimaryTextLength) {
            this.averagePrimaryTextLength = averagePrimaryTextLength;
        }

        public double getAverageDescriptionLength() {
            return averageDescriptionLength;
        }

        public void setAverageDescriptionLength(double averageDescriptionLength) {
            this.averageDescriptionLength = averageDescriptionLength;
        }

        public List<KeywordHighlight> getKeywordHighlights() {
            return keywordHighlights;
        }

        public void setKeywordHighlights(List<KeywordHighlight> keywordHighlights) {
            this.keywordHighlights = keywordHighlights;
        }
    }

    public static class KeywordHighlight {
        private String keyword;
        private long count;

        public KeywordHighlight() {}

        public KeywordHighlight(String keyword, long count) {
            this.keyword = keyword;
            this.count = count;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class CTAUsage {
        private String cta;
        private long count;
        private double percentage;

        public CTAUsage() {}

        public CTAUsage(String cta, long count, double percentage) {
            this.cta = cta;
            this.count = count;
            this.percentage = percentage;
        }

        public String getCta() {
            return cta;
        }

        public void setCta(String cta) {
            this.cta = cta;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }

    public static class RecentAd {
        private Long id;
        private String name;
        private String campaignName;
        private String adType;
        private boolean hasMedia;
        private String excerpt;
        private LocalDateTime createdDate;

        public RecentAd() {}

        public RecentAd(Long id, String name, String campaignName, String adType,
                        boolean hasMedia, String excerpt, LocalDateTime createdDate) {
            this.id = id;
            this.name = name;
            this.campaignName = campaignName;
            this.adType = adType;
            this.hasMedia = hasMedia;
            this.excerpt = excerpt;
            this.createdDate = createdDate;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCampaignName() {
            return campaignName;
        }

        public void setCampaignName(String campaignName) {
            this.campaignName = campaignName;
        }

        public String getAdType() {
            return adType;
        }

        public void setAdType(String adType) {
            this.adType = adType;
        }

        public boolean isHasMedia() {
            return hasMedia;
        }

        public void setHasMedia(boolean hasMedia) {
            this.hasMedia = hasMedia;
        }

        public String getExcerpt() {
            return excerpt;
        }

        public void setExcerpt(String excerpt) {
            this.excerpt = excerpt;
        }

        public LocalDateTime getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
        }
    }
}
