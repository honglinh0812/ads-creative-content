package com.fbadsautomation.integration.ai;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.AdContent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor

public class OpenAIContentGenerator implements AIContentGenerator {

    private final AIProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public AdContent generateAdContent(String prompt, AdContent.ContentType contentType) {
        try {
            log.info("Generating content with OpenAI for prompt: {}", prompt);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiProperties.getOpenaiModel());
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt(contentType));
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            
            requestBody.put("messages", new Object[]{systemMessage, userMessage});
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);
            
            // Set headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + aiProperties.getOpenaiApiKey());
            headers.set("Content-Type", "application/json");
            
            // Create HTTP entity
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // Make API call
            org.springframework.http.ResponseEntity<Map> response =
                restTemplate.postForEntity(API_URL, entity, Map.class);
            
            // Process response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                Object[] choices = (Object[]) responseBody.get("choices");
                if (choices.length > 0) {
                    Map<String, Object> choice = (Map<String, Object>) choices[0];
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    // Parse the content
                    return parseContent(content, contentType);
                }
            }
            
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate content with OpenAI");
        } catch (Exception e) {
            log.error("Error generating content with OpenAI", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error generating content with OpenAI: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    private String getSystemPrompt(AdContent.ContentType contentType) {
        return "You are an expert Facebook advertising copywriter. "
            + "Create compelling ad content for a Facebook " 
            + (contentType == AdContent.ContentType.TEXT ? "text ad" : "image ad")
            + ". Your response should be in JSON format with the following structure: "
            + "{\n"
            + "  \"primaryText\": \"The main text of the ad (no strict character limit)\",\n"
            + "  \"headline\": \"The headline (max 40 characters)\",\n"
            + "  \"description\": \"The description (max 125 characters)\"\n"
            + "}\n"
            + "Make sure the content is engaging, concise, and follows Facebook's best practices.";
    }
    
    private AdContent parseContent(String content, AdContent.ContentType contentType) {
        try {
            // Extract JSON from the response
            int startIndex = content.indexOf('{');
            int endIndex = content.lastIndexOf('}');
            if (startIndex >= 0 && endIndex >= 0) {
                String jsonContent = content.substring(startIndex, endIndex + 1);
                
                // Parse JSON using Jackson
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, String> contentMap = mapper.readValue(jsonContent, Map.class);
                
                AdContent adContent = new AdContent();
                adContent.setPrimaryText(contentMap.get("primaryText"));
                adContent.setHeadline(contentMap.get("headline"));
                adContent.setDescription(contentMap.get("description"));
                adContent.setContentType(contentType);
                adContent.setAiProvider(AdContent.AIProvider.OPENAI);
                adContent.setIsSelected(false);
                
                return adContent;
            }
            
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse OpenAI response");
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error parsing OpenAI response: " + e.getMessage());
        }
    }
}
