package com.fbadsautomation.service;

import com.fbadsautomation.util.ValidationMessages;
import com.fbadsautomation.util.ValidationMessages.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * AI-powered contextual prompt improvement service
 * Uses OpenAI to generate natural, context-aware prompt suggestions
 */
@Service
public class AIPromptImprovementService {

    private static final Logger log = LoggerFactory.getLogger(AIPromptImprovementService.class);

    @Autowired
    private AIProviderService aiProviderService;

    /**
     * Generate contextual improvement for a prompt using AI
     * Cached for 24 hours to reduce API costs
     *
     * @param originalPrompt The original user prompt
     * @param adType The ad type for context
     * @param targetAudience Target audience if provided
     * @return Improved prompt suggestion, or null if improvement not needed/failed
     */
    @Cacheable(value = "improvedPrompts", key = "#originalPrompt + '_' + #adType + '_' + #targetAudience", unless = "#result == null")
    public String generateImprovedPrompt(String originalPrompt, String adType, String targetAudience) {
        if (originalPrompt == null || originalPrompt.trim().length() < 20) {
            log.debug("Prompt too short for AI improvement: {}", originalPrompt);
            return null; // Too short to improve meaningfully
        }

        try {
            // Detect language for appropriate system prompt
            Language language = ValidationMessages.detectLanguage(originalPrompt);
            log.info("Generating AI-powered prompt improvement for language: {}", language);

            // Build AI improvement prompt
            String improvementPrompt = buildImprovementPrompt(originalPrompt, adType, targetAudience, language);

            // Get AI provider (use interface, not concrete class to avoid ClassCastException)
            com.fbadsautomation.ai.AIProvider provider = aiProviderService.getProvider("openai");
            if (provider == null) {
                log.warn("OpenAI provider not available for prompt improvement");
                return null;
            }

            // Generate improvement using AI provider
            String improved = callAIForImprovement(provider, improvementPrompt);

            if (improved != null && !improved.trim().isEmpty() && !improved.equals(originalPrompt)) {
                log.info("Successfully generated improved prompt (length: {} -> {})",
                    originalPrompt.length(), improved.length());
                return improved.trim();
            }

            log.debug("AI did not generate meaningful improvement");
            return null;

        } catch (Exception e) {
            log.error("Failed to generate improved prompt: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Call AI provider to improve prompt
     */
    private String callAIForImprovement(com.fbadsautomation.ai.AIProvider provider, String improvementPrompt) {
        try {
            // Use AI provider's generateAdContent with special prompt
            // This reuses existing API call infrastructure
            var contents = provider.generateAdContent(improvementPrompt, 1, null, null);

            if (contents != null && !contents.isEmpty()) {
                // Return the primary text as improved prompt (most detailed and contextual)
                String improved = contents.get(0).getPrimaryText();

                // Fallback to headline + description if primaryText is empty
                if (improved == null || improved.trim().isEmpty()) {
                    improved = contents.get(0).getHeadline() + ". " + contents.get(0).getDescription();
                }

                return improved;
            }
        } catch (Exception e) {
            log.error("AI API call failed for prompt improvement: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Build the system prompt for AI to improve user's prompt
     */
    private String buildImprovementPrompt(String original, String adType, String targetAudience, Language language) {
        StringBuilder prompt = new StringBuilder();

        if (language == Language.VIETNAMESE) {
            prompt.append("Bạn là chuyên gia tối ưu hóa prompt quảng cáo Facebook cho thị trường Việt Nam.\n\n");
            prompt.append("NHIỆM VỤ: Cải thiện prompt dưới đây để tạo quảng cáo Facebook tốt hơn. ");
            prompt.append("Trả về PROMPT CẢI TIẾN trong 1-2 câu ngắn gọn.\n\n");

            prompt.append("PROMPT GỐC:\n\"");
            prompt.append(original).append("\"\n\n");

            prompt.append("YÊU CẦU CẢI THIỆN:\n");
            prompt.append("1. Làm rõ sản phẩm/dịch vụ (tên, tính năng độc đáo, giá trị)\n");
            prompt.append("2. Xác định đối tượng khách hàng mục tiêu (tuổi, sở thích, pain points)\n");
            prompt.append("3. Thêm lời kêu gọi hành động cụ thể (mua ngay, đăng ký, tìm hiểu...)\n");
            prompt.append("4. Thêm yếu tố cảm xúc hoặc lợi ích thiết thực cho khách hàng\n");
            prompt.append("5. Giữ nguyên tiếng Việt, tự nhiên, không sáo rỗng hoặc quá formal\n\n");

            if (targetAudience != null && !targetAudience.trim().isEmpty()) {
                prompt.append("ĐỐI TƯỢNG MỤC TIÊU: ").append(targetAudience).append("\n\n");
            }

            if (adType != null && !adType.trim().isEmpty()) {
                prompt.append("LOẠI QUẢNG CÁO: ").append(mapAdTypeToVietnamese(adType)).append("\n\n");
            }

            prompt.append("CHỈ TRẢ VỀ PROMPT CẢI TIẾN (1-2 câu, không giải thích, không markdown):");

        } else {
            prompt.append("You are an expert at optimizing Facebook ad prompts.\n\n");
            prompt.append("TASK: Improve the prompt below to create better Facebook ads. ");
            prompt.append("Return the IMPROVED PROMPT in 1-2 concise sentences.\n\n");

            prompt.append("ORIGINAL PROMPT:\n\"");
            prompt.append(original).append("\"\n\n");

            prompt.append("IMPROVEMENT REQUIREMENTS:\n");
            prompt.append("1. Clarify product/service (name, unique features, value)\n");
            prompt.append("2. Define target audience (age, interests, pain points)\n");
            prompt.append("3. Add specific call-to-action (buy now, sign up, learn more...)\n");
            prompt.append("4. Add emotional triggers or tangible customer benefits\n");
            prompt.append("5. Keep it natural, avoid corporate jargon or being overly formal\n\n");

            if (targetAudience != null && !targetAudience.trim().isEmpty()) {
                prompt.append("TARGET AUDIENCE: ").append(targetAudience).append("\n\n");
            }

            if (adType != null && !adType.trim().isEmpty()) {
                prompt.append("AD TYPE: ").append(adType).append("\n\n");
            }

            prompt.append("RETURN ONLY THE IMPROVED PROMPT (1-2 sentences, no explanations, no markdown):");
        }

        return prompt.toString();
    }

    /**
     * Map ad type enum to Vietnamese display name
     */
    private String mapAdTypeToVietnamese(String adType) {
        return switch (adType.toUpperCase()) {
            case "WEBSITE_CONVERSION_AD" -> "Chuyển đổi website";
            case "LEAD_FORM_AD", "LEAD_GENERATION" -> "Thu thập khách hàng tiềm năng";
            case "PAGE_POST_AD", "PAGE_POST" -> "Bài viết trang";
            default -> adType;
        };
    }
}
