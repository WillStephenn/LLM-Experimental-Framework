package com.locallab.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for RAG (Retrieval-Augmented Generation) queries.
 *
 * <p>This class encapsulates all fields required to perform a RAG query against a document.
 * Validation annotations ensure that incoming requests meet the constraints defined in the API
 * contract.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RagQueryRequest request = RagQueryRequest.builder()
 *     .query("What is the architecture pattern?")
 *     .embeddingModel("nomic-embed-text")
 *     .topK(5)
 *     .chunkSize(500)
 *     .chunkOverlap(50)
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.RagService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryRequest {

    /**
     * The query text to search for in the document.
     *
     * <p>This text will be embedded and used for semantic similarity search.
     */
    @NotBlank(message = "Query is required")
    @Size(max = 10000, message = "Query must not exceed 10,000 characters")
    private String query;

    /**
     * The Ollama embedding model to use for the query.
     *
     * <p>This should match an embedding model configured in the system.
     */
    @NotBlank(message = "Embedding model is required")
    private String embeddingModel;

    /**
     * Number of top matching chunks to retrieve.
     *
     * <p>Defaults to 5 if not provided.
     */
    @Min(value = 1, message = "Top K must be at least 1")
    @Max(value = 20, message = "Top K must not exceed 20")
    @Builder.Default
    private Integer topK = 5;

    /**
     * Size of each chunk in characters.
     *
     * <p>Defaults to 500 if not provided.
     */
    @Min(value = 100, message = "Chunk size must be at least 100")
    @Max(value = 2000, message = "Chunk size must not exceed 2,000")
    @Builder.Default
    private Integer chunkSize = 500;

    /**
     * Number of overlapping characters between consecutive chunks.
     *
     * <p>Defaults to 50 if not provided.
     */
    @Min(value = 0, message = "Chunk overlap must not be negative")
    @Max(value = 500, message = "Chunk overlap must not exceed 500")
    @Builder.Default
    private Integer chunkOverlap = 50;
}
