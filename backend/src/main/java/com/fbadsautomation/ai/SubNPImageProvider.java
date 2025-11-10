package com.fbadsautomation.ai;

import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Capability;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.service.MinIOStorageService;
import com.fbadsautomation.util.ByteArrayMultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * SubNP Image Provider - Free unlimited image generation via subnp.com
 * API Documentation: https://t2i.mcpcore.xyz/api/free/generate
 * Features:
 * - Completely free, no API key required
 * - Multiple model support (flux-dev, flux-schnell, stable-diffusion-3.5, etc.)
 * - Unlimited generations
 * - No authentication required
 */
@Service
public class SubNPImageProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(SubNPImageProvider.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds

    private final RestTemplate restTemplate;
    private final String apiUrl;

    @Autowired(required = false)
    private MinIOStorageService minIOStorageService;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStoragePath;

    public SubNPImageProvider(
            RestTemplate restTemplate,
            @Value("${ai.subnp.api-url:https://t2i.mcpcore.xyz/api/free/generate}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        log.info("SubNP Image Provider initialized with API URL: {}", this.apiUrl);
    }

    @Override
    public String getName() {
        return "SubNP";
    }

    @Override
    public String getProviderName() {
        return "SubNP";
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public Set<Capability> getCapabilities() {
        // SubNP only supports image generation, not text
        return EnumSet.of(Capability.IMAGE_GENERATION);
    }

    @Override
    public boolean supportsImageGeneration() {
        return true;
    }

    @Override
    public String generateImage(String prompt) {
        log.info("Generating image with SubNP for prompt: {}", prompt);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                log.debug("SubNP API attempt {} of {}", attempt, MAX_RETRIES);
                return generateImageInternal(prompt);
            } catch (Exception e) {
                lastException = e;

                if (attempt < MAX_RETRIES) {
                    log.warn("SubNP API attempt {} failed, retrying in {}ms: {}", attempt, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for SubNP image generation");
                        break;
                    }
                } else {
                    log.error("SubNP API attempt {} failed (final attempt): {}", attempt, e.getMessage());
                }
            }
        }

        throw new RuntimeException(
            "SubNP API failed after " + MAX_RETRIES + " attempts: " +
            (lastException != null ? lastException.getMessage() : "Unknown error"),
            lastException
        );
    }

    /**
     * Internal method to generate image with SubNP API (single attempt)
     */
    private String generateImageInternal(String prompt) {
        // Step 1: Enhance prompt for better quality
        String enhancedPrompt = enhanceImagePrompt(prompt);
        log.debug("Enhanced prompt: {}", enhancedPrompt);

        // Step 2: Build SubNP API request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build request body according to SubNP API format
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", enhancedPrompt);
        requestBody.put("model", "flux-schnell"); // Fast model for ads (alternatives: flux-dev, stable-diffusion-3.5)
        requestBody.put("width", 1024);
        requestBody.put("height", 1024);
        requestBody.put("steps", 4); // Faster generation for flux-schnell

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        log.debug("Calling SubNP API at: {}", apiUrl);

        // Step 3: Call API
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            log.debug("SubNP API response received: {}", responseBody);

            // Step 4: Extract image URL from response
            // SubNP returns: { "status": "success", "image_url": "https://..." }
            if ("success".equals(responseBody.get("status")) && responseBody.containsKey("image_url")) {
                String imageUrl = (String) responseBody.get("image_url");

                // Step 5: Download and save image to our storage
                String savedImageUrl = downloadAndSaveImage(imageUrl);
                log.info("Image generated successfully with SubNP and saved to: {}", savedImageUrl);
                return savedImageUrl;
            } else {
                log.error("SubNP API returned unsuccessful status: {}", responseBody.get("status"));
                throw new RuntimeException("SubNP API returned unsuccessful status");
            }
        } else {
            log.error("SubNP API returned error status: {}", response.getStatusCode());
            throw new RuntimeException("SubNP API error: " + response.getStatusCode());
        }
    }

    /**
     * Enhance prompt for better image generation quality
     */
    private String enhanceImagePrompt(String prompt) {
        return "Professional, high-quality advertisement image for: " + prompt +
               ". Clear focus, vibrant colors, professional lighting, " +
               "crisp details, advertising photography style, " +
               "clean composition, eye-catching, suitable for Facebook ads, " +
               "no text, no watermark.";
    }

    /**
     * Download image from SubNP URL and save to our storage (MinIO or local)
     */
    private String downloadAndSaveImage(String imageUrl) {
        try {
            log.debug("Downloading image from SubNP URL: {}", imageUrl);

            // Download image bytes
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Downloaded image is empty");
            }

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + ".png";

            // Try MinIO first, fallback to local storage
            if (minIOStorageService != null) {
                try {
                    log.debug("Uploading image to MinIO: {}", filename);
                    ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                        imageBytes,
                        "image",
                        "image/png",
                        filename
                    );
                    String storedFilename = minIOStorageService.uploadFile(multipartFile);
                    String publicUrl = minIOStorageService.getFileUrl(storedFilename);
                    log.info("Image uploaded to MinIO successfully: {}", publicUrl);
                    return publicUrl;
                } catch (Exception minioError) {
                    log.warn("MinIO upload failed, falling back to local storage: {}", minioError.getMessage());
                }
            }

            // Fallback to local filesystem
            Path uploadPath = Paths.get(imageStoragePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, imageBytes);

            String localUrl = "/api/images/" + filename;
            log.info("Image saved to local filesystem: {}", localUrl);
            return localUrl;

        } catch (Exception e) {
            log.error("Error downloading and saving SubNP image", e);
            return "/img/placeholder.png";
        }
    }

    @Override
    @Async("imageProcessingExecutor")
    public CompletableFuture<String> generateImageAsync(String prompt) {
        try {
            String result = generateImage(prompt);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error in async image generation with SubNP", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    // ========== NOT SUPPORTED METHODS (Image-only provider) ==========

    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        throw new UnsupportedOperationException("SubNP does not support text generation - image only provider");
    }

    @Override
    public CompletableFuture<List<AdContent>> generateAdContentAsync(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        throw new UnsupportedOperationException("SubNP does not support text generation - image only provider");
    }

    @Override
    public String generateTextCompletion(String prompt, String systemPrompt, Integer maxTokens) {
        throw new UnsupportedOperationException("SubNP does not support text generation - image only provider");
    }

    @Override
    public String enhanceImage(String imagePath, String enhancementType, Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by SubNP");
    }

    @Override
    @Async("imageProcessingExecutor")
    public CompletableFuture<String> enhanceImageAsync(String imagePath, String enhancementType, Map<String, Object> params) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Image enhancement not supported by SubNP"));
        return future;
    }
}
