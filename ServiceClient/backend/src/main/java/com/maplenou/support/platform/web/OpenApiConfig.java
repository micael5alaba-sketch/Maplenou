package com.maplenou.support.platform.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI maplenouOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Maplenou Customer Service API")
                        .version("0.2.0")
                        .description("Ticket management for customers, sellers and delivery drivers, handled by "
                                + "support agents. Errors follow RFC 9457 (application/problem+json) with a stable "
                                + "'code' and, when relevant, the business 'rule' (BR-xxx)."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
