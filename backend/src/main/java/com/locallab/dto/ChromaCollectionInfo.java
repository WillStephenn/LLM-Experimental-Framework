package com.locallab.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents information about a Chroma collection.
 *
 * <p>This DTO provides metadata about a vector collection in Chroma, including its name, document
 * count, and associated metadata. It is used for collection management operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ChromaCollectionInfo info = ChromaCollectionInfo.builder()
 *     .name("doc-1-nomic-embed-text")
 *     .documentCount(45)
 *     .metadata(Map.of("embeddingModel", "nomic-embed-text", "dimensions", 768))
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.client.ChromaClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChromaCollectionInfo {

    /**
     * Name of the collection.
     *
     * <p>Collection names follow the convention: {@code doc-{documentId}-{embeddingModel}}.
     */
    private String name;

    /**
     * Number of documents (chunks) stored in the collection.
     *
     * <p>This count represents the total number of vector embeddings stored, which corresponds to
     * the number of document chunks.
     */
    private Integer documentCount;

    /**
     * Collection metadata.
     *
     * <p>Typically includes:
     *
     * <ul>
     *   <li>{@code documentId} (Long): Reference to source Document entity
     *   <li>{@code embeddingModel} (String): Model used for embeddings in this collection
     *   <li>{@code dimensions} (Integer): Vector dimensionality
     * </ul>
     */
    private Map<String, Object> metadata;
}
