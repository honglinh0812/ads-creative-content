package com.fbadsautomation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Structured representation của quảng cáo tham chiếu từ Meta Ad Library.
 */
@Value
@Builder
public class ReferenceAdData {
    String headline;
    String primaryText;
    String description;
    String callToAction;
    String mediaUrl;
    List<String> toneHints;
    List<String> hooks;
    List<String> hashtags;
}
