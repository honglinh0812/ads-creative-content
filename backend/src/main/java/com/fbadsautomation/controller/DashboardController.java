package com.fbadsautomation.controller;

import com.fbadsautomation.dto.DashboardResponse;
import com.fbadsautomation.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")

public class DashboardController {

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
