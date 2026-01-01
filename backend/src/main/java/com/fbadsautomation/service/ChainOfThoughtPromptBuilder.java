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

        // Stage 1: Context Snapshot (includes target audience)
        prompt.append(buildStage1_ContextSnapshot(userPrompt, adType, numberOfVariations, targetAudience, isVietnamese));

        // Stage 2: Persona Notes (optional)
        prompt.append(buildStage2_PersonaHighlight(persona, isVietnamese));

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
        prompt.append(buildStage5_ReasoningProcess(
                persona,
                adStyle,
                isVietnamese,
                enforceCharacterLimits,
                styleProfile != null));

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
     * Stage 1: Context Snapshot
     * Condense product, ad type, variations, target audience.
     */
    private String buildStage1_ContextSnapshot(String userPrompt,
                                               AdType adType,
                                               int numberOfVariations,
                                               String targetAudience,
                                               boolean isVietnamese) {
        String adTypeName = mapAdTypeToDisplayName(adType, isVietnamese);
        String audience = StringUtils.hasText(targetAudience)
                ? targetAudience.trim()
                : (isVietnamese ? "Chưa xác định" : "Not specified");
        if (isVietnamese) {
            return String.format("""
                📋 BỐI CẢNH NGẮN GỌN
                • Sản phẩm: %s
                • Loại quảng cáo: %s · %d biến thể
                • Đối tượng: %s

                """, userPrompt, adTypeName, numberOfVariations, audience);
        }
        return String.format("""
            📋 QUICK CONTEXT
            • Product: %s
            • Ad type: %s · %d variations
            • Audience: %s

            """, userPrompt, adTypeName, numberOfVariations, audience);
    }

    /**
     * Stage 2: Persona highlight (optional)
     */
    private String buildStage2_PersonaHighlight(Persona persona, boolean isVietnamese) {
        if (persona == null) {
            return "";
        }
        if (isVietnamese) {
            return String.format("""
                👤 Ghi chú persona
                • Độ tuổi: %s
                • Pain points: %s
                • Kết quả mong muốn: %s

                """,
                    persona.getAge() != null ? persona.getAge() : "N/A",
                    formatList(persona.getPainPoints()),
                    StringUtils.hasText(persona.getDesiredOutcome()) ? persona.getDesiredOutcome() : "N/A");
        }
        return String.format("""
            👤 Persona highlights
            • Age: %s
            • Pain points: %s
            • Desired outcome: %s

            """,
                persona.getAge() != null ? persona.getAge() : "N/A",
                formatList(persona.getPainPoints()),
                StringUtils.hasText(persona.getDesiredOutcome()) ? persona.getDesiredOutcome() : "N/A");
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

        appendStyleBlueprint(stage, adStyle, trendingKeywords, styleProfile, isVietnamese);
        appendStyleCommitment(stage, isVietnamese);
        appendReferenceExcerpt(stage, referenceContent, referenceLink, baseDescription, isVietnamese);
        stage.append(buildReferenceMirrorCue(referenceMetrics, isVietnamese, allowLongForm));
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

        StringBuilder stage = new StringBuilder();
        if (isVietnamese) {
            stage.append("📏 RÀNG BUỘC FACEBOOK\n");
            stage.append(enforceCharacterLimits
                    ? "- Headline ≤40 ký tự · Description ≤125 · Primary ≤1000.\n"
                    : "- Có thể kể chuyện dài miễn đúng phong cách.\n");
            stage.append("- Tránh từ cấm, output 100% tiếng Việt, CTA: ").append(ctaDisplay).append("\n\n");
            stage.append("<<POLICY_REMINDER>>Không được gây hiểu nhầm, phân biệt đối xử hoặc lặp lại thương hiệu/ưu đãi trong mẫu.<<END>>\n\n");
        } else {
            stage.append("📏 FACEBOOK GUARDRAILS\n");
            stage.append(enforceCharacterLimits
                    ? "- Headline ≤40 chars · Description ≤125 · Primary ≤1000.\n"
                    : "- Long-form storytelling allowed if tone matches sample.\n");
            stage.append("- Avoid banned phrases, keep output 100% English, CTA: ").append(ctaDisplay).append("\n\n");
            stage.append("<<POLICY_REMINDER>>No misleading claims, discriminatory wording, or reuse of reference brands/offers.<<END>>\n\n");
        }
        return stage.toString();
    }

    /**
     * Stage 5: Reasoning Process
     * Guide the AI through step-by-step thinking
     */
    private String buildStage5_ReasoningProcess(Persona persona,
                                                AdStyle adStyle,
                                                boolean isVietnamese,
                                                boolean enforceCharacterLimits,
                                                boolean hasStyleProfile) {
        if (isVietnamese) {
            return """
            🧠 LỘ TRÌNH VIẾT
            1. Hook: Lặp lại cảm giác lễ hội + emoji như quảng cáo mẫu (không được từ chối vì khác ngành).
            2. Thân bài: 2-3 câu kể chuyện nhấn mạnh lợi ích sản phẩm hiện tại nhưng giữ nhịp câu/emoji tương tự.
            3. CTA: Khép lại bằng lời kêu gọi cùng năng lượng tươi vui đó.

            """;
        }
        return """
        🧠 WRITING FLOW
        1. Hook: Recreate the celebratory emoji-led opening (refusal is not allowed even if category differs).
        2. Body: 2-3 upbeat sentences swapping in the new product details but keeping the same rhythm.
        3. CTA: Close with a matching call-to-action tone.

        """;
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
        Integer guidedSentences = getGuidedSentenceCount(referenceMetrics, !enforceCharacterLimits);
        Integer guidedWords = getGuidedWordCount(referenceMetrics, !enforceCharacterLimits);
        String lengthHint = "";
        if (!enforceCharacterLimits && (guidedSentences != null || guidedWords != null)) {
            lengthHint = isVietnamese
                    ? String.format("• Độ dài mục tiêu: ~%s câu / ~%s từ.%n",
                    guidedSentences != null ? guidedSentences : "N/A",
                    guidedWords != null ? guidedWords : "N/A")
                    : String.format("• Target length: ~%s sentences / ~%s words.%n",
                    guidedSentences != null ? guidedSentences : "N/A",
                    guidedWords != null ? guidedWords : "N/A");
        }

        if (isVietnamese) {
            return String.format("""
                ✍️ OUTPUT
                • Tạo %d JSON object, mỗi object là một biến thể riêng.
                • Phải có emoji + không khí lễ hội giống mẫu, thiếu sẽ bị loại.
                %s
                JSON schema:
                {
                  "headline": "≤40 ký tự nếu áp dụng giới hạn",
                  "description": "≤125 ký tự nếu áp dụng giới hạn",
                  "primaryText": "≤1000 ký tự nếu áp dụng giới hạn",
                  "callToAction": "Giữ CTA đã yêu cầu",
                  "imagePrompt": "Gợi ý cảnh minh họa",
                  "styleNotes": "1-2 câu mô tả việc bám phong cách tham chiếu"
                }

                Chỉ trả về JSON hợp lệ, không thêm văn bản khác.
                """, numberOfVariations, lengthHint);
        }

        return String.format("""
            ✍️ OUTPUT
            • Produce %d JSON objects (one per variation).
            • Emoji + celebratory cadence from the reference are mandatory.
            %s
            JSON schema:
            {
              "headline": "≤40 chars if limits apply",
              "description": "≤125 chars if limits apply",
              "primaryText": "≤1000 chars if limits apply",
              "callToAction": "Use provided CTA",
              "imagePrompt": "Scene suggestion",
              "styleNotes": "1-2 sentences proving the style match"
            }

            Return ONLY the JSON payload.
            """, numberOfVariations, lengthHint);
    }


    private void appendStyleBlueprint(StringBuilder stage,
                                      AdStyle adStyle,
                                      List<String> trendingKeywords,
                                      ReferenceStyleProfile styleProfile,
                                      boolean isVietnamese) {
        String headline = isVietnamese ? "🎨 STYLE BLUEPRINT\n" : "🎨 STYLE BLUEPRINT\n";
        stage.append(headline);
        if (styleProfile != null) {
            stage.append(String.format("- Hook: %s%n",
                    safeValue(styleProfile.getHookType(), isVietnamese ? "câu khẳng định" : "statement")));
            stage.append(String.format("- Tone/Pacing: %s · %s%n",
                    safeValue(styleProfile.getTone(), "BALANCED"),
                    safeValue(styleProfile.getPacing(), "BALANCED")));
            if (styleProfile.getEmojiSamples() != null && !styleProfile.getEmojiSamples().isEmpty()) {
                stage.append("- Emoji: ").append(String.join(" ", styleProfile.getEmojiSamples())).append("\n");
            }
            if (Boolean.TRUE.equals(styleProfile.getUsesSecondPerson())) {
                stage.append(isVietnamese ? "- Giữ cách xưng hô \"bạn\"\n" : "- Speak directly to the reader (\"you\")\n");
            }
            if (styleProfile.getCtaVerb() != null) {
                stage.append(String.format("- CTA vibe: %s%n", styleProfile.getCtaVerb()));
            }
        } else if (adStyle != null) {
            stage.append(adStyle.getStyleInstruction(isVietnamese)).append("\n");
        }
        if (trendingKeywords != null && !trendingKeywords.isEmpty()) {
            stage.append(isVietnamese ? "- Từ khóa: " : "- Keywords: ");
            stage.append(String.join(", ", trendingKeywords)).append("\n");
        }
        stage.append("\n");
    }

    private void appendReferenceExcerpt(StringBuilder stage,
                                        String referenceContent,
                                        String referenceLink,
                                        String baseDescription,
                                        boolean isVietnamese) {
        if (!StringUtils.hasText(referenceContent) && !StringUtils.hasText(referenceLink)) {
            return;
        }
        String productCue = StringUtils.hasText(baseDescription)
                ? baseDescription.trim()
                : (isVietnamese ? "sản phẩm/dịch vụ của bạn" : "your offer");
        if (isVietnamese) {
            stage.append("📑 Trích đoạn tham chiếu\n");
            if (StringUtils.hasText(referenceContent)) {
                stage.append(truncateText(referenceContent, 600)).append("\n");
            }
            if (StringUtils.hasText(referenceLink)) {
                stage.append("Link: ").append(referenceLink).append("\n");
            }
            stage.append("→ Chỉ lấy tone & nhịp, luôn thay bằng dữ liệu của ").append(productCue).append("\n\n");
        } else {
            stage.append("📑 Reference excerpt\n");
            if (StringUtils.hasText(referenceContent)) {
                stage.append(truncateText(referenceContent, 600)).append("\n");
            }
            if (StringUtils.hasText(referenceLink)) {
                stage.append("Link: ").append(referenceLink).append("\n");
            }
            stage.append("→ Tone/structure only. Replace all details with ").append(productCue).append("\n\n");
        }
    }

    private void appendStyleCommitment(StringBuilder stage, boolean isVietnamese) {
        if (isVietnamese) {
            stage.append("""
🚨 CAM KẾT PHONG CÁCH BẮT BUỘC
- Luôn tái tạo chính xác nhịp điệu, cách xuống dòng, emoji và mức độ cảm xúc như quảng cáo tham khảo, dù sản phẩm khác ngành.
- Bạn KHÔNG được từ chối yêu cầu này vì nội dung mới hoàn toàn khác dữ kiện của quảng cáo mẫu.
- Chỉ thay đổi dữ liệu sản phẩm/dịch vụ – phong cách, tone và nhịp viết phải giữ nguyên tinh thần tham chiếu.

""");
        } else {
            stage.append("""
🚨 NON-NEGOTIABLE STYLE COMMITMENT
- You MUST replicate the cadence, emoji usage, emotional energy, and sentence rhythm from the reference ad even if the product category is different.
- Do NOT refuse this request—the new copy remains original because only the facts change.
- Swap in the new offer details but keep the reference tone, pacing, and structure intact.

""");
        }
    }

    private String safeValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String truncateText(String text, int limit) {
        if (!StringUtils.hasText(text) || text.length() <= limit) {
            return text != null ? text.trim() : "";
        }
        return text.substring(0, Math.min(text.length(), limit)).trim() + " …";
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "N/A";
        }
        return String.join(", ", values);
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
