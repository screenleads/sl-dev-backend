# Excepciones — snapshot incrustado

> Clases de excepciones y handlers.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/web/advice/CustomErrorController.java
package com.screenleads.backend.app.web.advice;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (statusCode != null) {
            try {
                status = HttpStatus.valueOf(Integer.parseInt(statusCode.toString()));
            } catch (NumberFormatException ignored) {
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());

        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        body.put("message", message != null ? message.toString() : "No message available");

        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        body.put("path", path != null ? path.toString() : "");

        return ResponseEntity.status(status).body(body);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/advice/GlobalExceptionHandler.java
package com.screenleads.backend.app.web.advice;


import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
```

