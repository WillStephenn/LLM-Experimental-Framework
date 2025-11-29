package com.locallab.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing the result of an embedding generation operation from Ollama.
 *
 * <p>This class encapsulates the vector embedding along with metadata about the operation. The
 * embedding is a list of floating-point values representing the semantic meaning of the input text
 * in vector space.
 *
 * <p>Example response:
 *
 * <pre>{@code
 * EmbeddingResponse response = EmbeddingResponse.builder()
 *     .embedding(List.of(0.1, 0.2, 0.3, ...))
 *     .model("nomic-embed-text")
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.request.EmbeddingRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /**
     * The vector embedding as a list of floating-point values. The dimensionality depends on the
     * embedding model used.
     */
    private List<Double> embedding;

    /** The name of the model that generated this embedding. */
    private String model;

    /** The number of dimensions in the embedding vector. */
    private Integer dimensions;
}
