package com.smarttask.apigateway.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Task API Gateway")
                        .version("v1")
                        .description("""
                                Routes **/auth/** to Auth Service and **/tasks/** to Task Service (JWT required for tasks).

                                Use the **Explore** dropdown in Swagger UI to load **Auth Service** or **Task Service** OpenAPI (proxied at `/openapi/auth/v3/api-docs` and `/openapi/tasks/v3/api-docs`).

                                Call **POST /auth/login** first, then **Authorize** with `Bearer <token>` when using Task Service operations through the gateway (`http://localhost:8080`)."""))
                .externalDocs(new ExternalDocumentation()
                        .description("Auth + Task services (direct ports)")
                        .url("http://localhost:8081/swagger-ui.html"));
    }
}
