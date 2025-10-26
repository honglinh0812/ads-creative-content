package com.fbadsautomation.model;

/**
 * Ad persona definitions for natural, category-specific ad copy generation.
 * Each persona defines category and tone to guide AI.
 */
public enum AdPersona {

    GEN_Z_GAMER(
        "Gaming & Entertainment",
        "Thân thiện, hài hước, energetic như Gen Z gamer",
        "Friendly, humorous, energetic like Gen Z gamer"
    ),

    TRENDY_SHOPPER(
        "Fashion & Lifestyle",
        "Trendy, stylish, FOMO-inducing như fashionista",
        "Trendy, stylish, FOMO-inducing like fashionista"
    ),

    STUDENT_FOCUSED(
        "Education & Learning",
        "Động viên, dễ hiểu, relatable như bạn cùng lớp",
        "Encouraging, easy to understand, relatable like classmate"
    ),

    PROFESSIONAL_TRUSTWORTHY(
        "Finance & B2B Services",
        "Đáng tin cậy, chuyên nghiệp, rõ ràng",
        "Trustworthy, professional, clear"
    ),

    HEALTH_WELLNESS(
        "Health, Fitness & Beauty",
        "Động viên, tích cực, empathetic",
        "Encouraging, positive, empathetic"
    ),

    FOOD_BEVERAGE(
        "Food & Beverage",
        "Hấp dẫn, gợi cảm giác thèm ăn, friendly",
        "Appetizing, crave-inducing, friendly"
    ),

    GENERAL_FRIENDLY(
        "General/Default",
        "Thân thiện, rõ ràng, hướng tới hành động",
        "Friendly, clear, action-oriented"
    );

    private final String category;
    private final String toneVietnamese;
    private final String toneEnglish;

    AdPersona(String category,
              String toneVietnamese,
              String toneEnglish) {
        this.category = category;
        this.toneVietnamese = toneVietnamese;
        this.toneEnglish = toneEnglish;
    }

    public String getCategory() {
        return category;
    }

    public String getTone(boolean isVietnamese) {
        return isVietnamese ? toneVietnamese : toneEnglish;
    }

    /**
     * Get persona instruction text for AI prompting
     */
    public String getPersonaInstruction(boolean isVietnamese) {
        StringBuilder instruction = new StringBuilder();

        if (isVietnamese) {
            instruction.append("🎭 VAI TRÒ CỦA BẠN:\n");
            instruction.append("Bạn là copywriter chuyên viết quảng cáo ").append(category).append(" cho thị trường Việt Nam.\n\n");

            instruction.append("✨ GIỌNG ĐIỆU & PHONG CÁCH:\n");
            instruction.append(toneVietnamese).append("\n\n");

            instruction.append("⚠️ LƯU Ý QUAN TRỌNG:\n");
            instruction.append("- Tạo nội dung HOÀN TOÀN MỚI dựa trên yêu cầu của người dùng\n");
            instruction.append("- KHÔNG sao chép bất kỳ ví dụ nào từ training data\n");
            instruction.append("- Sử dụng giọng điệu phù hợp với ").append(category).append("\n");
        } else {
            instruction.append("🎭 YOUR ROLE:\n");
            instruction.append("You are a copywriter specializing in ").append(category).append(" ads.\n\n");

            instruction.append("✨ TONE & STYLE:\n");
            instruction.append(toneEnglish).append("\n\n");

            instruction.append("⚠️ IMPORTANT NOTES:\n");
            instruction.append("- Create COMPLETELY NEW content based on user request\n");
            instruction.append("- DO NOT copy any examples from training data\n");
            instruction.append("- Use appropriate tone for ").append(category).append("\n");
        }

        return instruction.toString();
    }
}
