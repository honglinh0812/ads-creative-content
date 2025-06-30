package com.fbadsautomation.service;

import com.fbadsautomation.model.VideoGenerationJob;
import com.fbadsautomation.repository.VideoGenerationJobRepository;
import com.fbadsautomation.video.JobNotCompletedException;
import com.fbadsautomation.video.VideoGenerationProvider;
import com.fbadsautomation.video.VideoJobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing video generation jobs
 */
@Service
public class VideoGenerationService {
    private final VideoGenerationProvider videoProvider;
    private final VideoGenerationJobRepository jobRepository;
    
    @Autowired
    public VideoGenerationService(VideoGenerationProvider videoProvider, 
                                 VideoGenerationJobRepository jobRepository) {
        this.videoProvider = videoProvider;
        this.jobRepository = jobRepository;
    }
    
    /**
     * Submits a new video generation job
     * @param prompt The text prompt
     * @param durationSeconds The duration in seconds (10-15)
     * @return The created job entity
     */
    public VideoGenerationJob createVideoGenerationJob(String prompt, int durationSeconds) {
        // Validate input
        if (durationSeconds < 10 || durationSeconds > 15) {
            throw new IllegalArgumentException("Video duration must be between 10 and 15 seconds");
        }
        
        // Submit job to provider
        String externalJobId = videoProvider.submitVideoGenerationJob(prompt, durationSeconds);
        
        // Create and save job entity
        VideoGenerationJob job = new VideoGenerationJob();
        job.setExternalJobId(externalJobId);
        job.setPrompt(prompt);
        job.setDurationSeconds(durationSeconds);
        job.setStatus(VideoJobStatus.PENDING.name());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        
        return jobRepository.save(job);
    }
    
    /**
     * Updates the status of a job by checking with the provider
     * @param jobId The internal job ID
     * @return The updated job entity
     */
    public VideoGenerationJob updateJobStatus(Long jobId) {
        VideoGenerationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        
        // Only check status if not already completed or failed
        if (!job.getStatus().equals(VideoJobStatus.COMPLETED.name()) && 
            !job.getStatus().equals(VideoJobStatus.FAILED.name())) {
            
            VideoJobStatus providerStatus = videoProvider.checkJobStatus(job.getExternalJobId());
            
            // Update job status
            job.setStatus(providerStatus.name());
            job.setUpdatedAt(LocalDateTime.now());
            
            // If completed, get the result URL
            if (providerStatus == VideoJobStatus.COMPLETED) {
                try {
                    String videoUrl = videoProvider.getVideoResult(job.getExternalJobId());
                    job.setResultUrl(videoUrl);
                } catch (JobNotCompletedException e) {
                    // This shouldn't happen if status is COMPLETED
                    job.setStatus(VideoJobStatus.PROCESSING.name());
                }
            }
            
            jobRepository.save(job);
        }
        
        return job;
    }
    
    /**
     * Retrieves all jobs with their current status
     * @return List of all video generation jobs
     */
    public List<VideoGenerationJob> getAllJobs() {
        return jobRepository.findAll();
    }
    
    /**
     * Gets a specific job by ID
     * @param jobId The internal job ID
     * @return The job entity
     */
    public VideoGenerationJob getJob(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
    }
}
