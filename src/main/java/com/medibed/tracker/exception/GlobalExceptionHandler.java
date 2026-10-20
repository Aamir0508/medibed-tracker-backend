package com.medibed.tracker.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Centralized exception handling: every error leaves the API as an {@link ApiError}. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getFieldErrors());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> badCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server", null);
    }

    // ---- Spring MVC's own exceptions (bad JSON, wrong method, unknown URL, type mismatch...) ----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ApiError body = new ApiError(java.time.Instant.now(), 400, "Bad Request", "Validation failed", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Malformed JSON request or an invalid value (check enum values, dates and number formats)";
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException && cause.getMessage() != null) {
            detail = cause.getMessage();
        }
        return ResponseEntity.badRequest().body(ApiError.of(400, "Bad Request", detail));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = ex.getMessage();
        if (body instanceof ProblemDetail pd && pd.getDetail() != null) {
            message = pd.getDetail();
        }
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return ResponseEntity.status(statusCode).headers(headers).body(ApiError.of(statusCode.value(), reason, message));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fields) {
        ApiError body = new ApiError(java.time.Instant.now(), status.value(), status.getReasonPhrase(), message,
                fields == null || fields.isEmpty() ? null : fields);
        return ResponseEntity.status(status).body(body);
    }
}
