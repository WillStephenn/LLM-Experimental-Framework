package com.locallab.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.dto.response.ModelsResponse;
import com.locallab.dto.response.OllamaStatusResponse;
import com.locallab.service.OllamaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for Ollama integration endpoints.
 *
 * <p>This controller provides endpoints for interacting with the Ollama service, including model
 * listing, connectivity checking, and text generation. All endpoints follow the API contract
 * defined in the project specification.
 *
 * <h3>Endpoint Summary:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/ollama/models} - List all available models
 *   <li>{@code GET /api/ollama/status} - Check Ollama connectivity
 *   <li>{@code POST /api/ollama/generate} - Execute a single generation request
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract:
 *
 * <ul>
 *   <li>400 Bad Request - Validation errors (invalid request body)
 *   <li>503 Service Unavailable - Ollama service unreachable
 * </ul>
 *
 * @author William Stephen
 * @see OllamaService
 * @see GenerationRequest
 * @see GenerationResponse
 * @see OllamaStatusResponse
 */
@RestController
@RequestMapping("/api/ollama")
@RequiredArgsConstructor
public class OllamaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaController.class);

    private final OllamaService ollamaService;

    /**
     * Retrieves all available models from the Ollama instance.
     *
     * <p>This endpoint queries the connected Ollama service for all installed models and returns
     * their names in a response object.
     *
     * @return a response containing the list of available model names
     */
    @GetMapping("/models")
    public ResponseEntity<ModelsResponse> listModels() {
        LOGGER.info("Received request to list available Ollama models");

        ModelsResponse response =
                ModelsResponse.builder().models(ollamaService.getAvailableModels()).build();

        LOGGER.debug("Returning {} models", response.getModels().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Checks connectivity to the Ollama service.
     *
     * <p>This endpoint performs a health check against the Ollama service and returns a status
     * response containing availability information, the base URL, and the number of available
     * models.
     *
     * @return a status response indicating Ollama availability
     */
    @GetMapping("/status")
    public ResponseEntity<OllamaStatusResponse> checkStatus() {
        LOGGER.info("Received request to check Ollama status");

        OllamaStatusResponse status = ollamaService.checkConnectivity();

        LOGGER.debug(
                "Ollama status - available: {}, models: {}",
                status.getAvailable(),
                status.getModelCount());
        return ResponseEntity.ok(status);
    }

    /**
     * Executes a single text generation request.
     *
     * <p>This endpoint sends the provided generation request to the configured Ollama model and
     * returns the generated response along with performance metrics.
     *
     * @param request the generation request containing the model, prompt, and parameters
     * @return the generation response with output text and metrics
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerationResponse> generate(
            @Valid @RequestBody GenerationRequest request) {
        LOGGER.info("Received generation request for model: {}", request.getModel());

        GenerationResponse response = ollamaService.generate(request);

        LOGGER.info(
                "Generation completed - model: {}, duration: {}ms",
                response.getModel(),
                response.getDurationMs());
        return ResponseEntity.ok(response);
    }
}
