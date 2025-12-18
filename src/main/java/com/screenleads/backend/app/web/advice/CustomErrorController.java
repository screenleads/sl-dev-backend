package com.screenleads.backend.app.web.advice;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
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

    public CustomErrorController(ErrorAttributes errorAttributes) {
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (statusCode != null) {
            try {
                status = HttpStatus.valueOf(Integer.parseInt(statusCode.toString()));
            } catch (NumberFormatException ignored) {
                // Invalid status code format - use default INTERNAL_SERVER_ERROR
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
