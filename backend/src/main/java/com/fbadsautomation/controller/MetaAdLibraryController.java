package com.fbadsautomation.controller;

import com.fbadsautomation.service.MetaAdLibraryApiService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/meta-ad-library")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

public class MetaAdLibraryController {

    private final MetaAdLibraryApiService metaAdLibraryApiService;

    /**
     * Extract ad content from Meta Ad Library URL
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractAdContent(
            @RequestParam String url,
            @RequestParam(required = false) String accessToken) {
        
        log.info("Extracting ad content from URL: {}", url);
        
        try {
            if (!metaAdLibraryApiService.isValidAdLibraryUrl(url)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "URL không hợp lệ"));
            }
            String adId = metaAdLibraryApiService.extractAdIdFromUrl(url);
            if (adId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Không thể trích xuất Ad ID từ URL"));
            }
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                Map<String, Object> result = metaAdLibraryApiService.extractAdContent(adId, accessToken);
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "adId", adId,
                    "message", "Đã trích xuất Ad ID. Sử dụng Meta API với access token để lấy nội dung chi tiết.",
                    "apiUrl", String.format("https://graph.facebook.com/v18.0/%s?fields= id,ad_creative,ad_snapshot_url&access_token = YOUR_ACCESS_TOKEN", adId)
                ));
            }
        } catch (Exception e) {
            log.error("Error extracting ad content: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Validate Meta Ad Library URL
     */
    @PostMapping("/validate-url")
    public ResponseEntity<Map<String, Object>> validateUrl(@RequestParam String url) {
        boolean isValid = metaAdLibraryApiService.isValidAdLibraryUrl(url);
        String adId = isValid ? metaAdLibraryApiService.extractAdIdFromUrl(url) : null;
        
        return ResponseEntity.ok(Map.of(
            "isValid", isValid,
            "adId", adId,
            "message", isValid ? "URL hợp lệ" : "URL không hợp lệ"
        ));
    }
}