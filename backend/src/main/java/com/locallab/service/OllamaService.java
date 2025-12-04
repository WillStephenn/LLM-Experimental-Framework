package com.locallab.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.locallab.client.OllamaClient;
import com.locallab.config.OllamaConfig;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.dto.response.OllamaStatusResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for Ollama operations.
 *
 * <p>This service provides high-level methods for interacting with the Ollama instance, including
 * model listing, connectivity checking, and text generation. It encapsulates the {@link
 * OllamaClient} and handles error conversion to appropriate standard exceptions.
 *
 * <p>All operations are logged at appropriate levels:
 *
 * <ul>
 *   <li>INFO: Model listing, connectivity checks
 *   <li>DEBUG: Request/response details
 *   <li>ERROR: Service failures
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Autowired
 * private OllamaService ollamaService;
 *
 * // Check connectivity
 * OllamaStatusResponse status = ollamaService.checkConnectivity();
 *
 * // List available models
 * List<String> models = ollamaService.getAvailableModels();
 *
 * // Generate response
 * GenerationRequest request = GenerationRequest.builder()
 *     .model("qwen2.5-coder:7b")
 *     .prompt("Explain dependency injection")
 *     .build();
 * GenerationResponse response = ollamaService.generate(request);
 * }</pre>
 *
 * @author William Stephen
 * @see OllamaClient
 * @see OllamaConfig
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OllamaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaService.class);

    private final OllamaClient ollamaClient;
    private final OllamaConfig ollamaConfig;

    /**
     * Retrieves all available models from the Ollama instance.
     *
     * <p>This method queries the connected Ollama service for all installed models and returns
     * their names as a list of strings. The returned names can be used directly in generation
     * requests.
     *
     * @return list of model names available in the Ollama instance
     * @throws ResponseStatusException with {@code HttpStatus.SERVICE_UNAVAILABLE} if Ollama is
     *     unreachable
     */
    public List<String> getAvailableModels() {
        LOGGER.info("Retrieving available models from Ollama");

        try {
            List<String> models = ollamaClient.listModels();
            LOGGER.info("Retrieved {} models from Ollama", models.size());
            LOGGER.debug("Available models: {}", models);
            return models;
        } catch (ResponseStatusException e) {
            LOGGER.error("Failed to retrieve models from Ollama: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error retrieving models from Ollama: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to retrieve models from Ollama: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Checks connectivity to the Ollama service.
     *
     * <p>This method performs a health check against the Ollama service and returns a status
     * response containing availability information, the base URL, and the number of available
     * models if the service is reachable.
     *
     * <p>Unlike other methods, this method does not throw exceptions when Ollama is unavailable.
     * Instead, it returns a status response with {@code available = false} and an appropriate
     * message.
     *
     * @return status response with availability and model count
     */
    public OllamaStatusResponse checkConnectivity() {
        LOGGER.info("Checking Ollama connectivity at {}", ollamaConfig.getBaseUrl());

        boolean available = ollamaClient.isAvailable();

        if (available) {
            try {
                List<String> models = ollamaClient.listModels();
                OllamaStatusResponse response =
                        OllamaStatusResponse.builder()
                                .available(true)
                                .baseUrl(ollamaConfig.getBaseUrl())
                                .modelCount(models.size())
                                .message("Ollama is running")
                                .build();
                LOGGER.info("Ollama is available with {} models", models.size());
                return response;
            } catch (Exception e) {
                LOGGER.warn("Ollama ping succeeded but model listing failed: {}", e.getMessage());
                return OllamaStatusResponse.builder()
                        .available(true)
                        .baseUrl(ollamaConfig.getBaseUrl())
                        .modelCount(0)
                        .message("Ollama is running but model listing failed: " + e.getMessage())
                        .build();
            }
        } else {
            LOGGER.warn("Ollama is not available at {}", ollamaConfig.getBaseUrl());
            return OllamaStatusResponse.builder()
                    .available(false)
                    .baseUrl(ollamaConfig.getBaseUrl())
                    .modelCount(0)
                    .message("Ollama is not available")
                    .build();
        }
    }

    /**
     * Executes a text generation request.
     *
     * <p>This method sends the provided generation request to the configured Ollama model and
     * returns the generated response along with performance metrics. The request specifies the
     * model, prompt, and optional hyperparameters that control the generation behaviour.
     *
     * <p>Validation is performed on the request:
     *
     * <ul>
     *   <li>Request must not be null
     *   <li>Model name must not be blank
     *   <li>Prompt must not be blank
     * </ul>
     *
     * @param request the generation request with model and prompt
     * @return generation response with output and metrics
     * @throws IllegalArgumentException if the request is null or contains invalid parameters
     * @throws ResponseStatusException with {@code HttpStatus.SERVICE_UNAVAILABLE} if generation
     *     fails due to connectivity issues
     * @throws ResponseStatusException with {@code HttpStatus.BAD_REQUEST} if the model is not found
     *     or the request is invalid
     */
    public GenerationResponse generate(GenerationRequest request) {
        validateGenerationRequest(request);

        LOGGER.info("Generating response with model: {}", request.getModel());
        LOGGER.debug(
                "Generation request - prompt length: {}, temperature: {}, topP: {}, topK: {}",
                request.getPrompt().length(),
                request.getTemperature(),
                request.getTopP(),
                request.getTopK());

        try {
            GenerationResponse response = ollamaClient.generate(request);

            LOGGER.info(
                    "Generation completed - model: {}, duration: {}ms",
                    response.getModel(),
                    response.getDurationMs());
            LOGGER.debug(
                    "Generation metrics - tokens/s: {}, TTFT: {}ms, prompt tokens: {}, "
                            + "completion tokens: {}",
                    response.getTokensPerSecond(),
                    response.getTimeToFirstTokenMs(),
                    response.getPromptTokens(),
                    response.getCompletionTokens());

            return response;
        } catch (ResponseStatusException e) {
            LOGGER.error("Generation failed for model {}: {}", request.getModel(), e.getReason());
            throw e;
        } catch (Exception e) {
            LOGGER.error(
                    "Unexpected error during generation with model {}: {}",
                    request.getModel(),
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the generation request parameters.
     *
     * @param request the request to validate
     * @throws IllegalArgumentException if the request is null or contains invalid parameters
     */
    private void validateGenerationRequest(GenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Generation request must not be null");
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new IllegalArgumentException("Model name must not be blank");
        }
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }
    }
}
