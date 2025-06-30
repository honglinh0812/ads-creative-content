// package com.fbadsautomation.controller;

// import com.fbadsautomation.model.AdContent;
// import com.fbadsautomation.service.AIContentService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import javax.validation.Valid;
// import java.util.List; // Import List
// import java.util.Map;

// @RestController
// @RequestMapping("/api/ai")
// @RequiredArgsConstructor
// @Slf4j
// public class AIContentController {

//     private final AIContentService aiContentService;
//     private static final int DEFAULT_VARIATIONS = 3; // Default number of variations for API call

//     @PostMapping("/generate")
//     // Corrected return type to List<AdContent>
//     public ResponseEntity<List<AdContent>> generateContent(
//             @Valid @RequestBody GenerateContentRequest request) {
//         log.info("Generating content with prompt: {}, provider: {}, variations: {}", 
//                  request.getPrompt(), request.getProvider(), request.getNumberOfVariations());
        
//         // Use the method that accepts numberOfVariations
//         List<AdContent> contents = aiContentService.generateContent(
//                 request.getPrompt(), 
//                 request.getContentType(), 
//                 request.getProvider(),
//                 request.getNumberOfVariations() > 0 ? request.getNumberOfVariations() : DEFAULT_VARIATIONS // Use requested variations or default
//         );
//         return ResponseEntity.ok(contents);
//     }

//     @GetMapping("/providers")
//     // Corrected return type to Map<String, List<String>>
//     public ResponseEntity<Map<String, List<String>>> getAvailableProviders() {
//         return ResponseEntity.ok(aiContentService.getAvailableProviders());
//     }

//     // Added numberOfVariations to the request object
//     public static class GenerateContentRequest {
//         private String prompt;
//         private AdContent.ContentType contentType;
//         private String provider;
//         private int numberOfVariations = DEFAULT_VARIATIONS; // Default to generating multiple variations

//         public String getPrompt() {
//             return prompt;
//         }

//         public void setPrompt(String prompt) {
//             this.prompt = prompt;
//         }

//         public AdContent.ContentType getContentType() {
//             return contentType;
//         }

//         public void setContentType(AdContent.ContentType contentType) {
//             this.contentType = contentType;
//         }

//         public String getProvider() {
//             // Default to OpenAI if provider is null or empty
//             return (provider == null || provider.trim().isEmpty()) ? "OpenAI" : provider;
//         }

//         public void setProvider(String provider) {
//             this.provider = provider;
//         }

//         public int getNumberOfVariations() {
//             return numberOfVariations;
//         }

//         public void setNumberOfVariations(int numberOfVariations) {
//             this.numberOfVariations = numberOfVariations;
//         }
//     }
// }

