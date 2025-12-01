package com.locallab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating vector embeddings via Ollama.
 *
 * <p>This class encapsulates the parameters required to generate embeddings for a given input text.
 * Embeddings are vector representations used for semantic similarity, retrieval-augmented
 * generation (RAG), and other vector-based operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EmbeddingRequest request = EmbeddingRequest.builder()
 *     .model("nomic-embed-text")
 *     .input("What is the architecture pattern used in this project?")
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.response.EmbeddingResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /** The name of the embedding model to use (e.g., "nomic-embed-text"). */
    @NotBlank(message = "Model name is required")
    private String model;

    /** The input text to generate embeddings for. */
    @NotBlank(message = "Input text is required")
    @Size(max = 50000, message = "Input text must not exceed 50,000 characters")
    private String input;
}
