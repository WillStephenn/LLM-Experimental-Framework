package com.locallab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Configuration properties for the Chroma vector store integration.
 *
 * <p>This class binds to the {@code chroma} prefix in application properties, providing type-safe
 * access to Chroma-related configuration values. It includes connection settings and timeout
 * configuration for HTTP calls to the Chroma REST API.
 *
 * <p>Example configuration in {@code application.yml}:
 *
 * <pre>{@code
 * chroma:
 *   base-url: http://localhost:8000
 *   timeout-seconds: 30
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.client.ChromaClient
 */
@Data
@ConfigurationProperties(prefix = "chroma")
public class ChromaConfig {

    /**
     * The base URL of the Chroma server.
     *
     * <p>Default: {@code http://localhost:8000}
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * Request timeout in seconds for Chroma API calls.
     *
     * <p>This timeout applies to the entire request/response cycle, including connection
     * establishment and data transfer. Default: {@code 30}
     */
    private int timeoutSeconds = 30;
}
