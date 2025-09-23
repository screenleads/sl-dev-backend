package com.screenleads.backend.app.infraestructure.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI baseOpenAPI() {
    return new OpenAPI().info(new Info()
        .title("ScreenLeads API")
        .version("v1"));
  }

  // Grupo "public" (el que consulta tu Swagger UI)
  @Bean
  public GroupedOpenApi publicGroup() {
    return GroupedOpenApi.builder()
        .group("public")
        // Limita el escaneo SOLO a tus controllers
        .packagesToScan("com.screenleads.backend.app.web.controller")
        // y a estos paths
        .pathsToMatch("/api/**", "/public/**", "/auth/**")
        .build();
  }
}
