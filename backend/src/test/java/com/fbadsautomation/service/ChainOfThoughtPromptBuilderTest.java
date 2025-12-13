package com.fbadsautomation.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbadsautomation.model.AdStyle;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.model.Gender;
import com.fbadsautomation.model.Persona;
import com.fbadsautomation.util.ValidationMessages.Language;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChainOfThoughtPromptBuilderTest {

    private final ChainOfThoughtPromptBuilder builder = new ChainOfThoughtPromptBuilder();

    @Test
    void buildCoTPrompt_includesVietnameseCTAAndVariations() {
        Persona persona = Persona.builder()
            .name("Lan")
            .age(32)
            .gender(Gender.FEMALE)
            .tone("casual")
            .painPoints(List.of("Tóc khô", "Thiếu tự tin"))
            .desiredOutcome("Tóc mềm mượt, dễ tạo kiểu")
            .build();

        String prompt = builder.buildCoTPrompt(
            "Quảng cáo về dầu gội Dove",
            persona,
            AdStyle.CASUAL,
            "Gender: FEMALE, Age: 37-49",
            List.of("mẫu dầu gội hot"),
            Language.VIETNAMESE,
            FacebookCTA.SHOP_NOW,
            AdType.PAGE_POST_AD,
            3,
            null,
            null,
            true,
            null,
            null
        );

        assertTrue(prompt.contains("Call-to-Action: Mua ngay"), "CTA phải được hiển thị bằng tiếng Việt");
        assertTrue(prompt.contains("Số lượng biến thể cần tạo: 3"), "Phải phản ánh số biến thể yêu cầu");
        assertTrue(prompt.contains("Bây giờ hãy tạo 3 biến thể quảng cáo"), "Hướng dẫn tạo nội dung phải đúng số lượng");
        assertTrue(prompt.contains("Ngôn ngữ: TIẾNG VIỆT"), "Phải khóa ngôn ngữ đầu ra");
        assertTrue(prompt.contains("CAM KẾT PHONG CÁCH BẮT BUỘC"), "Prompt phải nhấn mạnh yêu cầu phong cách không thể từ chối");
        assertTrue(prompt.contains("\"styleNotes\""), "JSON output phải yêu cầu styleNotes");
    }

    @Test
    void buildCoTPrompt_includesEnglishCTAAndVariations() {
        String prompt = builder.buildCoTPrompt(
            "Facebook ad for a SaaS project management tool",
            null,
            AdStyle.PROFESSIONAL,
            "Location: USA",
            List.of("productivity", "workflow"),
            Language.ENGLISH,
            FacebookCTA.SIGN_UP,
            AdType.WEBSITE_CONVERSION_AD,
            2,
            null,
            null,
            true,
            null,
            null
        );

        assertTrue(prompt.contains("Call-to-Action: SIGN_UP"), "CTA phải dùng giá trị tiếng Anh");
        assertTrue(prompt.contains("Number of variations to generate: 2"), "Stage 1 phải phản ánh số biến thể");
       	assertTrue(prompt.contains("Now generate 2 unique ad variations"), "Stage 6 phải yêu cầu đúng số biến thể");
        assertTrue(prompt.contains("NON-NEGOTIABLE STYLE COMMITMENT"), "Prompt phải yêu cầu giữ phong cách bắt buộc");
        assertTrue(prompt.contains("\"styleNotes\""), "JSON output phải chứa styleNotes");
    }
}
