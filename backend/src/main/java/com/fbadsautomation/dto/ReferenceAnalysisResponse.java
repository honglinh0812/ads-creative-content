package com.fbadsautomation.dto;

import com.fbadsautomation.model.FacebookCTA;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Response chuẩn cho endpoint phân tích link tham chiếu.
 */
@Value
@Builder
public class ReferenceAnalysisResponse {
    boolean success;
    String referenceAdId;
    String referenceContent;
    Map<String, Object> metadata;
    String message;
    String detectedStyle;
    FacebookCTA suggestedCallToAction;
    ReferenceAdData referenceAdData;
    ReferenceInsights insights;
    ReferenceStyleProfile styleProfile;

    @Value
    @Builder
    public static class ReferenceInsights {
        int wordCount;
        int sentenceCount;
        boolean containsCallToAction;
        boolean containsPrice;
    }
}
