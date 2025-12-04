package com.screenleads.backend.app.web.advice;


import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;


@ControllerAdvice
public class GlobalExceptionHandler {


@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
var errors = ex.getBindingResult().getFieldErrors().stream()
.collect(java.util.stream.Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a,b)->a));
return ResponseEntity.badRequest().body(Map.of(
"timestamp", Instant.now().toString(),
"path", req.getDescription(false),
"code", "VALIDATION_ERROR",
"message", "Datos inválidos",
"details", errors
));
}


@ExceptionHandler(IllegalArgumentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, WebRequest req) {
return ResponseEntity.badRequest().body(Map.of(
"timestamp", Instant.now().toString(),
"path", req.getDescription(false),
"code", "BAD_REQUEST",
"message", ex.getMessage()
));
}

@ExceptionHandler(BadCredentialsException.class)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex, WebRequest req) {
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
"timestamp", Instant.now().toString(),
"path", req.getDescription(false),
"code", "UNAUTHORIZED",
"message", "Invalid username or password"
));
}

@ExceptionHandler(NoSuchElementException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ResponseEntity<?> handleNotFound(NoSuchElementException ex, WebRequest req) {
return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
"timestamp", Instant.now().toString(),
"path", req.getDescription(false),
"code", "NOT_FOUND",
"message", ex.getMessage() != null && !ex.getMessage().isEmpty() ? ex.getMessage() : "Resource not found"
));
}

@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ResponseEntity<?> handleGeneric(Exception ex, WebRequest req) {
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
"timestamp", Instant.now().toString(),
"path", req.getDescription(false),
"code", "INTERNAL_ERROR",
"message", ex.getMessage()
));
}
}