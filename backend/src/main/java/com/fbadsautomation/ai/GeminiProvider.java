package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiProvider implements AIProvider {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON
    public GeminiProvider(
            RestTemplate restTemplate,
            @Value("${ai.gemini.api-key}") String apiKey,
            @Value("${ai.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}") String baseUrl) { // Use v1beta for JSON mode
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        // Construct the full URL with the model name and action
        this.apiUrl = baseUrl + "/gemini-1.5-pro:generateContent";
        log.info("Using Gemini API URL: {}", this.apiUrl);
    }
    // Corrected return type to List<AdContent>
    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, com.fbadsautomation.model.FacebookCTA callToAction) {
        List<AdContent> adContents = new ArrayList<>();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key is missing. Returning mock data.");
            return generateMockAdContents(prompt, numberOfVariations, callToAction);
        }
        String fullUrl = apiUrl + "?key=" + apiKey;
        log.debug("Calling Gemini API at: {}", fullUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Updated prompt for JSON output
        String fullPrompt;
        if ("en".equalsIgnoreCase(language)) {
            fullPrompt = "You are an expert Facebook advertising copywriter with deep knowledge of Facebook's advertising policies. " +
                    "Create " + numberOfVariations + " compliant, high-converting ad variations for: \"" + prompt + "\". " +
                    "\n\nFACEBOOK COMPLIANCE CHECKLIST:" +
                    "\n✓ Headline: Exactly 40 characters or less" +
                    "\n✓ Description: Exactly 125 characters or less" +
                    "\n✓ Primary Text: Exactly 1000 characters or less" +
                    "\n✓ No prohibited words (hate, violence, drugs, miracle, guaranteed, cure, instant)" +
                    "\n✓ No excessive punctuation (!!!, $$$, ???)" +
                    "\n✓ No ALL CAPS (except brand names)" +
                    "\n✓ Professional, clear language" +
                    "\n✓ Minimum 3 words per field" +
                    "\n\nBefore finalizing each ad, count characters and verify compliance. " +
                    "Return as JSON array of " + numberOfVariations + " objects: {\"headline\": \"...\", \"description\": \"...\", \"primaryText\": \"...\", \"callToAction\": \"...\"}.";
        } else {
            fullPrompt = "Bạn là chuyên gia viết quảng cáo Facebook am hiểu sâu về chính sách quảng cáo Facebook. " +
                    "Tạo " + numberOfVariations + " mẫu quảng cáo tuân thủ và hiệu quả cho: \"" + prompt + "\". " +
                    "\n\nDANH SÁCH KIỂM TRA TUÂN THỦ FACEBOOK:" +
                    "\n✓ Tiêu đề: Chính xác 40 ký tự hoặc ít hơn" +
                    "\n✓ Mô tả: Chính xác 30 ký tự hoặc ít hơn" +
                    "\n✓ Nội dung chính: Chính xác 125 ký tự hoặc ít hơn" +
                    "\n✓ Không có từ cấm (ghét, bạo lực, ma túy, phép màu, đảm bảo, chữa khỏi, tức thì)" +
                    "\n✓ Không dấu câu thừa (!!!, $$$, ???)" +
                    "\n✓ Không viết hoa toàn bộ (trừ tên thương hiệu)" +
                    "\n✓ Ngôn ngữ chuyên nghiệp, rõ ràng" +
                    "\n✓ Tối thiểu 3 từ mỗi trường" +
                    "\n\nTrước khi hoàn thiện mỗi quảng cáo, hãy đếm ký tự và xác minh tuân thủ. " +
                    "Trả về dạng JSON array gồm " + numberOfVariations + " đối tượng: {\"headline\": \"...\", \"description\": \"...\", \"primaryText\": \"...\", \"callToAction\": \"...\"}.";
        }
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", fullPrompt);
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(contentPart));
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        // Add generationConfig for JSON output
        Map<String, Object> generationConfig = new HashMap<>();
        // Loại bỏ responseMimeType vì không được hỗ trợ trong API version hiện tại
        generationConfig.put("candidateCount", 1); // Request one candidate with potentially multiple variations inside
        requestBody.put("generationConfig", generationConfig);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);
            log.debug("Gemini API Response: {}", response);
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty() && candidates.get(0).containsKey("content")) {
                    Map<String, Object> responseContent = (Map<String, Object>) candidates.get(0).get("content");
                    if (responseContent.containsKey("parts")) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) responseContent.get("parts");
                        if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                            String contentText = (String) parts.get(0).get("text");
                            log.debug("Received content text from Gemini: {}", contentText);
                            // Attempt to parse the JSON content directly into List<AdContent>
                            try {
                                List<AdContent> parsedContents = objectMapper.readValue(contentText, new TypeReference<List<AdContent>>() {});
                                for (AdContent adContent : parsedContents) {
                                    adContent.setAiProvider(AdContent.AIProvider.GEMINI);
                                    adContent.setIsSelected(false); // Default value
                                    adContent.setCallToAction(callToAction);
                                    adContent.setCta(callToAction);
                                    adContents.add(adContent);
                                    if (adContents.size() >= numberOfVariations) break; // Stop if enough variations are parsed
                                }
                                log.info("Successfully parsed {} ad contents from Gemini.", adContents.size());
                            } catch (Exception e) {
                                log.error("Failed to parse JSON response from Gemini: {}", contentText, e);
                            }
                        }
                    }
                }
            }
            if (adContents.isEmpty()) {
                 log.warn("Failed to parse valid ad content from Gemini response. Response: {}", response);
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
        }
        // Fill with mock data if not enough variations generated/parsed
        while (adContents.size() < numberOfVariations) {
            log.warn("Generated/Parsed only {} valid ad contents from Gemini, filling remaining {} with mock data.", adContents.size(), numberOfVariations - adContents.size());
            adContents.addAll(generateMockAdContents(prompt, 1, callToAction));
        }
        return adContents;
    }

    @Override
    public String generateImage(String prompt) {
        log.warn("Gemini does not support image generation via this API.");
        return "/img/placeholder.png"; // Return local placeholder
    }

    @Override
    public java.util.Set<com.fbadsautomation.model.Capability> getCapabilities() {
        return java.util.EnumSet.of(com.fbadsautomation.model.Capability.TEXT_GENERATION);
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    public boolean supportsImageGeneration() {
        return false;
    }

    @Override
    public String enhanceImage(String imagePath, String enhancementType, java.util.Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by Gemini");
    }

    // Mock Ad Content Generation (Corrected return type)
    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations, com.fbadsautomation.model.FacebookCTA callToAction) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            adContent.setHeadline("Gemini: Tiêu đề mẫu #" + (i + 1) + " cho: " + prompt);
            adContent.setDescription("Mô tả ngắn gọn cho mẫu quảng cáo Gemini #" + (i + 1));
            adContent.setPrimaryText("Đây là nội dung chính của mẫu quảng cáo Gemini #" + (i + 1) + ". Nội dung này sẽ mô tả chi tiết về sản phẩm hoặc dịch vụ được quảng cáo.");
            adContent.setCallToAction(callToAction);
            adContent.setCta(callToAction);
            adContent.setImageUrl("/img/placeholder.png"); // Use local placeholder
            adContent.setAiProvider(AdContent.AIProvider.GEMINI);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
    }
}