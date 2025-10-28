package com.fbadsautomation.service;

import com.fbadsautomation.model.AdPersona;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.util.ValidationMessages.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Multi-stage prompt builder for natural, persona-driven ad content generation.
 *
 * Architecture:
 * Stage 1: Extract product info from user prompt (lightweight analysis)
 * Stage 2: Enrich with persona, context, and examples
 * Stage 3: Generate natural ad content with constraints
 */
@Service
public class MultiStagePromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(MultiStagePromptBuilder.class);

    /**
     * Build final enhanced prompt for ad content generation
     * This combines persona, context, and constraints into a natural prompt
     */
    public String buildEnhancedPrompt(String userPrompt,
                                       AdPersona persona,
                                      AdType adType,
                                      Language language,
                                      com.fbadsautomation.dto.AudienceSegmentRequest audienceSegment,
                                      com.fbadsautomation.model.AdStyle adStyle) {

        log.info("Building multi-stage prompt with persona: {}, style: {}, language: {}",
                 persona.name(), adStyle != null ? adStyle.name() : "none", language);

        StringBuilder enhancedPrompt = new StringBuilder();
        boolean isVietnamese = (language == Language.VIETNAMESE);

        // Stage 1: Context - User's original request
        enhancedPrompt.append(buildUserContextSection(userPrompt, isVietnamese));
        enhancedPrompt.append("\n\n");

        // Stage 2: Persona - Role, tone, vocabulary, examples
        enhancedPrompt.append(persona.getPersonaInstruction(isVietnamese));
        enhancedPrompt.append("\n\n");

        // Stage 2.5: Creative Style (if specified) - Issue #8
        if (adStyle != null) {
            enhancedPrompt.append(adStyle.getStyleInstruction(isVietnamese));
            enhancedPrompt.append("\n\n");
            log.debug("Added style instruction: {}", adStyle.name());
        }

        // Stage 3: Audience targeting (if provided)
        if (audienceSegment != null) {
            enhancedPrompt.append(buildAudienceSection(audienceSegment, isVietnamese));
            enhancedPrompt.append("\n\n");
        }

        // Stage 4: Technical constraints & format
        enhancedPrompt.append(buildConstraintsSection(adType, isVietnamese));
        enhancedPrompt.append("\n\n");

        // Stage 5: Critical instructions (CTA handling, natural tone)
        enhancedPrompt.append(buildCriticalInstructions(isVietnamese));

        log.debug("Enhanced prompt built (length: {} chars)", enhancedPrompt.length());
        return enhancedPrompt.toString();
    }

    /**
     * Issue #9: Build enhanced prompt with Campaign (audience at campaign level)
     * This is the NEW method that gets audience from Campaign.targetAudience
     */
    public String buildEnhancedPrompt(String userPrompt,
                                       AdPersona persona,
                                       AdType adType,
                                       Language language,
                                       Campaign campaign,
                                       com.fbadsautomation.model.AdStyle adStyle) {

        log.info("Building prompt with campaign-level audience (Issue #9): campaign={}, persona={}, style={}",
                 campaign != null ? campaign.getId() : "none",
                 persona.name(),
                 adStyle != null ? adStyle.name() : "none");

        StringBuilder enhancedPrompt = new StringBuilder();
        boolean isVietnamese = (language == Language.VIETNAMESE);

        // Stage 1: Context - User's original request
        enhancedPrompt.append(buildUserContextSection(userPrompt, isVietnamese));
        enhancedPrompt.append("\n\n");

        // Stage 2: Persona - Role, tone, vocabulary, examples
        enhancedPrompt.append(persona.getPersonaInstruction(isVietnamese));
        enhancedPrompt.append("\n\n");

        // Stage 2.5: Creative Style (if specified) - Issue #8
        if (adStyle != null) {
            enhancedPrompt.append(adStyle.getStyleInstruction(isVietnamese));
            enhancedPrompt.append("\n\n");
            log.debug("Added style instruction: {}", adStyle.name());
        }

        // Stage 3: Audience targeting from Campaign (Issue #9)
        if (campaign != null && campaign.getTargetAudience() != null && !campaign.getTargetAudience().trim().isEmpty()) {
            enhancedPrompt.append(buildAudienceSectionFromCampaign(campaign.getTargetAudience(), isVietnamese));
            enhancedPrompt.append("\n\n");
            log.debug("Added campaign audience: {}", campaign.getTargetAudience());
        }

        // Stage 4: Technical constraints & format
        enhancedPrompt.append(buildConstraintsSection(adType, isVietnamese));
        enhancedPrompt.append("\n\n");

        // Stage 5: Critical instructions (CTA handling, natural tone)
        enhancedPrompt.append(buildCriticalInstructions(isVietnamese));

        log.debug("Enhanced prompt built with campaign audience (length: {} chars)", enhancedPrompt.length());
        return enhancedPrompt.toString();
    }

    /**
     * Stage 1: User context section
     */
    private String buildUserContextSection(String userPrompt, boolean isVietnamese) {
        StringBuilder section = new StringBuilder();

        if (isVietnamese) {
            section.append("📋 YÊU CẦU TỪ NGƯỜI DÙNG:\n");
            section.append(userPrompt);
        } else {
            section.append("📋 USER REQUEST:\n");
            section.append(userPrompt);
        }

        return section.toString();
    }

    /**
     * Stage 3: Audience targeting section
     */
    private String buildAudienceSection(com.fbadsautomation.dto.AudienceSegmentRequest audienceSegment, boolean isVietnamese) {
        StringBuilder section = new StringBuilder();

        if (isVietnamese) {
            section.append("🎯 ĐỐI TƯỢNG MỤC TIÊU:\n");

            if (audienceSegment.getGender() != null) {
                section.append("• Giới tính: ").append(audienceSegment.getGender().getDisplayName()).append("\n");
            }

            if (audienceSegment.getMinAge() != null && audienceSegment.getMaxAge() != null) {
                section.append("• Độ tuổi: ").append(audienceSegment.getMinAge())
                        .append("-").append(audienceSegment.getMaxAge()).append(" tuổi\n");
            } else if (audienceSegment.getMinAge() != null) {
                section.append("• Độ tuổi: ").append(audienceSegment.getMinAge()).append("+ tuổi\n");
            }

            if (audienceSegment.getLocation() != null && !audienceSegment.getLocation().trim().isEmpty()) {
                section.append("• Khu vực: ").append(audienceSegment.getLocation()).append("\n");
            }

            if (audienceSegment.getInterests() != null && !audienceSegment.getInterests().trim().isEmpty()) {
                section.append("• Sở thích: ").append(audienceSegment.getInterests()).append("\n");
            }

            section.append("\n💡 Tạo nội dung phù hợp với đối tượng này!");
        } else {
            section.append("🎯 TARGET AUDIENCE:\n");

            if (audienceSegment.getGender() != null) {
                section.append("• Gender: ").append(audienceSegment.getGender().getDisplayName()).append("\n");
            }

            if (audienceSegment.getMinAge() != null && audienceSegment.getMaxAge() != null) {
                section.append("• Age: ").append(audienceSegment.getMinAge())
                        .append("-").append(audienceSegment.getMaxAge()).append(" years old\n");
            } else if (audienceSegment.getMinAge() != null) {
                section.append("• Age: ").append(audienceSegment.getMinAge()).append("+ years old\n");
            }

            if (audienceSegment.getLocation() != null && !audienceSegment.getLocation().trim().isEmpty()) {
                section.append("• Location: ").append(audienceSegment.getLocation()).append("\n");
            }

            if (audienceSegment.getInterests() != null && !audienceSegment.getInterests().trim().isEmpty()) {
                section.append("• Interests: ").append(audienceSegment.getInterests()).append("\n");
            }

            section.append("\n💡 Create content suitable for this audience!");
        }

        return section.toString();
    }

    /**
     * Issue #9: Build audience section from Campaign's targetAudience string
     * This is simpler than AudienceSegmentRequest as it's a free-form text field
     */
    private String buildAudienceSectionFromCampaign(String targetAudience, boolean isVietnamese) {
        StringBuilder section = new StringBuilder();

        if (isVietnamese) {
            section.append("🎯 ĐỐI TƯỢNG MỤC TIÊU (CẤP CHIẾN DỊCH):\n");
            section.append(targetAudience);
            section.append("\n\n💡 Tạo nội dung phù hợp với đối tượng này!");
        } else {
            section.append("🎯 TARGET AUDIENCE (CAMPAIGN LEVEL):\n");
            section.append(targetAudience);
            section.append("\n\n💡 Create content suitable for this audience!");
        }

        return section.toString();
    }

    /**
     * Stage 4: Technical constraints & format
     */
    private String buildConstraintsSection(AdType adType, boolean isVietnamese) {
        StringBuilder section = new StringBuilder();

        if (isVietnamese) {
            section.append("📐 YÊU CẦU KỸ THUẬT:\n");
            section.append("• Tiêu đề (Headline): Tối đa 40 ký tự, ngắn gọn, thu hút\n");
            section.append("• Mô tả (Description): Tối đa 125 ký tự, làm rõ lợi ích\n");
            section.append("• Văn bản chính (Primary Text): Tối đa 1000 ký tự, kể chuyện + social proof\n");
            section.append("• Định dạng: JSON với các trường headline, description, primaryText\n\n");

            section.append("⚖️ TUÂN THỦ CHÍNH SÁCH FACEBOOK:\n");
            section.append("• Không dùng: 'đảm bảo 100%', 'thần kỳ', 'làm giàu nhanh', 'chữa bệnh'\n");
            section.append("• Tránh: Chữ IN HOA toàn bộ, dấu chấm than quá nhiều (!!!), spam\n");
            section.append("• Phải: Trung thực, rõ ràng, không phóng đại\n");
        } else {
            section.append("📐 TECHNICAL REQUIREMENTS:\n");
            section.append("• Headline: Max 40 characters, concise, attention-grabbing\n");
            section.append("• Description: Max 125 characters, clarify benefits\n");
            section.append("• Primary Text: Max 1000 characters, storytelling + social proof\n");
            section.append("• Format: JSON with headline, description, primaryText fields\n\n");

            section.append("⚖️ FACEBOOK POLICY COMPLIANCE:\n");
            section.append("• Don't use: '100% guaranteed', 'miracle', 'get rich quick', 'cure disease'\n");
            section.append("• Avoid: ALL CAPS, excessive exclamation marks (!!!), spam\n");
            section.append("• Must: Be honest, clear, not exaggerated\n");
        }

        return section.toString();
    }

    /**
     * Stage 5: Critical instructions
     */
    private String buildCriticalInstructions(boolean isVietnamese) {
        StringBuilder section = new StringBuilder();

        if (isVietnamese) {
            section.append("⚠️ QUAN TRỌNG:\n");
            section.append("1. TUYỆT ĐỐI KHÔNG thêm từ khóa CTA (LEARN_MORE, SHOP_NOW, v.v.) vào cuối nội dung\n");
            section.append("2. Call-to-action sẽ được xử lý riêng bởi hệ thống\n");
            section.append("3. Viết tự nhiên như người thật nói chuyện, KHÔNG như brochure công ty\n");
            section.append("4. Dùng từ ngữ, cảm xúc phù hợp với persona và đối tượng mục tiêu\n");
            section.append("5. Tập trung vào lợi ích khách hàng, không chỉ liệt kê tính năng\n\n");

            section.append("✨ NHIỆM VỤ:\n");
            section.append("Tạo 1 quảng cáo Facebook theo format JSON, tự nhiên và hấp dẫn như ví dụ mẫu ở trên.");
        } else {
            section.append("⚠️ CRITICAL:\n");
            section.append("1. ABSOLUTELY DO NOT add CTA keywords (LEARN_MORE, SHOP_NOW, etc.) at the end of content\n");
            section.append("2. Call-to-action will be handled separately by the system\n");
            section.append("3. Write naturally like real people talk, NOT like corporate brochure\n");
            section.append("4. Use words and emotions matching the persona and target audience\n");
            section.append("5. Focus on customer benefits, not just listing features\n\n");

            section.append("✨ TASK:\n");
            section.append("Create 1 Facebook ad in JSON format, natural and engaging like the example above.");
        }

        return section.toString();
    }

    /**
     * Build simplified prompt (without persona) for backward compatibility
     * This is the old method that will be deprecated
     */
    @Deprecated
    public String buildLegacyPrompt(String userPrompt, AdType adType, Language language) {
        log.warn("Using legacy prompt builder - consider upgrading to persona-based prompting");

        StringBuilder enhanced = new StringBuilder(userPrompt);
        boolean isVietnamese = (language == Language.VIETNAMESE);

        if (isVietnamese) {
            enhanced.append("\n\n📋 YÊU CẦU NỘI DUNG:\n");
            enhanced.append("✓ Tiêu đề: Tối đa 40 ký tự\n");
            enhanced.append("✓ Mô tả: Tối đa 125 ký tự\n");
            enhanced.append("✓ Văn bản chính: Tối đa 1000 ký tự\n");
            enhanced.append("✓ Giọng điệu: Chuyên nghiệp, rõ ràng, hướng tới hành động\n");
            enhanced.append("✓ Phong cách: Tránh dấu chấm than/hỏi quá nhiều\n");
        } else {
            enhanced.append("\n\n📋 CONTENT REQUIREMENTS:\n");
            enhanced.append("✓ Headline: Maximum 40 characters\n");
            enhanced.append("✓ Description: Maximum 125 characters\n");
            enhanced.append("✓ Primary Text: Maximum 1000 characters\n");
            enhanced.append("✓ Tone: Professional, clear, action-oriented\n");
            enhanced.append("✓ Style: Avoid excessive punctuation\n");
        }

        return enhanced.toString();
    }
}
