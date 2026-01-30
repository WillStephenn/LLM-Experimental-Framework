package com.locallab.dto;

import java.time.Instant;

import com.locallab.model.enums.WebSocketMessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a message sent via WebSocket during experiment execution.
 *
 * <p>This class provides a standardised envelope for all WebSocket messages sent during experiment
 * execution. It includes the message type, experiment ID, payload, and timestamp for client-side
 * processing.
 *
 * <h3>Message Structure (as per API Contract):</h3>
 *
 * <pre>{@code
 * {
 *   "type": "MESSAGE_TYPE",
 *   "experimentId": 1,
 *   "timestamp": "2025-11-27T10:05:00Z",
 *   "payload": { ... }
 * }
 * }</pre>
 *
 * <h3>Message Types:</h3>
 *
 * <ul>
 *   <li>{@code PROGRESS} - Overall progress update with completed/total runs
 *   <li>{@code RUN_STARTED} - Sent when a new run begins execution
 *   <li>{@code RUN_COMPLETED} - Sent when a run finishes (success or failure)
 *   <li>{@code EXPERIMENT_COMPLETED} - Sent when all runs finish
 *   <li>{@code EXPERIMENT_PAUSED} - Sent when experiment is paused
 *   <li>{@code ERROR} - Sent when a critical error occurs
 * </ul>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>{@code
 * WebSocketMessage message = WebSocketMessage.builder()
 *     .type(WebSocketMessageType.RUN_COMPLETED)
 *     .experimentId(1L)
 *     .payload(runCompletedPayload)
 *     .timestamp(Instant.now())
 *     .build();
 * }</pre>
 *
 * <h3>WebSocket Destination:</h3>
 *
 * <p>Messages are published to: {@code /topic/experiments/{experimentId}/progress}
 *
 * @author William Stephen
 * @see com.locallab.model.enums.WebSocketMessageType
 * @see com.locallab.service.ExperimentWebSocketHandler
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
     * <p>Used by clients to determine how to process the payload. Corresponds to values in {@link
     * WebSocketMessageType}.
     */
    private WebSocketMessageType type;

    /**
     * The unique identifier of the experiment this message relates to.
     *
     * <p>Used by clients to associate messages with the correct experiment context.
     */
    private Long experimentId;

    /**
     * The message payload.
     *
     * <p>The structure of the payload depends on the message type. Each type has a corresponding
     * payload DTO in the {@code com.locallab.dto.websocket} package.
     */
    private Object payload;

    /**
     * The timestamp when this message was created.
     *
     * <p>Uses ISO 8601 format for serialisation. Set to the instant when the message is
     * constructed.
     */
    private Instant timestamp;
}
