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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HuggingFaceProvider implements AIProvider {
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceProvider.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String textApiUrl;
    private final String imageApiUrl;
    private final String imageSavePath = "src/main/resources/static/generated_images"; // Define image save directory
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing JSON

    // Define patterns for parsing structured output if the model provides it
    private static final Pattern HEADLINE_PATTERN = Pattern.compile("Headline:(.*?)(?:Description:|Primary Text:|Call to Action:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("Description:(.*?)(?:Headline:|Primary Text:|Call to Action:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PRIMARY_TEXT_PATTERN = Pattern.compile("(?:Primary Text:|Main Text:)(.*?)(?:Headline:|Description:|Call to Action:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CTA_PATTERN = Pattern.compile("Call to Action:(.*?)(?:Headline:|Description:|Primary Text:|\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


    public HuggingFaceProvider(
            RestTemplate restTemplate,
            @Value("${ai.huggingface.api-key:}") String apiKey,
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
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("TEXT");
        if (apiKey != null && !apiKey.isEmpty()) { // Only support image if API key is present
            capabilities.add("IMAGE");
        }
        return capabilities;
    }

    // Corrected return type to List<AdContent>
    @Override
    public List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language) {
        List<AdContent> adContents = new ArrayList<>();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Hugging Face API key is missing for text generation. Returning mock data.");
            return generateMockAdContents(prompt, numberOfVariations);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + apiKey);

        String fullPrompt;
        if ("en".equalsIgnoreCase(language)) {
            fullPrompt = "You are a professional ad copywriter. " +
                    "Please generate " + numberOfVariations + " different ad variations for: \"" + prompt + "\". " +
                    "Each ad should include: headline, description, primaryText, and callToAction. " +
                    "Return the result as a JSON list of " + numberOfVariations + " objects, each with keys: headline, description, primaryText, callToAction.";
        } else {
            fullPrompt = "Bạn là một chuyên gia viết quảng cáo. " +
                    "Hãy tạo " + numberOfVariations + " mẫu quảng cáo khác nhau cho: \"" + prompt + "\". " +
                    "Mỗi mẫu quảng cáo cần có tiêu đề (headline), mô tả ngắn (description), nội dung chính (primaryText) và lời kêu gọi hành động (callToAction). " +
                    "Trả về kết quả dưới dạng một danh sách JSON gồm " + numberOfVariations + " đối tượng, mỗi đối tượng có các trường: headline, description, primaryText, callToAction.";
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", fullPrompt);
        requestBody.put("parameters", Map.of(
                "max_new_tokens", 150,
                "num_return_sequences", numberOfVariations,
                "do_sample", true,
                "temperature", 0.7,
                "top_k", 50,
                "top_p", 0.95,
                "repetition_penalty", 1.2
        ));
        requestBody.put("options", Map.of("wait_for_model", true)); // Add wait_for_model

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Hugging Face Text API at: {} with prompt: {}", textApiUrl, fullPrompt);

        try {
            ResponseEntity<List> responseEntity = restTemplate.exchange(textApiUrl, HttpMethod.POST, request, List.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                List<Map<String, String>> responses = responseEntity.getBody();
                log.debug("Received {} responses from Hugging Face Text API: {}", responses.size(), responses);

                for (int i = 0; i < Math.min(responses.size(), numberOfVariations); i++) {
                    String generatedText = responses.get(i).getOrDefault("generated_text", "").trim();
                    if (generatedText.startsWith(fullPrompt)) {
                        generatedText = generatedText.substring(fullPrompt.length()).trim();
                    }
                    log.info("Raw generated text {}: {}", i + 1, generatedText);

                    if (!generatedText.isEmpty()) {
                        AdContent adContent = parseResponseToAdContent(generatedText);
                        if (!adContent.getHeadline().equals("Hugging Face Ad")) { // Check if parsing was successful
                            // Generate image only if capability is supported
                            adContent.setAiProvider(AdContent.AIProvider.HUGGINGFACE); // Set provider
                            adContent.setIsSelected(false); // Default value
                            adContents.add(adContent);
                            log.info("Parsed Ad Content {}: {}", adContents.size(), adContent);
                        } else {
                             log.warn("Failed to parse structured content from generated text {}: {}", i + 1, generatedText);
                        }
                    } else {
                        log.warn("Received empty generated_text for variation {}", i + 1);
                    }
                }
            } else {
                log.error("Hugging Face Text API call failed with status: {} and body: {}", responseEntity.getStatusCode(), responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error calling Hugging Face Text API: {}", e.getMessage(), e);
            return generateMockAdContents(prompt, numberOfVariations);
        }

        // Fill with mock data if not enough valid variations generated
        while (adContents.size() < numberOfVariations) {
             log.warn("Generated only {} valid ad contents, filling remaining {} with mock data.", adContents.size(), numberOfVariations - adContents.size());
            adContents.addAll(generateMockAdContents(prompt, 1));
        }

        return adContents;
    }

    @Override
    public String generateImage(String prompt) {
        if (!supportsImageGeneration()) { // Check capability first
             log.warn("Hugging Face image generation not supported (likely missing API key).");
             return "/img/placeholder.png";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", prompt);
        requestBody.put("options", Map.of("wait_for_model", true)); // Add wait_for_model

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        log.debug("Calling Hugging Face Image API at: {} with prompt: {}", imageApiUrl, prompt);

        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(imageApiUrl, HttpMethod.POST, request, byte[].class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                byte[] imageBytes = responseEntity.getBody();
                log.info("Successfully received image bytes from Hugging Face Image API.");

                // Save the image and return its URL
                String filename = UUID.randomUUID().toString() + ".png";
                Path filePath = Paths.get(imageSavePath, filename);
                try {
                    Files.write(filePath, imageBytes);
                    String imageUrl = "/generated_images/" + filename; // Relative path for serving
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
    public String getProviderName() {
        return "Hugging Face";
    }

    @Override
    public boolean supportsImageGeneration() {
        // Image generation is supported only if an API key is provided
        return apiKey != null && !apiKey.isEmpty();
    }

    // Changed to return AdContent
    private AdContent parseResponseToAdContent(String responseText) {
        AdContent adContent = new AdContent();
        responseText = responseText.trim();

        String headline = extractField(responseText, HEADLINE_PATTERN);
        String description = extractField(responseText, DESCRIPTION_PATTERN);
        String primaryText = extractField(responseText, PRIMARY_TEXT_PATTERN);
        String callToAction = extractField(responseText, CTA_PATTERN);

        if (headline.isEmpty() && description.isEmpty() && primaryText.isEmpty()) {
            log.warn("Could not parse structured fields using regex. Using basic split.");
            String[] lines = responseText.split("\\n");
            headline = lines.length > 0 ? lines[0].trim() : "Hugging Face Ad";
            description = lines.length > 1 ? lines[1].trim() : "Generated ad description.";
            primaryText = responseText;
            callToAction = "Learn More";
        } else {
            if (headline.isEmpty()) headline = "Hugging Face Ad";
            if (description.isEmpty()) description = "Generated ad description.";
            if (primaryText.isEmpty()) primaryText = responseText;
            if (callToAction.isEmpty()) callToAction = "Learn More";
        }

        adContent.setHeadline(headline);
        adContent.setDescription(description);
        adContent.setPrimaryText(primaryText);
        adContent.setCallToAction(callToAction);
        adContent.setCta(callToAction); // Set cta field as well

        return adContent;
    }

    private String extractField(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    // Corrected return type to List<AdContent>
    private List<AdContent> generateMockAdContents(String prompt, int numberOfVariations) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent adContent = new AdContent();
            adContent.setHeadline("Hugging Face: Tiêu đề mẫu #" + (i + 1) + " cho: " + prompt);
            adContent.setDescription("Mô tả ngắn gọn cho mẫu quảng cáo Hugging Face #" + (i + 1));
            adContent.setPrimaryText("Đây là nội dung chính của mẫu quảng cáo Hugging Face #" + (i + 1) + ". Nội dung này sẽ mô tả chi tiết về sản phẩm hoặc dịch vụ được quảng cáo.");
            adContent.setCallToAction("Đặt hàng ngay #" + (i + 1));
            adContent.setCta("Đặt hàng ngay #" + (i + 1)); // Set cta field
            adContent.setImageUrl("/img/placeholder.png");
            adContent.setAiProvider(AdContent.AIProvider.HUGGINGFACE);
            adContent.setIsSelected(false);
            mockContents.add(adContent);
        }
        return mockContents;
    }
}

