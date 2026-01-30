package com.locallab.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.locallab.dto.response.ExperimentRunResponse;
import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.RunStatus;
import com.locallab.service.ResultService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing experiment results.
 *
 * <p>This controller provides endpoints for retrieving experiment runs and exporting results in
 * various formats. All endpoints return consistent response structures and appropriate HTTP status
 * codes.
 *
 * <h3>Endpoint Summary:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/experiments/{id}/runs} - List all runs for an experiment
 *   <li>{@code GET /api/runs/{id}} - Get a single run by ID
 *   <li>{@code GET /api/experiments/{id}/export} - Export experiment results
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract.
 *
 * @author William Stephen
 * @see ResultService
 * @see ExperimentRunResponse
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResultController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultController.class);

    private final ResultService resultService;

    /**
     * Retrieves all runs for a specific experiment with optional filtering.
     *
     * <p>Results are ordered by iteration ascending. Optional filters can be applied using query
     * parameters.
     *
     * @param experimentId the ID of the experiment to retrieve runs for
     * @param status optional filter by run status (PENDING, RUNNING, SUCCESS, FAILED)
     * @param modelName optional filter by model name
     * @return a list of experiment runs matching the criteria
     * @throws jakarta.persistence.EntityNotFoundException if the experiment does not exist
     */
    @GetMapping("/experiments/{id}/runs")
    public ResponseEntity<List<ExperimentRunResponse>> getExperimentRuns(
            @PathVariable("id") Long experimentId,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) String modelName) {

        LOGGER.debug(
                "Received request to list runs for experiment: {} - status: {}, modelName: {}",
                experimentId,
                status,
                modelName);

        List<ExperimentRun> runs;

        if (status != null) {
            runs = resultService.findRunsByExperimentIdAndStatus(experimentId, status);
        } else if (modelName != null && !modelName.trim().isEmpty()) {
            runs = resultService.findRunsByExperimentIdAndModelName(experimentId, modelName);
        } else {
            runs = resultService.findRunsByExperimentId(experimentId);
        }

        List<ExperimentRunResponse> response =
                runs.stream().map(ExperimentRunResponse::fromEntity).toList();

        LOGGER.debug("Returning {} runs for experiment: {}", response.size(), experimentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single experiment run by its identifier.
     *
     * @param id the unique identifier of the run
     * @return the experiment run with the specified ID
     * @throws jakarta.persistence.EntityNotFoundException if no run exists with the given ID
     */
    @GetMapping("/runs/{id}")
    public ResponseEntity<ExperimentRunResponse> getRunById(@PathVariable Long id) {

        LOGGER.debug("Received request to get run with id: {}", id);

        ExperimentRun run = resultService.findRunById(id);
        ExperimentRunResponse response = ExperimentRunResponse.fromEntity(run);

        Long experimentId = run.getExperiment() != null ? run.getExperiment().getId() : null;
        LOGGER.debug("Returning run: {} for experiment: {}", run.getId(), experimentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Exports experiment results in the specified format.
     *
     * <p>Supported formats:
     *
     * <ul>
     *   <li>{@code csv} - Returns CSV file with Content-Disposition attachment header
     *   <li>{@code json} - Returns JSON array of ExperimentRunResponse objects
     * </ul>
     *
     * @param experimentId the ID of the experiment to export
     * @param format the export format (must be "csv" or "json")
     * @return the exported data with appropriate content type
     * @throws jakarta.persistence.EntityNotFoundException if the experiment does not exist
     * @throws IllegalArgumentException if the format is not supported
     */
    @GetMapping("/experiments/{id}/export")
    public ResponseEntity<String> exportExperiment(
            @PathVariable("id") Long experimentId, @RequestParam String format) {

        LOGGER.info("Received request to export experiment: {} as {}", experimentId, format);

        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Export format is required");
        }

        String normalisedFormat = format.trim().toLowerCase();

        return switch (normalisedFormat) {
            case "csv" -> {
                String csv = resultService.exportToCsv(experimentId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv"));
                headers.setContentDispositionFormData(
                        "attachment", "experiment-" + experimentId + "-results.csv");
                LOGGER.info("Exported experiment {} to CSV", experimentId);
                yield ResponseEntity.ok().headers(headers).body(csv);
            }
            case "json" -> {
                String json = resultService.exportToJson(experimentId);
                LOGGER.info("Exported experiment {} to JSON", experimentId);
                yield ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported export format: " + format + ". Use 'csv' or 'json'");
        };
    }
}
