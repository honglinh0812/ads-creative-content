package com.fbadsautomation.controller;

import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AsyncJobStatus;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.service.AsyncAIContentService;
import com.fbadsautomation.service.AsyncJobService;
import com.fbadsautomation.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/ads/async")
@CrossOrigin(origins = "*")
@Tag(name = "Async Ads", description = "API endpoints for asynchronous advertisement content generation")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class AsyncAdController {

    private final AsyncJobService asyncJobService;
    private final AsyncAIContentService asyncAIContentService;
    private final AdService adService;

    @Operation(summary = "Start async ad content generation",
               description = "Initiates asynchronous generation of ad content and returns a job ID for tracking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job started successfully",
                     content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "429", description = "Too many active jobs")
    })
    @PostMapping("/generate")
    public ResponseEntity<?> generateAdContentAsync(
            @Valid @RequestBody AdGenerationRequest request,
            Authentication authentication) {

        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("Starting async ad content generation for user: {}", userId);

            if (!asyncJobService.canCreateJob(userId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many active jobs. Please wait for current jobs to complete."));
            }

            String prompt = request.getPrompt();
            List<String> adLinks = request.getAdLinks();
            String extractedContent = request.getExtractedContent();

            boolean isPromptEmpty = (prompt == null || prompt.trim().isEmpty());
            boolean isAdLinksEmpty = (adLinks == null || adLinks.isEmpty() ||
                                    adLinks.stream().allMatch(link -> link == null || link.trim().isEmpty()));
            boolean isExtractedContentEmpty = (extractedContent == null || extractedContent.trim().isEmpty());

            if (isPromptEmpty && isAdLinksEmpty && isExtractedContentEmpty) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not create ads. Please check the ad prompt / ad link and try again."));
            }

            int totalSteps = calculateTotalSteps(request);
            String jobId = asyncJobService.createJob(userId, AsyncJobStatus.JobType.AD_CONTENT_GENERATION, totalSteps);

            AdContent.ContentType contentType = mapFrontendContentTypeToEnum(request.getAdType());
            FacebookCTA callToAction = request.getCallToAction() != null ?
                request.getCallToAction() : FacebookCTA.LEARN_MORE;

            asyncAIContentService.generateContentAsync(
                jobId,
                userId,
                request.getPrompt(),
                contentType,
                request.getTextProvider(),
                request.getImageProvider(),
                request.getNumberOfVariations() != null ? request.getNumberOfVariations() : 3,
                request.getLanguage(),
                request.getAdLinks(),
                request.getExtractedContent(),
                callToAction,
                request.getMediaFileUrl(),
                request.getWebsiteUrl(),
                request.getLeadFormQuestions(),
                request.getAudienceSegment()
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                    "jobId", jobId,
                    "status", "accepted",
                    "message", "Ad content generation started. Use job ID to check status."
                ));

        } catch (Exception e) {
            log.error("Error starting async ad generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to start ad generation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get job status", description = "Get the status and progress of an async job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getJobStatus(
            @PathVariable String jobId,
            Authentication authentication) {

        try {
            Long userId = Long.parseLong(authentication.getName());
            Optional<AsyncJobStatus> jobOpt = asyncJobService.getUserJob(jobId, userId);

            if (jobOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            AsyncJobStatus job = jobOpt.get();
            return ResponseEntity.ok(Map.of(
                "jobId", job.getJobId(),
                "status", job.getStatus().name(),
                "progress", job.getProgress(),
                "currentStep", job.getCurrentStep() != null ? job.getCurrentStep() : "",
                "createdAt", job.getCreatedAt(),
                "updatedAt", job.getUpdatedAt(),
                "completedAt", job.getCompletedAt(),
                "errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : ""
            ));

        } catch (Exception e) {
            log.error("Error getting job status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get job status"));
        }
    }

    @Operation(summary = "Get job result", description = "Get the result of a completed async job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job result retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Job not found or not completed"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<?> getJobResult(
            @PathVariable String jobId,
            Authentication authentication) {

        try {
            Long userId = Long.parseLong(authentication.getName());
            Optional<List> result = asyncJobService.getJobResult(jobId, userId, List.class);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Job not found or not completed"));
            }

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "result", result.get()
            ));

        } catch (Exception e) {
            log.error("Error getting job result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get job result"));
        }
    }

    @Operation(summary = "Cancel job", description = "Cancel a running async job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "400", description = "Job cannot be cancelled")
    })
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<?> cancelJob(
            @PathVariable String jobId,
            Authentication authentication) {

        try {
            Long userId = Long.parseLong(authentication.getName());
            asyncJobService.cancelJob(jobId, userId);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "status", "cancelled",
                "message", "Job cancelled successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling job", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to cancel job"));
        }
    }

    @Operation(summary = "Get user jobs", description = "Get all jobs for the current user")
    @GetMapping("/jobs")
    public ResponseEntity<?> getUserJobs(Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            List<AsyncJobStatus> jobs = asyncJobService.getUserJobs(userId);

            return ResponseEntity.ok(jobs.stream()
                .map(job -> Map.of(
                    "jobId", job.getJobId(),
                    "jobType", job.getJobType().name(),
                    "status", job.getStatus().name(),
                    "progress", job.getProgress(),
                    "currentStep", job.getCurrentStep() != null ? job.getCurrentStep() : "",
                    "createdAt", job.getCreatedAt(),
                    "updatedAt", job.getUpdatedAt(),
                    "completedAt", job.getCompletedAt()
                ))
                .toList());

        } catch (Exception e) {
            log.error("Error getting user jobs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user jobs"));
        }
    }

    @Operation(summary = "Health check", description = "Check if async service is healthy and available")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy"),
        @ApiResponse(responseCode = "503", description = "Service is unavailable")
    })
    @GetMapping("/health")
    public ResponseEntity<?> checkHealth() {
        try {
            // Check if async services are available
            boolean isHealthy = asyncJobService != null && asyncAIContentService != null;

            if (isHealthy) {
                return ResponseEntity.ok(Map.of(
                    "healthy", true,
                    "status", "UP",
                    "message", "Async service is available"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "healthy", false,
                        "status", "DOWN",
                        "message", "Async service is unavailable"
                    ));
            }
        } catch (Exception e) {
            log.error("Error checking async service health", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "healthy", false,
                    "status", "DOWN",
                    "message", "Service health check failed"
                ));
        }
    }

    private int calculateTotalSteps(AdGenerationRequest request) {
        int steps = 10; // Base steps for text generation
        if (request.getImageProvider() != null && !request.getImageProvider().isBlank()) {
            steps += request.getNumberOfVariations() != null ? request.getNumberOfVariations() : 3;
        }
        return steps;
    }

    private AdContent.ContentType mapFrontendContentTypeToEnum(String frontendType) {
        if (frontendType == null) {
            return AdContent.ContentType.COMBINED;
        }

        return switch (frontendType.toLowerCase()) {
            case "page_post_ad", "page_post" -> AdContent.ContentType.PAGE_POST;
            case "website_conversion_ad", "lead_form_ad", "combined" -> AdContent.ContentType.COMBINED;
            default -> AdContent.ContentType.COMBINED;
        };
    }
}