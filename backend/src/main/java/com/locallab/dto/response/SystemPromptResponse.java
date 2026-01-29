package com.locallab.dto.response;

import java.time.LocalDateTime;

import com.locallab.model.SystemPrompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SystemPrompt entities.
 *
 * <p>This class encapsulates all fields returned when retrieving system prompts. It provides a
 * clean separation between the internal entity representation and the API response format.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "alias": "code-assistant",
 *   "content": "You are an expert code reviewer...",
 *   "createdAt": "2025-11-27T10:00:00"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see SystemPrompt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPromptResponse {

    /** Unique identifier for the system prompt. */
    private Long id;

    /** Short, unique alias for the system prompt. */
    private String alias;

    /** The full content of the system prompt. */
    private String content;

    /** Timestamp when this system prompt was created. */
    private LocalDateTime createdAt;

    /**
     * Creates a SystemPromptResponse from a SystemPrompt entity.
     *
     * <p>This factory method provides a convenient way to convert entity objects to response DTOs.
     *
     * @param systemPrompt the system prompt entity to convert (must not be null)
     * @return a new SystemPromptResponse containing all entity data
     * @throws IllegalArgumentException if systemPrompt is null
     */
    public static SystemPromptResponse fromEntity(SystemPrompt systemPrompt) {
        if (systemPrompt == null) {
            throw new IllegalArgumentException("SystemPrompt must not be null");
        }
        return SystemPromptResponse.builder()
                .id(systemPrompt.getId())
                .alias(systemPrompt.getAlias())
                .content(systemPrompt.getContent())
                .createdAt(systemPrompt.getCreatedAt())
                .build();
    }
}
