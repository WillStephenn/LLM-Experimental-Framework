package com.locallab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating system prompts.
 *
 * <p>This class encapsulates the data required to create or update a {@code SystemPrompt} entity.
 * It includes validation constraints to ensure data integrity before persistence.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SystemPromptRequest request = SystemPromptRequest.builder()
 *     .alias("code-assistant")
 *     .content("You are a helpful coding assistant...")
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.model.SystemPrompt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemPromptRequest {

    /**
     * Short, unique alias for the system prompt.
     *
     * <p>Used as a human-readable identifier for quick selection. Examples: "code-assistant",
     * "technical-writer", "json-formatter".
     */
    @NotBlank(message = "Alias is required")
    @Size(max = 50, message = "Alias must not exceed 50 characters")
    private String alias;

    /**
     * The full content of the system prompt.
     *
     * <p>This text is sent to the LLM as the system message to guide its behaviour.
     */
    @NotBlank(message = "Content is required")
    @Size(max = 50000, message = "Content must not exceed 50,000 characters")
    private String content;
}
