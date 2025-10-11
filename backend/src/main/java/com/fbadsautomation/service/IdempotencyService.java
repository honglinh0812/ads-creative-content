package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final long DEFAULT_TTL_HOURS = 24;

    /**
     * Generate idempotency key based on request content
     */
    public String generateIdempotencyKey(String operation, Object requestBody, String userId) {
        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            String input = operation + ":" + userId + ":" + requestJson;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return IDEMPOTENCY_PREFIX + hexString.toString();

        } catch (Exception e) {
            log.error("Error generating idempotency key", e);
            return IDEMPOTENCY_PREFIX + System.currentTimeMillis() + ":" + operation;
        }
    }

    /**
     * Store idempotent operation result
     */
    public void storeIdempotentResult(String idempotencyKey, Object result, long ttlHours) {
        try {
            IdempotentResult idempotentResult = new IdempotentResult();
            idempotentResult.setResult(result);
            idempotentResult.setTimestamp(LocalDateTime.now());
            idempotentResult.setStatus("SUCCESS");

            redisTemplate.opsForValue().set(idempotencyKey, idempotentResult, ttlHours, TimeUnit.HOURS);

            log.debug("Stored idempotent result for key: {}", idempotencyKey);

        } catch (Exception e) {
            log.error("Error storing idempotent result for key: {}", idempotencyKey, e);
        }
    }

    /**
     * Store idempotent operation result with default TTL
     */
    public void storeIdempotentResult(String idempotencyKey, Object result) {
        storeIdempotentResult(idempotencyKey, result, DEFAULT_TTL_HOURS);
    }

    /**
     * Store idempotent operation error
     */
    public void storeIdempotentError(String idempotencyKey, String errorMessage, String errorCode) {
        try {
            IdempotentResult idempotentResult = new IdempotentResult();
            idempotentResult.setErrorMessage(errorMessage);
            idempotentResult.setErrorCode(errorCode);
            idempotentResult.setTimestamp(LocalDateTime.now());
            idempotentResult.setStatus("ERROR");

            redisTemplate.opsForValue().set(idempotencyKey, idempotentResult, DEFAULT_TTL_HOURS, TimeUnit.HOURS);

            log.debug("Stored idempotent error for key: {}", idempotencyKey);

        } catch (Exception e) {
            log.error("Error storing idempotent error for key: {}", idempotencyKey, e);
        }
    }

    /**
     * Get cached idempotent result
     */
    public IdempotentResult getIdempotentResult(String idempotencyKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(idempotencyKey);
            if (cached instanceof IdempotentResult) {
                log.debug("Retrieved cached idempotent result for key: {}", idempotencyKey);
                return (IdempotentResult) cached;
            }
            return null;

        } catch (Exception e) {
            log.error("Error retrieving idempotent result for key: {}", idempotencyKey, e);
            return null;
        }
    }

    /**
     * Check if operation has been processed
     */
    public boolean hasBeenProcessed(String idempotencyKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey));
        } catch (Exception e) {
            log.error("Error checking if operation has been processed: {}", idempotencyKey, e);
            return false;
        }
    }

    /**
     * Process operation with idempotency guarantee
     */
    public <T> T processIdempotently(String operation, Object requestBody, String userId,
                                   IdempotentOperation<T> operation_func) {
        String idempotencyKey = generateIdempotencyKey(operation, requestBody, userId);

        // Check if already processed
        IdempotentResult existingResult = getIdempotentResult(idempotencyKey);
        if (existingResult != null) {
            if ("SUCCESS".equals(existingResult.getStatus())) {
                log.debug("Returning cached result for idempotent operation: {}", operation);
                return (T) existingResult.getResult();
            } else if ("ERROR".equals(existingResult.getStatus())) {
                log.debug("Returning cached error for idempotent operation: {}", operation);
                throw new IdempotentOperationException(existingResult.getErrorMessage(),
                    existingResult.getErrorCode());
            }
        }

        try {
            // Process operation
            T result = operation_func.execute();

            // Store successful result
            storeIdempotentResult(idempotencyKey, result);

            return result;

        } catch (RuntimeException e) {
            // Store error result
            storeIdempotentError(idempotencyKey, e.getMessage(),
                e.getClass().getSimpleName());

            throw e;
        } catch (Exception e) {
            // Store error result
            storeIdempotentError(idempotencyKey, e.getMessage(),
                e.getClass().getSimpleName());

            throw new RuntimeException(e);
        }
    }

    /**
     * Clean up expired idempotency keys
     */
    public void cleanupExpiredKeys() {
        try {
            // Redis TTL will handle cleanup automatically
            log.debug("Idempotency keys cleanup handled by Redis TTL");
        } catch (Exception e) {
            log.error("Error during idempotency cleanup", e);
        }
    }

    // Functional interface for idempotent operations
    @FunctionalInterface
    public interface IdempotentOperation<T> {
        T execute() throws Exception;
    }

    // Exception for idempotent operation errors
    public static class IdempotentOperationException extends RuntimeException {
        private final String errorCode;

        public IdempotentOperationException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    // Data class for storing idempotent results
    public static class IdempotentResult {
        private Object result;
        private String errorMessage;
        private String errorCode;
        private LocalDateTime timestamp;
        private String status;

        // Getters and setters
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}