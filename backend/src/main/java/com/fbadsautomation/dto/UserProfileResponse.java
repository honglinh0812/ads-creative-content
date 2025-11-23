package com.fbadsautomation.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private LocalDateTime createdDate;
    private ProfileStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileStats {
        private long totalCampaigns;
        private long totalAds;
        private List<RecentAdSummary> recentAds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAdSummary {
        private Long id;
        private String name;
        private String status;
        private LocalDateTime createdDate;
    }
}
