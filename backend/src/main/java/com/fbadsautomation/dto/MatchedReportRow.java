package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing a matched report row from Facebook import
 *
 * Contains the Facebook ad name, matched internal ad, and metrics
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchedReportRow {

    private String facebookAdName;
    private Long adId;
    private String adName;
    private Long campaignId;
    private String campaignName;

    /**
     * Match confidence score (0.0 to 1.0)
     * 1.0 = exact match, < 1.0 = fuzzy match
     */
    @Builder.Default
    private Double matchConfidence = 1.0;

    /**
     * Raw metrics from Facebook report
     */
    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    /**
     * Match type for debugging
     */
    private String matchType; // "EXACT", "ID_EXTRACTION", "FUZZY"
}
