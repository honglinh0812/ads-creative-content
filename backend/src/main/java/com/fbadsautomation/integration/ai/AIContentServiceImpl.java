package com.fbadsautomation.integration.ai;


import com.fbadsautomation.ai.AIProvider; // Import the main interface
import com.fbadsautomation.ai.GeminiProvider;
import com.fbadsautomation.ai.HuggingFaceProvider;
import com.fbadsautomation.ai.OpenAIProvider;
import com.fbadsautomation.service.AIProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import com.fbadsautomation.model.AdContent; // Import AdContent
import com.fbadsautomation.model.AdType; // Import AdType
import com.fbadsautomation.exception.ApiException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for generating ad content using AI based on ad type
 */
@Service
@Slf4j
public class AIContentServiceImpl {

    private final AIProviderService aiProviderService;

    @Autowired
    public AIContentServiceImpl(AIProviderService aiProviderService) {
        this.aiProviderService = aiProviderService;
    }

    public List<AdContent> generateContent(String prompt,
                                           AdContent.ContentType contentType,
                                           String textProvider,
                                           String imageProvider,
                                           int numberOfVariations,
                                           String language) {
        
        String providerId = (textProvider == null || textProvider.isBlank())
                ? "openai" : textProvider;
        AIProvider textAI = aiProviderService.getProvider(providerId);
        if (textAI == null) {
            log.error("Unsupported text AI provider: {}", providerId);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported AI provider: " + providerId);
        }

        AIProvider imageAI = null;
        if (imageProvider != null && !imageProvider.isBlank()) {
            imageAI = aiProviderService.getProvider(imageProvider);
            if (imageAI == null) {
                log.error("Unsupported image AI provider: {}", imageProvider);
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported image AI provider: " + imageProvider);
            }
        }

        try {
            AdType adType = convertContentTypeToAdType(contentType);
            String enhancedPrompt = enhancePromptForAdType(prompt, adType);

            log.info("Generating {} text variations using {}", numberOfVariations, textAI.getProviderName());
            List<AdContent> contents = textAI.generateAdContent(enhancedPrompt, numberOfVariations, language);

            if (imageAI != null) {
                for (AdContent content : contents) {
                    String imagePrompt = content.getPrimaryText();
                    String imageUrl = imageAI.generateImage(imagePrompt);
                    content.setImageUrl(imageUrl);
                }
                log.info("Images added using {}", imageAI.getProviderName());
            }

            return contents;

        } catch (Exception e) {
            log.error("Failed to generate content with {}: {}", textAI.getProviderName(), e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate content: " + e.getMessage(), e);
        }
    }

    /**
     * Convert ContentType to AdType for backward compatibility or prompt enhancement.
     * Corrected to use actual values from AdContent.ContentType enum.
     */
    private AdType convertContentTypeToAdType(AdContent.ContentType contentType) {
        if (contentType == null) {
            return AdType.PAGE_POST; // Default if null
        }
        // Corrected switch cases to use actual enum constants from AdContent.ContentType
        switch (contentType) {
            case TEXT: 
            case IMAGE: 
            case COMBINED: 
            case PAGE_POST: // This was likely intended to map to AdType.PAGE_POST
            default:
                // Currently, all ContentTypes map to AdType.PAGE_POST for prompt enhancement.
                // This logic can be refined if different ContentTypes should map to different AdTypes.
                return AdType.PAGE_POST; 
        }
    }

    /**
     * Enhance user prompt based on ad type to potentially get better AI-generated content.
     */
    private String enhancePromptForAdType(String userPrompt, AdType adType) {
        // This prompt enhancement logic can be kept or modified based on requirements.
        // It adds context based on the intended Facebook ad type.
        String basePrompt = "Tạo nội dung quảng cáo Facebook với:";
        String typeSpecificPrompt;

        // Corrected: Use unqualified enum names in switch cases
        if (adType == null) {
             return basePrompt + "\n" + userPrompt; // Default if null
        }
        // Corrected switch cases to use unqualified enum names
        switch (adType) {
            case PAGE_POST: 
                typeSpecificPrompt =
                    "Tạo nội dung quảng cáo Facebook dạng Page Post Ad với:\n" +
                    "- Primary text: Nội dung chính của bài đăng\n" +
                    "- Headline: Tiêu đề ngắn gọn, hấp dẫn\n" +
                    "- Description: Mô tả chi tiết\n" +
                    "- CTA: Nút kêu gọi hành động phù hợp\n" +
                    "Yêu cầu: " + userPrompt;
                break;

            case LEAD_FORM: 
                typeSpecificPrompt =
                    "Tạo nội dung quảng cáo Facebook dạng Lead Form Ad với:\n" +
                    "- Primary text: Nội dung chính nhấn mạnh giá trị người dùng nhận được khi điền form\n" +
                    "- Headline: Tiêu đề ngắn gọn, hấp dẫn, khuyến khích điền form\n" +
                    "- Description: Mô tả lợi ích khi đăng ký\n" +
                    "- CTA: Nút kêu gọi hành động liên quan đến đăng ký/liên hệ\n" +
                    "Yêu cầu: " + userPrompt;
                break;

            case WEBSITE_CONVERSION: 
                typeSpecificPrompt =
                    "Tạo nội dung quảng cáo Facebook dạng Website Conversion Ad với:\n" +
                    "- Primary text: Nội dung chính nhấn mạnh giá trị và lợi ích khi truy cập website\n" +
                    "- Headline: Tiêu đề ngắn gọn, hấp dẫn, thúc đẩy chuyển đổi\n" +
                    "- Description: Mô tả chi tiết về sản phẩm/dịch vụ và lợi ích\n" +
                    "- CTA: Nút kêu gọi hành động liên quan đến mua hàng/đăng ký\n" +
                    "Yêu cầu: " + userPrompt;
                break;

            default:
                typeSpecificPrompt = basePrompt + "\n" + userPrompt;
        }

        return typeSpecificPrompt;
    }

    // Removed the old generator dependencies and the legacy generateAdContent method
    // Removed the internal AIProvider enum as it's now defined in the ai package
}

