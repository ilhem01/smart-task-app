package com.smarttask.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("v1")
                        .description("User registration and JWT-based login for Smart Task App.")
                        .contact(new Contact().name("Smart Task App")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local (direct)"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")));
    }
}
