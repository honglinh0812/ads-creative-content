package com.fbadsautomation.controller;

import com.fbadsautomation.service.MetaAdLibraryApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meta-ad-library")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Meta Ad Library", description = "API endpoints for extracting content from Meta Ad Library")

public class MetaAdLibraryController {

    private static final Logger log = LoggerFactory.getLogger(MetaAdLibraryController.class);
    private final MetaAdLibraryApiService metaAdLibraryApiService;

    @Operation(summary = "Extract ad content", description = "Extract ad content from Meta Ad Library URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad content extracted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid URL or extraction error"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractAdContent(
            @Parameter(description = "Meta Ad Library URL") @RequestParam String url,
            @Parameter(description = "Facebook access token (optional)") @RequestParam(required = false) String accessToken) {
        
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

    @Operation(summary = "Validate Meta Ad Library URL", description = "Validates if the provided URL is a valid Meta Ad Library URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL validation result"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/validate-url")
    public ResponseEntity<Map<String, Object>> validateUrl(
            @Parameter(description = "Meta Ad Library URL to validate") @RequestParam String url) {
        boolean isValid = metaAdLibraryApiService.isValidAdLibraryUrl(url);
        String adId = isValid ? metaAdLibraryApiService.extractAdIdFromUrl(url) : null;
        
        return ResponseEntity.ok(Map.of(
            "isValid", isValid,
            "adId", adId,
            "message", isValid ? "URL hợp lệ" : "URL không hợp lệ"
        ));
    }
}