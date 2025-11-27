package com.locallab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for CORS settings.
 *
 * <p>These properties are bound from the {@code cors} prefix in application.yml. They allow
 * fine-grained control over Cross-Origin Resource Sharing behaviour for the LocalLab API.
 *
 * <p>Example configuration:
 *
 * <pre>
 * cors:
 *   allowed-origins: http://localhost:5173
 *   allowed-methods: GET,POST,PUT,DELETE
 *   allowed-headers: "*"
 *   allow-credentials: true
 *   max-age: 3600
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * Comma-separated list of allowed origins for CORS requests. Use "*" to allow all origins (not
     * recommended for production with credentials).
     */
    private String allowedOrigins = "http://localhost:5173";

    /** Comma-separated list of allowed HTTP methods. */
    private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";

    /** Comma-separated list of allowed headers, or "*" for all headers. */
    private String allowedHeaders = "*";

    /** Whether credentials (cookies, authorisation headers) are supported. */
    private boolean allowCredentials = true;

    /** Maximum age (in seconds) of the preflight request cache. */
    private long maxAge = 3600;

    /**
     * Returns the allowed origins for CORS requests.
     *
     * @return comma-separated list of allowed origins
     */
    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Sets the allowed origins for CORS requests.
     *
     * @param allowedOrigins comma-separated list of allowed origins
     */
    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Returns the allowed HTTP methods.
     *
     * @return comma-separated list of allowed methods
     */
    public String getAllowedMethods() {
        return allowedMethods;
    }

    /**
     * Sets the allowed HTTP methods.
     *
     * @param allowedMethods comma-separated list of allowed methods
     */
    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    /**
     * Returns the allowed headers.
     *
     * @return comma-separated list of allowed headers, or "*" for all
     */
    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    /**
     * Sets the allowed headers.
     *
     * @param allowedHeaders comma-separated list of allowed headers, or "*" for all
     */
    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    /**
     * Returns whether credentials are allowed.
     *
     * @return true if credentials are supported
     */
    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    /**
     * Sets whether credentials are allowed.
     *
     * @param allowCredentials true to support credentials
     */
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    /**
     * Returns the maximum age of the preflight cache.
     *
     * @return cache duration in seconds
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Sets the maximum age of the preflight cache.
     *
     * @param maxAge cache duration in seconds
     */
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
