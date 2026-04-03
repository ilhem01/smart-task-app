package com.smarttask.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS for all URL paths. Spring Security must enable CORS
 * ({@code Customizer.withDefaults()}) so browser preflight succeeds.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://3.68.189.37:3000")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
