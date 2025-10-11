package com.fbadsautomation.exception;

import com.fbadsautomation.dto.ErrorResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, WebRequest request) {
        log.error("API Exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.error("Business Exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<ErrorResponse> handleAIProviderException(AIProviderException ex, WebRequest request) {
        log.error("AI Provider Exception [{}]: {}", ex.getProviderName(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Validation Exception: {}", ex.getMessage());

        ErrorResponse errorResponse;
        if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
            errorResponse = ErrorResponse.validationError(ex.getMessage(),
                    ex.getFieldErrors(),
                    getPath(request));
        } else {
            errorResponse = ErrorResponse.of(ex.getHttpStatus().value(),
                    ex.getErrorCode(),
                    ex.getMessage(),
                    getPath(request));
        }
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<ErrorResponse> handleResourceException(ResourceException ex, WebRequest request) {
        log.warn("Resource Exception [{}:{}]: {}", ex.getResourceType(), ex.getResourceId(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex, WebRequest request) {
        log.error("External Service Exception [{}]: {}", ex.getServiceName(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        BindingResult result = ex.getBindingResult();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fieldError : result.getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Check for custom validation error (prompt or ad links must be provided)
        boolean hasPromptOrAdLinksError = result.getGlobalErrors().stream()
            .anyMatch(error -> error.getDefaultMessage().contains("Either prompt or ad links must be provided"));
        String message = hasPromptOrAdLinksError ?
            "Could not create ads. Please check the ad prompt / ad link and try again." :
            "Validation failed. Please check your input and try again.";

        ErrorResponse errorResponse = ErrorResponse.validationError(message,
                fieldErrors,
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "You don't have permission to access this resource",
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File upload size exceeded: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                "File Too Large",
                "The uploaded file exceeds the maximum allowed size",
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            fieldErrors.put(fieldName, message);
        }

        ErrorResponse errorResponse = ErrorResponse.validationError("Validation constraints violated",
                fieldErrors,
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                getPath(request)
        );
        errorResponse.setRequestId(generateRequestId());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri= ", "");
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
