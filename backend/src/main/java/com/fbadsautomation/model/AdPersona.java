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
        List.of("chill", "flex", "vibe", "bro", "lit", "ez", "carry", "rank", "solo", "duo", "squad"),
        List.of("chill", "flex", "vibe", "bro", "lit", "ez", "carry", "rank", "solo", "duo", "squad"),
        List.of("trải nghiệm", "kết nối đích thực", "chuyên nghiệp", "uy tín", "đẳng cấp"),
        List.of("experience the", "genuine connection", "professional", "prestigious", "high-class"),
        "Solo buồn? Duo với Yang!\n\n" +
        "Bạn bè bận rộn, rank down vì team troll? YangBuffSao có đội ngũ player chuyên nghiệp, " +
        "vừa chơi vừa chill, giúp bạn carry game dễ dàng. Giá chỉ từ 50k/giờ, book ngay!",

        "Tired of solo queue?\n\n" +
        "Friends busy, losing rank because of troll teammates? YangBuffSao has professional players " +
        "who can carry you while keeping it chill. Starting at $2/hour, book now!"
    ),

    TRENDY_SHOPPER(
        "Fashion & Lifestyle",
        "Trendy, stylish, FOMO-inducing như fashionista",
        "Trendy, stylish, FOMO-inducing like fashionista",
        List.of("xịn", "chanh sả", "hot trend", "sale sập sàn", "limited", "best seller", "đu trend"),
        List.of("trendy", "hot sale", "limited edition", "best seller", "must-have", "viral", "trending"),
        List.of("sản phẩm", "dịch vụ", "cơ hội", "trải nghiệm"),
        List.of("product", "service", "opportunity", "experience"),
        "Set đồ này hot lắm chị ơi! 🔥\n\n" +
        "Limited 100 bộ đầu tiên giảm 50%, ship COD toàn quốc. Mấy em hot girl đang đu trend này nha, " +
        "chần chừ là hết size đó! Free ship + tặng kèm túi xinh xắn nữa nhé.",

        "This outfit is on fire! 🔥\n\n" +
        "Limited 50% off for first 100 sets, COD shipping nationwide. All the influencers are rocking this trend, " +
        "hesitate and your size will be gone! Free shipping + cute bag included."
    ),

    STUDENT_FOCUSED(
        "Education & Learning",
        "Động viên, dễ hiểu, relatable như bạn cùng lớp",
        "Encouraging, easy to understand, relatable like classmate",
        List.of("dễ hiểu", "học nhanh", "thi đỗ", "điểm cao", "tiết kiệm thời gian", "ôn tập hiệu quả"),
        List.of("easy to learn", "quick study", "pass exam", "high score", "save time", "effective review"),
        List.of("đảm bảo", "thần kỳ", "100% đỗ"),
        List.of("guaranteed", "miracle", "100% pass"),
        "Ôn thi căng thẳng quá phải không? 📚\n\n" +
        "Khóa luyện đề IELTS 7.0+ của mình có 1000+ bạn đã đạt target, học theo lộ trình rõ ràng, " +
        "giáo viên 8.5 IELTS hướng dẫn tận tình. Học thử FREE 3 ngày, thấy hiệu quả mới đóng tiền nhé!",

        "Stressed about exam prep? 📚\n\n" +
        "Our IELTS 7.0+ prep course helped 1000+ students hit their target, clear learning path, " +
        "experienced 8.5 IELTS instructor. Try FREE for 3 days, only pay if you see results!"
    ),

    PROFESSIONAL_TRUSTWORTHY(
        "Finance & B2B Services",
        "Đáng tin cậy, chuyên nghiệp, rõ ràng",
        "Trustworthy, professional, clear",
        List.of("hiệu quả", "chất lượng", "uy tín", "minh bạch", "an toàn", "bảo mật"),
        List.of("effective", "quality", "reputable", "transparent", "secure", "privacy"),
        List.of("làm giàu nhanh", "thần kỳ", "bí mật", "không cần vốn"),
        List.of("get rich quick", "miracle", "secret", "no capital needed"),
        "Quản lý tài chính doanh nghiệp hiệu quả với CloudBooks\n\n" +
        "Phần mềm kế toán số 1 Việt Nam với 50,000+ doanh nghiệp tin dùng. Tự động hóa 90% công việc kế toán, " +
        "tuân thủ luật thuế mới nhất, hỗ trợ 24/7. Dùng thử miễn phí 30 ngày, không cần thẻ tín dụng.",

        "Efficient business finance management with CloudBooks\n\n" +
        "Vietnam's #1 accounting software trusted by 50,000+ businesses. Automate 90% of accounting tasks, " +
        "comply with latest tax laws, 24/7 support. Free 30-day trial, no credit card required."
    ),

    HEALTH_WELLNESS(
        "Health, Fitness & Beauty",
        "Động viên, tích cực, empathetic",
        "Encouraging, positive, empathetic",
        List.of("khỏe đẹp", "tự tin", "năng động", "tươi trẻ", "khoa học", "tự nhiên"),
        List.of("healthy", "confident", "energetic", "youthful", "scientific", "natural"),
        List.of("đảm bảo", "thần kỳ", "giảm 10kg trong 1 tuần", "không tác dụng phụ"),
        List.of("guaranteed", "miracle", "lose 10kg in 1 week", "no side effects"),
        "Bạn đã thử mọi cách giảm cân nhưng vẫn không hiệu quả?\n\n" +
        "FitLife đồng hành cùng 10,000+ người Việt lấy lại vóc dáng khỏe đẹp. Lộ trình khoa học từ chuyên gia dinh dưỡng, " +
        "tập luyện phù hợp cơ địa, theo dõi sát sao. Cam kết hoàn tiền nếu không hài lòng. Đăng ký tư vấn FREE!",

        "Tried everything to lose weight but nothing works?\n\n" +
        "FitLife helped 10,000+ Vietnamese get back in shape. Scientific program from nutrition experts, " +
        "personalized workouts, close monitoring. Money-back guarantee if not satisfied. Sign up for FREE consultation!"
    ),

    FOOD_BEVERAGE(
        "Food & Beverage",
        "Hấp dẫn, gợi cảm giác thèm ăn, friendly",
        "Appetizing, crave-inducing, friendly",
        List.of("ngon tuyệt", "đậm đà", "thơm nức", "tươi ngon", "nóng hổi", "giòn tan"),
        List.of("delicious", "rich flavor", "aromatic", "fresh", "hot & crispy", "crunchy"),
        List.of("tuyệt đối an toàn", "không hóa chất", "100% tự nhiên"),
        List.of("absolutely safe", "no chemicals", "100% natural"),
        "Phở bò Hà Nội chuẩn vị tại Sài Gòn! 🍜\n\n" +
        "Nước dùng ninh 12 tiếng, thịt bò Úc mềm tan, bánh phở tươi mỗi ngày. Ăn 1 lần là nhớ mãi! " +
        "Giờ mới mở cửa, giảm 20% combo 2 tô. Giao tận nơi trong 30 phút, miễn phí ship bán kính 3km.",

        "Authentic Hanoi beef pho in Saigon! 🍜\n\n" +
        "Broth simmered 12 hours, Australian beef melts in mouth, fresh noodles daily. One taste and you're hooked! " +
        "Grand opening 20% off combo for 2 bowls. Delivery in 30 min, free shipping within 3km."
    ),

    GENERAL_FRIENDLY(
        "General/Default",
        "Thân thiện, rõ ràng, hướng tới hành động",
        "Friendly, clear, action-oriented",
        List.of("dễ dàng", "nhanh chóng", "tiện lợi", "chất lượng", "uy tín"),
        List.of("easy", "fast", "convenient", "quality", "reliable"),
        List.of("đảm bảo 100%", "thần kỳ", "làm giàu nhanh"),
        List.of("100% guaranteed", "miracle", "get rich quick"),
        "Giải pháp hoàn hảo cho nhu cầu của bạn!\n\n" +
        "Sản phẩm/dịch vụ chất lượng cao với giá cả hợp lý. Hàng nghìn khách hàng hài lòng. " +
        "Hỗ trợ tư vấn miễn phí, đổi trả trong 7 ngày. Liên hệ ngay để nhận ưu đãi đặc biệt!",

        "Perfect solution for your needs!\n\n" +
        "High-quality product/service at reasonable price. Thousands of satisfied customers. " +
        "Free consultation, 7-day return policy. Contact now for special offers!"
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
