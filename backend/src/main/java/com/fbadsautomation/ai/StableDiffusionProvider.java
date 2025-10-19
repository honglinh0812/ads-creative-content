package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Capability;
import com.fbadsautomation.model.FacebookCTA;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

@Service
public class StableDiffusionProvider implements AIProvider {
    
    private static final Logger log = LoggerFactory.getLogger(StableDiffusionProvider.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String imageApiUrl;
    private String imageStorageLocation;
    private final ObjectMapper objectMapper = new ObjectMapper();
        public StableDiffusionProvider(
            RestTemplate restTemplate,
            @Value("${ai.huggingface.api-key}") String apiKey,
            @Value("${ai.huggingface.image-api-url:https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0}") String imageApiUrl,
            @Value("${app.image.storage.location:uploads/images}") String imageStorageLocation) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.imageApiUrl = imageApiUrl;
        this.imageStorageLocation = imageStorageLocation;
        log.info("Using Stable Diffusion Image API URL: {}", this.imageApiUrl);
        
        // Ensure the image save directory exists
        try {
            Files.createDirectories(Paths.get(imageStorageLocation));
            log.info("Image save directory ensured at: {}", imageStorageLocation);
        } catch (IOException e) {
            log.error("Could not create image save directory: {}", imageStorageLocation, e);
        }
    }

    @Override
    public Set<Capability> getCapabilities() {
        Set<Capability> capabilities = EnumSet.of(Capability.IMAGE_GENERATION, Capability.REAL_TIME);
        return capabilities;
    }

    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        // Stable Diffusion is for image generation, so we return mock text content
        log.warn("Stable Diffusion does not support text generation. Returning mock data.");
        List<AdContent> adContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent mockContent = new AdContent();
            // Ensure headline stays within 40 character limit
            String shortPrompt = prompt.length() > 22 ? prompt.substring(0, 19) + "..." : prompt;
            mockContent.setHeadline("SD " + (i + 1) + ": " + shortPrompt); // Max ~30 chars
            mockContent.setDescription("Hình ảnh SD cho quảng cáo, mẫu " + (i + 1)); // Max 125 chars
            mockContent.setPrimaryText("Nội dung mẫu vì Stable Diffusion chuyên tạo hình ảnh. Quảng cáo: " + prompt); // Within 1000 chars
            mockContent.setCallToAction(callToAction);
            mockContent.setCta(callToAction);
            mockContent.setAiProvider(AdContent.AIProvider.STABLE_DIFFUSION);
            adContents.add(mockContent);
        }
        return adContents;
    }

    @Override
    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) {
            log.warn("Stable Diffusion image generation not supported (likely missing API key).");
            return "/img/placeholder.png";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        headers.set("Authorization", "Bearer " + apiKey);

        // Enhanced prompt for better commercial image quality
        String enhancedPrompt = "Professional commercial advertisement image for: " + prompt +
            ". High quality, studio lighting, product photography, marketing style, clean background, " +
            "vibrant colors, professional composition, 4k resolution, commercial photography";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", enhancedPrompt);
        requestBody.put("parameters", Map.of(
            "num_inference_steps", 50,
            "guidance_scale", 7.5,
            "width", 1024,
            "height", 1024
        ));
        requestBody.put("options", Map.of("wait_for_model", true));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Stable Diffusion Image API at: {} with prompt: {}", imageApiUrl, enhancedPrompt);

        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(imageApiUrl, HttpMethod.POST, request, byte[].class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                byte[] imageBytes = responseEntity.getBody();
                log.info("Successfully received image bytes from Stable Diffusion API.");

                // Save the image and return its URL
                String filename = UUID.randomUUID().toString() + ".png";
                Path uploadPath = Paths.get(imageStorageLocation);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(filename);
                try {
                    Files.write(filePath, imageBytes);
                    String imageUrl = "/api/images/" + filename; // Use the same URL pattern as configured
                    log.info("Saved generated image to: {} | URL: {}", filePath, imageUrl);
                    return imageUrl;
                } catch (IOException e) {
                    log.error("Failed to save generated image: {}", filePath, e);
                }
            } else {
                log.error("Stable Diffusion Image API call failed with status: {} and body: {}", 
                    responseEntity.getStatusCode(), responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error calling Stable Diffusion Image API: {}", e.getMessage(), e);
        }
        
        // Fallback to placeholder if API call or saving fails
        return "/img/placeholder.png";
    }

    public String enhanceImage(String imageUrl, String enhancementType, Map<String, Object> params) {
        if (!supportsImageGeneration()) {
            log.warn("Stable Diffusion enhancement not supported (likely missing API key)."); 
            return imageUrl;
        }
        String endpoint = getEnhancementEndpoint(enhancementType);
        if (endpoint == null) {
            throw new IllegalArgumentException("Unsupported enhancement type: " + enhancementType);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", imageUrl); // For upscale, inputs might need to be the image data or URL, but HF API typically expects data
        requestBody.putAll(params);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Stable Diffusion enhancement API at: {} with type: {}", endpoint, enhancementType);

        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(endpoint, HttpMethod.POST, request, byte[].class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                byte[] imageBytes = responseEntity.getBody();
                String filename = UUID.randomUUID().toString() + ".png";
                Path uploadPath = Paths.get(imageStorageLocation);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(filename);
                Files.write(filePath, imageBytes);
                String imageUrlEnhanced = "/api/images/" + filename;
                log.info("Saved enhanced image to: {} | URL: {}", filePath, imageUrlEnhanced);
                return imageUrlEnhanced;
            }
        } catch (Exception e) {
            log.error("Error calling Stable Diffusion enhancement API: {}", e.getMessage(), e);
        }
        return imageUrl; // Fallback
    }

    private String getEnhancementEndpoint(String type) {
        switch (type) {
            case "upscale": return "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-x4-upscaler";
            default: return null;
        }
    }

    @Override
    public String getName() {
        return "Stable Diffusion";
    }

    @Override
    public String getProviderName() {
        return "Stable Diffusion";
    }

    @Override
    public String getApiUrl() {
        return imageApiUrl;
    }

    @Override
    public boolean supportsImageGeneration() {
        return apiKey != null && !apiKey.isEmpty();
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
