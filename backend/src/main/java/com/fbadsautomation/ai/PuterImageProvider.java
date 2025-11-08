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
 * Puter Image Provider - Free image generation with 35+ models via developer.puter.com
 * API Documentation: https://docs.puter.com/api/ai
 * Features:
 * - 35+ AI models (DALL-E 2/3, Flux.1, Stable Diffusion 3, etc.)
 * - Completely free, no API key required
 * - No usage limits
 * - Great for experimentation
 *
 * Note: Puter provides a REST API endpoint for image generation
 */
@Service
public class PuterImageProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(PuterImageProvider.class);
    private final RestTemplate restTemplate;
    private final String apiUrl;

    @Autowired(required = false)
    private MinIOStorageService minIOStorageService;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStoragePath;

    public PuterImageProvider(
            RestTemplate restTemplate,
            @Value("${ai.puter.api-url:https://api.puter.com/drivers/call}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        log.info("Puter Image Provider initialized with API URL: {}", this.apiUrl);
    }

    @Override
    public String getName() {
        return "Puter AI";
    }

    @Override
    public String getProviderName() {
        return "Puter";
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public Set<Capability> getCapabilities() {
        // Puter supports image generation only
        return EnumSet.of(Capability.IMAGE_GENERATION);
    }

    @Override
    public boolean supportsImageGeneration() {
        return true;
    }

    @Override
    public String generateImage(String prompt) {
        log.info("Generating image with Puter AI for prompt: {}", prompt);

        try {
            // Step 1: Enhance prompt for better quality
            String enhancedPrompt = enhanceImagePrompt(prompt);
            log.debug("Enhanced prompt: {}", enhancedPrompt);

            // Step 2: Build Puter API request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build request body according to Puter AI format
            // Puter uses a driver-based API structure
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("interface", "puter-image-generation");
            requestBody.put("driver", "flux-1-schnell"); // Fast Flux model (alternatives: dall-e-3, stable-diffusion-3, etc.)
            requestBody.put("method", "generate");

            Map<String, Object> args = new HashMap<>();
            args.put("prompt", enhancedPrompt);
            args.put("width", 1024);
            args.put("height", 1024);
            args.put("n", 1); // Number of images to generate

            requestBody.put("args", args);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Puter API at: {}", apiUrl);

            // Step 3: Call API
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.debug("Puter API response received: {}", responseBody);

                // Step 4: Extract image data from response
                // Puter may return: { "result": { "images": [{ "url": "...", "base64": "..." }] } }
                // or { "result": "base64data" }
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

                    log.info("Image generated successfully with Puter AI and saved to: {}", savedImageUrl);
                    return savedImageUrl;
                } else {
                    log.error("No image data found in Puter response");
                    throw new RuntimeException("No image data in Puter response");
                }
            } else {
                log.error("Puter API returned error status: {}", response.getStatusCode());
                throw new RuntimeException("Puter API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error generating image with Puter AI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate image with Puter AI", e);
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
               "no text overlay, commercial quality.";
    }

    /**
     * Extract image URL or base64 from Puter response
     * Supports multiple response structure variations
     */
    private String extractImageDataFromResponse(Map<String, Object> response) {
        try {
            // Try structure 1: response.result.images[0].url or .base64
            if (response.containsKey("result")) {
                Object resultObj = response.get("result");

                // If result is a string (direct base64)
                if (resultObj instanceof String) {
                    log.debug("Found image as string in result");
                    return (String) resultObj;
                }

                // If result is a map
                if (resultObj instanceof Map) {
                    Map<String, Object> result = (Map<String, Object>) resultObj;

                    // Try result.images[0]
                    if (result.containsKey("images")) {
                        List<?> images = (List<?>) result.get("images");
                        if (!images.isEmpty()) {
                            Object firstImage = images.get(0);

                            // If it's a string (URL or base64)
                            if (firstImage instanceof String) {
                                log.debug("Found image as string in result.images[0]");
                                return (String) firstImage;
                            }

                            // If it's a map with url/base64 fields
                            if (firstImage instanceof Map) {
                                Map<String, Object> imageMap = (Map<String, Object>) firstImage;
                                if (imageMap.containsKey("url")) {
                                    log.debug("Found image URL in result.images[0].url");
                                    return (String) imageMap.get("url");
                                }
                                if (imageMap.containsKey("base64")) {
                                    log.debug("Found image base64 in result.images[0].base64");
                                    return (String) imageMap.get("base64");
                                }
                                if (imageMap.containsKey("data")) {
                                    log.debug("Found image data in result.images[0].data");
                                    return (String) imageMap.get("data");
                                }
                            }
                        }
                    }

                    // Try result.image
                    if (result.containsKey("image")) {
                        log.debug("Found image in result.image");
                        return (String) result.get("image");
                    }

                    // Try result.data
                    if (result.containsKey("data")) {
                        log.debug("Found image in result.data");
                        return (String) result.get("data");
                    }
                }
            }

            // Try structure 2: response.data (direct)
            if (response.containsKey("data")) {
                Object dataObj = response.get("data");
                if (dataObj instanceof String) {
                    log.debug("Found image in response.data");
                    return (String) dataObj;
                }
            }

            log.warn("Could not find image data in Puter response structure: {}", response);
        } catch (Exception e) {
            log.error("Error extracting image data from Puter response", e);
        }

        return null;
    }

    /**
     * Download image from URL and save to storage
     */
    private String downloadAndSaveImage(String imageUrl) {
        try {
            log.debug("Downloading image from Puter URL: {}", imageUrl);

            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Downloaded image is empty");
            }

            return saveImageBytesToStorage(imageBytes);

        } catch (Exception e) {
            log.error("Error downloading Puter image", e);
            return "/img/placeholder.png";
        }
    }

    /**
     * Save base64 encoded image to storage
     */
    private String saveBase64ImageToStorage(String base64Image) {
        try {
            // Remove data:image/png;base64, prefix if present
            String cleanBase64 = base64Image.replaceFirst("^data:image/[^;]+;base64,", "");
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
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
            log.error("Error in async image generation with Puter AI", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    // ========== NOT SUPPORTED METHODS (Image-only provider) ==========

    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        throw new UnsupportedOperationException("Puter does not support text generation - image only provider");
    }

    @Override
    public CompletableFuture<List<AdContent>> generateAdContentAsync(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        throw new UnsupportedOperationException("Puter does not support text generation - image only provider");
    }

    @Override
    public String generateTextCompletion(String prompt, String systemPrompt, Integer maxTokens) {
        throw new UnsupportedOperationException("Puter does not support text generation - image only provider");
    }

    @Override
    public String enhanceImage(String imagePath, String enhancementType, Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by Puter");
    }

    @Override
    @Async("imageProcessingExecutor")
    public CompletableFuture<String> enhanceImageAsync(String imagePath, String enhancementType, Map<String, Object> params) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Image enhancement not supported by Puter"));
        return future;
    }
}
