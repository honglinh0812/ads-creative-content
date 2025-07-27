package com.fbadsautomation.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j

public class AdResponse {
    private Long id;
    private String name;
    private String adType;
    private String status;
    private String campaignName;
    private String imageUrl;
    private String videoUrl;
    private String headline;
    private String description;
    private String primaryText;
    private String callToAction;
    private LocalDateTime createdDate;
}