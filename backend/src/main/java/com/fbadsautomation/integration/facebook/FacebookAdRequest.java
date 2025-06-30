package com.fbadsautomation.integration.facebook;

import lombok.Data;

/**
 * Request object for creating a Facebook ad
 */
@Data
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
