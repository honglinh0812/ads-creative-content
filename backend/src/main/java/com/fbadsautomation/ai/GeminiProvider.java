package com.fbadsautomation.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.service.MinIOStorageService;
import com.fbadsautomation.util.ByteArrayMultipartFile;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiProvider implements AIProvider {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final String imageApiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON

    @Autowired(required = false)
    private MinIOStorageService minIOStorageService;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStoragePath;

    public GeminiProvider(
            RestTemplate restTemplate,
            @Value("${ai.gemini.api-key}") String apiKey,
            @Value("${ai.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}") String baseUrl,
            @Value("${ai.gemini.image-api-url:https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict}") String imageApiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        // Construct the full URL with the model name and action
        this.apiUrl = baseUrl + "/gemini-1.5-pro:generateContent";
        this.imageApiUrl = imageApiUrl;
        log.info("Using Gemini Text API URL: {}", this.apiUrl);
        log.info("Using Gemini Image API URL: {}", this.imageApiUrl);
    }
    // Corrected return type to List<AdContent>
    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, com.fbadsautomation.model.FacebookCTA callToAction) {
        List<AdContent> adContents = new ArrayList<>();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key is missing. Returning mock data.");
            return generateMockAdContents(prompt, numberOfVariations, callToAction);
        }
        String fullUrl = apiUrl + "?key=" + apiKey;
        log.debug("Calling Gemini API at: {}", fullUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Updated prompt for JSON output
        String fullPrompt;
        if ("en".equalsIgnoreCase(language)) {
            fullPrompt = "You are an expert Facebook advertising copywriter with deep knowledge of Facebook's advertising policies. " +
                    "Create " + numberOfVariations + " compliant, high-converting ad variations for: \"" + prompt + "\". " +
                    "\n\nFACEBOOK COMPLIANCE CHECKLIST:" +
                    "\n✓ Headline: Exactly 40 characters or less" +
                    "\n✓ Description: Exactly 125 characters or less" +
                    "\n✓ Primary Text: Exactly 1000 characters or less" +
                    "\n✓ No prohibited words (hate, violence, drugs, miracle, guaranteed, cure, instant)" +
                    "\n✓ No excessive punctuation (!!!, $$$, ???)" +
                    "\n✓ No ALL CAPS (except brand names)" +
                    "\n✓ Professional, clear language" +
                    "\n✓ Minimum 3 words per field" +
                    "\n\nBefore finalizing each ad, count characters and verify compliance. " +
                    "Return as JSON array of " + numberOfVariations + " objects: {\"headline\": \"...\", \"description\": \"...\", \"primaryText\": \"...\", \"callToAction\": \"...\"}.";
        } else {
            fullPrompt = "Bạn là chuyên gia viết quảng cáo Facebook am hiểu sâu về chính sách quảng cáo Facebook. " +
                    "Tạo " + numberOfVariations + " mẫu quảng cáo tuân thủ và hiệu quả cho: \"" + prompt + "\". " +
                    "\n\nDANH SÁCH KIỂM TRA TUÂN THỦ FACEBOOK:" +
                    "\n✓ Tiêu đề: Chính xác 40 ký tự hoặc ít hơn" +
                    "\n✓ Mô tả: Chính xác 30 ký tự hoặc ít hơn" +
                    "\n✓ Nội dung chính: Chính xác 125 ký tự hoặc ít hơn" +
                    "\n✓ Không có từ cấm (ghét, bạo lực, ma túy, phép màu, đảm bảo, chữa khỏi, tức thì)" +
                    "\n✓ Không dấu câu thừa (!!!, $$$, ???)" +
                    "\n✓ Không viết hoa toàn bộ (trừ tên thương hiệu)" +
                    "\n✓ Ngôn ngữ chuyên nghiệp, rõ ràng" +
                    "\n✓ Tối thiểu 3 từ mỗi trường" +
                    "\n\nTrước khi hoàn thiện mỗi quảng cáo, hãy đếm ký tự và xác minh tuân thủ. " +
                    "Trả về dạng JSON array gồm " + numberOfVariations + " đối tượng: {\"headline\": \"...\", \"description\": \"...\", \"primaryText\": \"...\", \"callToAction\": \"...\"}.";
        }
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", fullPrompt);
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(contentPart));
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        // Add generationConfig for JSON output
        Map<String, Object> generationConfig = new HashMap<>();
        // Loại bỏ responseMimeType vì không được hỗ trợ trong API version hiện tại
        generationConfig.put("candidateCount", 1); // Request one candidate with potentially multiple variations inside
        requestBody.put("generationConfig", generationConfig);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);
            log.debug("Gemini API Response: {}", response);
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty() && candidates.get(0).containsKey("content")) {
                    Map<String, Object> responseContent = (Map<String, Object>) candidates.get(0).get("content");
                    if (responseContent.containsKey("parts")) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) responseContent.get("parts");
                        if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                            String contentText = (String) parts.get(0).get("text");
                            log.debug("Received content text from Gemini: {}", contentText);
                            // Attempt to parse the JSON content directly into List<AdContent>
                            try {
                                List<AdContent> parsedContents = objectMapper.readValue(contentText, new TypeReference<List<AdContent>>() {});
                                for (AdContent adContent : parsedContents) {
                                    adContent.setAiProvider(AdContent.AIProvider.GEMINI);
                                    adContent.setIsSelected(false); // Default value
                                    adContent.setCallToAction(callToAction);
                                    adContent.setCta(callToAction);
                                    adContents.add(adContent);
                                    if (adContents.size() >= numberOfVariations) break; // Stop if enough variations are parsed
                                }
                                log.info("Successfully parsed {} ad contents from Gemini.", adContents.size());
                            } catch (Exception e) {
                                log.error("Failed to parse JSON response from Gemini: {}", contentText, e);
                            }
                        }
                    }
                }
            }
            if (adContents.isEmpty()) {
                 log.warn("Failed to parse valid ad content from Gemini response. Response: {}", response);
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
        }
        // Fill with mock data if not enough variations generated/parsed
        while (adContents.size() < numberOfVariations) {
            log.warn("Generated/Parsed only {} valid ad contents from Gemini, filling remaining {} with mock data.", adContents.size(), numberOfVariations - adContents.size());
            adContents.addAll(generateMockAdContents(prompt, 1, callToAction));
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
            log.warn("Gemini API key is missing for text completion");
            return null;
        }

        try {
            String fullUrl = apiUrl + "?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Combine system prompt and user prompt
            String combinedPrompt = "";
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                combinedPrompt = systemPrompt + "\n\n" + prompt;
            } else {
                combinedPrompt = prompt;
            }

            // Build Gemini request format
            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("text", combinedPrompt);

            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");
            content.put("parts", List.of(contentPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            // Add generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("maxOutputTokens", maxTokens != null ? maxTokens : 300);
            generationConfig.put("temperature", 0.7);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Gemini Text Completion API for prompt enhancement");

            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty() && candidates.get(0).containsKey("content")) {
                    Map<String, Object> contentResponse = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");

                    if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                        String text = (String) parts.get(0).get("text");
                        log.info("Gemini text completion successful, length: {}", text != null ? text.length() : 0);
                        return text != null ? text.trim() : null;
                    }
                }
            }

            log.warn("Gemini text completion returned empty response");
            return null;

        } catch (Exception e) {
            log.error("Error calling Gemini for text completion: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String generateImage(String prompt) {
        log.info("Generating image with Gemini Imagen 3 for prompt: {}", prompt);

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Gemini API key is missing. Cannot generate image.");
            return "/img/placeholder.png";
        }

        try {
            // Step 1: Enhance prompt for better quality
            String enhancedPrompt = enhanceImagePrompt(prompt);
            log.debug("Enhanced prompt: {}", enhancedPrompt);

            // Step 2: Build Gemini Imagen API request
            String fullUrl = imageApiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // Build request body according to Gemini Imagen format
            Map<String, Object> instancePrompt = new HashMap<>();
            instancePrompt.put("prompt", enhancedPrompt);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sampleCount", 1);
            parameters.put("aspectRatio", "1:1");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("instances", List.of(instancePrompt));
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Gemini Imagen API at: {}", imageApiUrl);

            // Step 3: Call API
            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);

            if (response != null) {
                log.debug("Gemini Imagen response received");

                // Step 4: Parse response and extract base64 image
                String base64Image = extractBase64ImageFromResponse(response);

                if (base64Image != null && !base64Image.isEmpty()) {
                    // Step 5: Save image to storage
                    String savedImageUrl = saveBase64ImageToStorage(base64Image);
                    log.info("Image generated successfully and saved to: {}", savedImageUrl);
                    return savedImageUrl;
                } else {
                    log.error("No image data found in Gemini response");
                    throw new RuntimeException("No image data in Gemini response");
                }
            } else {
                log.error("Gemini API returned null response");
                throw new RuntimeException("Gemini API returned null response");
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Check for specific billing error
            if (e.getStatusCode().value() == 400 &&
                e.getResponseBodyAsString().contains("billed users")) {
                log.error("Gemini Imagen requires billing - triggering fallback to next provider");
                throw new RuntimeException("Gemini Imagen API requires billing account", e);
            }
            log.error("HTTP error from Gemini Imagen: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Failed to generate image with Gemini Imagen: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating image with Gemini Imagen: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate image with Gemini", e);
        }
    }

    /**
     * Enhance prompt for better image generation quality
     * Similar to OpenAI enhancement logic
     */
    private String enhanceImagePrompt(String prompt) {
        return "Professional, high-quality advertisement image for: " + prompt +
               ". Clear product focus, vibrant colors, professional lighting, " +
               "crisp details, studio quality, advertising photography style, " +
               "clean composition, eye-catching, suitable for Facebook ads.";
    }

    /**
     * Extract base64 encoded image from Gemini Imagen API response
     * Supports both new (candidates) and old (generatedImages) response structures
     */
    private String extractBase64ImageFromResponse(Map<String, Object> response) {
        try {
            if (response.containsKey("predictions")) {
                List<Map<String, Object>> predictions = (List<Map<String, Object>>) response.get("predictions");
                if (!predictions.isEmpty()) {
                    Map<String, Object> prediction = predictions.get(0);

                    // Try new API structure first: predictions[0].candidates[0].content
                    if (prediction.containsKey("candidates")) {
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) prediction.get("candidates");
                        if (!candidates.isEmpty()) {
                            Map<String, Object> candidateData = candidates.get(0);
                            if (candidateData.containsKey("content")) {
                                String mimeType = (String) candidateData.get("mimeType");
                                log.debug("Found image in candidates structure, mimeType: {}", mimeType);
                                return (String) candidateData.get("content");
                            }
                        }
                    }

                    // Fallback to old structure: predictions[0].generatedImages[0].imageBytes
                    if (prediction.containsKey("generatedImages")) {
                        List<Map<String, Object>> generatedImages = (List<Map<String, Object>>) prediction.get("generatedImages");
                        if (!generatedImages.isEmpty()) {
                            Map<String, Object> imageData = generatedImages.get(0);
                            if (imageData.containsKey("imageBytes")) {
                                log.debug("Found image in generatedImages structure (legacy)");
                                return (String) imageData.get("imageBytes");
                            }
                        }
                    }
                }
            }

            log.warn("Could not find image data in response structure: {}", response);
        } catch (Exception e) {
            log.error("Error extracting base64 image from response", e);
        }

        return null;
    }

    /**
     * Save base64 encoded image to MinIO storage or local filesystem
     */
    private String saveBase64ImageToStorage(String base64Image) {
        try {
            // Decode base64 to byte array
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + ".png";

            // Try MinIO first, fallback to local storage
            if (minIOStorageService != null) {
                try {
                    log.debug("Uploading image to MinIO: {}", filename);
                    // Wrap byte array in MultipartFile wrapper
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
            log.error("Error saving base64 image to storage", e);
            return "/img/placeholder.png";
        }
    }

    @Override
    public java.util.Set<com.fbadsautomation.model.Capability> getCapabilities() {
        // Gemini now supports both text and image generation
        return java.util.EnumSet.of(
            com.fbadsautomation.model.Capability.TEXT_GENERATION,
            com.fbadsautomation.model.Capability.IMAGE_GENERATION
        );
    }

    @Override
    public String getApiUrl() {
        return apiUrl;
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    public boolean supportsImageGeneration() {
        // Gemini now supports image generation via Imagen 3
        return true;
    }

    @Override
    public String enhanceImage(String imagePath, String enhancementType, java.util.Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by Gemini");
    }

    // Mock Ad Content Generation (Corrected return type)
    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations, com.fbadsautomation.model.FacebookCTA callToAction) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            // Ensure headline stays within 40 character limit
            String shortPrompt = prompt.length() > 20 ? prompt.substring(0, 17) + "..." : prompt;
            adContent.setHeadline("Phiên bản " + (i + 1) + ": " + shortPrompt); // Max ~35 chars
            adContent.setDescription("Nội dung quảng cáo được tạo bởi Gemini AI, mẫu số " + (i + 1)); // Max 125 chars
            adContent.setPrimaryText("Phiên bản quảng cáo " + (i + 1) + " cho: " + prompt + ". " +
                "Nội dung được tạo tự động bởi Gemini AI với chất lượng cao, tối ưu hóa cho Facebook Ads."); // Within 1000 chars
            adContent.setCallToAction(callToAction);
            adContent.setCta(callToAction);
            adContent.setImageUrl("/img/placeholder.png"); // Use local placeholder
            adContent.setAiProvider(AdContent.AIProvider.GEMINI);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
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