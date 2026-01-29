package com.locallab.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.locallab.model.TaskTemplate;

/**
 * Unit tests for {@link TaskTemplateResponse}.
 *
 * <p>Tests the factory method for converting TaskTemplate entities to response DTOs.
 *
 * @see TaskTemplateResponse
 */
@DisplayName("TaskTemplateResponse")
class TaskTemplateResponseTest {

    @Nested
    @DisplayName("fromEntity")
    class FromEntityTests {

        @Test
        @DisplayName("Should create response from entity with all fields")
        void shouldCreateResponseFromEntityWithAllFields() {
            LocalDateTime createdAt = LocalDateTime.of(2025, 11, 27, 10, 0);
            TaskTemplate entity =
                    TaskTemplate.builder()
                            .id(1L)
                            .name("Code Review Task")
                            .description("Review code for best practices")
                            .promptTemplate("Review the following code:\n\n{{code}}")
                            .tags("code,review,quality")
                            .evaluationNotes("Look for correctness and style")
                            .createdAt(createdAt)
                            .build();

            TaskTemplateResponse response = TaskTemplateResponse.fromEntity(entity);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("Code Review Task", response.getName());
            assertEquals("Review code for best practices", response.getDescription());
            assertEquals("Review the following code:\n\n{{code}}", response.getPromptTemplate());
            assertEquals("code,review,quality", response.getTags());
            assertEquals("Look for correctness and style", response.getEvaluationNotes());
            assertEquals(createdAt, response.getCreatedAt());
        }

        @Test
        @DisplayName("Should create response from entity with minimal fields")
        void shouldCreateResponseFromEntityWithMinimalFields() {
            TaskTemplate entity =
                    TaskTemplate.builder()
                            .id(2L)
                            .name("Minimal Task")
                            .promptTemplate("Simple prompt")
                            .build();

            TaskTemplateResponse response = TaskTemplateResponse.fromEntity(entity);

            assertNotNull(response);
            assertEquals(2L, response.getId());
            assertEquals("Minimal Task", response.getName());
            assertEquals("Simple prompt", response.getPromptTemplate());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when entity is null")
        void shouldThrowIllegalArgumentExceptionWhenEntityIsNull() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> TaskTemplateResponse.fromEntity(null));

            assertEquals("TaskTemplate must not be null", exception.getMessage());
        }
    }
}
