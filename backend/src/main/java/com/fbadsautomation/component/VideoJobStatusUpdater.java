package com.fbadsautomation.component;

import com.fbadsautomation.model.VideoGenerationJob;
import com.fbadsautomation.repository.VideoGenerationJobRepository;
import com.fbadsautomation.service.VideoGenerationService;
import com.fbadsautomation.video.VideoJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

/**
 * Background component that periodically updates the status of pending and processing video jobs.
 * Includes a timeout mechanism and reduces log noise when no active jobs are found.
 */
@Component
public class VideoJobStatusUpdater {
    private static final Logger log = LoggerFactory.getLogger(VideoJobStatusUpdater.class);
    private static final long JOB_TIMEOUT_MINUTES = 60; // Timeout after 60 minutes

    private final VideoGenerationService videoService;
    private final VideoGenerationJobRepository jobRepository;

    @Autowired
    public VideoJobStatusUpdater(VideoGenerationService videoService,
                               VideoGenerationJobRepository jobRepository) {
        this.videoService = videoService;
        this.jobRepository = jobRepository;
    }

    /**
     * Scheduled task that runs every 10 seconds to update the status of non-completed jobs.
     */
    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    public void updatePendingJobs() {
        // Find jobs that are PENDING or PROCESSING
        List<VideoGenerationJob> activeJobs = jobRepository.findByStatusIn(List.of(VideoJobStatus.PENDING.name(), VideoJobStatus.PROCESSING.name()));

        // Only proceed and log if there are active jobs
        if (activeJobs.isEmpty()) {
            // log.trace("No active video generation jobs to update."); // Use trace level if needed
            return; // Exit early if no jobs need updating
        }

        log.debug("Running VideoJobStatusUpdater for {} active jobs...", activeJobs.size());

        activeJobs.forEach(job -> {
            try {
                // Check for timeout
                long minutesSinceCreation = ChronoUnit.MINUTES.between(job.getCreatedAt(), LocalDateTime.now());
                if (minutesSinceCreation > JOB_TIMEOUT_MINUTES) {
                    log.warn("Job {} timed out after {} minutes. Marking as FAILED.", job.getId(), minutesSinceCreation);
                    job.setStatus(VideoJobStatus.FAILED.name());
                    job.setUpdatedAt(LocalDateTime.now());
                    jobRepository.save(job);
                } else {
                    // If not timed out, update status normally
                    log.debug("Updating status for job {}", job.getId());
                    videoService.updateJobStatus(job.getId());
                }
            } catch (Exception e) {
                log.error("Error updating status for job {}: {}", job.getId(), e.getMessage(), e);
                // Optionally mark the job as FAILED on persistent errors
                // Consider adding logic to prevent repeated errors for the same job
                // job.setStatus(VideoJobStatus.FAILED.name());
                // jobRepository.save(job);
            }
        });
        log.debug("VideoJobStatusUpdater finished.");
    }
}
