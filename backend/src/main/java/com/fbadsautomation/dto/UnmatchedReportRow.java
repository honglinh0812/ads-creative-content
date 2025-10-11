package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing an unmatched report row from Facebook import
 *
 * These rows could not be automatically matched to ads in the system
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnmatchedReportRow {

    private int rowNumber;
    private String facebookAdName;

    @Builder.Default
    private Map<String, Object> rawData = new HashMap<>();

    private String reason; // Why it couldn't be matched
}
