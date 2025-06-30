package com.fbadsautomation.video;

/**
 * Enum representing the possible statuses of a video generation job
 */
public enum VideoJobStatus {
    PENDING,    // Job is queued but not yet processing
    PROCESSING, // Job is currently being processed
    COMPLETED,  // Job has completed successfully
    FAILED      // Job has failed
}
