package com.locallab.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.locallab.dto.ExperimentConfig;
import com.locallab.dto.request.ExperimentRequest;
import com.locallab.dto.response.ExperimentResponse;
import com.locallab.model.Experiment;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.service.ExperimentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing Experiment resources.
 *
 * <p>This controller provides full CRUD operations for experiments, as well as execution control
 * endpoints for starting, pausing, resuming, and cancelling experiments. All endpoints follow the
 * API contract defined in the project specification.
 *
 * <h3>Endpoint Summary:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/experiments} - List all experiments with optional status filter
 *   <li>{@code GET /api/experiments/{id}} - Get a single experiment by ID
 *   <li>{@code POST /api/experiments} - Create a new experiment
 *   <li>{@code PUT /api/experiments/{id}} - Update an existing experiment (DRAFT only)
 *   <li>{@code DELETE /api/experiments/{id}} - Delete an experiment
 *   <li>{@code POST /api/experiments/{id}/start} - Begin experiment execution
 *   <li>{@code POST /api/experiments/{id}/pause} - Pause experiment execution
 *   <li>{@code POST /api/experiments/{id}/resume} - Resume paused experiment
 *   <li>{@code POST /api/experiments/{id}/cancel} - Cancel experiment execution
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract.
 *
 * @author William Stephen
 * @see ExperimentService
 * @see ExperimentRequest
 * @see ExperimentResponse
 */
@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
public class ExperimentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentController.class);

    private final ExperimentService experimentService;

    /**
     * Retrieves all experiments with optional status filtering.
     *
     * @param status optional filter by experiment status
     * @return a list of experiments matching the criteria, or all experiments if no filter provided
     */
    @GetMapping
    public ResponseEntity<List<ExperimentResponse>> getAllExperiments(
            @RequestParam(required = false) ExperimentStatus status) {

        LOGGER.debug("Received request to list experiments - status filter: {}", status);

        List<Experiment> experiments;

        if (status != null) {
            experiments = experimentService.findByStatus(status);
        } else {
            experiments = experimentService.findAll();
        }

        List<ExperimentResponse> response =
                experiments.stream().map(this::toExperimentResponse).toList();

        LOGGER.debug("Returning {} experiments", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single experiment by its identifier.
     *
     * @param id the unique identifier of the experiment
     * @return the experiment with the specified ID
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperimentResponse> getExperimentById(@PathVariable Long id) {

        LOGGER.debug("Received request to get experiment with id: {}", id);

        Experiment experiment = experimentService.findById(id);
        ExperimentResponse response = toExperimentResponse(experiment);

        LOGGER.debug("Returning experiment: {}", experiment.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new experiment.
     *
     * <p>The experiment is created with DRAFT status. The request body is validated against the
     * constraints defined in {@link ExperimentRequest}.
     *
     * @param request the experiment creation request containing all required fields
     * @return the newly created experiment with a 201 Created status
     */
    @PostMapping
    public ResponseEntity<ExperimentResponse> createExperiment(
            @Valid @RequestBody ExperimentRequest request) {

        LOGGER.info("Received request to create experiment: {}", request.getName());

        Experiment createdExperiment = experimentService.create(request);
        ExperimentResponse response = toExperimentResponse(createdExperiment);

        LOGGER.info("Created experiment with id: {}", createdExperiment.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing experiment.
     *
     * <p>Only experiments in DRAFT status can be updated. A 409 Conflict response is returned if
     * attempting to update a non-draft experiment.
     *
     * @param id the identifier of the experiment to update
     * @param request the experiment update request containing updated fields
     * @return the updated experiment
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     * @throws IllegalStateException if the experiment is not in DRAFT status
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExperimentResponse> updateExperiment(
            @PathVariable Long id, @Valid @RequestBody ExperimentRequest request) {

        LOGGER.info("Received request to update experiment with id: {}", id);

        Experiment updatedExperiment = experimentService.update(id, request);
        ExperimentResponse response = toExperimentResponse(updatedExperiment);

        LOGGER.info("Updated experiment: {}", updatedExperiment.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an experiment by its identifier.
     *
     * <p>Running experiments cannot be deleted. Deleting an experiment cascades to all associated
     * runs.
     *
     * @param id the identifier of the experiment to delete
     * @return a 204 No Content response on successful deletion
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     * @throws IllegalStateException if the experiment is currently running
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable Long id) {

        LOGGER.info("Received request to delete experiment with id: {}", id);

        experimentService.delete(id);

        LOGGER.info("Deleted experiment with id: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Starts experiment execution.
     *
     * <p>Changes experiment status from DRAFT to RUNNING. Only experiments in DRAFT status can be
     * started.
     *
     * @param id the identifier of the experiment to start
     * @return the updated experiment with RUNNING status
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     * @throws IllegalStateException if the experiment is not in DRAFT status
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ExperimentResponse> startExperiment(@PathVariable Long id) {

        LOGGER.info("Received request to start experiment with id: {}", id);

        Experiment experiment = experimentService.findById(id);

        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            LOGGER.warn(
                    "Cannot start experiment {} - invalid status: {}", id, experiment.getStatus());
            throw new IllegalStateException(
                    "Cannot start experiment with status: " + experiment.getStatus());
        }

        Experiment updatedExperiment = experimentService.updateStatus(id, ExperimentStatus.RUNNING);
        ExperimentResponse response = toExperimentResponse(updatedExperiment);

        LOGGER.info("Started experiment with id: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Pauses experiment execution.
     *
     * <p>Changes experiment status from RUNNING to PAUSED. Only experiments in RUNNING status can
     * be paused.
     *
     * @param id the identifier of the experiment to pause
     * @return the updated experiment with PAUSED status
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     * @throws IllegalStateException if the experiment is not in RUNNING status
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<ExperimentResponse> pauseExperiment(@PathVariable Long id) {

        LOGGER.info("Received request to pause experiment with id: {}", id);

        Experiment experiment = experimentService.findById(id);

        if (experiment.getStatus() != ExperimentStatus.RUNNING) {
            LOGGER.warn(
                    "Cannot pause experiment {} - invalid status: {}", id, experiment.getStatus());
            throw new IllegalStateException(
                    "Cannot pause experiment with status: " + experiment.getStatus());
        }

        Experiment updatedExperiment = experimentService.updateStatus(id, ExperimentStatus.PAUSED);
        ExperimentResponse response = toExperimentResponse(updatedExperiment);

        LOGGER.info("Paused experiment with id: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Resumes a paused experiment.
     *
     * <p>Changes experiment status from PAUSED to RUNNING. Only experiments in PAUSED status can be
     * resumed.
     *
     * @param id the identifier of the experiment to resume
     * @return the updated experiment with RUNNING status
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     * @throws IllegalStateException if the experiment is not in PAUSED status
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<ExperimentResponse> resumeExperiment(@PathVariable Long id) {

        LOGGER.info("Received request to resume experiment with id: {}", id);

        Experiment experiment = experimentService.findById(id);

        if (experiment.getStatus() != ExperimentStatus.PAUSED) {
            LOGGER.warn(
                    "Cannot resume experiment {} - invalid status: {}", id, experiment.getStatus());
            throw new IllegalStateException(
                    "Cannot resume experiment with status: " + experiment.getStatus());
        }

        Experiment updatedExperiment = experimentService.updateStatus(id, ExperimentStatus.RUNNING);
        ExperimentResponse response = toExperimentResponse(updatedExperiment);

        LOGGER.info("Resumed experiment with id: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels experiment execution.
     *
     * <p>Changes experiment status to FAILED. Only experiments in RUNNING or PAUSED status can be
     * cancelled.
     *
     * @param id the identifier of the experiment to cancel
     * @return the updated experiment with FAILED status
     * @throws jakarta.persistence.EntityNotFoundException if no experiment exists with the given ID
     * @throws IllegalStateException if the experiment is not in RUNNING or PAUSED status
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ExperimentResponse> cancelExperiment(@PathVariable Long id) {

        LOGGER.info("Received request to cancel experiment with id: {}", id);

        Experiment experiment = experimentService.findById(id);

        if (experiment.getStatus() != ExperimentStatus.RUNNING
                && experiment.getStatus() != ExperimentStatus.PAUSED) {
            LOGGER.warn(
                    "Cannot cancel experiment {} - invalid status: {}", id, experiment.getStatus());
            throw new IllegalStateException(
                    "Cannot cancel experiment with status: " + experiment.getStatus());
        }

        Experiment updatedExperiment = experimentService.updateStatus(id, ExperimentStatus.FAILED);
        ExperimentResponse response = toExperimentResponse(updatedExperiment);

        LOGGER.info("Cancelled experiment with id: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Converts an Experiment entity to an ExperimentResponse DTO.
     *
     * @param experiment the experiment entity to convert
     * @return the corresponding ExperimentResponse
     */
    private ExperimentResponse toExperimentResponse(Experiment experiment) {
        ExperimentConfig config = experimentService.parseConfig(experiment.getConfig());
        int totalRuns = experimentService.calculateTotalRuns(config);
        return ExperimentResponse.fromEntity(experiment, config, totalRuns);
    }
}
