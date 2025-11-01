package com.fbadsautomation.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@CrossOrigin(origins = {"https://linhnh.site", "https://api.linhnh.site"})
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @GetMapping
    public ResponseEntity<?> getSettings() {
        log.info("Getting user settings");
        Map<String, Object> settings = new HashMap<>();
        
        // General settings
        Map<String, Object> general = new HashMap<>();
        general.put("language", "en");
        general.put("timezone", "Asia/Ho_Chi_Minh");
        general.put("theme", "light");
        general.put("autoSave", true);
        
        // AI settings
        Map<String, Object> ai = new HashMap<>();
        ai.put("defaultProvider", "openai");
        ai.put("creativity", 0.7);
        ai.put("quality", "high");
        ai.put("autoOptimize", true);
        
        // Notification settings
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("emailNotifications", true);
        notifications.put("pushNotifications", true);
        notifications.put("campaignUpdates", true);
        notifications.put("weeklyReports", true);
        notifications.put("browserNotifications", true);
        
        settings.put("general", general);
        settings.put("ai", ai);
        settings.put("notifications", notifications);
        
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/general")
    public ResponseEntity<?> updateGeneralSettings(@RequestBody Map<String, Object> generalSettings) {
        log.info("Updating general settings");
        // For now, just return success - can be implemented later
        return ResponseEntity.ok().build();
    }

    @PutMapping("/ai")
    public ResponseEntity<?> updateAISettings(@RequestBody Map<String, Object> aiSettings) {
        log.info("Updating AI settings");
        // For now, just return success - can be implemented later
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody Map<String, Object> notificationSettings) {
        log.info("Updating notification settings");
        // For now, just return success - can be implemented later
        return ResponseEntity.ok().build();
    }
}