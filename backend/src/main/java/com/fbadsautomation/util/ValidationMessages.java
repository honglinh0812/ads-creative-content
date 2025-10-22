package com.fbadsautomation.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for bilingual validation messages (Vietnamese/English)
 * Provides language detection and message templates for prompt validation
 */
public class ValidationMessages {

    public enum Language {
        VIETNAMESE("vi"),
        ENGLISH("en");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    // Vietnamese keywords for language detection
    private static final String[] VIETNAMESE_KEYWORDS = {
        "là", "của", "và", "có", "được", "không", "với", "các", "người", "cho",
        "từ", "này", "đến", "như", "đã", "trong", "hay", "về", "khi", "để",
        "tôi", "bạn", "sản phẩm", "dịch vụ", "quảng cáo", "mua", "bán"
    };

    /**
     * Detect language from text content
     * @param text The text to analyze
     * @return Detected language (Vietnamese or English)
     */
    public static Language detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Language.ENGLISH; // Default to English
        }

        String lowerText = text.toLowerCase();
        int vietnameseCount = 0;

        // Count Vietnamese keyword occurrences
        for (String keyword : VIETNAMESE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                vietnameseCount++;
            }
        }

        // If 2 or more Vietnamese keywords found, consider it Vietnamese
        return vietnameseCount >= 2 ? Language.VIETNAMESE : Language.ENGLISH;
    }

    /**
     * Get validation error message in the appropriate language
     * @param messageKey The message key
     * @param language The target language
     * @return Localized message
     */
    public static String getMessage(String messageKey, Language language) {
        Map<String, String> messages = language == Language.VIETNAMESE
                ? getVietnameseMessages()
                : getEnglishMessages();

        return messages.getOrDefault(messageKey, messages.get("default_error"));
    }

    /**
     * Get improvement suggestion in the appropriate language
     * @param suggestionKey The suggestion key
     * @param language The target language
     * @return Localized suggestion
     */
    public static String getSuggestion(String suggestionKey, Language language) {
        Map<String, String> suggestions = language == Language.VIETNAMESE
                ? getVietnameseSuggestions()
                : getEnglishSuggestions();

        return suggestions.getOrDefault(suggestionKey, "");
    }

    // Vietnamese Messages
    private static Map<String, String> getVietnameseMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("too_vague", "Prompt quá chung chung và thiếu chi tiết cụ thể.");
        messages.put("missing_product", "Không tìm thấy thông tin về sản phẩm/dịch vụ trong prompt.");
        messages.put("missing_audience", "Không xác định được đối tượng khách hàng mục tiêu.");
        messages.put("missing_action", "Không có lời kêu gọi hành động (call-to-action) rõ ràng.");
        messages.put("too_short", "Prompt quá ngắn, cần thêm chi tiết để tạo quảng cáo hiệu quả.");
        messages.put("too_long", "Prompt quá dài, hãy tập trung vào các điểm chính.");
        messages.put("default_error", "Prompt cần được cải thiện để tạo quảng cáo chất lượng.");
        return messages;
    }

    // English Messages
    private static Map<String, String> getEnglishMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("too_vague", "Prompt is too vague and lacks specific details.");
        messages.put("missing_product", "No product or service information found in prompt.");
        messages.put("missing_audience", "Target audience is not clearly defined.");
        messages.put("missing_action", "No clear call-to-action specified.");
        messages.put("too_short", "Prompt is too short. Add more details for effective ad creation.");
        messages.put("too_long", "Prompt is too long. Focus on key points.");
        messages.put("default_error", "Prompt needs improvement for quality ad generation.");
        return messages;
    }

    // Vietnamese Suggestions
    private static Map<String, String> getVietnameseSuggestions() {
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("add_product_details", "Hãy mô tả chi tiết sản phẩm/dịch vụ của bạn: tính năng, lợi ích, giá trị đặc biệt.");
        suggestions.put("add_target_audience", "Xác định đối tượng mục tiêu: độ tuổi, giới tính, sở thích, vấn đề cần giải quyết.");
        suggestions.put("add_action", "Thêm lời kêu gọi hành động: 'mua ngay', 'đăng ký', 'tìm hiểu thêm', v.v.");
        suggestions.put("be_specific", "Cung cấp thông tin cụ thể hơn về ưu đãi, thời gian, địa điểm.");
        suggestions.put("simplify", "Rút gọn nội dung, tập trung vào 2-3 điểm chính quan trọng nhất.");
        suggestions.put("add_urgency", "Thêm yếu tố khẩn cấp hoặc khan hiếm để tạo động lực hành động.");
        suggestions.put("highlight_benefits", "Nhấn mạnh lợi ích khách hàng nhận được, không chỉ tính năng sản phẩm.");
        return suggestions;
    }

    // English Suggestions
    private static Map<String, String> getEnglishSuggestions() {
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("add_product_details", "Describe your product/service in detail: features, benefits, unique value.");
        suggestions.put("add_target_audience", "Define target audience: age, gender, interests, problems to solve.");
        suggestions.put("add_action", "Add call-to-action: 'buy now', 'sign up', 'learn more', etc.");
        suggestions.put("be_specific", "Provide specific information about offers, timing, location.");
        suggestions.put("simplify", "Simplify content, focus on 2-3 most important key points.");
        suggestions.put("add_urgency", "Add urgency or scarcity to motivate action.");
        suggestions.put("highlight_benefits", "Emphasize customer benefits, not just product features.");
        return suggestions;
    }

    /**
     * Get all suggestions for a prompt in the detected language
     * @param text The prompt text
     * @return Array of suggestions in the appropriate language
     */
    public static String[] getAllSuggestions(String text) {
        Language language = detectLanguage(text);
        String[] suggestionKeys = {
            "add_product_details",
            "add_target_audience",
            "add_action",
            "be_specific"
        };

        String[] suggestions = new String[suggestionKeys.length];
        for (int i = 0; i < suggestionKeys.length; i++) {
            suggestions[i] = getSuggestion(suggestionKeys[i], language);
        }

        return suggestions;
    }
}
