package com.fbadsautomation.integration.facebook;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j

/**
 * Request object for creating a Facebook ad
 */
public class FacebookAdRequest {
    private String campaignName;
    private String adName;
    private String headline;
    private String primaryText;
    private String description;
    private String imageUrl;
    private String cta;
    private Double budget;
    private String budgetType;
    private String objective;
}
