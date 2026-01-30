package com.locallab.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for PROGRESS WebSocket messages.
 *
 * <p>Provides real-time progress information for an executing experiment, sent after each run
 * completes. This payload conforms to the API contract specification for the PROGRESS message type.
 *
 * <h3>Example JSON:</h3>
 *
 * <pre>{@code
 * {
 *   "totalRuns": 18,
 *   "completedRuns": 5,
 *   "failedRuns": 0,
 *   "percentComplete": 27.8,
 *   "currentRunId": 6,
 *   "estimatedTimeRemainingMs": 32500
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentWebSocketHandler
 * @see com.locallab.model.enums.WebSocketMessageType#PROGRESS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressPayload {

    /**
     * The total number of runs to be executed.
     *
     * <p>Calculated from the experiment configuration: models x embeddings x iterations.
     */
    private int totalRuns;

    /**
     * The number of runs that have completed.
     *
     * <p>This includes both successful and failed runs.
     */
    private int completedRuns;

    /**
     * The number of runs that have failed.
     *
     * <p>A subset of completedRuns that ended with an error.
     */
    private int failedRuns;

    /**
     * The percentage of the experiment that has completed.
     *
     * <p>Calculated as: (completedRuns / totalRuns) * 100. Range: 0.0 to 100.0.
     */
    private double percentComplete;

    /**
     * The identifier of the next run about to be executed.
     *
     * <p>Typically null in progress messages sent after a run completes, as the next run has not
     * yet started. May be set when broadcasting progress at the start of a run.
     */
    private Long currentRunId;

    /**
     * Estimated time remaining in milliseconds.
     *
     * <p>Calculated based on average run duration and remaining runs. May be null if insufficient
     * data is available for estimation.
     */
    private Long estimatedTimeRemainingMs;
}
