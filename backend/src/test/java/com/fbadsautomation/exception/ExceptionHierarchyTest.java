package com.fbadsautomation.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHierarchyTest {

    @Test
    void testBusinessExceptionCreation() {
        String message = "Test business error";
        String errorCode = "BUSINESS_ERROR";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        TestBusinessException exception = new TestBusinessException(message, errorCode, httpStatus);

        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(httpStatus, exception.getHttpStatus());
    }

    @Test
    void testAIProviderExceptionFactoryMethods() {
        String providerName = "openai";

        AIProviderException quotaException = AIProviderException.quotaExceeded(providerName);
        assertEquals(providerName, quotaException.getProviderName());
        assertFalse(quotaException.isRetryable());
        assertTrue(quotaException.getMessage().contains("quota exceeded"));

        AIProviderException rateLimitException = AIProviderException.rateLimit(providerName);
        assertEquals(providerName, rateLimitException.getProviderName());
        assertTrue(rateLimitException.isRetryable());
        assertTrue(rateLimitException.getMessage().contains("rate limit"));

        AIProviderException invalidKeyException = AIProviderException.invalidApiKey(providerName);
        assertEquals(providerName, invalidKeyException.getProviderName());
        assertFalse(invalidKeyException.isRetryable());
        assertTrue(invalidKeyException.getMessage().contains("Invalid API key"));

        RuntimeException cause = new RuntimeException("Network timeout");
        AIProviderException networkException = AIProviderException.networkError(providerName, cause);
        assertEquals(providerName, networkException.getProviderName());
        assertTrue(networkException.isRetryable());
        assertEquals(cause, networkException.getCause());

        AIProviderException invalidResponseException = AIProviderException.invalidResponse(providerName, "Invalid JSON");
        assertEquals(providerName, invalidResponseException.getProviderName());
        assertFalse(invalidResponseException.isRetryable());
        assertTrue(invalidResponseException.getMessage().contains("Invalid JSON"));
    }

    @Test
    void testValidationExceptionFactoryMethods() {
        String fieldName = "email";
        String value = "invalid-email";

        ValidationException requiredFieldException = ValidationException.requiredField(fieldName);
        assertNotNull(requiredFieldException.getFieldErrors());
        assertTrue(requiredFieldException.getFieldErrors().containsKey(fieldName));

        ValidationException invalidFormatException = ValidationException.invalidFormat(fieldName, "email format");
        assertNotNull(invalidFormatException.getFieldErrors());
        assertTrue(invalidFormatException.getFieldErrors().containsKey(fieldName));

        ValidationException invalidValueException = ValidationException.invalidValue(fieldName, value);
        assertNotNull(invalidValueException.getFieldErrors());
        assertTrue(invalidValueException.getFieldErrors().containsKey(fieldName));
    }

    @Test
    void testResourceExceptionFactoryMethods() {
        String resourceType = "User";
        String resourceId = "123";

        ResourceException notFoundException = ResourceException.notFound(resourceType, resourceId);
        assertEquals(resourceType, notFoundException.getResourceType());
        assertEquals(resourceId, notFoundException.getResourceId());
        assertEquals(HttpStatus.NOT_FOUND, notFoundException.getHttpStatus());
        assertTrue(notFoundException.getMessage().contains("not found"));

        ResourceException alreadyExistsException = ResourceException.alreadyExists(resourceType, resourceId);
        assertEquals(HttpStatus.CONFLICT, alreadyExistsException.getHttpStatus());
        assertTrue(alreadyExistsException.getMessage().contains("already exists"));

        ResourceException accessDeniedException = ResourceException.accessDenied(resourceType, resourceId);
        assertEquals(HttpStatus.FORBIDDEN, accessDeniedException.getHttpStatus());
        assertTrue(accessDeniedException.getMessage().contains("Access denied"));

        RuntimeException cause = new RuntimeException("Database error");
        ResourceException operationFailedException = ResourceException.operationFailed(
            resourceType, resourceId, "delete", cause);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, operationFailedException.getHttpStatus());
        assertEquals(cause, operationFailedException.getCause());
        assertTrue(operationFailedException.getMessage().contains("Failed to delete"));
    }

    @Test
    void testExternalServiceExceptionFactoryMethods() {
        String serviceName = "Facebook API";

        ExternalServiceException timeoutException = ExternalServiceException.timeout(serviceName);
        assertEquals(serviceName, timeoutException.getServiceName());
        assertEquals(408, timeoutException.getResponseCode());
        assertTrue(timeoutException.isRetryable());

        ExternalServiceException unavailableException = ExternalServiceException.unavailable(serviceName);
        assertEquals(503, unavailableException.getResponseCode());
        assertTrue(unavailableException.isRetryable());

        ExternalServiceException unauthorizedException = ExternalServiceException.unauthorized(serviceName);
        assertEquals(401, unauthorizedException.getResponseCode());
        assertFalse(unauthorizedException.isRetryable());

        ExternalServiceException rateLimitedException = ExternalServiceException.rateLimited(serviceName);
        assertEquals(429, rateLimitedException.getResponseCode());
        assertTrue(rateLimitedException.isRetryable());

        ExternalServiceException badRequestException = ExternalServiceException.badRequest(serviceName, "Invalid parameter");
        assertEquals(400, badRequestException.getResponseCode());
        assertFalse(badRequestException.isRetryable());
        assertTrue(badRequestException.getMessage().contains("Invalid parameter"));
    }

    // Test implementation of BusinessException for testing
    private static class TestBusinessException extends BusinessException {
        public TestBusinessException(String message, String errorCode, HttpStatus httpStatus) {
            super(message, errorCode, httpStatus);
        }
    }
}