package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.model.Capability;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service

public class OpenAIProvider implements AIProvider {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String textApiUrl;
    private final String imageApiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
        // For parsing JSON;
    @Value("${app.image.storage.location:uploads/images}")
    private String imageStorageLocation;
    public OpenAIProvider(
            RestTemplate restTemplate,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.text-api-url:https://api.openai.com/v1/chat/completions}") String textApiUrl,
            @Value("${ai.openai.image-api-url:https://api.openai.com/v1/images/generations}") String imageApiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.textApiUrl = textApiUrl;
        this.imageApiUrl = imageApiUrl;
        log.info("Using OpenAI Text API URL: {}", this.textApiUrl);
        log.info("Using OpenAI Image API URL: {}", this.imageApiUrl);
    }
    @Override
    public Set<Capability> getCapabilities() {
        Set<Capability> capabilities = EnumSet.noneOf(Capability.class);
        capabilities.add(Capability.TEXT_GENERATION);
        capabilities.add(Capability.IMAGE_GENERATION);
        return capabilities;
    }
    @Override
    public String getApiUrl() {
        return textApiUrl;
    }
    @Override
    public String getName() {
        return "OpenAI";
    }
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    // Corrected return type to List<AdContent>
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        List<AdContent> adContents = new ArrayList<>();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key is missing for text generation. Returning mock data.");
            return generateMockAdContents(prompt, numberOfVariations, callToAction);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        String systemPrompt = "You are an elite Facebook advertising copywriter with 20+ years of experience creating compliant, high-converting ads. " +
                "Your ads consistently achieve 3-5x higher CTR while maintaining 100% Facebook policy compliance. " +
                "\n\nCRITICAL FACEBOOK REQUIREMENTS (MUST FOLLOW):" +
                "\n1. CHARACTER LIMITS (STRICT):" +
                "\n   - Headline: Maximum 40 characters (including spaces)" +
                "\n   - Description: Maximum 30 characters (including spaces)" +
                "\n   - Primary Text: Maximum 125 characters (including spaces)" +
                "\n\n2. CONTENT QUALITY REQUIREMENTS:" +
                "\n   - Each field must contain at least 3 meaningful words" +
                "\n   - Use clear, professional language" +
                "\n   - Avoid ALL CAPS (except for brand names if necessary)" +
                "\n   - No repetitive or meaningless text" +
                "\n\n3. FACEBOOK POLICY COMPLIANCE (ZERO TOLERANCE):" +
                "\n   - NO prohibited words: 'hate', 'violence', 'drugs', 'miracle', 'guaranteed results', 'cure', 'instant'" +
                "\n   - NO excessive punctuation: 'FREE!!!', 'ACT NOW!!!', '$$$$'" +
                "\n   - NO misleading claims or exaggerated promises" +
                "\n   - NO personal attributes targeting (age, race, religion, etc.)" +
                "\n   - NO adult content, gambling, or controversial topics" +
                "\n\n4. BEST PRACTICES:" +
                "\n   - Use 'you' and 'your' for personalization" +
                "\n   - Include specific, verifiable benefits" +
                "\n   - Create urgency without being pushy" +
                "\n   - Maintain professional, conversational tone" +
                "\n   - Focus on value proposition and benefits" +
                "\n\n5. CALL TO ACTION: Must be exactly: " + callToAction.name() +
                "\n\nReturn ONLY valid JSON with keys: \"headline\", \"description\", \"primaryText\". " +
                "Double-check character limits before responding. If any field exceeds limits, shorten it immediately.";
        Map<String, Object> message1 = new HashMap<>();
        message1.put("role", "system");
        message1.put("content", systemPrompt);
        Map<String, Object> message2 = new HashMap<>();
        message2.put("role", "user");
        String langInstruction = "vi".equalsIgnoreCase(language)
                ? "Viết bằng tiếng Việt.\n"
                : "Write in English.\n";
        message2.put("content", langInstruction + "Generate professional, high-converting Facebook ad content for: " + prompt);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message1);
        messages.add(message2);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo"); // Consider making model configurable
        requestBody.put("messages", messages);
        requestBody.put("n", numberOfVariations);
        requestBody.put("temperature", 0.7); // Slightly lower temperature for more focused results
        requestBody.put("max_tokens", 1000); // Set explicit token limit to prevent truncation
        // Ensure response format is JSON
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        requestBody.put("response_format", responseFormat);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling OpenAI Text API at: {} with prompt: {}", textApiUrl, prompt);
        try {
            Map<String, Object> responseBody = restTemplate.postForObject(textApiUrl, request, Map.class);
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                for (Map<String, Object> choice : choices) {
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    log.info("Raw OpenAI response content: {}", content);
                    try {
                        JsonNode rootNode = objectMapper.readTree(content);
                        AdContent adContent = new AdContent();
                        adContent.setHeadline(rootNode.path("headline").asText());
                        adContent.setDescription(rootNode.path("description").asText());
                        adContent.setPrimaryText(rootNode.path("primaryText").asText());
                        adContent.setCallToAction(callToAction); // Gán trực tiếp CTA từ tham số
                        adContent.setCta(callToAction); // Gán trực tiếp CTA từ tham số
                        adContent.setAiProvider(AdContent.AIProvider.OPENAI);
                        adContent.setIsSelected(false); // Default value
                        adContents.add(adContent);
                        log.info("Parsed OpenAI Ad Content: {}", adContent);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse JSON content from OpenAI: {}", content, e);
                        // Fallback to mock data if parsing fails for one variation
                        adContents.add(generateMockAdContent(prompt, callToAction));
                    };
                }
            }
    } catch (HttpClientErrorException e) {
            log.error("HTTP Error calling OpenAI Text API: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error calling OpenAI Text API: {}", e.getMessage(), e);
        // Fill with mock data if not enough variations generated
        while (adContents.size() < numberOfVariations) {
            log.warn("Generated only {} valid ad contents from OpenAI, filling remaining {} with mock data.", adContents.size(), numberOfVariations - adContents.size());
            adContents.addAll(generateMockAdContents(prompt, numberOfVariations, callToAction));
        }
    }
        return adContents;
    }

    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) {
            log.warn("OpenAI image generation not supported (likely missing API key).");
            return "/img/placeholder.png";
        }
        String enhancedPrompt = "Professional, high-quality advertisement for: " + prompt +
            ". Clear product focus, studio lighting, crisp details, vibrant colors, commercial photography style, 8k resolution, professional marketing image";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-2");
        requestBody.put("prompt", enhancedPrompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");
        requestBody.put("response_format", "url");
        log.debug("Calling OpenAI Image API at: {} with prompt: {}", imageApiUrl, enhancedPrompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(imageApiUrl, request, Map.class);
        if (response != null && response.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (!data.isEmpty() && data.get(0).containsKey("url")) {
                String imageUrl = (String) data.get(0).get("url");
                log.info("Successfully received image URL from OpenAI: {}", imageUrl);
                // Tải ảnh về local
                try (InputStream in = new URL(imageUrl).openStream()) {
                    String filename = UUID.randomUUID().toString() + ".png";
                    Path uploadPath = Paths.get(imageStorageLocation);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    Path filePath = uploadPath.resolve(filename);
                    Files.copy(in, filePath);
                    String localUrl = "/api/images/" + filename;
                    log.info("Saved OpenAI image to local: {} | URL: {}", filePath, localUrl);
                    return localUrl;
                } catch (Exception ex) {
                    log.error("Failed to save OpenAI image to local: {}", ex.getMessage(), ex);
                    return "/img/placeholder.png";
                }
    }
        } else {
            log.warn("Received unexpected response format from OpenAI Image API: {}", response);
        }
        return "/img/placeholder.png";
    }

    public boolean supportsImageGeneration() {
        // Image generation is supported only if an API key is provided
        return apiKey != null && !apiKey.isEmpty();
    }

    // Mock Ad Content Generation (Corrected return type)
    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations, FacebookCTA callToAction) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            adContent.setHeadline("OpenAI: Tiêu đề mẫu #" + (i + 1) + " cho: " + prompt);
            adContent.setDescription("Mô tả ngắn gọn cho mẫu quảng cáo OpenAI #" + (i + 1));
            adContent.setPrimaryText("Đây là nội dung chính của mẫu quảng cáo OpenAI #" + (i + 1) + ". Nội dung này sẽ mô tả chi tiết về sản phẩm hoặc dịch vụ được quảng cáo.");
            adContent.setCallToAction(callToAction);
            adContent.setCta(callToAction);
            adContent.setImageUrl("/img/placeholder.png"); // Use local placeholder
            adContent.setAiProvider(AdContent.AIProvider.OPENAI);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
    }
    private AdContent generateMockAdContent(String prompt, FacebookCTA callToAction) {
        AdContent mockContent = new AdContent();
        mockContent.setHeadline("Mock Fallback Headline");
        mockContent.setDescription("Fallback description due to parsing error.");
        mockContent.setPrimaryText("Fallback primary text for prompt: " + prompt);
        mockContent.setCallToAction(callToAction);
        mockContent.setCta(callToAction);
        return mockContent;
    }
}
