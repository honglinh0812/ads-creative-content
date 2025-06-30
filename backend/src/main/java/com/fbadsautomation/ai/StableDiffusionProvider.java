package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StableDiffusionProvider implements AIProvider {
    private static final Logger log = LoggerFactory.getLogger(StableDiffusionProvider.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String imageApiUrl;
    private final String imageSavePath = "uploads/images"; // Use same path as configured
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StableDiffusionProvider(
            RestTemplate restTemplate,
            @Value("${ai.huggingface.api-key:}") String apiKey,
            @Value("${ai.huggingface.image-api-url:https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0}") String imageApiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.imageApiUrl = imageApiUrl;
        log.info("Using Stable Diffusion Image API URL: {}", this.imageApiUrl);
        
        // Ensure the image save directory exists
        try {
            Files.createDirectories(Paths.get(imageSavePath));
            log.info("Image save directory ensured at: {}", imageSavePath);
        } catch (IOException e) {
            log.error("Could not create image save directory: {}", imageSavePath, e);
        }
    }

    @Override
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        if (apiKey != null && !apiKey.isEmpty()) {
            capabilities.add("IMAGE");
        }
        return capabilities;
    }

    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language) {
        // This provider only supports image generation, not text content
        log.warn("Stable Diffusion provider only supports image generation, not text content generation.");
        return generateMockAdContents(prompt, numberOfVariations);
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
                Path filePath = Paths.get(imageSavePath, filename);
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

    @Override
    public String getProviderName() {
        return "Stable Diffusion";
    }

    @Override
    public boolean supportsImageGeneration() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            adContent.setHeadline("Stable Diffusion: Tiêu đề mẫu #" + (i + 1) + " cho: " + prompt);
            adContent.setDescription("Mô tả ngắn gọn cho mẫu quảng cáo Stable Diffusion #" + (i + 1));
            adContent.setPrimaryText("Đây là nội dung chính của mẫu quảng cáo Stable Diffusion #" + (i + 1) + ". Nội dung này sẽ mô tả chi tiết về sản phẩm hoặc dịch vụ được quảng cáo.");
            adContent.setCallToAction("Xem ngay #" + (i + 1));
            adContent.setImageUrl("/img/placeholder.png");
            adContent.setAiProvider(AdContent.AIProvider.HUGGINGFACE); // Use HUGGINGFACE as it's the closest enum value
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
    }
}

