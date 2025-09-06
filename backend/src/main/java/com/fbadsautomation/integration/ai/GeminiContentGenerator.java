package com.fbadsautomation.integration.ai;

import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.AdContent;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiContentGenerator implements AIContentGenerator {

    private static final Logger log = LoggerFactory.getLogger(GeminiContentGenerator.class);
    
    private final AIProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public GeminiContentGenerator(AIProperties aiProperties) {
        this.aiProperties = aiProperties;
    }
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override
    public AdContent generateAdContent(String prompt, AdContent.ContentType contentType) {
        try {
            log.info("Generating content with Gemini for prompt: {}", prompt);
            
            String url = API_URL + aiProperties.getGeminiModel() + ":generateContent?key=" + aiProperties.getGeminiApiKey();
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> contents = new HashMap<>();
            contents.put("role", "user");
            contents.put("parts", new Object[]{
                Map.of("text", getSystemPrompt(contentType) + "\n\n" + prompt)
            });
            
            requestBody.put("contents", new Object[]{contents});
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 500);
            requestBody.put("generationConfig", generationConfig);
            
            // Set headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Create HTTP entity
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // Make API call
            org.springframework.http.ResponseEntity<Map> response =
                restTemplate.postForEntity(url, entity, Map.class);
            
            // Process response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("candidates")) {
                Object[] candidates = (Object[]) responseBody.get("candidates");
                if (candidates.length > 0) {
                    Map<String, Object> candidate = (Map<String, Object>) candidates[0];
                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    Object[] parts = (Object[]) content.get("parts");
                    if (parts.length > 0) {
                        Map<String, Object> part = (Map<String, Object>) parts[0];
                        String text = (String) part.get("text");
                        // Parse the content
                        return parseContent(text, contentType);
                    }
                }
            }
            
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate content with Gemini");
        } catch (Exception e) {
            log.error("Error generating content with Gemini", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error generating content with Gemini: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Gemini";
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
                adContent.setAiProvider(AdContent.AIProvider.GEMINI);
                adContent.setIsSelected(false);
                
                return adContent;
            }
            
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse Gemini response");
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error parsing Gemini response: " + e.getMessage());
        }
    }
}
