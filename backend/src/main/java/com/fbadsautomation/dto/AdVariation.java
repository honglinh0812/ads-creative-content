package com.fbadsautomation.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j

public class AdVariation {
    private String headline;
    private String description;
    private String primaryText;
    private String callToAction;
    private String imageUrl;
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // Có thể bổ sung các trường khác nếu cần;
    } 