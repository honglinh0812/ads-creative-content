package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.exception.AIProviderException;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIProviderFallbackService {

    private final Map<String, AIProvider> aiProviders;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DeadLetterQueueService deadLetterQueueService;

    private static final String CIRCUIT_BREAKER_PREFIX = "circuit_breaker:";
    private static final String FALLBACK_CACHE_PREFIX = "fallback_cache:";
    private static final int FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MINUTES = 5;

    // Provider priority order for fallback
    private static final List<String> PROVIDER_PRIORITY = Arrays.asList(
        "openai", "anthropic", "gemini", "huggingface"
    );

    /**
     * Generate AI content with graceful degradation
     */
    public List<AdContent> generateWithFallback(String prompt, int numberOfVariations,
                                               String language, FacebookCTA callToAction) {
        List<String> providersToTry = getAvailableProviders();

        for (String providerName : providersToTry) {
            if (isProviderCircuitOpen(providerName)) {
                log.warn("Skipping provider {} - circuit breaker is open", providerName);
                continue;
            }

            try {
                AIProvider provider = aiProviders.get(providerName);
                if (provider == null) {
                    log.warn("Provider {} not found", providerName);
                    continue;
                }

                log.debug("Attempting AI content generation with provider: {}", providerName);

                List<AdContent> result = provider.generateAdContent(prompt, numberOfVariations, language, callToAction);

                // Reset circuit breaker on success
                resetCircuitBreaker(providerName);

                // Cache successful result for emergency fallback
                cacheFallbackResult(prompt, result);

                log.info("Successfully generated content with provider: {}", providerName);
                return result;

            } catch (Exception e) {
                log.error("Provider {} failed: {}", providerName, e.getMessage());

                // Record failure for circuit breaker
                recordProviderFailure(providerName, e);

                // Add to dead letter queue if it's the last provider
                if (providersToTry.indexOf(providerName) == providersToTry.size() - 1) {
                    addToDeadLetterQueue(prompt, providerName, e, numberOfVariations, language, callToAction);
                }
            }
        }

        // All providers failed, try cached fallback
        List<AdContent> cachedResult = getCachedFallbackResult(prompt);
        if (cachedResult != null && !cachedResult.isEmpty()) {
            log.warn("All AI providers failed, returning cached fallback result");
            return cachedResult;
        }

        // Generate basic fallback content
        log.error("All AI providers failed and no cached result available, generating basic fallback");
        return generateBasicFallback(prompt, numberOfVariations);
    }

    /**
     * Get list of available providers in priority order
     */
    private List<String> getAvailableProviders() {
        List<String> available = new ArrayList<>();

        for (String providerName : PROVIDER_PRIORITY) {
            if (aiProviders.containsKey(providerName)) {
                available.add(providerName);
            }
        }

        return available;
    }

    /**
     * Check if provider circuit breaker is open
     */
    private boolean isProviderCircuitOpen(String providerName) {
        try {
            String circuitKey = CIRCUIT_BREAKER_PREFIX + providerName;
            Object circuitState = redisTemplate.opsForValue().get(circuitKey);

            if (circuitState instanceof CircuitBreakerState) {
                CircuitBreakerState state = (CircuitBreakerState) circuitState;
                return state.isOpen() && LocalDateTime.now().isBefore(state.getOpenUntil());
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking circuit breaker for provider: {}", providerName, e);
            return false;
        }
    }

    /**
     * Record provider failure for circuit breaker
     */
    private void recordProviderFailure(String providerName, Exception exception) {
        try {
            String circuitKey = CIRCUIT_BREAKER_PREFIX + providerName;
            CircuitBreakerState state = getOrCreateCircuitBreakerState(circuitKey);

            state.recordFailure();

            if (state.getFailureCount() >= FAILURE_THRESHOLD) {
                state.openCircuit(LocalDateTime.now().plusMinutes(CIRCUIT_BREAKER_TIMEOUT_MINUTES));
                log.warn("Circuit breaker opened for provider: {} after {} failures",
                    providerName, state.getFailureCount());
            }

            redisTemplate.opsForValue().set(circuitKey, state, 1, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("Error recording failure for provider: {}", providerName, e);
        }
    }

    /**
     * Reset circuit breaker on successful call
     */
    private void resetCircuitBreaker(String providerName) {
        try {
            String circuitKey = CIRCUIT_BREAKER_PREFIX + providerName;
            redisTemplate.delete(circuitKey);
            log.debug("Circuit breaker reset for provider: {}", providerName);

        } catch (Exception e) {
            log.error("Error resetting circuit breaker for provider: {}", providerName, e);
        }
    }

    /**
     * Get or create circuit breaker state
     */
    private CircuitBreakerState getOrCreateCircuitBreakerState(String circuitKey) {
        try {
            Object existing = redisTemplate.opsForValue().get(circuitKey);
            if (existing instanceof CircuitBreakerState) {
                return (CircuitBreakerState) existing;
            }
        } catch (Exception e) {
            log.debug("Creating new circuit breaker state");
        }

        return new CircuitBreakerState();
    }

    /**
     * Cache successful result for emergency fallback
     */
    private void cacheFallbackResult(String prompt, List<AdContent> result) {
        try {
            String cacheKey = FALLBACK_CACHE_PREFIX + hashPrompt(prompt);
            redisTemplate.opsForValue().set(cacheKey, result, 7, TimeUnit.DAYS);
            log.debug("Cached fallback result for prompt hash");

        } catch (Exception e) {
            log.error("Error caching fallback result", e);
        }
    }

    /**
     * Get cached fallback result
     */
    @SuppressWarnings("unchecked")
    private List<AdContent> getCachedFallbackResult(String prompt) {
        try {
            String cacheKey = FALLBACK_CACHE_PREFIX + hashPrompt(prompt);
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached instanceof List) {
                return (List<AdContent>) cached;
            }

        } catch (Exception e) {
            log.error("Error retrieving cached fallback result", e);
        }

        return null;
    }

    /**
     * Generate basic fallback content when all else fails
     */
    private List<AdContent> generateBasicFallback(String prompt, int numberOfVariations) {
        List<AdContent> fallbackContent = new ArrayList<>();

        for (int i = 0; i < numberOfVariations; i++) {
            AdContent content = new AdContent();
            content.setHeadline("Discover Amazing Products!");
            content.setPrimaryText("Check out our latest offerings and special deals. " +
                "Limited time offer - don't miss out!");
            content.setDescription("Explore our wide range of quality products designed just for you.");

            fallbackContent.add(content);
        }

        log.warn("Generated {} basic fallback content items", numberOfVariations);
        return fallbackContent;
    }

    /**
     * Add failed request to dead letter queue
     */
    private void addToDeadLetterQueue(String prompt, String lastProviderTried, Exception exception,
                                     int numberOfVariations, String language, FacebookCTA callToAction) {
        try {
            DeadLetterQueueService.FailedAIRequest failedRequest = new DeadLetterQueueService.FailedAIRequest();
            failedRequest.setRequestId(UUID.randomUUID().toString());
            failedRequest.setProvider(lastProviderTried);
            failedRequest.setPrompt(prompt);
            failedRequest.setErrorMessage(exception.getMessage());
            failedRequest.setErrorCode(exception.getClass().getSimpleName());
            failedRequest.setRetryable(isRetryableException(exception));
            failedRequest.setFailureTime(LocalDateTime.now());

            Map<String, Object> params = new HashMap<>();
            params.put("numberOfVariations", numberOfVariations);
            params.put("language", language);
            params.put("callToAction", callToAction);
            failedRequest.setRequestParameters(params);

            deadLetterQueueService.addFailedRequest(failedRequest);

        } catch (Exception e) {
            log.error("Error adding request to dead letter queue", e);
        }
    }

    /**
     * Determine if exception is retryable
     */
    private boolean isRetryableException(Exception exception) {
        if (exception instanceof AIProviderException) {
            return ((AIProviderException) exception).isRetryable();
        }

        String message = exception.getMessage().toLowerCase();
        return message.contains("timeout") ||
               message.contains("network") ||
               message.contains("rate limit") ||
               message.contains("service unavailable");
    }

    /**
     * Hash prompt for caching
     */
    private String hashPrompt(String prompt) {
        return String.valueOf(prompt.hashCode());
    }

    /**
     * Get provider health status
     */
    public Map<String, ProviderHealth> getProviderHealthStatus() {
        Map<String, ProviderHealth> healthStatus = new HashMap<>();

        for (String providerName : PROVIDER_PRIORITY) {
            ProviderHealth health = new ProviderHealth();
            health.setProviderName(providerName);
            health.setAvailable(aiProviders.containsKey(providerName));
            health.setCircuitOpen(isProviderCircuitOpen(providerName));

            if (health.isCircuitOpen()) {
                health.setStatus("CIRCUIT_OPEN");
            } else if (health.isAvailable()) {
                health.setStatus("AVAILABLE");
            } else {
                health.setStatus("UNAVAILABLE");
            }

            healthStatus.put(providerName, health);
        }

        return healthStatus;
    }

    // Data classes
    public static class CircuitBreakerState {
        private int failureCount = 0;
        private boolean open = false;
        private LocalDateTime openUntil;

        public void recordFailure() {
            failureCount++;
        }

        public void openCircuit(LocalDateTime until) {
            this.open = true;
            this.openUntil = until;
        }

        // Getters and setters
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

        public boolean isOpen() { return open; }
        public void setOpen(boolean open) { this.open = open; }

        public LocalDateTime getOpenUntil() { return openUntil; }
        public void setOpenUntil(LocalDateTime openUntil) { this.openUntil = openUntil; }
    }

    public static class ProviderHealth {
        private String providerName;
        private boolean available;
        private boolean circuitOpen;
        private String status;

        // Getters and setters
        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        public boolean isCircuitOpen() { return circuitOpen; }
        public void setCircuitOpen(boolean circuitOpen) { this.circuitOpen = circuitOpen; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}