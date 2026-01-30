package com.locallab.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for ERROR WebSocket messages.
 *
 * <p>Provides information about critical errors that occur during experiment execution. This
 * payload conforms to the API contract specification for the ERROR message type.
 *
 * <h3>Example JSON:</h3>
 *
 * <pre>{@code
 * {
 *   "errorCode": "OLLAMA_UNAVAILABLE",
 *   "message": "Failed to connect to Ollama service",
 *   "recoverable": false
 * }
 * }</pre>
 *
 * <h3>Standard Error Codes:</h3>
 *
 * <ul>
 *   <li>{@code OLLAMA_UNAVAILABLE} - Cannot connect to Ollama (recoverable)
 *   <li>{@code CHROMA_UNAVAILABLE} - Cannot connect to Chroma (recoverable)
 *   <li>{@code MODEL_NOT_FOUND} - Requested model not available (not recoverable)
 *   <li>{@code OUT_OF_MEMORY} - Insufficient memory for model (not recoverable)
 *   <li>{@code GENERATION_TIMEOUT} - Model generation timed out (recoverable)
 * </ul>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentWebSocketHandler
 * @see com.locallab.model.enums.WebSocketMessageType#ERROR
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorPayload {

    /**
     * Error code identifying the type of error.
     *
     * <p>Standard codes include: OLLAMA_UNAVAILABLE, CHROMA_UNAVAILABLE, MODEL_NOT_FOUND,
     * OUT_OF_MEMORY, GENERATION_TIMEOUT.
     */
    private String errorCode;

    /**
     * Human-readable description of the error.
     *
     * <p>Provides context about what went wrong and potential remediation steps.
     */
    private String message;

    /**
     * Indicates whether the error is recoverable.
     *
     * <p>If true, the client may retry the operation. If false, the experiment cannot continue
     * without user intervention.
     */
    private boolean recoverable;
}
