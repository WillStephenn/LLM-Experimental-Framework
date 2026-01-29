package com.locallab.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating embedding model configurations.
 *
 * <p>This class encapsulates all fields required to create an embedding model configuration.
 * Validation annotations ensure that incoming requests meet the constraints defined in the API
 * contract.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EmbeddingModelRequest request = EmbeddingModelRequest.builder()
 *     .name("Nomic Embed Text")
 *     .ollamaModelName("nomic-embed-text")
 *     .dimensions(768)
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.model.EmbeddingModel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingModelRequest {

    /**
     * Display name for the embedding model.
     *
     * <p>Must be unique to prevent duplicate configurations.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * The Ollama model name used for API calls.
     *
     * <p>This is the identifier passed to Ollama's embedding endpoint.
     */
    @NotBlank(message = "Ollama model name is required")
    @Size(max = 100, message = "Ollama model name must not exceed 100 characters")
    private String ollamaModelName;

    /**
     * The number of dimensions in the embedding vectors produced by this model.
     *
     * <p>This must match the configuration in the vector store for compatibility.
     */
    @NotNull(message = "Dimensions is required")
    @Min(value = 1, message = "Dimensions must be at least 1")
    @Max(value = 8192, message = "Dimensions must not exceed 8,192")
    private Integer dimensions;
}
