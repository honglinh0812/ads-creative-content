package com.fbadsautomation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when AI provider operations fail
 */
public class AIProviderException extends BusinessException {
    private final String providerName;
    private final boolean retryable;

    public AIProviderException(String providerName, String message, boolean retryable) {
        super(message, "AI_PROVIDER_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
        this.providerName = providerName;
        this.retryable = retryable;
    }

    public AIProviderException(String providerName, String message, boolean retryable, Throwable cause) {
        super(message, "AI_PROVIDER_ERROR", HttpStatus.SERVICE_UNAVAILABLE, cause);
        this.providerName = providerName;
        this.retryable = retryable;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isRetryable() {
        return retryable;
    }

    // Factory methods for common scenarios
    public static AIProviderException quotaExceeded(String providerName) {
        return new AIProviderException(providerName,
            "AI provider quota exceeded", false);
    }

    public static AIProviderException rateLimit(String providerName) {
        return new AIProviderException(providerName,
            "AI provider rate limit exceeded", true);
    }

    public static AIProviderException invalidApiKey(String providerName) {
        return new AIProviderException(providerName,
            "Invalid API key for AI provider", false);
    }

    public static AIProviderException networkError(String providerName, Throwable cause) {
        return new AIProviderException(providerName,
            "Network error connecting to AI provider", true, cause);
    }

    public static AIProviderException invalidResponse(String providerName, String details) {
        return new AIProviderException(providerName,
            "Invalid response from AI provider: " + details, false);
    }
}