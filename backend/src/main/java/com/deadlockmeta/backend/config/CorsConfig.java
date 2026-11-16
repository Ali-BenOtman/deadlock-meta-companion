package com.deadlockmeta.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        // The feedback form is the only place the site sends data, so it is the only
        // POST allowed.
        // It is registered first so it wins over the general rule below.
        registry.addMapping("/api/feedback")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("POST")
                .allowedHeaders("Content-Type")
                .maxAge(3600);
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET")
                .maxAge(3600);
    }
}