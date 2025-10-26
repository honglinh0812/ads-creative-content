package com.fbadsautomation.model;

import java.util.List;

/**
 * Ad persona definitions for natural, category-specific ad copy generation.
 * Each persona defines tone, vocabulary, examples, and forbidden words to guide AI.
 */
public enum AdPersona {

    GEN_Z_GAMER(
        "Gaming & Entertainment",
        "Thân thiện, hài hước, energetic như Gen Z gamer",
        "Friendly, humorous, energetic like Gen Z gamer",
    ),

    TRENDY_SHOPPER(
        "Fashion & Lifestyle",
        "Trendy, stylish, FOMO-inducing như fashionista",
        "Trendy, stylish, FOMO-inducing like fashionista",
    ),

    STUDENT_FOCUSED(
        "Education & Learning",
        "Động viên, dễ hiểu, relatable như bạn cùng lớp",
        "Encouraging, easy to understand, relatable like classmate",
    ),

    PROFESSIONAL_TRUSTWORTHY(
        "Finance & B2B Services",
        "Đáng tin cậy, chuyên nghiệp, rõ ràng",
        "Trustworthy, professional, clear",
    ),

    HEALTH_WELLNESS(
        "Health, Fitness & Beauty",
        "Động viên, tích cực, empathetic",
        "Encouraging, positive, empathetic",
    ),

    FOOD_BEVERAGE(
        "Food & Beverage",
        "Hấp dẫn, gợi cảm giác thèm ăn, friendly",
        "Appetizing, crave-inducing, friendly",
    ),

    GENERAL_FRIENDLY(
        "General/Default",
        "Thân thiện, rõ ràng, hướng tới hành động",
        "Friendly, clear, action-oriented",
    );

    private final String category;
    private final String toneVietnamese;
    private final String toneEnglish;
    private final List<String> vocabularyVietnamese;
    private final List<String> vocabularyEnglish;
    private final List<String> avoidVietnamese;
    private final List<String> avoidEnglish;
    private final String exampleVietnamese;
    private final String exampleEnglish;

    AdPersona(String category,
              String toneVietnamese,
              String toneEnglish,
              List<String> vocabularyVietnamese,
              List<String> vocabularyEnglish,
              List<String> avoidVietnamese,
              List<String> avoidEnglish,
              String exampleVietnamese,
              String exampleEnglish) {
        this.category = category;
        this.toneVietnamese = toneVietnamese;
        this.toneEnglish = toneEnglish;
        this.vocabularyVietnamese = vocabularyVietnamese;
        this.vocabularyEnglish = vocabularyEnglish;
        this.avoidVietnamese = avoidVietnamese;
        this.avoidEnglish = avoidEnglish;
        this.exampleVietnamese = exampleVietnamese;
        this.exampleEnglish = exampleEnglish;
    }

    public String getCategory() {
        return category;
    }

    public String getTone(boolean isVietnamese) {
        return isVietnamese ? toneVietnamese : toneEnglish;
    }

    public List<String> getVocabulary(boolean isVietnamese) {
        return isVietnamese ? vocabularyVietnamese : vocabularyEnglish;
    }

    public List<String> getAvoid(boolean isVietnamese) {
        return isVietnamese ? avoidVietnamese : avoidEnglish;
    }

    public String getExample(boolean isVietnamese) {
        return isVietnamese ? exampleVietnamese : exampleEnglish;
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

            instruction.append("📝 TỪ VỰNG NÊN DÙNG:\n");
            instruction.append(String.join(", ", vocabularyVietnamese)).append("\n\n");

            instruction.append("🚫 TUYỆT ĐỐI TRÁNH:\n");
            instruction.append(String.join(", ", avoidVietnamese)).append("\n\n");

            instruction.append("💡 PHONG CÁCH THAM KHẢO (KHÔNG SAO CHÉP):\n");
            instruction.append("Viết tự nhiên, hài hước, gần gũi như ví dụ này (CHỈ THAM KHẢO CÁCH VIẾT, không copy nội dung):\n");
            instruction.append("\"").append(exampleVietnamese).append("\"\n\n");
            instruction.append("⚠️ LƯU Ý: Tạo nội dung HOÀN TOÀN MỚI dựa trên yêu cầu của người dùng, ");
            instruction.append("CHỈ học phong cách viết từ ví dụ trên, KHÔNG sao chép nội dung, tên sản phẩm hay chi tiết cụ thể.\n");
        } else {
            instruction.append("🎭 YOUR ROLE:\n");
            instruction.append("You are a copywriter specializing in ").append(category).append(" ads for Vietnamese market.\n\n");

            instruction.append("✨ TONE & STYLE:\n");
            instruction.append(toneEnglish).append("\n\n");

            instruction.append("📝 VOCABULARY TO USE:\n");
            instruction.append(String.join(", ", vocabularyEnglish)).append("\n\n");

            instruction.append("🚫 ABSOLUTELY AVOID:\n");
            instruction.append(String.join(", ", avoidEnglish)).append("\n\n");

            instruction.append("💡 STYLE REFERENCE (DO NOT COPY):\n");
            instruction.append("Write naturally, engagingly like this example (REFERENCE STYLE ONLY, don't copy content):\n");
            instruction.append("\"").append(exampleEnglish).append("\"\n\n");
            instruction.append("⚠️ NOTE: Create COMPLETELY NEW content based on user request, ");
            instruction.append("ONLY learn writing style from example above, DO NOT copy content, product names or specific details.\n");
        }

        return instruction.toString();
    }
}
