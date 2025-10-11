package com.fbadsautomation.controller;

import com.fbadsautomation.service.FacebookExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facebook-export")
@RequiredArgsConstructor
@Tag(name = "Facebook Export", description = "Export ads to Facebook-compatible formats")
@SecurityRequirement(name = "bearerAuth")
public class FacebookExportController {
    private static final Logger log = LoggerFactory.getLogger(FacebookExportController.class);

    private final FacebookExportService facebookExportService;

    @Operation(summary = "Export single ad to CSV", description = "Export a single ad to Facebook CSV template")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad exported successfully"),
        @ApiResponse(responseCode = "404", description = "Ad not found"),
        @ApiResponse(responseCode = "400", description = "Invalid ad data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/ad/{adId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> exportAdToFacebookTemplate(
            @Parameter(description = "ID of the ad to export", required = true)
            @PathVariable Long adId) {
        log.info("Exporting ad {} to Facebook template", adId);
        return facebookExportService.exportAdToFacebookTemplate(adId);
    }

    @Operation(summary = "Export multiple ads to CSV (deprecated)",
               description = "Export multiple ads to Facebook CSV template. Use /ads/bulk/export for format selection.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ads exported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or ad data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/ads/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> exportMultipleAdsToFacebookTemplate(
            @Parameter(description = "List of ad IDs to export", required = true)
            @RequestBody @Valid @NotNull @Size(min = 1, max = 1000) List<Long> adIds) {
        log.info("Exporting {} ads to Facebook template (CSV)", adIds.size());
        return facebookExportService.exportMultipleAdsToFacebookTemplate(adIds);
    }

    @Operation(summary = "Bulk export ads with format selection (Phase 4)",
               description = "Export multiple ads to Facebook-compatible format. Supports CSV and Excel (.xlsx)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ads exported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid format or ad data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/ads/bulk/export")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> bulkExportAds(
            @Parameter(description = "Export request with ad IDs and format", required = true)
            @RequestBody @Valid BulkExportRequest request) {
        log.info("Bulk exporting {} ads in format: {}", request.getAdIds().size(), request.getFormat());
        return facebookExportService.exportAdsBulk(request.getAdIds(), request.getFormat());
    }

    @Operation(summary = "Preview Facebook format for single ad",
               description = "Preview how the ad will appear in Facebook export format without downloading")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preview generated successfully"),
        @ApiResponse(responseCode = "404", description = "Ad not found"),
        @ApiResponse(responseCode = "400", description = "Invalid ad data")
    })
    @GetMapping("/preview/ad/{adId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> previewFacebookFormat(
            @Parameter(description = "ID of the ad to preview", required = true)
            @PathVariable Long adId) {
        log.info("Previewing Facebook format for ad: {}", adId);
        Map<String, Object> preview = facebookExportService.previewFacebookFormat(adId);
        return ResponseEntity.ok(preview);
    }

    @Operation(summary = "Preview Facebook format for multiple ads",
               description = "Preview how multiple ads will appear in Facebook export format")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preview generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or ad data")
    })
    @PostMapping("/preview/ads/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> previewMultipleFacebookFormat(
            @Parameter(description = "List of ad IDs to preview", required = true)
            @RequestBody @Valid @NotNull @Size(min = 1, max = 100) List<Long> adIds) {
        log.info("Previewing Facebook format for {} ads", adIds.size());
        Map<String, Object> preview = facebookExportService.previewMultipleFacebookFormat(adIds);
        return ResponseEntity.ok(preview);
    }

    /**
     * DTO for bulk export request with format selection
     * Security: Input validation, size limits
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkExportRequest {
        @NotNull(message = "Ad IDs list cannot be null")
        @Size(min = 1, max = 1000, message = "Must export between 1 and 1000 ads")
        private List<Long> adIds;

        @NotNull(message = "Export format cannot be null")
        @Parameter(description = "Export format: csv, excel, or xlsx", example = "excel")
        private String format;
    }
}