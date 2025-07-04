package com.fbadsautomation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdResponse {
    private Long id;
    private String name;
    private String adType;
    private String status;
    private String campaignName;
    private String imageUrl;
    private String headline;
    private String description;
    private String primaryText;
    private String callToAction;
    private LocalDateTime createdDate;
}