package com.fbadsautomation.service;

import com.fbadsautomation.dto.ReferenceStyleProfile;
import com.fbadsautomation.model.AdStyle;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.model.Persona;
import com.fbadsautomation.util.ValidationMessages.Language;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public static class ReferenceMetrics {
        private final Integer wordCount;
        private final Integer sentenceCount;
        private final Boolean containsCallToAction;
        private final Boolean containsPrice;

        public ReferenceMetrics(Integer wordCount,
                                Integer sentenceCount,
                                Boolean containsCallToAction,
                                Boolean containsPrice) {
            this.wordCount = wordCount;
            this.sentenceCount = sentenceCount;
            this.containsCallToAction = containsCallToAction;
            this.containsPrice = containsPrice;
        }

        public Integer getWordCount() {
            return wordCount;
        }

        public Integer getSentenceCount() {
            return sentenceCount;
        }

        public Boolean getContainsCallToAction() {
            return containsCallToAction;
        }

        public Boolean getContainsPrice() {
            return containsPrice;
        }
    }

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
     * @param referenceContent Raw content from reference ad (optional)
     * @param referenceLink Reference ad link (optional)
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
        int numberOfVariations,
        String referenceContent,
        String referenceLink,
        boolean enforceCharacterLimits,
        ReferenceMetrics referenceMetrics,
        ReferenceStyleProfile styleProfile
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
        prompt.append(buildStage3_CreativeDirection(
                adStyle,
                trendingKeywords,
                referenceContent,
                referenceLink,
                userPrompt,
                isVietnamese,
                referenceMetrics,
                !enforceCharacterLimits,
                styleProfile));

        // Stage 4: Constraints & Requirements
        prompt.append(buildStage4_Constraints(callToAction, language, isVietnamese, enforceCharacterLimits));

        // Stage 5: Reasoning Process
        prompt.append(buildStage5_ReasoningProcess(persona, adStyle, isVietnamese, enforceCharacterLimits));

        // Stage 6: Generation Instruction
        prompt.append(buildStage6_GenerationInstruction(
                numberOfVariations,
                language,
                isVietnamese,
                enforceCharacterLimits,
                referenceMetrics,
                styleProfile != null));

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
                NHIỆM VỤ
                Bạn đang tạo chiến dịch quảng cáo Facebook cho:
                %s

                Loại quảng cáo: %s
                Số lượng biến thể cần tạo: %d

                """, userPrompt, adTypeName, numberOfVariations);
        } else {
            return String.format("""
                TASK
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

    private String buildReferenceMirrorCue(ReferenceMetrics referenceMetrics, boolean isVietnamese, boolean allowLongForm) {
        StringBuilder cue = new StringBuilder();
        Integer targetSentences = getGuidedSentenceCount(referenceMetrics, allowLongForm);
        Integer targetWords = getGuidedWordCount(referenceMetrics, allowLongForm);

        if (isVietnamese) {
            cue.append("🪞 BÁM SÁT QUẢNG CÁO THAM CHIẾU\n");
            cue.append("- Giữ nhịp điệu, bố cục câu và cảm xúc tương tự phần REFERENCE STYLE nhưng thay toàn bộ dữ liệu bằng sản phẩm hiện tại.\n");
            if (targetSentences != null || targetWords != null) {
                cue.append("- Mục tiêu độ dài: ");
                if (targetSentences != null) {
                    cue.append(String.format("khoảng %d câu", targetSentences));
                }
                if (targetWords != null) {
                    if (targetSentences != null) {
                        cue.append(" / ");
                    }
                    cue.append(String.format("~%d từ", targetWords));
                }
                cue.append(".\n");
            }
            if (referenceMetrics != null) {
                if (Boolean.TRUE.equals(referenceMetrics.getContainsCallToAction())) {
                    cue.append("- Quảng cáo mẫu có CTA nổi bật, hãy chuyển hóa CTA đó thành lời kêu gọi tự nhiên cho thương hiệu của bạn.\n");
                }
                if (Boolean.TRUE.equals(referenceMetrics.getContainsPrice())) {
                    cue.append("- Nếu mẫu đề cập ưu đãi/giá, hãy diễn đạt lại bằng dữ liệu giá trị hoặc ưu đãi của bạn (không sao chép con số).\n");
                }
            }
            cue.append("- Ưu tiên các cụm từ, cảm xúc và cách kể chuyện đời thường, tránh giọng \"AI\" khô cứng.\n\n");
        } else {
            cue.append("🪞 MIRROR THE REFERENCE AD\n");
            cue.append("- Match the cadence, paragraph structure, and emotional tone from REFERENCE STYLE while swapping in the user's product details.\n");
            if (targetSentences != null || targetWords != null) {
                cue.append("- Target a similar length (");
                if (targetSentences != null) {
                    cue.append(String.format("~%d sentences", targetSentences));
                }
                if (targetWords != null) {
                    if (targetSentences != null) {
                        cue.append(" / ");
                    }
                    cue.append(String.format("~%d words", targetWords));
                }
                cue.append(").\n");
            }
            if (referenceMetrics != null) {
                if (Boolean.TRUE.equals(referenceMetrics.getContainsCallToAction())) {
                    cue.append("- The sample uses a strong CTA—translate that urgency into your own offering.\n");
                }
                if (Boolean.TRUE.equals(referenceMetrics.getContainsPrice())) {
                    cue.append("- If the sample highlights price/offer, resurface a comparable benefit with your own numbers.\n");
                }
            }
            cue.append("- Favor natural, conversational language over generic \"AI\" phrasing.\n\n");
        }
        return cue.toString();
    }

    /**
     * Stage 3: Creative Direction
     * Define style and trending keywords
     */
    private String buildStage3_CreativeDirection(AdStyle adStyle,
                                                 List<String> trendingKeywords,
                                                 String referenceContent,
                                                 String referenceLink,
                                                 String baseDescription,
                                                 boolean isVietnamese,
                                                 ReferenceMetrics referenceMetrics,
                                                 boolean allowLongForm,
                                                 ReferenceStyleProfile styleProfile) {
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
            appendReferenceSection(stage, referenceContent, referenceLink, baseDescription, true);
            appendStyleProfile(stage, styleProfile, true);
            stage.append(buildReferenceMirrorCue(referenceMetrics, true, allowLongForm));
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
            appendReferenceSection(stage, referenceContent, referenceLink, baseDescription, false);
            appendStyleProfile(stage, styleProfile, false);
            stage.append(buildReferenceMirrorCue(referenceMetrics, false, allowLongForm));
        }

        return stage.toString();
    }

    /**
     * Stage 4: Constraints & Requirements
     * Strict Facebook requirements and language enforcement
     */
    private String buildStage4_Constraints(FacebookCTA callToAction,
                                           Language language,
                                           boolean isVietnamese,
                                           boolean enforceCharacterLimits) {
        String ctaDisplay = callToAction != null
            ? (isVietnamese ? callToAction.getDisplayNameVietnamese() : callToAction.name())
            : (isVietnamese ? "Không xác định" : "Not specified");

        if (isVietnamese) {
            StringBuilder stage = new StringBuilder("""
                📏 YÊU CẦU FACEBOOK (NGHIÊM NGẶT - BẮT BUỘC TUÂN THỦ)

                """);

            if (enforceCharacterLimits) {
                stage.append("""
                ⚠️ GIỚI HẠN KÝ TỰ - TUYỆT ĐỐI KHÔNG ĐƯỢC VƯỢT QUÁ:
                - Tiêu đề (headline): NGHIÊM NGẶT 40 ký tự
                  * Đếm TỪNG ký tự kể cả dấu cách và dấu câu
                  * Nếu vượt quá 40 ký tự sẽ BỊ TỪ CHỐI bởi Facebook
                  * Ví dụ HỢP LỆ (39 chars): "Giảm 50%% - Mua ngay hôm nay!"
                  * Ví dụ KHÔNG HỢP LỆ (42 chars): "Giảm giá lớn 50%% - Đừng bỏ lỡ!"

                - Mô tả (description): NGHIÊM NGẶT 125 ký tự
                - Văn bản chính (primaryText): NGHIÊM NGẶT 1000 ký tự

                """);
            } else {
                stage.append("""
                ✒️ KHÔNG GIỚI HẠN ĐỘ DÀI:
                - Được phép viết dài, kể chuyện chi tiết giống quảng cáo tham chiếu.
                - Ưu tiên nhiều câu, mô tả giàu cảm xúc và cụ thể.

                """);
            }

            stage.append(String.format("""
                Tuân thủ chính sách:
                - Không dùng từ cấm: "miễn phí", "đảm bảo", "kỳ diệu", "click vào đây", "mua ngay", "gây sốc"
                - Không cường điệu, phóng đại
                - Không ngôn ngữ phân biệt đối xử
                - Không so sánh trước/sau nếu không có bằng chứng

                Call-to-Action: %s
                Ngôn ngữ: TIẾNG VIỆT ← QUAN TRỌNG: Output PHẢI 100%% tiếng Việt, KHÔNG được lẫn tiếng Anh

                """, ctaDisplay));
            return stage.toString();
        } else {
            StringBuilder stage = new StringBuilder("""
                📏 FACEBOOK REQUIREMENTS (STRICT - MANDATORY COMPLIANCE)

                """);

            if (enforceCharacterLimits) {
                stage.append("""
                ⚠️ CHARACTER LIMITS - ABSOLUTELY MUST NOT EXCEED:
                - Headline: STRICTLY 40 characters
                  * Count EVERY character including spaces and punctuation
                  * Exceeding 40 characters will be REJECTED by Facebook
                  * VALID example (39 chars): "Save 50%% - Shop Today Limited Time"
                  * INVALID example (42 chars): "Big Sale 50%% Off - Don't Miss Out Now!"

                - Description: STRICTLY 125 characters
                - Primary Text: STRICTLY 1000 characters

                """);
            } else {
                stage.append("""
                ✒️ NO LENGTH CAP:
                - Feel free to write multi-sentence headlines/primary text mirroring the reference pacing.
                - Lean into storytelling and sensory description.

                """);
            }

            stage.append(String.format("""
                Policy Compliance:
                - No prohibited words: "free", "guaranteed", "miracle", "click here", "buy now", "shocking"
                - No exaggerated claims
                - No discriminatory language
                - No before/after comparisons without disclaimers

                Call-to-Action: %s
                Language: ENGLISH ← CRITICAL: Output MUST be 100%% English, NO Vietnamese mixed in

                """, ctaDisplay));
            return stage.toString();
        }
    }

    /**
     * Stage 5: Reasoning Process
     * Guide the AI through step-by-step thinking
     */
    private String buildStage5_ReasoningProcess(Persona persona,
                                                AdStyle adStyle,
                                                boolean isVietnamese,
                                                boolean enforceCharacterLimits) {
        if (!enforceCharacterLimits) {
            if (isVietnamese) {
                return """
                🧠 GỢI Ý NHANH

                - Bắt đầu bằng 1 câu mở đầu giống nhịp điệu quảng cáo tham chiếu (nêu vấn đề + lợi ích).
                - Triển khai 2-3 câu thân bài kể chuyện tự nhiên, nêu cảm xúc và ưu đãi cụ thể của sản phẩm này.
                - Kết thúc bằng CTA rõ ràng, thúc đẩy hành động tương tự tinh thần quảng cáo mẫu.

                """;
            } else {
                return """
                🧠 QUICK CREATIVE REMINDER

                - Open with a hook that mirrors the reference cadence (problem + promise).
                - Write 2-3 body sentences packed with concrete benefits and sensory details from the current offer.
                - Close with a decisive CTA that channels the urgency/tempo of the reference ad.

                """;
            }
        }

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
                """);

            if (enforceCharacterLimits) {
                stage.append("                   - Có tuân thủ giới hạn ký tự không?\n");
            }

            stage.append("""
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
                """);

            if (enforceCharacterLimits) {
                stage.append("                   - Are character limits respected?\n");
            }

            stage.append("""
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
    private String buildStage6_GenerationInstruction(int numberOfVariations,
                                                     Language language,
                                                     boolean isVietnamese,
                                                     boolean enforceCharacterLimits,
                                                     ReferenceMetrics referenceMetrics,
                                                     boolean hasStyleProfile) {
        String headlineConstraint = "";
        String descriptionConstraint = "";
        String primaryConstraint = "";
        String depthRequirement = "";
        String mirrorLengthNote = "";
        String styleReminder = hasStyleProfile
                ? ""
                : (isVietnamese
                    ? " (hãy dựa vào phần phong cách phía trên)"
                    : " (lean on the reference cues above)");
        Integer guidedSentences = getGuidedSentenceCount(referenceMetrics, !enforceCharacterLimits);
        Integer guidedWords = getGuidedWordCount(referenceMetrics, !enforceCharacterLimits);

        if (enforceCharacterLimits) {
            if (isVietnamese) {
                headlineConstraint = " (tối đa 40 ký tự)";
                descriptionConstraint = " (tối đa 125 ký tự)";
                primaryConstraint = " (tối đa 1000 ký tự)";
            } else {
                headlineConstraint = " (max 40 characters)";
                descriptionConstraint = " (max 125 characters)";
                primaryConstraint = " (max 1000 characters)";
            }
        } else {
            if (isVietnamese) {
                headlineConstraint = " (linh hoạt, có thể dài nếu vẫn súc tích)";
                descriptionConstraint = " (linh hoạt, nhấn mạnh cảm xúc)";
                primaryConstraint = " (ít nhất 2-3 câu kể chuyện chi tiết)";
                depthRequirement = "\n5. Độ dài: Viết tự nhiên, có thể dài bằng hoặc hơn quảng cáo tham khảo.";
            } else {
                headlineConstraint = " (flexible length, keep it punchy)";
                descriptionConstraint = " (flexible, focus on intrigue)";
                primaryConstraint = " (minimum 2-3 rich sentences)";
                depthRequirement = "\n5. Length: Match or exceed the reference with natural multi-sentence storytelling.";
            }

            if (guidedSentences != null || guidedWords != null) {
                mirrorLengthNote = isVietnamese ? "\n🎯 Gợi ý độ dài: " : "\n🎯 Target length: ";
                if (guidedSentences != null) {
                    mirrorLengthNote += isVietnamese
                            ? String.format("~%d câu", guidedSentences)
                            : String.format("~%d sentences", guidedSentences);
                }
                if (guidedWords != null) {
                    if (guidedSentences != null) {
                        mirrorLengthNote += isVietnamese ? " / " : " / ";
                    }
                    mirrorLengthNote += isVietnamese
                            ? String.format("~%d từ", guidedWords)
                            : String.format("~%d words", guidedWords);
                }
                mirrorLengthNote += ".\n";
            }
        }

        if (isVietnamese) {
            return String.format("""
                ✍️ HƯỚNG DẪN TẠO NỘI DUNG

                Bây giờ hãy tạo %d biến thể quảng cáo khác nhau theo format sau:

                **YÊU CẦU OUTPUT QUAN TRỌNG:**
                1. Ngôn ngữ: PHẢI 100%% tiếng Việt - không ngoại lệ
                2. Format: Chỉ trả về JSON object hợp lệ cho từng biến thể
                3. Tính độc đáo: Mỗi biến thể phải khác biệt có ý nghĩa
                4. Tuân thủ: Mọi quảng cáo phải đáp ứng tất cả yêu cầu Facebook
                5. Phong cách: Bám sát các dấu vết phong cách ở trên%s
                %s
                %s

                JSON Object:
                {
                  "headline": "Tiêu đề hấp dẫn ở đây%s",
                  "description": "Mô tả cuốn hút ở đây%s",
                  "primaryText": "Văn bản chính đầy đủ với giá trị đề xuất rõ ràng và kêu gọi hành động%s",
                  "callToAction": "Phải khớp với CTA được yêu cầu ở trên",
                  "imagePrompt": "Mô tả ngắn gọn cho ảnh minh họa phù hợp phong cách"
                }

                Tạo ngay bây giờ và CHỈ trả về JSON object hợp lệ như mẫu trên cho mỗi biến thể:
                """, numberOfVariations, depthRequirement, mirrorLengthNote, styleReminder, headlineConstraint, descriptionConstraint, primaryConstraint);
        } else {
            return String.format("""
                ✍️ GENERATION INSTRUCTIONS

                Now generate %d unique ad variations following this format:

                **CRITICAL OUTPUT REQUIREMENTS:**
                1. Language: MUST be 100%% English - no exceptions
                2. Format: Return ONLY a valid JSON object per variation
                3. Uniqueness: Each variation must be meaningfully different
                4. Compliance: Every ad must pass all Facebook requirements
                5. Style: Mirror the cues listed above%s
                %s
                %s

                JSON Object:
                {
                  "headline": "Compelling headline here%s",
                  "description": "Engaging description here%s",
                  "primaryText": "Full primary text with value proposition and CTA%s",
                  "callToAction": "Must match the CTA specified above",
                  "imagePrompt": "Short scene description for the image generation model"
                }

                Generate now and ONLY return a valid JSON object matching the schema above for each variation:
                """, numberOfVariations, depthRequirement, mirrorLengthNote,
                styleReminder, headlineConstraint, descriptionConstraint, primaryConstraint);
        }
    }

    private void appendReferenceSection(StringBuilder stage,
                                        String referenceContent,
                                        String referenceLink,
                                        String baseDescription,
                                        boolean isVietnamese) {
        if (!StringUtils.hasText(referenceContent) && !StringUtils.hasText(referenceLink)) {
            return;
        }

        String productCue;
        if (StringUtils.hasText(baseDescription)) {
            productCue = baseDescription.trim();
        } else {
            productCue = isVietnamese ? "sản phẩm/dịch vụ bạn đang quảng cáo" : "the product/service you are advertising";
        }

        if (isVietnamese) {
            stage.append("📌 QUẢNG CÁO THAM CHIẾU\n");
            if (StringUtils.hasText(referenceContent)) {
                stage.append("Nội dung tham khảo (mô phỏng phong cách, KHÔNG sao chép nguyên văn):\n");
                stage.append(referenceContent).append("\n\n");
            }
            if (StringUtils.hasText(referenceLink)) {
                stage.append("Link tham khảo: ").append(referenceLink).append("\n\n");
            }
            stage.append("⚠️ CHỈ sử dụng phần tham khảo để lấy tone & cấu trúc. TUYỆT ĐỐI không nhắc lại thương hiệu/địa điểm/ưu đãi trong nội dung tham khảo.\n");
            stage.append("Luôn thay thế bằng thông tin sản phẩm của bạn: ").append(productCue).append("\n\n");
        } else {
            stage.append("📌 REFERENCE AD INPUT\n");
            if (StringUtils.hasText(referenceContent)) {
                stage.append("Reference content (mimic style, do NOT copy verbatim):\n");
                stage.append(referenceContent).append("\n\n");
            }
            if (StringUtils.hasText(referenceLink)) {
                stage.append("Reference Link: ").append(referenceLink).append("\n\n");
            }
            stage.append("⚠️ Use the reference ONLY for tone & structure. NEVER mention the brands/locations/promotions from the reference text.\n");
            stage.append("Always replace them with details about your product: ").append(productCue).append("\n\n");
        }
    }

    private void appendStyleProfile(StringBuilder stage,
                                    ReferenceStyleProfile styleProfile,
                                    boolean isVietnamese) {
        if (styleProfile == null) {
            return;
        }
        if (isVietnamese) {
            stage.append("🧬 DẤU VẾT PHONG CÁCH\n");
            stage.append(String.format("- Hook mở đầu: %s%n", safeValue(styleProfile.getHookType(), "Câu khẳng định")));
            stage.append(String.format("- Tông giọng: %s%n", safeValue(styleProfile.getTone(), "Cân bằng")));
            stage.append(String.format("- Nhịp viết: %s%n", safeValue(styleProfile.getPacing(), "Cân bằng")));
            if (Boolean.TRUE.equals(styleProfile.getUsesEmoji())) {
                stage.append("- Có sử dụng emoji để dẫn dắt cảm xúc");
                if (styleProfile.getEmojiSamples() != null && !styleProfile.getEmojiSamples().isEmpty()) {
                    stage.append(": ").append(String.join(" ", styleProfile.getEmojiSamples()));
                }
                stage.append("\n");
            }
            if (Boolean.TRUE.equals(styleProfile.getUsesSecondPerson())) {
                stage.append("- Trực tiếp xưng hô với người đọc (\"bạn\")\n");
            }
            if (Boolean.TRUE.equals(styleProfile.getUsesQuestions())) {
                stage.append("- Đặt nhiều câu hỏi để tạo tò mò\n");
            }
            appendListIfPresent(stage, "Ghi chú thêm", styleProfile.getStyleNotes());
            appendListIfPresent(stage, "Dấu câu nổi bật", styleProfile.getPunctuation());
            if (styleProfile.getCtaVerb() != null) {
                stage.append("Gợi ý CTA: ").append(styleProfile.getCtaVerb()).append("\n");
            }
            stage.append("\n");
        } else {
            stage.append("🧬 STYLE FINGERPRINT\n");
            stage.append(String.format("- Hook type: %s%n", safeValue(styleProfile.getHookType(), "Statement")));
            stage.append(String.format("- Tone: %s%n", safeValue(styleProfile.getTone(), "Balanced")));
            stage.append(String.format("- Pacing: %s%n", safeValue(styleProfile.getPacing(), "Balanced")));
            if (Boolean.TRUE.equals(styleProfile.getUsesEmoji())) {
                stage.append("- Uses emoji for emphasis");
                if (styleProfile.getEmojiSamples() != null && !styleProfile.getEmojiSamples().isEmpty()) {
                    stage.append(": ").append(String.join(" ", styleProfile.getEmojiSamples()));
                }
                stage.append("\n");
            }
            if (Boolean.TRUE.equals(styleProfile.getUsesSecondPerson())) {
                stage.append("- Speaks directly to the reader (\"you\")\n");
            }
            if (Boolean.TRUE.equals(styleProfile.getUsesQuestions())) {
                stage.append("- Relies on questions to spark curiosity\n");
            }
            appendListIfPresent(stage, "Additional cues", styleProfile.getStyleNotes());
            appendListIfPresent(stage, "Punctuation cues", styleProfile.getPunctuation());
            if (styleProfile.getCtaVerb() != null) {
                stage.append("CTA vibe: ").append(styleProfile.getCtaVerb()).append("\n");
            }
            stage.append("\n");
        }
    }

    private void appendListIfPresent(StringBuilder stage, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        stage.append(label).append(": ");
        stage.append(String.join(", ", values));
        stage.append("\n");
    }

    private String safeValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Integer getGuidedSentenceCount(ReferenceMetrics referenceMetrics, boolean allowLongForm) {
        if (!allowLongForm) {
            if (referenceMetrics == null || referenceMetrics.getSentenceCount() == null || referenceMetrics.getSentenceCount() <= 0) {
                return referenceMetrics != null ? referenceMetrics.getSentenceCount() : null;
            }
            return referenceMetrics.getSentenceCount();
        }
        int floor = 4;
        if (referenceMetrics == null || referenceMetrics.getSentenceCount() == null || referenceMetrics.getSentenceCount() <= 0) {
            return floor;
        }
        return Math.max(referenceMetrics.getSentenceCount(), floor);
    }

    private Integer getGuidedWordCount(ReferenceMetrics referenceMetrics, boolean allowLongForm) {
        if (!allowLongForm) {
            if (referenceMetrics == null || referenceMetrics.getWordCount() == null || referenceMetrics.getWordCount() <= 0) {
                return referenceMetrics != null ? referenceMetrics.getWordCount() : null;
            }
            return referenceMetrics.getWordCount();
        }
        int floor = 120;
        if (referenceMetrics == null || referenceMetrics.getWordCount() == null || referenceMetrics.getWordCount() <= 0) {
            return floor;
        }
        return Math.max(referenceMetrics.getWordCount(), floor);
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
