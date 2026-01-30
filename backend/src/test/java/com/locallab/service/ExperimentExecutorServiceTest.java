package com.locallab.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.dto.ExperimentConfig;
import com.locallab.dto.ExperimentExecutionState;
import com.locallab.dto.ExperimentProgress;
import com.locallab.dto.Hyperparameters;
import com.locallab.dto.RetrievedChunk;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.model.Experiment;
import com.locallab.model.ExperimentRun;
import com.locallab.model.SystemPrompt;
import com.locallab.model.TaskTemplate;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;
import com.locallab.repository.ExperimentRepository;
import com.locallab.repository.ExperimentRunRepository;
import com.locallab.repository.SystemPromptRepository;
import com.locallab.service.ExperimentExecutorService.ExperimentRunConfig;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link ExperimentExecutorService}.
 *
 * <p>These tests verify the behaviour of the experiment executor service, including experiment
 * execution, pause/resume/cancel functionality, run configuration generation, and WebSocket message
 * publishing.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
class ExperimentExecutorServiceTest {

    @Mock private ExperimentRepository experimentRepository;
    @Mock private ExperimentRunRepository experimentRunRepository;
    @Mock private OllamaService ollamaService;
    @Mock private RagService ragService;
    @Mock private TaskService taskService;
    @Mock private SystemPromptRepository systemPromptRepository;
    @Mock private ExperimentWebSocketHandler webSocketHandler;

    private ObjectMapper objectMapper;
    private ExperimentExecutorService experimentExecutorService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        experimentExecutorService =
                new ExperimentExecutorService(
                        experimentRepository,
                        experimentRunRepository,
                        ollamaService,
                        ragService,
                        taskService,
                        systemPromptRepository,
                        webSocketHandler,
                        objectMapper);
    }

    /**
     * Helper method to inject an execution state into the service for testing. Uses reflection to
     * access the private executionStates map.
     */
    @SuppressWarnings("unchecked")
    private void injectExecutionState(Long experimentId, ExperimentExecutionState state)
            throws Exception {
        Field field = ExperimentExecutorService.class.getDeclaredField("executionStates");
        field.setAccessible(true);
        Map<Long, ExperimentExecutionState> states =
                (Map<Long, ExperimentExecutionState>) field.get(experimentExecutorService);
        states.put(experimentId, state);
    }

    /**
     * Helper method to get the execution states map for verification. Uses reflection to access the
     * private executionStates map.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, ExperimentExecutionState> getExecutionStates() throws Exception {
        Field field = ExperimentExecutorService.class.getDeclaredField("executionStates");
        field.setAccessible(true);
        return (Map<Long, ExperimentExecutionState>) field.get(experimentExecutorService);
    }

    @Nested
    @DisplayName("startExperiment")
    class StartExperimentTests {

        @Test
        @DisplayName("should throw EntityNotFoundException when experiment not found")
        void shouldThrowWhenExperimentNotFound() {
            when(experimentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> experimentExecutorService.startExperiment(1L));
        }

        @Test
        @DisplayName("should throw IllegalStateException when experiment not in DRAFT status")
        void shouldThrowWhenNotDraft() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.RUNNING);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.startExperiment(1L));

            assertTrue(exception.getMessage().contains("DRAFT"));
        }

        @Test
        @DisplayName("should throw IllegalStateException for COMPLETED status")
        void shouldThrowWhenCompleted() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.COMPLETED);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.startExperiment(1L));

            assertTrue(exception.getMessage().contains("DRAFT"));
            assertTrue(exception.getMessage().contains("COMPLETED"));
        }

        @Test
        @DisplayName("should throw IllegalStateException for FAILED status")
        void shouldThrowWhenFailed() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.FAILED);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.startExperiment(1L));

            assertTrue(exception.getMessage().contains("DRAFT"));
        }

        @Test
        @DisplayName("should throw IllegalStateException for PAUSED status")
        void shouldThrowWhenPaused() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.PAUSED);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.startExperiment(1L));

            assertTrue(exception.getMessage().contains("DRAFT"));
        }
    }

    @Nested
    @DisplayName("resumeExperiment")
    class ResumeExperimentTests {

        @Test
        @DisplayName("should throw EntityNotFoundException when experiment not found")
        void shouldThrowWhenExperimentNotFound() {
            when(experimentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> experimentExecutorService.resumeExperiment(1L));
        }

        @Test
        @DisplayName("should throw IllegalStateException when experiment not in PAUSED status")
        void shouldThrowWhenNotPaused() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.resumeExperiment(1L));

            assertTrue(exception.getMessage().contains("PAUSED"));
        }

        @Test
        @DisplayName("should throw IllegalStateException for RUNNING status")
        void shouldThrowWhenRunning() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.RUNNING);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.resumeExperiment(1L));

            assertTrue(exception.getMessage().contains("PAUSED"));
        }

        @Test
        @DisplayName("should throw IllegalStateException for COMPLETED status")
        void shouldThrowWhenCompleted() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.COMPLETED);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.resumeExperiment(1L));

            assertTrue(exception.getMessage().contains("PAUSED"));
        }

        @Test
        @DisplayName("should resume successfully when experiment is paused with remaining runs")
        void shouldResumeSuccessfully() throws Exception {
            Experiment experiment = createExperiment(1L, ExperimentStatus.PAUSED);
            String configJson = objectMapper.writeValueAsString(createBasicConfig());
            experiment.setConfig(configJson);
            experiment.setTaskTemplate(createTaskTemplate());
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
            when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);

            List<ExperimentRun> completedRuns = new ArrayList<>();
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(completedRuns);

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            experimentExecutorService.resumeExperiment(1L);

            Thread.sleep(500);

            verify(experimentRepository, atLeastOnce()).save(any(Experiment.class));
            verify(webSocketHandler, atLeastOnce())
                    .broadcastExperimentCompleted(
                            eq(1L),
                            any(ExperimentStatus.class),
                            anyInt(),
                            anyInt(),
                            anyInt(),
                            any());
        }

        @Test
        @DisplayName("should mark as completed when all runs already finished")
        void shouldCompleteWhenAllRunsFinished() throws Exception {
            Experiment experiment = createExperiment(1L, ExperimentStatus.PAUSED);
            String configJson = objectMapper.writeValueAsString(createBasicConfig());
            experiment.setConfig(configJson);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
            when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);

            List<ExperimentRun> completedRuns = new ArrayList<>();
            completedRuns.add(createExperimentRun(1L, experiment));
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(completedRuns);

            experimentExecutorService.resumeExperiment(1L);

            Thread.sleep(200);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository, atLeast(2)).save(captor.capture());

            List<Experiment> savedExperiments = captor.getAllValues();
            boolean hasCompleted =
                    savedExperiments.stream()
                            .anyMatch(exp -> exp.getStatus() == ExperimentStatus.COMPLETED);
            assertTrue(hasCompleted);
        }

        @Test
        @DisplayName("should throw IllegalStateException for FAILED status")
        void shouldThrowWhenFailed() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.FAILED);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentExecutorService.resumeExperiment(1L));

            assertTrue(exception.getMessage().contains("PAUSED"));
        }
    }

    @Nested
    @DisplayName("pauseExperiment")
    class PauseExperimentTests {

        @Test
        @DisplayName("should handle pause when no active execution state")
        void shouldHandleNoActiveState() {
            assertDoesNotThrow(() -> experimentExecutorService.pauseExperiment(999L));
        }

        @Test
        @DisplayName("should set paused flag when execution state exists")
        void shouldSetPausedFlag() throws Exception {
            ExperimentExecutionState state = new ExperimentExecutionState(1L);
            state.setTotalRuns(5);
            state.setCompletedRuns(2);
            injectExecutionState(1L, state);

            experimentExecutorService.pauseExperiment(1L);

            assertTrue(state.isPaused());
        }
    }

    @Nested
    @DisplayName("cancelExperiment")
    class CancelExperimentTests {

        @Test
        @DisplayName("should handle cancel when no active execution state")
        void shouldHandleNoActiveState() {
            assertDoesNotThrow(() -> experimentExecutorService.cancelExperiment(999L));
        }

        @Test
        @DisplayName("should set cancelled flag when execution state exists")
        void shouldSetCancelledFlag() throws Exception {
            ExperimentExecutionState state = new ExperimentExecutionState(1L);
            state.setTotalRuns(5);
            state.setCompletedRuns(2);
            injectExecutionState(1L, state);

            experimentExecutorService.cancelExperiment(1L);

            assertTrue(state.isCancelled());
        }
    }

    @Nested
    @DisplayName("getProgress")
    class GetProgressTests {

        @Test
        @DisplayName("should return null when experiment not executing")
        void shouldReturnNullWhenNotExecuting() {
            ExperimentProgress progress = experimentExecutorService.getProgress(999L);

            assertNull(progress);
        }

        @Test
        @DisplayName("should return progress when experiment is executing")
        void shouldReturnProgressWhenExecuting() throws Exception {
            ExperimentExecutionState state = new ExperimentExecutionState(1L);
            state.setTotalRuns(10);
            state.setCompletedRuns(5);
            injectExecutionState(1L, state);

            ExperimentProgress progress = experimentExecutorService.getProgress(1L);

            assertNotNull(progress);
            assertEquals(1L, progress.getExperimentId());
            assertEquals(5, progress.getCompletedRuns());
            assertEquals(10, progress.getTotalRuns());
            assertEquals(50.0, progress.getProgressPercent());
        }

        @Test
        @DisplayName("should return zero percent when total runs is zero")
        void shouldReturnZeroPercentWhenNoRuns() throws Exception {
            ExperimentExecutionState state = new ExperimentExecutionState(1L);
            state.setTotalRuns(0);
            state.setCompletedRuns(0);
            injectExecutionState(1L, state);

            ExperimentProgress progress = experimentExecutorService.getProgress(1L);

            assertNotNull(progress);
            assertEquals(0.0, progress.getProgressPercent());
        }
    }

    @Nested
    @DisplayName("isExecuting")
    class IsExecutingTests {

        @Test
        @DisplayName("should return false when experiment not executing")
        void shouldReturnFalseWhenNotExecuting() {
            assertFalse(experimentExecutorService.isExecuting(999L));
        }

        @Test
        @DisplayName("should return true when experiment is executing")
        void shouldReturnTrueWhenExecuting() throws Exception {
            ExperimentExecutionState state = new ExperimentExecutionState(1L);
            injectExecutionState(1L, state);

            assertTrue(experimentExecutorService.isExecuting(1L));
        }
    }

    @Nested
    @DisplayName("executeSingleRun")
    class ExecuteSingleRunTests {

        @Test
        @DisplayName("should execute run successfully")
        void shouldExecuteRunSuccessfully() throws Exception {
            Experiment experiment = createExperimentWithTemplate(1L, ExperimentStatus.RUNNING);
            ExperimentConfig config = createBasicConfig();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test response")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .tokensPerSecond(45.5)
                            .timeToFirstTokenMs(100L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            verify(experimentRunRepository, times(2)).save(any(ExperimentRun.class));
            verify(ollamaService).generate(any(GenerationRequest.class));
        }

        @Test
        @DisplayName("should handle generation failure")
        void shouldHandleGenerationFailure() throws Exception {
            Experiment experiment = createExperimentWithTemplate(1L, ExperimentStatus.RUNNING);
            ExperimentConfig config = createBasicConfig();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenThrow(new RuntimeException("Generation failed"));

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            verify(experimentRunRepository, times(2)).save(any(ExperimentRun.class));
        }

        @Test
        @DisplayName("should execute run with RAG context")
        void shouldExecuteRunWithRagContext() throws Exception {
            Experiment experiment = createExperimentWithTemplate(1L, ExperimentStatus.RUNNING);
            ExperimentConfig config = createRagConfig();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel("nomic-embed-text")
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            when(ragService.buildCollectionName(1L, "nomic-embed-text"))
                    .thenReturn("doc-1-nomic-embed-text");

            List<RetrievedChunk> chunks = new ArrayList<>();
            chunks.add(RetrievedChunk.builder().content("Test chunk").build());
            when(ragService.query(anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(chunks);
            when(ragService.buildContext(chunks)).thenReturn("Context:\n\n[1] Test chunk\n\n");

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test response with RAG")
                            .model("llama3:8b")
                            .durationMs(1500L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            verify(ragService)
                    .query(
                            eq("doc-1-nomic-embed-text"),
                            anyString(),
                            eq("nomic-embed-text"),
                            eq(5));
            verify(ragService).buildContext(chunks);
        }

        @Test
        @DisplayName("should execute run with system prompt")
        void shouldExecuteRunWithSystemPrompt() throws Exception {
            Experiment experiment = createExperimentWithTemplate(1L, ExperimentStatus.RUNNING);
            ExperimentConfig config = createConfigWithSystemPrompt();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            SystemPrompt systemPrompt =
                    SystemPrompt.builder().id(1L).content("You are a helpful assistant.").build();
            when(systemPromptRepository.findById(1L)).thenReturn(Optional.of(systemPrompt));

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test response")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            verify(systemPromptRepository).findById(1L);

            ArgumentCaptor<GenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(GenerationRequest.class);
            verify(ollamaService).generate(requestCaptor.capture());
            assertEquals(
                    "You are a helpful assistant.", requestCaptor.getValue().getSystemPrompt());
        }

        @Test
        @DisplayName("should handle missing system prompt gracefully")
        void shouldHandleMissingSystemPrompt() throws Exception {
            Experiment experiment = createExperimentWithTemplate(1L, ExperimentStatus.RUNNING);
            ExperimentConfig config = createConfigWithSystemPrompt();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);
            when(systemPromptRepository.findById(1L)).thenReturn(Optional.empty());

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test response")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            ArgumentCaptor<GenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(GenerationRequest.class);
            verify(ollamaService).generate(requestCaptor.capture());
            assertNull(requestCaptor.getValue().getSystemPrompt());
        }

        @Test
        @DisplayName("should substitute variable values in prompt")
        void shouldSubstituteVariableValues() throws Exception {
            TaskTemplate template =
                    TaskTemplate.builder()
                            .id(1L)
                            .name("Test Template")
                            .promptTemplate("Review this {{language}} code: {{code}}")
                            .build();
            Experiment experiment = createExperimentWithCustomTemplate(1L, template);
            ExperimentConfig config = createConfigWithVariables();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Code looks good")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            ArgumentCaptor<GenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(GenerationRequest.class);
            verify(ollamaService).generate(requestCaptor.capture());
            String prompt = requestCaptor.getValue().getPrompt();
            assertTrue(prompt.contains("JavaScript"));
            assertTrue(prompt.contains("function test() {}"));
        }

        @Test
        @DisplayName("should execute run with null hyperparameters")
        void shouldExecuteRunWithNullHyperparameters() throws Exception {
            Experiment experiment = createExperimentWithTemplate(1L, ExperimentStatus.RUNNING);
            ExperimentConfig config = createConfigWithNullHyperparameters();
            ExperimentRunConfig runConfig =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test response")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            ExperimentRun result =
                    experimentExecutorService.executeSingleRun(experiment, runConfig, config);

            assertNotNull(result);
            ArgumentCaptor<GenerationRequest> requestCaptor =
                    ArgumentCaptor.forClass(GenerationRequest.class);
            verify(ollamaService).generate(requestCaptor.capture());
            GenerationRequest captured = requestCaptor.getValue();
            assertNull(captured.getTemperature());
            assertNull(captured.getTopP());
            assertNull(captured.getTopK());
            assertNull(captured.getContextWindow());
            assertNull(captured.getMaxTokens());
        }
    }

    @Nested
    @DisplayName("executeRuns - control flow")
    class ExecuteRunsControlFlowTests {

        @Test
        @DisplayName("should pause execution when pause flag is set")
        void shouldPauseExecution() throws Exception {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            String configJson = objectMapper.writeValueAsString(createConfigWithMultipleModels());
            experiment.setConfig(configJson);
            experiment.setTaskTemplate(createTaskTemplate());
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
            when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenAnswer(
                            invocation -> {
                                experimentExecutorService.pauseExperiment(1L);
                                return response;
                            });

            experimentExecutorService.startExperiment(1L);

            Thread.sleep(1000);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository, atLeast(2)).save(captor.capture());

            List<Experiment> savedExperiments = captor.getAllValues();
            boolean hasPaused =
                    savedExperiments.stream()
                            .anyMatch(exp -> exp.getStatus() == ExperimentStatus.PAUSED);
            assertTrue(hasPaused);
        }

        @Test
        @DisplayName("should cancel execution when cancel flag is set")
        void shouldCancelExecution() throws Exception {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            String configJson = objectMapper.writeValueAsString(createConfigWithMultipleModels());
            experiment.setConfig(configJson);
            experiment.setTaskTemplate(createTaskTemplate());
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
            when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenAnswer(
                            invocation -> {
                                experimentExecutorService.cancelExperiment(1L);
                                return response;
                            });

            experimentExecutorService.startExperiment(1L);

            Thread.sleep(1000);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository, atLeast(2)).save(captor.capture());

            List<Experiment> savedExperiments = captor.getAllValues();
            boolean hasFailed =
                    savedExperiments.stream()
                            .anyMatch(exp -> exp.getStatus() == ExperimentStatus.FAILED);
            assertTrue(hasFailed);
        }
    }

    @Nested
    @DisplayName("updateExperimentStatus")
    class UpdateExperimentStatusTests {

        @Test
        @DisplayName("should update experiment status")
        void shouldUpdateStatus() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
            when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);

            experimentExecutorService.updateExperimentStatus(1L, ExperimentStatus.RUNNING);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository).save(captor.capture());
            assertEquals(ExperimentStatus.RUNNING, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should throw when experiment not found")
        void shouldThrowWhenNotFound() {
            when(experimentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            experimentExecutorService.updateExperimentStatus(
                                    1L, ExperimentStatus.RUNNING));
        }
    }

    @Nested
    @DisplayName("Run Configuration Generation")
    class RunConfigurationGenerationTests {

        @Test
        @DisplayName("should generate correct number of run configurations")
        void shouldGenerateCorrectRunConfigs() throws Exception {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            String configJson = objectMapper.writeValueAsString(createConfigWithMultipleModels());
            experiment.setConfig(configJson);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            experimentExecutorService.startExperiment(1L);

            Thread.sleep(500);

            verify(experimentRunRepository, atLeast(1)).save(any(ExperimentRun.class));
        }
    }

    @Nested
    @DisplayName("WebSocket Message Publishing")
    class WebSocketMessagePublishingTests {

        @Test
        @DisplayName("should publish messages to correct destination")
        void shouldPublishToCorrectDestination() throws Exception {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            String configJson = objectMapper.writeValueAsString(createBasicConfig());
            experiment.setConfig(configJson);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            ExperimentRun savedRun = createExperimentRun(1L, experiment);
            when(experimentRunRepository.save(any(ExperimentRun.class))).thenReturn(savedRun);

            GenerationResponse response =
                    GenerationResponse.builder()
                            .response("Test")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();
            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(response);

            experimentExecutorService.startExperiment(1L);

            Thread.sleep(500);

            verify(webSocketHandler, atLeastOnce())
                    .broadcastExperimentCompleted(
                            eq(1L),
                            any(ExperimentStatus.class),
                            anyInt(),
                            anyInt(),
                            anyInt(),
                            any());
        }
    }

    @Nested
    @DisplayName("Config Parsing")
    class ConfigParsingTests {

        @Test
        @DisplayName("should throw IllegalArgumentException for null config")
        void shouldThrowForNullConfig() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            experiment.setConfig(null);
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> experimentExecutorService.startExperiment(1L));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for blank config")
        void shouldThrowForBlankConfig() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            experiment.setConfig("   ");
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> experimentExecutorService.startExperiment(1L));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for invalid JSON config")
        void shouldThrowForInvalidJson() {
            Experiment experiment = createExperiment(1L, ExperimentStatus.DRAFT);
            experiment.setConfig("not valid json");
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> experimentExecutorService.startExperiment(1L));
        }
    }

    @Nested
    @DisplayName("ExperimentRunConfig")
    class ExperimentRunConfigTests {

        @Test
        @DisplayName("should build run config correctly")
        void shouldBuildRunConfig() {
            ExperimentRunConfig config =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel("nomic-embed-text")
                            .iteration(3)
                            .build();

            assertEquals("llama3:8b", config.getModel());
            assertEquals("nomic-embed-text", config.getEmbeddingModel());
            assertEquals(3, config.getIteration());
        }

        @Test
        @DisplayName("should allow null embedding model for non-RAG runs")
        void shouldAllowNullEmbeddingModel() {
            ExperimentRunConfig config =
                    ExperimentRunConfig.builder()
                            .model("llama3:8b")
                            .embeddingModel(null)
                            .iteration(1)
                            .build();

            assertEquals("llama3:8b", config.getModel());
            assertNull(config.getEmbeddingModel());
        }
    }

    private Experiment createExperiment(Long id, ExperimentStatus status) {
        return Experiment.builder().id(id).name("Test Experiment").status(status).build();
    }

    private Experiment createExperimentWithTemplate(Long id, ExperimentStatus status) {
        TaskTemplate template =
                TaskTemplate.builder()
                        .id(1L)
                        .name("Test Template")
                        .promptTemplate("{{prompt}}")
                        .build();
        return Experiment.builder()
                .id(id)
                .name("Test Experiment")
                .status(status)
                .taskTemplate(template)
                .build();
    }

    private Experiment createExperimentWithCustomTemplate(Long id, TaskTemplate template) {
        return Experiment.builder()
                .id(id)
                .name("Test Experiment")
                .status(ExperimentStatus.RUNNING)
                .taskTemplate(template)
                .build();
    }

    private ExperimentRun createExperimentRun(Long id, Experiment experiment) {
        return ExperimentRun.builder()
                .id(id)
                .experiment(experiment)
                .modelName("llama3:8b")
                .iteration(1)
                .status(RunStatus.PENDING)
                .build();
    }

    private ExperimentConfig createBasicConfig() {
        return ExperimentConfig.builder()
                .models(List.of("llama3:8b"))
                .iterations(1)
                .contextMode("NONE")
                .hyperparameters(Hyperparameters.builder().temperature(0.7).build())
                .build();
    }

    private ExperimentConfig createRagConfig() {
        return ExperimentConfig.builder()
                .models(List.of("llama3:8b"))
                .embeddingModels(List.of("nomic-embed-text"))
                .iterations(1)
                .contextMode("RAG")
                .documentId(1L)
                .hyperparameters(Hyperparameters.builder().temperature(0.7).build())
                .build();
    }

    private ExperimentConfig createConfigWithSystemPrompt() {
        return ExperimentConfig.builder()
                .models(List.of("llama3:8b"))
                .iterations(1)
                .contextMode("NONE")
                .systemPromptId(1L)
                .hyperparameters(Hyperparameters.builder().temperature(0.7).build())
                .build();
    }

    private ExperimentConfig createConfigWithMultipleModels() {
        return ExperimentConfig.builder()
                .models(List.of("llama3:8b", "qwen2.5-coder:7b"))
                .iterations(2)
                .contextMode("NONE")
                .hyperparameters(Hyperparameters.builder().temperature(0.7).build())
                .build();
    }

    private ExperimentConfig createConfigWithVariables() {
        return ExperimentConfig.builder()
                .models(List.of("llama3:8b"))
                .iterations(1)
                .contextMode("NONE")
                .hyperparameters(Hyperparameters.builder().temperature(0.7).build())
                .variableValues(
                        Map.of(
                                "language", "JavaScript",
                                "code", "function test() {}"))
                .build();
    }

    private ExperimentConfig createConfigWithNullHyperparameters() {
        return ExperimentConfig.builder()
                .models(List.of("llama3:8b"))
                .iterations(1)
                .contextMode("NONE")
                .hyperparameters(null)
                .build();
    }

    private TaskTemplate createTaskTemplate() {
        return TaskTemplate.builder()
                .id(1L)
                .name("Test Template")
                .promptTemplate("{{prompt}}")
                .build();
    }
}
