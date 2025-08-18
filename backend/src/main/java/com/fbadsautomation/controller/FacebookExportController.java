package com.fbadsautomation.controller;

import com.fbadsautomation.service.FacebookExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facebook-export")
@RequiredArgsConstructor
@Slf4j
public class FacebookExportController {
    
    private final FacebookExportService facebookExportService;
    
    @GetMapping("/ad/{adId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> exportAdToFacebookTemplate(@PathVariable Long adId) {
        log.info("Exporting ad {} to Facebook template", adId);
        return facebookExportService.exportAdToFacebookTemplate(adId);
    }
    
    @PostMapping("/ads/bulk")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> exportMultipleAdsToFacebookTemplate(@RequestBody List<Long> adIds) {
        log.info("Exporting {} ads to Facebook template", adIds.size());
        return facebookExportService.exportMultipleAdsToFacebookTemplate(adIds);
    }
}