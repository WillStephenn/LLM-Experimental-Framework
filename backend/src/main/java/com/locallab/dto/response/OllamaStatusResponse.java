package com.locallab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing the status of the Ollama service.
 *
 * <p>This class encapsulates information about the connectivity and availability of the Ollama
 * service, including the number of available models and any relevant status messages.
 *
 * <p>Example response:
 *
 * <pre>{@code
 * OllamaStatusResponse response = OllamaStatusResponse.builder()
 *     .available(true)
 *     .baseUrl("http://localhost:11434")
 *     .modelCount(5)
 *     .message("Ollama is running")
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
public class OllamaStatusResponse {

    /** Indicates whether the Ollama service is available and responding. */
    private Boolean available;

    /** The base URL of the Ollama service. */
    private String baseUrl;

    /** The number of models available in the Ollama instance. */
    private Integer modelCount;

    /** A human-readable status message describing the current state. */
    private String message;
}
