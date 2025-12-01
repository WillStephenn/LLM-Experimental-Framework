package com.locallab.client;

import java.util.List;

import com.locallab.dto.ChromaDocument;
import com.locallab.dto.ChromaQueryResult;
import com.locallab.exception.LocalLabException;

/**
 * Client interface for interacting with a Chroma vector store instance.
 *
 * <p>This interface defines the contract for all Chroma operations required by the LocalLab
 * application. It provides methods for collection management, document insertion, and semantic
 * similarity querying.
 *
 * <p>Implementations of this interface should handle the underlying HTTP communication with the
 * Chroma service, including connection management, request serialisation, and response parsing.
 *
 * <p>This interface is designed to support dependency injection, enabling easy mocking in unit
 * tests without requiring a running Chroma instance.
 *
 * <p>Collection naming convention: {@code doc-{documentId}-{embeddingModel}}
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Autowired
 * private ChromaClient chromaClient;
 *
 * // Create a collection for document embeddings
 * chromaClient.createCollection("doc-1-nomic-embed-text", 768);
 *
 * // Add documents to the collection
 * List<ChromaDocument> documents = List.of(
 *     ChromaDocument.builder()
 *         .id("chunk-0")
 *         .content("The system uses a layered architecture...")
 *         .embedding(new double[]{0.1, 0.2, ...})
 *         .build()
 * );
 * chromaClient.addDocuments("doc-1-nomic-embed-text", documents);
 *
 * // Query for similar documents
 * double[] queryEmbedding = new double[]{0.15, 0.25, ...};
 * List<ChromaQueryResult> results = chromaClient.query("doc-1-nomic-embed-text", queryEmbedding, 5);
 * }</pre>
 *
 * @author William Stephen
 * @see ChromaDocument
 * @see ChromaQueryResult
 */
public interface ChromaClient {

    /**
     * Creates a new vector collection in Chroma.
     *
     * <p>This method creates an empty collection that can store vector embeddings of the specified
     * dimensionality. The collection must be created before documents can be added.
     *
     * @param name the collection identifier, typically following the format {@code
     *     doc-{documentId}-{embeddingModel}}
     * @param dimensions the vector dimensionality, which must match the embedding model output
     *     (e.g., 768 for nomic-embed-text)
     * @throws LocalLabException with {@code HttpStatus.CONFLICT} if a collection with the same name
     *     already exists
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Chroma service
     *     is unreachable or fails to respond
     */
    void createCollection(String name, int dimensions);

    /**
     * Removes a collection and all its documents from Chroma.
     *
     * <p>This method permanently deletes the specified collection along with all stored embeddings
     * and documents. This operation is idempotent - calling it on a non-existent collection will
     * not throw an exception.
     *
     * @param name the collection identifier to delete
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Chroma service
     *     is unreachable or fails to respond
     */
    void deleteCollection(String name);

    /**
     * Checks whether a collection exists in Chroma.
     *
     * <p>This method performs a lookup to determine if a collection with the given name exists. It
     * is designed to be a safe operation that does not throw exceptions for non-existent
     * collections.
     *
     * @param name the collection identifier to check
     * @return {@code true} if the collection exists; {@code false} otherwise
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Chroma service
     *     is unreachable or fails to respond
     */
    boolean collectionExists(String name);

    /**
     * Inserts documents with embeddings into a collection.
     *
     * <p>This method adds multiple documents to the specified collection. Each document includes an
     * identifier, text content, vector embedding, and optional metadata. The embedding dimensions
     * must match the collection's configured dimensions.
     *
     * <p>Documents with duplicate IDs will be rejected by Chroma. To update an existing document,
     * delete it first and then re-add.
     *
     * @param collection the target collection name
     * @param documents the list of documents to insert, each containing id, content, embedding, and
     *     optional metadata
     * @throws LocalLabException with {@code HttpStatus.NOT_FOUND} if the collection does not exist
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Chroma service
     *     is unreachable or fails to respond
     */
    void addDocuments(String collection, List<ChromaDocument> documents);

    /**
     * Performs a similarity search in a collection.
     *
     * <p>This method queries the specified collection using the provided embedding vector and
     * returns the most similar documents. Results are ordered by distance, with the most similar
     * documents first.
     *
     * <p>Distance values represent dissimilarity - lower values indicate higher semantic
     * similarity. The exact range depends on the distance metric used (typically cosine distance).
     *
     * @param collection the collection to search
     * @param embedding the query embedding vector, must have the same dimensions as the collection
     * @param topK the maximum number of results to return
     * @return a list of query results ordered by ascending distance (most similar first); returns
     *     an empty list if no matches are found
     * @throws LocalLabException with {@code HttpStatus.NOT_FOUND} if the collection does not exist
     * @throws LocalLabException with {@code HttpStatus.SERVICE_UNAVAILABLE} if the Chroma service
     *     is unreachable or fails to respond
     */
    List<ChromaQueryResult> query(String collection, double[] embedding, int topK);
}
