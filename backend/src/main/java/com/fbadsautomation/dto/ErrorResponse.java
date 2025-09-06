package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ErrorResponse {
    
    private boolean success;
    private String error;
    private String message;
    private int status;
    private String path;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
        // For validation errors
    private Map<String, String> fieldErrors;
    
    // For multiple error messages
    private List<String> errors;
    
    // Request ID for tracking
    private String requestId;
    
    public ErrorResponse() {}
    
    public ErrorResponse(boolean success, String error, String message, int status, String path, LocalDateTime timestamp, Map<String, String> fieldErrors, List<String> errors, String requestId) {
        this.success = success;
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = timestamp;
        this.fieldErrors = fieldErrors;
        this.errors = errors;
        this.requestId = requestId;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
    
    public static class ErrorResponseBuilder {
        private boolean success;
        private String error;
        private String message;
        private int status;
        private String path;
        private LocalDateTime timestamp;
        private Map<String, String> fieldErrors;
        private List<String> errors;
        private String requestId;
        
        public ErrorResponseBuilder success(boolean success) { this.success = success; return this; }
        public ErrorResponseBuilder error(String error) { this.error = error; return this; }
        public ErrorResponseBuilder message(String message) { this.message = message; return this; }
        public ErrorResponseBuilder status(int status) { this.status = status; return this; }
        public ErrorResponseBuilder path(String path) { this.path = path; return this; }
        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ErrorResponseBuilder fieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; return this; }
        public ErrorResponseBuilder errors(List<String> errors) { this.errors = errors; return this; }
        public ErrorResponseBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        
        public ErrorResponse build() {
            return new ErrorResponse(success, error, message, status, path, timestamp, fieldErrors, errors, requestId);
        }
    }
    
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse validationError(String message, Map<String, String> fieldErrors, String path) {
        return ErrorResponse.builder()
                .success(false)
                .status(400)
                .error("Validation Error")
                .message(message)
                .fieldErrors(fieldErrors)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
