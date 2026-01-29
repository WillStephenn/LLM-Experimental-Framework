package com.locallab.dto.response;

import java.util.List;

import com.locallab.dto.RetrievedChunk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for RAG (Retrieval-Augmented Generation) queries.
 *
 * <p>This class encapsulates the results of a RAG query, including the retrieved chunks and the
 * assembled context string ready for LLM prompts.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "query": "What is the architecture pattern?",
 *   "retrievedChunks": [
 *     {
 *       "content": "The system uses a layered architecture...",
 *       "distance": 0.15,
 *       "chunkIndex": 3
 *     }
 *   ],
 *   "assembledContext": "Context:\n\n[1] The system uses a layered architecture...\n\n",
 *   "embeddingModel": "nomic-embed-text"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.RagService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryResponse {

    /** The original query text that was searched. */
    private String query;

    /** The list of retrieved chunks ordered by relevance. */
    private List<RetrievedChunk> retrievedChunks;

    /** The assembled context string ready for injection into an LLM prompt. */
    private String assembledContext;

    /** The embedding model used for the query. */
    private String embeddingModel;
}
