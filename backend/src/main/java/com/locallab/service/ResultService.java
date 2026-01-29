package com.locallab.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.dto.response.ExperimentRunResponse;
import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.RunStatus;
import com.locallab.repository.ExperimentRepository;
import com.locallab.repository.ExperimentRunRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for managing experiment results and exports.
 *
 * <p>Provides operations for retrieving experiment runs and exporting results in various formats.
 * All operations are transactional with read-only optimisation.
 *
 * <h3>Supported Export Formats:</h3>
 *
 * <ul>
 *   <li><strong>CSV:</strong> Comma-separated values suitable for spreadsheet applications
 *   <li><strong>JSON:</strong> Full JSON representation of all runs
 * </ul>
 *
 * @author William Stephen
 * @see ExperimentRun
 * @see ExperimentRunRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultService.class);

    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentRepository experimentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves all runs for a specific experiment.
     *
     * <p>Results are ordered by iteration ascending.
     *
     * @param experimentId the ID of the experiment to retrieve runs for
     * @return a list of experiment runs, or an empty list if none found
     * @throws EntityNotFoundException if the experiment does not exist
     */
    public List<ExperimentRun> findRunsByExperimentId(Long experimentId) {
        LOGGER.debug("Retrieving runs for experiment: {}", experimentId);

        // Verify experiment exists
        verifyExperimentExists(experimentId);

        List<ExperimentRun> runs =
                experimentRunRepository.findByExperimentIdOrderByIterationAsc(experimentId);
        LOGGER.debug("Found {} runs for experiment: {}", runs.size(), experimentId);
        return runs;
    }

    /**
     * Retrieves all runs for a specific experiment filtered by status.
     *
     * @param experimentId the ID of the experiment to retrieve runs for
     * @param status the run status to filter by
     * @return a list of experiment runs matching the criteria
     * @throws EntityNotFoundException if the experiment does not exist
     */
    public List<ExperimentRun> findRunsByExperimentIdAndStatus(
            Long experimentId, RunStatus status) {
        LOGGER.debug("Retrieving runs for experiment: {} with status: {}", experimentId, status);

        // Verify experiment exists
        verifyExperimentExists(experimentId);

        List<ExperimentRun> runs =
                experimentRunRepository.findByExperimentIdAndStatus(experimentId, status);
        LOGGER.debug(
                "Found {} runs for experiment: {} with status: {}",
                runs.size(),
                experimentId,
                status);
        return runs;
    }

    /**
     * Retrieves all runs for a specific experiment filtered by model name.
     *
     * @param experimentId the ID of the experiment to retrieve runs for
     * @param modelName the model name to filter by
     * @return a list of experiment runs matching the criteria
     * @throws EntityNotFoundException if the experiment does not exist
     */
    public List<ExperimentRun> findRunsByExperimentIdAndModelName(
            Long experimentId, String modelName) {
        LOGGER.debug("Retrieving runs for experiment: {} with model: {}", experimentId, modelName);

        // Verify experiment exists
        verifyExperimentExists(experimentId);

        List<ExperimentRun> runs =
                experimentRunRepository.findByExperimentIdAndModelName(experimentId, modelName);
        LOGGER.debug(
                "Found {} runs for experiment: {} with model: {}",
                runs.size(),
                experimentId,
                modelName);
        return runs;
    }

    /**
     * Retrieves a single experiment run by its identifier.
     *
     * @param id the unique identifier of the run
     * @return the experiment run with the specified identifier
     * @throws EntityNotFoundException if no run exists with the given identifier
     */
    public ExperimentRun findRunById(Long id) {
        LOGGER.debug("Retrieving run with id: {}", id);
        return experimentRunRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("Experiment run not found with id: {}", id);
                            return new EntityNotFoundException("Experiment run not found: " + id);
                        });
    }

    /**
     * Exports experiment runs to CSV format.
     *
     * <p>The CSV includes headers and all relevant fields for each run. Fields containing commas or
     * newlines are properly escaped.
     *
     * @param experimentId the ID of the experiment to export
     * @return the CSV content as a string
     * @throws EntityNotFoundException if the experiment does not exist
     */
    public String exportToCsv(Long experimentId) {
        LOGGER.info("Exporting experiment {} to CSV", experimentId);

        List<ExperimentRun> runs = findRunsByExperimentId(experimentId);
        StringWriter writer = new StringWriter();

        // Write CSV header
        writer.append(getCsvHeader());

        // Write data rows
        for (ExperimentRun run : runs) {
            writer.append(formatRunAsCsvRow(run));
        }

        LOGGER.info("Exported {} runs to CSV for experiment {}", runs.size(), experimentId);
        return writer.toString();
    }

    /**
     * Exports experiment runs to JSON format.
     *
     * <p>Returns a JSON array containing ExperimentRunResponse objects for all runs.
     *
     * @param experimentId the ID of the experiment to export
     * @return the JSON content as a string
     * @throws EntityNotFoundException if the experiment does not exist
     * @throws IllegalStateException if JSON serialisation fails
     */
    public String exportToJson(Long experimentId) {
        LOGGER.info("Exporting experiment {} to JSON", experimentId);

        List<ExperimentRun> runs = findRunsByExperimentId(experimentId);
        List<ExperimentRunResponse> responses =
                runs.stream().map(ExperimentRunResponse::fromEntity).toList();

        try {
            String json =
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responses);
            LOGGER.info("Exported {} runs to JSON for experiment {}", runs.size(), experimentId);
            return json;
        } catch (IOException e) {
            LOGGER.error("Failed to serialise runs to JSON: {}", e.getMessage());
            throw new IllegalStateException("Failed to export runs to JSON", e);
        }
    }

    /**
     * Verifies that an experiment exists.
     *
     * @param experimentId the ID of the experiment to verify
     * @throws EntityNotFoundException if the experiment does not exist
     */
    private void verifyExperimentExists(Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            LOGGER.warn("Experiment not found with id: {}", experimentId);
            throw new EntityNotFoundException("Experiment not found: " + experimentId);
        }
    }

    /**
     * Returns the CSV header row.
     *
     * @return the CSV header string
     */
    private String getCsvHeader() {
        return "id,experimentId,modelName,embeddingModel,systemPrompt,iteration,"
                + "status,durationMs,tokensPerSecond,timeToFirstTokenMs,errorMessage,timestamp\n";
    }

    /**
     * Formats a single experiment run as a CSV row.
     *
     * @param run the experiment run to format
     * @return the CSV row string
     */
    private String formatRunAsCsvRow(ExperimentRun run) {
        StringBuilder row = new StringBuilder();
        row.append(run.getId()).append(",");
        row.append(run.getExperiment() != null ? run.getExperiment().getId() : "").append(",");
        row.append(escapeCsv(run.getModelName())).append(",");
        row.append(
                        escapeCsv(
                                run.getEmbeddingModel() != null
                                        ? run.getEmbeddingModel().getName()
                                        : ""))
                .append(",");
        row.append(escapeCsv(run.getSystemPrompt() != null ? run.getSystemPrompt().getAlias() : ""))
                .append(",");
        row.append(run.getIteration()).append(",");
        row.append(run.getStatus() != null ? run.getStatus().name() : "").append(",");
        row.append(run.getDurationMs() != null ? run.getDurationMs() : "").append(",");
        row.append(run.getTokensPerSecond() != null ? run.getTokensPerSecond() : "").append(",");
        row.append(run.getTimeToFirstTokenMs() != null ? run.getTimeToFirstTokenMs() : "")
                .append(",");
        row.append(escapeCsv(run.getErrorMessage())).append(",");
        row.append(run.getTimestamp() != null ? run.getTimestamp().toString() : "").append("\n");
        return row.toString();
    }

    /**
     * Escapes a string for CSV output.
     *
     * <p>Wraps the value in quotes if it contains commas, quotes, or newlines. Doubles any existing
     * quote characters.
     *
     * @param value the string to escape (may be null)
     * @return the escaped string, or empty string if null
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
