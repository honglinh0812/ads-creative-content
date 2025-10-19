package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Capability;
import com.fbadsautomation.model.FacebookCTA;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

@Service
public class FalAiProvider implements AIProvider {
    
    private static final Logger log = LoggerFactory.getLogger(FalAiProvider.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${app.image.storage.location}")
    private String imageStorageLocation;

    public FalAiProvider(
        RestTemplate restTemplate,
        @Value("${fal.ai.api.key}") String apiKey,
        @Value("${fal.ai.api.url:https://fal.run/fal-ai/fast-sdxl}") String apiUrl) {
            this.restTemplate = restTemplate;
            this.apiKey = apiKey;
            this.apiUrl = apiUrl;
            
            log.info("Using Fal.ai API URL: {}", this.apiUrl);
    }

    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        // Fal.ai is primarily for image generation, so we return mock text content
        log.warn("Fal.ai does not support text generation. Returning mock data.");
        List<AdContent> adContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent mockContent = new AdContent();
            // Ensure headline stays within 40 character limit
            String shortPrompt = prompt.length() > 25 ? prompt.substring(0, 22) + "..." : prompt;
            mockContent.setHeadline("Fal.ai: " + shortPrompt); // Max ~35 chars
            mockContent.setDescription("Hình ảnh được tạo bởi Fal.ai cho quảng cáo"); // Max 125 chars
            mockContent.setPrimaryText("Fal.ai mẫu cho: " + prompt + ". " +
                "Nội dung được tạo tự động với hình ảnh chất lượng cao, tối ưu cho Facebook Ads."); // Within 1000 chars
            mockContent.setCallToAction(callToAction != null ? callToAction : FacebookCTA.LEARN_MORE);
            mockContent.setCta(callToAction != null ? callToAction : FacebookCTA.LEARN_MORE);
            mockContent.setAiProvider(AdContent.AIProvider.FAL_AI);
            mockContent.setImageUrl("/img/placeholder.png");
            adContents.add(mockContent);
        }
        return adContents;
    }

    @Override
    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) {
            log.warn("Fal.ai image generation not supported (likely missing API key).");
            return "/img/placeholder.png";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + apiKey);

        // Enhanced prompt for better commercial image quality
        String enhancedPrompt = "Professional commercial advertisement image for: " + prompt + 
            ". High quality, studio lighting, product photography, marketing style, clean background, " +
            "vibrant colors, professional composition, 4k resolution, commercial photography";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", enhancedPrompt);
        requestBody.put("image_size", "1024x1024");
        requestBody.put("num_inference_steps", 50);
        requestBody.put("guidance_scale", 7.5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Fal.ai API at: {} with prompt: {}", apiUrl, enhancedPrompt);

        try {
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            if (response != null && response.containsKey("images")) {
                List<Map<String, Object>> images = (List<Map<String, Object>>) response.get("images");
                if (!images.isEmpty() && images.get(0).containsKey("url")) {
                    String imageUrl = (String) images.get(0).get("url");
                    log.info("Successfully received image URL from Fal.ai: {}", imageUrl);
                    try (InputStream in = new URL(imageUrl).openStream()) {
                        String filename = UUID.randomUUID().toString() + ".png";
                        Path uploadPath = Paths.get(imageStorageLocation);
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }
                        Path filePath = uploadPath.resolve(filename);
                        Files.copy(in, filePath);
                        String localUrl = "/api/images/" + filename;
                        log.info("Saved Fal.ai image to local: {} | URL: {}", filePath, localUrl);
                        return localUrl;
                    } catch (Exception ex) {
                        log.error("Failed to save Fal.ai image to local: {}", ex.getMessage(), ex);
                        return "/img/placeholder.png";
                    }
                }
            } else {
                log.error("Fal.ai API call failed with response: {}", response);
            }
        } catch (Exception e) {
            log.error("Error calling Fal.ai API: {}", e.getMessage(), e);
        }

        return "/img/placeholder.png";
    }

    @Override
    public String getName() {
        return "Fal.ai";
    }

    @Override
    public String getProviderName() {
        return "Fal.ai";
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public Set<Capability> getCapabilities() {
        return EnumSet.of(Capability.IMAGE_GENERATION, Capability.REAL_TIME);
    }

    public boolean supportsImageGeneration() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String enhanceImage(String imageUrl, String enhancementType, Map<String, Object> params) {
        String endpoint = getEnhancementEndpoint(enhancementType);
        if (endpoint == null) {
            throw new IllegalArgumentException("Unsupported enhancement type: " + enhancementType);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image_url", imageUrl);
        requestBody.putAll(params);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Fal.ai enhancement API at: {} with type: {}", endpoint, enhancementType);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);
            if (response != null && response.containsKey("image")) {
                Map<String, Object> imageData = (Map<String, Object>) response.get("image");
                String enhancedUrl = (String) imageData.get("url");
                // Save to local storage similar to generateImage
                try (InputStream in = new URL(enhancedUrl).openStream()) {
                    String filename = UUID.randomUUID().toString() + ".png";
                    Path uploadPath = Paths.get(imageStorageLocation);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    Path filePath = uploadPath.resolve(filename);
                    Files.copy(in, filePath);
                    String localUrl = "/api/images/" + filename;
                    log.info("Saved enhanced image to local: {} | URL: {}", filePath, localUrl);
                    return localUrl;
                }
            }
        } catch (Exception e) {
            log.error("Error calling Fal.ai enhancement API: {}", e.getMessage(), e);
        }
        return imageUrl; // Fallback to original
    }

    private String getEnhancementEndpoint(String type) {
        switch (type) {
            case "upscale": return "https://fal.run/fal-ai/esrgan";
            case "style_transfer": return "https://fal.run/fal-ai/flux/dev/image-to-image";
            default: return null;
        }
    }

    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            // Ensure headline stays within 40 character limit
            String shortPrompt = prompt.length() > 20 ? prompt.substring(0, 17) + "..." : prompt;
            adContent.setHeadline("Fal " + (i + 1) + ": " + shortPrompt); // Max ~30 chars
            adContent.setDescription("Nội dung quảng cáo với hình ảnh Fal.ai, mẫu " + (i + 1)); // Max 125 chars
            adContent.setPrimaryText("Fal mẫu " + (i + 1) + " cho: " + prompt + ". " +
                "Nội dung được tạo tự động bởi Fal.ai với hình ảnh chất lượng cao, tối ưu cho Facebook Ads."); // Within 1000 chars
            adContent.setCallToAction(FacebookCTA.LEARN_MORE);
            adContent.setCta(FacebookCTA.LEARN_MORE);
            adContent.setImageUrl("/img/placeholder.png");
            adContent.setAiProvider(AdContent.AIProvider.FAL_AI);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
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
