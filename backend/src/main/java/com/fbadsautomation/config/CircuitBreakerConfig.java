package com.fbadsautomation.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration

public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default circuit breaker configuration
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate threshold
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Minimum 5 calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .slowCallRateThreshold(50) // 50% slow call rate threshold
                .slowCallDurationThreshold(Duration.ofSeconds(10)) // Calls slower than 10s are considered slow
                .recordExceptions(Exception.class) // Record all exceptions as failures
                .ignoreExceptions() // Don't ignore any exceptions by default
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        
        // Create specific circuit breakers for each AI provider
        createAIProviderCircuitBreakers(registry);
        
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        // Exponential backoff retry configuration
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3) // Maximum 3 retry attempts
                .intervalFunction(attempt -> Duration.ofSeconds((long) Math.pow(2, attempt)).toMillis()) // Exponential backoff
                .retryOnException(throwable -> {
                    // Retry on specific exceptions
                    return throwable instanceof java.net.SocketTimeoutException ||
                           throwable instanceof java.net.ConnectException ||
                           throwable instanceof org.springframework.web.client.ResourceAccessException ||
                           (throwable instanceof org.springframework.web.client.HttpServerErrorException &&
                            ((org.springframework.web.client.HttpServerErrorException) throwable).getStatusCode().is5xxServerError()); })
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig); // Create specific retry policies for different AI providers
        createAIProviderRetryPolicies(registry);
        
        return registry;
    }

    private void createAIProviderCircuitBreakers(CircuitBreakerRegistry registry) {
        // OpenAI Circuit Breaker - More lenient due to higher reliability
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig openaiConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(60) // 60% failure rate threshold
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait 1 minute
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .slowCallDurationThreshold(Duration.ofSeconds(15))
                .build();
                registry.circuitBreaker("openai", openaiConfig);

        // Gemini Circuit Breaker
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig geminiConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .slowCallDurationThreshold(Duration.ofSeconds(12))
                .build();
                registry.circuitBreaker("gemini", geminiConfig);

        // Anthropic Circuit Breaker
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig anthropicConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(55)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(12)
                .minimumNumberOfCalls(6)
                .slowCallDurationThreshold(Duration.ofSeconds(20))
                .build();
                registry.circuitBreaker("anthropic", anthropicConfig);

        // HuggingFace Circuit Breaker - More strict due to potential instability
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig huggingfaceConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(40)
                .waitDurationInOpenState(Duration.ofSeconds(120)) // Wait 2 minutes
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .slowCallDurationThreshold(Duration.ofSeconds(30))
                .build();
                registry.circuitBreaker("huggingface", huggingfaceConfig);

        // Fal.ai Circuit Breaker - For image generation
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig falaiConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(45)
                .waitDurationInOpenState(Duration.ofSeconds(90))
                .slidingWindowSize(8)
                .minimumNumberOfCalls(4)
                .slowCallDurationThreshold(Duration.ofSeconds(25))
                .build();
                registry.circuitBreaker("fal-ai", falaiConfig);

        // Add event listeners for monitoring
        registry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> 
                        log.info("Circuit breaker '{}' state transition: {} -> {}", event.getCircuitBreakerName(), event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                    .onFailureRateExceeded(event -> 
                        log.warn("Circuit breaker '{}' failure rate exceeded: {}%", event.getCircuitBreakerName(), event.getFailureRate()))
                    .onSlowCallRateExceeded(event -> 
                        log.warn("Circuit breaker '{}' slow call rate exceeded: {}%", event.getCircuitBreakerName(), event.getSlowCallRate()));
        });
    }

    private void createAIProviderRetryPolicies(RetryRegistry registry) {
        // OpenAI Retry - Quick retries due to good reliability
        RetryConfig openaiRetryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(attempt -> Duration.ofMillis(500 * attempt).toMillis())
                .build();
                registry.retry("openai", openaiRetryConfig);

        // Gemini Retry
        RetryConfig geminiRetryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(attempt -> Duration.ofSeconds(attempt).toMillis())
                .build();
                registry.retry("gemini", geminiRetryConfig);

        // Anthropic Retry
        RetryConfig anthropicRetryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(attempt -> Duration.ofSeconds(2 * attempt).toMillis())
                .build();
                registry.retry("anthropic", anthropicRetryConfig);

        // HuggingFace Retry - More aggressive backoff
        RetryConfig huggingfaceRetryConfig = RetryConfig.custom()
                .maxAttempts(4)
                .intervalFunction(attempt -> Duration.ofSeconds((long) Math.pow(2, attempt)).toMillis())
                .build();
                registry.retry("huggingface", huggingfaceRetryConfig);

        // Fal.ai Retry - For image generation
        RetryConfig falaiRetryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(attempt -> Duration.ofSeconds(3 * attempt).toMillis())
                .build();
                registry.retry("fal-ai", falaiRetryConfig);

        // Add event listeners for monitoring
        registry.getAllRetries().forEach(retry -> {
            retry.getEventPublisher()
                    .onRetry(event -> 
                        log.info("Retry '{}' attempt #{}: {}", event.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                    .onSuccess(event -> 
                        log.debug("Retry '{}' succeeded after {} attempts", event.getName(), event.getNumberOfRetryAttempts()));
        });
    }
}
