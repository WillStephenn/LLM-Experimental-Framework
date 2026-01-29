package com.locallab.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.dto.ExperimentConfig;
import com.locallab.dto.ExperimentExecutionState;
import com.locallab.dto.ExperimentProgress;
import com.locallab.dto.RetrievedChunk;
import com.locallab.dto.WebSocketMessage;
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

import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Service for executing experiments asynchronously.
 *
 * <p>This service manages the execution lifecycle of experiments, including starting, pausing,
 * cancelling, and resuming experiment runs. It coordinates between multiple services to generate
 * model responses and publish progress updates via WebSocket.
 *
 * <h3>Execution Flow:</h3>
 *
 * <ol>
 *   <li>Generate run configurations from experiment config (models x embeddings x iterations)
 *   <li>Execute each run sequentially (to respect local resource constraints)
 *   <li>Persist results immediately after each run
 *   <li>Publish progress updates via WebSocket
 *   <li>Update experiment status upon completion
 * </ol>
 *
 * <h3>Control Operations:</h3>
 *
 * <ul>
 *   <li><strong>Pause:</strong> Stops execution after current run completes
 *   <li><strong>Cancel:</strong> Aborts execution and marks experiment as FAILED
 *   <li><strong>Resume:</strong> Continues from where execution was paused
 * </ul>
 *
 * <h3>WebSocket Messages:</h3>
 *
 * <p>Progress updates are published to: {@code /topic/experiments/{id}/progress}
 *
 * @author William Stephen
 * @see ExperimentExecutionState
 * @see ExperimentProgress
 * @see WebSocketMessage
 */
@Service
@RequiredArgsConstructor
public class ExperimentExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentExecutorService.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final OllamaService ollamaService;
    private final RagService ragService;
    private final TaskService taskService;
    private final SystemPromptRepository systemPromptRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /** Tracks execution state for running experiments. Thread-safe for concurrent access. */
    private final Map<Long, ExperimentExecutionState> executionStates = new ConcurrentHashMap<>();

    /**
     * Starts experiment execution asynchronously.
     *
     * <p>This method validates the experiment status, generates run configurations, and begins
     * sequential execution of all runs. Progress updates are published via WebSocket.
     *
     * @param experimentId the ID of the experiment to start
     * @throws EntityNotFoundException if the experiment is not found
     * @throws IllegalStateException if the experiment is not in DRAFT status
     */
    @Async
    public void startExperiment(Long experimentId) {
        LOGGER.info("Starting experiment: {}", experimentId);

        Experiment experiment = findExperimentById(experimentId);

        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            throw new IllegalStateException(
                    "Experiment must be in DRAFT status to start, current status: "
                            + experiment.getStatus());
        }

        updateExperimentStatus(experimentId, ExperimentStatus.RUNNING);

        ExperimentConfig config = parseConfig(experiment.getConfig());
        List<ExperimentRunConfig> runConfigs = generateRunConfigurations(config);

        ExperimentExecutionState state = new ExperimentExecutionState(experimentId);
        state.setTotalRuns(runConfigs.size());
        executionStates.put(experimentId, state);

        LOGGER.info(
                "Generated {} run configurations for experiment {}",
                runConfigs.size(),
                experimentId);

        executeRuns(experiment, runConfigs, state, config);
    }

    /**
     * Pauses experiment execution after the current run completes.
     *
     * <p>Sets the pause flag on the execution state. The executor will check this flag after each
     * run and stop if set. The experiment status will be updated to PAUSED.
     *
     * @param experimentId the ID of the experiment to pause
     */
    public void pauseExperiment(Long experimentId) {
        LOGGER.info("Pausing experiment: {}", experimentId);

        ExperimentExecutionState state = executionStates.get(experimentId);
        if (state != null) {
            state.setPaused(true);
            LOGGER.info("Pause flag set for experiment: {}", experimentId);
        } else {
            LOGGER.warn("No active execution state found for experiment: {}", experimentId);
        }
    }

    /**
     * Cancels experiment execution after the current run completes.
     *
     * <p>Sets the cancel flag on the execution state. The executor will check this flag after each
     * run and stop if set. The experiment status will be updated to FAILED.
     *
     * @param experimentId the ID of the experiment to cancel
     */
    public void cancelExperiment(Long experimentId) {
        LOGGER.info("Cancelling experiment: {}", experimentId);

        ExperimentExecutionState state = executionStates.get(experimentId);
        if (state != null) {
            state.setCancelled(true);
            LOGGER.info("Cancel flag set for experiment: {}", experimentId);
        } else {
            LOGGER.warn("No active execution state found for experiment: {}", experimentId);
        }
    }

    /**
     * Resumes a paused experiment from where it left off.
     *
     * <p>Validates that the experiment is in PAUSED status and restarts execution. Runs that have
     * already completed will not be re-executed.
     *
     * @param experimentId the ID of the experiment to resume
     * @throws EntityNotFoundException if the experiment is not found
     * @throws IllegalStateException if the experiment is not in PAUSED status
     */
    @Async
    public void resumeExperiment(Long experimentId) {
        LOGGER.info("Resuming experiment: {}", experimentId);

        Experiment experiment = findExperimentById(experimentId);

        if (experiment.getStatus() != ExperimentStatus.PAUSED) {
            throw new IllegalStateException(
                    "Experiment must be in PAUSED status to resume, current status: "
                            + experiment.getStatus());
        }

        updateExperimentStatus(experimentId, ExperimentStatus.RUNNING);

        ExperimentConfig config = parseConfig(experiment.getConfig());
        List<ExperimentRunConfig> runConfigs = generateRunConfigurations(config);

        List<ExperimentRun> completedRuns =
                experimentRunRepository.findByExperimentIdOrderByIterationAsc(experimentId);
        int completedCount = completedRuns.size();

        List<ExperimentRunConfig> remainingConfigs =
                runConfigs.subList(completedCount, runConfigs.size());

        ExperimentExecutionState state = new ExperimentExecutionState(experimentId);
        state.setTotalRuns(runConfigs.size());
        state.setCompletedRuns(completedCount);
        executionStates.put(experimentId, state);

        LOGGER.info(
                "Resuming experiment {} with {} remaining runs out of {} total",
                experimentId,
                remainingConfigs.size(),
                runConfigs.size());

        if (remainingConfigs.isEmpty()) {
            LOGGER.info("No remaining runs for experiment {}, marking as completed", experimentId);
            updateExperimentStatus(experimentId, ExperimentStatus.COMPLETED);
            publishMessage(experimentId, "EXPERIMENT_COMPLETED", null);
            executionStates.remove(experimentId);
        } else {
            executeRuns(experiment, remainingConfigs, state, config);
        }
    }

    /**
     * Gets the current progress of an executing experiment.
     *
     * @param experimentId the ID of the experiment
     * @return the experiment progress, or null if not currently executing
     */
    public ExperimentProgress getProgress(Long experimentId) {
        ExperimentExecutionState state = executionStates.get(experimentId);
        if (state == null) {
            return null;
        }

        double progressPercent =
                state.getTotalRuns() > 0
                        ? (double) state.getCompletedRuns() / state.getTotalRuns() * 100
                        : 0;

        return ExperimentProgress.builder()
                .experimentId(experimentId)
                .completedRuns(state.getCompletedRuns())
                .totalRuns(state.getTotalRuns())
                .progressPercent(progressPercent)
                .build();
    }

    /**
     * Checks if an experiment is currently executing.
     *
     * @param experimentId the ID of the experiment
     * @return true if the experiment is currently executing, false otherwise
     */
    public boolean isExecuting(Long experimentId) {
        return executionStates.containsKey(experimentId);
    }

    /**
     * Generates run configurations from experiment config.
     *
     * <p>Creates the run matrix: models x embeddings x iterations. If no embedding models are
     * specified, a single null embedding is used (non-RAG mode).
     *
     * @param config the experiment configuration
     * @return list of run configurations to execute
     */
    private List<ExperimentRunConfig> generateRunConfigurations(ExperimentConfig config) {
        List<ExperimentRunConfig> runConfigs = new ArrayList<>();

        List<String> models = config.getModels();
        List<String> embeddings = config.getEmbeddingModels();
        if (embeddings == null || embeddings.isEmpty()) {
            embeddings = new ArrayList<>();
            embeddings.add(null);
        }
        int iterations = config.getIterations() != null ? config.getIterations() : 1;

        for (String model : models) {
            for (String embedding : embeddings) {
                for (int i = 1; i <= iterations; i++) {
                    runConfigs.add(
                            ExperimentRunConfig.builder()
                                    .model(model)
                                    .embeddingModel(embedding)
                                    .iteration(i)
                                    .build());
                }
            }
        }

        return runConfigs;
    }

    /**
     * Executes all runs sequentially.
     *
     * @param experiment the experiment being executed
     * @param runConfigs the run configurations to execute
     * @param state the execution state for tracking progress and control
     * @param config the experiment configuration
     */
    private void executeRuns(
            Experiment experiment,
            List<ExperimentRunConfig> runConfigs,
            ExperimentExecutionState state,
            ExperimentConfig config) {

        for (ExperimentRunConfig runConfig : runConfigs) {
            if (state.isPaused()) {
                LOGGER.info("Experiment {} paused", experiment.getId());
                updateExperimentStatus(experiment.getId(), ExperimentStatus.PAUSED);
                publishMessage(experiment.getId(), "EXPERIMENT_PAUSED", null);
                return;
            }

            if (state.isCancelled()) {
                LOGGER.info("Experiment {} cancelled", experiment.getId());
                updateExperimentStatus(experiment.getId(), ExperimentStatus.FAILED);
                publishMessage(experiment.getId(), "EXPERIMENT_FAILED", "Cancelled by user");
                executionStates.remove(experiment.getId());
                return;
            }

            publishMessage(experiment.getId(), "RUN_STARTED", runConfig);

            ExperimentRun run = executeSingleRun(experiment, runConfig, config);

            state.setCompletedRuns(state.getCompletedRuns() + 1);

            publishMessage(experiment.getId(), "RUN_COMPLETED", run);
            publishProgress(experiment.getId(), state);
        }

        LOGGER.info("Experiment {} completed successfully", experiment.getId());
        updateExperimentStatus(experiment.getId(), ExperimentStatus.COMPLETED);
        publishMessage(experiment.getId(), "EXPERIMENT_COMPLETED", null);
        executionStates.remove(experiment.getId());
    }

    /**
     * Executes a single run and persists the result.
     *
     * @param experiment the experiment
     * @param runConfig the run configuration
     * @param config the experiment configuration
     * @return the completed experiment run
     */
    @Transactional
    protected ExperimentRun executeSingleRun(
            Experiment experiment, ExperimentRunConfig runConfig, ExperimentConfig config) {

        LOGGER.debug(
                "Executing run for experiment {}: model={}, embedding={}, iteration={}",
                experiment.getId(),
                runConfig.getModel(),
                runConfig.getEmbeddingModel(),
                runConfig.getIteration());

        ExperimentRun run = createInitialRun(experiment, runConfig, config);
        run = experimentRunRepository.save(run);

        try {
            executeGeneration(run, experiment, runConfig, config);
        } catch (Exception e) {
            handleRunFailure(run, experiment, runConfig, e);
        }

        return experimentRunRepository.save(run);
    }

    /** Creates the initial ExperimentRun entity with RUNNING status. */
    private ExperimentRun createInitialRun(
            Experiment experiment, ExperimentRunConfig runConfig, ExperimentConfig config) {
        return ExperimentRun.builder()
                .experiment(experiment)
                .modelName(runConfig.getModel())
                .iteration(runConfig.getIteration())
                .status(RunStatus.RUNNING)
                .config(serializeConfig(config))
                .build();
    }

    /** Executes the generation and updates the run with results. */
    private void executeGeneration(
            ExperimentRun run,
            Experiment experiment,
            ExperimentRunConfig runConfig,
            ExperimentConfig config) {

        String prompt = buildPrompt(experiment, runConfig, config);
        GenerationRequest generationRequest = buildGenerationRequest(runConfig, config, prompt);
        applySystemPrompt(run, generationRequest, config);

        GenerationResponse response = ollamaService.generate(generationRequest);
        updateRunWithSuccess(run, response, experiment, runConfig);
    }

    /** Builds a GenerationRequest from config and prompt. */
    private GenerationRequest buildGenerationRequest(
            ExperimentRunConfig runConfig, ExperimentConfig config, String prompt) {
        return GenerationRequest.builder()
                .model(runConfig.getModel())
                .prompt(prompt)
                .temperature(
                        config.getHyperparameters() != null
                                ? config.getHyperparameters().getTemperature()
                                : null)
                .topP(
                        config.getHyperparameters() != null
                                ? config.getHyperparameters().getTopP()
                                : null)
                .topK(
                        config.getHyperparameters() != null
                                ? config.getHyperparameters().getTopK()
                                : null)
                .contextWindow(
                        config.getHyperparameters() != null
                                ? config.getHyperparameters().getContextWindow()
                                : null)
                .maxTokens(
                        config.getHyperparameters() != null
                                ? config.getHyperparameters().getMaxTokens()
                                : null)
                .build();
    }

    /** Applies system prompt to the generation request if configured. */
    private void applySystemPrompt(
            ExperimentRun run, GenerationRequest request, ExperimentConfig config) {
        if (config.getSystemPromptId() == null) {
            return;
        }
        SystemPrompt systemPrompt =
                systemPromptRepository.findById(config.getSystemPromptId()).orElse(null);
        if (systemPrompt != null) {
            request.setSystemPrompt(systemPrompt.getContent());
            run.setSystemPrompt(systemPrompt);
        }
    }

    /** Updates the run with successful generation results. */
    private void updateRunWithSuccess(
            ExperimentRun run,
            GenerationResponse response,
            Experiment experiment,
            ExperimentRunConfig runConfig) {
        run.setStatus(RunStatus.SUCCESS);
        run.setOutput(response.getResponse());
        run.setDurationMs(response.getDurationMs());
        run.setTokensPerSecond(response.getTokensPerSecond());
        run.setTimeToFirstTokenMs(response.getTimeToFirstTokenMs());

        LOGGER.debug(
                "Run completed successfully for experiment {}, model {}, duration {}ms",
                experiment.getId(),
                runConfig.getModel(),
                response.getDurationMs());
    }

    /** Handles run failure by setting error status and message. */
    private void handleRunFailure(
            ExperimentRun run, Experiment experiment, ExperimentRunConfig runConfig, Exception e) {
        LOGGER.error(
                "Run failed for experiment {}, model {}: {}",
                experiment.getId(),
                runConfig.getModel(),
                e.getMessage(),
                e);
        run.setStatus(RunStatus.FAILED);
        run.setErrorMessage(e.getMessage());
    }

    /**
     * Builds the prompt for a run, including RAG context if applicable.
     *
     * @param experiment the experiment
     * @param runConfig the run configuration
     * @param config the experiment configuration
     * @return the complete prompt string
     */
    private String buildPrompt(
            Experiment experiment, ExperimentRunConfig runConfig, ExperimentConfig config) {

        TaskTemplate taskTemplate = experiment.getTaskTemplate();
        String promptTemplate =
                taskTemplate != null ? taskTemplate.getPromptTemplate() : "{{prompt}}";

        String prompt = promptTemplate;
        if (config.getVariableValues() != null) {
            for (Map.Entry<String, String> entry : config.getVariableValues().entrySet()) {
                prompt = prompt.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }

        if ("RAG".equalsIgnoreCase(config.getContextMode())
                && config.getDocumentId() != null
                && runConfig.getEmbeddingModel() != null) {

            String collectionName =
                    ragService.buildCollectionName(
                            config.getDocumentId(), runConfig.getEmbeddingModel());

            List<RetrievedChunk> chunks =
                    ragService.query(collectionName, prompt, runConfig.getEmbeddingModel(), 5);

            String context = ragService.buildContext(chunks);
            prompt = context + "\n\n" + prompt;
        }

        return prompt;
    }

    /**
     * Publishes a WebSocket message.
     *
     * @param experimentId the experiment ID
     * @param type the message type
     * @param payload the message payload
     */
    private void publishMessage(Long experimentId, String type, Object payload) {
        WebSocketMessage message =
                WebSocketMessage.builder()
                        .type(type)
                        .payload(payload)
                        .timestamp(Instant.now())
                        .build();

        String destination = "/topic/experiments/" + experimentId + "/progress";

        try {
            messagingTemplate.convertAndSend(destination, message);
            LOGGER.debug("Published {} message to {}", type, destination);
        } catch (Exception e) {
            LOGGER.error("Failed to publish WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Publishes a progress update message.
     *
     * @param experimentId the experiment ID
     * @param state the execution state
     */
    private void publishProgress(Long experimentId, ExperimentExecutionState state) {
        double progressPercent =
                state.getTotalRuns() > 0
                        ? (double) state.getCompletedRuns() / state.getTotalRuns() * 100
                        : 0;

        ExperimentProgress progressData =
                ExperimentProgress.builder()
                        .experimentId(experimentId)
                        .completedRuns(state.getCompletedRuns())
                        .totalRuns(state.getTotalRuns())
                        .progressPercent(progressPercent)
                        .build();

        publishMessage(experimentId, "PROGRESS", progressData);
    }

    /**
     * Parses the experiment config JSON string into an ExperimentConfig object.
     *
     * @param configJson the JSON configuration string
     * @return the parsed ExperimentConfig
     * @throws IllegalArgumentException if parsing fails
     */
    private ExperimentConfig parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Experiment config is required");
        }

        try {
            return objectMapper.readValue(configJson, ExperimentConfig.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse experiment config: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid experiment config JSON: " + e.getMessage());
        }
    }

    /**
     * Serialises an ExperimentConfig to JSON string.
     *
     * @param config the configuration to serialise
     * @return the JSON string, or null if serialisation fails
     */
    private String serializeConfig(ExperimentConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialise config: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Finds an experiment by ID.
     *
     * @param experimentId the experiment ID
     * @return the experiment
     * @throws EntityNotFoundException if not found
     */
    private Experiment findExperimentById(Long experimentId) {
        return experimentRepository
                .findById(experimentId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    }

    /**
     * Updates the status of an experiment.
     *
     * @param experimentId the experiment ID
     * @param status the new status
     */
    @Transactional
    protected void updateExperimentStatus(Long experimentId, ExperimentStatus status) {
        Experiment experiment = findExperimentById(experimentId);
        experiment.setStatus(status);
        experimentRepository.save(experiment);
        LOGGER.info("Updated experiment {} status to {}", experimentId, status);
    }

    /**
     * Internal DTO for run configuration.
     *
     * <p>Represents a single combination of model, embedding model, and iteration for execution.
     */
    @Data
    @Builder
    public static class ExperimentRunConfig {
        private String model;
        private String embeddingModel;
        private int iteration;
    }
}
