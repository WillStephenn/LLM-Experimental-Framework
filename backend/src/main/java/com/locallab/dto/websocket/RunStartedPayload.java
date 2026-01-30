package com.locallab.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for RUN_STARTED WebSocket messages.
 *
 * <p>Provides information about a run that has just begun execution. This payload conforms to the
 * API contract specification for the RUN_STARTED message type.
 *
 * <h3>Example JSON:</h3>
 *
 * <pre>{@code
 * {
 *   "runId": 6,
 *   "modelName": "qwen2.5-coder:7b",
 *   "iteration": 2,
 *   "embeddingModel": "nomic-embed-text"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentWebSocketHandler
 * @see com.locallab.model.enums.WebSocketMessageType#RUN_STARTED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunStartedPayload {

    /**
     * The unique identifier of the run.
     *
     * <p>May be null if the run ID has not yet been assigned (before persistence).
     */
    private Long runId;

    /**
     * The name of the model being used for this run.
     *
     * <p>Corresponds to the Ollama model identifier (e.g., "qwen2.5-coder:7b").
     */
    private String modelName;

    /**
     * The iteration number of this run.
     *
     * <p>Runs are numbered starting from 1 within each model/embedding combination.
     */
    private int iteration;

    /**
     * The embedding model being used for RAG context.
     *
     * <p>Null if the experiment is not using RAG context mode.
     */
    private String embeddingModel;
}
