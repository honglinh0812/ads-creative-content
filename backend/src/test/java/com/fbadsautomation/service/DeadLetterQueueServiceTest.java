package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private DeadLetterQueueService deadLetterQueueService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        deadLetterQueueService = new DeadLetterQueueService(redisTemplate, objectMapper);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void testAddFailedRequest() {
        DeadLetterQueueService.FailedAIRequest failedRequest = createTestFailedRequest();

        deadLetterQueueService.addFailedRequest(failedRequest);

        verify(valueOperations).set(startsWith("dlq:"), eq(failedRequest), eq(7L), eq(TimeUnit.DAYS));
        verify(hashOperations, atLeastOnce()).increment(anyString(), anyString(), eq(1L));
        verify(redisTemplate).expire(anyString(), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    void testAddFailedRequestWithRetry() {
        DeadLetterQueueService.FailedAIRequest failedRequest = createTestFailedRequest();
        failedRequest.setRetryable(true);
        failedRequest.setRetryCount(0);

        deadLetterQueueService.addFailedRequest(failedRequest);

        // Should store both the failed request and schedule a retry
        verify(valueOperations, times(2)).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testGetDLQStats() {
        // Mock hash operations for statistics
        Map<Object, Object> mockStats = new HashMap<>();
        mockStats.put("total_failures", "10");
        mockStats.put("retryable_failures", "6");
        mockStats.put("permanent_failures", "4");
        mockStats.put("provider_openai", "5");
        mockStats.put("provider_gemini", "5");

        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(hashOperations.entries(anyString())).thenReturn(mockStats);

        DeadLetterQueueService.DLQStats stats = deadLetterQueueService.getDLQStats(24);

        assertNotNull(stats);
        assertTrue(stats.getTotalFailures() > 0);
        assertTrue(stats.getRetryableFailures() > 0);
        assertTrue(stats.getPermanentFailures() > 0);
        assertFalse(stats.getProviderFailures().isEmpty());
        assertTrue(stats.getProviderFailures().containsKey("openai"));
        assertTrue(stats.getProviderFailures().containsKey("gemini"));
    }

    @Test
    void testGetDLQSize() {
        Set<String> mockKeys = Set.of("dlq:key1", "dlq:key2", "dlq:key3");
        when(redisTemplate.keys("dlq:*")).thenReturn(mockKeys);

        long size = deadLetterQueueService.getDLQSize();

        assertEquals(3, size);
        verify(redisTemplate).keys("dlq:*");
    }

    @Test
    void testProcessRetryQueueEmpty() {
        when(redisTemplate.keys("dlq:retry:*")).thenReturn(Set.of());

        // Should complete without errors
        assertDoesNotThrow(() -> deadLetterQueueService.processRetryQueue());
    }

    @Test
    void testProcessRetryQueueWithValidRetry() {
        Set<String> retryKeys = Set.of("dlq:retry:test1");
        when(redisTemplate.keys("dlq:retry:*")).thenReturn(retryKeys);

        // Create a retry request that's ready to be retried
        DeadLetterQueueService.RetryRequest retryRequest = new DeadLetterQueueService.RetryRequest();
        retryRequest.setOriginalKey("dlq:original");
        retryRequest.setScheduledRetryTime(LocalDateTime.now().minusMinutes(1)); // Past due
        retryRequest.setFailedRequest(createTestFailedRequest());

        when(valueOperations.get("dlq:retry:test1")).thenReturn(retryRequest);

        deadLetterQueueService.processRetryQueue();

        // Should attempt to process the retry request
        verify(valueOperations).get("dlq:retry:test1");
    }

    @Test
    void testCleanupOldEntries() {
        Set<String> mockKeys = Set.of("dlq:key1", "dlq:key2");
        when(redisTemplate.keys("dlq:*")).thenReturn(mockKeys);
        when(redisTemplate.getExpire("dlq:key1")).thenReturn(-1L); // Expired
        when(redisTemplate.getExpire("dlq:key2")).thenReturn(3600L); // Still valid

        deadLetterQueueService.cleanupOldEntries();

        verify(redisTemplate).delete("dlq:key1");
        verify(redisTemplate, never()).delete("dlq:key2");
    }

    @Test
    void testFailedAIRequestGettersAndSetters() {
        DeadLetterQueueService.FailedAIRequest request = new DeadLetterQueueService.FailedAIRequest();

        String requestId = "test-123";
        String provider = "openai";
        String prompt = "Test prompt";
        String errorMessage = "API Error";
        String errorCode = "API_ERROR";
        boolean retryable = true;
        int retryCount = 2;
        LocalDateTime failureTime = LocalDateTime.now();
        LocalDateTime lastRetryTime = LocalDateTime.now().minusHours(1);
        Map<String, Object> parameters = Map.of("param1", "value1");

        request.setRequestId(requestId);
        request.setProvider(provider);
        request.setPrompt(prompt);
        request.setErrorMessage(errorMessage);
        request.setErrorCode(errorCode);
        request.setRetryable(retryable);
        request.setRetryCount(retryCount);
        request.setFailureTime(failureTime);
        request.setLastRetryTime(lastRetryTime);
        request.setRequestParameters(parameters);

        assertEquals(requestId, request.getRequestId());
        assertEquals(provider, request.getProvider());
        assertEquals(prompt, request.getPrompt());
        assertEquals(errorMessage, request.getErrorMessage());
        assertEquals(errorCode, request.getErrorCode());
        assertEquals(retryable, request.isRetryable());
        assertEquals(retryCount, request.getRetryCount());
        assertEquals(failureTime, request.getFailureTime());
        assertEquals(lastRetryTime, request.getLastRetryTime());
        assertEquals(parameters, request.getRequestParameters());
    }

    @Test
    void testDLQStatsGettersAndSetters() {
        DeadLetterQueueService.DLQStats stats = new DeadLetterQueueService.DLQStats();

        long totalFailures = 100;
        long retryableFailures = 60;
        long permanentFailures = 40;
        Map<String, Long> providerFailures = Map.of("openai", 50L, "gemini", 50L);

        stats.setTotalFailures(totalFailures);
        stats.setRetryableFailures(retryableFailures);
        stats.setPermanentFailures(permanentFailures);
        stats.setProviderFailures(providerFailures);

        assertEquals(totalFailures, stats.getTotalFailures());
        assertEquals(retryableFailures, stats.getRetryableFailures());
        assertEquals(permanentFailures, stats.getPermanentFailures());
        assertEquals(providerFailures, stats.getProviderFailures());
    }

    private DeadLetterQueueService.FailedAIRequest createTestFailedRequest() {
        DeadLetterQueueService.FailedAIRequest request = new DeadLetterQueueService.FailedAIRequest();
        request.setRequestId("test-123");
        request.setProvider("openai");
        request.setPrompt("Test prompt");
        request.setErrorMessage("Test error");
        request.setErrorCode("TEST_ERROR");
        request.setRetryable(false);
        request.setRetryCount(0);
        request.setFailureTime(LocalDateTime.now());
        request.setRequestParameters(Map.of("param1", "value1"));
        return request;
    }
}