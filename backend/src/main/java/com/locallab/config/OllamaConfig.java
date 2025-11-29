package com.locallab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Configuration properties for the Ollama integration.
 *
 * <p>This class binds to the {@code ollama} prefix in application properties, providing type-safe
 * access to Ollama-related configuration values. It includes connection settings, timeout values,
 * and retry configuration for resilient API calls.
 *
 * <p>Example configuration in {@code application.yml}:
 *
 * <pre>{@code
 * ollama:
 *   base-url: http://localhost:11434
 *   timeout-seconds: 120
 *   retry:
 *     max-attempts: 3
 *     initial-delay-ms: 1000
 *     multiplier: 2.0
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.client.OllamaClient
 */
@Data
@ConfigurationProperties(prefix = "ollama")
public class OllamaConfig {

    /**
     * The base URL of the Ollama server.
     *
     * <p>Default: {@code http://localhost:11434}
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * Request timeout in seconds for Ollama API calls.
     *
     * <p>This timeout applies to the entire request/response cycle, including connection
     * establishment and data transfer. Default: {@code 120}
     */
    private int timeoutSeconds = 120;

    /** Retry configuration for handling transient failures. */
    private RetryConfig retry = new RetryConfig();

    /**
     * Configuration for retry behaviour with exponential backoff.
     *
     * <p>When Ollama API calls fail due to transient errors (e.g., network issues, server
     * overload), the client will retry the request according to these settings.
     */
    @Data
    public static class RetryConfig {

        /**
         * Maximum number of retry attempts before giving up.
         *
         * <p>Default: {@code 3}
         */
        private int maxAttempts = 3;

        /**
         * Initial delay in milliseconds before the first retry.
         *
         * <p>Subsequent retries will increase this delay according to the multiplier. Default:
         * {@code 1000}
         */
        private long initialDelayMs = 1000;

        /**
         * Multiplier for exponential backoff.
         *
         * <p>Each retry delay is calculated as: {@code initialDelayMs * (multiplier ^ attempt)}
         * Default: {@code 2.0}
         */
        private double multiplier = 2.0;
    }
}
