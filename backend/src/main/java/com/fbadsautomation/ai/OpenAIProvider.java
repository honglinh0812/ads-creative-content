package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIProvider implements AIProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String textApiUrl;
    private final String imageApiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON

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
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        if (apiKey != null && !apiKey.isEmpty()) {
            capabilities.add("TEXT");
            capabilities.add("IMAGE");
        }
        return capabilities;
    }

    // Corrected return type to List<AdContent>
    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language) {
        List<AdContent> adContents = new ArrayList<>();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key is missing for text generation. Returning mock data.");
            return generateMockAdContents(prompt, numberOfVariations);
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
                "4. Call to Action: Clear, action-oriented phrase that creates urgency and reduces friction\n\n" +
                "Incorporate these proven techniques:\n" +
                "- Use 'you' and 'your' language to make it personal\n" +
                "- Include specific numbers or statistics when relevant\n" +
                "- Create a sense of exclusivity or scarcity when appropriate\n" +
                "- Use power words that evoke emotion\n" +
                "- Maintain brand voice: professional, authoritative, yet conversational\n\n" +
                "Return the result strictly as a JSON object with keys: \"headline\", \"description\", \"primaryText\", \"callToAction\".";

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
        // Ensure response format is JSON
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        requestBody.put("response_format", responseFormat);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling OpenAI Text API at: {} with prompt: {}", textApiUrl, prompt);

        try {
            Map<String, Object> response = restTemplate.postForObject(textApiUrl, request, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

                for (Map<String, Object> choice : choices) {
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    log.info("Raw OpenAI response content: {}", content);

                    try {
                        // Parse JSON string directly into AdContent object
                        AdContent adContent = objectMapper.readValue(content, AdContent.class);
                        adContent.setAiProvider(AdContent.AIProvider.OPENAI);
                        adContent.setIsSelected(false); // Default value
                        
                        adContents.add(adContent);
                        log.info("Parsed OpenAI Ad Content: {}", adContent);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse JSON response from OpenAI: {}", content, e);
                        // Add mock content if parsing fails for this choice
                        adContents.addAll(generateMockAdContents("Failed to parse response", 1));
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP Error calling OpenAI Text API: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return generateMockAdContents(prompt, numberOfVariations);
        } catch (Exception e) {
            log.error("Error calling OpenAI Text API: {}", e.getMessage(), e);
            return generateMockAdContents(prompt, numberOfVariations);
        }

        // Fill with mock data if not enough variations generated
        while (adContents.size() < numberOfVariations) {
            log.warn("Generated only {} valid ad contents from OpenAI, filling remaining {} with mock data.", adContents.size(), numberOfVariations - adContents.size());
            adContents.addAll(generateMockAdContents(prompt, 1));
        }

        return adContents;
    }

    @Override
    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) { // Check capability first
            log.warn("OpenAI image generation not supported (likely missing API key).");
            return "/img/placeholder.png";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Enhanced prompt for better image quality and accuracy
        String enhancedPrompt = "Professional, high-quality advertisement for: " + prompt + 
            ". Clear product focus, studio lighting, crisp details, vibrant colors, commercial photography style, 8k resolution, professional marketing image";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-2"); // Upgrade to dall-e-3 for better quality
        requestBody.put("prompt", enhancedPrompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024"); // Valid for dall-e-2 and dall-e-3
        //requestBody.put("quality", "hd"); // Request HD quality for dall-e-3
        requestBody.put("response_format", "url"); // Request URL directly

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling OpenAI Image API at: {} with prompt: {}", imageApiUrl, enhancedPrompt);

        try {
            Map<String, Object> response = restTemplate.postForObject(imageApiUrl, request, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty() && data.get(0).containsKey("url")) {
                    String imageUrl = (String) data.get(0).get("url");
                    log.info("Successfully received image URL from OpenAI: {}", imageUrl);
                    return imageUrl;
                }
            }
            log.warn("Received unexpected response format from OpenAI Image API: {}", response);
        } catch (HttpClientErrorException e) {
            // Log the specific error response from OpenAI
            log.error("HTTP Error calling OpenAI Image API: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            // If dall-e-3 fails, try falling back to dall-e-2
            try {
                log.info("Falling back to dall-e-2 model");
                requestBody.put("model", "dall-e-2");
                requestBody.remove("quality"); // Remove HD quality parameter not supported by dall-e-2
                
                HttpEntity<Map<String, Object>> fallbackRequest = new HttpEntity<>(requestBody, headers);
                Map<String, Object> fallbackResponse = restTemplate.postForObject(imageApiUrl, fallbackRequest, Map.class);
                
                if (fallbackResponse != null && fallbackResponse.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) fallbackResponse.get("data");
                    if (!data.isEmpty() && data.get(0).containsKey("url")) {
                        String imageUrl = (String) data.get(0).get("url");
                        log.info("Successfully received image URL from OpenAI (dall-e-2 fallback): {}", imageUrl);
                        return imageUrl;
                    }
                }
            } catch (Exception fallbackEx) {
                log.error("Error in fallback image generation: {}", fallbackEx.getMessage(), fallbackEx);
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI Image API: {}", e.getMessage(), e);
        }

        // Fallback to placeholder image on any error
        return "/img/placeholder.png";
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean supportsImageGeneration() {
        // Image generation is supported only if an API key is provided
        return apiKey != null && !apiKey.isEmpty();
    }

    // Mock Ad Content Generation (Corrected return type)
    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            adContent.setHeadline("OpenAI: Tiêu đề mẫu #" + (i + 1) + " cho: " + prompt);
            adContent.setDescription("Mô tả ngắn gọn cho mẫu quảng cáo OpenAI #" + (i + 1));
            adContent.setPrimaryText("Đây là nội dung chính của mẫu quảng cáo OpenAI #" + (i + 1) + ". Nội dung này sẽ mô tả chi tiết về sản phẩm hoặc dịch vụ được quảng cáo.");
            adContent.setCallToAction("Tìm hiểu thêm #" + (i + 1));
            adContent.setImageUrl("/img/placeholder.png"); // Use local placeholder
            adContent.setAiProvider(AdContent.AIProvider.OPENAI);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
    }
}
