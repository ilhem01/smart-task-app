package com.smarttask.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS via Spring MVC. Spring Security must call {@code http.cors(Customizer.withDefaults())}
 * so the same rules apply to the security filter chain (including preflight).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String ALLOWED_ORIGIN = "http://3.68.189.37:3000";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ALLOWED_ORIGIN)
                .allowedMethods("*")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
