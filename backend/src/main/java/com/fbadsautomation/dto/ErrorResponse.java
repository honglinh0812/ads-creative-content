package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j

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
    };
    }
