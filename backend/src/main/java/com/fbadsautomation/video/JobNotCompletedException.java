package com.fbadsautomation.video;

/**
 * Exception thrown when attempting to retrieve results for a job that is not yet complete
 */
public class JobNotCompletedException extends Exception {
    
    public JobNotCompletedException(String message) {
        super(message);
    }
    
    public JobNotCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
