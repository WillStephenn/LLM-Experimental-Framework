package com.locallab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating and updating task templates.
 *
 * <p>This class encapsulates all fields required to create or update a TaskTemplate entity.
 * Validation annotations ensure that incoming requests meet the constraints defined in the API
 * contract.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TaskTemplateRequest request = TaskTemplateRequest.builder()
 *     .name("Code Review Task")
 *     .description("Review code for best practices")
 *     .promptTemplate("Review the following code:\n\n{{code}}\n\nProvide feedback...")
 *     .tags("code,review,quality")
 *     .evaluationNotes("Look for correctness, style, and performance")
 *     .build();
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.model.TaskTemplate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateRequest {

    /**
     * Short, descriptive name for the task template.
     *
     * <p>Used for identification and selection in the UI.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * Detailed description of what this task evaluates or accomplishes.
     *
     * <p>This field is optional.
     */
    @Size(max = 5000, message = "Description must not exceed 5,000 characters")
    private String description;

    /**
     * The main prompt template with optional variable placeholders.
     *
     * <p>Contains the prompt text with {@code {{variable}}} placeholders that are resolved at
     * experiment execution time.
     */
    @NotBlank(message = "Prompt template is required")
    @Size(max = 50000, message = "Prompt template must not exceed 50,000 characters")
    private String promptTemplate;

    /**
     * Comma-separated list of tags for categorisation and filtering.
     *
     * <p>This field is optional. Example: {@code "code,review,quality"}
     */
    @Size(max = 500, message = "Tags must not exceed 500 characters")
    private String tags;

    /**
     * Optional notes describing evaluation criteria or assessment guidelines.
     *
     * <p>Provides human reference notes for how to evaluate model outputs.
     */
    @Size(max = 10000, message = "Evaluation notes must not exceed 10,000 characters")
    private String evaluationNotes;
}
