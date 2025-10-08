package com.screenleads.backend.app.infraestructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI baseOpenAPI() {
    return new OpenAPI().info(new Info()
        .title("ScreenLeads API")
        .version("v1")).components(new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

  }

  // Grupo "public" (el que consulta tu Swagger UI)
  @Bean
  public GroupedOpenApi publicGroup() {
    return GroupedOpenApi.builder()
        .group("public")
        .packagesToScan("com.screenleads.backend.app.web") // más amplio
        .pathsToMatch("/**") // sin limitar
        .build();
  }
}
