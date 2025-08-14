package com.screenleads.backend.app.application.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        res.setContentType("application/json;charset=UTF-8");
        String body = """
                {"error":"Forbidden","message":"Insufficient permissions","path":"%s"}
                """.formatted(req.getRequestURI());
        res.getWriter().write(body);
    }
}
