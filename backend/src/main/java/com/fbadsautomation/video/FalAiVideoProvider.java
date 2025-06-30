package com.fbadsautomation.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of VideoGenerationProvider using Fal.ai's Stable Video Diffusion API
 */
@Service
public class FalAiVideoProvider implements VideoGenerationProvider {
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiBaseUrl = "https://api.fal.ai/v1";
    private final String modelId = "fal-ai/fast-svd/text-to-video";
    private static final String PROVIDER_NAME = "Fal.ai"; // Added provider name
    
    public FalAiVideoProvider(RestTemplate restTemplate, @Value("${fal.ai.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }
    
    @Override
    public String submitVideoGenerationJob(String prompt, int durationSeconds) {
        // Validate input
        if (durationSeconds < 10 || durationSeconds > 15) {
            throw new IllegalArgumentException("Video duration must be between 10 and 15 seconds");
        }
        
        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        input.put("duration_seconds", durationSeconds);
        requestBody.put("input", input);
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + apiKey);
        
        // Create HTTP entity
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Make API call
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                apiBaseUrl + "/queue/submit/" + modelId,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            // Extract and return job ID
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("request_id");
            } else {
                throw new RuntimeException("Failed to submit video generation job: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error submitting video generation job: " + e.getMessage(), e);
        }
    }
    
    @Override
    public VideoJobStatus checkJobStatus(String jobId) {
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + apiKey);
        
        // Create HTTP entity
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // Make API call
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                apiBaseUrl + "/queue/status/" + modelId + "?request_id=" + jobId,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            // Extract and return status
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                
                switch (status) {
                    case "PENDING":
                        return VideoJobStatus.PENDING;
                    case "IN_PROGRESS":
                        return VideoJobStatus.PROCESSING;
                    case "COMPLETED":
                        return VideoJobStatus.COMPLETED;
                    case "FAILED":
                        return VideoJobStatus.FAILED;
                    default:
                        return VideoJobStatus.PENDING;
                }
            } else {
                throw new RuntimeException("Failed to check job status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking job status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getVideoResult(String jobId) throws JobNotCompletedException {
        // Check status first
        VideoJobStatus status = checkJobStatus(jobId);
        if (status != VideoJobStatus.COMPLETED) {
            throw new JobNotCompletedException("Job is not yet complete. Current status: " + status);
        }
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + apiKey);
        
        // Create HTTP entity
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        // Make API call
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                apiBaseUrl + "/queue/result/" + modelId + "?request_id=" + jobId,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            // Extract and return video URL
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("video_url")) {
                    return (String) data.get("video_url");
                } else {
                    throw new RuntimeException("Video URL not found in response");
                }
            } else {
                throw new RuntimeException("Failed to get video result: " + response.getStatusCode());
            }
        } catch (Exception e) {
            if (e instanceof JobNotCompletedException) {
                throw (JobNotCompletedException) e;
            }
            throw new RuntimeException("Error getting video result: " + e.getMessage(), e);
        }
    }
}
