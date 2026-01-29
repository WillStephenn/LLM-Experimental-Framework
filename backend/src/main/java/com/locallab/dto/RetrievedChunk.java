package com.locallab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a chunk of text retrieved from a RAG (Retrieval-Augmented Generation) query.
 *
 * <p>This DTO encapsulates the result of a semantic similarity search, containing the text content
 * of a document chunk along with its distance score and position within the source document.
 *
 * <p>Distance values represent the dissimilarity between the query and the chunk. Lower values
 * indicate higher semantic similarity.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RetrievedChunk chunk = RetrievedChunk.builder()
 *     .content("The system uses a layered architecture...")
 *     .distance(0.15)
 *     .chunkIndex(3)
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
public class RetrievedChunk {

    /**
     * The text content of the retrieved chunk.
     *
     * <p>This is the original text that was stored during document processing.
     */
    private String content;

    /**
     * Distance score from the query embedding.
     *
     * <p>Lower values indicate higher semantic similarity. The exact range depends on the distance
     * metric used (typically cosine distance, where values range from 0 to 2).
     */
    private Double distance;

    /**
     * The position of this chunk within the source document.
     *
     * <p>Zero-indexed, representing the order in which chunks were created during document
     * processing.
     */
    private Integer chunkIndex;
}
