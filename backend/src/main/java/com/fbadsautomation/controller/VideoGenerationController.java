package com.fbadsautomation.controller;

import com.fbadsautomation.model.VideoGenerationJob;
import com.fbadsautomation.service.VideoGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for video generation operations
 */
@RestController
@RequestMapping("/api/video")
public class VideoGenerationController {
    private final VideoGenerationService videoService;
    
    @Autowired
    public VideoGenerationController(VideoGenerationService videoService) {
        this.videoService = videoService;
    }
    
    /**
     * Endpoint to submit a new video generation job
     * @param request The video generation request
     * @return The created job
     */
    @PostMapping("/generate")
    public ResponseEntity<VideoGenerationJob> generateVideo(@RequestBody VideoGenerationRequest request) {
        VideoGenerationJob job = videoService.createVideoGenerationJob(
            request.getPrompt(), 
            request.getDurationSeconds()
        );
        return ResponseEntity.ok(job);
    }
    
    /**
     * Endpoint to get the status of a specific job
     * @param jobId The job ID
     * @return The job with updated status
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<VideoGenerationJob> getJobStatus(@PathVariable Long jobId) {
        VideoGenerationJob job = videoService.updateJobStatus(jobId);
        return ResponseEntity.ok(job);
    }
    
    /**
     * Endpoint to get all jobs
     * @return List of all jobs
     */
    @GetMapping
    public ResponseEntity<List<VideoGenerationJob>> getAllJobs() {
        List<VideoGenerationJob> jobs = videoService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * Request class for video generation
     */
    public static class VideoGenerationRequest {
        private String prompt;
        private int durationSeconds;
        
        // Getters and setters
        public String getPrompt() {
            return prompt;
        }
        
        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
        
        public int getDurationSeconds() {
            return durationSeconds;
        }
        
        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}
