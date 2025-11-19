package com.fbadsautomation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * Request dùng để kiểm tra/phân tích quảng cáo tham chiếu.
 */
@Data
public class ReferenceAnalysisRequest {

    @NotBlank(message = "Cần cung cấp link quảng cáo tham chiếu")
    @Pattern(regexp = "^https?://.*$", message = "Link quảng cáo phải là URL hợp lệ")
    private String referenceLink;

    @Size(max = 4096, message = "Access token không hợp lệ")
    private String accessToken;

    @Size(max = 10000, message = "Nội dung tham chiếu không được vượt quá 10.000 ký tự")
    private String fallbackContent;
}
