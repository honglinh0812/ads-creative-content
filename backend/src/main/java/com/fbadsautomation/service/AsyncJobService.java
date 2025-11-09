package com.fbadsautomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.model.AsyncJobStatus;
import com.fbadsautomation.repository.AsyncJobStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncJobService {

    private final AsyncJobStatusRepository jobStatusRepository;
    private final ObjectMapper objectMapper;

    // Constants for job limits
    private static final int MAX_ACTIVE_JOBS_PER_USER = 5;
    private static final int MAX_DAILY_JOBS_PER_USER = 50;

    public String createJob(Long userId, AsyncJobStatus.JobType jobType, Integer totalSteps) {
        // Check user limits
        if (!canCreateJob(userId)) {
            throw new IllegalStateException("User has reached maximum active job limit");
        }

        String jobId = UUID.randomUUID().toString();
        AsyncJobStatus job = AsyncJobStatus.builder()
                .jobId(jobId)
                .userId(userId)
                .jobType(jobType)
                .status(AsyncJobStatus.Status.PENDING)
                .progress(0)
                .totalSteps(totalSteps)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        jobStatusRepository.save(job);
        log.info("Created async job: {} for user: {} with type: {}", jobId, userId, jobType);
        return jobId;
    }

    public void updateJobStatus(String jobId, AsyncJobStatus.Status status, String currentStep) {
        updateJobStatus(jobId, status, null, currentStep, null, null);
    }

    public void updateJobProgress(String jobId, int progress, String currentStep) {
        updateJobStatus(jobId, null, progress, currentStep, null, null);
    }

    public void completeJob(String jobId, Object resultData) {
        try {
            String resultJson = objectMapper.writeValueAsString(resultData);
            updateJobStatus(jobId, AsyncJobStatus.Status.COMPLETED, 100, "Completed", resultJson, null);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result data for job: {}", jobId, e);
            failJob(jobId, "Failed to serialize result data: " + e.getMessage());
        }
    }

    public void failJob(String jobId, String errorMessage) {
        updateJobStatus(jobId, AsyncJobStatus.Status.FAILED, null, "Failed", null, errorMessage);
    }

    public void startJob(String jobId, String currentStep) {
        updateJobStatus(jobId, AsyncJobStatus.Status.IN_PROGRESS, 0, currentStep, null, null);
    }

    @Transactional
    public void updateJobStatus(String jobId, AsyncJobStatus.Status status, Integer progress,
                               String currentStep, String resultData, String errorMessage) {
        Optional<AsyncJobStatus> jobOpt = jobStatusRepository.findByJobId(jobId);
        if (jobOpt.isEmpty()) {
            log.warn("Attempted to update non-existent job: {}", jobId);
            return;
        }

        AsyncJobStatus job = jobOpt.get();
        if (status != null) {
            job.setStatus(status);
        }
        if (progress != null) {
            job.setProgress(Math.min(100, Math.max(0, progress)));
        }
        if (currentStep != null) {
            job.setCurrentStep(currentStep);
        }
        if (resultData != null) {
            job.setResultData(resultData);
        }
        if (errorMessage != null) {
            job.setErrorMessage(errorMessage);
        }

        jobStatusRepository.save(job);
        log.debug("Updated job {}: status={}, progress={}, step={}", jobId, status, progress, currentStep);
    }

    public Optional<AsyncJobStatus> getJob(String jobId) {
        return jobStatusRepository.findByJobId(jobId);
    }

    public Optional<AsyncJobStatus> getUserJob(String jobId, Long userId) {
        return jobStatusRepository.findByJobId(jobId)
                .filter(job -> job.getUserId().equals(userId));
    }

    public List<AsyncJobStatus> getUserJobs(Long userId) {
        return jobStatusRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<AsyncJobStatus> getUserActiveJobs(Long userId) {
        List<AsyncJobStatus.Status> activeStatuses = Arrays.asList(
                AsyncJobStatus.Status.PENDING,
                AsyncJobStatus.Status.IN_PROGRESS
        );
        return jobStatusRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
    }

    public boolean canCreateJob(Long userId) {
        long activeJobs = jobStatusRepository.countByUserIdAndStatusIn(userId, Arrays.asList(
                AsyncJobStatus.Status.PENDING,
                AsyncJobStatus.Status.IN_PROGRESS
        ));
        return activeJobs < MAX_ACTIVE_JOBS_PER_USER;
    }

    public void cancelJob(String jobId, Long userId) {
        Optional<AsyncJobStatus> jobOpt = getUserJob(jobId, userId);
        if (jobOpt.isEmpty()) {
            throw new IllegalArgumentException("Job not found or access denied");
        }

        AsyncJobStatus job = jobOpt.get();

        // Check if job is already in a terminal state
        if (job.getStatus() == AsyncJobStatus.Status.COMPLETED) {
            throw new IllegalStateException("Cannot cancel job - it has already completed successfully");
        }
        if (job.getStatus() == AsyncJobStatus.Status.FAILED) {
            throw new IllegalStateException("Cannot cancel job - it has already failed");
        }
        if (job.getStatus() == AsyncJobStatus.Status.CANCELLED) {
            throw new IllegalStateException("Job has already been cancelled");
        }

        job.setStatus(AsyncJobStatus.Status.CANCELLED);
        jobStatusRepository.save(job);
        log.info("Cancelled job: {} for user: {}", jobId, userId);
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredJobs() {
        LocalDateTime now = LocalDateTime.now();

        // Mark expired jobs as expired
        int markedExpired = jobStatusRepository.markExpiredJobs(
                AsyncJobStatus.Status.PENDING, AsyncJobStatus.Status.EXPIRED, now);
        int markedExpiredInProgress = jobStatusRepository.markExpiredJobs(
                AsyncJobStatus.Status.IN_PROGRESS, AsyncJobStatus.Status.EXPIRED, now);

        if (markedExpired > 0 || markedExpiredInProgress > 0) {
            log.info("Marked {} jobs as expired", markedExpired + markedExpiredInProgress);
        }

        // Delete really old expired jobs (older than 7 days)
        LocalDateTime oldThreshold = now.minusDays(7);
        int deleted = jobStatusRepository.deleteExpiredJobs(oldThreshold);
        if (deleted > 0) {
            log.info("Deleted {} old expired jobs", deleted);
        }
    }

    public <T> Optional<T> getJobResult(String jobId, Long userId, Class<T> resultType) {
        return getUserJob(jobId, userId)
                .filter(job -> job.getStatus() == AsyncJobStatus.Status.COMPLETED)
                .map(job -> {
                    try {
                        return objectMapper.readValue(job.getResultData(), resultType);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize job result for job: {}", jobId, e);
                        return null;
                    }
                });
    }
}