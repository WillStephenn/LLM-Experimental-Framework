package com.locallab.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a document chunk to be stored in the Chroma vector store.
 *
 * <p>This DTO encapsulates all data required to store a document chunk in Chroma, including the
 * text content, its vector embedding, and optional metadata. It is used when adding documents to
 * Chroma collections.
 *
 * <p>The embedding dimensions must match the collection's configured embedding model dimensions.
 * Mismatched dimensions will result in a storage error.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ChromaDocument doc = ChromaDocument.builder()
 *     .id("chunk-0")
 *     .content("The system uses a layered architecture...")
 *     .embedding(new double[]{0.1, 0.2, 0.3})
 *     .metadata(Map.of(
 *         "documentId", 1L,
 *         "chunkIndex", 0,
 *         "embeddingModel", "nomic-embed-text"
 *     ))
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see ChromaQueryResult
 * @see com.locallab.client.ChromaClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChromaDocument {

    /**
     * Unique identifier for the document chunk. Format: "chunk-{index}" or UUID.
     *
     * <p>This identifier must be unique within a collection and is used for retrieval and deletion
     * operations.
     */
    @NotBlank(message = "Document ID is required")
    private String id;

    /**
     * The text content of the document chunk.
     *
     * <p>This is the raw text that was extracted and chunked from the source document. It is stored
     * alongside the embedding for retrieval.
     */
    @NotBlank(message = "Content is required")
    private String content;

    /**
     * Vector embedding of the content.
     *
     * <p>Dimensions must match the collection's embedding model output. The embedding is used for
     * semantic similarity search.
     */
    @NotNull(message = "Embedding is required")
    private double[] embedding;

    /**
     * Additional metadata for the document chunk.
     *
     * <p>Standard metadata fields include:
     *
     * <ul>
     *   <li>{@code documentId} (Long): Reference to source Document entity
     *   <li>{@code chunkIndex} (Integer): Position of chunk within document
     *   <li>{@code embeddingModel} (String): Model used to generate embedding
     * </ul>
     */
    private Map<String, Object> metadata;
}
