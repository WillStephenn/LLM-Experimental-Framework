package com.locallab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the progress of an experiment execution.
 *
 * <p>This class provides real-time progress information for an executing experiment, including the
 * number of completed runs, total runs, and calculated percentage completion.
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>{@code
 * ExperimentProgress progress = ExperimentProgress.builder()
 *     .experimentId(1L)
 *     .completedRuns(5)
 *     .totalRuns(12)
 *     .progressPercent(41.67)
 *     .build();
 * }</pre>
 *
 * <h3>WebSocket Integration:</h3>
 *
 * <p>This DTO is serialised and sent to clients via WebSocket messages during experiment execution.
 * Clients can subscribe to {@code /topic/experiments/{id}/progress} to receive these updates.
 *
 * @author William Stephen
 * @see com.locallab.dto.WebSocketMessage
 * @see com.locallab.service.ExperimentExecutorService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentProgress {

    /**
     * The unique identifier of the experiment.
     *
     * <p>Used by clients to associate progress updates with the correct experiment.
     */
    private Long experimentId;

    /**
     * The number of runs that have completed.
     *
     * <p>This includes both successful and failed runs. Incremented after each run finishes
     * regardless of outcome.
     */
    private int completedRuns;

    /**
     * The total number of runs to be executed.
     *
     * <p>Calculated from the experiment configuration: models x embeddings x iterations.
     */
    private int totalRuns;

    /**
     * The percentage of the experiment that has completed.
     *
     * <p>Calculated as: (completedRuns / totalRuns) * 100. Range: 0.0 to 100.0.
     */
    private double progressPercent;
}
