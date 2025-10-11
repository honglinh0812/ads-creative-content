package com.fbadsautomation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when external service operations fail
 */
public class ExternalServiceException extends BusinessException {
    private final String serviceName;
    private final int responseCode;
    private final boolean retryable;

    public ExternalServiceException(String serviceName, String message, int responseCode, boolean retryable) {
        super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.BAD_GATEWAY);
        this.serviceName = serviceName;
        this.responseCode = responseCode;
        this.retryable = retryable;
    }

    public ExternalServiceException(String serviceName, String message, int responseCode,
                                  boolean retryable, Throwable cause) {
        super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.BAD_GATEWAY, cause);
        this.serviceName = serviceName;
        this.responseCode = responseCode;
        this.retryable = retryable;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    // Factory methods for common external service scenarios
    public static ExternalServiceException timeout(String serviceName) {
        return new ExternalServiceException(serviceName,
            "External service request timeout", 408, true);
    }

    public static ExternalServiceException unavailable(String serviceName) {
        return new ExternalServiceException(serviceName,
            "External service unavailable", 503, true);
    }

    public static ExternalServiceException unauthorized(String serviceName) {
        return new ExternalServiceException(serviceName,
            "Unauthorized access to external service", 401, false);
    }

    public static ExternalServiceException rateLimited(String serviceName) {
        return new ExternalServiceException(serviceName,
            "Rate limited by external service", 429, true);
    }

    public static ExternalServiceException badRequest(String serviceName, String details) {
        return new ExternalServiceException(serviceName,
            "Bad request to external service: " + details, 400, false);
    }
}