package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.model.Capability;
import com.fbadsautomation.service.MinIOStorageService;
import com.fbadsautomation.util.ByteArrayMultipartFile;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

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

    @Autowired(required = false)
    private MinIOStorageService minIOStorageService;
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

        // Phase 4: Use pre-built CoT prompt directly (no system prompt, no language wrapping)
        // The prompt parameter already contains the complete 6-stage Chain-of-Thought prompt
        // from ChainOfThoughtPromptBuilder with all instructions, constraints, and language requirements
        log.debug("[Phase 4] Using unified CoT prompt (length: {} chars)", prompt.length());

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt); // Complete CoT prompt - no modifications needed

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(userMessage); // Only user message, no system message
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

    /**
     * Generate simple text completion for prompt enhancement
     * Returns plain text without structured JSON parsing
     */
    @Override
    public String generateTextCompletion(String prompt, String systemPrompt, Integer maxTokens) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key is missing for text completion");
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Build messages array
            List<Map<String, Object>> messages = new ArrayList<>();

            // Add system prompt if provided
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }

            // Add user prompt
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", maxTokens != null ? maxTokens : 300);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling OpenAI Text Completion API for prompt enhancement");

            Map<String, Object> responseBody = restTemplate.postForObject(textApiUrl, request, Map.class);

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String content = (String) message.get("content");

                    log.info("OpenAI text completion successful, length: {}", content != null ? content.length() : 0);
                    return content != null ? content.trim() : null;
                }
            }

            log.warn("OpenAI text completion returned empty response");
            return null;

        } catch (HttpClientErrorException e) {
            log.error("HTTP Error calling OpenAI for text completion: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Error calling OpenAI for text completion: {}", e.getMessage(), e);
            return null;
        }
    }

    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) {
            log.warn("OpenAI image generation not supported (likely missing API key).");
            return "/img/placeholder.png";
        }
        // Use standardized prompt (already enhanced by ImagePromptService)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-3");  // Upgraded from dall-e-2 for better quality and prompt adherence
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");
        requestBody.put("quality", "standard");  // DALL-E 3 supports "standard" or "hd"
        requestBody.put("response_format", "url");
        log.debug("Calling OpenAI Image API at: {} with standardized prompt: {}", imageApiUrl, prompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(imageApiUrl, request, Map.class);
        if (response != null && response.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (!data.isEmpty() && data.get(0).containsKey("url")) {
                String imageUrl = (String) data.get(0).get("url");
                log.info("Successfully received image URL from OpenAI: {}", imageUrl);

                // Download image from OpenAI
                try (InputStream in = new URL(imageUrl).openStream()) {
                    byte[] imageBytes = in.readAllBytes();
                    String filename = UUID.randomUUID().toString() + ".png";

                    // Try MinIO first
                    if (minIOStorageService != null) {
                        try {
                            log.debug("Uploading OpenAI image to MinIO: {}", filename);
                            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                                imageBytes, "image", "image/png", filename
                            );
                            String storedFilename = minIOStorageService.uploadFile(multipartFile);
                            String publicUrl = minIOStorageService.getFileUrl(storedFilename);
                            log.info("OpenAI image uploaded to MinIO successfully: {}", publicUrl);
                            return publicUrl;
                        } catch (Exception minioError) {
                            log.warn("MinIO upload failed, falling back to local storage: {}", minioError.getMessage());
                        }
                    }

                    // Fallback to local filesystem
                    Path uploadPath = Paths.get(imageStorageLocation);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    Path filePath = uploadPath.resolve(filename);
                    Files.write(filePath, imageBytes);

                    String localUrl = "/api/images/" + filename;
                    log.info("Saved OpenAI image to local: {} | URL: {}", filePath, localUrl);
                    return localUrl;

                } catch (Exception ex) {
                    log.error("Failed to save OpenAI image: {}", ex.getMessage(), ex);
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

    @Override
    public String enhanceImage(String imagePath, String enhancementType, Map<String, Object> params) throws Exception {
        if (!supportsImageGeneration()) {
            throw new UnsupportedOperationException("Image enhancement not supported without API key.");
        }
        String editsUrl = "https://api.openai.com/v1/images/edits";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        String enhancementPrompt = switch (enhancementType) {
            case "upscale" -> "Upscale this image to higher resolution, maintain details.";
            case "remove_background" -> "Remove the background from this image, keep subject intact.";
            case "color_correct" -> "Apply color correction and enhancement to this image.";
            default -> throw new IllegalArgumentException("Unsupported enhancement type: " + enhancementType);
        };
        if (params != null && !params.isEmpty()) {
            enhancementPrompt += " Additional instructions: " + params.toString();
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(imagePath));
        body.add("prompt", enhancementPrompt);
        body.add("n", 1);
        body.add("size", "1024x1024");
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(editsUrl, requestEntity, Map.class);
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty() && data.get(0).containsKey("url")) {
                    String enhancedUrl = (String) data.get(0).get("url");
                    try (InputStream in = new URL(enhancedUrl).openStream()) {
                        String filename = UUID.randomUUID().toString() + ".png";
                        Path uploadPath = Paths.get(imageStorageLocation);
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }
                        Path filePath = uploadPath.resolve(filename);
                        Files.copy(in, filePath);
                        return "/api/images/" + filename;
                    }
                }
            }
            throw new Exception("Failed to get enhanced image URL from OpenAI.");
        } catch (Exception e) {
            log.error("Error enhancing image with OpenAI: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Mock Ad Content Generation (Corrected return type)
    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations, FacebookCTA callToAction) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            // Ensure headline stays within 40 character limit
            String shortPrompt = prompt.length() > 20 ? prompt.substring(0, 17) + "..." : prompt;
            adContent.setHeadline("Mẫu " + (i + 1) + ": " + shortPrompt); // Max ~30 chars
            adContent.setDescription("Nội dung quảng cáo được tạo bởi OpenAI, phiên bản " + (i + 1)); // Max 125 chars
            adContent.setPrimaryText("Mẫu quảng cáo số " + (i + 1) + " cho: " + prompt + ". " +
                "Nội dung được tạo tự động bởi OpenAI với chất lượng cao, tối ưu hóa cho Facebook Ads."); // Within 1000 chars
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
        mockContent.setHeadline("Quảng cáo mẫu"); // Only 14 chars - safe!
        mockContent.setDescription("Nội dung được tạo tự động"); // 27 chars - safe!
        mockContent.setPrimaryText("Nội dung chính cho quảng cáo: " + prompt);
        mockContent.setCallToAction(callToAction);
        mockContent.setCta(callToAction);
        return mockContent;
    }

    @Override
    @Async("aiProcessingExecutor")
    public CompletableFuture<List<AdContent>> generateAdContentAsync(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
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
