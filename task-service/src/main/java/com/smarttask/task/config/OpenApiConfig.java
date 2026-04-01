package com.smarttask.task.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI taskServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Service API")
                        .version("v1")
                        .description("Task CRUD and status updates. When calling this service directly, use **Authorize** with a JWT from Auth Service. Through API Gateway, the gateway validates the token.")
                        .contact(new Contact().name("Smart Task App")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local (direct)"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a JWT from POST /auth/login (use Auth Service or gateway).")));
    }
}
