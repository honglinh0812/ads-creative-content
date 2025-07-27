package com.fbadsautomation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j

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
    };
    }
