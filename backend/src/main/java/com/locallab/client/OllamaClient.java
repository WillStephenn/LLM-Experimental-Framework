package com.locallab.client;

import java.util.List;

import com.locallab.dto.request.EmbeddingRequest;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.EmbeddingResponse;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.exception.LocalLabException;

/**
 * Client interface for interacting with an Ollama instance.
 *
 * <p>This interface defines the contract for all Ollama operations required by the LocalLab
 * application. It provides methods for model discovery, availability checking, text generation, and
 * embedding generation.
 *
 * <p>Implementations of this interface should handle the underlying HTTP communication with the
 * Ollama service, including connection management, request serialisation, and response parsing.
 *
 * <p>This interface is designed to support dependency injection, enabling easy mocking in unit
 * tests without requiring a running Ollama instance.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Autowired
 * private OllamaClient ollamaClient;
 *
 * // Check availability before operations
 * if (ollamaClient.isAvailable()) {
 *     List<String> models = ollamaClient.listModels();
 *     GenerationResponse response = ollamaClient.generate(request);
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.request.GenerationRequest
 * @see com.locallab.dto.response.GenerationResponse
 * @see com.locallab.dto.request.EmbeddingRequest
 * @see com.locallab.dto.response.EmbeddingResponse
 */
public interface OllamaClient {

    /**
     * Retrieves a list of all available model names from the connected Ollama instance.
     *
     * <p>This method queries the Ollama service for all installed models and returns their names as
     * a list of strings. The returned names can be used directly in generation or embedding
     * requests.
     *
     * @return a list of available model names; returns an empty list if no models are installed
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Ollama service
     *     is unreachable or fails to respond
     */
    List<String> listModels();

    /**
     * Checks whether the Ollama service is available and responding.
     *
     * <p>This method performs a lightweight health check against the Ollama service to determine if
     * it is reachable and operational. It is designed to be a safe operation that does not throw
     * exceptions under normal circumstances.
     *
     * <p>Use this method to verify connectivity before performing generation or embedding
     * operations, or to implement health check endpoints.
     *
     * @return {@code true} if the Ollama service is reachable and responding; {@code false} if the
     *     connection fails, times out, or returns an error
     */
    boolean isAvailable();

    /**
     * Executes a text generation request against the Ollama service.
     *
     * <p>This method sends the provided generation request to the configured Ollama model and
     * returns the generated response along with performance metrics. The request specifies the
     * model, prompt, and optional hyperparameters that control the generation behaviour.
     *
     * <p>The returned response includes:
     *
     * <ul>
     *   <li>The generated text
     *   <li>Performance metrics (duration, tokens per second, time to first token)
     *   <li>Token counts (prompt and completion)
     * </ul>
     *
     * @param request the generation request containing the model, prompt, and parameters; must not
     *     be {@code null}
     * @return a {@link GenerationResponse} containing the generated text and metrics
     * @throws LocalLabException with {@code HttpStatus.BAD_REQUEST} if the request is invalid
     *     (e.g., missing required fields, invalid model name)
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Ollama service
     *     is unreachable or fails during generation
     * @throws LocalLabException with {@code HttpStatus.INTERNAL_SERVER_ERROR} for unexpected errors
     *     during generation
     */
    GenerationResponse generate(GenerationRequest request);

    /**
     * Generates vector embeddings for the input text using the specified embedding model.
     *
     * <p>This method sends the provided embedding request to an Ollama embedding model and returns
     * the resulting vector representation. Embeddings are used for semantic similarity
     * calculations, retrieval-augmented generation (RAG), and other vector-based operations.
     *
     * <p>The dimensionality of the returned embedding vector depends on the model used. Common
     * dimensions include 384, 768, and 1024.
     *
     * @param request the embedding request containing the model name and input text; must not be
     *     {@code null}
     * @return an {@link EmbeddingResponse} containing the embedding vector and model name
     * @throws LocalLabException with {@code HttpStatus.BAD_REQUEST} if the request is invalid
     *     (e.g., missing required fields, invalid model name)
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Ollama service
     *     is unreachable or fails during embedding generation
     * @throws LocalLabException with {@code HttpStatus.INTERNAL_SERVER_ERROR} for unexpected errors
     *     during embedding generation
     */
    EmbeddingResponse embed(EmbeddingRequest request);
}
