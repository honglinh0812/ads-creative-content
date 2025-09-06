package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ApiResponse;
import com.fbadsautomation.service.DatabasePerformanceService;
import io.swagger.v3.oas.annotations.Operation;
// Removed conflicting import - using fully qualified name instead
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/database/performance")
@Tag(name = "Database Performance", description = "Database performance monitoring and optimization endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DatabasePerformanceController {

    private static final Logger log = LoggerFactory.getLogger(DatabasePerformanceController.class);
    
    private final DatabasePerformanceService performanceService;

    @Autowired
    public DatabasePerformanceController(DatabasePerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Operation(summary = "Get performance report", description = "Retrieves comprehensive database performance report (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Performance report generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceReport() {
        try {
            Map<String, Object> report = performanceService.getPerformanceReport();
            return ResponseEntity.ok(ApiResponse.success("Performance report generated successfully", report));
        } catch (Exception e) {
            log.error("Error generating performance report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate performance report"));
        }
    }

    @Operation(summary = "Get connection pool statistics", description = "Retrieves database connection pool statistics (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connection pool statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/connection-pool")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionPoolStats() {
        try {
            Map<String, Object> stats = performanceService.getConnectionPoolStats();
            return ResponseEntity.ok(ApiResponse.success("Connection pool statistics retrieved", stats));
        } catch (Exception e) {
            log.error("Error getting connection pool stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get connection pool statistics"));
        }
    }

    @Operation(summary = "Get slow queries", description = "Retrieves list of slow database queries (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Slow queries retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/slow-queries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSlowQueries() {
        try {
            List<Map<String, Object>> slowQueries = performanceService.getSlowQueries();
            return ResponseEntity.ok(ApiResponse.success("Slow queries retrieved", slowQueries));
        } catch (Exception e) {
            log.error("Error getting slow queries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get slow queries"));
        }
    }

    /**
     * Get index usage statistics
     */
    @GetMapping("/index-usage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getIndexUsageStats() {
        try {
            List<Map<String, Object>> indexStats = performanceService.getIndexUsageStats();
            return ResponseEntity.ok(ApiResponse.success("Index usage statistics retrieved", indexStats));
        } catch (Exception e) {
            log.error("Error getting index usage stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get index usage statistics"));
        }
    }

    /**
     * Get table statistics
     */
    @GetMapping("/table-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTableStats() {
        try {
            List<Map<String, Object>> tableStats = performanceService.getTableStats();
            return ResponseEntity.ok(ApiResponse.success("Table statistics retrieved", tableStats));
        } catch (Exception e) {
            log.error("Error getting table stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get table statistics"));
        }
    }

    /**
     * Get cache hit ratio
     */
    @GetMapping("/cache-hit-ratio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheHitRatio() {
        try {
            Map<String, Object> cacheStats = performanceService.getCacheHitRatio();
            return ResponseEntity.ok(ApiResponse.success("Cache hit ratio retrieved", cacheStats));
        } catch (Exception e) {
            log.error("Error getting cache hit ratio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get cache hit ratio"));
        }
    }

    /**
     * Get database size information
     */
    @GetMapping("/database-size")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseSize() {
        try {
            Map<String, Object> sizeInfo = performanceService.getDatabaseSize();
            return ResponseEntity.ok(ApiResponse.success("Database size information retrieved", sizeInfo));
        } catch (Exception e) {
            log.error("Error getting database size: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get database size information"));
        }
    }

    /**
     * Get table sizes
     */
    @GetMapping("/table-sizes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTableSizes() {
        try {
            List<Map<String, Object>> tableSizes = performanceService.getTableSizes();
            return ResponseEntity.ok(ApiResponse.success("Table sizes retrieved", tableSizes));
        } catch (Exception e) {
            log.error("Error getting table sizes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get table sizes"));
        }
    }

    /**
     * Get lock information
     */
    @GetMapping("/locks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLockInfo() {
        try {
            List<Map<String, Object>> lockInfo = performanceService.getLockInfo();
            return ResponseEntity.ok(ApiResponse.success("Lock information retrieved", lockInfo));
        } catch (Exception e) {
            log.error("Error getting lock info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get lock information"));
        }
    }

    /**
     * Get maintenance recommendations
     */
    @GetMapping("/maintenance-recommendations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMaintenanceRecommendations() {
        try {
            Map<String, Object> recommendations = performanceService.getMaintenanceRecommendations();
            return ResponseEntity.ok(ApiResponse.success("Maintenance recommendations retrieved", recommendations));
        } catch (Exception e) {
            log.error("Error getting maintenance recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get maintenance recommendations"));
        }
    }

    /**
     * Analyze table statistics
     */
    @PostMapping("/analyze-tables")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> analyzeTableStatistics() {
        try {
            performanceService.analyzeTableStatistics();
            return ResponseEntity.ok(ApiResponse.success("Table statistics analysis completed", "Analysis completed"));
        } catch (Exception e) {
            log.error("Error analyzing table statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to analyze table statistics"));
        }
    }

    /**
     * Get database health status
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseHealth() {
        try {
            Map<String, Object> connectionStats = performanceService.getConnectionPoolStats();
            Map<String, Object> cacheStats = performanceService.getCacheHitRatio();
            List<Map<String, Object>> lockInfo = performanceService.getLockInfo();
            
            // Determine health status
            String healthStatus = "HEALTHY";
            List<String> issues = List.of();
            
            // Check connection pool
            Object activeConnections = connectionStats.get("active_connections");
            Object maxConnections = connectionStats.get("max_connections");
            if (activeConnections != null && maxConnections != null) {
                int active = ((Number) activeConnections).intValue();
                int max = ((Number) maxConnections).intValue();
                if (active > max * 0.8) {
                    healthStatus = "WARNING";
                    issues.add("High connection pool usage: " + active + "/" + max);
        }
    }
            
            // Check cache hit ratio
            Object hitRatio = cacheStats.get("cache_hit_ratio");
            if (hitRatio != null && ((Number) hitRatio).doubleValue() < 95.0) {
                healthStatus = "WARNING";
                issues.add("Low cache hit ratio: " + hitRatio + "%");
            }
            
            // Check for locks
            if (!lockInfo.isEmpty()) {
                healthStatus = "WARNING";
                issues.add("Active locks detected: " + lockInfo.size());
            }
            
            Map<String, Object> health = Map.of(
                "status", healthStatus,
                "issues", issues,
                "connection_pool", connectionStats,
                "cache_stats", cacheStats,
                "active_locks", lockInfo.size(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Database health status retrieved", health));
        } catch (Exception e) {
            log.error("Error getting database health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get database health status"));
        }
    }
}
