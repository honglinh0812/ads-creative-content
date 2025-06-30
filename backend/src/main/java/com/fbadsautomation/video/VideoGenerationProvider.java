package com.fbadsautomation.video;

/**
 * Interface for video generation providers
 * Defines methods for submitting jobs, checking status, and retrieving results
 */
public interface VideoGenerationProvider {
    /**
     * Submits a video generation request and returns a job ID
     * @param prompt The text prompt for video generation
     * @param durationSeconds The duration of the video in seconds (10-15 seconds)
     * @return A job ID that can be used to check status and retrieve results
     */
    String submitVideoGenerationJob(String prompt, int durationSeconds);
    
    /**
     * Checks the status of a video generation job
     * @param jobId The job ID returned from submitVideoGenerationJob
     * @return The current status of the job (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    VideoJobStatus checkJobStatus(String jobId);
    
    /**
     * Retrieves the result of a completed video generation job
     * @param jobId The job ID returned from submitVideoGenerationJob
     * @return The URL of the generated video
     * @throws JobNotCompletedException if the job is not yet complete
     */
    String getVideoResult(String jobId) throws JobNotCompletedException;
}
