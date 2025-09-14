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

public class AnthropicProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public AnthropicProvider(
            RestTemplate restTemplate,
            @Value("${ai.anthropic.api-key:") String apiKey,
            @Value("${ai.anthropic.api-url:https://api.anthropic.com/v1/messages}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        log.info("Using Anthropic API URL: {}", this.apiUrl);
    }
    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, com.fbadsautomation.model.FacebookCTA callToAction) {
        List<AdContent> adContents = new ArrayList<>();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Anthropic API key is missing. Returning mock data.");
            return generateMockAdContents(prompt, numberOfVariations, callToAction);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        String fullPrompt;
        if ("en".equalsIgnoreCase(language)) {
            fullPrompt = "You are a professional Facebook ad copywriter specializing in policy-compliant, high-converting ads. " +
                    "Generate " + numberOfVariations + " different ad variations for: \"" + prompt + "\". " +
                    "\n\nSTRICT FACEBOOK REQUIREMENTS:" +
                    "\n- Headline: MAX 40 characters (count carefully)" +
                    "\n- Description: MAX 125 characters (count carefully)" +
                    "\n- Primary Text: MAX 1000 characters (count carefully)" +
                    "\n- Each field needs minimum 3 meaningful words" +
                    "\n\nFORBIDDEN CONTENT:" +
                    "\n- Words: hate, violence, drugs, miracle, guaranteed, cure, instant, free (with exclamation)" +
                    "\n- Excessive punctuation: !!!, $$$, ???" +
                    "\n- ALL CAPS text (except brand names)" +
                    "\n- Misleading or exaggerated claims" +
                    "\n\nReturn as JSON array of " + numberOfVariations + " objects with keys: headline, description, primaryText, callToAction.";
        } else {
            fullPrompt = "Bạn là chuyên gia viết quảng cáo Facebook tuân thủ chính sách và hiệu quả cao. " +
                    "Tạo " + numberOfVariations + " mẫu quảng cáo khác nhau cho: \"" + prompt + "\". " +
                    "\n\nYÊU CẦU FACEBOOK NGHIÊM NGẶT:" +
                    "\n- Tiêu đề (headline): TỐI ĐA 40 ký tự (đếm cẩn thận)" +
                    "\n- Mô tả (description): TỐI ĐA 30 ký tự (đếm cẩn thận)" +
                    "\n- Nội dung chính (primaryText): TỐI ĐA 125 ký tự (đếm cẩn thận)" +
                    "\n- Mỗi trường cần tối thiểu 3 từ có nghĩa" +
                    "\n\nNỘI DUNG CẤM:" +
                    "\n- Từ ngữ: ghét, bạo lực, ma túy, phép màu, đảm bảo, chữa khỏi, tức thì, miễn phí (với dấu chấm than)" +
                    "\n- Dấu câu thừa: !!!, $$$, ???" +
                    "\n- Viết hoa toàn bộ (trừ tên thương hiệu)" +
                    "\n- Tuyên bố sai lệch hoặc phóng đại" +
                    "\n\nTrả về dạng JSON array gồm " + numberOfVariations + " đối tượng với keys: headline, description, primaryText, callToAction.";
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-sonnet-20240229");
        requestBody.put("max_tokens", 4000);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", fullPrompt)));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            log.debug("Anthropic API Response: {}", response);
            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
                if (!contentList.isEmpty() && contentList.get(0).containsKey("text")) {
                    String contentText = (String) contentList.get(0).get("text");
                    log.debug("Received content text from Anthropic: {}", contentText);
                    
                    try {
                        List<AdContent> parsedContents = objectMapper.readValue(contentText, new TypeReference<List<AdContent>>() {});
                        for (AdContent adContent : parsedContents) {
                            adContent.setAiProvider(AdContent.AIProvider.ANTHROPIC);
                            adContent.setIsSelected(false);
                            adContent.setCallToAction(callToAction);
                            adContent.setCta(callToAction);
                            adContents.add(adContent);
                            if (adContents.size() >= numberOfVariations) break; }
                        log.info("Successfully parsed {} ad contents from Anthropic.", adContents.size());
                    } catch (Exception e) {
                        log.error("Failed to parse JSON response from Anthropic: {}", contentText, e);
                    };
                }
            }
            
            if (adContents.isEmpty()) {
                log.warn("Failed to parse valid ad content from Anthropic response. Response: {}", response);
            }
        } catch (Exception e) {
            log.error("Error calling Anthropic API: {}", e.getMessage(), e);
        }

        while (adContents.size() < numberOfVariations) {
            log.warn("Generated/Parsed only {} valid ad contents from Anthropic, filling remaining {} with mock data.", adContents.size(), numberOfVariations - adContents.size());
            adContents.addAll(generateMockAdContents(prompt, 1, callToAction));
        }
        return adContents;
    }
    public String generateImage(String prompt) {
        log.warn("Anthropic Claude does not support image generation via this API.");
        return "/img/placeholder.png";
    }

    public String getProviderName() {
        return "Anthropic Claude";
    }

    public boolean supportsImageGeneration() {
        return false;
    }

    @Override
    public String enhanceImage(String imagePath, String enhancementType, java.util.Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by Anthropic");
    }

    @Override
    public String getName() {
        return "Anthropic";
    }

    @Override
    public java.util.Set<com.fbadsautomation.model.Capability> getCapabilities() {
        return java.util.EnumSet.of(com.fbadsautomation.model.Capability.TEXT_GENERATION);
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations, com.fbadsautomation.model.FacebookCTA callToAction) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            adContent.setHeadline("Claude: Tiêu đề mẫu #" + (i + 1) + " cho: " + prompt);
            adContent.setDescription("Mô tả ngắn gọn cho mẫu quảng cáo Claude #" + (i + 1));
            adContent.setPrimaryText("Đây là nội dung chính của mẫu quảng cáo Claude #" + (i + 1) + ". Nội dung này sẽ mô tả chi tiết về sản phẩm hoặc dịch vụ được quảng cáo.");
            adContent.setCallToAction(callToAction);
            adContent.setCta(callToAction);
            adContent.setImageUrl("/img/placeholder.png");
            adContent.setAiProvider(AdContent.AIProvider.ANTHROPIC);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
    }
}