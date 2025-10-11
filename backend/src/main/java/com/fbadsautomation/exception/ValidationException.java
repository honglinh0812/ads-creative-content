package com.fbadsautomation.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

/**
 * Exception thrown for validation errors
 */
public class ValidationException extends BusinessException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message) {
        this(message, null);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    // Factory methods for common validation scenarios
    public static ValidationException requiredField(String fieldName) {
        return new ValidationException(
            String.format("Field '%s' is required", fieldName),
            Map.of(fieldName, "This field is required")
        );
    }

    public static ValidationException invalidFormat(String fieldName, String expectedFormat) {
        return new ValidationException(
            String.format("Field '%s' has invalid format. Expected: %s", fieldName, expectedFormat),
            Map.of(fieldName, "Invalid format: " + expectedFormat)
        );
    }

    public static ValidationException invalidValue(String fieldName, String value) {
        return new ValidationException(
            String.format("Field '%s' has invalid value: %s", fieldName, value),
            Map.of(fieldName, "Invalid value")
        );
    }
}