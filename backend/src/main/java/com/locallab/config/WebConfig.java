package com.locallab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the LocalLab application.
 *
 * <p>This configuration class handles Cross-Origin Resource Sharing (CORS) settings to allow the
 * React frontend (running on localhost:5173) to communicate with the Spring Boot backend (running
 * on localhost:8080).
 *
 * <p>CORS settings are loaded from {@link CorsProperties} which reads from application.yml.
 * Different profiles (dev, prod) can override these settings as needed.
 *
 * @see CorsProperties
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * Constructs WebConfig with the specified CORS properties.
     *
     * @param corsProperties the CORS configuration properties
     */
    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * Configures CORS mappings for the application.
     *
     * <p>This method applies CORS settings to all API endpoints ({@code /api/**}) based on the
     * configuration in application.yml. The settings include:
     *
     * <ul>
     *   <li>Allowed origins (e.g., http://localhost:5173 for development)
     *   <li>Allowed HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
     *   <li>Allowed headers
     *   <li>Credentials support
     *   <li>Preflight cache duration
     * </ul>
     *
     * @param registry the CORS registry to configure
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = corsProperties.getAllowedOrigins().split(",");
        String[] methods = corsProperties.getAllowedMethods().split(",");
        String[] headers = corsProperties.getAllowedHeaders().split(",");

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods(methods)
                .allowedHeaders(headers)
                .allowCredentials(corsProperties.isAllowCredentials())
                .maxAge(corsProperties.getMaxAge());

        // Also configure CORS for H2 console access if needed
        registry.addMapping("/h2-console/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST")
                .allowedHeaders(headers)
                .allowCredentials(corsProperties.isAllowCredentials());
    }
}
