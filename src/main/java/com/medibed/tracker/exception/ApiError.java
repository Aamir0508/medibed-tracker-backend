package com.medibed.tracker.exception;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Single error shape returned by every failing endpoint. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, null);
    }
}
