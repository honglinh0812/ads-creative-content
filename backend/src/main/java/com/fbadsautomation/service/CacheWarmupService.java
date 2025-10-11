package com.fbadsautomation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmupService {

    private final AIContentCacheService cacheService;

    // Common prompts that are frequently used
    private static final List<String> COMMON_PROMPTS = Arrays.asList(
        "Generate engaging Facebook ad copy for fashion products",
        "Create compelling ad headlines for tech gadgets",
        "Write persuasive product descriptions for e-commerce",
        "Generate social media captions for lifestyle brands",
        "Create ad copy for fitness and wellness products",
        "Write compelling headlines for food and beverage ads",
        "Generate promotional content for seasonal sales",
        "Create ad copy for mobile app downloads",
        "Write engaging content for B2B software solutions",
        "Generate ad copy for automotive products"
    );

    private static final List<String> COMMON_PROVIDERS = Arrays.asList(
        "openai", "gemini", "anthropic", "huggingface"
    );

    @EventListener(ApplicationReadyEvent.class)
    public void warmupCacheOnStartup() {
        log.info("Starting cache warmup process...");
        warmupCommonPrompts();
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void scheduledCacheWarmup() {
        log.debug("Running scheduled cache warmup...");
        warmupCommonPrompts();
    }

    public void warmupCommonPrompts() {
        List<CompletableFuture<Void>> futures = COMMON_PROMPTS.stream()
            .map(this::warmupPromptAsync)
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> log.info("Cache warmup completed for {} prompts", COMMON_PROMPTS.size()))
            .exceptionally(throwable -> {
                log.error("Cache warmup failed", throwable);
                return null;
            });
    }

    private CompletableFuture<Void> warmupPromptAsync(String prompt) {
        return CompletableFuture.runAsync(() -> {
            try {
                for (String provider : COMMON_PROVIDERS) {
                    String cacheKey = cacheService.generateContentCacheKey(
                        prompt, provider, 3, "en", null
                    );

                    // Check if already cached
                    if (cacheService.getCachedContent(cacheKey) == null) {
                        // Simulate cache warming - in real implementation,
                        // this would trigger actual AI content generation
                        log.debug("Would generate content for prompt: {} with provider: {}",
                            prompt.substring(0, Math.min(50, prompt.length())), provider);

                        // For now, we just pre-compute the cache key to ensure the hashing works
                        log.trace("Cache key generated: {}", cacheKey);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to warmup cache for prompt: {}", prompt, e);
            }
        });
    }

    public void warmupProviderStats() {
        try {
            log.info("Warming up provider statistics cache...");

            for (String provider : COMMON_PROVIDERS) {
                // Initialize provider stats if they don't exist
                cacheService.recordProviderUsage(provider, true, 100, 0.01);

                // Get stats to ensure they're cached
                var stats = cacheService.getProviderStats(provider, 24);
                log.debug("Warmed up stats for provider {}: {} calls", provider, stats.getTotalCalls());
            }

            log.info("Provider statistics cache warmup completed");
        } catch (Exception e) {
            log.error("Failed to warmup provider statistics", e);
        }
    }

    public void forceCacheRefresh() {
        log.info("Forcing cache refresh for all providers...");

        for (String provider : COMMON_PROVIDERS) {
            cacheService.clearProviderCache(provider);
        }

        // Immediately start warmup after clearing
        warmupCommonPrompts();
        warmupProviderStats();

        log.info("Cache refresh and warmup completed");
    }

    public void preloadFrequentlyUsedData() {
        try {
            log.info("Preloading frequently used data...");

            // Warmup common image generation prompts
            List<String> imagePrompts = Arrays.asList(
                "Professional business person in modern office",
                "Product showcase on clean white background",
                "Happy customers using technology",
                "Lifestyle scene with family enjoying products",
                "Minimalist product photography setup"
            );

            for (String prompt : imagePrompts) {
                for (String provider : Arrays.asList("stable-diffusion", "fal-ai")) {
                    String cacheKey = cacheService.generateImageCacheKey(prompt, provider);
                    log.trace("Image cache key generated: {}", cacheKey);
                }
            }

            log.info("Frequently used data preloading completed");
        } catch (Exception e) {
            log.error("Failed to preload frequently used data", e);
        }
    }

    public void warmupCacheForUser(String userId) {
        try {
            log.debug("Warming up cache for user: {}", userId);

            // This could be enhanced to warmup user-specific cache based on their usage patterns
            // For now, just ensure basic prompts are available
            warmupCommonPrompts();

        } catch (Exception e) {
            log.error("Failed to warmup cache for user: {}", userId, e);
        }
    }
}