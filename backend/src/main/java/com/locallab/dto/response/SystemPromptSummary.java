package com.locallab.dto.response;

import com.locallab.model.SystemPrompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO for SystemPrompt entity.
 *
 * <p>Provides a lightweight representation of a system prompt containing only the essential fields
 * needed for display in run responses.
 *
 * @author William Stephen
 * @see SystemPrompt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPromptSummary {

    /** Unique identifier for the system prompt. */
    private Long id;

    /** Short alias for the system prompt. */
    private String alias;

    /**
     * Creates a SystemPromptSummary from a SystemPrompt entity.
     *
     * @param systemPrompt the system prompt entity to convert (may be null)
     * @return a new SystemPromptSummary, or null if the input is null
     */
    public static SystemPromptSummary fromEntity(SystemPrompt systemPrompt) {
        if (systemPrompt == null) {
            return null;
        }
        return SystemPromptSummary.builder()
                .id(systemPrompt.getId())
                .alias(systemPrompt.getAlias())
                .build();
    }
}
