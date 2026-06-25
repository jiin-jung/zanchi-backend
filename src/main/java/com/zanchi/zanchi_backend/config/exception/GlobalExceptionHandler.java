package com.zanchi.zanchi_backend.config.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handle(ApiException ex, HttpServletRequest req) {
        return response(ex.getStatus(), ex.getMessage(), req);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return response(NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return response(BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex, HttpServletRequest req) {
        return response(CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return response(FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::fieldErrorMessage)
                .orElse("invalid request");
        return response(BAD_REQUEST, message, req);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception ex, HttpServletRequest req) {
        return response(BAD_REQUEST, ex.getMessage(), req);
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus st, String msg, HttpServletRequest req) {
        return ResponseEntity.status(st).body(problem(st, msg, req.getRequestURI()));
    }

    private Map<String, Object> problem(HttpStatus st, String msg, String uri) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("type", "about:blank");
        b.put("title", st.getReasonPhrase());
        b.put("status", st.value());
        b.put("detail", msg);
        b.put("instance", uri);
        return b;
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
