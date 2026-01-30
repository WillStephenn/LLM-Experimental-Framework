package com.locallab.dto.websocket;

import com.locallab.model.enums.ExperimentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for EXPERIMENT_COMPLETED WebSocket messages.
 *
 * <p>Provides summary information when all runs in an experiment have finished. This payload
 * conforms to the API contract specification for the EXPERIMENT_COMPLETED message type.
 *
 * <h3>Example JSON:</h3>
 *
 * <pre>{@code
 * {
 *   "finalStatus": "COMPLETED",
 *   "totalRuns": 18,
 *   "successfulRuns": 17,
 *   "failedRuns": 1,
 *   "totalDurationMs": 45000
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentWebSocketHandler
 * @see com.locallab.model.enums.WebSocketMessageType#EXPERIMENT_COMPLETED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentCompletedPayload {

    /**
     * The final status of the experiment.
     *
     * <p>Either COMPLETED (all runs finished successfully or with partial failures) or FAILED
     * (experiment was cancelled or encountered a critical error).
     */
    private ExperimentStatus finalStatus;

    /**
     * The total number of runs originally planned for the experiment.
     *
     * <p>This represents the intended total from the experiment configuration (models x embeddings
     * x iterations). When an experiment is cancelled, this may be greater than successfulRuns +
     * failedRuns since not all planned runs were executed.
     */
    private int totalRuns;

    /** The number of runs that completed successfully. */
    private int successfulRuns;

    /** The number of runs that failed. */
    private int failedRuns;

    /**
     * The total duration of the experiment in milliseconds.
     *
     * <p>Measures the time from experiment start to completion of the final run.
     */
    private Long totalDurationMs;
}
