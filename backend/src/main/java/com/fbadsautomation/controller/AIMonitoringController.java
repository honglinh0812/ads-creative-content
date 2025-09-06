package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.service.AIContentCacheService;
import com.fbadsautomation.service.AIProviderService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
// Removed conflicting import - using fully qualified name instead
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/monitoring")
@Tag(name = "AI Monitoring", description = "AI provider monitoring and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AIMonitoringController {

    private static final Logger log = LoggerFactory.getLogger(AIMonitoringController.class);

    private final AIProviderService aiProviderService;
    private final AIContentCacheService cacheService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    public AIMonitoringController(AIProviderService aiProviderService, 
                                 AIContentCacheService cacheService,
                                 CircuitBreakerRegistry circuitBreakerRegistry) {
        this.aiProviderService = aiProviderService;
        this.cacheService = cacheService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Operation(summary = "Get AI provider statistics", description = "Retrieves statistics for all AI providers")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/providers/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderStats(
            @Parameter(description = "Number of hours to look back for statistics") @RequestParam(defaultValue = "24") int hoursBack) {
        
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

    @Operation(summary = "Get circuit breaker status", description = "Retrieves circuit breaker status for all AI providers")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circuit breaker status retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

    @Operation(summary = "Get cache statistics", description = "Retrieves AI content cache statistics")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

    @Operation(summary = "Clear provider cache", description = "Clears cache for a specific AI provider")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider cache cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/cache/provider/{providerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearProviderCache(
            @Parameter(description = "ID of the AI provider") @PathVariable String providerId) {
        
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

    @Operation(summary = "Clear all cache", description = "Clears all AI content cache")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All cache cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

    @Operation(summary = "Get provider health status", description = "Retrieves health status of all AI providers")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Provider health status retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

    @Operation(summary = "Get cost analysis", description = "Retrieves cost analysis for AI providers")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cost analysis retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/costs")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCostAnalysis(
            @Parameter(description = "Number of hours to look back for cost analysis") @RequestParam(defaultValue = "24") int hoursBack) {
        
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

    @Operation(summary = "Force circuit breaker transition", description = "Forces circuit breaker state transition for testing/maintenance")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circuit breaker state changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid state parameter"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/circuit-breaker/{providerId}/transition/{state}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> forceCircuitBreakerTransition(
            @Parameter(description = "ID of the AI provider") @PathVariable String providerId,
            @Parameter(description = "Target state (OPEN, CLOSED, HALF_OPEN)") @PathVariable String state) {
        
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
