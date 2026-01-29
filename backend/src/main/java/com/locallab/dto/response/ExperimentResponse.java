package com.locallab.dto.response;

import java.time.LocalDateTime;

import com.locallab.dto.ExperimentConfig;
import com.locallab.model.Experiment;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Experiment entities.
 *
 * <p>This class encapsulates all fields returned when retrieving experiments. It provides a clean
 * separation between the internal entity representation and the API response format.
 *
 * <p>The response includes a summary of the associated task template (if any), the experiment
 * configuration, and run statistics.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "name": "LLM Code Review Comparison",
 *   "taskTemplate": {
 *     "id": 1,
 *     "name": "Code Review Task"
 *   },
 *   "status": "DRAFT",
 *   "config": { ... },
 *   "totalRuns": 18,
 *   "completedRuns": 0,
 *   "createdAt": "2025-11-27T10:00:00"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see Experiment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentResponse {

    /** Unique identifier for the experiment. */
    private Long id;

    /** Name of the experiment. */
    private String name;

    /** Summary of the associated task template, or null if none. */
    private TaskTemplateSummary taskTemplate;

    /** Current status of the experiment. */
    private ExperimentStatus status;

    /** Configuration object for the experiment. */
    private ExperimentConfig config;

    /** Total number of runs calculated from the configuration. */
    private Integer totalRuns;

    /** Number of completed runs (both successful and failed). */
    private Integer completedRuns;

    /** Timestamp when this experiment was created. */
    private LocalDateTime createdAt;

    /**
     * Creates an ExperimentResponse from an Experiment entity.
     *
     * <p>This factory method provides a convenient way to convert entity objects to response DTOs.
     * It calculates the total runs from the configuration and counts completed runs from the runs
     * list.
     *
     * @param experiment the experiment entity to convert (must not be null)
     * @param parsedConfig the parsed experiment configuration (may be null)
     * @param totalRuns the calculated total number of runs
     * @return a new ExperimentResponse containing all entity data
     * @throws IllegalArgumentException if experiment is null
     */
    public static ExperimentResponse fromEntity(
            Experiment experiment, ExperimentConfig parsedConfig, int totalRuns) {
        if (experiment == null) {
            throw new IllegalArgumentException("Experiment must not be null");
        }

        TaskTemplateSummary taskTemplateSummary = null;
        if (experiment.getTaskTemplate() != null) {
            taskTemplateSummary =
                    TaskTemplateSummary.builder()
                            .id(experiment.getTaskTemplate().getId())
                            .name(experiment.getTaskTemplate().getName())
                            .build();
        }

        int completedRuns = 0;
        if (experiment.getRuns() != null) {
            completedRuns =
                    (int)
                            experiment.getRuns().stream()
                                    .filter(
                                            run ->
                                                    run.getStatus() == RunStatus.SUCCESS
                                                            || run.getStatus() == RunStatus.FAILED)
                                    .count();
        }

        return ExperimentResponse.builder()
                .id(experiment.getId())
                .name(experiment.getName())
                .taskTemplate(taskTemplateSummary)
                .status(experiment.getStatus())
                .config(parsedConfig)
                .totalRuns(totalRuns)
                .completedRuns(completedRuns)
                .createdAt(experiment.getCreatedAt())
                .build();
    }

    /**
     * Summary DTO for task template information.
     *
     * <p>Contains only the essential fields needed for displaying in experiment lists.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskTemplateSummary {

        /** Unique identifier for the task template. */
        private Long id;

        /** Name of the task template. */
        private String name;
    }
}
