package com.screenleads.backend.app.infraestructure.config;

public final class SwaggerWhitelist {
private SwaggerWhitelist() {}
public static final String[] ENDPOINTS = {
"/v3/api-docs/**",
"/swagger-ui.html",
"/swagger-ui/**"
};
}