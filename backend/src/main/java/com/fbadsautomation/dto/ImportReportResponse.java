package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for Facebook report import preview
 *
 * Contains matched and unmatched rows for user review before final import
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportReportResponse {

    private int totalRows;
    private int matchedRows;
    private int unmatchedRows;

    @Builder.Default
    private List<MatchedReportRow> matchedReports = new ArrayList<>();

    @Builder.Default
    private List<UnmatchedReportRow> unmatchedReports = new ArrayList<>();

    private String message;
    private boolean hasWarnings;
}
