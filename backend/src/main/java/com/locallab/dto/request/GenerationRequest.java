package com.locallab.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for text generation operations via Ollama.
 *
 * <p>This class encapsulates all parameters required to execute a single generation request against
 * an Ollama model. It includes the prompt, model selection, and various hyperparameters that
 * control the generation behaviour.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GenerationRequest request = GenerationRequest.builder()
 *     .model("qwen2.5-coder:7b")
 *     .prompt("Explain the concept of dependency injection")
 *     .temperature(0.7)
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.response.GenerationResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequest {

    /** The name of the Ollama model to use for generation. */
    @NotBlank(message = "Model name is required")
    private String model;

    /** The prompt text to send to the model for generation. */
    @NotBlank(message = "Prompt is required")
    @Size(max = 100000, message = "Prompt must not exceed 100,000 characters")
    private String prompt;

    /** Optional system prompt to set the model's behaviour and context. */
    @Size(max = 50000, message = "System prompt must not exceed 50,000 characters")
    private String systemPrompt;

    /**
     * Controls randomness in generation. Higher values produce more creative outputs. Range: 0.0 to
     * 2.0
     */
    @Min(value = 0, message = "Temperature must be at least 0.0")
    @Max(value = 2, message = "Temperature must not exceed 2.0")
    private Double temperature;

    /**
     * Nucleus sampling parameter. Controls diversity by limiting to top probability mass. Range:
     * 0.0 to 1.0
     */
    @Min(value = 0, message = "Top P must be at least 0.0")
    @Max(value = 1, message = "Top P must not exceed 1.0")
    private Double topP;

    /** Limits generation to the top K most likely tokens. Range: 1 to 100 */
    @Min(value = 1, message = "Top K must be at least 1")
    @Max(value = 100, message = "Top K must not exceed 100")
    private Integer topK;

    /** The maximum context window size in tokens. Range: 512 to 128000 */
    @Min(value = 512, message = "Context window must be at least 512")
    @Max(value = 128000, message = "Context window must not exceed 128,000")
    private Integer contextWindow;

    /** Optional maximum number of tokens to generate in the response. */
    @Min(value = 1, message = "Max tokens must be at least 1")
    private Integer maxTokens;

    /** Whether to stream the response. Defaults to false. */
    @Builder.Default private Boolean stream = false;

    /** Whether to request a structured JSON response. Defaults to false. */
    @Builder.Default private Boolean jsonMode = false;
}
