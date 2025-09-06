package com.fbadsautomation.service;

import com.fbadsautomation.model.AdContent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
// import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AIContentCacheService {

    private static final Logger log = LoggerFactory.getLogger(AIContentCacheService.class);

    public AIContentCacheService() {
    }

    // private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String AI_CONTENT_PREFIX = "ai:content:";
    private static final String AI_IMAGE_PREFIX = "ai:image:";
    private static final String PROVIDER_STATS_PREFIX = "ai:stats:";
    /**
     * Generate cache key for AI content based on prompt and parameters
     */
    public String generateContentCacheKey(String prompt, String provider, int variations, String language, List<String> adLinks) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(prompt).append("|")
                 .append(provider).append("|")
                 .append(variations).append("|")
                 .append(language != null ? language : "en");
        if (adLinks != null && !adLinks.isEmpty()) {
            keyBuilder.append("|").append(String.join(",", adLinks));
        }
        
        return AI_CONTENT_PREFIX + hashString(keyBuilder.toString());
    }
    
    /**
     * Generate cache key for AI image based on prompt and provider
     */
    public String generateImageCacheKey(String prompt, String provider) {
        String keyData = prompt + "|" + provider;
        return AI_IMAGE_PREFIX + hashString(keyData);
    }
    
    /**
     * Cache AI generated content
     */
    public void cacheContent(String cacheKey, List<AdContent> content, Duration ttl) {
        try {
            // redisTemplate.opsForValue().set(cacheKey, content, ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("Cached AI content with key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Failed to cache AI content: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve cached AI content
     */
    @SuppressWarnings("unchecked")
    public List<AdContent> getCachedContent(String cacheKey) {
        try {
            // Object cached = redisTemplate.opsForValue().get(cacheKey);
            // if (cached instanceof List) {
            //     log.debug("Retrieved cached AI content with key: {}", cacheKey);
            //     return (List<AdContent>) cached;
            // }
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve cached AI content: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Cache AI generated image URL
     */
    public void cacheImage(String cacheKey, String imageUrl, Duration ttl) {
        try {
            // redisTemplate.opsForValue().set(cacheKey, imageUrl, ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("Cached AI image with key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Failed to cache AI image: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve cached AI image URL
     */
    public String getCachedImage(String cacheKey) {
        try {
            // Object cached = redisTemplate.opsForValue().get(cacheKey);
            // if (cached instanceof String) {
            //     log.debug("Retrieved cached AI image with key: {}", cacheKey);
            //     return (String) cached;
            // }
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve cached AI image: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Record AI provider usage statistics
     */
    public void recordProviderUsage(String provider, boolean success, long responseTimeMs, double cost) {
        try {
            // String statsKey = PROVIDER_STATS_PREFIX + provider + ":" + getCurrentHour(); // Increment counters
            // redisTemplate.opsForHash().increment(statsKey, "total_calls", 1);
            // if (success) {
            //     redisTemplate.opsForHash().increment(statsKey, "successful_calls", 1);
            // } else {
            //     redisTemplate.opsForHash().increment(statsKey, "failed_calls", 1);
            // }
            
            // // Update response time (moving average)
            // Object currentAvgTime = redisTemplate.opsForHash().get(statsKey, "avg_response_time");
            // Object currentCallCount = redisTemplate.opsForHash().get(statsKey, "total_calls");
            
            // if (currentAvgTime != null && currentCallCount != null) {
            //     double avgTime = Double.parseDouble(currentAvgTime.toString());
            //     long callCount = Long.parseLong(currentCallCount.toString());
            //     double newAvgTime = ((avgTime * (callCount - 1)) + responseTimeMs) / callCount;
            //     redisTemplate.opsForHash().put(statsKey, "avg_response_time", String.valueOf(newAvgTime));
            // } else {
            //     redisTemplate.opsForHash().put(statsKey, "avg_response_time", String.valueOf(responseTimeMs));
            // }
            
            // // Update cost
            // redisTemplate.opsForHash().increment(statsKey, "total_cost", cost);
            
            // // Set expiration for stats (keep for 7 days)
            // redisTemplate.expire(statsKey, Duration.ofDays(7));
            
        } catch (Exception e) {
            log.error("Failed to record provider usage stats: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get provider statistics for monitoring
     */
    public ProviderStats getProviderStats(String provider, int hoursBack) {
        try {
            ProviderStats stats = new ProviderStats();
            stats.setProvider(provider);
            
            long totalCalls = 0;
            long successfulCalls = 0;
            long failedCalls = 0;
            double totalCost = 0.0;
            double totalResponseTime = 0.0;
            int hourCount = 0;
            
            // for (int i = 0; i < hoursBack; i++) {
            //     String statsKey = PROVIDER_STATS_PREFIX + provider + ":" + (getCurrentHour() - i);
            //     if (redisTemplate.hasKey(statsKey)) {
            //         Object calls = redisTemplate.opsForHash().get(statsKey, "total_calls");
            //         Object successful = redisTemplate.opsForHash().get(statsKey, "successful_calls");
            //         Object failed = redisTemplate.opsForHash().get(statsKey, "failed_calls");
            //         Object cost = redisTemplate.opsForHash().get(statsKey, "total_cost");
            //         Object avgTime = redisTemplate.opsForHash().get(statsKey, "avg_response_time");
                    
            //         if (calls != null) {
            //             totalCalls += Long.parseLong(calls.toString());
            //             hourCount++;
            //         }
            //         if (successful != null) {
            //             successfulCalls += Long.parseLong(successful.toString());
            //         }
            //         if (failed != null) {
            //             failedCalls += Long.parseLong(failed.toString());
            //         }
            //         if (cost != null) {
            //             totalCost += Double.parseDouble(cost.toString());
            //         }
            //         if (avgTime != null) {
            //             totalResponseTime += Double.parseDouble(avgTime.toString());
            //         }
            //     }
            // }
            
            stats.setTotalCalls(totalCalls);
            stats.setSuccessfulCalls(successfulCalls);
            stats.setFailedCalls(failedCalls);
            stats.setSuccessRate(totalCalls > 0 ? (double) successfulCalls / totalCalls * 100 : 0.0);
            stats.setTotalCost(totalCost);
            stats.setAverageResponseTime(hourCount > 0 ? totalResponseTime / hourCount : 0.0);
            
            return stats;
            
        } catch (Exception e) {
            log.error("Failed to get provider stats: {}", e.getMessage(), e);
            return new ProviderStats();
        }
    }
    
    /**
     * Clear cache for specific provider (useful for maintenance)
     */
    @CacheEvict(value = "ai-content", allEntries = true)
    public void clearProviderCache(String provider) {
        try {
            // String pattern = AI_CONTENT_PREFIX + "*" + provider + "*";
            // redisTemplate.delete(redisTemplate.keys(pattern));
            log.info("Cleared cache for provider: {}", provider);
        } catch (Exception e) {
            log.error("Failed to clear provider cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear all AI content cache
     */
    @CacheEvict(value = "ai-content", allEntries = true)
    public void clearAllCache() {
        try {
            // redisTemplate.delete(redisTemplate.keys(AI_CONTENT_PREFIX + "*"));
            // redisTemplate.delete(redisTemplate.keys(AI_IMAGE_PREFIX + "*"));
            log.info("Cleared all AI content cache");
        } catch (Exception e) {
            log.error("Failed to clear all cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Hash string to create consistent cache keys
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to hash string: {}", e.getMessage(), e);
            return input.hashCode() + "";
        }
    }
    
    /**
     * Get current hour for stats tracking
     */
    private long getCurrentHour() {
        return System.currentTimeMillis() / (1000 * 60 * 60);
    }
    
    /**
     * Provider statistics data class
     */
    public static class ProviderStats {
        private String provider;
        private long totalCalls;
        private long successfulCalls;
        private long failedCalls;
        private double successRate;
        private double totalCost;
        private double averageResponseTime;
        
        public ProviderStats() {
            this.provider = "";
            this.totalCalls = 0;
            this.successfulCalls = 0;
            this.failedCalls = 0;
            this.successRate = 0.0;
            this.totalCost = 0.0;
            this.averageResponseTime = 0.0;
        }
        
        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public long getTotalCalls() { return totalCalls; }
        public void setTotalCalls(long totalCalls) { this.totalCalls = totalCalls; }
        
        public long getSuccessfulCalls() { return successfulCalls; }
        public void setSuccessfulCalls(long successfulCalls) { this.successfulCalls = successfulCalls; }
        
        public long getFailedCalls() { return failedCalls; }
        public void setFailedCalls(long failedCalls) { this.failedCalls = failedCalls; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    }
}
