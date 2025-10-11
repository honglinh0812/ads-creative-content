package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private IdempotencyService idempotencyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        idempotencyService = new IdempotencyService(redisTemplate, objectMapper);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGenerateIdempotencyKey() {
        String operation = "createUser";
        TestRequest requestBody = new TestRequest("test@example.com", "Test User");
        String userId = "user123";

        String key1 = idempotencyService.generateIdempotencyKey(operation, requestBody, userId);
        String key2 = idempotencyService.generateIdempotencyKey(operation, requestBody, userId);

        assertNotNull(key1);
        assertNotNull(key2);
        assertEquals(key1, key2); // Same inputs should generate same key
        assertTrue(key1.startsWith("idempotency:"));

        // Different operation should generate different key
        String key3 = idempotencyService.generateIdempotencyKey("updateUser", requestBody, userId);
        assertNotEquals(key1, key3);

        // Different user should generate different key
        String key4 = idempotencyService.generateIdempotencyKey(operation, requestBody, "user456");
        assertNotEquals(key1, key4);
    }

    @Test
    void testStoreIdempotentResult() {
        String key = "idempotency:test123";
        String result = "Operation successful";
        long ttlHours = 12;

        idempotencyService.storeIdempotentResult(key, result, ttlHours);

        verify(valueOperations).set(eq(key), any(IdempotencyService.IdempotentResult.class),
                                   eq(ttlHours), eq(TimeUnit.HOURS));
    }

    @Test
    void testStoreIdempotentResultWithDefaultTTL() {
        String key = "idempotency:test123";
        String result = "Operation successful";

        idempotencyService.storeIdempotentResult(key, result);

        verify(valueOperations).set(eq(key), any(IdempotencyService.IdempotentResult.class),
                                   eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testStoreIdempotentError() {
        String key = "idempotency:test123";
        String errorMessage = "Operation failed";
        String errorCode = "OPERATION_ERROR";

        idempotencyService.storeIdempotentError(key, errorMessage, errorCode);

        verify(valueOperations).set(eq(key), any(IdempotencyService.IdempotentResult.class),
                                   eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testGetIdempotentResultFound() {
        String key = "idempotency:test123";
        IdempotencyService.IdempotentResult cachedResult = createSuccessResult("Test result");

        when(valueOperations.get(key)).thenReturn(cachedResult);

        IdempotencyService.IdempotentResult result = idempotencyService.getIdempotentResult(key);

        assertNotNull(result);
        assertEquals("Test result", result.getResult());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void testGetIdempotentResultNotFound() {
        String key = "idempotency:test123";

        when(valueOperations.get(key)).thenReturn(null);

        IdempotencyService.IdempotentResult result = idempotencyService.getIdempotentResult(key);

        assertNull(result);
    }

    @Test
    void testHasBeenProcessed() {
        String key = "idempotency:test123";

        when(redisTemplate.hasKey(key)).thenReturn(true);
        assertTrue(idempotencyService.hasBeenProcessed(key));

        when(redisTemplate.hasKey(key)).thenReturn(false);
        assertFalse(idempotencyService.hasBeenProcessed(key));
    }

    @Test
    void testProcessIdempotentlyFirstTime() throws Exception {
        String operation = "createUser";
        TestRequest requestBody = new TestRequest("test@example.com", "Test User");
        String userId = "user123";
        String expectedResult = "User created successfully";

        when(valueOperations.get(anyString())).thenReturn(null); // No cached result

        IdempotencyService.IdempotentOperation<String> operationFunc = () -> expectedResult;

        String result = idempotencyService.processIdempotently(operation, requestBody, userId, operationFunc);

        assertEquals(expectedResult, result);
        verify(valueOperations).set(anyString(), any(IdempotencyService.IdempotentResult.class),
                                   eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testProcessIdempotentlyCachedSuccess() throws Exception {
        String operation = "createUser";
        TestRequest requestBody = new TestRequest("test@example.com", "Test User");
        String userId = "user123";
        String cachedResult = "User already created";

        IdempotencyService.IdempotentResult cached = createSuccessResult(cachedResult);
        when(valueOperations.get(anyString())).thenReturn(cached);

        IdempotencyService.IdempotentOperation<String> operationFunc = () -> "This should not be called";

        String result = idempotencyService.processIdempotently(operation, requestBody, userId, operationFunc);

        assertEquals(cachedResult, result);
        // Should not store new result since it was cached
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testProcessIdempotentlyCachedError() {
        String operation = "createUser";
        TestRequest requestBody = new TestRequest("test@example.com", "Test User");
        String userId = "user123";

        IdempotencyService.IdempotentResult cached = createErrorResult("Validation failed", "VALIDATION_ERROR");
        when(valueOperations.get(anyString())).thenReturn(cached);

        IdempotencyService.IdempotentOperation<String> operationFunc = () -> "This should not be called";

        assertThrows(IdempotencyService.IdempotentOperationException.class, () ->
            idempotencyService.processIdempotently(operation, requestBody, userId, operationFunc));
    }

    @Test
    void testProcessIdempotentlyOperationThrowsException() {
        String operation = "createUser";
        TestRequest requestBody = new TestRequest("test@example.com", "Test User");
        String userId = "user123";

        when(valueOperations.get(anyString())).thenReturn(null); // No cached result

        IdempotencyService.IdempotentOperation<String> operationFunc = () -> {
            throw new RuntimeException("Operation failed");
        };

        assertThrows(RuntimeException.class, () ->
            idempotencyService.processIdempotently(operation, requestBody, userId, operationFunc));

        // Should store error result
        verify(valueOperations, times(2)).set(anyString(), any(IdempotencyService.IdempotentResult.class),
                                             eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testIdempotentResultGettersAndSetters() {
        IdempotencyService.IdempotentResult result = new IdempotencyService.IdempotentResult();

        Object resultData = "Test result";
        String errorMessage = "Test error";
        String errorCode = "TEST_ERROR";
        LocalDateTime timestamp = LocalDateTime.now();
        String status = "SUCCESS";

        result.setResult(resultData);
        result.setErrorMessage(errorMessage);
        result.setErrorCode(errorCode);
        result.setTimestamp(timestamp);
        result.setStatus(status);

        assertEquals(resultData, result.getResult());
        assertEquals(errorMessage, result.getErrorMessage());
        assertEquals(errorCode, result.getErrorCode());
        assertEquals(timestamp, result.getTimestamp());
        assertEquals(status, result.getStatus());
    }

    @Test
    void testIdempotentOperationException() {
        String message = "Test error";
        String errorCode = "TEST_ERROR";

        IdempotencyService.IdempotentOperationException exception =
            new IdempotencyService.IdempotentOperationException(message, errorCode);

        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
    }

    private IdempotencyService.IdempotentResult createSuccessResult(Object result) {
        IdempotencyService.IdempotentResult idempotentResult = new IdempotencyService.IdempotentResult();
        idempotentResult.setResult(result);
        idempotentResult.setStatus("SUCCESS");
        idempotentResult.setTimestamp(LocalDateTime.now());
        return idempotentResult;
    }

    private IdempotencyService.IdempotentResult createErrorResult(String errorMessage, String errorCode) {
        IdempotencyService.IdempotentResult idempotentResult = new IdempotencyService.IdempotentResult();
        idempotentResult.setErrorMessage(errorMessage);
        idempotentResult.setErrorCode(errorCode);
        idempotentResult.setStatus("ERROR");
        idempotentResult.setTimestamp(LocalDateTime.now());
        return idempotentResult;
    }

    // Test request class for testing
    private static class TestRequest {
        private String email;
        private String name;

        public TestRequest(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() { return email; }
        public String getName() { return name; }
    }
}