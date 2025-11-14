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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

@Service

public class AnthropicProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean providerUnavailable = false;
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
        if (!isProviderAvailable()) {
            log.warn("Anthropic provider unavailable (missing API key or disabled).");
            throw new IllegalStateException("Anthropic provider unavailable");
        }

        List<AdContent> adContents = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        // Phase 4: Use pre-built CoT prompt directly (no template selection, no language wrapping)
        // The prompt parameter already contains the complete 6-stage Chain-of-Thought prompt
        // from ChainOfThoughtPromptBuilder with all instructions, constraints, and language requirements
        log.debug("[Phase 4] Using unified CoT prompt (length: {} chars)", prompt.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-sonnet-20240229");
        requestBody.put("max_tokens", 4000);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt))); // Complete CoT prompt - no modifications
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
                throw new IllegalStateException("Failed to parse valid ad content from Anthropic response");
            }

            return adContents;
        } catch (HttpClientErrorException e) {
            handleAnthropicError(e);
            throw new IllegalStateException("Anthropic API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Error calling Anthropic API: {}", e.getMessage(), e);
            throw new IllegalStateException("Anthropic API error", e);
        }
    }

    /**
     * Generate simple text completion for prompt enhancement
     * Returns plain text without structured JSON parsing
     */
    @Override
    public String generateTextCompletion(String prompt, String systemPrompt, Integer maxTokens) {
        if (!isProviderAvailable()) {
            log.warn("Anthropic provider unavailable for text completion");
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-3-sonnet-20240229");
            requestBody.put("max_tokens", maxTokens != null ? maxTokens : 300);

            // Combine system prompt and user prompt
            String combinedPrompt = "";
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                combinedPrompt = systemPrompt + "\n\n" + prompt;
            } else {
                combinedPrompt = prompt;
            }

            requestBody.put("messages", List.of(Map.of("role", "user", "content", combinedPrompt)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Anthropic Text Completion API for prompt enhancement");

            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
                if (!contentList.isEmpty() && contentList.get(0).containsKey("text")) {
                    String text = (String) contentList.get(0).get("text");
                    log.info("Anthropic text completion successful, length: {}", text != null ? text.length() : 0);
                    return text != null ? text.trim() : null;
                }
            }

            log.warn("Anthropic text completion returned empty response");
            return null;

        } catch (HttpClientErrorException e) {
            handleAnthropicError(e);
            return null;
        } catch (Exception e) {
            log.error("Error calling Anthropic for text completion: {}", e.getMessage(), e);
            return null;
        }
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
        if (!isProviderAvailable()) {
            return java.util.EnumSet.noneOf(com.fbadsautomation.model.Capability.class);
        }
        return java.util.EnumSet.of(com.fbadsautomation.model.Capability.TEXT_GENERATION);
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    @Async("aiProcessingExecutor")
    public CompletableFuture<List<AdContent>> generateAdContentAsync(String prompt, int numberOfVariations, String language, com.fbadsautomation.model.FacebookCTA callToAction) {
        try {
            List<AdContent> result = generateAdContent(prompt, numberOfVariations, language, callToAction);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error in async ad content generation", e);
            CompletableFuture<List<AdContent>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private boolean isProviderAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !providerUnavailable;
    }

    private void handleAnthropicError(HttpClientErrorException e) {
        log.error("Anthropic API returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED ||
            e.getStatusCode() == HttpStatus.FORBIDDEN ||
            e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ||
            e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            providerUnavailable = true;
            log.warn("Anthropic provider disabled due to API status {}. It will remain hidden until service access is restored.", e.getStatusCode());
        }
    }

    @Override
    @Async("imageProcessingExecutor")
    public CompletableFuture<String> generateImageAsync(String prompt) {
        try {
            String result = generateImage(prompt);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error in async image generation", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    @Async("imageProcessingExecutor")
    public CompletableFuture<String> enhanceImageAsync(String imagePath, String enhancementType, java.util.Map<String, Object> params) {
        try {
            String result = enhanceImage(imagePath, enhancementType, params);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error in async image enhancement", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
