package com.fbadsautomation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for resource-related errors
 */
public class ResourceException extends BusinessException {
    private final String resourceType;
    private final String resourceId;

    private ResourceException(String message, String errorCode, HttpStatus httpStatus,
                             String resourceType, String resourceId) {
        super(message, errorCode, httpStatus);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    private ResourceException(String message, String errorCode, HttpStatus httpStatus,
                             String resourceType, String resourceId, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    // Factory methods for common resource scenarios
    public static ResourceException notFound(String resourceType, String resourceId) {
        return new ResourceException(
            String.format("%s with ID '%s' not found", resourceType, resourceId),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            resourceType,
            resourceId
        );
    }

    public static ResourceException alreadyExists(String resourceType, String resourceId) {
        return new ResourceException(
            String.format("%s with ID '%s' already exists", resourceType, resourceId),
            "RESOURCE_ALREADY_EXISTS",
            HttpStatus.CONFLICT,
            resourceType,
            resourceId
        );
    }

    public static ResourceException accessDenied(String resourceType, String resourceId) {
        return new ResourceException(
            String.format("Access denied to %s with ID '%s'", resourceType, resourceId),
            "RESOURCE_ACCESS_DENIED",
            HttpStatus.FORBIDDEN,
            resourceType,
            resourceId
        );
    }

    public static ResourceException operationFailed(String resourceType, String resourceId,
                                                   String operation, Throwable cause) {
        return new ResourceException(
            String.format("Failed to %s %s with ID '%s'", operation, resourceType, resourceId),
            "RESOURCE_OPERATION_FAILED",
            HttpStatus.INTERNAL_SERVER_ERROR,
            resourceType,
            resourceId,
            cause
        );
    }
}