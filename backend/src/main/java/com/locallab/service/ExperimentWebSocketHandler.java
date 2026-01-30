package com.locallab.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.locallab.dto.WebSocketMessage;
import com.locallab.dto.websocket.ErrorPayload;
import com.locallab.dto.websocket.ExperimentCompletedPayload;
import com.locallab.dto.websocket.ExperimentPausedPayload;
import com.locallab.dto.websocket.ProgressPayload;
import com.locallab.dto.websocket.RunCompletedPayload;
import com.locallab.dto.websocket.RunStartedPayload;
import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;
import com.locallab.model.enums.WebSocketMessageType;

import lombok.RequiredArgsConstructor;

/**
 * Service for handling WebSocket communication during experiment execution.
 *
 * <p>This service provides a centralised interface for broadcasting real-time updates to clients
 * subscribed to experiment progress topics. It encapsulates the message construction and
 * transmission logic, ensuring consistent message formats as defined in the API contract.
 *
 * <h3>WebSocket Topic:</h3>
 *
 * <p>All messages are published to: {@code /topic/experiments/{experimentId}/progress}
 *
 * <h3>Supported Message Types:</h3>
 *
 * <ul>
 *   <li>{@link WebSocketMessageType#PROGRESS} - Overall experiment progress
 *   <li>{@link WebSocketMessageType#RUN_STARTED} - New run beginning
 *   <li>{@link WebSocketMessageType#RUN_COMPLETED} - Run finished (success/failure)
 *   <li>{@link WebSocketMessageType#EXPERIMENT_COMPLETED} - All runs finished
 *   <li>{@link WebSocketMessageType#EXPERIMENT_PAUSED} - Experiment paused
 *   <li>{@link WebSocketMessageType#ERROR} - Critical error occurred
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 *
 * <p>This service is thread-safe. The underlying {@link SimpMessagingTemplate} handles concurrent
 * message transmission safely.
 *
 * @author William Stephen
 * @see WebSocketMessage
 * @see com.locallab.config.WebSocketConfig
 */
@Service
@RequiredArgsConstructor
public class ExperimentWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentWebSocketHandler.class);

    /** Base path for experiment progress topics. */
    private static final String TOPIC_BASE = "/topic/experiments/";

    /** Suffix for progress subscription topic. */
    private static final String TOPIC_SUFFIX = "/progress";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts a progress update for an experiment.
     *
     * <p>Sent after each run completes to provide overall experiment progress information,
     * including completed runs, failed runs, and estimated time remaining.
     *
     * @param experimentId the ID of the experiment
     * @param totalRuns the total number of runs to execute
     * @param completedRuns the number of runs that have completed
     * @param failedRuns the number of runs that have failed
     * @param currentRunId the ID of the currently executing run (may be null)
     * @param estimatedTimeRemainingMs estimated time remaining in milliseconds (may be null)
     */
    public void broadcastProgress(
            Long experimentId,
            int totalRuns,
            int completedRuns,
            int failedRuns,
            Long currentRunId,
            Long estimatedTimeRemainingMs) {

        double percentComplete = totalRuns > 0 ? (double) completedRuns / totalRuns * 100 : 0;

        ProgressPayload payload =
                ProgressPayload.builder()
                        .totalRuns(totalRuns)
                        .completedRuns(completedRuns)
                        .failedRuns(failedRuns)
                        .percentComplete(percentComplete)
                        .currentRunId(currentRunId)
                        .estimatedTimeRemainingMs(estimatedTimeRemainingMs)
                        .build();

        sendMessage(experimentId, WebSocketMessageType.PROGRESS, payload);
    }

    /**
     * Broadcasts that a run has started execution.
     *
     * <p>Sent immediately before model generation begins for a run.
     *
     * @param experimentId the ID of the experiment
     * @param runId the ID of the run (may be null if not yet persisted)
     * @param modelName the name of the model being used
     * @param iteration the iteration number
     * @param embeddingModel the embedding model (null if not using RAG)
     */
    public void broadcastRunStarted(
            Long experimentId, Long runId, String modelName, int iteration, String embeddingModel) {

        RunStartedPayload payload =
                RunStartedPayload.builder()
                        .runId(runId)
                        .modelName(modelName)
                        .iteration(iteration)
                        .embeddingModel(embeddingModel)
                        .build();

        sendMessage(experimentId, WebSocketMessageType.RUN_STARTED, payload);
    }

    /**
     * Broadcasts that a run has completed execution.
     *
     * <p>Sent when a run finishes, whether successful or failed.
     *
     * @param experimentId the ID of the experiment
     * @param run the completed experiment run
     */
    public void broadcastRunCompleted(Long experimentId, ExperimentRun run) {
        RunCompletedPayload payload =
                RunCompletedPayload.builder()
                        .runId(run.getId())
                        .status(run.getStatus())
                        .durationMs(run.getDurationMs())
                        .tokensPerSecond(run.getTokensPerSecond())
                        .errorMessage(run.getErrorMessage())
                        .build();

        sendMessage(experimentId, WebSocketMessageType.RUN_COMPLETED, payload);
    }

    /**
     * Broadcasts that a run has completed execution using individual parameters.
     *
     * <p>Alternative to {@link #broadcastRunCompleted(Long, ExperimentRun)} for cases where the
     * full ExperimentRun entity is not available.
     *
     * @param experimentId the ID of the experiment
     * @param runId the ID of the completed run
     * @param status the final status of the run
     * @param durationMs the duration in milliseconds (may be null)
     * @param tokensPerSecond the tokens per second metric (may be null)
     * @param errorMessage error message if failed (may be null)
     */
    public void broadcastRunCompleted(
            Long experimentId,
            Long runId,
            RunStatus status,
            Long durationMs,
            Double tokensPerSecond,
            String errorMessage) {

        RunCompletedPayload payload =
                RunCompletedPayload.builder()
                        .runId(runId)
                        .status(status)
                        .durationMs(durationMs)
                        .tokensPerSecond(tokensPerSecond)
                        .errorMessage(errorMessage)
                        .build();

        sendMessage(experimentId, WebSocketMessageType.RUN_COMPLETED, payload);
    }

    /**
     * Broadcasts that an experiment has completed all runs.
     *
     * <p>Sent when the experiment transitions to COMPLETED or FAILED status.
     *
     * @param experimentId the ID of the experiment
     * @param finalStatus the final status of the experiment
     * @param totalRuns the total number of runs
     * @param successfulRuns the number of successful runs
     * @param failedRuns the number of failed runs
     * @param totalDurationMs the total duration in milliseconds
     */
    public void broadcastExperimentCompleted(
            Long experimentId,
            ExperimentStatus finalStatus,
            int totalRuns,
            int successfulRuns,
            int failedRuns,
            Long totalDurationMs) {

        ExperimentCompletedPayload payload =
                ExperimentCompletedPayload.builder()
                        .finalStatus(finalStatus)
                        .totalRuns(totalRuns)
                        .successfulRuns(successfulRuns)
                        .failedRuns(failedRuns)
                        .totalDurationMs(totalDurationMs)
                        .build();

        sendMessage(experimentId, WebSocketMessageType.EXPERIMENT_COMPLETED, payload);
    }

    /**
     * Broadcasts that an experiment has been paused.
     *
     * <p>Sent when a user pauses the experiment.
     *
     * @param experimentId the ID of the experiment
     * @param completedRuns the number of runs completed before pause
     * @param remainingRuns the number of runs remaining
     */
    public void broadcastExperimentPaused(Long experimentId, int completedRuns, int remainingRuns) {

        ExperimentPausedPayload payload =
                ExperimentPausedPayload.builder()
                        .completedRuns(completedRuns)
                        .remainingRuns(remainingRuns)
                        .build();

        sendMessage(experimentId, WebSocketMessageType.EXPERIMENT_PAUSED, payload);
    }

    /**
     * Broadcasts an error that occurred during experiment execution.
     *
     * <p>Sent when a critical error prevents experiment continuation.
     *
     * @param experimentId the ID of the experiment
     * @param errorCode the error code identifier
     * @param message the error message
     * @param recoverable whether the error is recoverable
     */
    public void broadcastError(
            Long experimentId, String errorCode, String message, boolean recoverable) {

        ErrorPayload payload =
                ErrorPayload.builder()
                        .errorCode(errorCode)
                        .message(message)
                        .recoverable(recoverable)
                        .build();

        sendMessage(experimentId, WebSocketMessageType.ERROR, payload);
    }

    /**
     * Sends a WebSocket message to the experiment progress topic.
     *
     * <p>Constructs the message envelope and transmits it to the appropriate topic.
     *
     * @param experimentId the ID of the experiment
     * @param type the message type
     * @param payload the message payload
     */
    private void sendMessage(Long experimentId, WebSocketMessageType type, Object payload) {
        WebSocketMessage message =
                WebSocketMessage.builder()
                        .type(type)
                        .experimentId(experimentId)
                        .payload(payload)
                        .timestamp(Instant.now())
                        .build();

        String destination = TOPIC_BASE + experimentId + TOPIC_SUFFIX;

        try {
            messagingTemplate.convertAndSend(destination, message);
            LOGGER.debug("Published {} message to {}", type, destination);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to publish WebSocket message to {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Builds the WebSocket topic destination for a given experiment.
     *
     * <p>Useful for clients that need to construct the subscription topic.
     *
     * @param experimentId the ID of the experiment
     * @return the full topic path
     */
    public String buildTopicDestination(Long experimentId) {
        return TOPIC_BASE + experimentId + TOPIC_SUFFIX;
    }
}
