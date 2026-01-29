package com.locallab.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing hyperparameters for model generation.
 *
 * <p>Contains configuration values that control the behaviour of LLM generation, including
 * temperature, top-p, top-k, context window size, and maximum token limits. All fields are optional
 * with sensible defaults applied at execution time.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Hyperparameters params = Hyperparameters.builder()
 *     .temperature(0.7)
 *     .topP(0.9)
 *     .topK(40)
 *     .contextWindow(4096)
 *     .maxTokens(1000)
 *     .build();
 * }</pre>
 *
 * <h3>Validation Constraints:</h3>
 *
 * <ul>
 *   <li>temperature: 0.0 to 2.0 (controls randomness)
 *   <li>topP: 0.0 to 1.0 (nucleus sampling probability)
 *   <li>topK: 1 to 100 (top-k sampling)
 *   <li>contextWindow: 512 to 128,000 (context window size in tokens)
 *   <li>maxTokens: minimum 1 if provided (maximum response length)
 * </ul>
 *
 * @author William Stephen
 * @see com.locallab.dto.ExperimentConfig
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hyperparameters {

    /**
     * Controls randomness in generation.
     *
     * <p>Higher values (e.g., 1.5) make output more creative and diverse, whilst lower values
     * (e.g., 0.2) make output more focused and deterministic. Default is typically 0.7.
     */
    @DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @DecimalMax(value = "2.0", message = "Temperature must not exceed 2.0")
    private Double temperature;

    /**
     * Nucleus sampling probability threshold.
     *
     * <p>Considers only tokens whose cumulative probability exceeds this value. A value of 0.9
     * means the model considers the smallest set of tokens whose cumulative probability is at least
     * 90%.
     */
    @DecimalMin(value = "0.0", message = "Top P must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Top P must not exceed 1.0")
    private Double topP;

    /**
     * Top-k sampling parameter.
     *
     * <p>Limits the number of highest-probability tokens to consider for each step. A value of 40
     * means only the top 40 most likely tokens are considered.
     */
    @Min(value = 1, message = "Top K must be at least 1")
    @Max(value = 100, message = "Top K must not exceed 100")
    private Integer topK;

    /**
     * Context window size in tokens.
     *
     * <p>Determines the maximum number of tokens the model can process in a single request,
     * including both the prompt and the generated response.
     */
    @Min(value = 512, message = "Context window must be at least 512")
    @Max(value = 128000, message = "Context window must not exceed 128,000")
    private Integer contextWindow;

    /**
     * Maximum number of tokens to generate.
     *
     * <p>Caps the length of the model's response. If not specified, the model will generate until
     * it reaches a natural stopping point or hits the context window limit.
     */
    @Min(value = 1, message = "Max tokens must be at least 1")
    private Integer maxTokens;
}
