package com.locallab.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.locallab.dto.RetrievedChunk;
import com.locallab.dto.request.EmbeddingModelRequest;
import com.locallab.dto.request.RagQueryRequest;
import com.locallab.dto.response.DocumentResponse;
import com.locallab.dto.response.EmbeddingModelResponse;
import com.locallab.dto.response.RagQueryResponse;
import com.locallab.model.Document;
import com.locallab.model.EmbeddingModel;
import com.locallab.service.DocumentService;
import com.locallab.service.EmbeddingModelService;
import com.locallab.service.RagService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing Document resources and RAG operations.
 *
 * <p>This controller provides document upload, retrieval, deletion, and RAG query operations, as
 * well as embedding model management. All endpoints return consistent response structures and
 * appropriate HTTP status codes as defined in the API contract.
 *
 * <h3>Document Endpoints:</h3>
 *
 * <ul>
 *   <li>{@code POST /api/documents} - Upload a document (multipart/form-data)
 *   <li>{@code GET /api/documents} - List all documents
 *   <li>{@code GET /api/documents/{id}} - Get a single document by ID
 *   <li>{@code DELETE /api/documents/{id}} - Delete a document
 *   <li>{@code POST /api/documents/{id}/query} - Query a document with RAG
 * </ul>
 *
 * <h3>Embedding Model Endpoints:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/embedding-models} - List all embedding models
 *   <li>{@code POST /api/embedding-models} - Create a new embedding model
 *   <li>{@code DELETE /api/embedding-models/{id}} - Delete an embedding model
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract.
 *
 * @author William Stephen
 * @see DocumentService
 * @see RagService
 * @see EmbeddingModelService
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final RagService ragService;
    private final EmbeddingModelService embeddingModelService;

    /**
     * Uploads a document for RAG processing.
     *
     * <p>Accepts PDF or TXT files up to 10MB. The document content is extracted and stored in the
     * database. A 400 Bad Request response is returned if the file type is unsupported.
     *
     * @param file the uploaded file (PDF or TXT)
     * @return the created document with a 201 Created status
     * @throws IllegalArgumentException if the file type is unsupported or filename is missing
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        LOGGER.info("Received request to upload document: {}", file.getOriginalFilename());

        Document uploadedDocument = documentService.upload(file);
        DocumentResponse response = DocumentResponse.fromEntity(uploadedDocument);

        LOGGER.info("Uploaded document with id: {}", uploadedDocument.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all documents.
     *
     * @return a list of all documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {

        LOGGER.debug("Received request to list all documents");

        List<Document> documents = documentService.findAll();
        List<DocumentResponse> response =
                documents.stream().map(DocumentResponse::fromEntity).toList();

        LOGGER.debug("Returning {} documents", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single document by its identifier.
     *
     * @param id the unique identifier of the document
     * @return the document with the specified ID
     * @throws jakarta.persistence.EntityNotFoundException if no document exists with the given ID
     */
    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {

        LOGGER.debug("Received request to get document with id: {}", id);

        Document document = documentService.findById(id);
        DocumentResponse response = DocumentResponse.fromEntity(document);

        LOGGER.debug("Returning document: {}", document.getFilename());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a document by its identifier.
     *
     * @param id the identifier of the document to delete
     * @return a 204 No Content response on successful deletion
     * @throws jakarta.persistence.EntityNotFoundException if no document exists with the given ID
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {

        LOGGER.info("Received request to delete document with id: {}", id);

        documentService.delete(id);

        LOGGER.info("Deleted document with id: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Queries a document using RAG (Retrieval-Augmented Generation).
     *
     * <p>This endpoint embeds the query, chunks the document if needed, performs a similarity
     * search, and returns the most relevant chunks along with an assembled context string.
     *
     * @param id the identifier of the document to query
     * @param request the RAG query request containing query text and parameters
     * @return the RAG query response with retrieved chunks and assembled context
     * @throws jakarta.persistence.EntityNotFoundException if the document or embedding model is not
     *     found
     */
    @PostMapping("/documents/{id}/query")
    public ResponseEntity<RagQueryResponse> queryDocument(
            @PathVariable Long id, @Valid @RequestBody RagQueryRequest request) {

        LOGGER.info(
                "Received RAG query for document id: {} with model: {}",
                id,
                request.getEmbeddingModel());

        // Ensure the document exists
        Document document = documentService.findById(id);

        // Embed and store if not already done for this document/model combination
        ragService.embedAndStore(
                id, request.getEmbeddingModel(), request.getChunkSize(), request.getChunkOverlap());

        // Build collection name and query
        String collectionName = ragService.buildCollectionName(id, request.getEmbeddingModel());

        List<RetrievedChunk> chunks =
                ragService.query(
                        collectionName,
                        request.getQuery(),
                        request.getEmbeddingModel(),
                        request.getTopK());

        String assembledContext = ragService.buildContext(chunks);

        RagQueryResponse response =
                RagQueryResponse.builder()
                        .query(request.getQuery())
                        .retrievedChunks(chunks)
                        .assembledContext(assembledContext)
                        .embeddingModel(request.getEmbeddingModel())
                        .build();

        LOGGER.info(
                "RAG query completed for document: {}, retrieved {} chunks",
                document.getFilename(),
                chunks.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all embedding models.
     *
     * @return a list of all embedding models
     */
    @GetMapping("/embedding-models")
    public ResponseEntity<List<EmbeddingModelResponse>> getAllEmbeddingModels() {

        LOGGER.debug("Received request to list all embedding models");

        List<EmbeddingModel> models = embeddingModelService.findAll();
        List<EmbeddingModelResponse> response =
                models.stream().map(EmbeddingModelResponse::fromEntity).toList();

        LOGGER.debug("Returning {} embedding models", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new embedding model configuration.
     *
     * <p>A 400 Bad Request response is returned if validation fails. A 409 Conflict response is
     * returned if an embedding model with the same name already exists.
     *
     * @param request the embedding model creation request
     * @return the created embedding model with a 201 Created status
     * @throws IllegalStateException if an embedding model with the same name already exists
     */
    @PostMapping("/embedding-models")
    public ResponseEntity<EmbeddingModelResponse> createEmbeddingModel(
            @Valid @RequestBody EmbeddingModelRequest request) {

        LOGGER.info("Received request to create embedding model: {}", request.getName());

        EmbeddingModel createdModel = embeddingModelService.create(request);
        EmbeddingModelResponse response = EmbeddingModelResponse.fromEntity(createdModel);

        LOGGER.info("Created embedding model with id: {}", createdModel.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes an embedding model by its identifier.
     *
     * @param id the identifier of the embedding model to delete
     * @return a 204 No Content response on successful deletion
     * @throws jakarta.persistence.EntityNotFoundException if no embedding model exists with the
     *     given ID
     */
    @DeleteMapping("/embedding-models/{id}")
    public ResponseEntity<Void> deleteEmbeddingModel(@PathVariable Long id) {

        LOGGER.info("Received request to delete embedding model with id: {}", id);

        embeddingModelService.delete(id);

        LOGGER.info("Deleted embedding model with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
