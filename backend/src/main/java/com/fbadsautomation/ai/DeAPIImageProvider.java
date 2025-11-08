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
 * DeAPI Image Provider - High-quality realistic image generation via gamercoin.com
 * API Documentation: https://api.deapi.ai/api/v1/client/txt2img
 * Features:
 * - High-quality realistic images
 * - Customizable parameters (size, model, steps, etc.)
 * - Requires DEAPI_KEY from gamercoin.com
 * - Advanced controls for fine-tuning
 */
@Service
public class DeAPIImageProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(DeAPIImageProvider.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    @Autowired(required = false)
    private MinIOStorageService minIOStorageService;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStoragePath;

    public DeAPIImageProvider(
            RestTemplate restTemplate,
            @Value("${ai.deapi.api-key:}") String apiKey,
            @Value("${ai.deapi.api-url:https://api.deapi.ai/api/v1/client/txt2img}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DeAPI API key is not configured. Image generation will fail.");
        } else {
            log.info("DeAPI Image Provider initialized with API URL: {}", this.apiUrl);
        }
    }

    @Override
    public String getName() {
        return "DeAPI";
    }

    @Override
    public String getProviderName() {
        return "DeAPI";
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public Set<Capability> getCapabilities() {
        // DeAPI only supports image generation, not text
        return EnumSet.of(Capability.IMAGE_GENERATION);
    }

    @Override
    public boolean supportsImageGeneration() {
        return true;
    }

    @Override
    public String generateImage(String prompt) {
        log.info("Generating image with DeAPI for prompt: {}", prompt);

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("DeAPI API key is missing. Cannot generate image.");
            throw new RuntimeException("DeAPI API key is not configured");
        }

        try {
            // Step 1: Enhance prompt for better quality
            String enhancedPrompt = enhanceImagePrompt(prompt);
            log.debug("Enhanced prompt: {}", enhancedPrompt);

            // Step 2: Build DeAPI request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Build request body according to DeAPI format
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", enhancedPrompt);
            requestBody.put("negative_prompt", "low quality, blurry, watermark, text, logo, signature, out of frame");
            requestBody.put("model", "sd3.5"); // Stable Diffusion 3.5 for realistic images
            requestBody.put("width", 1024);
            requestBody.put("height", 1024);
            requestBody.put("steps", 30); // More steps for better quality
            requestBody.put("guidance_scale", 7.5); // Control how closely to follow prompt
            requestBody.put("num_images", 1);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling DeAPI at: {}", apiUrl);

            // Step 3: Call API
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.debug("DeAPI response received: {}", responseBody);

                // Step 4: Extract image URL or base64 from response
                // DeAPI may return: { "status": "success", "images": ["url1", "url2"] }
                // or { "images": [{ "url": "...", "base64": "..." }] }
                String imageData = extractImageDataFromResponse(responseBody);

                if (imageData != null && !imageData.isEmpty()) {
                    // Step 5: Save image to our storage
                    String savedImageUrl;
                    if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
                        // It's a URL, download and save
                        savedImageUrl = downloadAndSaveImage(imageData);
                    } else {
                        // It's base64, decode and save
                        savedImageUrl = saveBase64ImageToStorage(imageData);
                    }

                    log.info("Image generated successfully with DeAPI and saved to: {}", savedImageUrl);
                    return savedImageUrl;
                } else {
                    log.error("No image data found in DeAPI response");
                    throw new RuntimeException("No image data in DeAPI response");
                }
            } else {
                log.error("DeAPI returned error status: {}", response.getStatusCode());
                throw new RuntimeException("DeAPI error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error generating image with DeAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate image with DeAPI", e);
        }
    }

    /**
     * Enhance prompt for better image generation quality
     */
    private String enhanceImagePrompt(String prompt) {
        return "Professional, high-quality, photorealistic advertisement image for: " + prompt +
               ". Sharp focus, vibrant colors, studio lighting, " +
               "ultra detailed, 8k resolution, advertising photography, " +
               "clean composition, eye-catching, commercial quality, " +
               "suitable for Facebook ads, no text overlay.";
    }

    /**
     * Extract image URL or base64 from DeAPI response
     * Supports multiple response structure variations
     */
    private String extractImageDataFromResponse(Map<String, Object> response) {
        try {
            // Try structure 1: response.images[0] (URL string)
            if (response.containsKey("images")) {
                Object imagesObj = response.get("images");

                if (imagesObj instanceof List) {
                    List<?> images = (List<?>) imagesObj;
                    if (!images.isEmpty()) {
                        Object firstImage = images.get(0);

                        // If it's a string (URL or base64)
                        if (firstImage instanceof String) {
                            log.debug("Found image as string in images array");
                            return (String) firstImage;
                        }

                        // If it's a map with url/base64 fields
                        if (firstImage instanceof Map) {
                            Map<String, Object> imageMap = (Map<String, Object>) firstImage;
                            if (imageMap.containsKey("url")) {
                                log.debug("Found image URL in images[0].url");
                                return (String) imageMap.get("url");
                            }
                            if (imageMap.containsKey("base64")) {
                                log.debug("Found image base64 in images[0].base64");
                                return (String) imageMap.get("base64");
                            }
                        }
                    }
                }
            }

            // Try structure 2: response.image (single URL or base64)
            if (response.containsKey("image")) {
                log.debug("Found image in response.image");
                return (String) response.get("image");
            }

            // Try structure 3: response.data.images[0]
            if (response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data.containsKey("images")) {
                    List<?> images = (List<?>) data.get("images");
                    if (!images.isEmpty() && images.get(0) instanceof String) {
                        log.debug("Found image in response.data.images[0]");
                        return (String) images.get(0);
                    }
                }
            }

            log.warn("Could not find image data in response structure: {}", response);
        } catch (Exception e) {
            log.error("Error extracting image data from response", e);
        }

        return null;
    }

    /**
     * Download image from URL and save to storage
     */
    private String downloadAndSaveImage(String imageUrl) {
        try {
            log.debug("Downloading image from DeAPI URL: {}", imageUrl);

            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Downloaded image is empty");
            }

            return saveImageBytesToStorage(imageBytes);

        } catch (Exception e) {
            log.error("Error downloading DeAPI image", e);
            return "/img/placeholder.png";
        }
    }

    /**
     * Save base64 encoded image to storage
     */
    private String saveBase64ImageToStorage(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return saveImageBytesToStorage(imageBytes);
        } catch (Exception e) {
            log.error("Error saving base64 image to storage", e);
            return "/img/placeholder.png";
        }
    }

    /**
     * Save image bytes to MinIO or local filesystem
     */
    private String saveImageBytesToStorage(byte[] imageBytes) {
        try {
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
            log.error("Error saving image bytes to storage", e);
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
            log.error("Error in async image generation with DeAPI", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    // ========== NOT SUPPORTED METHODS (Image-only provider) ==========

    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        throw new UnsupportedOperationException("DeAPI does not support text generation - image only provider");
    }

    @Override
    public CompletableFuture<List<AdContent>> generateAdContentAsync(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        throw new UnsupportedOperationException("DeAPI does not support text generation - image only provider");
    }

    @Override
    public String generateTextCompletion(String prompt, String systemPrompt, Integer maxTokens) {
        throw new UnsupportedOperationException("DeAPI does not support text generation - image only provider");
    }

    @Override
    public String enhanceImage(String imagePath, String enhancementType, Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by DeAPI");
    }

    @Override
    @Async("imageProcessingExecutor")
    public CompletableFuture<String> enhanceImageAsync(String imagePath, String enhancementType, Map<String, Object> params) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Image enhancement not supported by DeAPI"));
        return future;
    }
}
