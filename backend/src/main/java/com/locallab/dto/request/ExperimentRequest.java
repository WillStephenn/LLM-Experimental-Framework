package com.locallab.dto.request;

import com.locallab.dto.ExperimentConfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating and updating experiments.
 *
 * <p>This class encapsulates all fields required to create or update an Experiment entity.
 * Validation annotations ensure that incoming requests meet the constraints defined in the API
 * contract.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ExperimentRequest request = ExperimentRequest.builder()
 *     .name("LLM Code Review Comparison")
 *     .taskTemplateId(1L)
 *     .config(ExperimentConfig.builder()
 *         .models(List.of("qwen2.5-coder:7b", "codellama:7b"))
 *         .iterations(3)
 *         .contextMode("NONE")
 *         .hyperparameters(Hyperparameters.builder()
 *             .temperature(0.7)
 *             .build())
 *         .build())
 *     .build();
 * }</pre>
 *
 * <h3>Validation Constraints:</h3>
 *
 * <ul>
 *   <li>name: Required, maximum 200 characters
 *   <li>taskTemplateId: Optional, references existing TaskTemplate if provided
 *   <li>config: Required, contains experiment configuration with nested validation
 * </ul>
 *
 * @author William Stephen
 * @see com.locallab.model.Experiment
 * @see ExperimentConfig
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentRequest {

    /**
     * Name of the experiment.
     *
     * <p>A descriptive name to identify the experiment in listings and reports.
     *
     * <p>Example: "LLM Code Review Comparison"
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * ID of the task template to use for this experiment.
     *
     * <p>Optional. If provided, references an existing TaskTemplate that defines the prompt
     * structure for the experiment. Experiments can be created without a task template for ad-hoc
     * testing.
     */
    private Long taskTemplateId;

    /**
     * Configuration for the experiment.
     *
     * <p>Contains all settings required to execute the experiment, including model selection,
     * iterations, context mode, and hyperparameters. Nested validation is applied to ensure all
     * config constraints are satisfied.
     */
    @NotNull(message = "Configuration is required")
    @Valid
    private ExperimentConfig config;
}
