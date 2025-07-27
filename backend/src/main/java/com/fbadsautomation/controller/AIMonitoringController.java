package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.service.AIContentCacheService;
import com.fbadsautomation.service.AIProviderService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai/monitoring")
@RequiredArgsConstructor

public class AIMonitoringController {

    private final AIProviderService aiProviderService;
    private final AIContentCacheService cacheService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get AI provider statistics
     */
    @GetMapping("/providers/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderStats(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        try {
            Map<String, Object> stats = new HashMap<>(); // Get stats for all known providers
            List<String> providers = List.of("openai", "gemini", "anthropic", "huggingface", "fal-ai", "stable-diffusion");
            
            Map<String, AIContentCacheService.ProviderStats> providerStats = new HashMap<>();
            for (String provider : providers) {
                AIContentCacheService.ProviderStats providerStat = aiProviderService.getProviderStats(provider, hoursBack);
                providerStats.put(provider, providerStat);
            }
            
            stats.put("providers", providerStats);
            stats.put("timeRange", hoursBack + " hours");
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("Provider statistics retrieved successfully", stats));
            
        } catch (Exception e) {
            log.error("Error retrieving provider stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to retrieve provider statistics", null));
        }
    }

    /**
     * Get circuit breaker status for all providers
     */
    @GetMapping("/circuit-breakers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCircuitBreakerStatus() {
        
        try {
            Map<String, Object> circuitBreakerStatus = circuitBreakerRegistry.getAllCircuitBreakers()
                    .asJava()
                    .stream()
                    .collect(Collectors.toMap(
                            CircuitBreaker::getName,
                            cb -> {
                                Map<String, Object> status = new HashMap<>();
                                status.put("state", cb.getState().toString());
                                status.put("failureRate", cb.getMetrics().getFailureRate());
                                status.put("slowCallRate", cb.getMetrics().getSlowCallRate());
                                status.put("numberOfCalls", cb.getMetrics().getNumberOfBufferedCalls());
                                status.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
                                status.put("numberOfSlowCalls", cb.getMetrics().getNumberOfSlowCalls());
                                return status;
                            }
                    ));

            Map<String, Object> response = new HashMap<>();
            response.put("circuitBreakers", circuitBreakerStatus);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success("Circuit breaker status retrieved successfully", response));
            
        } catch (Exception e) {
            log.error("Error retrieving circuit breaker status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to retrieve circuit breaker status", null));
        }
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats() {
        
        try {
            // Note: This is a simplified implementation
            // In a real application, you might want to implement cache metrics collection
            Map<String, Object> cacheStats = new HashMap<>();
            cacheStats.put("message", "Cache statistics would be implemented with Redis metrics");
            cacheStats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("Cache statistics retrieved successfully", cacheStats));
            
        } catch (Exception e) {
            log.error("Error retrieving cache stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to retrieve cache statistics", null));
        }
    }

    /**
     * Clear cache for specific provider
     */
    @DeleteMapping("/cache/provider/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearProviderCache(@PathVariable String providerId) {
        
        try {
            aiProviderService.clearProviderCache(providerId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Cache cleared successfully for provider: " + providerId, 
                    "Cache cleared"));
            
        } catch (Exception e) {
            log.error("Error clearing provider cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to clear provider cache", null));
        }
    }

    /**
     * Clear all AI content cache
     */
    @DeleteMapping("/cache/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearAllCache() {
        
        try {
            cacheService.clearAllCache();
            
            return ResponseEntity.ok(ApiResponse.success("All AI content cache cleared successfully", "Cache cleared"));
            
        } catch (Exception e) {
            log.error("Error clearing all cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to clear all cache", null));
        }
    }

    /**
     * Get provider health status
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderHealth() {
        
        try {
            Map<String, Object> health = new HashMap<>(); // Check circuit breaker states to determine health
            Map<String, String> providerHealth = circuitBreakerRegistry.getAllCircuitBreakers()
                    .asJava()
                    .stream()
                    .collect(Collectors.toMap(
                            CircuitBreaker::getName,
                            cb -> {
                                switch (cb.getState()) {
                                    case CLOSED:
                                        return "HEALTHY";
                                    case OPEN:
                                        return "UNHEALTHY";
                                    case HALF_OPEN:
                                        return "RECOVERING";
                                    default:
                                        return "UNKNOWN";
                                }
                            }
                    ));

            health.put("providers", providerHealth);
            health.put("overallStatus", determineOverallHealth(providerHealth));
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success("Provider health status retrieved successfully", health));
            
        } catch (Exception e) {
            log.error("Error retrieving provider health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to retrieve provider health", null));
        }
    }

    /**
     * Get cost analysis for AI providers
     */
    @GetMapping("/costs")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCostAnalysis(
            @RequestParam(defaultValue = "24") int hoursBack) {
        
        try {
            Map<String, Object> costAnalysis = new HashMap<>();
            List<String> providers = List.of("openai", "gemini", "anthropic", "huggingface", "fal-ai", "stable-diffusion");
            
            double totalCost = 0.0;
            Map<String, Double> providerCosts = new HashMap<>();
            
            for (String provider : providers) {
                AIContentCacheService.ProviderStats stats = aiProviderService.getProviderStats(provider, hoursBack);
                double cost = stats.getTotalCost();
                providerCosts.put(provider, cost);
                totalCost += cost;
            }
            
            costAnalysis.put("totalCost", totalCost);
            costAnalysis.put("providerCosts", providerCosts);
            costAnalysis.put("timeRange", hoursBack + " hours");
            costAnalysis.put("currency", "USD");
            costAnalysis.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("Cost analysis retrieved successfully", costAnalysis));
            
        } catch (Exception e) {
            log.error("Error retrieving cost analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to retrieve cost analysis", null));
        }
    }

    /**
     * Force circuit breaker state transition (for testing/maintenance)
     */
    @PostMapping("/circuit-breaker/{providerId}/transition/{state}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> forceCircuitBreakerTransition(
            @PathVariable String providerId,
            @PathVariable String state) {
        
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerId);
            switch (state.toUpperCase()) {
                case "OPEN":
                    circuitBreaker.transitionToOpenState();
                    break;
                case "CLOSED":
                    circuitBreaker.transitionToClosedState();
                    break;
                case "HALF_OPEN":
                    circuitBreaker.transitionToHalfOpenState();
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.success("Invalid state. Use: OPEN, CLOSED, or HALF_OPEN", null));
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Circuit breaker for %s transitioned to %s", providerId, state),
                    "State changed"));
            
        } catch (Exception e) {
            log.error("Error forcing circuit breaker transition: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("Failed to change circuit breaker state", null));
        }
    }

    /**
     * Determine overall health status
     */
    private String determineOverallHealth(Map<String, String> providerHealth) {
        long healthyCount = providerHealth.values().stream()
                .filter(status -> "HEALTHY".equals(status))
                .count();
        long totalCount = providerHealth.size();
        
        if (healthyCount == totalCount) {
            return "ALL_HEALTHY";
        } else if (healthyCount > totalCount / 2) {
            return "MOSTLY_HEALTHY";
        } else if (healthyCount > 0) {
            return "PARTIALLY_HEALTHY";
        } else {
            return "UNHEALTHY";
        }
    }
}
