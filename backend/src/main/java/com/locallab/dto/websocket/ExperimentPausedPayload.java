package com.locallab.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for EXPERIMENT_PAUSED WebSocket messages.
 *
 * <p>Provides information when an experiment has been paused by user request. This payload conforms
 * to the API contract specification for the EXPERIMENT_PAUSED message type.
 *
 * <h3>Example JSON:</h3>
 *
 * <pre>{@code
 * {
 *   "completedRuns": 10,
 *   "remainingRuns": 8
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentWebSocketHandler
 * @see com.locallab.model.enums.WebSocketMessageType#EXPERIMENT_PAUSED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentPausedPayload {

    /**
     * The number of runs that completed before the pause.
     *
     * <p>Includes both successful and failed runs.
     */
    private int completedRuns;

    /**
     * The number of runs remaining to be executed.
     *
     * <p>These runs will be executed when the experiment is resumed.
     */
    private int remainingRuns;
}
