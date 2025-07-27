package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service

public class GeminiProvider implements AIProvider {
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
            fullPrompt = "You are a professional ad copywriter. " +
                    "Please generate " + numberOfVariations + " different ad variations for: \"" + prompt + "\". " +
                    "Each ad should include: headline, description, primaryText, and callToAction. " +
                    "Return the result as a JSON list of " + numberOfVariations + " objects, each with keys: headline, description, primaryText, callToAction.";
        } else {
            fullPrompt = "Bạn là một chuyên gia viết quảng cáo. " +
                    "Hãy tạo " + numberOfVariations + " mẫu quảng cáo khác nhau cho: \"" + prompt + "\". " +
                    "Mỗi mẫu quảng cáo cần có tiêu đề (headline), mô tả ngắn (description), nội dung chính (primaryText) và lời kêu gọi hành động (callToAction). " +
                    "Trả về kết quả dưới dạng một danh sách JSON gồm " + numberOfVariations + " đối tượng, mỗi đối tượng có các trường: headline, description, primaryText, callToAction.";
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