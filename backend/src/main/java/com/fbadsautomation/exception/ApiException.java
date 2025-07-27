package com.fbadsautomation.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Getter
@Slf4j

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String message;
    private final String errorCode;

    public ApiException(HttpStatus status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

    public ApiException(HttpStatus status, String message) {
        this(status, message, "ERROR_" + status.value());
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
    	super(message, cause);
    	this.status = status;
    	this.message = message;
    	this.errorCode = "ERROR_" + status.value();
    }

    public ApiException(String message) {
    	this(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public ApiException(String message, Throwable cause) {
    	this(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    };
    }
