package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ImportConfirmRequest;
import com.fbadsautomation.dto.ImportReportResponse;
import com.fbadsautomation.dto.MatchedReportRow;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdPerformanceReport;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.AdPerformanceReportRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.UserRepository;
import com.fbadsautomation.security.JwtTokenProvider;
import com.fbadsautomation.service.FacebookReportParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for importing Facebook Ads Manager performance reports
 *
 * Security: User authorization, file validation, rate limiting (via interceptor)
 * Performance: Streaming parser, batch inserts
 * Maintainability: Clear REST API design
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */
@RestController
@RequestMapping("/facebook-report")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Facebook Report Import", description = "Import and manage Facebook Ads Manager performance reports")
public class FacebookReportController {

    private final FacebookReportParserService parserService;
    private final AdPerformanceReportRepository reportRepository;
    private final AdRepository adRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Preview import of Facebook report
     * Does not save to database - shows matching preview
     */
    @PostMapping("/import/preview")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Preview Facebook report import", description = "Parse and match ads without saving")
    public ResponseEntity<ImportReportResponse> previewImport(
        @RequestParam("file") MultipartFile file,
        Principal principal
    ) {
        log.info("User {} previewing report import", principal.getName());

        User user = getCurrentUser(principal);

        try {
            ImportReportResponse response;

            // Detect file type and parse accordingly
            String filename = file.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
                response = parserService.parseExcelReport(file, user.getId());
            } else {
                response = parserService.parseCsvReport(file, user.getId());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error previewing report import", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ImportReportResponse.builder()
                    .message("Failed to parse report: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Confirm and save imported reports
     */
    @PostMapping("/import/confirm")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Confirm Facebook report import", description = "Save matched reports to database")
    public ResponseEntity<Map<String, Object>> confirmImport(
        @Valid @RequestBody ImportConfirmRequest request,
        Principal principal
    ) {
        log.info("User {} confirming report import of {} reports", principal.getName(), request.getMatchedReports().size());

        User user = getCurrentUser(principal);

        try {
            // Build reports from matched rows
            List<AdPerformanceReport> reports = new ArrayList<>();

            for (MatchedReportRow row : request.getMatchedReports()) {
                Ad ad = adRepository.findById(row.getAdId())
                    .orElseThrow(() -> new RuntimeException("Ad not found: " + row.getAdId()));

                // Security: Verify ad belongs to user
                if (!ad.getUser().getId().equals(user.getId())) {
                    log.warn("User {} attempted to import report for ad {} owned by different user",
                        user.getId(), ad.getId());
                    continue;
                }

                AdPerformanceReport report = buildReport(row, ad, user, request.getSource());
                reports.add(report);
            }

            // Save all reports (batch operation)
            List<AdPerformanceReport> savedReports = reportRepository.saveAll(reports);

            log.info("Successfully imported {} reports for user {}", savedReports.size(), user.getId());

            return ResponseEntity.ok(Map.of(
                "imported", savedReports.size(),
                "message", "Successfully imported " + savedReports.size() + " reports",
                "reportIds", savedReports.stream().map(AdPerformanceReport::getId).toList()
            ));
        } catch (Exception e) {
            log.error("Error confirming import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to import reports: " + e.getMessage()
                ));
        }
    }

    /**
     * Get reports for a specific ad
     */
    @GetMapping("/ad/{adId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get ad performance reports", description = "Retrieve all reports for a specific ad")
    public ResponseEntity<List<AdPerformanceReport>> getAdReports(
        @PathVariable Long adId,
        Principal principal
    ) {
        User user = getCurrentUser(principal);

        List<AdPerformanceReport> reports = reportRepository.findByAdIdAndUserId(adId, user.getId());

        return ResponseEntity.ok(reports);
    }

    /**
     * Get reports for a campaign
     */
    @GetMapping("/campaign/{campaignId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get campaign performance reports", description = "Retrieve all reports for a campaign")
    public ResponseEntity<List<AdPerformanceReport>> getCampaignReports(
        @PathVariable Long campaignId,
        Principal principal
    ) {
        User user = getCurrentUser(principal);

        List<AdPerformanceReport> reports = reportRepository.findByCampaignIdAndUserId(campaignId, user.getId());

        return ResponseEntity.ok(reports);
    }

    /**
     * Get aggregated analytics for an ad
     */
    @GetMapping("/ad/{adId}/analytics")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get ad analytics", description = "Get aggregated performance metrics for an ad")
    public ResponseEntity<Map<String, Object>> getAdAnalytics(
        @PathVariable Long adId,
        Principal principal
    ) {
        User user = getCurrentUser(principal);

        Object[] metrics = reportRepository.getAggregatedMetricsByAdIdAndUserId(adId, user.getId());

        if (metrics == null || metrics.length == 0) {
            return ResponseEntity.ok(Map.of(
                "message", "No performance data available for this ad"
            ));
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalImpressions", metrics[0]);
        analytics.put("totalClicks", metrics[1]);
        analytics.put("avgCtr", metrics[2]);
        analytics.put("totalSpend", metrics[3]);
        analytics.put("avgCpc", metrics[4]);
        analytics.put("avgCpm", metrics[5]);
        analytics.put("totalConversions", metrics[6]);

        return ResponseEntity.ok(analytics);
    }

    /**
     * Get aggregated analytics for a campaign
     */
    @GetMapping("/campaign/{campaignId}/analytics")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get campaign analytics", description = "Get aggregated performance metrics for a campaign")
    public ResponseEntity<Map<String, Object>> getCampaignAnalytics(
        @PathVariable Long campaignId,
        Principal principal
    ) {
        User user = getCurrentUser(principal);

        Object[] metrics = reportRepository.getAggregatedMetricsByCampaignIdAndUserId(campaignId, user.getId());

        if (metrics == null || metrics.length == 0) {
            return ResponseEntity.ok(Map.of(
                "message", "No performance data available for this campaign"
            ));
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalImpressions", metrics[0]);
        analytics.put("totalClicks", metrics[1]);
        analytics.put("avgCtr", metrics[2]);
        analytics.put("totalSpend", metrics[3]);
        analytics.put("avgCpc", metrics[4]);
        analytics.put("avgCpm", metrics[5]);
        analytics.put("totalConversions", metrics[6]);

        return ResponseEntity.ok(analytics);
    }

    /**
     * Helper: Build AdPerformanceReport from MatchedReportRow
     */
    private AdPerformanceReport buildReport(MatchedReportRow row, Ad ad, User user, String source) {
        Map<String, Object> metrics = row.getMetrics();

        LocalDate reportDate = (LocalDate) metrics.getOrDefault("reportDate", LocalDate.now());

        return AdPerformanceReport.builder()
            .ad(ad)
            .campaign(ad.getCampaign())
            .user(user)
            .reportDate(reportDate)
            .impressions((Long) metrics.getOrDefault("impressions", 0L))
            .clicks((Long) metrics.getOrDefault("clicks", 0L))
            .ctr((Double) metrics.get("ctr"))
            .spend((Double) metrics.get("spend"))
            .cpc((Double) metrics.get("cpc"))
            .cpm((Double) metrics.get("cpm"))
            .conversions((Long) metrics.getOrDefault("conversions", 0L))
            .conversionRate((Double) metrics.get("conversionRate"))
            .source(source != null ? source : "FACEBOOK")
            .importedBy(user.getUsername())
            .build();
    }

    /**
     * Helper: Get current user from principal
     */
    private User getCurrentUser(Principal principal) {
        String username = principal.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
