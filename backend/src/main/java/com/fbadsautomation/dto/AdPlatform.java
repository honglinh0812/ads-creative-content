package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported ad platforms for competitor search features.
 */
public enum AdPlatform {

    META("meta"),
    FACEBOOK("facebook"),
    INSTAGRAM("instagram"),
    GOOGLE("google"),
    YOUTUBE("youtube"),
    TIKTOK("tiktok"),
    UNKNOWN("unknown");

    private final String jsonValue;

    AdPlatform(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
