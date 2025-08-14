package com.screenleads.backend.app.application.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        res.setContentType("application/json;charset=UTF-8");
        String body = """
                {"error":"Unauthorized","message":"Invalid or missing token","path":"%s"}
                """.formatted(req.getRequestURI());
        res.getWriter().write(body);
    }
}
