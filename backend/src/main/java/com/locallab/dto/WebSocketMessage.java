package com.locallab.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a message sent via WebSocket during experiment execution.
 *
 * <p>This class provides a standardised envelope for all WebSocket messages sent during experiment
 * execution. It includes the message type, payload, and timestamp for client-side processing.
 *
 * <h3>Message Types:</h3>
 *
 * <ul>
 *   <li>{@code RUN_STARTED} - Sent when a new run begins execution. Payload contains run config.
 *   <li>{@code RUN_COMPLETED} - Sent when a run finishes. Payload contains the ExperimentRun.
 *   <li>{@code PROGRESS} - Sent after each run completes. Payload contains ExperimentProgress.
 *   <li>{@code EXPERIMENT_COMPLETED} - Sent when all runs finish. Payload is null.
 *   <li>{@code EXPERIMENT_PAUSED} - Sent when experiment is paused. Payload is null.
 *   <li>{@code EXPERIMENT_FAILED} - Sent when experiment fails/is cancelled. Payload may contain
 *       error details.
 * </ul>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>{@code
 * WebSocketMessage message = WebSocketMessage.builder()
 *     .type("RUN_COMPLETED")
 *     .payload(experimentRun)
 *     .timestamp(Instant.now())
 *     .build();
 * }</pre>
 *
 * <h3>WebSocket Destination:</h3>
 *
 * <p>Messages are published to: {@code /topic/experiments/{experimentId}/progress}
 *
 * @author William Stephen
 * @see com.locallab.dto.ExperimentProgress
 * @see com.locallab.service.ExperimentExecutorService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * The type of message being sent.
     *
     * <p>Used by clients to determine how to process the payload. Standard types include:
     * RUN_STARTED, RUN_COMPLETED, PROGRESS, EXPERIMENT_COMPLETED, EXPERIMENT_PAUSED,
     * EXPERIMENT_FAILED.
     */
    private String type;

    /**
     * The message payload.
     *
     * <p>The structure of the payload depends on the message type. Can be null for messages like
     * EXPERIMENT_COMPLETED that don't require additional data.
     */
    private Object payload;

    /**
     * The timestamp when this message was created.
     *
     * <p>Used by clients for ordering and logging purposes. Set to the instant when the message is
     * constructed.
     */
    private Instant timestamp;
}
