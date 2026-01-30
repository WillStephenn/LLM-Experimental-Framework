package com.locallab.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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

/**
 * Unit tests for {@link ExperimentWebSocketHandler}.
 *
 * <p>These tests verify the correct construction and broadcasting of WebSocket messages for
 * experiment progress updates.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
class ExperimentWebSocketHandlerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private ExperimentWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExperimentWebSocketHandler(messagingTemplate);
    }

    @Nested
    @DisplayName("broadcastProgress")
    class BroadcastProgressTests {

        @Test
        @DisplayName("should broadcast progress message with correct payload")
        void shouldBroadcastProgressWithCorrectPayload() {
            handler.broadcastProgress(1L, 10, 5, 1, 6L, 25000L);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/experiments/1/progress"), captor.capture());

            WebSocketMessage message = captor.getValue();
            assertEquals(WebSocketMessageType.PROGRESS, message.getType());
            assertEquals(1L, message.getExperimentId());
            assertNotNull(message.getTimestamp());

            ProgressPayload payload = (ProgressPayload) message.getPayload();
            assertEquals(10, payload.getTotalRuns());
            assertEquals(5, payload.getCompletedRuns());
            assertEquals(1, payload.getFailedRuns());
            assertEquals(50.0, payload.getPercentComplete(), 0.1);
            assertEquals(6L, payload.getCurrentRunId());
            assertEquals(25000L, payload.getEstimatedTimeRemainingMs());
        }

        @Test
        @DisplayName("should calculate percentage correctly when totalRuns is zero")
        void shouldCalculatePercentageCorrectlyWhenTotalRunsIsZero() {
            handler.broadcastProgress(1L, 0, 0, 0, null, null);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

            ProgressPayload payload = (ProgressPayload) captor.getValue().getPayload();
            assertEquals(0.0, payload.getPercentComplete(), 0.1);
        }
    }

    @Nested
    @DisplayName("broadcastRunStarted")
    class BroadcastRunStartedTests {

        @Test
        @DisplayName("should broadcast run started message with correct payload")
        void shouldBroadcastRunStartedWithCorrectPayload() {
            handler.broadcastRunStarted(1L, 10L, "llama3:8b", 2, "nomic-embed-text");

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/experiments/1/progress"), captor.capture());

            WebSocketMessage message = captor.getValue();
            assertEquals(WebSocketMessageType.RUN_STARTED, message.getType());
            assertEquals(1L, message.getExperimentId());

            RunStartedPayload payload = (RunStartedPayload) message.getPayload();
            assertEquals(10L, payload.getRunId());
            assertEquals("llama3:8b", payload.getModelName());
            assertEquals(2, payload.getIteration());
            assertEquals("nomic-embed-text", payload.getEmbeddingModel());
        }

        @Test
        @DisplayName("should handle null embedding model")
        void shouldHandleNullEmbeddingModel() {
            handler.broadcastRunStarted(1L, 10L, "llama3:8b", 1, null);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

            RunStartedPayload payload = (RunStartedPayload) captor.getValue().getPayload();
            assertNull(payload.getEmbeddingModel());
        }
    }

    @Nested
    @DisplayName("broadcastRunCompleted")
    class BroadcastRunCompletedTests {

        @Test
        @DisplayName("should broadcast run completed from ExperimentRun entity")
        void shouldBroadcastRunCompletedFromEntity() {
            ExperimentRun run =
                    ExperimentRun.builder()
                            .id(10L)
                            .status(RunStatus.SUCCESS)
                            .durationMs(2500L)
                            .tokensPerSecond(45.5)
                            .errorMessage(null)
                            .build();

            handler.broadcastRunCompleted(1L, run);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/experiments/1/progress"), captor.capture());

            WebSocketMessage message = captor.getValue();
            assertEquals(WebSocketMessageType.RUN_COMPLETED, message.getType());

            RunCompletedPayload payload = (RunCompletedPayload) message.getPayload();
            assertEquals(10L, payload.getRunId());
            assertEquals(RunStatus.SUCCESS, payload.getStatus());
            assertEquals(2500L, payload.getDurationMs());
            assertEquals(45.5, payload.getTokensPerSecond());
            assertNull(payload.getErrorMessage());
        }

        @Test
        @DisplayName("should broadcast run completed with parameters")
        void shouldBroadcastRunCompletedWithParameters() {
            handler.broadcastRunCompleted(1L, 10L, RunStatus.FAILED, null, null, "Model not found");

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

            RunCompletedPayload payload = (RunCompletedPayload) captor.getValue().getPayload();
            assertEquals(10L, payload.getRunId());
            assertEquals(RunStatus.FAILED, payload.getStatus());
            assertNull(payload.getDurationMs());
            assertEquals("Model not found", payload.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("broadcastExperimentCompleted")
    class BroadcastExperimentCompletedTests {

        @Test
        @DisplayName("should broadcast experiment completed message")
        void shouldBroadcastExperimentCompleted() {
            handler.broadcastExperimentCompleted(1L, ExperimentStatus.COMPLETED, 18, 17, 1, 45000L);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/experiments/1/progress"), captor.capture());

            WebSocketMessage message = captor.getValue();
            assertEquals(WebSocketMessageType.EXPERIMENT_COMPLETED, message.getType());

            ExperimentCompletedPayload payload = (ExperimentCompletedPayload) message.getPayload();
            assertEquals(ExperimentStatus.COMPLETED, payload.getFinalStatus());
            assertEquals(18, payload.getTotalRuns());
            assertEquals(17, payload.getSuccessfulRuns());
            assertEquals(1, payload.getFailedRuns());
            assertEquals(45000L, payload.getTotalDurationMs());
        }

        @Test
        @DisplayName("should handle failed experiment status")
        void shouldHandleFailedExperimentStatus() {
            handler.broadcastExperimentCompleted(1L, ExperimentStatus.FAILED, 10, 5, 5, null);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

            ExperimentCompletedPayload payload =
                    (ExperimentCompletedPayload) captor.getValue().getPayload();
            assertEquals(ExperimentStatus.FAILED, payload.getFinalStatus());
            assertNull(payload.getTotalDurationMs());
        }
    }

    @Nested
    @DisplayName("broadcastExperimentPaused")
    class BroadcastExperimentPausedTests {

        @Test
        @DisplayName("should broadcast experiment paused message")
        void shouldBroadcastExperimentPaused() {
            handler.broadcastExperimentPaused(1L, 10, 8);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/experiments/1/progress"), captor.capture());

            WebSocketMessage message = captor.getValue();
            assertEquals(WebSocketMessageType.EXPERIMENT_PAUSED, message.getType());

            ExperimentPausedPayload payload = (ExperimentPausedPayload) message.getPayload();
            assertEquals(10, payload.getCompletedRuns());
            assertEquals(8, payload.getRemainingRuns());
        }
    }

    @Nested
    @DisplayName("broadcastError")
    class BroadcastErrorTests {

        @Test
        @DisplayName("should broadcast error message")
        void shouldBroadcastError() {
            handler.broadcastError(1L, "OLLAMA_UNAVAILABLE", "Cannot connect to Ollama", true);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/experiments/1/progress"), captor.capture());

            WebSocketMessage message = captor.getValue();
            assertEquals(WebSocketMessageType.ERROR, message.getType());

            ErrorPayload payload = (ErrorPayload) message.getPayload();
            assertEquals("OLLAMA_UNAVAILABLE", payload.getErrorCode());
            assertEquals("Cannot connect to Ollama", payload.getMessage());
            assertTrue(payload.isRecoverable());
        }

        @Test
        @DisplayName("should handle non-recoverable error")
        void shouldHandleNonRecoverableError() {
            handler.broadcastError(1L, "MODEL_NOT_FOUND", "Model not found", false);

            ArgumentCaptor<WebSocketMessage> captor =
                    ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

            ErrorPayload payload = (ErrorPayload) captor.getValue().getPayload();
            assertFalse(payload.isRecoverable());
        }
    }

    @Nested
    @DisplayName("buildTopicDestination")
    class BuildTopicDestinationTests {

        @Test
        @DisplayName("should build correct topic destination")
        void shouldBuildCorrectTopicDestination() {
            String destination = handler.buildTopicDestination(42L);
            assertEquals("/topic/experiments/42/progress", destination);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle messaging template exception gracefully")
        void shouldHandleMessagingExceptionGracefully() {
            doThrow(new RuntimeException("Connection failed"))
                    .when(messagingTemplate)
                    .convertAndSend(anyString(), any(WebSocketMessage.class));

            // Should not throw exception
            assertDoesNotThrow(() -> handler.broadcastProgress(1L, 10, 5, 1, null, null));
        }
    }
}
