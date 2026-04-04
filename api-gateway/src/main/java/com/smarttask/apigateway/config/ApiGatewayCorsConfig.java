package com.smarttask.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS at the gateway edge: the browser calls :8080 here, not auth-service directly.
 * Set {@code CORS_ALLOWED_ORIGIN} to the SPA origin(s), comma-separated (e.g. {@code http://PUBLIC_IP:3000}).
 * If the list is empty after parsing, allows all origins with pattern {@code *} (dev / loose EC2 only).
 */
@Configuration
public class ApiGatewayCorsConfig {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayCorsConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${CORS_ALLOWED_ORIGIN:http://localhost:3000}") String allowedOriginsRaw) {
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> patterns = origins.isEmpty() ? List.of("*") : origins;
        log.info("API Gateway CORS allowed origin patterns: {}", patterns);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
