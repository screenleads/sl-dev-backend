package com.screenleads.backend.app.application.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro que normaliza URLs eliminando slashes duplicados.
 * Workaround para bug de frontend que genera URLs como /actuator//health
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class UrlNormalizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String originalUri = httpRequest.getRequestURI();
        
        // Normalizar: reemplazar múltiples slashes por uno solo
        String normalizedUri = originalUri.replaceAll("/+", "/");
        
        if (!originalUri.equals(normalizedUri)) {
            log.warn("URL normalizada: {} -> {}", originalUri, normalizedUri);
            
            // Crear wrapper con URI normalizada
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getRequestURI() {
                    return normalizedUri;
                }
                
                @Override
                public StringBuffer getRequestURL() {
                    StringBuffer url = new StringBuffer();
                    url.append(getScheme())
                       .append("://")
                       .append(getServerName());
                    
                    if ((getScheme().equals("http") && getServerPort() != 80) ||
                        (getScheme().equals("https") && getServerPort() != 443)) {
                        url.append(':').append(getServerPort());
                    }
                    
                    url.append(normalizedUri);
                    return url;
                }
                
                @Override
                public String getServletPath() {
                    return normalizedUri;
                }
            };
            
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
