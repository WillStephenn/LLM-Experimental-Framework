package com.locallab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing the result of a text generation operation from Ollama.
 *
 * <p>This class encapsulates the generated text along with performance metrics that are useful for
 * benchmarking and analysis. All timing values are measured in milliseconds for consistency.
 *
 * <p>Example response:
 *
 * <pre>{@code
 * GenerationResponse response = GenerationResponse.builder()
 *     .response("Dependency injection is a design pattern...")
 *     .model("qwen2.5-coder:7b")
 *     .durationMs(2500L)
 *     .tokensPerSecond(45.5)
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.dto.request.GenerationRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResponse {

    /** The generated text response from the model. */
    private String response;

    /** The name of the model that generated this response. */
    private String model;

    /** Total duration of the generation in milliseconds. */
    private Long durationMs;

    /** Generation throughput measured in tokens per second. */
    private Double tokensPerSecond;

    /** Time to first token in milliseconds (latency before generation begins). */
    private Long timeToFirstTokenMs;

    /** Number of tokens in the prompt that was sent. */
    private Integer promptTokens;

    /** Number of tokens generated in the response. */
    private Integer completionTokens;
}
