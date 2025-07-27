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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service

public class OpenAIProvider implements AIProvider {
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
        String systemPrompt = "You are an elite advertising copywriter with 20+ years of experience creating high-converting Facebook ads for major brands. " +
                "Your ads consistently achieve 3-5x higher CTR than industry averages because you understand consumer psychology and emotional triggers. " +
                "Create a professional, persuasive advertisement with these components:\n\n" +
                "1. Headline: Attention-grabbing, concise (5-9 words), creates curiosity or highlights key benefit\n" +
                "2. Description: Compelling secondary message (1-2 sentences) that supports the headline\n" +
                "3. Primary Text: Engaging body copy that follows AIDA framework (Attention, Interest, Desire, Action), " +
                "uses social proof, addresses pain points, and emphasizes benefits over features\n" +
                "4. Call to Action: The Call to Action is fixed and must be: " + callToAction.name() + "\n\n" +
                "Incorporate these proven techniques:\n" +
                "- Use 'you' and 'your' language to make it personal\n" +
                "- Include specific numbers or statistics when relevant\n" +
                "- Create a sense of exclusivity or scarcity when appropriate\n" +
                "- Use power words that evoke emotion\n" +
                "- Maintain brand voice: professional, authoritative, yet conversational\n\n" +
                "Return the result strictly as a JSON object with keys: \"headline\", \"description\", \"primaryText\". The callToAction field should NOT be in the response.";
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
