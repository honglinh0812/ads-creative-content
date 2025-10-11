package com.fbadsautomation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryStatisticsService {

    private final JdbcTemplate jdbcTemplate;

    // In-memory statistics for query tracking
    private final Map<String, AtomicLong> queryExecutionCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queryExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queryErrors = new ConcurrentHashMap<>();

    public void recordQueryExecution(String queryType, long executionTimeMs) {
        queryExecutionCounts.computeIfAbsent(queryType, k -> new AtomicLong(0)).incrementAndGet();
        queryExecutionTimes.computeIfAbsent(queryType, k -> new AtomicLong(0)).addAndGet(executionTimeMs);
    }

    public void recordQueryError(String queryType) {
        queryErrors.computeIfAbsent(queryType, k -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Object> getQueryStatistics() {
        Map<String, Object> stats = new HashMap<>();

        Map<String, Object> executionStats = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : queryExecutionCounts.entrySet()) {
            String queryType = entry.getKey();
            long count = entry.getValue().get();
            long totalTime = queryExecutionTimes.getOrDefault(queryType, new AtomicLong(0)).get();
            long errors = queryErrors.getOrDefault(queryType, new AtomicLong(0)).get();

            Map<String, Object> queryStats = new HashMap<>();
            queryStats.put("count", count);
            queryStats.put("totalTimeMs", totalTime);
            queryStats.put("averageTimeMs", count > 0 ? totalTime / count : 0);
            queryStats.put("errors", errors);

            executionStats.put(queryType, queryStats);
        }

        stats.put("applicationQueryStats", executionStats);

        // Get database-level statistics
        try {
            stats.put("databaseStats", getDatabaseQueryStats());
        } catch (Exception e) {
            log.error("Error getting database query stats", e);
            stats.put("databaseStatsError", e.getMessage());
        }

        return stats;
    }

    private Map<String, Object> getDatabaseQueryStats() {
        Map<String, Object> stats = new HashMap<>();

        // Get table access statistics
        String tableStatsSQL = """
            SELECT
                schemaname,
                relname as table_name,
                seq_scan,
                seq_tup_read,
                idx_scan,
                idx_tup_fetch,
                n_tup_ins,
                n_tup_upd,
                n_tup_del
            FROM pg_stat_user_tables
            WHERE schemaname = 'public'
            ORDER BY seq_tup_read + idx_tup_fetch DESC
            """;

        try {
            List<Map<String, Object>> tableStats = jdbcTemplate.queryForList(tableStatsSQL);
            stats.put("tableAccessStats", tableStats);
        } catch (Exception e) {
            log.error("Error getting table access stats", e);
        }

        // Get index usage statistics
        String indexStatsSQL = """
            SELECT
                schemaname,
                relname as table_name,
                indexrelname as index_name,
                idx_scan,
                idx_tup_read,
                idx_tup_fetch
            FROM pg_stat_user_indexes
            WHERE schemaname = 'public'
            AND idx_scan > 0
            ORDER BY idx_scan DESC
            """;

        try {
            List<Map<String, Object>> indexStats = jdbcTemplate.queryForList(indexStatsSQL);
            stats.put("indexUsageStats", indexStats);
        } catch (Exception e) {
            log.error("Error getting index usage stats", e);
        }

        // Get cache hit ratios
        String cacheHitSQL = """
            SELECT
                schemaname,
                relname as table_name,
                heap_blks_read,
                heap_blks_hit,
                CASE
                    WHEN heap_blks_read + heap_blks_hit = 0 THEN 0
                    ELSE ROUND(100.0 * heap_blks_hit / (heap_blks_read + heap_blks_hit), 2)
                END as cache_hit_ratio
            FROM pg_statio_user_tables
            WHERE schemaname = 'public'
            ORDER BY cache_hit_ratio DESC
            """;

        try {
            List<Map<String, Object>> cacheStats = jdbcTemplate.queryForList(cacheHitSQL);
            stats.put("cacheHitStats", cacheStats);
        } catch (Exception e) {
            log.error("Error getting cache hit stats", e);
        }

        // Get database-wide statistics
        String dbStatsSQL = """
            SELECT
                datname,
                numbackends,
                xact_commit,
                xact_rollback,
                blks_read,
                blks_hit,
                CASE
                    WHEN blks_read + blks_hit = 0 THEN 0
                    ELSE ROUND(100.0 * blks_hit / (blks_read + blks_hit), 2)
                END as overall_cache_hit_ratio,
                tup_returned,
                tup_fetched,
                tup_inserted,
                tup_updated,
                tup_deleted
            FROM pg_stat_database
            WHERE datname = current_database()
            """;

        try {
            List<Map<String, Object>> dbStats = jdbcTemplate.queryForList(dbStatsSQL);
            if (!dbStats.isEmpty()) {
                stats.put("databaseWideStats", dbStats.get(0));
            }
        } catch (Exception e) {
            log.error("Error getting database-wide stats", e);
        }

        return stats;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logQueryStatistics() {
        try {
            Map<String, Object> stats = getQueryStatistics();
            Map<String, Object> appStats = (Map<String, Object>) stats.get("applicationQueryStats");

            if (appStats != null && !appStats.isEmpty()) {
                log.info("📊 Application Query Statistics (5-minute window):");
                for (Map.Entry<String, Object> entry : appStats.entrySet()) {
                    Map<String, Object> queryStats = (Map<String, Object>) entry.getValue();
                    log.info("   {} - Count: {}, Avg Time: {}ms, Errors: {}",
                            entry.getKey(),
                            queryStats.get("count"),
                            queryStats.get("averageTimeMs"),
                            queryStats.get("errors"));
                }
            }

            // Log database cache hit ratio
            Map<String, Object> dbStats = (Map<String, Object>) stats.get("databaseWideStats");
            if (dbStats != null) {
                Object cacheHitRatio = dbStats.get("overall_cache_hit_ratio");
                if (cacheHitRatio != null) {
                    double ratio = ((Number) cacheHitRatio).doubleValue();
                    if (ratio < 95.0) {
                        log.warn("🚨 LOW DATABASE CACHE HIT RATIO: {}% (should be > 95%)", ratio);
                    } else {
                        log.info("✅ Database Cache Hit Ratio: {}%", ratio);
                    }
                }
            }

            // Reset counters for next window
            queryExecutionCounts.clear();
            queryExecutionTimes.clear();
            queryErrors.clear();

        } catch (Exception e) {
            log.error("Error logging query statistics", e);
        }
    }

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void logDetailedPerformanceStats() {
        try {
            Map<String, Object> stats = getDatabaseQueryStats();

            // Log top accessed tables
            List<Map<String, Object>> tableStats = (List<Map<String, Object>>) stats.get("tableAccessStats");
            if (tableStats != null && !tableStats.isEmpty()) {
                log.info("📊 Top 5 Most Accessed Tables:");
                for (int i = 0; i < Math.min(5, tableStats.size()); i++) {
                    Map<String, Object> table = tableStats.get(i);
                    log.info("   {}. {} - Seq: {}, Idx: {}, Total Tuples: {}",
                            i + 1,
                            table.get("table_name"),
                            table.get("seq_scan"),
                            table.get("idx_scan"),
                            ((Number) table.get("seq_tup_read")).longValue() +
                            ((Number) table.get("idx_tup_fetch")).longValue());
                }
            }

            // Log cache hit ratios by table
            List<Map<String, Object>> cacheStats = (List<Map<String, Object>>) stats.get("cacheHitStats");
            if (cacheStats != null && !cacheStats.isEmpty()) {
                log.info("📊 Table Cache Hit Ratios:");
                for (Map<String, Object> table : cacheStats) {
                    Object ratio = table.get("cache_hit_ratio");
                    if (ratio != null) {
                        double hitRatio = ((Number) ratio).doubleValue();
                        if (hitRatio < 90.0) {
                            log.warn("   ⚠️ {}: {}% (LOW)", table.get("table_name"), hitRatio);
                        } else {
                            log.info("   ✅ {}: {}%", table.get("table_name"), hitRatio);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error logging detailed performance stats", e);
        }
    }

    public void resetStatistics() {
        queryExecutionCounts.clear();
        queryExecutionTimes.clear();
        queryErrors.clear();
        log.info("Query statistics reset");
    }
}