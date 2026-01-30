package com.locallab.dto.websocket;

import com.locallab.model.enums.RunStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for RUN_COMPLETED WebSocket messages.
 *
 * <p>Provides information about a run that has finished execution (success or failure). This
 * payload conforms to the API contract specification for the RUN_COMPLETED message type.
 *
 * <h3>Example JSON:</h3>
 *
 * <pre>{@code
 * {
 *   "runId": 6,
 *   "status": "SUCCESS",
 *   "durationMs": 2500,
 *   "tokensPerSecond": 45.5,
 *   "errorMessage": null
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentWebSocketHandler
 * @see com.locallab.model.enums.WebSocketMessageType#RUN_COMPLETED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunCompletedPayload {

    /** The unique identifier of the completed run. */
    private Long runId;

    /**
     * The final status of the run.
     *
     * <p>Either SUCCESS or FAILED, indicating whether the model generation completed successfully.
     */
    private RunStatus status;

    /**
     * The total duration of the run in milliseconds.
     *
     * <p>Measures the time from generation request to response completion. May be null for failed
     * runs that did not complete generation.
     */
    private Long durationMs;

    /**
     * The tokens per second throughput metric.
     *
     * <p>Calculated from the model's generation performance. May be null for failed runs.
     */
    private Double tokensPerSecond;

    /**
     * Error message describing the failure reason.
     *
     * <p>Null for successful runs. Contains a descriptive error message for failed runs.
     */
    private String errorMessage;
}
