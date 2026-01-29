package com.locallab.dto.response;

import java.time.LocalDateTime;

import com.locallab.model.TaskTemplate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for TaskTemplate entities.
 *
 * <p>This class encapsulates all fields returned when retrieving task templates. It provides a
 * clean separation between the internal entity representation and the API response format.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "name": "Code Review Task",
 *   "description": "Review code for best practices",
 *   "promptTemplate": "Review the following code:\n\n{{code}}\n\nProvide feedback...",
 *   "tags": "code,review,quality",
 *   "evaluationNotes": "Look for correctness, style, and performance",
 *   "createdAt": "2025-11-27T10:00:00"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see TaskTemplate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateResponse {

    /** Unique identifier for the task template. */
    private Long id;

    /** Short, descriptive name for the task template. */
    private String name;

    /** Detailed description of what this task evaluates or accomplishes. */
    private String description;

    /** The main prompt template with optional variable placeholders. */
    private String promptTemplate;

    /** Comma-separated list of tags for categorisation and filtering. */
    private String tags;

    /** Optional notes describing evaluation criteria or assessment guidelines. */
    private String evaluationNotes;

    /** Timestamp when this task template was created. */
    private LocalDateTime createdAt;

    /**
     * Creates a TaskTemplateResponse from a TaskTemplate entity.
     *
     * <p>This factory method provides a convenient way to convert entity objects to response DTOs.
     *
     * @param taskTemplate the task template entity to convert (must not be null)
     * @return a new TaskTemplateResponse containing all entity data
     * @throws IllegalArgumentException if taskTemplate is null
     */
    public static TaskTemplateResponse fromEntity(TaskTemplate taskTemplate) {
        if (taskTemplate == null) {
            throw new IllegalArgumentException("TaskTemplate must not be null");
        }
        return TaskTemplateResponse.builder()
                .id(taskTemplate.getId())
                .name(taskTemplate.getName())
                .description(taskTemplate.getDescription())
                .promptTemplate(taskTemplate.getPromptTemplate())
                .tags(taskTemplate.getTags())
                .evaluationNotes(taskTemplate.getEvaluationNotes())
                .createdAt(taskTemplate.getCreatedAt())
                .build();
    }
}
