package com.locallab.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.dto.ExperimentConfig;
import com.locallab.dto.request.ExperimentRequest;
import com.locallab.model.Experiment;
import com.locallab.model.TaskTemplate;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.repository.ExperimentRepository;
import com.locallab.repository.TaskTemplateRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for managing Experiment entities.
 *
 * <p>Provides CRUD operations, configuration validation, status management, and run matrix
 * calculation for experiments. All operations are transactional with read-only optimisation for
 * query methods.
 *
 * <h3>Status Management:</h3>
 *
 * <p>Experiments follow a strict lifecycle:
 *
 * <ul>
 *   <li>DRAFT - Initial state, can be updated or deleted
 *   <li>RUNNING - Experiment in progress, cannot be updated or deleted
 *   <li>PAUSED - Experiment paused, awaiting resume
 *   <li>COMPLETED - All runs finished successfully
 *   <li>FAILED - Experiment terminated due to error or cancellation
 * </ul>
 *
 * <p>Only experiments in DRAFT status can be updated. Running experiments cannot be deleted.
 *
 * <h3>Run Matrix Calculation:</h3>
 *
 * <p>The total number of runs is calculated as: {@code models × max(embeddingModels, 1) ×
 * iterations}
 *
 * <h3>Exception Handling:</h3>
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} - Thrown when a requested experiment or task template is
 *       not found
 *   <li>{@link IllegalStateException} - Thrown when attempting invalid status transitions
 *   <li>{@link IllegalArgumentException} - Thrown for configuration validation failures
 * </ul>
 *
 * @author William Stephen
 * @see Experiment
 * @see ExperimentRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExperimentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentService.class);

    private final ExperimentRepository experimentRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves all experiments ordered by creation date descending.
     *
     * @return a list of all experiments with newest first, or an empty list if none exist
     */
    public List<Experiment> findAll() {
        LOGGER.debug("Retrieving all experiments");
        List<Experiment> experiments = experimentRepository.findAllByOrderByCreatedAtDesc();
        LOGGER.debug("Found {} experiments", experiments.size());
        return experiments;
    }

    /**
     * Retrieves all experiments with the specified status.
     *
     * @param status the experiment status to filter by
     * @return a list of experiments with the specified status, ordered by creation date descending,
     *     or an empty list if none found
     */
    public List<Experiment> findByStatus(ExperimentStatus status) {
        LOGGER.debug("Retrieving experiments with status: {}", status);
        List<Experiment> experiments =
                experimentRepository.findByStatusOrderByCreatedAtDesc(status);
        LOGGER.debug("Found {} experiments with status: {}", experiments.size(), status);
        return experiments;
    }

    /**
     * Retrieves an experiment by its identifier.
     *
     * @param id the unique identifier of the experiment
     * @return the experiment with the specified identifier
     * @throws EntityNotFoundException if no experiment exists with the given identifier
     */
    public Experiment findById(Long id) {
        LOGGER.debug("Retrieving experiment with id: {}", id);
        return experimentRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("Experiment not found with id: {}", id);
                            return new EntityNotFoundException("Experiment not found: " + id);
                        });
    }

    /**
     * Creates a new experiment from the provided request.
     *
     * <p>The experiment is created with DRAFT status. If a task template ID is provided, the
     * template is validated to exist before associating it with the experiment.
     *
     * @param request the experiment creation request containing all required fields
     * @return the newly created experiment with generated identifier
     * @throws EntityNotFoundException if the specified task template does not exist
     * @throws IllegalArgumentException if the configuration is invalid
     */
    @Transactional
    public Experiment create(ExperimentRequest request) {
        LOGGER.info("Creating new experiment with name: {}", request.getName());

        // Validate configuration
        validateConfig(request.getConfig());

        // Look up task template if provided
        TaskTemplate taskTemplate = null;
        if (request.getTaskTemplateId() != null) {
            taskTemplate = findTaskTemplate(request.getTaskTemplateId());
        }

        // Serialise config to JSON
        String configJson = serialiseConfig(request.getConfig());

        Experiment experiment =
                Experiment.builder()
                        .name(request.getName())
                        .taskTemplate(taskTemplate)
                        .status(ExperimentStatus.DRAFT)
                        .config(configJson)
                        .build();

        Experiment savedExperiment = experimentRepository.save(experiment);
        LOGGER.info("Created experiment with id: {}", savedExperiment.getId());
        return savedExperiment;
    }

    /**
     * Updates an existing experiment with the provided request data.
     *
     * <p>Only experiments in DRAFT status can be updated. Attempting to update an experiment in any
     * other status will result in an IllegalStateException.
     *
     * @param id the identifier of the experiment to update
     * @param request the experiment update request containing updated fields
     * @return the updated experiment
     * @throws EntityNotFoundException if no experiment exists with the given identifier
     * @throws IllegalStateException if the experiment is not in DRAFT status
     * @throws IllegalArgumentException if the configuration is invalid
     */
    @Transactional
    public Experiment update(Long id, ExperimentRequest request) {
        LOGGER.info("Updating experiment with id: {}", id);

        Experiment existingExperiment = findById(id);

        // Only DRAFT experiments can be updated
        if (existingExperiment.getStatus() != ExperimentStatus.DRAFT) {
            LOGGER.warn(
                    "Cannot update experiment {} with status: {}",
                    id,
                    existingExperiment.getStatus());
            throw new IllegalStateException(
                    "Cannot update experiment with status: " + existingExperiment.getStatus());
        }

        // Validate configuration
        validateConfig(request.getConfig());

        // Look up task template if provided
        TaskTemplate taskTemplate = null;
        if (request.getTaskTemplateId() != null) {
            taskTemplate = findTaskTemplate(request.getTaskTemplateId());
        }

        // Serialise config to JSON
        String configJson = serialiseConfig(request.getConfig());

        existingExperiment.setName(request.getName());
        existingExperiment.setTaskTemplate(taskTemplate);
        existingExperiment.setConfig(configJson);

        Experiment updatedExperiment = experimentRepository.save(existingExperiment);
        LOGGER.info("Updated experiment with id: {}", updatedExperiment.getId());
        return updatedExperiment;
    }

    /**
     * Deletes an experiment by its identifier.
     *
     * <p>Running experiments cannot be deleted. Deleting an experiment will cascade delete all
     * associated experiment runs.
     *
     * @param id the identifier of the experiment to delete
     * @throws EntityNotFoundException if no experiment exists with the given identifier
     * @throws IllegalStateException if the experiment is currently running
     */
    @Transactional
    public void delete(Long id) {
        LOGGER.info("Deleting experiment with id: {}", id);

        Experiment experiment = findById(id);

        // Cannot delete running experiments
        if (experiment.getStatus() == ExperimentStatus.RUNNING) {
            LOGGER.warn("Cannot delete running experiment with id: {}", id);
            throw new IllegalStateException("Cannot delete a running experiment");
        }

        experimentRepository.delete(experiment);
        LOGGER.info("Deleted experiment with id: {}", id);
    }

    /**
     * Updates the status of an experiment.
     *
     * <p>This method is primarily used by the ExperimentExecutorService to update experiment status
     * during execution. No validation of status transitions is performed here; the executor is
     * responsible for ensuring valid transitions.
     *
     * @param id the identifier of the experiment to update
     * @param status the new status to set
     * @throws EntityNotFoundException if no experiment exists with the given identifier
     */
    @Transactional
    public void updateStatus(Long id, ExperimentStatus status) {
        LOGGER.info("Updating experiment {} status to: {}", id, status);

        Experiment experiment = findById(id);
        experiment.setStatus(status);
        experimentRepository.save(experiment);

        LOGGER.info("Updated experiment {} status to: {}", id, status);
    }

    /**
     * Calculates the total number of runs for an experiment configuration.
     *
     * <p>The formula is: {@code models × max(embeddingModels, 1) × iterations}
     *
     * <p>If no embedding models are specified (non-RAG mode), the embedding count is treated as 1.
     *
     * @param config the experiment configuration
     * @return the total number of runs that will be executed
     */
    public int calculateTotalRuns(ExperimentConfig config) {
        int modelCount = config.getModels() != null ? config.getModels().size() : 0;
        int embeddingCount =
                config.getEmbeddingModels() != null && !config.getEmbeddingModels().isEmpty()
                        ? config.getEmbeddingModels().size()
                        : 1;
        int iterations = config.getIterations() != null ? config.getIterations() : 1;

        int totalRuns = modelCount * embeddingCount * iterations;
        LOGGER.debug(
                "Calculated total runs: {} (models={}, embeddings={}, iterations={})",
                totalRuns,
                modelCount,
                embeddingCount,
                iterations);
        return totalRuns;
    }

    /**
     * Parses the JSON configuration string into an ExperimentConfig object.
     *
     * @param configJson the JSON string to parse
     * @return the parsed ExperimentConfig object
     * @throws IllegalArgumentException if the JSON cannot be parsed
     */
    public ExperimentConfig parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            LOGGER.debug("Config JSON is null or empty");
            return null;
        }

        try {
            return objectMapper.readValue(configJson, ExperimentConfig.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse experiment config JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid experiment configuration JSON", e);
        }
    }

    /**
     * Validates the experiment configuration.
     *
     * <p>Checks that:
     *
     * <ul>
     *   <li>At least one model is specified
     *   <li>Iterations is at least 1
     *   <li>If context mode is RAG, embedding models and document ID are provided
     * </ul>
     *
     * @param config the configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateConfig(ExperimentConfig config) {
        if (config.getModels() == null || config.getModels().isEmpty()) {
            throw new IllegalArgumentException("At least one model must be selected");
        }

        if (config.getIterations() != null && config.getIterations() < 1) {
            throw new IllegalArgumentException("Iterations must be at least 1");
        }

        // Validate RAG-specific requirements
        if ("RAG".equalsIgnoreCase(config.getContextMode())) {
            if (config.getEmbeddingModels() == null || config.getEmbeddingModels().isEmpty()) {
                throw new IllegalArgumentException(
                        "Embedding models must be specified for RAG context mode");
            }
            if (config.getDocumentId() == null) {
                throw new IllegalArgumentException(
                        "Document ID must be specified for RAG context mode");
            }
        }

        LOGGER.debug("Configuration validation passed");
    }

    /**
     * Finds a task template by ID.
     *
     * @param taskTemplateId the ID of the task template to find
     * @return the task template
     * @throws EntityNotFoundException if the task template does not exist
     */
    private TaskTemplate findTaskTemplate(Long taskTemplateId) {
        return taskTemplateRepository
                .findById(taskTemplateId)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("Task template not found with id: {}", taskTemplateId);
                            return new EntityNotFoundException(
                                    "Task template not found: " + taskTemplateId);
                        });
    }

    /**
     * Serialises an ExperimentConfig to JSON.
     *
     * @param config the configuration to serialise
     * @return the JSON string representation
     * @throws IllegalArgumentException if serialisation fails
     */
    private String serialiseConfig(ExperimentConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialise experiment config: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to serialise experiment configuration", e);
        }
    }
}
