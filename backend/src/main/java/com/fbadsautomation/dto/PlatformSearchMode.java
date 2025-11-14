package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Rendering mode for competitor search results.
 */
public enum PlatformSearchMode {

    DATA("data"),
    IFRAME("iframe"),
    EMPTY("empty"),
    ERROR("error");

    private final String jsonValue;

    PlatformSearchMode(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
