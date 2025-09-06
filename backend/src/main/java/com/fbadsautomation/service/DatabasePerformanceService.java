package com.fbadsautomation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabasePerformanceService {
    
    private static final Logger log = LoggerFactory.getLogger(DatabasePerformanceService.class);

    private final JdbcTemplate jdbcTemplate;
    
    public DatabasePerformanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get database connection pool statistics
     */
    public Map<String, Object> getConnectionPoolStats() {
        try {
            String sql = """
SELECT 
    numbackends as active_connections,
    numbackends - (SELECT count(*) FROM pg_stat_activity WHERE state = 'idle') as busy_connections,
    (SELECT count(*) FROM pg_stat_activity WHERE state = 'idle') as idle_connections,
    (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') as max_connections
FROM pg_stat_database 
WHERE datname = current_database();
""";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            if (!result.isEmpty()) {
                Map<String, Object> stats = result.get(0);
                log.info("Connection pool stats: {}", stats);
                return stats;
            }
        } catch (Exception e) {
            log.error("Error getting connection pool stats: {}", e.getMessage(), e);
        }
        return Map.of();
    }

    /**
     * Get slow query statistics
     */
    public List<Map<String, Object>> getSlowQueries() {
        try {
            String sql = """
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif (shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
WHERE mean_time > 100 -- queries slower than 100ms
ORDER BY mean_time DESC 
LIMIT 10
""";
            List<Map<String, Object>> slowQueries = jdbcTemplate.queryForList(sql);
            log.info("Found {} slow queries", slowQueries.size());
            return slowQueries;
        } catch (Exception e) {
            log.warn("pg_stat_statements extension not available or error getting slow queries: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get index usage statistics
     */
    public List<Map<String, Object>> getIndexUsageStats() {
        try {
            String sql = """
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    CASE 
        WHEN idx_scan = 0 THEN 'UNUSED'
        WHEN idx_scan < 100 THEN 'LOW_USAGE'
        ELSE 'ACTIVE'
    END as usage_status
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC
""";
            List<Map<String, Object>> indexStats = jdbcTemplate.queryForList(sql);
            log.info("Retrieved index usage stats for {} indexes", indexStats.size());
            return indexStats;
        } catch (Exception e) {
            log.error("Error getting index usage stats: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get table statistics
     */
    public List<Map<String, Object>> getTableStats() {
        try {
            String sql = """
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables 
WHERE schemaname = 'public'
ORDER BY n_live_tup DESC
""";
            List<Map<String, Object>> tableStats = jdbcTemplate.queryForList(sql);
            log.info("Retrieved table stats for {} tables", tableStats.size());
            return tableStats;
        } catch (Exception e) {
            log.error("Error getting table stats: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get cache hit ratio
     */
    public Map<String, Object> getCacheHitRatio() {
        try {
            String sql = """
SELECT 
    sum(heap_blks_read) as heap_read,
    sum(heap_blks_hit) as heap_hit,
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) * 100 as cache_hit_ratio
FROM pg_statio_user_tables
""";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            if (!result.isEmpty()) {
                Map<String, Object> cacheStats = result.get(0);
                log.info("Cache hit ratio: {}%", cacheStats.get("cache_hit_ratio"));
                return cacheStats;
            }
        } catch (Exception e) {
            log.error("Error getting cache hit ratio: {}", e.getMessage(), e);
        }
        return Map.of();
    }

    /**
     * Get database size information
     */
    public Map<String, Object> getDatabaseSize() {
        try {
            String sql = """
SELECT 
    pg_size_pretty(pg_database_size(current_database())) as database_size,
    (SELECT pg_size_pretty(sum(pg_total_relation_size(c.oid))) 
     FROM pg_class c 
     LEFT JOIN pg_namespace n ON (n.oid = c.relnamespace) 
     WHERE nspname = 'public') as public_schema_size
""";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            if (!result.isEmpty()) {
                Map<String, Object> sizeInfo = result.get(0);
                log.info("Database size info: {}", sizeInfo);
                return sizeInfo;
            }
        } catch (Exception e) {
            log.error("Error getting database size: {}", e.getMessage(), e);
        }
        return Map.of();
    }

    /**
     * Get table sizes
     */
    public List<Map<String, Object>> getTableSizes() {
        try {
            String sql = """
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
""";
            List<Map<String, Object>> tableSizes = jdbcTemplate.queryForList(sql);
            log.info("Retrieved size info for {} tables", tableSizes.size());
            return tableSizes;
        } catch (Exception e) {
            log.error("Error getting table sizes: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get lock information
     */
    public List<Map<String, Object>> getLockInfo() {
        try {
            String sql = """
SELECT 
    pg_class.relname,
    pg_locks.locktype,
    pg_locks.mode,
    pg_locks.granted,
    pg_stat_activity.query,
    pg_stat_activity.query_start,
    pg_stat_activity.state
FROM pg_locks
JOIN pg_class ON pg_locks.relation = pg_class.oid
JOIN pg_stat_activity ON pg_locks.pid = pg_stat_activity.pid
WHERE pg_class.relname IN ('users', 'campaigns', 'ads', 'ad_contents')
AND NOT pg_locks.granted
ORDER BY pg_stat_activity.query_start
""";
            List<Map<String, Object>> lockInfo = jdbcTemplate.queryForList(sql);
            if (!lockInfo.isEmpty()) {
                log.warn("Found {} active locks", lockInfo.size());
            }
            return lockInfo;
        } catch (Exception e) {
            log.error("Error getting lock info: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Analyze table statistics (should be run periodically)
     */
    public void analyzeTableStatistics() {
        try {
            log.info("Starting table statistics analysis...");
            String[] tables = {"users", "campaigns", "ads", "ad_contents"};
            for (String table : tables) {
                try {
                    jdbcTemplate.execute("ANALYZE " + table);
                    log.info("Analyzed table: {}", table);
                } catch (Exception e) {
                    log.error("Error analyzing table {}: {}", table, e.getMessage());
                }
            }
            log.info("Completed table statistics analysis");
        } catch (Exception e) {
            log.error("Error during table analysis: {}", e.getMessage(), e);
        }
    }

    /**
     * Get comprehensive performance report
     */
    public Map<String, Object> getPerformanceReport() {
        try {
            log.info("Generating comprehensive performance report...");
            return Map.of(
                "connection_pool", getConnectionPoolStats(),
                "cache_hit_ratio", getCacheHitRatio(),
                "database_size", getDatabaseSize(),
                "table_stats", getTableStats(),
                "table_sizes", getTableSizes(),
                "index_usage", getIndexUsageStats(),
                "slow_queries", getSlowQueries(),
                "locks", getLockInfo(),
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("Error generating performance report: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis());
        }
    }

    /**
     * Check if database maintenance is needed
     */
    public Map<String, Object> getMaintenanceRecommendations() {
        try {
            List<String> recommendations = new java.util.ArrayList<>();
            // Check cache hit ratio
            Map<String, Object> cacheStats = getCacheHitRatio();
            Object hitRatio = cacheStats.get("cache_hit_ratio");
            if (hitRatio != null && ((Number) hitRatio).doubleValue() < 95.0) {
                recommendations.add("Cache hit ratio is below 95%. Consider increasing shared_buffers.");
            }
            // Check for unused indexes
            List<Map<String, Object>> indexStats = getIndexUsageStats();
            long unusedIndexes = indexStats.stream()
                    .filter(idx -> "UNUSED".equals(idx.get("usage_status")))
                    .count();
            if (unusedIndexes > 0) {
                recommendations.add("Found " + unusedIndexes + " unused indexes. Consider dropping them.");
            }
            // Check table sizes
            List<Map<String, Object>> tableSizes = getTableSizes();
            for (Map<String, Object> table : tableSizes) {
                Long sizeBytes = null;
                Object sizeBytesObj = table.get("size_bytes");
                if (sizeBytesObj instanceof Number) {
                    sizeBytes = ((Number) sizeBytesObj).longValue();
                }
                if (sizeBytes != null && sizeBytes > 100_000_000) { // > 100MB
                    recommendations.add("Table " + table.get("tablename") + " is large (" + 
                                      table.get("size") + "). Consider partitioning or archiving.");
                }
            }
            return Map.of(
                "recommendations", recommendations,
                "maintenance_needed", !recommendations.isEmpty(),
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("Error generating maintenance recommendations: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis());
        }
    }
}
