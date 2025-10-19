package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.dto.ProviderResponse;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Capability;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIProviderService {

    private static final Logger log = LoggerFactory.getLogger(AIProviderService.class);
    
    private final List<AIProvider> aiProviders;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final AIContentCacheService cacheService;
    
    @Autowired
    public AIProviderService(List<AIProvider> aiProviders, CircuitBreakerRegistry circuitBreakerRegistry, 
                           RetryRegistry retryRegistry, AIContentCacheService cacheService) {
        this.aiProviders = aiProviders;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.cacheService = cacheService;
    }

    private final Map<String, AIProvider> providerMap = new HashMap<>();
    private final Map<String, List<String>> providerFallbacks = new HashMap<>();

    @PostConstruct
    public void initProviderMap() {
        for (AIProvider provider : aiProviders) {
            String key = normalizeKey(provider.getProviderName());
            providerMap.put(key, provider);
        }

        // Initialize fallback chains for text providers
        initializeFallbackChains();

        log.info("✅ Loaded {} AI providers: {}", providerMap.size(), providerMap.keySet());
        log.info("✅ Initialized fallback chains: {}", providerFallbacks);
    }

    private void initializeFallbackChains() {
        // Text generation fallback chains (ordered by reliability and speed)
        providerFallbacks.put("openai", Arrays.asList("gemini", "anthropic", "huggingface"));
        providerFallbacks.put("gemini", Arrays.asList("openai", "anthropic", "huggingface"));
        providerFallbacks.put("anthropic", Arrays.asList("openai", "gemini", "huggingface"));
        providerFallbacks.put("huggingface", Arrays.asList("openai", "gemini", "anthropic"));

        // Image generation fallback chains
        providerFallbacks.put("openai", Arrays.asList("fal-ai", "stable-diffusion"));
        providerFallbacks.put("fal-ai", Arrays.asList("openai", "stable-diffusion"));
        providerFallbacks.put("stable-diffusion", Arrays.asList("openai", "fal-ai"));
    }

    public List<ProviderResponse> getTextProviders() {
        return aiProviders.stream()
                .filter(provider -> provider.getCapabilities().contains(Capability.TEXT_GENERATION))
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public List<ProviderResponse> getImageProviders() {
        return aiProviders.stream()
                .filter(provider -> provider.getCapabilities().contains(Capability.IMAGE_GENERATION))
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public List<ProviderResponse> getAllProviders() {
        return aiProviders.stream()
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public AIProvider getProvider(String providerId) {
        if (providerId == null) return null;
        return providerMap.get(normalizeKey(providerId));
    }

    /**
     * Generate AI content with caching, circuit breaker, and fallback support
     */
    public List<AdContent> generateContentWithReliability(String prompt, String providerId,
                                                          int numberOfVariations, String language,
                                                          List<String> adLinks, com.fbadsautomation.model.FacebookCTA callToAction) {
        String normalizedProviderId = normalizeKey(providerId);
        // Cache temporarily disabled
        // String cacheKey = cacheService.generateContentCacheKey(prompt, normalizedProviderId, numberOfVariations, language, adLinks);
        // List<AdContent> cachedContent = cacheService.getCachedContent(cacheKey);
        // if (cachedContent != null) {
        //     log.info("Retrieved cached content for provider: {}", providerId);
        //     return cachedContent;
        // }

        // Try primary provider with circuit breaker and retry
        List<AdContent> content = generateWithFallback(prompt, normalizedProviderId, numberOfVariations, language, adLinks, callToAction);
        // Cache successful results
        // if (content != null && !content.isEmpty()) {
        //     cacheService.cacheContent(cacheKey, content, Duration.ofHours(24));
        // }

        return content;
    }

    /**
     * Generate AI image with caching, circuit breaker, and fallback support
     */
    public String generateImageWithReliability(String prompt, String providerId) {
        String normalizedProviderId = normalizeKey(providerId);
        // Cache temporarily disabled
        // String cacheKey = cacheService.generateImageCacheKey(prompt, normalizedProviderId);
        // String cachedImage = cacheService.getCachedImage(cacheKey);
        // if (cachedImage != null) {
        //     log.info("Retrieved cached image for provider: {}", providerId);
        //     return cachedImage;
        // }

        // Try primary provider with circuit breaker and retry
        String imageUrl = generateImageWithFallback(prompt, normalizedProviderId);
        // Cache successful results
        // if (imageUrl != null && !imageUrl.equals("/img/placeholder.png")) {
        //     cacheService.cacheImage(cacheKey, imageUrl, Duration.ofHours(24));
        // }

        return imageUrl;
    }

    private ProviderResponse toProviderResponse(AIProvider provider) {
        String id = normalizeKey(provider.getProviderName());
        List<String> capabilities = provider.getCapabilities().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return new ProviderResponse(
                id,
                provider.getProviderName(),
                "Tạo nội dung bằng " + provider.getProviderName(),
                capabilities
        );
    }

    /**
     * Generate content with fallback mechanism
     */
    private List<AdContent> generateWithFallback(String prompt, String primaryProviderId,
                                                int numberOfVariations, String language,
                                                List<String> adLinks, com.fbadsautomation.model.FacebookCTA callToAction) {
        // Try primary provider
        log.info("🎯 Trying primary provider: '{}'", primaryProviderId);
        List<AdContent> content = tryProviderWithCircuitBreaker(prompt, primaryProviderId, numberOfVariations, language, adLinks, callToAction);
        if (content != null && !content.isEmpty()) {
            log.info("✅ Primary provider '{}' succeeded", primaryProviderId);
            return content;
        }
        log.warn("❌ Primary provider '{}' failed", primaryProviderId);

        // Try fallback providers
        List<String> fallbacks = providerFallbacks.get(primaryProviderId);
        if (fallbacks != null) {
            log.info("🔄 Trying fallback providers: {}", fallbacks);
            for (String fallbackProviderId : fallbacks) {
                log.warn("🔄 Primary provider '{}' failed, trying fallback: '{}'", primaryProviderId, fallbackProviderId);
                content = tryProviderWithCircuitBreaker(prompt, fallbackProviderId, numberOfVariations, language, adLinks, callToAction);
                if (content != null && !content.isEmpty()) {
                    log.info("✅ Fallback provider '{}' succeeded", fallbackProviderId);
                    return content;
                }
                log.warn("❌ Fallback provider '{}' also failed", fallbackProviderId);
            }
        } else {
            log.warn("⚠️ No fallback providers configured for '{}'", primaryProviderId);
        }

        log.error("All providers failed for content generation");
        return generateMockContent(prompt, numberOfVariations);
    }

    /**
     * Generate image with fallback mechanism
     */
    private String generateImageWithFallback(String prompt, String primaryProviderId) {
        // Try primary provider
        String imageUrl = tryImageProviderWithCircuitBreaker(prompt, primaryProviderId);
        if (imageUrl != null && !imageUrl.equals("/img/placeholder.png")) {
            return imageUrl;
        }

        // Try fallback providers
        List<String> fallbacks = providerFallbacks.get(primaryProviderId);
        if (fallbacks != null) {
            for (String fallbackProviderId : fallbacks) {
                log.warn("Primary image provider '{}' failed, trying fallback: '{}'", primaryProviderId, fallbackProviderId);
                imageUrl = tryImageProviderWithCircuitBreaker(prompt, fallbackProviderId);
                if (imageUrl != null && !imageUrl.equals("/img/placeholder.png")) {
                    log.info("Fallback image provider '{}' succeeded", fallbackProviderId);
                    return imageUrl;
                }
            }
        }

        log.error("All image providers failed");
        return "/img/placeholder.png";
    }

    /**
     * Try provider with circuit breaker and retry
     */
    private List<AdContent> tryProviderWithCircuitBreaker(String prompt, String providerId,
                                                         int numberOfVariations, String language,
                                                         List<String> adLinks, com.fbadsautomation.model.FacebookCTA callToAction) {
        AIProvider provider = providerMap.get(providerId);
        if (provider == null) {
            log.warn("Provider not found: {}", providerId);
            return null;
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerId);
        Retry retry = retryRegistry.retry(providerId);

        Supplier<List<AdContent>> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        // Sử dụng CTA được truyền hoặc default nếu null
                        com.fbadsautomation.model.FacebookCTA cta = callToAction != null ? callToAction : com.fbadsautomation.model.FacebookCTA.LEARN_MORE;
                        List<AdContent> result = provider.generateAdContent(prompt, numberOfVariations, language, cta);
                        long responseTime = System.currentTimeMillis() - startTime;
                        // Record successful usage
                        // cacheService.recordProviderUsage(providerId, true, responseTime, estimateCost(providerId, numberOfVariations));
                        return result;
                    } catch (Exception e) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        // Record failed usage
                        // cacheService.recordProviderUsage(providerId, false, responseTime, 0.0);
                        throw e;
                    }
                });
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.error("Provider '{}' failed after retries: {}", providerId, e.getMessage());
            return null;
        }
    }

    /**
     * Try image provider with circuit breaker and retry
     */
    private String tryImageProviderWithCircuitBreaker(String prompt, String providerId) {
        AIProvider provider = providerMap.get(providerId);
        if (provider == null) {
            log.warn("Image provider not found: {}", providerId);
            return "/img/placeholder.png";
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerId);
        Retry retry = retryRegistry.retry(providerId);

        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        String result = provider.generateImage(prompt);
                        long responseTime = System.currentTimeMillis() - startTime;
                        // Record successful usage
                        // cacheService.recordProviderUsage(providerId, true, responseTime, estimateImageCost(providerId));
                        return result;
                    } catch (Exception e) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        // Record failed usage
                        // cacheService.recordProviderUsage(providerId, false, responseTime, 0.0);
                        throw e;
                    }
                });
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.error("Image provider '{}' failed after retries: {}", providerId, e.getMessage());
            return "/img/placeholder.png";
        }
    }

    /**
     * Generate mock content as last resort
     */
    private List<AdContent> generateMockContent(String prompt, int numberOfVariations) {
        List<AdContent> mockContents = new ArrayList<>();
        for (int i = 0; i < numberOfVariations; i++) {
            AdContent mockContent = AdContent.builder()
                    .headline("Mock Headline " + (i + 1))
                    .description("Mock description for: " + prompt)
                    .primaryText("This is mock content generated when all AI providers failed.")
                    .aiProvider(AdContent.AIProvider.MOCK)
                    .build();
            mockContents.add(mockContent);
        }
        return mockContents;
    }

    /**
     * Estimate cost for text generation (rough estimates)
     */
    private double estimateCost(String providerId, int numberOfVariations) {
        switch (providerId) {
            case "openai": return 0.002 * numberOfVariations; // Rough estimate
            case "gemini": return 0.001 * numberOfVariations;
            case "anthropic": return 0.003 * numberOfVariations;
            case "huggingface": return 0.0005 * numberOfVariations;
            default: return 0.001 * numberOfVariations;
        }
    }

    /**
     * Estimate cost for image generation
     */
    private double estimateImageCost(String providerId) {
        switch (providerId) {
            case "fal-ai": return 0.05; // Rough estimate per image
            case "stable-diffusion": return 0.02;
            default: return 0.03;
        }
    }

    /**
     * Get provider statistics for monitoring
     */
    public AIContentCacheService.ProviderStats getProviderStats(String providerId, int hoursBack) {
        return cacheService.getProviderStats(normalizeKey(providerId), hoursBack);
    }

    /**
     * Clear cache for specific provider
     */
    public void clearProviderCache(String providerId) {
        cacheService.clearProviderCache(normalizeKey(providerId));
    }

    private String normalizeKey(String input) {
        return input.trim().toLowerCase()
            .replace(" ", "-")     // spaces → hyphens
            .replace(".", "-");    // dots → hyphens
    }
}
