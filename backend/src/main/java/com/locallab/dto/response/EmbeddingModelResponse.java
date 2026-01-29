package com.locallab.dto.response;

import java.time.LocalDateTime;

import com.locallab.model.EmbeddingModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for EmbeddingModel entities.
 *
 * <p>This class encapsulates all fields returned when retrieving embedding models. It provides a
 * clean separation between the internal entity representation and the API response format.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "name": "Nomic Embed Text",
 *   "ollamaModelName": "nomic-embed-text",
 *   "dimensions": 768,
 *   "createdAt": "2025-11-27T10:00:00"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see EmbeddingModel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingModelResponse {

    /** Unique identifier for the embedding model. */
    private Long id;

    /** Display name for the embedding model. */
    private String name;

    /** The Ollama model name used for API calls. */
    private String ollamaModelName;

    /** The number of dimensions in the embedding vectors produced by this model. */
    private Integer dimensions;

    /** Timestamp when this embedding model configuration was created. */
    private LocalDateTime createdAt;

    /**
     * Creates an EmbeddingModelResponse from an EmbeddingModel entity.
     *
     * <p>This factory method provides a convenient way to convert entity objects to response DTOs.
     *
     * @param embeddingModel the embedding model entity to convert (must not be null)
     * @return a new EmbeddingModelResponse containing all entity data
     * @throws IllegalArgumentException if embeddingModel is null
     */
    public static EmbeddingModelResponse fromEntity(EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            throw new IllegalArgumentException("EmbeddingModel must not be null");
        }
        return EmbeddingModelResponse.builder()
                .id(embeddingModel.getId())
                .name(embeddingModel.getName())
                .ollamaModelName(embeddingModel.getOllamaModelName())
                .dimensions(embeddingModel.getDimensions())
                .createdAt(embeddingModel.getCreatedAt())
                .build();
    }
}
