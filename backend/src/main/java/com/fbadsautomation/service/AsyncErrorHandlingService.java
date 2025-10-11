package com.fbadsautomation.service;

import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncErrorHandlingService {

    private final AsyncJobService asyncJobService;

    @CircuitBreaker(name = "ai-content-generation", fallbackMethod = "fallbackContentGeneration")
    @Retry(name = "ai-content-generation")
    @TimeLimiter(name = "ai-content-generation")
    public CompletableFuture<List<AdContent>> generateContentWithResilience(
            String jobId,
            CompletableFuture<List<AdContent>> contentGeneration,
            String prompt,
            int numberOfVariations,
            FacebookCTA callToAction) {

        return contentGeneration
            .exceptionally(throwable -> {
                log.error("AI content generation failed for job: {}", jobId, throwable);
                asyncJobService.updateJobStatus(jobId, null, null,
                    "Generation failed, attempting fallback", null, null);
                throw new RuntimeException("Content generation failed", throwable);
            })
            .orTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof TimeoutException) {
                    log.error("AI content generation timed out for job: {}", jobId);
                    asyncJobService.failJob(jobId, "Content generation timed out after 5 minutes");
                }
                throw new RuntimeException("Content generation timed out", throwable);
            });
    }

    @CircuitBreaker(name = "ai-image-generation", fallbackMethod = "fallbackImageGeneration")
    @Retry(name = "ai-image-generation")
    @TimeLimiter(name = "ai-image-generation")
    public CompletableFuture<String> generateImageWithResilience(
            String jobId,
            CompletableFuture<String> imageGeneration,
            String prompt) {

        return imageGeneration
            .exceptionally(throwable -> {
                log.error("AI image generation failed for job: {}", jobId, throwable);
                asyncJobService.updateJobStatus(jobId, null, null,
                    "Image generation failed, using placeholder", null, null);
                return "/img/placeholder.png";
            })
            .orTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof TimeoutException) {
                    log.warn("AI image generation timed out for job: {}, using placeholder", jobId);
                }
                return "/img/placeholder.png";
            });
    }

    public CompletableFuture<List<AdContent>> fallbackContentGeneration(
            String jobId,
            CompletableFuture<List<AdContent>> contentGeneration,
            String prompt,
            int numberOfVariations,
            FacebookCTA callToAction,
            Exception ex) {

        log.warn("Using fallback content generation for job: {} due to: {}", jobId, ex.getMessage());

        asyncJobService.updateJobStatus(jobId, null, null,
            "Using fallback content generation", null, null);

        List<AdContent> fallbackContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent content = new AdContent();
            content.setHeadline("Fallback: Creative Headline #" + (i + 1));
            content.setDescription("Professional description for your product or service.");
            content.setPrimaryText("Discover amazing features and benefits. " +
                "Join thousands of satisfied customers who trust our quality and service. " +
                "Limited time offer - don't miss out!");
            content.setCallToAction(callToAction);
            content.setCta(callToAction);
            content.setImageUrl("/img/placeholder.png");
            content.setAiProvider(AdContent.AIProvider.FALLBACK);
            content.setIsSelected(false);
            fallbackContents.add(content);
        }

        return CompletableFuture.completedFuture(fallbackContents);
    }

    public CompletableFuture<String> fallbackImageGeneration(
            String jobId,
            CompletableFuture<String> imageGeneration,
            String prompt,
            Exception ex) {

        log.warn("Using fallback image generation for job: {} due to: {}", jobId, ex.getMessage());
        return CompletableFuture.completedFuture("/img/placeholder.png");
    }

    public void handleAsyncException(String jobId, String operation, Exception ex) {
        log.error("Async operation '{}' failed for job: {}", operation, jobId, ex);

        String errorMessage = determineUserFriendlyErrorMessage(ex);
        asyncJobService.failJob(jobId, errorMessage);

        // Could add notification service here for critical errors
        if (isCriticalError(ex)) {
            notifyCriticalError(jobId, operation, ex);
        }
    }

    private String determineUserFriendlyErrorMessage(Exception ex) {
        if (ex instanceof TimeoutException) {
            return "Request timed out. Please try again later.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("API key")) {
            return "AI service configuration error. Please contact support.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("rate limit")) {
            return "Service temporarily overloaded. Please try again in a few minutes.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("quota")) {
            return "Daily usage limit reached. Please try again tomorrow.";
        } else {
            return "Content generation failed. Please try again with a different prompt.";
        }
    }

    private boolean isCriticalError(Exception ex) {
        return ex instanceof SecurityException ||
               (ex.getMessage() != null && ex.getMessage().contains("database")) ||
               (ex.getMessage() != null && ex.getMessage().contains("configuration")) ||
               (ex.getCause() instanceof OutOfMemoryError);
    }

    private void notifyCriticalError(String jobId, String operation, Exception ex) {
        log.error("CRITICAL ERROR - Job: {}, Operation: {}, Error: {}",
            jobId, operation, ex.getMessage(), ex);

        // TODO: Implement notification service (email, Slack, etc.)
        // For now, just log as critical
    }

    public void validateJobExecution(String jobId, String operation) {
        try {
            var job = asyncJobService.getJob(jobId);
            if (job.isEmpty()) {
                throw new IllegalStateException("Job not found: " + jobId);
            }

            if (job.get().isExpired()) {
                throw new IllegalStateException("Job expired: " + jobId);
            }

            if (job.get().getStatus() == com.fbadsautomation.model.AsyncJobStatus.Status.CANCELLED) {
                throw new IllegalStateException("Job was cancelled: " + jobId);
            }

        } catch (Exception ex) {
            log.error("Job validation failed for {}: {}", jobId, ex.getMessage());
            throw ex;
        }
    }

    public CompletableFuture<Void> executeWithErrorHandling(
            String jobId,
            String operation,
            Runnable task) {

        return CompletableFuture.runAsync(() -> {
            try {
                validateJobExecution(jobId, operation);
                task.run();
            } catch (Exception ex) {
                handleAsyncException(jobId, operation, ex);
                throw new RuntimeException(ex);
            }
        });
    }
}