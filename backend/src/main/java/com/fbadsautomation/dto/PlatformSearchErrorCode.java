package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Canonical error codes for platform search responses.
 */
public enum PlatformSearchErrorCode {

    NONE("none"),
    NO_RESULTS("no_results"),
    REGION_UNSUPPORTED("region_unsupported"),
    CONFIG_MISSING("config_missing"),
    RATE_LIMITED("rate_limited"),
    QUOTA_EXCEEDED("quota_exceeded"),
    PROVIDER_ERROR("provider_error"),
    TEMPORARY_ERROR("temporary_error"),
    VALIDATION_ERROR("validation_error"),
    UNKNOWN("unknown");

    private final String jsonValue;

    PlatformSearchErrorCode(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
