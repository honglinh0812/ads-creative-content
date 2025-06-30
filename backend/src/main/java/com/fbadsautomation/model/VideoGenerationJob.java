package com.fbadsautomation.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a video generation job
 */
@Entity
@Table(name = "video_generation_jobs")
public class VideoGenerationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "external_job_id", nullable = false)
    private String externalJobId; // The job ID from Fal.ai
    
    @Column(nullable = false)
    private String prompt;
    
    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;
    
    @Column(nullable = false)
    private String status; // Maps to VideoJobStatus enum values
    
    @Column(name = "result_url")
    private String resultUrl; // URL of the generated video when complete
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor
    public VideoGenerationJob() {
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getExternalJobId() {
        return externalJobId;
    }
    
    public void setExternalJobId(String externalJobId) {
        this.externalJobId = externalJobId;
    }
    
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getResultUrl() {
        return resultUrl;
    }
    
    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
