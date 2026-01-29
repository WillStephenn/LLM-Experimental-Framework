package com.locallab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.locallab.client.ChromaClient;
import com.locallab.client.OllamaClient;
import com.locallab.dto.ChromaDocument;
import com.locallab.dto.ChromaQueryResult;
import com.locallab.dto.RetrievedChunk;
import com.locallab.dto.request.EmbeddingRequest;
import com.locallab.dto.response.EmbeddingResponse;
import com.locallab.model.Document;
import com.locallab.model.EmbeddingModel;
import com.locallab.repository.EmbeddingModelRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for RAG (Retrieval-Augmented Generation) operations.
 *
 * <p>Provides document chunking, embedding generation, vector storage, and semantic retrieval
 * functionality. This service orchestrates the RAG pipeline by coordinating between the document
 * service, Ollama for embeddings, and Chroma for vector storage.
 *
 * <h3>RAG Pipeline:</h3>
 *
 * <ol>
 *   <li><strong>Chunking:</strong> Split document content into overlapping chunks
 *   <li><strong>Embedding:</strong> Generate vector embeddings for each chunk via Ollama
 *   <li><strong>Storage:</strong> Store embeddings in Chroma vector store
 *   <li><strong>Query:</strong> Embed query text and retrieve similar chunks
 *   <li><strong>Context Assembly:</strong> Build context string from retrieved chunks
 * </ol>
 *
 * <h3>Collection Naming Convention:</h3>
 *
 * <p>Collections are named using the format {@code doc-{documentId}-{embeddingModel}} where the
 * embedding model name has colons replaced with hyphens for compatibility.
 *
 * <h3>Exception Handling:</h3>
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} - Thrown when a document or embedding model is not found
 *   <li>{@link IllegalArgumentException} - Thrown for invalid chunking parameters
 * </ul>
 *
 * @author William Stephen
 * @see DocumentService
 * @see com.locallab.client.OllamaClient
 * @see com.locallab.client.ChromaClient
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagService.class);

    private final DocumentService documentService;
    private final OllamaClient ollamaClient;
    private final ChromaClient chromaClient;
    private final EmbeddingModelRepository embeddingModelRepository;

    /**
     * Splits document content into overlapping chunks.
     *
     * <p>Creates chunks of the specified size with the given overlap between consecutive chunks.
     * This overlap helps maintain context continuity across chunk boundaries.
     *
     * @param content the document text to chunk
     * @param chunkSize the target size of each chunk in characters
     * @param overlap the number of overlapping characters between chunks
     * @return a list of text chunks
     * @throws IllegalArgumentException if overlap is greater than or equal to chunkSize, or if
     *     chunkSize is not positive
     */
    public List<String> chunkDocument(String content, int chunkSize, int overlap) {
        LOGGER.debug(
                "Chunking document with chunkSize: {}, overlap: {}, contentLength: {}",
                chunkSize,
                overlap,
                content != null ? content.length() : 0);

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }

        if (overlap < 0) {
            throw new IllegalArgumentException("Chunk overlap must not be negative");
        }

        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }

        if (content == null || content.isEmpty()) {
            LOGGER.debug("Empty or null content provided, returning empty chunk list");
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            start += chunkSize - overlap;
        }

        LOGGER.debug("Created {} chunks from document", chunks.size());
        return chunks;
    }

    /**
     * Chunks a document, generates embeddings, and stores them in Chroma.
     *
     * <p>This method performs the full embedding pipeline:
     *
     * <ol>
     *   <li>Retrieves the document by ID
     *   <li>Chunks the document content
     *   <li>Creates a Chroma collection if it doesn't exist
     *   <li>Generates embeddings for each chunk via Ollama
     *   <li>Stores the embeddings in Chroma
     *   <li>Updates the document's chunk count
     * </ol>
     *
     * @param documentId the document to process
     * @param embeddingModel the Ollama embedding model to use
     * @param chunkSize characters per chunk
     * @param overlap overlapping characters between chunks
     * @throws EntityNotFoundException if the document or embedding model is not found
     * @throws IllegalArgumentException if chunk parameters are invalid
     */
    @Transactional
    public void embedAndStore(Long documentId, String embeddingModel, int chunkSize, int overlap) {
        LOGGER.info(
                "Embedding and storing document: {} with model: {}, chunkSize: {}, overlap: {}",
                documentId,
                embeddingModel,
                chunkSize,
                overlap);

        Document document = documentService.findById(documentId);
        List<String> chunks = chunkDocument(document.getContent(), chunkSize, overlap);
        String collectionName = buildCollectionName(documentId, embeddingModel);

        ensureCollectionExists(collectionName, embeddingModel);

        List<ChromaDocument> chromaDocs =
                generateChunkEmbeddings(chunks, embeddingModel, documentId);
        chromaClient.addDocuments(collectionName, chromaDocs);

        document.setChunkCount(chunks.size());
        LOGGER.info(
                "Successfully embedded and stored {} chunks for document: {}",
                chunks.size(),
                documentId);
    }

    /**
     * Ensures a Chroma collection exists, creating it if necessary.
     *
     * @param collectionName the name of the collection to ensure exists
     * @param embeddingModel the embedding model name to look up dimensions
     * @throws EntityNotFoundException if the embedding model is not found
     */
    private void ensureCollectionExists(String collectionName, String embeddingModel) {
        EmbeddingModel model =
                embeddingModelRepository
                        .findByOllamaModelName(embeddingModel)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn("Embedding model not found: {}", embeddingModel);
                                    return new EntityNotFoundException(
                                            "Embedding model not found: " + embeddingModel);
                                });

        if (!chromaClient.collectionExists(collectionName)) {
            LOGGER.debug(
                    "Creating collection: {} with dimensions: {}",
                    collectionName,
                    model.getDimensions());
            chromaClient.createCollection(collectionName, model.getDimensions());
        }
    }

    /**
     * Generates embeddings for document chunks and creates Chroma documents.
     *
     * @param chunks the document chunks to embed
     * @param embeddingModel the Ollama embedding model to use
     * @param documentId the document identifier for metadata
     * @return a list of Chroma documents ready for storage
     */
    private List<ChromaDocument> generateChunkEmbeddings(
            List<String> chunks, String embeddingModel, Long documentId) {
        List<ChromaDocument> chromaDocs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            EmbeddingResponse embedding =
                    ollamaClient.embed(
                            EmbeddingRequest.builder()
                                    .model(embeddingModel)
                                    .input(chunks.get(i))
                                    .build());

            chromaDocs.add(
                    ChromaDocument.builder()
                            .id("chunk-" + i)
                            .content(chunks.get(i))
                            .embedding(convertToDoubleArray(embedding.getEmbedding()))
                            .metadata(
                                    Map.of(
                                            "documentId",
                                            documentId,
                                            "chunkIndex",
                                            i,
                                            "embeddingModel",
                                            embeddingModel))
                            .build());
        }
        return chromaDocs;
    }

    /**
     * Queries a collection for similar chunks.
     *
     * <p>Embeds the query text using the specified model and performs a similarity search in the
     * Chroma collection. Results are ordered by distance, with the most similar chunks first.
     *
     * @param collectionName the Chroma collection to query
     * @param query the query text
     * @param embeddingModel the model to embed the query
     * @param topK number of results to return
     * @return a list of retrieved chunks with relevance scores
     */
    public List<RetrievedChunk> query(
            String collectionName, String query, String embeddingModel, int topK) {
        LOGGER.debug(
                "Querying collection: {} with model: {}, topK: {}",
                collectionName,
                embeddingModel,
                topK);

        EmbeddingResponse queryEmbedding =
                ollamaClient.embed(
                        EmbeddingRequest.builder().model(embeddingModel).input(query).build());

        List<ChromaQueryResult> results =
                chromaClient.query(
                        collectionName, convertToDoubleArray(queryEmbedding.getEmbedding()), topK);

        List<RetrievedChunk> retrievedChunks =
                results.stream()
                        .map(
                                r ->
                                        RetrievedChunk.builder()
                                                .content(r.getContent())
                                                .distance(r.getDistance())
                                                .chunkIndex(extractChunkIndex(r.getMetadata()))
                                                .build())
                        .toList();

        LOGGER.debug(
                "Retrieved {} chunks from collection: {}", retrievedChunks.size(), collectionName);
        return retrievedChunks;
    }

    /**
     * Assembles retrieved chunks into context for an LLM prompt.
     *
     * <p>Formats the retrieved chunks into a numbered list that can be injected into a prompt
     * template. Each chunk is prefixed with its position number for reference.
     *
     * @param chunks the retrieved chunks
     * @return a formatted context string, or an empty string if no chunks are provided
     */
    public String buildContext(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            LOGGER.debug("No chunks provided, returning empty context");
            return "";
        }

        StringBuilder context = new StringBuilder("Context:\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            context.append("[")
                    .append(i + 1)
                    .append("] ")
                    .append(chunks.get(i).getContent())
                    .append("\n\n");
        }

        LOGGER.debug("Built context from {} chunks", chunks.size());
        return context.toString();
    }

    /**
     * Builds a collection name for a document and embedding model combination.
     *
     * <p>The collection name follows the convention {@code doc-{documentId}-{embeddingModel}} where
     * colons in the embedding model name are replaced with hyphens.
     *
     * @param documentId the document identifier
     * @param embeddingModel the embedding model name
     * @return the formatted collection name
     */
    public String buildCollectionName(Long documentId, String embeddingModel) {
        return "doc-" + documentId + "-" + embeddingModel.replace(":", "-");
    }

    /**
     * Converts a List of Double values to a primitive double array.
     *
     * @param embedding the embedding as a List of Doubles
     * @return the embedding as a primitive double array
     */
    private double[] convertToDoubleArray(List<Double> embedding) {
        if (embedding == null) {
            return new double[0];
        }
        return embedding.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * Extracts the chunk index from metadata.
     *
     * @param metadata the metadata map from a Chroma query result
     * @return the chunk index, or null if not present
     */
    private Integer extractChunkIndex(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey("chunkIndex")) {
            return null;
        }
        Object value = metadata.get("chunkIndex");
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
