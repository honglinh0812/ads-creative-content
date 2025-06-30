package com.fbadsautomation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")  // Allow all origins for development
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(
                    "authorization",
                    "content-type", 
                    "x-auth-token",
                    "bypass-tunnel-reminder"  // Add bypass header for localtunnel
                )
                .exposedHeaders("x-auth-token")
                .allowCredentials(false)  // Set to false when allowing all origins
                .maxAge(3600);
    }
}