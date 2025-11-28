package com.locallab.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit tests for {@link SystemPrompt} entity.
 *
 * <p>Tests verify builder pattern, field assignments, validation constraints, and entity
 * construction. Database-level constraints (unique alias) are tested in repository integration
 * tests.
 */
class SystemPromptTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create SystemPrompt with all fields via builder")
        void shouldCreateSystemPromptWithAllFieldsViaBuilder() {
            SystemPrompt systemPrompt =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("code-assistant")
                            .content("You are an expert code reviewer...")
                            .build();

            assertEquals(1L, systemPrompt.getId());
            assertEquals("code-assistant", systemPrompt.getAlias());
            assertEquals("You are an expert code reviewer...", systemPrompt.getContent());
            assertNull(systemPrompt.getCreatedAt());
        }

        @Test
        @DisplayName("Should create SystemPrompt with minimal fields via builder")
        void shouldCreateSystemPromptWithMinimalFieldsViaBuilder() {
            SystemPrompt systemPrompt =
                    SystemPrompt.builder().alias("minimal-prompt").content("Basic content").build();

            assertNull(systemPrompt.getId());
            assertEquals("minimal-prompt", systemPrompt.getAlias());
            assertEquals("Basic content", systemPrompt.getContent());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty SystemPrompt with no-args constructor")
        void shouldCreateEmptySystemPromptWithNoArgsConstructor() {
            SystemPrompt systemPrompt = new SystemPrompt();

            assertNull(systemPrompt.getId());
            assertNull(systemPrompt.getAlias());
            assertNull(systemPrompt.getContent());
            assertNull(systemPrompt.getCreatedAt());
        }

        @Test
        @DisplayName("Should create SystemPrompt with all-args constructor")
        void shouldCreateSystemPromptWithAllArgsConstructor() {
            SystemPrompt systemPrompt = new SystemPrompt(1L, "test-alias", "Test content", null);

            assertEquals(1L, systemPrompt.getId());
            assertEquals("test-alias", systemPrompt.getAlias());
            assertEquals("Test content", systemPrompt.getContent());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void shouldUpdateFieldsViaSetters() {
            SystemPrompt systemPrompt = new SystemPrompt();

            systemPrompt.setId(99L);
            systemPrompt.setAlias("updated-alias");
            systemPrompt.setContent("Updated content");

            assertEquals(99L, systemPrompt.getId());
            assertEquals("updated-alias", systemPrompt.getAlias());
            assertEquals("Updated content", systemPrompt.getContent());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            SystemPrompt prompt1 =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("same-alias")
                            .content("Same content")
                            .build();

            SystemPrompt prompt2 =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("same-alias")
                            .content("Same content")
                            .build();

            assertEquals(prompt1, prompt2);
            assertEquals(prompt1.hashCode(), prompt2.hashCode());
        }

        @Test
        @DisplayName("Should have consistent toString output")
        void shouldHaveConsistentToStringOutput() {
            SystemPrompt systemPrompt =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("test-alias")
                            .content("Test content")
                            .build();

            String toString = systemPrompt.toString();

            assertNotNull(toString);
            // Lombok @Data generates a toString that includes field names and values
            assertEquals(true, toString.contains("alias=test-alias"));
            assertEquals(true, toString.contains("content=Test content"));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid fields")
        void shouldPassValidationWithValidFields() {
            SystemPrompt systemPrompt = createValidSystemPrompt();

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when alias is blank")
        void shouldFailValidationWhenAliasIsBlank() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setAlias("");

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertEquals(1, violations.size());
            ConstraintViolation<SystemPrompt> violation = violations.iterator().next();
            assertEquals("alias", violation.getPropertyPath().toString());
            assertEquals("Alias is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when alias is null")
        void shouldFailValidationWhenAliasIsNull() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setAlias(null);

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertEquals(1, violations.size());
            ConstraintViolation<SystemPrompt> violation = violations.iterator().next();
            assertEquals("alias", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when alias exceeds 100 characters")
        void shouldFailValidationWhenAliasExceedsMaxLength() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setAlias("a".repeat(101));

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertEquals(1, violations.size());
            ConstraintViolation<SystemPrompt> violation = violations.iterator().next();
            assertEquals("alias", violation.getPropertyPath().toString());
            assertEquals("Alias must not exceed 100 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept alias at maximum length boundary")
        void shouldAcceptAliasAtMaximumLengthBoundary() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setAlias("a".repeat(100));

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when content is blank")
        void shouldFailValidationWhenContentIsBlank() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setContent("");

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertEquals(1, violations.size());
            ConstraintViolation<SystemPrompt> violation = violations.iterator().next();
            assertEquals("content", violation.getPropertyPath().toString());
            assertEquals("Content is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when content is null")
        void shouldFailValidationWhenContentIsNull() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setContent(null);

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertEquals(1, violations.size());
            ConstraintViolation<SystemPrompt> violation = violations.iterator().next();
            assertEquals("content", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should accept very long content (LOB field)")
        void shouldAcceptVeryLongContent() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setContent("a".repeat(50000));

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation with multiple invalid fields")
        void shouldFailValidationWithMultipleInvalidFields() {
            SystemPrompt systemPrompt = new SystemPrompt();

            Set<ConstraintViolation<SystemPrompt>> violations = validator.validate(systemPrompt);

            assertEquals(2, violations.size());
        }
    }

    /**
     * Creates a valid SystemPrompt instance for testing.
     *
     * @return a fully populated SystemPrompt with valid field values
     */
    private SystemPrompt createValidSystemPrompt() {
        SystemPrompt systemPrompt = new SystemPrompt();
        systemPrompt.setAlias("code-assistant");
        systemPrompt.setContent("You are an expert code reviewer...");
        return systemPrompt;
    }
}
