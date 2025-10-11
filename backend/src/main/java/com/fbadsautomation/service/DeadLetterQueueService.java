package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.exception.AIProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DLQ_PREFIX = "dlq:";
    private static final String DLQ_RETRY_PREFIX = "dlq:retry:";
    private static final String DLQ_STATS_PREFIX = "dlq:stats:";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_HOURS = 1;

    /**
     * Add a failed AI request to the dead letter queue
     */
    public void addFailedRequest(FailedAIRequest failedRequest) {
        try {
            String dlqKey = DLQ_PREFIX + UUID.randomUUID().toString();

            // Set expiry to 7 days
            redisTemplate.opsForValue().set(dlqKey, failedRequest, 7, TimeUnit.DAYS);

            // Add to retry queue if retryable and under max attempts
            if (failedRequest.isRetryable() && failedRequest.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                scheduleRetry(failedRequest, dlqKey);
            }

            // Update statistics
            updateDLQStats(failedRequest);

            log.warn("Added failed AI request to DLQ: Provider={}, Error={}, Retryable={}",
                failedRequest.getProvider(), failedRequest.getErrorMessage(), failedRequest.isRetryable());

        } catch (Exception e) {
            log.error("Failed to add request to dead letter queue", e);
        }
    }

    /**
     * Schedule a retry for a failed request
     */
    private void scheduleRetry(FailedAIRequest failedRequest, String originalKey) {
        try {
            String retryKey = DLQ_RETRY_PREFIX + UUID.randomUUID().toString();

            RetryRequest retryRequest = new RetryRequest();
            retryRequest.setOriginalKey(originalKey);
            retryRequest.setFailedRequest(failedRequest);
            retryRequest.setScheduledRetryTime(LocalDateTime.now().plusHours(RETRY_DELAY_HOURS));

            // Store retry request with appropriate delay
            redisTemplate.opsForValue().set(retryKey, retryRequest,
                RETRY_DELAY_HOURS + 1, TimeUnit.HOURS);

            log.debug("Scheduled retry for failed AI request: Provider={}, Attempt={}",
                failedRequest.getProvider(), failedRequest.getRetryCount() + 1);

        } catch (Exception e) {
            log.error("Failed to schedule retry for dead letter queue item", e);
        }
    }

    /**
     * Process retry queue - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void processRetryQueue() {
        try {
            Set<String> retryKeys = redisTemplate.keys(DLQ_RETRY_PREFIX + "*");
            if (retryKeys == null || retryKeys.isEmpty()) {
                return;
            }

            int processedCount = 0;
            int retriedCount = 0;

            for (String retryKey : retryKeys) {
                try {
                    Object obj = redisTemplate.opsForValue().get(retryKey);
                    if (obj instanceof RetryRequest) {
                        RetryRequest retryRequest = (RetryRequest) obj;

                        if (LocalDateTime.now().isAfter(retryRequest.getScheduledRetryTime())) {
                            // Time to retry
                            if (attemptRetry(retryRequest)) {
                                // Retry successful, remove from retry queue
                                redisTemplate.delete(retryKey);
                                redisTemplate.delete(retryRequest.getOriginalKey());
                                retriedCount++;
                            } else {
                                // Retry failed, increment retry count
                                FailedAIRequest failedRequest = retryRequest.getFailedRequest();
                                failedRequest.setRetryCount(failedRequest.getRetryCount() + 1);
                                failedRequest.setLastRetryTime(LocalDateTime.now());

                                if (failedRequest.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                                    // Max retries exceeded, move to permanent DLQ
                                    redisTemplate.delete(retryKey);
                                    log.warn("Max retries exceeded for AI request: Provider={}",
                                        failedRequest.getProvider());
                                } else {
                                    // Schedule another retry
                                    retryRequest.setScheduledRetryTime(
                                        LocalDateTime.now().plusHours(RETRY_DELAY_HOURS * (failedRequest.getRetryCount() + 1))
                                    );
                                    redisTemplate.opsForValue().set(retryKey, retryRequest,
                                        RETRY_DELAY_HOURS * (failedRequest.getRetryCount() + 2), TimeUnit.HOURS);
                                }
                            }
                            processedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing retry queue item: {}", retryKey, e);
                }
            }

            if (processedCount > 0) {
                log.info("Processed {} retry queue items, {} successful retries", processedCount, retriedCount);
            }

        } catch (Exception e) {
            log.error("Error processing dead letter queue retry items", e);
        }
    }

    /**
     * Attempt to retry a failed AI request
     */
    private boolean attemptRetry(RetryRequest retryRequest) {
        try {
            FailedAIRequest failedRequest = retryRequest.getFailedRequest();

            // This is a placeholder for actual retry logic
            // In a real implementation, this would call the appropriate AI service
            log.info("Attempting retry for AI request: Provider={}, Attempt={}",
                failedRequest.getProvider(), failedRequest.getRetryCount() + 1);

            // For now, simulate retry logic based on error type
            if (failedRequest.getErrorMessage().contains("rate limit")) {
                // Rate limit errors might succeed after delay
                return true;
            } else if (failedRequest.getErrorMessage().contains("network")) {
                // Network errors might succeed on retry
                return Math.random() > 0.3; // 70% success rate for simulation
            }

            return false; // Other errors unlikely to succeed on retry

        } catch (Exception e) {
            log.error("Error attempting retry for dead letter queue item", e);
            return false;
        }
    }

    /**
     * Update DLQ statistics
     */
    private void updateDLQStats(FailedAIRequest failedRequest) {
        try {
            String statsKey = DLQ_STATS_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

            redisTemplate.opsForHash().increment(statsKey, "total_failures", 1);
            redisTemplate.opsForHash().increment(statsKey, "provider_" + failedRequest.getProvider(), 1);

            if (failedRequest.isRetryable()) {
                redisTemplate.opsForHash().increment(statsKey, "retryable_failures", 1);
            } else {
                redisTemplate.opsForHash().increment(statsKey, "permanent_failures", 1);
            }

            // Set expiry for stats (keep for 30 days)
            redisTemplate.expire(statsKey, 30, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("Error updating DLQ statistics", e);
        }
    }

    /**
     * Get DLQ statistics
     */
    public DLQStats getDLQStats(int hoursBack) {
        try {
            DLQStats stats = new DLQStats();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < hoursBack; i++) {
                String hour = now.minusHours(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
                String statsKey = DLQ_STATS_PREFIX + hour;

                if (Boolean.TRUE.equals(redisTemplate.hasKey(statsKey))) {
                    Map<Object, Object> hourStats = redisTemplate.opsForHash().entries(statsKey);

                    for (Map.Entry<Object, Object> entry : hourStats.entrySet()) {
                        String key = entry.getKey().toString();
                        long value = Long.parseLong(entry.getValue().toString());

                        switch (key) {
                            case "total_failures":
                                stats.setTotalFailures(stats.getTotalFailures() + value);
                                break;
                            case "retryable_failures":
                                stats.setRetryableFailures(stats.getRetryableFailures() + value);
                                break;
                            case "permanent_failures":
                                stats.setPermanentFailures(stats.getPermanentFailures() + value);
                                break;
                            default:
                                if (key.startsWith("provider_")) {
                                    String provider = key.substring("provider_".length());
                                    stats.getProviderFailures().put(provider,
                                        stats.getProviderFailures().getOrDefault(provider, 0L) + value);
                                }
                                break;
                        }
                    }
                }
            }

            return stats;

        } catch (Exception e) {
            log.error("Error getting DLQ statistics", e);
            return new DLQStats();
        }
    }

    /**
     * Get current DLQ size
     */
    public long getDLQSize() {
        try {
            Set<String> keys = redisTemplate.keys(DLQ_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Error getting DLQ size", e);
            return 0;
        }
    }

    /**
     * Clear old DLQ entries (older than 7 days)
     */
    @Scheduled(fixedRate = 86400000) // Daily
    public void cleanupOldEntries() {
        try {
            Set<String> keys = redisTemplate.keys(DLQ_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            int deletedCount = 0;
            for (String key : keys) {
                // Redis TTL will handle cleanup, but we can add additional logic here if needed
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl <= 0) {
                    redisTemplate.delete(key);
                    deletedCount++;
                }
            }

            if (deletedCount > 0) {
                log.info("Cleaned up {} expired DLQ entries", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error cleaning up old DLQ entries", e);
        }
    }

    // Data classes
    public static class FailedAIRequest {
        private String requestId;
        private String provider;
        private String prompt;
        private String errorMessage;
        private String errorCode;
        private boolean retryable;
        private int retryCount = 0;
        private LocalDateTime failureTime;
        private LocalDateTime lastRetryTime;
        private Map<String, Object> requestParameters;

        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public boolean isRetryable() { return retryable; }
        public void setRetryable(boolean retryable) { this.retryable = retryable; }

        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

        public LocalDateTime getFailureTime() { return failureTime; }
        public void setFailureTime(LocalDateTime failureTime) { this.failureTime = failureTime; }

        public LocalDateTime getLastRetryTime() { return lastRetryTime; }
        public void setLastRetryTime(LocalDateTime lastRetryTime) { this.lastRetryTime = lastRetryTime; }

        public Map<String, Object> getRequestParameters() { return requestParameters; }
        public void setRequestParameters(Map<String, Object> requestParameters) { this.requestParameters = requestParameters; }
    }

    public static class RetryRequest {
        private String originalKey;
        private FailedAIRequest failedRequest;
        private LocalDateTime scheduledRetryTime;

        // Getters and setters
        public String getOriginalKey() { return originalKey; }
        public void setOriginalKey(String originalKey) { this.originalKey = originalKey; }

        public FailedAIRequest getFailedRequest() { return failedRequest; }
        public void setFailedRequest(FailedAIRequest failedRequest) { this.failedRequest = failedRequest; }

        public LocalDateTime getScheduledRetryTime() { return scheduledRetryTime; }
        public void setScheduledRetryTime(LocalDateTime scheduledRetryTime) { this.scheduledRetryTime = scheduledRetryTime; }
    }

    public static class DLQStats {
        private long totalFailures = 0;
        private long retryableFailures = 0;
        private long permanentFailures = 0;
        private Map<String, Long> providerFailures = new HashMap<>();

        // Getters and setters
        public long getTotalFailures() { return totalFailures; }
        public void setTotalFailures(long totalFailures) { this.totalFailures = totalFailures; }

        public long getRetryableFailures() { return retryableFailures; }
        public void setRetryableFailures(long retryableFailures) { this.retryableFailures = retryableFailures; }

        public long getPermanentFailures() { return permanentFailures; }
        public void setPermanentFailures(long permanentFailures) { this.permanentFailures = permanentFailures; }

        public Map<String, Long> getProviderFailures() { return providerFailures; }
        public void setProviderFailures(Map<String, Long> providerFailures) { this.providerFailures = providerFailures; }
    }
}