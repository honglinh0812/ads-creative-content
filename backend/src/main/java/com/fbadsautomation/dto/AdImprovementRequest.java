package com.fbadsautomation.dto;

import com.fbadsautomation.dto.AdGenerationRequest.LeadFormQuestion;
import com.fbadsautomation.dto.AdGenerationRequest.VariationProviderConfig;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.dto.AudienceSegmentRequest;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload dành cho chức năng học tập & cải thiện quảng cáo dựa trên link tham chiếu.
 */
@Data
public class AdImprovementRequest {

    @NotNull(message = "Campaign ID là bắt buộc")
    private Long campaignId;

    @NotBlank(message = "Loại quảng cáo là bắt buộc")
    private String adType;

    @NotBlank(message = "Tên quảng cáo là bắt buộc")
    private String name;

    @Size(max = 10000, message = "Phần mô tả sản phẩm không được vượt quá 10.000 ký tự")
    private String productDescription;

    @Size(max = 5000, message = "Phong cách mô tả không được vượt quá 5.000 ký tự")
    private String creativeStyle;

    @Pattern(regexp = "^https?://.*$", message = "Link tham chiếu phải là URL hợp lệ")
    private String referenceLink;

    @Size(max = 10000, message = "Nội dung tham chiếu không được vượt quá 10.000 ký tự")
    private String referenceContent;

    private ReferenceAdData referenceAdData;

    private String referenceAccessToken;

    private ReferenceInsights referenceInsights;

    @NotBlank(message = "Text provider là bắt buộc")
    private String textProvider;

    private String imageProvider;

    @NotNull(message = "Số lượng biến thể là bắt buộc")
    @Min(value = 1, message = "Tối thiểu 1 biến thể")
    @Max(value = 10, message = "Tối đa 10 biến thể")
    private Integer numberOfVariations;

    private String language;

    private FacebookCTA callToAction;

    private String mediaFileUrl;

    private String websiteUrl;

    /**
     * When true, prompt builder skips Facebook character limit instructions.
     * Defaults to false for backward compatibility.
     */
    private Boolean allowUnlimitedLength;

    @Valid
    private List<LeadFormQuestion> leadFormQuestions;

    @Valid
    private AudienceSegmentRequest audienceSegment;

    private Long personaId;

    @Size(max = 10, message = "Tối đa 10 từ khóa thịnh hành")
    private List<String> trendingKeywords;

    @Valid
    private List<VariationProviderConfig> variations;

    @Data
    public static class ReferenceInsights {
        private Integer wordCount;
        private Integer sentenceCount;
        private Boolean containsCallToAction;
        private Boolean containsPrice;
    }
}
