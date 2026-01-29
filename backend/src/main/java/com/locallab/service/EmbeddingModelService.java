package com.locallab.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.locallab.dto.request.EmbeddingModelRequest;
import com.locallab.model.EmbeddingModel;
import com.locallab.repository.EmbeddingModelRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for managing EmbeddingModel entities.
 *
 * <p>Provides CRUD operations for embedding model configurations. All operations are transactional
 * with read-only optimisation for query methods.
 *
 * <h3>Exception Handling:</h3>
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} - Thrown when a requested embedding model is not found
 *   <li>{@link IllegalStateException} - Thrown when creating an embedding model with a duplicate
 *       name
 * </ul>
 *
 * @author William Stephen
 * @see EmbeddingModel
 * @see EmbeddingModelRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmbeddingModelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingModelService.class);

    private final EmbeddingModelRepository embeddingModelRepository;

    /**
     * Retrieves all embedding models.
     *
     * @return a list of all embedding models, or an empty list if none exist
     */
    public List<EmbeddingModel> findAll() {
        LOGGER.debug("Retrieving all embedding models");
        List<EmbeddingModel> models = embeddingModelRepository.findAll();
        LOGGER.debug("Found {} embedding models", models.size());
        return models;
    }

    /**
     * Retrieves an embedding model by its identifier.
     *
     * @param id the unique identifier of the embedding model
     * @return the embedding model with the specified identifier
     * @throws EntityNotFoundException if no embedding model exists with the given identifier
     */
    public EmbeddingModel findById(Long id) {
        LOGGER.debug("Retrieving embedding model with id: {}", id);
        return embeddingModelRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("Embedding model not found with id: {}", id);
                            return new EntityNotFoundException("Embedding model not found: " + id);
                        });
    }

    /**
     * Creates a new embedding model configuration.
     *
     * <p>Validates that the name is unique before creating the model.
     *
     * @param request the embedding model creation request
     * @return the created embedding model entity with generated identifier
     * @throws IllegalStateException if an embedding model with the same name already exists
     */
    @Transactional
    public EmbeddingModel create(EmbeddingModelRequest request) {
        LOGGER.info("Creating embedding model: {}", request.getName());

        if (embeddingModelRepository.findByName(request.getName()).isPresent()) {
            LOGGER.warn("Embedding model name already exists: {}", request.getName());
            throw new IllegalStateException("Embedding model already exists: " + request.getName());
        }

        EmbeddingModel model =
                EmbeddingModel.builder()
                        .name(request.getName())
                        .ollamaModelName(request.getOllamaModelName())
                        .dimensions(request.getDimensions())
                        .build();

        EmbeddingModel savedModel = embeddingModelRepository.save(model);
        LOGGER.info(
                "Created embedding model with id: {}, name: {}",
                savedModel.getId(),
                savedModel.getName());
        return savedModel;
    }

    /**
     * Deletes an embedding model by its identifier.
     *
     * @param id the identifier of the embedding model to delete
     * @throws EntityNotFoundException if no embedding model exists with the given identifier
     */
    @Transactional
    public void delete(Long id) {
        LOGGER.info("Deleting embedding model with id: {}", id);

        if (!embeddingModelRepository.existsById(id)) {
            LOGGER.warn("Cannot delete embedding model - not found with id: {}", id);
            throw new EntityNotFoundException("Embedding model not found: " + id);
        }

        embeddingModelRepository.deleteById(id);
        LOGGER.info("Deleted embedding model with id: {}", id);
    }
}
