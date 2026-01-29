package com.locallab.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing the list of available Ollama models.
 *
 * <p>This class encapsulates the list of model names available in the connected Ollama instance. It
 * is returned by the {@code GET /api/ollama/models} endpoint.
 *
 * <p>Example response:
 *
 * <pre>{@code
 * ModelsResponse response = ModelsResponse.builder()
 *     .models(Arrays.asList("llama3:8b", "qwen2.5-coder:7b"))
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.controller.OllamaController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelsResponse {

    /** The list of available model names in the Ollama instance. */
    private List<String> models;
}
