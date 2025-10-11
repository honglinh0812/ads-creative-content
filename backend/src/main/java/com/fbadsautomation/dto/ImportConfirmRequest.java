package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for confirming Facebook report import
 *
 * User reviews matched reports and confirms import
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportConfirmRequest {

    @NotNull(message = "Matched reports list cannot be null")
    @NotEmpty(message = "Must have at least one matched report to import")
    @Size(max = 1000, message = "Cannot import more than 1000 reports at once")
    @Valid
    private List<MatchedReportRow> matchedReports;

    private String source; // Default: "FACEBOOK"
}
