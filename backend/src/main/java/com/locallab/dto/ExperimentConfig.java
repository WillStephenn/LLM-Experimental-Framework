package com.locallab.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the configuration for an experiment.
 *
 * <p>Contains all settings required to execute an experiment, including model selection, embedding
 * models for RAG, iteration count, context mode, and hyperparameters. This configuration determines
 * the run matrix for the experiment.
 *
 * <p>The total number of experiment runs is calculated as: {@code models.size() ×
 * max(embeddingModels.size(), 1) × iterations}
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ExperimentConfig config = ExperimentConfig.builder()
 *     .models(List.of("qwen2.5-coder:7b", "codellama:7b"))
 *     .embeddingModels(List.of("nomic-embed-text"))
 *     .iterations(3)
 *     .contextMode("RAG")
 *     .documentId(1L)
 *     .hyperparameters(Hyperparameters.builder()
 *         .temperature(0.7)
 *         .topP(0.9)
 *         .build())
 *     .build();
 * }</pre>
 *
 * <h3>Context Modes:</h3>
 *
 * <ul>
 *   <li><strong>NONE:</strong> No additional context; prompt only
 *   <li><strong>RAG:</strong> Retrieval-augmented generation using document chunks
 *   <li><strong>FULL_CONTEXT:</strong> Complete context provided inline
 * </ul>
 *
 * @author William Stephen
 * @see com.locallab.dto.request.ExperimentRequest
 * @see Hyperparameters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentConfig {

    /**
     * List of model names to test in the experiment.
     *
     * <p>Each model will be run with all combinations of embedding models and iterations. At least
     * one model must be specified.
     *
     * <p>Example: {@code ["qwen2.5-coder:7b", "codellama:7b", "deepseek-coder:6.7b"]}
     */
    @NotEmpty(message = "At least one model must be selected")
    private List<String> models;

    /**
     * List of embedding model names for RAG-based experiments.
     *
     * <p>Required when contextMode is RAG. Each embedding model will be tested with all model and
     * iteration combinations. If not provided or empty, non-RAG mode is assumed.
     *
     * <p>Example: {@code ["nomic-embed-text", "mxbai-embed-large"]}
     */
    private List<String> embeddingModels;

    /**
     * Number of iterations to run for each model/embedding combination.
     *
     * <p>Multiple iterations allow for statistical analysis of model performance variance. Must be
     * at least 1 and at most 100.
     */
    @NotNull(message = "Iterations must be specified")
    @Min(value = 1, message = "Iterations must be at least 1")
    @Max(value = 100, message = "Iterations must not exceed 100")
    private Integer iterations;

    /**
     * Determines how context is provided to the model.
     *
     * <p>Valid values are:
     *
     * <ul>
     *   <li>NONE - No additional context
     *   <li>RAG - Retrieval-augmented generation from a document
     *   <li>FULL_CONTEXT - Complete context provided inline
     * </ul>
     */
    @NotNull(message = "Context mode must be specified")
    private String contextMode;

    /**
     * ID of the document to use for RAG-based experiments.
     *
     * <p>Required when contextMode is RAG. References a document that has been uploaded and
     * processed for vector retrieval.
     */
    private Long documentId;

    /**
     * ID of the system prompt to use for all runs.
     *
     * <p>Optional. If specified, references a saved SystemPrompt that will be used consistently
     * across all experiment runs.
     */
    private Long systemPromptId;

    /**
     * Hyperparameters controlling model generation behaviour.
     *
     * <p>Contains settings such as temperature, top-p, top-k, context window size, and maximum
     * token limits. Validated according to Hyperparameters constraints.
     */
    @NotNull(message = "Hyperparameters must be specified")
    @Valid
    private Hyperparameters hyperparameters;

    /**
     * Variable values to substitute into the task template prompt.
     *
     * <p>Keys correspond to variable placeholders in the format {@code {{variableName}}} within the
     * task template's prompt. Values are substituted at execution time.
     *
     * <p>Example: {@code {"code": "function example() { ... }", "language": "JavaScript"}}
     */
    private Map<String, String> variableValues;
}
