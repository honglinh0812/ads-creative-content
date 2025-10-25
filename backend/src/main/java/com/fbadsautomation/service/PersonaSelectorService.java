package com.fbadsautomation.service;

import com.fbadsautomation.ai.OpenAIProvider;
import com.fbadsautomation.model.AdPersona;
import com.fbadsautomation.util.ValidationMessages.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for dynamically selecting appropriate persona based on product/service analysis.
 * Uses AI to classify product category and map to predefined personas.
 */
@Service
public class PersonaSelectorService {

    private static final Logger log = LoggerFactory.getLogger(PersonaSelectorService.class);

    @Autowired(required = false)
    private AIProviderService aiProviderService;

    // Fallback keyword matching for persona selection
    private static final Map<String, AdPersona> KEYWORD_PERSONA_MAP = new HashMap<>();

    static {
        // Gaming keywords
        KEYWORD_PERSONA_MAP.put("game", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("gaming", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("play together", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("esport", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("streamer", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("rank", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("solo", AdPersona.GEN_Z_GAMER);
        KEYWORD_PERSONA_MAP.put("duo", AdPersona.GEN_Z_GAMER);

        // Fashion keywords
        KEYWORD_PERSONA_MAP.put("thời trang", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("fashion", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("quần áo", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("clothes", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("phụ kiện", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("accessories", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("outfit", AdPersona.TRENDY_SHOPPER);
        KEYWORD_PERSONA_MAP.put("style", AdPersona.TRENDY_SHOPPER);

        // Education keywords
        KEYWORD_PERSONA_MAP.put("học", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("education", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("khóa học", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("course", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("ielts", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("toeic", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("luyện thi", AdPersona.STUDENT_FOCUSED);
        KEYWORD_PERSONA_MAP.put("exam prep", AdPersona.STUDENT_FOCUSED);

        // Finance/B2B keywords
        KEYWORD_PERSONA_MAP.put("tài chính", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("finance", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("kế toán", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("accounting", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("doanh nghiệp", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("business", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("b2b", AdPersona.PROFESSIONAL_TRUSTWORTHY);
        KEYWORD_PERSONA_MAP.put("enterprise", AdPersona.PROFESSIONAL_TRUSTWORTHY);

        // Health/Fitness keywords
        KEYWORD_PERSONA_MAP.put("sức khỏe", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("health", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("fitness", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("gym", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("giảm cân", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("weight loss", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("beauty", AdPersona.HEALTH_WELLNESS);
        KEYWORD_PERSONA_MAP.put("làm đẹp", AdPersona.HEALTH_WELLNESS);

        // Food keywords
        KEYWORD_PERSONA_MAP.put("ăn", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("food", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("nhà hàng", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("restaurant", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("món ăn", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("dish", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("cafe", AdPersona.FOOD_BEVERAGE);
        KEYWORD_PERSONA_MAP.put("đồ uống", AdPersona.FOOD_BEVERAGE);
    }

    /**
     * Select persona based on product description with AI classification + keyword fallback
     * Results are cached for 24 hours to reduce API calls
     */
    @Cacheable(value = "personaSelection", key = "#productPrompt", unless = "#result == null")
    public AdPersona selectPersona(String productPrompt, Language language) {
        log.info("Selecting persona for prompt: {}", productPrompt.substring(0, Math.min(100, productPrompt.length())));

        // Try AI-powered classification first
        if (aiProviderService != null) {
            try {
                AdPersona aiSelected = classifyWithAI(productPrompt, language);
                if (aiSelected != null) {
                    log.info("AI selected persona: {} for category: {}", aiSelected.name(), aiSelected.getCategory());
                    return aiSelected;
                }
            } catch (Exception e) {
                log.warn("AI persona classification failed, falling back to keyword matching: {}", e.getMessage());
            }
        }

        // Fallback to keyword-based matching
        AdPersona keywordSelected = classifyWithKeywords(productPrompt);
        log.info("Keyword selected persona: {} for category: {}", keywordSelected.name(), keywordSelected.getCategory());
        return keywordSelected;
    }

    /**
     * AI-powered product classification
     */
    private AdPersona classifyWithAI(String productPrompt, Language language) {
        try {
            OpenAIProvider provider = (OpenAIProvider) aiProviderService.getProvider("openai");
            if (provider == null) {
                return null;
            }

            String classificationPrompt = buildClassificationPrompt(productPrompt, language);
            String response = callOpenAIForClassification(provider, classificationPrompt);

            return mapCategoryToPersona(response);
        } catch (Exception e) {
            log.error("Error during AI classification: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build classification prompt for AI
     */
    private String buildClassificationPrompt(String productPrompt, Language language) {
        StringBuilder prompt = new StringBuilder();

        if (language == Language.VIETNAMESE) {
            prompt.append("Phân loại sản phẩm/dịch vụ sau vào 1 trong các danh mục:\n\n");
            prompt.append("DANH MỤC:\n");
            prompt.append("1. GAMING - Game, esports, play together, streaming\n");
            prompt.append("2. FASHION - Thời trang, quần áo, phụ kiện, giày dép\n");
            prompt.append("3. EDUCATION - Khóa học, luyện thi, đào tạo, học online\n");
            prompt.append("4. FINANCE - Tài chính, kế toán, B2B, doanh nghiệp\n");
            prompt.append("5. HEALTH - Sức khỏe, fitness, gym, làm đẹp, giảm cân\n");
            prompt.append("6. FOOD - Đồ ăn, nhà hàng, cafe, món ăn, đồ uống\n");
            prompt.append("7. GENERAL - Các sản phẩm/dịch vụ khác\n\n");
            prompt.append("SẢN PHẨM/DỊCH VỤ:\n");
            prompt.append(productPrompt).append("\n\n");
            prompt.append("YÊU CẦU: Chỉ trả lời 1 từ (GAMING, FASHION, EDUCATION, FINANCE, HEALTH, FOOD, hoặc GENERAL)");
        } else {
            prompt.append("Classify the following product/service into one category:\n\n");
            prompt.append("CATEGORIES:\n");
            prompt.append("1. GAMING - Games, esports, play together, streaming\n");
            prompt.append("2. FASHION - Fashion, clothes, accessories, shoes\n");
            prompt.append("3. EDUCATION - Courses, exam prep, training, online learning\n");
            prompt.append("4. FINANCE - Finance, accounting, B2B, enterprise\n");
            prompt.append("5. HEALTH - Health, fitness, gym, beauty, weight loss\n");
            prompt.append("6. FOOD - Food, restaurant, cafe, dishes, beverages\n");
            prompt.append("7. GENERAL - Other products/services\n\n");
            prompt.append("PRODUCT/SERVICE:\n");
            prompt.append(productPrompt).append("\n\n");
            prompt.append("REQUIREMENT: Reply with only 1 word (GAMING, FASHION, EDUCATION, FINANCE, HEALTH, FOOD, or GENERAL)");
        }

        return prompt.toString();
    }

    /**
     * Call OpenAI for classification
     */
    private String callOpenAIForClassification(OpenAIProvider provider, String prompt) {
        try {
            // Use OpenAI's generateAdContent method - we'll use headline as the classification response
            var contents = provider.generateAdContent(prompt, 1, null, null);

            if (contents != null && !contents.isEmpty()) {
                // Use headline as classification category (it will be shortest/most concise)
                String category = contents.get(0).getHeadline();
                if (category != null) {
                    return category.trim().toUpperCase();
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error calling OpenAI for classification: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Map AI response category to persona
     */
    private AdPersona mapCategoryToPersona(String category) {
        if (category == null) {
            return AdPersona.GENERAL_FRIENDLY;
        }

        String normalized = category.trim().toUpperCase();

        if (normalized.contains("GAMING")) {
            return AdPersona.GEN_Z_GAMER;
        } else if (normalized.contains("FASHION")) {
            return AdPersona.TRENDY_SHOPPER;
        } else if (normalized.contains("EDUCATION")) {
            return AdPersona.STUDENT_FOCUSED;
        } else if (normalized.contains("FINANCE")) {
            return AdPersona.PROFESSIONAL_TRUSTWORTHY;
        } else if (normalized.contains("HEALTH")) {
            return AdPersona.HEALTH_WELLNESS;
        } else if (normalized.contains("FOOD")) {
            return AdPersona.FOOD_BEVERAGE;
        } else {
            return AdPersona.GENERAL_FRIENDLY;
        }
    }

    /**
     * Keyword-based classification (fallback)
     */
    private AdPersona classifyWithKeywords(String productPrompt) {
        String lowerPrompt = productPrompt.toLowerCase();

        // Count matches for each persona
        Map<AdPersona, Integer> matchCounts = new HashMap<>();

        for (Map.Entry<String, AdPersona> entry : KEYWORD_PERSONA_MAP.entrySet()) {
            if (lowerPrompt.contains(entry.getKey())) {
                matchCounts.put(entry.getValue(), matchCounts.getOrDefault(entry.getValue(), 0) + 1);
            }
        }

        // Return persona with most keyword matches
        if (!matchCounts.isEmpty()) {
            return matchCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get()
                    .getKey();
        }

        // Default to GENERAL_FRIENDLY if no matches
        return AdPersona.GENERAL_FRIENDLY;
    }
}
