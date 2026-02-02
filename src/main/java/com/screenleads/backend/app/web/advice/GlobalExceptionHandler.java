package com.screenleads.backend.app.web.advice;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";

    /**
     * Manejo específico para credenciales incorrectas
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "INVALID_CREDENTIALS",
                MESSAGE, "Usuario o contraseña incorrectos"));
    }

    /**
     * Manejo para usuario desactivado
     */
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "USER_DISABLED",
                MESSAGE, "Usuario desactivado"));
    }

    /**
     * Manejo para usuario bloqueado
     */
    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleLocked(LockedException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "USER_LOCKED",
                MESSAGE, "Usuario bloqueado"));
    }

    /**
     * Manejo genérico para otras excepciones de autenticación
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "AUTHENTICATION_ERROR",
                MESSAGE, "Error de autenticación"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (a, b) -> a));
        return ResponseEntity.badRequest().body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "VALIDATION_ERROR",
                MESSAGE, "Datos inválidos",
                "details", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest req) {
        return ResponseEntity.badRequest().body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "BAD_REQUEST",
                MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                TIMESTAMP, Instant.now().toString(),
                "path", req.getDescription(false),
                "code", "INTERNAL_ERROR",
                MESSAGE, ex.getMessage()));
    }
}