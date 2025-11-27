package com.locallab.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
 * Unit tests for {@link TaskTemplate} entity.
 *
 * <p>These tests verify the entity's structure, validation constraints, and builder pattern as
 * defined in the specification and API contract. Database-level constraints (e.g., foreign key
 * relationships) are tested in repository integration tests.
 */
class TaskTemplateTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create TaskTemplate with all fields via builder")
        void shouldCreateTaskTemplateWithAllFieldsViaBuilder() {
            TaskTemplate taskTemplate =
                    TaskTemplate.builder()
                            .id(1L)
                            .name("Code Review Task")
                            .description("Review code for best practices")
                            .promptTemplate("Review the following code:\n\n{{code}}")
                            .tags("code,review,quality")
                            .evaluationNotes("Look for correctness and style")
                            .experiments(new ArrayList<>())
                            .build();

            assertEquals(1L, taskTemplate.getId());
            assertEquals("Code Review Task", taskTemplate.getName());
            assertEquals("Review code for best practices", taskTemplate.getDescription());
            assertEquals(
                    "Review the following code:\n\n{{code}}", taskTemplate.getPromptTemplate());
            assertEquals("code,review,quality", taskTemplate.getTags());
            assertEquals("Look for correctness and style", taskTemplate.getEvaluationNotes());
            assertNull(taskTemplate.getCreatedAt());
            assertNotNull(taskTemplate.getExperiments());
            assertTrue(taskTemplate.getExperiments().isEmpty());
        }

        @Test
        @DisplayName("Should create TaskTemplate with minimal required fields via builder")
        void shouldCreateTaskTemplateWithMinimalFieldsViaBuilder() {
            TaskTemplate taskTemplate =
                    TaskTemplate.builder()
                            .name("Simple Task")
                            .promptTemplate("Do something: {{input}}")
                            .build();

            assertNull(taskTemplate.getId());
            assertEquals("Simple Task", taskTemplate.getName());
            assertNull(taskTemplate.getDescription());
            assertEquals("Do something: {{input}}", taskTemplate.getPromptTemplate());
            assertNull(taskTemplate.getTags());
            assertNull(taskTemplate.getEvaluationNotes());
            assertNotNull(taskTemplate.getExperiments());
        }

        @Test
        @DisplayName("Should initialise experiments list by default via builder")
        void shouldInitialiseExperimentsListByDefaultViaBuilder() {
            TaskTemplate taskTemplate =
                    TaskTemplate.builder().name("Test Task").promptTemplate("Test prompt").build();

            assertNotNull(taskTemplate.getExperiments());
            assertTrue(taskTemplate.getExperiments().isEmpty());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty TaskTemplate with no-args constructor")
        void shouldCreateEmptyTaskTemplateWithNoArgsConstructor() {
            TaskTemplate taskTemplate = new TaskTemplate();

            assertNull(taskTemplate.getId());
            assertNull(taskTemplate.getName());
            assertNull(taskTemplate.getDescription());
            assertNull(taskTemplate.getPromptTemplate());
            assertNull(taskTemplate.getTags());
            assertNull(taskTemplate.getEvaluationNotes());
            assertNull(taskTemplate.getCreatedAt());
            // experiments list is initialised by default to prevent null pointer exceptions
            assertNotNull(taskTemplate.getExperiments());
            assertTrue(taskTemplate.getExperiments().isEmpty());
        }

        @Test
        @DisplayName("Should create TaskTemplate with all-args constructor")
        void shouldCreateTaskTemplateWithAllArgsConstructor() {
            ArrayList<Experiment> experiments = new ArrayList<>();
            TaskTemplate taskTemplate =
                    new TaskTemplate(
                            1L,
                            "Test Task",
                            "Description",
                            "Prompt template",
                            "tags",
                            "notes",
                            null,
                            experiments);

            assertEquals(1L, taskTemplate.getId());
            assertEquals("Test Task", taskTemplate.getName());
            assertEquals("Description", taskTemplate.getDescription());
            assertEquals("Prompt template", taskTemplate.getPromptTemplate());
            assertEquals("tags", taskTemplate.getTags());
            assertEquals("notes", taskTemplate.getEvaluationNotes());
            assertEquals(experiments, taskTemplate.getExperiments());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void shouldUpdateFieldsViaSetters() {
            TaskTemplate taskTemplate = new TaskTemplate();

            taskTemplate.setId(99L);
            taskTemplate.setName("Updated Task");
            taskTemplate.setDescription("Updated description");
            taskTemplate.setPromptTemplate("Updated prompt: {{var}}");
            taskTemplate.setTags("updated,tags");
            taskTemplate.setEvaluationNotes("Updated notes");
            taskTemplate.setExperiments(new ArrayList<>());

            assertEquals(99L, taskTemplate.getId());
            assertEquals("Updated Task", taskTemplate.getName());
            assertEquals("Updated description", taskTemplate.getDescription());
            assertEquals("Updated prompt: {{var}}", taskTemplate.getPromptTemplate());
            assertEquals("updated,tags", taskTemplate.getTags());
            assertEquals("Updated notes", taskTemplate.getEvaluationNotes());
            assertNotNull(taskTemplate.getExperiments());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid fields")
        void shouldPassValidationWithValidFields() {
            TaskTemplate taskTemplate = createValidTaskTemplate();

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should pass validation with only required fields")
        void shouldPassValidationWithOnlyRequiredFields() {
            TaskTemplate taskTemplate = new TaskTemplate();
            taskTemplate.setName("Minimal Task");
            taskTemplate.setPromptTemplate("Minimal prompt");

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when name is blank")
        void shouldFailValidationWhenNameIsBlank() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setName("");

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(1, violations.size());
            ConstraintViolation<TaskTemplate> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertEquals("Name is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when name is null")
        void shouldFailValidationWhenNameIsNull() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setName(null);

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(1, violations.size());
            ConstraintViolation<TaskTemplate> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when name exceeds 100 characters")
        void shouldFailValidationWhenNameExceedsMaxLength() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setName("a".repeat(101));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(1, violations.size());
            ConstraintViolation<TaskTemplate> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertEquals("Name must not exceed 100 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept name at maximum length boundary")
        void shouldAcceptNameAtMaximumLengthBoundary() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setName("a".repeat(100));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when promptTemplate is blank")
        void shouldFailValidationWhenPromptTemplateIsBlank() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setPromptTemplate("");

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(1, violations.size());
            ConstraintViolation<TaskTemplate> violation = violations.iterator().next();
            assertEquals("promptTemplate", violation.getPropertyPath().toString());
            assertEquals("Prompt template is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when promptTemplate is null")
        void shouldFailValidationWhenPromptTemplateIsNull() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setPromptTemplate(null);

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(1, violations.size());
            ConstraintViolation<TaskTemplate> violation = violations.iterator().next();
            assertEquals("promptTemplate", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when tags exceeds 500 characters")
        void shouldFailValidationWhenTagsExceedsMaxLength() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setTags("a".repeat(501));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(1, violations.size());
            ConstraintViolation<TaskTemplate> violation = violations.iterator().next();
            assertEquals("tags", violation.getPropertyPath().toString());
            assertEquals("Tags must not exceed 500 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept tags at maximum length boundary")
        void shouldAcceptTagsAtMaximumLengthBoundary() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setTags("a".repeat(500));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null tags")
        void shouldAcceptNullTags() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setTags(null);

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null description")
        void shouldAcceptNullDescription() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setDescription(null);

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null evaluationNotes")
        void shouldAcceptNullEvaluationNotes() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setEvaluationNotes(null);

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation with multiple invalid fields")
        void shouldFailValidationWithMultipleInvalidFields() {
            TaskTemplate taskTemplate = new TaskTemplate();

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertEquals(2, violations.size());
        }

        @Test
        @DisplayName("Should accept very long description (LOB field)")
        void shouldAcceptVeryLongDescription() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setDescription("a".repeat(10000));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long promptTemplate (LOB field)")
        void shouldAcceptVeryLongPromptTemplate() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setPromptTemplate("a".repeat(50000));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long evaluationNotes (LOB field)")
        void shouldAcceptVeryLongEvaluationNotes() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setEvaluationNotes("a".repeat(10000));

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods Tests")
    class LombokTests {

        @Test
        @DisplayName("Should generate equals method correctly")
        void shouldGenerateEqualsMethodCorrectly() {
            TaskTemplate template1 = createValidTaskTemplate();
            template1.setId(1L);

            TaskTemplate template2 = createValidTaskTemplate();
            template2.setId(1L);

            assertEquals(template1, template2);
        }

        @Test
        @DisplayName("Should generate hashCode method correctly")
        void shouldGenerateHashCodeMethodCorrectly() {
            TaskTemplate template1 = createValidTaskTemplate();
            template1.setId(1L);

            TaskTemplate template2 = createValidTaskTemplate();
            template2.setId(1L);

            assertEquals(template1.hashCode(), template2.hashCode());
        }

        @Test
        @DisplayName("Should generate toString method correctly")
        void shouldGenerateToStringMethodCorrectly() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setId(1L);

            String toStringResult = taskTemplate.toString();

            assertNotNull(toStringResult);
            assertTrue(toStringResult.contains("TaskTemplate"));
            assertTrue(toStringResult.contains("id=1"));
            assertTrue(toStringResult.contains("name=Code Review Task"));
        }
    }

    @Nested
    @DisplayName("Variable Placeholder Tests")
    class VariablePlaceholderTests {

        @Test
        @DisplayName("Should accept promptTemplate with variable placeholders")
        void shouldAcceptPromptTemplateWithVariablePlaceholders() {
            TaskTemplate taskTemplate = createValidTaskTemplate();
            taskTemplate.setPromptTemplate(
                    "Review the following {{language}} code:\n\n{{code}}\n\nFocus on: {{focus}}");

            Set<ConstraintViolation<TaskTemplate>> violations = validator.validate(taskTemplate);

            assertTrue(violations.isEmpty());
            assertTrue(taskTemplate.getPromptTemplate().contains("{{language}}"));
            assertTrue(taskTemplate.getPromptTemplate().contains("{{code}}"));
            assertTrue(taskTemplate.getPromptTemplate().contains("{{focus}}"));
        }
    }

    /**
     * Creates a valid TaskTemplate instance for testing.
     *
     * @return a fully populated TaskTemplate with valid field values
     */
    private TaskTemplate createValidTaskTemplate() {
        TaskTemplate taskTemplate = new TaskTemplate();
        taskTemplate.setName("Code Review Task");
        taskTemplate.setDescription("Review code for best practices and potential issues");
        taskTemplate.setPromptTemplate(
                "Review the following code:\n\n{{code}}\n\nProvide feedback.");
        taskTemplate.setTags("code,review,quality");
        taskTemplate.setEvaluationNotes("Look for correctness, style, and performance");
        taskTemplate.setExperiments(new ArrayList<>());
        return taskTemplate;
    }
}
