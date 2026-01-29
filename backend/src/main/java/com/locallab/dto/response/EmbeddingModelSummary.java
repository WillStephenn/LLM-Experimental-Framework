package com.locallab.dto.response;

import com.locallab.model.EmbeddingModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO for EmbeddingModel entity.
 *
 * <p>Provides a lightweight representation of an embedding model containing only the essential
 * fields needed for display in run responses.
 *
 * @author William Stephen
 * @see EmbeddingModel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingModelSummary {

    /** Unique identifier for the embedding model. */
    private Long id;

    /** Display name for the embedding model. */
    private String name;

    /**
     * Creates an EmbeddingModelSummary from an EmbeddingModel entity.
     *
     * @param embeddingModel the embedding model entity to convert (may be null)
     * @return a new EmbeddingModelSummary, or null if the input is null
     */
    public static EmbeddingModelSummary fromEntity(EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            return null;
        }
        return EmbeddingModelSummary.builder()
                .id(embeddingModel.getId())
                .name(embeddingModel.getName())
                .build();
    }
}
