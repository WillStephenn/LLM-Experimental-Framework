package com.locallab.model.enums;

/**
 * Defines the types of messages sent via WebSocket during experiment execution.
 *
 * <p>These message types are used to communicate real-time updates to clients subscribed to
 * experiment progress topics. Each type corresponds to a specific event in the experiment execution
 * lifecycle.
 *
 * <h3>Message Flow:</h3>
 *
 * <pre>
 * Experiment Start
 *       │
 *       ├─► RUN_STARTED ─► PROGRESS ─► RUN_COMPLETED ─┐
 *       │                                             │
 *       │◄─────────────── (repeat for each run) ◄────┘
 *       │
 *       ├─► EXPERIMENT_COMPLETED (all runs finished)
 *       ├─► EXPERIMENT_PAUSED (user paused)
 *       └─► ERROR (critical failure)
 * </pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.WebSocketMessage
 * @see com.locallab.service.ExperimentWebSocketHandler
 */
public enum WebSocketMessageType {

    /**
     * Overall experiment progress update.
     *
     * <p>Sent after each run completes to provide an updated view of experiment progress, including
     * completed runs, failed runs, and estimated time remaining.
     */
    PROGRESS,

    /**
     * Indicates a new run has begun execution.
     *
     * <p>Sent immediately before the model generation starts. Contains information about the run
     * configuration including model name, iteration number, and embedding model.
     */
    RUN_STARTED,

    /**
     * Indicates a run has finished execution.
     *
     * <p>Sent when a run completes, whether successful or failed. Contains the run status,
     * duration, performance metrics, and any error message if applicable.
     */
    RUN_COMPLETED,

    /**
     * Indicates all runs in the experiment have finished.
     *
     * <p>Sent when the experiment transitions to COMPLETED or FAILED status. Contains the final
     * status, total runs, successful runs, failed runs, and total duration.
     */
    EXPERIMENT_COMPLETED,

    /**
     * Indicates the experiment has been paused.
     *
     * <p>Sent when a user pauses the experiment. Contains information about completed runs and
     * remaining runs to be executed upon resume.
     */
    EXPERIMENT_PAUSED,

    /**
     * Indicates a critical error has occurred.
     *
     * <p>Sent when an unrecoverable error occurs that prevents experiment continuation. Contains an
     * error code, descriptive message, and whether the error is recoverable.
     */
    ERROR
}
