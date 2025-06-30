package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdGenerationResponse {
    private Long adId; // ID của ad đã tạo
    private List<AdVariation> variations;
    private String status;
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdVariation {
        private Long id; // ID của AdContent
        private String headline;
        private String primaryText;
        private String description;
        private String callToAction;
        private String imageUrl;
        private Integer order;
    }
}

