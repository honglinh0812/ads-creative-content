package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.exception.AIProviderException;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIProviderFallbackServiceTest {

    @Mock
    private Map<String, AIProvider> aiProviders;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DeadLetterQueueService deadLetterQueueService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private AIProvider openaiProvider;

    @Mock
    private AIProvider anthropicProvider;

    @Mock
    private AIProvider geminiProvider;

    private AIProviderFallbackService fallbackService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock provider map
        when(aiProviders.containsKey("openai")).thenReturn(true);
        when(aiProviders.containsKey("anthropic")).thenReturn(true);
        when(aiProviders.containsKey("gemini")).thenReturn(true);
        when(aiProviders.containsKey("huggingface")).thenReturn(false);

        when(aiProviders.get("openai")).thenReturn(openaiProvider);
        when(aiProviders.get("anthropic")).thenReturn(anthropicProvider);
        when(aiProviders.get("gemini")).thenReturn(geminiProvider);

        fallbackService = new AIProviderFallbackService(aiProviders, redisTemplate, deadLetterQueueService);
    }

    @Test
    void testGenerateWithFallbackFirstProviderSuccess() throws Exception {
        String prompt = "Test prompt";
        int variations = 3;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        List<AdContent> expectedContent = createTestAdContent(variations);

        when(valueOperations.get(anyString())).thenReturn(null); // No circuit breaker
        when(openaiProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenReturn(expectedContent);

        List<AdContent> result = fallbackService.generateWithFallback(prompt, variations, language, callToAction);

        assertEquals(expectedContent, result);
        verify(openaiProvider).generateAdContent(prompt, variations, language, callToAction);
        verify(anthropicProvider, never()).generateAdContent(anyString(), anyInt(), anyString(), any(FacebookCTA.class));
        verify(redisTemplate).delete(startsWith("circuit_breaker:openai")); // Circuit breaker reset
    }

    @Test
    void testGenerateWithFallbackFirstProviderFailsSecondSucceeds() throws Exception {
        String prompt = "Test prompt";
        int variations = 3;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        List<AdContent> expectedContent = createTestAdContent(variations);

        when(valueOperations.get(anyString())).thenReturn(null); // No circuit breaker
        when(openaiProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("openai", "API Error", true));
        when(anthropicProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenReturn(expectedContent);

        List<AdContent> result = fallbackService.generateWithFallback(prompt, variations, language, callToAction);

        assertEquals(expectedContent, result);
        verify(openaiProvider).generateAdContent(prompt, variations, language, callToAction);
        verify(anthropicProvider).generateAdContent(prompt, variations, language, callToAction);
    }

    @Test
    void testGenerateWithFallbackCircuitBreakerOpen() throws Exception {
        String prompt = "Test prompt";
        int variations = 3;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        List<AdContent> expectedContent = createTestAdContent(variations);

        // Mock circuit breaker state for openai (open)
        AIProviderFallbackService.CircuitBreakerState openCircuit = new AIProviderFallbackService.CircuitBreakerState();
        openCircuit.openCircuit(LocalDateTime.now().plusMinutes(5));
        when(valueOperations.get("circuit_breaker:openai")).thenReturn(openCircuit);
        when(valueOperations.get("circuit_breaker:anthropic")).thenReturn(null);

        when(anthropicProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenReturn(expectedContent);

        List<AdContent> result = fallbackService.generateWithFallback(prompt, variations, language, callToAction);

        assertEquals(expectedContent, result);
        verify(openaiProvider, never()).generateAdContent(anyString(), anyInt(), anyString(), any(FacebookCTA.class));
        verify(anthropicProvider).generateAdContent(prompt, variations, language, callToAction);
    }

    @Test
    void testGenerateWithFallbackAllProvidersFail() throws Exception {
        String prompt = "Test prompt";
        int variations = 3;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        when(valueOperations.get(anyString())).thenReturn(null); // No circuit breaker
        when(openaiProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("openai", "API Error", false));
        when(anthropicProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("anthropic", "API Error", false));
        when(geminiProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("gemini", "API Error", false));

        // No cached fallback
        when(valueOperations.get(startsWith("fallback_cache:"))).thenReturn(null);

        List<AdContent> result = fallbackService.generateWithFallback(prompt, variations, language, callToAction);

        assertNotNull(result);
        assertEquals(variations, result.size());
        // Should return basic fallback content
        assertTrue(result.get(0).getHeadline().contains("Discover Amazing Products"));

        verify(deadLetterQueueService).addFailedRequest(any());
    }

    @Test
    void testGenerateWithFallbackUseCachedResult() throws Exception {
        String prompt = "Test prompt";
        int variations = 3;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        List<AdContent> cachedContent = createTestAdContent(variations);

        when(valueOperations.get(anyString())).thenReturn(null); // No circuit breaker
        when(openaiProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("openai", "API Error", false));
        when(anthropicProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("anthropic", "API Error", false));
        when(geminiProvider.generateAdContent(prompt, variations, language, callToAction))
            .thenThrow(new AIProviderException("gemini", "API Error", false));

        // Return cached fallback
        when(valueOperations.get(startsWith("fallback_cache:"))).thenReturn(cachedContent);

        List<AdContent> result = fallbackService.generateWithFallback(prompt, variations, language, callToAction);

        assertEquals(cachedContent, result);
    }

    @Test
    void testGetProviderHealthStatus() {
        // Mock circuit breaker states
        when(valueOperations.get("circuit_breaker:openai")).thenReturn(null);

        AIProviderFallbackService.CircuitBreakerState openCircuit = new AIProviderFallbackService.CircuitBreakerState();
        openCircuit.openCircuit(LocalDateTime.now().plusMinutes(5));
        when(valueOperations.get("circuit_breaker:anthropic")).thenReturn(openCircuit);

        Map<String, AIProviderFallbackService.ProviderHealth> health = fallbackService.getProviderHealthStatus();

        assertNotNull(health);
        assertTrue(health.containsKey("openai"));
        assertTrue(health.containsKey("anthropic"));
        assertTrue(health.containsKey("gemini"));

        assertEquals("AVAILABLE", health.get("openai").getStatus());
        assertEquals("CIRCUIT_OPEN", health.get("anthropic").getStatus());
        assertEquals("AVAILABLE", health.get("gemini").getStatus());
    }

    @Test
    void testCircuitBreakerStateOperations() {
        AIProviderFallbackService.CircuitBreakerState state = new AIProviderFallbackService.CircuitBreakerState();

        assertFalse(state.isOpen());
        assertEquals(0, state.getFailureCount());

        state.recordFailure();
        assertEquals(1, state.getFailureCount());

        LocalDateTime openUntil = LocalDateTime.now().plusMinutes(5);
        state.openCircuit(openUntil);
        assertTrue(state.isOpen());
        assertEquals(openUntil, state.getOpenUntil());
    }

    @Test
    void testProviderHealthGettersAndSetters() {
        AIProviderFallbackService.ProviderHealth health = new AIProviderFallbackService.ProviderHealth();

        String providerName = "openai";
        boolean available = true;
        boolean circuitOpen = false;
        String status = "AVAILABLE";

        health.setProviderName(providerName);
        health.setAvailable(available);
        health.setCircuitOpen(circuitOpen);
        health.setStatus(status);

        assertEquals(providerName, health.getProviderName());
        assertEquals(available, health.isAvailable());
        assertEquals(circuitOpen, health.isCircuitOpen());
        assertEquals(status, health.getStatus());
    }

    private List<AdContent> createTestAdContent(int count) {
        List<AdContent> content = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AdContent ad = new AdContent();
            ad.setHeadline("Test Headline " + (i + 1));
            ad.setPrimaryText("Test primary text " + (i + 1));
            ad.setDescription("Test description " + (i + 1));
            content.add(ad);
        }
        return content;
    }
}