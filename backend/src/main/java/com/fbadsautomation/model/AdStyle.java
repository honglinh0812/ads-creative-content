package com.fbadsautomation.model;

/**
 * Creative style/tone options for ad content generation.
 * Combines with AdPersona to guide AI content creation.
 *
 * This enum provides multilingual support for Vietnamese and English,
 * with style-specific instructions that are injected into AI prompts
 * to control the tone and creative approach of generated content.
 */
public enum AdStyle {

    PROFESSIONAL(
        "Chuyên nghiệp",
        "Professional",
        "Ngôn ngữ trang trọng, đáng tin cậy, tập trung vào uy tín và chuyên môn. Tránh slang, emoji, và cách diễn đạt quá thân mật.",
        "Formal, credible language focusing on expertise and trust. Avoid slang, emojis, and overly casual expressions."
    ),

    CASUAL(
        "Thân thiện",
        "Casual",
        "Ngôn ngữ thân thiện, trò chuyện tự nhiên như nói chuyện với bạn bè. Có thể dùng emoji và cách diễn đạt không chính thức.",
        "Friendly, conversational language like talking to a friend. Emojis and informal expressions are welcome."
    ),

    HUMOROUS(
        "Hài hước",
        "Humorous",
        "Kết hợp yếu tố hài hước, dí dỏm để tạo ấn tượng và dễ nhớ. Giữ sự liên quan đến sản phẩm/dịch vụ.",
        "Incorporate humor and wit to be memorable and entertaining. Stay relevant to the product/service."
    ),

    URGENT(
        "Khẩn cấp",
        "Urgent",
        "Tạo cảm giác cấp bách với ưu đãi có thời hạn, số lượng giới hạn hoặc lợi ích tức thì. Dùng ngôn ngữ kêu gọi hành động và FOMO.",
        "Create urgency with time-limited offers, limited availability, or immediate benefits. Use action-oriented language and FOMO triggers."
    ),

    LUXURY(
        "Cao cấp",
        "Luxury",
        "Nhấn mạnh chất lượng cao cấp, tính độc quyền và sự tinh tế. Sử dụng ngôn ngữ thanh lịch và làm nổi bật giá trị đẳng cấp.",
        "Emphasize premium quality, exclusivity, and sophistication. Use elegant language and highlight prestige value."
    ),

    EDUCATIONAL(
        "Giáo dục",
        "Educational",
        "Tập trung cung cấp giá trị qua thông tin, mẹo hay insights hữu ích. Định vị sản phẩm/dịch vụ như giải pháp cho vấn đề.",
        "Focus on providing value through information, tips, or insights. Position product/service as a solution."
    ),

    INSPIRATIONAL(
        "Truyền cảm hứng",
        "Inspirational",
        "Sử dụng ngôn ngữ động viên, trao quyền và nâng cao tinh thần. Kết nối sản phẩm với sự phát triển cá nhân, thành tựu hoặc thay đổi tích cực.",
        "Use motivational language that empowers and uplifts. Connect product to personal growth, achievement, or positive change."
    ),

    MINIMALIST(
        "Tối giản",
        "Minimalist",
        "Giữ thông điệp đơn giản, trực tiếp và gọn gàng. Tập trung vào lợi ích cốt lõi với tối thiểu trang trí.",
        "Keep messaging simple, direct, and uncluttered. Focus on core benefit with minimal embellishment."
    );

    private final String labelVietnamese;
    private final String labelEnglish;
    private final String descriptionVietnamese;
    private final String descriptionEnglish;

    AdStyle(String labelVietnamese, String labelEnglish,
            String descriptionVietnamese, String descriptionEnglish) {
        this.labelVietnamese = labelVietnamese;
        this.labelEnglish = labelEnglish;
        this.descriptionVietnamese = descriptionVietnamese;
        this.descriptionEnglish = descriptionEnglish;
    }

    /**
     * Get the display label for this style based on language
     * @param isVietnamese true for Vietnamese, false for English
     * @return Localized label
     */
    public String getLabel(boolean isVietnamese) {
        return isVietnamese ? labelVietnamese : labelEnglish;
    }

    /**
     * Get the description for this style based on language
     * @param isVietnamese true for Vietnamese, false for English
     * @return Localized description
     */
    public String getDescription(boolean isVietnamese) {
        return isVietnamese ? descriptionVietnamese : descriptionEnglish;
    }

    /**
     * Get style instruction for AI prompting.
     * This will be injected between Persona and Audience sections in the prompt.
     *
     * @param isVietnamese true for Vietnamese, false for English
     * @return Formatted style instruction for AI prompt
     */
    public String getStyleInstruction(boolean isVietnamese) {
        if (isVietnamese) {
            return "🎨 PHONG CÁCH SÁNG TẠO:\n" + descriptionVietnamese;
        } else {
            return "🎨 CREATIVE STYLE:\n" + descriptionEnglish;
        }
    }
}
