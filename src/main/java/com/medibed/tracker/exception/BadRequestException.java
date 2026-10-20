package com.medibed.tracker.exception;

import java.util.Map;

/** Business-rule validation failure -> HTTP 400 with an optional field-level message. */
public class BadRequestException extends RuntimeException {

    private final transient Map<String, String> fieldErrors;

    public BadRequestException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public BadRequestException(String field, String message) {
        super(field + " " + message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
