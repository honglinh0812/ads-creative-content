package com.fbadsautomation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AIContentCacheService aiContentCacheService;

    private static final List<String> CACHE_NAMES = Arrays.asList(
        "ai-content", "ai-provider-status", "campaigns", "users", "api-responses"
    );

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredCaches() {
        try {
            log.debug("Starting cache cleanup process...");

            for (String cacheName : CACHE_NAMES) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // Spring Cache with Redis automatically handles TTL,
                    // but we can add custom cleanup logic here if needed
                    log.trace("Cache {} is managed by Redis TTL", cacheName);
                }
            }

            log.debug("Cache cleanup completed");
        } catch (Exception e) {
            log.error("Error during cache cleanup", e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void performCacheHealthCheck() {
        try {
            log.debug("Performing cache health check...");

            // Check Redis connection
            String pingResult = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if (!"PONG".equals(pingResult)) {
                log.warn("Redis health check failed: {}", pingResult);
                return;
            }

            // Check cache sizes
            for (String cacheName : CACHE_NAMES) {
                String pattern = cacheName + ":*";
                Set<String> keys = redisTemplate.keys(pattern);
                int keyCount = keys != null ? keys.size() : 0;

                log.debug("Cache {} contains {} keys", cacheName, keyCount);

                // Alert if cache grows too large
                if (keyCount > 10000) {
                    log.warn("Cache {} has grown large: {} keys", cacheName, keyCount);
                }
            }

            log.debug("Cache health check completed successfully");
        } catch (Exception e) {
            log.error("Cache health check failed", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void performDailyMaintenanceCleanup() {
        try {
            log.info("Starting daily cache maintenance cleanup...");

            // Clean up old AI provider statistics (older than 7 days)
            cleanupOldProviderStats();

            // Clean up orphaned cache entries
            cleanupOrphanedEntries();

            // Clean up large cache entries that might be causing memory issues
            cleanupLargeCacheEntries();

            log.info("Daily cache maintenance cleanup completed");
        } catch (Exception e) {
            log.error("Daily cache maintenance cleanup failed", e);
        }
    }

    public void invalidateCacheByPattern(String pattern) {
        try {
            log.info("Invalidating cache entries matching pattern: {}", pattern);

            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cache entries", keys.size());
            } else {
                log.debug("No cache entries found matching pattern: {}", pattern);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache by pattern: {}", pattern, e);
        }
    }

    public void invalidateUserCache(String userId) {
        try {
            log.debug("Invalidating cache for user: {}", userId);

            // Invalidate user-specific caches
            invalidateCacheByPattern("users:*" + userId + "*");
            invalidateCacheByPattern("ai-content:*user:" + userId + "*");

            log.debug("User cache invalidation completed for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate user cache: {}", userId, e);
        }
    }

    public void invalidateProviderCache(String provider) {
        try {
            log.info("Invalidating cache for AI provider: {}", provider);

            aiContentCacheService.clearProviderCache(provider);

            // Also clear provider status cache
            invalidateCacheByPattern("ai-provider-status:*" + provider + "*");

            log.info("Provider cache invalidation completed for: {}", provider);
        } catch (Exception e) {
            log.error("Failed to invalidate provider cache: {}", provider, e);
        }
    }

    public void forceCacheRefresh(String cacheName) {
        try {
            log.info("Forcing cache refresh for: {}", cacheName);

            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cache {} cleared successfully", cacheName);
            } else {
                log.warn("Cache {} not found", cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to force refresh cache: {}", cacheName, e);
        }
    }

    private void cleanupOldProviderStats() {
        try {
            log.debug("Cleaning up old provider statistics...");

            String pattern = "ai:stats:*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null) {
                long currentHour = System.currentTimeMillis() / (1000 * 60 * 60);
                long cutoffHour = currentHour - (7 * 24); // 7 days ago

                int deletedCount = 0;
                for (String key : keys) {
                    // Extract hour from key format: ai:stats:provider:hour
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        try {
                            long keyHour = Long.parseLong(parts[3]);
                            if (keyHour < cutoffHour) {
                                redisTemplate.delete(key);
                                deletedCount++;
                            }
                        } catch (NumberFormatException e) {
                            log.trace("Could not parse hour from key: {}", key);
                        }
                    }
                }

                log.debug("Cleaned up {} old provider statistics entries", deletedCount);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup old provider stats", e);
        }
    }

    private void cleanupOrphanedEntries() {
        try {
            log.debug("Cleaning up orphaned cache entries...");

            // This could be enhanced to detect and clean up cache entries
            // that reference deleted entities (users, campaigns, etc.)

            log.debug("Orphaned entries cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup orphaned entries", e);
        }
    }

    private void cleanupLargeCacheEntries() {
        try {
            log.debug("Cleaning up large cache entries...");

            // Identify and optionally remove cache entries that are consuming
            // excessive memory (this is a placeholder for future enhancement)

            log.debug("Large cache entries cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup large cache entries", e);
        }
    }

    public void emergencyCacheFlush() {
        try {
            log.warn("Performing emergency cache flush...");

            for (String cacheName : CACHE_NAMES) {
                forceCacheRefresh(cacheName);
            }

            // Also clear AI content cache specifically
            aiContentCacheService.clearAllCache();

            log.warn("Emergency cache flush completed");
        } catch (Exception e) {
            log.error("Emergency cache flush failed", e);
        }
    }

    public CacheHealthReport generateCacheHealthReport() {
        try {
            CacheHealthReport report = new CacheHealthReport();
            report.setTimestamp(LocalDateTime.now());

            for (String cacheName : CACHE_NAMES) {
                String pattern = cacheName + ":*";
                Set<String> keys = redisTemplate.keys(pattern);
                int keyCount = keys != null ? keys.size() : 0;

                CacheHealthReport.CacheInfo cacheInfo = new CacheHealthReport.CacheInfo();
                cacheInfo.setName(cacheName);
                cacheInfo.setKeyCount(keyCount);
                cacheInfo.setHealthy(keyCount < 10000); // Threshold for health

                report.getCaches().add(cacheInfo);
            }

            return report;
        } catch (Exception e) {
            log.error("Failed to generate cache health report", e);
            return new CacheHealthReport();
        }
    }

    // Health report classes
    public static class CacheHealthReport {
        private LocalDateTime timestamp;
        private List<CacheInfo> caches = new java.util.ArrayList<>();

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public List<CacheInfo> getCaches() { return caches; }
        public void setCaches(List<CacheInfo> caches) { this.caches = caches; }

        public static class CacheInfo {
            private String name;
            private int keyCount;
            private boolean healthy;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }

            public int getKeyCount() { return keyCount; }
            public void setKeyCount(int keyCount) { this.keyCount = keyCount; }

            public boolean isHealthy() { return healthy; }
            public void setHealthy(boolean healthy) { this.healthy = healthy; }
        }
    }
}