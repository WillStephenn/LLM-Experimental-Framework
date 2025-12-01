package com.locallab.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single result from a Chroma similarity query.
 *
 * <p>This DTO encapsulates the result of a vector similarity search, including the matched document
 * chunk's content, its distance from the query embedding, and associated metadata.
 *
 * <p>Distance values represent the dissimilarity between the query and the result. Lower values
 * indicate higher semantic similarity.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ChromaQueryResult result = ChromaQueryResult.builder()
 *     .id("chunk-0")
 *     .content("The system uses a layered architecture...")
 *     .distance(0.15)
 *     .metadata(Map.of("documentId", 1L, "chunkIndex", 0))
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see ChromaDocument
 * @see com.locallab.client.ChromaClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChromaQueryResult {

    /**
     * Unique identifier of the matched document chunk.
     *
     * <p>This is the same identifier that was assigned when the document was stored.
     */
    private String id;

    /**
     * The text content of the matched chunk.
     *
     * <p>This is the original text content that was stored alongside the embedding.
     */
    private String content;

    /**
     * Distance score from the query embedding.
     *
     * <p>Lower values indicate higher similarity. The exact range depends on the distance metric
     * used by the collection (typically cosine distance, where values range from 0 to 2).
     */
    private Double distance;

    /**
     * Metadata associated with the document chunk.
     *
     * <p>Contains the same metadata that was stored with the document, typically including:
     *
     * <ul>
     *   <li>{@code documentId} (Long): Reference to source Document entity
     *   <li>{@code chunkIndex} (Integer): Position of chunk within document
     *   <li>{@code embeddingModel} (String): Model used to generate embedding
     * </ul>
     */
    private Map<String, Object> metadata;
}
