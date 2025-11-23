package com.fbadsautomation.controller;

import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.dto.AdImprovementRequest;
import com.fbadsautomation.dto.ReferenceAnalysisRequest;
import com.fbadsautomation.dto.ReferenceAnalysisResponse;
import com.fbadsautomation.service.AdImprovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ads/learn")
@Tag(name = "Ad Learning", description = "Endpoints học tập & cải thiện quảng cáo từ link tham chiếu")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Slf4j
public class AdImprovementController {

    private final AdImprovementService adImprovementService;

    @Operation(summary = "Phân tích link quảng cáo tham chiếu",
            description = "Kiểm tra URL Meta Ad Library và (nếu có access token) lấy nội dung để làm mẫu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phân tích thành công",
                    content = @Content(schema = @Schema(implementation = ReferenceAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "URL hoặc token không hợp lệ"),
            @ApiResponse(responseCode = "502", description = "Không lấy được dữ liệu từ Meta API")
    })
    @PostMapping("/reference")
    public ResponseEntity<ReferenceAnalysisResponse> analyzeReference(
            @Valid @RequestBody ReferenceAnalysisRequest request) {
        return ResponseEntity.ok(adImprovementService.analyzeReference(request));
    }

    @Operation(summary = "Sinh quảng cáo cải thiện",
            description = "Kết hợp nội dung người dùng và phong cách từ quảng cáo tham chiếu để sinh biến thể mới")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sinh quảng cáo thành công",
                    content = @Content(schema = @Schema(implementation = AdGenerationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Thiếu dữ liệu bắt buộc"),
            @ApiResponse(responseCode = "500", description = "Lỗi sinh nội dung")
    })
    @PostMapping("/generate")
    public ResponseEntity<AdGenerationResponse> generateImprovedAds(
            @Valid @RequestBody AdImprovementRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("Generating improved ads for user {} from reference {}", userId, request.getReferenceLink());
        return ResponseEntity.ok(adImprovementService.generateImprovedAds(request, userId));
    }

    @Operation(summary = "Sinh quảng cáo cải thiện (async)",
            description = "Tạo job async để sinh quảng cáo dựa trên phong cách tham chiếu")
    @PostMapping("/async/generate")
    public ResponseEntity<Map<String, Object>> generateImprovedAdsAsync(
            @Valid @RequestBody AdImprovementRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("Starting async improved ad generation for user {} from reference {}", userId, request.getReferenceLink());
        Map<String, Object> response = adImprovementService.generateImprovedAdsAsync(request, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Lưu quảng cáo cải thiện đã chọn",
            description = "Tái sử dụng logic lưu `AdGenerationRequest` để ghi vào database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lưu quảng cáo thành công",
                    content = @Content(schema = @Schema(implementation = AdGenerationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không có biến thể được chọn")
    })
    @PostMapping("/save")
    public ResponseEntity<AdGenerationResponse> saveImprovedAds(
            @Valid @RequestBody AdGenerationRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("Saving improved ads for user {} into campaign {}", userId, request.getCampaignId());
        return ResponseEntity.ok(adImprovementService.saveImprovedAds(request, userId));
    }
}
