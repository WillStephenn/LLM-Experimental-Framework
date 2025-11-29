package com.locallab.client.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.locallab.client.OllamaClient;
import com.locallab.config.OllamaConfig;
import com.locallab.dto.request.EmbeddingRequest;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.EmbeddingResponse;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.exception.LocalLabException;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.embeddings.OllamaEmbedRequestModel;
import io.github.ollama4j.models.embeddings.OllamaEmbedResponseModel;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;

/**
 * Implementation of {@link OllamaClient} using the ollama4j library.
 *
 * <p>This class provides the concrete implementation for interacting with an Ollama server. It
 * handles connection management, request serialisation, response parsing, and error handling. The
 * implementation includes retry logic with exponential backoff for resilience against transient
 * failures.
 *
 * <p>All API interactions are logged at DEBUG level for troubleshooting, with WARN level logs for
 * retry attempts and ERROR level for unrecoverable failures.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Autowired
 * private OllamaClient ollamaClient;
 *
 * // Check availability
 * if (ollamaClient.isAvailable()) {
 *     List<String> models = ollamaClient.listModels();
 *     GenerationResponse response = ollamaClient.generate(request);
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see OllamaClient
 * @see OllamaConfig
 */
@Component
@EnableConfigurationProperties(OllamaConfig.class)
public class OllamaClientImpl implements OllamaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaClientImpl.class);

    private final OllamaAPI ollamaApi;
    private final OllamaConfig config;

    /**
     * Constructs a new OllamaClientImpl with the specified configuration.
     *
     * @param config the Ollama configuration containing connection and retry settings
     */
    @Autowired
    public OllamaClientImpl(OllamaConfig config) {
        this.config = config;
        this.ollamaApi = createOllamaApi(config);
        LOGGER.info(
                "OllamaClient initialised with base URL: {}, timeout: {}s",
                config.getBaseUrl(),
                config.getTimeoutSeconds());
    }

    /**
     * Constructor for testing with a custom OllamaAPI instance.
     *
     * @param config the Ollama configuration
     * @param ollamaApi the OllamaAPI instance to use
     */
    OllamaClientImpl(OllamaConfig config, OllamaAPI ollamaApi) {
        this.config = config;
        this.ollamaApi = ollamaApi;
    }

    /**
     * Creates and configures an OllamaAPI instance.
     *
     * @param config the configuration settings
     * @return the configured OllamaAPI instance
     */
    private OllamaAPI createOllamaApi(OllamaConfig config) {
        OllamaAPI api = new OllamaAPI(config.getBaseUrl());
        api.setRequestTimeoutSeconds(config.getTimeoutSeconds());
        return api;
    }

    @Override
    public List<String> listModels() {
        LOGGER.debug("Listing available models from Ollama");
        return executeWithRetry(
                () -> {
                    List<Model> models = ollamaApi.listModels();
                    List<String> modelNames =
                            models.stream().map(Model::getName).collect(Collectors.toList());
                    LOGGER.debug("Retrieved {} models from Ollama", modelNames.size());
                    return modelNames;
                },
                "listModels");
    }

    @Override
    public boolean isAvailable() {
        LOGGER.debug("Checking Ollama availability at {}", config.getBaseUrl());
        try {
            boolean available = ollamaApi.ping();
            LOGGER.debug("Ollama availability check result: {}", available);
            return available;
        } catch (Exception e) {
            LOGGER.debug("Ollama is not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        LOGGER.debug(
                "Generating response with model: {}, prompt length: {}",
                request.getModel(),
                request.getPrompt().length());

        return executeWithRetry(
                () -> {
                    long startTime = System.currentTimeMillis();

                    Options options = buildOptions(request);
                    OllamaResult result =
                            ollamaApi.generate(
                                    request.getModel(),
                                    buildPromptWithSystem(request),
                                    false,
                                    options,
                                    null);

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    GenerationResponse response =
                            mapToGenerationResponse(result, request, duration);

                    LOGGER.debug(
                            "Generation completed in {}ms, tokens/s: {}",
                            duration,
                            response.getTokensPerSecond());

                    return response;
                },
                "generate");
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        LOGGER.debug(
                "Generating embedding with model: {}, input length: {}",
                request.getModel(),
                request.getInput().length());

        return executeWithRetry(
                () -> {
                    OllamaEmbedRequestModel embedRequest =
                            new OllamaEmbedRequestModel(
                                    request.getModel(),
                                    Collections.singletonList(request.getInput()));

                    OllamaEmbedResponseModel result = ollamaApi.embed(embedRequest);

                    List<Double> embedding =
                            result.getEmbeddings().isEmpty()
                                    ? Collections.emptyList()
                                    : result.getEmbeddings().get(0);

                    EmbeddingResponse response =
                            EmbeddingResponse.builder()
                                    .embedding(embedding)
                                    .model(request.getModel())
                                    .dimensions(embedding.size())
                                    .build();

                    LOGGER.debug(
                            "Embedding generated with {} dimensions", response.getDimensions());

                    return response;
                },
                "embed");
    }

    /**
     * Builds the complete prompt string, prepending system prompt if provided.
     *
     * @param request the generation request
     * @return the complete prompt string
     */
    private String buildPromptWithSystem(GenerationRequest request) {
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            return request.getSystemPrompt()
                    + "\n\n---System Prompt End ---\n\n"
                    + request.getPrompt();
        }
        return request.getPrompt();
    }

    /**
     * Builds ollama4j Options from the generation request parameters.
     *
     * @param request the generation request containing hyperparameters
     * @return the configured Options object
     */
    private Options buildOptions(GenerationRequest request) {
        OptionsBuilder builder = new OptionsBuilder();

        if (request.getTemperature() != null) {
            builder.setTemperature(request.getTemperature().floatValue());
        }
        if (request.getTopP() != null) {
            builder.setTopP(request.getTopP().floatValue());
        }
        if (request.getTopK() != null) {
            builder.setTopK(request.getTopK());
        }
        if (request.getContextWindow() != null) {
            builder.setNumCtx(request.getContextWindow());
        }
        if (request.getMaxTokens() != null) {
            builder.setNumPredict(request.getMaxTokens());
        }

        return builder.build();
    }

    /**
     * Maps an OllamaResult to a GenerationResponse DTO.
     *
     * @param result the ollama4j result
     * @param request the original request
     * @param durationMs the total duration in milliseconds
     * @return the mapped GenerationResponse
     */
    private GenerationResponse mapToGenerationResponse(
            OllamaResult result, GenerationRequest request, long durationMs) {

        Double tokensPerSecond = null;
        Long timeToFirstToken = null;
        Integer promptTokens = null;
        Integer completionTokens = null;

        // Extract metrics from OllamaResult if available
        if (result.getEvalCount() != null && result.getEvalDuration() != null) {
            // evalDuration is in nanoseconds
            double evalDurationSeconds = result.getEvalDuration() / 1_000_000_000.0;
            if (evalDurationSeconds > 0) {
                tokensPerSecond = result.getEvalCount() / evalDurationSeconds;
            }
            completionTokens = result.getEvalCount();
        }

        if (result.getPromptEvalCount() != null) {
            promptTokens = result.getPromptEvalCount();
        }

        // Calculate time to first token from prompt eval duration
        if (result.getPromptEvalDuration() != null) {
            // Convert nanoseconds to milliseconds
            timeToFirstToken = result.getPromptEvalDuration() / 1_000_000;
        }

        return GenerationResponse.builder()
                .response(result.getResponse())
                .model(request.getModel())
                .durationMs(durationMs)
                .tokensPerSecond(tokensPerSecond)
                .timeToFirstTokenMs(timeToFirstToken)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
    }

    /**
     * Executes an operation with retry logic and exponential backoff.
     *
     * @param operation the operation to execute
     * @param operationName the name of the operation for logging
     * @param <T> the return type
     * @return the result of the operation
     * @throws LocalLabException if all retry attempts fail
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) {
        int maxAttempts = config.getRetry().getMaxAttempts();
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxAttempts) {
            try {
                return operation.execute();
            } catch (OllamaBaseException e) {
                lastException = e;
                attempts++;
                if (!shouldRetryOllamaException(e, attempts, maxAttempts, operationName)) {
                    break;
                }
            } catch (IOException e) {
                lastException = e;
                attempts++;
                if (!shouldRetryIoException(e, attempts, maxAttempts, operationName)) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LocalLabException(
                        "Operation interrupted", e, HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                LOGGER.error(
                        "Ollama {} failed with unexpected error: {}",
                        operationName,
                        e.getMessage());
                throw mapToLocalLabException(e, operationName);
            }
        }
        logFinalFailure(operationName, maxAttempts, lastException);
        throw mapToLocalLabException(lastException, operationName);
    }

    private boolean shouldRetryOllamaException(
            OllamaBaseException e, int attempts, int maxAttempts, String operationName) {
        if (isRetryable(e) && attempts < maxAttempts) {
            logRetryAndSleep(operationName, attempts, maxAttempts, e.getMessage());
            return true;
        }
        return false;
    }

    private boolean shouldRetryIoException(
            IOException e, int attempts, int maxAttempts, String operationName) {
        if (attempts < maxAttempts) {
            logRetryAndSleep(operationName, attempts, maxAttempts, e.getMessage());
            return true;
        }
        return false;
    }

    private void logRetryAndSleep(String operationName, int attempts, int maxAttempts, String msg) {
        long delay =
                calculateDelay(
                        config.getRetry().getInitialDelayMs(),
                        config.getRetry().getMultiplier(),
                        attempts - 1);
        LOGGER.warn(
                "Ollama {} failed (attempt {}/{}), retrying in {}ms: {}",
                operationName,
                attempts,
                maxAttempts,
                delay,
                msg);
        sleep(delay);
    }

    private void logFinalFailure(String operationName, int maxAttempts, Exception lastException) {
        LOGGER.error(
                "Ollama {} failed after {} attempts: {}",
                operationName,
                maxAttempts,
                lastException != null ? lastException.getMessage() : "Unknown error");
    }

    /**
     * Determines if an exception is retryable.
     *
     * @param e the exception to check
     * @return true if the operation should be retried
     */
    private boolean isRetryable(OllamaBaseException e) {
        String message = e.getMessage();
        if (message == null) {
            return true;
        }
        // Don't retry for invalid model or bad request errors
        String lowerMessage = message.toLowerCase();
        return !lowerMessage.contains("model")
                && !lowerMessage.contains("invalid")
                && !lowerMessage.contains("not found");
    }

    /**
     * Calculates the delay for exponential backoff.
     *
     * @param initialDelay the initial delay in milliseconds
     * @param multiplier the backoff multiplier
     * @param attempt the current attempt number (0-based)
     * @return the delay in milliseconds
     */
    private long calculateDelay(long initialDelay, double multiplier, int attempt) {
        return (long) (initialDelay * Math.pow(multiplier, attempt));
    }

    /**
     * Sleeps for the specified duration.
     *
     * @param delayMs the delay in milliseconds
     */
    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LocalLabException("Sleep interrupted", e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Maps an exception to a LocalLabException with an appropriate HTTP status.
     *
     * @param e the exception to map
     * @param operationName the name of the operation that failed
     * @return the mapped LocalLabException
     */
    private LocalLabException mapToLocalLabException(Exception e, String operationName) {
        if (e == null) {
            return new LocalLabException(
                    "Ollama " + operationName + " failed: Unknown error",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }

        // Determine appropriate HTTP status based on error type
        HttpStatus status;
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("model") && lowerMessage.contains("not found")) {
            status = HttpStatus.BAD_REQUEST;
        } else if (lowerMessage.contains("invalid")) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof IOException
                || lowerMessage.contains("connection")
                || lowerMessage.contains("timeout")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return new LocalLabException("Ollama " + operationName + " failed: " + message, e, status);
    }

    /**
     * Functional interface for retryable operations.
     *
     * @param <T> the return type of the operation
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        /**
         * Executes the operation.
         *
         * @return the result
         * @throws OllamaBaseException if an Ollama-specific error occurs
         * @throws IOException if an I/O error occurs
         * @throws InterruptedException if the operation is interrupted
         * @throws URISyntaxException if a URI is malformed
         */
        T execute()
                throws OllamaBaseException, IOException, InterruptedException, URISyntaxException;
    }
}
