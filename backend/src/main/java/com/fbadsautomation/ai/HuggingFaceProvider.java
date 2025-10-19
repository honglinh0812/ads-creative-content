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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

@Service
public class HuggingFaceProvider implements AIProvider {
    
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceProvider.class);
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String textApiUrl;
    private final String imageApiUrl;
    private final String imageSavePath = "src/main/resources/static/generated_images"; // Define image save directory
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON
    @Value("${app.image.storage.location:uploads/images}")
    private String imageStorageLocation;
    // Define patterns for parsing structured output if the model provides it
    private static final Pattern HEADLINE_PATTERN = Pattern.compile("Headline:(.*?)(?:Description:|Primary Text:|Call to Action:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("Description:(.*?)(?:Headline:|Primary Text:|Call to Action:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PRIMARY_TEXT_PATTERN = Pattern.compile("(?:Primary Text:|Main Text:)(.*?)(?:Headline:|Description:|Call to Action:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public HuggingFaceProvider(
            RestTemplate restTemplate,
            @Value("${ai.huggingface.api-key}") String apiKey,
            @Value("${ai.huggingface.text-api-url:https://api-inference.huggingface.co/models/gpt2}") String textApiUrl,
            @Value("${ai.huggingface.image-api-url:https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-xl-base-1.0}") String imageApiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.textApiUrl = textApiUrl;
        this.imageApiUrl = imageApiUrl;
        log.info("Using Hugging Face Text API URL: {}", this.textApiUrl);
        log.info("Using Hugging Face Image API URL: {}", this.imageApiUrl);
        // Ensure the image save directory exists
        try {
            Files.createDirectories(Paths.get(imageSavePath));
            log.info("Image save directory ensured at: {}", imageSavePath);
        } catch (IOException e) {
            log.error("Could not create image save directory: {}", imageSavePath, e);
        }
    }
    @Override
    public Set<Capability> getCapabilities() {
        Set<Capability> capabilities = EnumSet.of(Capability.TEXT_GENERATION, Capability.MULTI_LANGUAGE, Capability.IMAGE_GENERATION);
        return capabilities;
    }
    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction) {
        List<AdContent> adContents = new ArrayList<>();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Hugging Face API key is missing. Returning mock data.");
            for (int i = 0; i < numberOfVariations; i++) {
                adContents.add(createMockAdContent(i + 1, callToAction));
            }
            return adContents;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Customize prompt for Hugging Face (might need different structure)
        String hgPrompt;
        if ("vi".equalsIgnoreCase(language)) {
            hgPrompt = "Tạo quảng cáo Facebook tuân thủ chính sách với: Tiêu đề (tối đa 40 ký tự), Mô tả (tối đa 125 ký tự), Nội dung chính (tối đa 1000 ký tự). " +
                    "Không dùng từ cấm: ghét, bạo lực, ma túy, phép màu, đảm bảo, chữa khỏi, tức thì. " +
                    "Chủ đề: " + prompt;
        } else {
            hgPrompt = "Create Facebook policy-compliant ad with: Headline (max 40 chars), Description (max 125 chars), Primary Text (max 1000 chars). " +
                    "Avoid prohibited words: hate, violence, drugs, miracle, guaranteed, cure, instant. " +
                    "Topic: " + prompt;
        }
        for (int i = 0; i < numberOfVariations; i++) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", hgPrompt);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            log.debug("Calling Hugging Face Text API at: {} with prompt: {}", textApiUrl, hgPrompt);
            try {
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(textApiUrl,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().isEmpty()) {
                    String generatedText = response.getBody().get(0).get("generated_text").toString();
                    AdContent adContent = parseResponseToAdContent(generatedText, callToAction);
                    adContent.setAiProvider(AdContent.AIProvider.HUGGINGFACE);
                    adContents.add(adContent);
                } else {
                    log.error("Hugging Face API call failed with status: {} and body: {}", response.getStatusCode(), response.getBody());
                    adContents.add(createMockAdContent(i + 1, callToAction));
                }
            } catch (Exception e) {
                log.error("Error calling Hugging Face API: {}", e.getMessage(), e);
                adContents.add(createMockAdContent(i + 1, callToAction));
            }
        }
        return adContents;
    }
    @Override
    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) {
             log.warn("Hugging Face image generation not supported (likely missing API key).");
             return "/img/placeholder.png";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));
        headers.set("Authorization", "Bearer " + apiKey);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", prompt);
        requestBody.put("options", Map.of("wait_for_model", true));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Hugging Face Image API at: {} with prompt: {}", imageApiUrl, prompt);
        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(imageApiUrl, HttpMethod.POST, request, byte[].class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                byte[] imageBytes = responseEntity.getBody();
                log.info("Successfully received image bytes from Hugging Face Image API.");
                // Save the image and return its URL
                String filename = UUID.randomUUID().toString() + ".png";
                Path uploadPath = Paths.get(imageStorageLocation);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(filename);
                try {
                    Files.write(filePath, imageBytes);
                    String imageUrl = "/api/images/" + filename;
                    log.info("Saved generated image to: {} | URL: {}", filePath, imageUrl);
                    return imageUrl;
                } catch (IOException e) {
                    log.error("Failed to save generated image: {}", filePath, e);
                }
            } else {
                 log.error("Hugging Face Image API call failed with status: {} and body: {}", responseEntity.getStatusCode(), responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error calling Hugging Face Image API: {}", e.getMessage(), e);
        }
        // Fallback to placeholder if API call or saving fails
        return "/img/placeholder.png";
    }
    @Override
    public String getName() {
        return "Hugging Face";
    }
    @Override
    public String getProviderName() {
        return "Hugging Face";
    }
    @Override
    public String getApiUrl() {
        return textApiUrl;
    }
    @Override
    public String enhanceImage(String imagePath, String enhancementType, java.util.Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("Image enhancement not supported by HuggingFace");
    }

    @Override
    public boolean supportsImageGeneration() {
        return apiKey != null && !apiKey.isEmpty();
    }
    private AdContent parseResponseToAdContent(String responseText, FacebookCTA callToAction) {
        AdContent adContent = new AdContent();
        responseText = responseText.trim();
        String headline = extractField(responseText, HEADLINE_PATTERN);
        String description = extractField(responseText, DESCRIPTION_PATTERN);
        String primaryText = extractField(responseText, PRIMARY_TEXT_PATTERN);
        if (headline.isEmpty() && description.isEmpty() && primaryText.isEmpty()) {
            log.warn("Could not parse structured fields using regex. Using basic split.");
            String[] lines = responseText.split("\\n");
            headline = lines.length > 0 ? lines[0].trim() : "Hugging Face Ad";
            description = lines.length > 1 ? lines[1].trim() : "Generated ad description.";
            primaryText = responseText;
        } else {
            if (headline.isEmpty()) headline = "Hugging Face Ad";
            if (description.isEmpty()) description = "Generated ad description.";
            if (primaryText.isEmpty()) primaryText = responseText;
        }
        adContent.setHeadline(headline);
        adContent.setDescription(description);
        adContent.setPrimaryText(primaryText);
        adContent.setCallToAction(callToAction); // Gán trực tiếp CTA
        adContent.setCta(callToAction); // Gán trực tiếp CTA
        return adContent;
    }
    private String extractField(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    private AdContent createMockAdContent(int index, FacebookCTA callToAction) {
        AdContent adContent = new AdContent();
        adContent.setHeadline("Mẫu HuggingFace " + index); // 20 chars - safe!
        adContent.setDescription("Nội dung được tạo bởi HuggingFace"); // 37 chars - safe!
        adContent.setPrimaryText("Nội dung quảng cáo mẫu được tạo bởi HuggingFace AI, phiên bản số " + index + ".");
        adContent.setCallToAction(callToAction);
        adContent.setCta(callToAction);
        adContent.setImageUrl("/img/placeholder.png");
        adContent.setAiProvider(AdContent.AIProvider.HUGGINGFACE);
        adContent.setIsSelected(false);
        return adContent;
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
