package com.fbadsautomation.service;

import com.fbadsautomation.model.AdStyle;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.model.Persona;
import com.fbadsautomation.util.ValidationMessages.Language;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 3: Unified Chain-of-Thought (CoT) Prompt Builder
 *
 * This builder creates a 6-stage CoT prompt that guides LLMs through explicit reasoning:
 * 1. Task Understanding
 * 2. Audience Analysis
 * 3. Creative Direction
 * 4. Constraints & Requirements
 * 5. Reasoning Process
 * 6. Generation Instruction
 *
 * Key Features:
 * - Unified prompt structure for all AI providers (OpenAI, Anthropic, Gemini, HuggingFace)
 * - Bilingual support (Vietnamese/English)
 * - Strict language enforcement to eliminate cross-language contamination
 * - Integration of persona, trending keywords, ad style, and campaign audience
 * - Explicit reasoning steps to improve output quality
 */
@Service
@RequiredArgsConstructor
public class ChainOfThoughtPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(ChainOfThoughtPromptBuilder.class);

    /**
     * Build unified Chain-of-Thought prompt with all input fields.
     * This is the main entry point for Phase 3.
     *
     * @param userPrompt Base user prompt describing the product/service
     * @param persona User-selected or auto-selected persona (can be null)
     * @param adStyle Creative style (optional)
     * @param targetAudience Campaign target audience description
     * @param trendingKeywords Trending keywords to incorporate (optional)
     * @param language Output language (CRITICAL for enforcement)
     * @param callToAction Facebook CTA
     * @param adType Ad type (PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD)
     * @param numberOfVariations Number of variations to generate
     * @return Complete CoT prompt ready for any provider
     */
    public String buildCoTPrompt(
        String userPrompt,
        Persona persona,
        AdStyle adStyle,
        String targetAudience,
        List<String> trendingKeywords,
        Language language,
        FacebookCTA callToAction,
        AdType adType,
        int numberOfVariations
    ) {
        log.info("[Phase 3] Building CoT prompt: language={}, adType={}, variations={}, persona={}, keywords={}",
                language, adType, numberOfVariations,
                persona != null ? persona.getName() : "none",
                trendingKeywords != null ? trendingKeywords.size() : 0);

        boolean isVietnamese = (language == Language.VIETNAMESE);
        StringBuilder prompt = new StringBuilder();

        // Stage 1: Task Understanding
        prompt.append(buildStage1_TaskUnderstanding(userPrompt, adType, numberOfVariations, isVietnamese));

        // Stage 2: Audience Analysis
        prompt.append(buildStage2_AudienceAnalysis(persona, targetAudience, isVietnamese));

        // Stage 3: Creative Direction
        prompt.append(buildStage3_CreativeDirection(adStyle, trendingKeywords, isVietnamese));

        // Stage 4: Constraints & Requirements
        prompt.append(buildStage4_Constraints(callToAction, language, isVietnamese));

        // Stage 5: Reasoning Process
        prompt.append(buildStage5_ReasoningProcess(persona, adStyle, isVietnamese));

        // Stage 6: Generation Instruction
        prompt.append(buildStage6_GenerationInstruction(numberOfVariations, language, isVietnamese));

        log.debug("[Phase 3] CoT prompt built successfully (length: {} chars)", prompt.length());
        return prompt.toString();
    }

    /**
     * Stage 1: Task Understanding
     * Clearly define what we're asking the AI to do
     */
    private String buildStage1_TaskUnderstanding(String userPrompt, AdType adType, int numberOfVariations, boolean isVietnamese) {
        String adTypeName = mapAdTypeToDisplayName(adType, isVietnamese);

        if (isVietnamese) {
            return String.format("""
                🎯 NHIỆM VỤ
                Bạn đang tạo chiến dịch quảng cáo Facebook cho:
                %s

                Loại quảng cáo: %s
                Số lượng biến thể cần tạo: %d

                """, userPrompt, adTypeName, numberOfVariations);
        } else {
            return String.format("""
                🎯 TASK
                You are creating a Facebook ad campaign for:
                %s

                Ad Type: %s
                Number of variations to generate: %d

                """, userPrompt, adTypeName, numberOfVariations);
        }
    }

    /**
     * Stage 2: Audience Analysis
     * Provide detailed persona and campaign audience information
     */
    private String buildStage2_AudienceAnalysis(Persona persona, String targetAudience, boolean isVietnamese) {
        StringBuilder stage = new StringBuilder();

        if (isVietnamese) {
            stage.append("👥 ĐỐI TƯỢNG MỤC TIÊU\n\n");

            if (targetAudience != null && !targetAudience.trim().isEmpty()) {
                stage.append("Đối tượng chiến dịch:\n");
                stage.append(targetAudience).append("\n\n");
            }

            if (persona != null) {
                stage.append("Hồ sơ Persona:\n");
                stage.append(persona.toPromptStringVietnamese());
                stage.append("\n");
            }
        } else {
            stage.append("👥 TARGET AUDIENCE\n\n");

            if (targetAudience != null && !targetAudience.trim().isEmpty()) {
                stage.append("Campaign Audience:\n");
                stage.append(targetAudience).append("\n\n");
            }

            if (persona != null) {
                stage.append("Persona Profile:\n");
                stage.append(persona.toPromptString());
                stage.append("\n");
            }
        }

        stage.append("\n");
        return stage.toString();
    }

    /**
     * Stage 3: Creative Direction
     * Define style and trending keywords
     */
    private String buildStage3_CreativeDirection(AdStyle adStyle, List<String> trendingKeywords, boolean isVietnamese) {
        StringBuilder stage = new StringBuilder();

        if (isVietnamese) {
            stage.append("🎨 HƯỚNG SÁNG TẠO\n\n");

            if (adStyle != null) {
                stage.append(adStyle.getStyleInstruction(true)).append("\n\n");
            }

            if (trendingKeywords != null && !trendingKeywords.isEmpty()) {
                stage.append("💡 TỪ KHÓA TRENDING\n");
                stage.append("Cân nhắc tích hợp các từ khóa trending này để tăng khả năng khám phá:\n");
                trendingKeywords.forEach(keyword -> stage.append("- ").append(keyword).append("\n"));
                stage.append("\n");
            }
        } else {
            stage.append("🎨 CREATIVE DIRECTION\n\n");

            if (adStyle != null) {
                stage.append(adStyle.getStyleInstruction(false)).append("\n\n");
            }

            if (trendingKeywords != null && !trendingKeywords.isEmpty()) {
                stage.append("💡 TRENDING INSIGHTS\n");
                stage.append("Consider incorporating these trending keywords to increase discoverability:\n");
                trendingKeywords.forEach(keyword -> stage.append("- ").append(keyword).append("\n"));
                stage.append("\n");
            }
        }

        return stage.toString();
    }

    /**
     * Stage 4: Constraints & Requirements
     * Strict Facebook requirements and language enforcement
     */
    private String buildStage4_Constraints(FacebookCTA callToAction, Language language, boolean isVietnamese) {
        String ctaDisplay = callToAction != null
            ? (isVietnamese ? callToAction.getDisplayNameVietnamese() : callToAction.name())
            : (isVietnamese ? "Không xác định" : "Not specified");

        if (isVietnamese) {
            return String.format("""
                📏 YÊU CẦU FACEBOOK (NGHIÊM NGẶT - BẮT BUỘC TUÂN THỦ)

                ⚠️ GIỚI HẠN KÝ TỰ - TUYỆT ĐỐI KHÔNG ĐƯỢC VƯỢT QUÁ:
                - Tiêu đề (headline): NGHIÊM NGẶT 40 ký tự
                  * Đếm TỪNG ký tự kể cả dấu cách và dấu câu
                  * Nếu vượt quá 40 ký tự sẽ BỊ TỪ CHỐI bởi Facebook
                  * Ví dụ HỢP LỆ (39 chars): "Giảm 50%% - Mua ngay hôm nay!"
                  * Ví dụ KHÔNG HỢP LỆ (42 chars): "Giảm giá lớn 50%% - Đừng bỏ lỡ!"

                - Mô tả (description): NGHIÊM NGẶT 125 ký tự
                - Văn bản chính (primaryText): NGHIÊM NGẶT 1000 ký tự

                Tuân thủ chính sách:
                - Không dùng từ cấm: "miễn phí", "đảm bảo", "kỳ diệu", "click vào đây", "mua ngay", "gây sốc"
                - Không cường điệu, phóng đại
                - Không ngôn ngữ phân biệt đối xử
                - Không so sánh trước/sau nếu không có bằng chứng

                Call-to-Action: %s
                Ngôn ngữ: TIẾNG VIỆT ← QUAN TRỌNG: Output PHẢI 100%% tiếng Việt, KHÔNG được lẫn tiếng Anh

                """, ctaDisplay);
        } else {
            return String.format("""
                📏 FACEBOOK REQUIREMENTS (STRICT - MANDATORY COMPLIANCE)

                ⚠️ CHARACTER LIMITS - ABSOLUTELY MUST NOT EXCEED:
                - Headline: STRICTLY 40 characters
                  * Count EVERY character including spaces and punctuation
                  * Exceeding 40 characters will be REJECTED by Facebook
                  * VALID example (39 chars): "Save 50%% - Shop Today Limited Time"
                  * INVALID example (42 chars): "Big Sale 50%% Off - Don't Miss Out Now!"

                - Description: STRICTLY 125 characters
                - Primary Text: STRICTLY 1000 characters

                Policy Compliance:
                - No prohibited words: "free", "guaranteed", "miracle", "click here", "buy now", "shocking"
                - No exaggerated claims
                - No discriminatory language
                - No before/after comparisons without disclaimers

                Call-to-Action: %s
                Language: ENGLISH ← CRITICAL: Output MUST be 100%% English, NO Vietnamese mixed in

                """, ctaDisplay);
        }
    }

    /**
     * Stage 5: Reasoning Process
     * Guide the AI through step-by-step thinking
     */
    private String buildStage5_ReasoningProcess(Persona persona, AdStyle adStyle, boolean isVietnamese) {
        if (isVietnamese) {
            StringBuilder stage = new StringBuilder("""
                🧠 QUY TRÌNH SUY LUẬN

                Trước khi tạo quảng cáo, hãy suy nghĩ từng bước:

                1. HIỂU PERSONA
                """);

            if (persona != null) {
                stage.append("   - Các pain points chính là gì? ");
                if (persona.getPainPoints() != null && !persona.getPainPoints().isEmpty()) {
                    stage.append(String.join(", ", persona.getPainPoints()));
                }
                stage.append("\n");

                if (persona.getDesiredOutcome() != null && !persona.getDesiredOutcome().isEmpty()) {
                    stage.append("   - Kết quả mong muốn: ").append(persona.getDesiredOutcome()).append("\n");
                }

                if (persona.getTone() != null) {
                    stage.append("   - Giọng điệu phù hợp: ").append(persona.getTone()).append("\n");
                }
            } else {
                stage.append("   - Hiểu rõ đối tượng mục tiêu và nhu cầu của họ\n");
            }

            stage.append("""

                2. PHÂN TÍCH GIÁ TRỊ ĐỀ XUẤT
                   - Sản phẩm/dịch vụ này giải quyết pain points như thế nào?
                   - Lợi ích độc đáo là gì?
                   - Cảm xúc nào cần kích hoạt?

                3. ÁP DỤNG HƯỚNG SÁNG TẠO
                """);

            if (adStyle != null) {
                stage.append("   - Làm thế nào để phù hợp với phong cách ").append(adStyle.name()).append("?\n");
            } else {
                stage.append("   - Phong cách nào phù hợp nhất với đối tượng?\n");
            }

            stage.append("""
                   - Từ khóa trending nào khớp tự nhiên?
                   - Tone/ngôn ngữ nào phù hợp nhất?

                4. ĐẢM BẢO TUÂN THỦ
                   - Có tuân thủ giới hạn ký tự không?
                   - Có tránh từ cấm không?
                   - Có 100% tiếng Việt không?
                   - Call-to-action có rõ ràng không?

                5. TỐI ƯU HÓA HIỆU SUẤT
                   - Tiêu đề có thu hút chú ý không?
                   - Mô tả có tạo tò mò không?
                   - Văn bản chính có xây dựng niềm tin và kêu gọi hành động không?

                """);

            return stage.toString();
        } else {
            StringBuilder stage = new StringBuilder("""
                🧠 CHAIN-OF-THOUGHT REASONING

                Before generating the ad, think step-by-step:

                1. UNDERSTAND THE PERSONA
                """);

            if (persona != null) {
                stage.append("   - What are their main pain points? ");
                if (persona.getPainPoints() != null && !persona.getPainPoints().isEmpty()) {
                    stage.append(String.join(", ", persona.getPainPoints()));
                }
                stage.append("\n");

                if (persona.getDesiredOutcome() != null && !persona.getDesiredOutcome().isEmpty()) {
                    stage.append("   - Desired outcome: ").append(persona.getDesiredOutcome()).append("\n");
                }

                if (persona.getTone() != null) {
                    stage.append("   - Tone that resonates: ").append(persona.getTone()).append("\n");
                }
            } else {
                stage.append("   - Understand target audience and their needs\n");
            }

            stage.append("""

                2. ANALYZE THE VALUE PROPOSITION
                   - How does this product/service solve their pain points?
                   - What unique benefits does it offer?
                   - What emotional triggers should we use?

                3. APPLY CREATIVE DIRECTION
                """);

            if (adStyle != null) {
                stage.append("   - How can we match the ").append(adStyle.name()).append(" style?\n");
            } else {
                stage.append("   - Which style best suits the audience?\n");
            }

            stage.append("""
                   - Which trending keywords fit naturally?
                   - What tone/language best suits the audience?

                4. ENSURE COMPLIANCE
                   - Are character limits respected?
                   - Are prohibited words avoided?
                   - Is the language 100% English?
                   - Is the call-to-action clear?

                5. OPTIMIZE FOR PERFORMANCE
                   - Is the headline attention-grabbing?
                   - Does the description create curiosity?
                   - Does the primary text build trust and drive action?

                """);

            return stage.toString();
        }
    }

    /**
     * Stage 6: Generation Instruction
     * Final instruction with strict format requirements
     */
    private String buildStage6_GenerationInstruction(int numberOfVariations, Language language, boolean isVietnamese) {
        if (isVietnamese) {
            return String.format("""
                ✍️ HƯỚNG DẪN TẠO NỘI DUNG

                Bây giờ hãy tạo %d biến thể quảng cáo khác nhau theo format sau:

                **YÊU CẦU OUTPUT QUAN TRỌNG:**
                1. Ngôn ngữ: PHẢI 100%% tiếng Việt - không ngoại lệ
                2. Format: Chỉ trả về JSON array hợp lệ
                3. Tính độc đáo: Mỗi biến thể phải khác biệt có ý nghĩa
                4. Tuân thủ: Mọi quảng cáo phải đáp ứng tất cả yêu cầu Facebook

                JSON Format:
                [
                  {
                    "headline": "Tiêu đề hấp dẫn ở đây (tối đa 40 ký tự)",
                    "description": "Mô tả cuốn hút ở đây (tối đa 125 ký tự)",
                    "primaryText": "Văn bản chính đầy đủ với giá trị đề xuất rõ ràng và kêu gọi hành động (tối đa 1000 ký tự)"
                  }
                ]

                Tạo ngay bây giờ:
                """, numberOfVariations);
        } else {
            return String.format("""
                ✍️ GENERATION INSTRUCTIONS

                Now generate %d unique ad variations following this format:

                **CRITICAL OUTPUT REQUIREMENTS:**
                1. Language: MUST be 100%% English - no exceptions
                2. Format: Return ONLY a valid JSON array
                3. Uniqueness: Each variation must be meaningfully different
                4. Compliance: Every ad must pass all Facebook requirements

                JSON Format:
                [
                  {
                    "headline": "Your compelling headline here (max 40 chars)",
                    "description": "Your engaging description here (max 125 chars)",
                    "primaryText": "Your full primary text here with clear value proposition and call-to-action (max 1000 chars)"
                  }
                ]

                Generate now:
                """, numberOfVariations);
        }
    }

    /**
     * Helper: Map AdType enum to display name
     */
    private String mapAdTypeToDisplayName(AdType adType, boolean isVietnamese) {
        if (adType == null) {
            return isVietnamese ? "Không xác định" : "Not specified";
        }

        if (isVietnamese) {
            return switch (adType) {
                case PAGE_POST_AD -> "Quảng cáo bài viết trang";
                case WEBSITE_CONVERSION_AD -> "Quảng cáo chuyển đổi website";
                case LEAD_FORM_AD -> "Quảng cáo biểu mẫu khách hàng tiềm năng";
            };
        } else {
            return switch (adType) {
                case PAGE_POST_AD -> "Page Post Ad";
                case WEBSITE_CONVERSION_AD -> "Website Conversion Ad";
                case LEAD_FORM_AD -> "Lead Form Ad";
            };
        }
    }
}
