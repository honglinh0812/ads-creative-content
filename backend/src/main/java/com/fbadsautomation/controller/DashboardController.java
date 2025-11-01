package com.fbadsautomation.controller;

import com.fbadsautomation.dto.DashboardResponse;
import com.fbadsautomation.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")

public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        try {
            Long userId = Long.valueOf(authentication.getName());
            DashboardResponse dashboard = dashboardService.getDashboardData(userId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            // Ghi log đầy đủ để hiện ra trong file logs
            log.error("🔥 Error loading dashboard for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
