package com.fbadsautomation.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public enum FacebookCTA {
    SHOP_NOW("Shop Now", "Mua ngay"),
    LEARN_MORE("Learn More", "Tìm hiểu thêm"),
    SIGN_UP("Sign Up", "Đăng ký"),
    DOWNLOAD("Download", "Tải xuống"),
    CONTACT_US("Contact Us", "Liên hệ"),
    APPLY_NOW("Apply Now", "Ứng tuyển ngay"),
    BOOK_NOW("Book Now", "Đặt ngay"),
    GET_OFFER("Get Offer", "Nhận ưu đãi"),
    MESSAGE_PAGE("Message Page", "Nhắn tin"),
    SUBSCRIBE("Subscribe", "Theo dõi");

    private final String englishLabel;
    private final String vietnameseLabel;

    FacebookCTA(String englishLabel, String vietnameseLabel) {
        this.englishLabel = englishLabel;
        this.vietnameseLabel = vietnameseLabel;
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }

    public String getLabel() {
        return englishLabel; // Default to English
    }
    
    public String getLabel(String language) {
        if ("vi".equalsIgnoreCase(language)) {
            return vietnameseLabel;
        }
        return englishLabel; // Default to English for any other language
    }

    public String getEnglishLabel() {
        return englishLabel;
    }

    public String getVietnameseLabel() {
        return vietnameseLabel;
    }

    /**
     * Phase 3: Alias for ChainOfThoughtPromptBuilder compatibility
     */
    public String getDisplayNameVietnamese() {
        return vietnameseLabel;
    }
} 