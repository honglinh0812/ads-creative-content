package com.fbadsautomation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Fingerprint mô tả phong cách của quảng cáo tham chiếu sau khi phân tích.
 */
@Value
@Builder
public class ReferenceStyleProfile {
    String hookType;
    String tone;
    String pacing;
    Boolean usesEmoji;
    Boolean usesQuestions;
    Boolean usesSecondPerson;
    List<String> emojiSamples;
    List<String> styleNotes;
    List<String> punctuation;
    String ctaVerb;
}
