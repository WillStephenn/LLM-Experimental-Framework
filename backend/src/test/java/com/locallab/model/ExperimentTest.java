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

import com.locallab.model.enums.ExperimentStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit tests for {@link Experiment} entity.
 *
 * <p>These tests verify the entity's structure, validation constraints, builder pattern, and
 * default values as defined in the specification and API contract. Database-level constraints
 * (e.g., foreign key relationships to TaskTemplate) are tested in repository integration tests.
 *
 * @see Experiment
 * @see ExperimentStatus
 */
class ExperimentTest {

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
        @DisplayName("Should create Experiment with all fields via builder")
        void shouldCreateExperimentWithAllFieldsViaBuilder() {
            TaskTemplate taskTemplate =
                    TaskTemplate.builder()
                            .id(1L)
                            .name("Test Task")
                            .promptTemplate("Test prompt")
                            .build();

            Experiment experiment =
                    Experiment.builder()
                            .id(1L)
                            .name("LLM Code Review Comparison")
                            .taskTemplate(taskTemplate)
                            .status(ExperimentStatus.RUNNING)
                            .config("{\"models\": [\"qwen2.5-coder:7b\"], \"iterations\": 3}")
                            .runs(new ArrayList<>())
                            .build();

            assertEquals(1L, experiment.getId());
            assertEquals("LLM Code Review Comparison", experiment.getName());
            assertEquals(taskTemplate, experiment.getTaskTemplate());
            assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
            assertEquals(
                    "{\"models\": [\"qwen2.5-coder:7b\"], \"iterations\": 3}",
                    experiment.getConfig());
            assertNull(experiment.getCreatedAt());
            assertNotNull(experiment.getRuns());
            assertTrue(experiment.getRuns().isEmpty());
        }

        @Test
        @DisplayName("Should create Experiment with minimal required fields via builder")
        void shouldCreateExperimentWithMinimalFieldsViaBuilder() {
            Experiment experiment = Experiment.builder().name("Simple Experiment").build();

            assertNull(experiment.getId());
            assertEquals("Simple Experiment", experiment.getName());
            assertNull(experiment.getTaskTemplate());
            assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());
            assertNull(experiment.getConfig());
            assertNotNull(experiment.getRuns());
            assertTrue(experiment.getRuns().isEmpty());
        }

        @Test
        @DisplayName("Should default status to DRAFT via builder")
        void shouldDefaultStatusToDraftViaBuilder() {
            Experiment experiment = Experiment.builder().name("Test Experiment").build();

            assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());
        }

        @Test
        @DisplayName("Should initialise runs list by default via builder")
        void shouldInitialiseRunsListByDefaultViaBuilder() {
            Experiment experiment = Experiment.builder().name("Test Experiment").build();

            assertNotNull(experiment.getRuns());
            assertTrue(experiment.getRuns().isEmpty());
        }

        @Test
        @DisplayName("Should allow null taskTemplate via builder")
        void shouldAllowNullTaskTemplateViaBuilder() {
            Experiment experiment =
                    Experiment.builder().name("Ad-hoc Experiment").taskTemplate(null).build();

            assertNull(experiment.getTaskTemplate());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty Experiment with no-args constructor")
        void shouldCreateEmptyExperimentWithNoArgsConstructor() {
            Experiment experiment = new Experiment();

            assertNull(experiment.getId());
            assertNull(experiment.getName());
            assertNull(experiment.getTaskTemplate());
            // Default value is set via @Builder.Default, so no-args constructor may not apply it
            // For Lombok @Data with @Builder.Default, the default is set by field initialiser
            assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());
            assertNull(experiment.getConfig());
            assertNull(experiment.getCreatedAt());
            assertNotNull(experiment.getRuns());
            assertTrue(experiment.getRuns().isEmpty());
        }

        @Test
        @DisplayName("Should create Experiment with all-args constructor")
        void shouldCreateExperimentWithAllArgsConstructor() {
            TaskTemplate taskTemplate = new TaskTemplate();
            taskTemplate.setId(1L);
            ArrayList<ExperimentRun> runs = new ArrayList<>();

            Experiment experiment =
                    new Experiment(
                            1L,
                            "Test Experiment",
                            taskTemplate,
                            ExperimentStatus.COMPLETED,
                            "{\"models\": [\"test\"]}",
                            null,
                            runs);

            assertEquals(1L, experiment.getId());
            assertEquals("Test Experiment", experiment.getName());
            assertEquals(taskTemplate, experiment.getTaskTemplate());
            assertEquals(ExperimentStatus.COMPLETED, experiment.getStatus());
            assertEquals("{\"models\": [\"test\"]}", experiment.getConfig());
            assertEquals(runs, experiment.getRuns());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void shouldUpdateFieldsViaSetters() {
            Experiment experiment = new Experiment();
            TaskTemplate taskTemplate = new TaskTemplate();
            taskTemplate.setId(2L);
            ArrayList<ExperimentRun> runs = new ArrayList<>();

            experiment.setId(99L);
            experiment.setName("Updated Experiment");
            experiment.setTaskTemplate(taskTemplate);
            experiment.setStatus(ExperimentStatus.PAUSED);
            experiment.setConfig("{\"updated\": true}");
            experiment.setRuns(runs);

            assertEquals(99L, experiment.getId());
            assertEquals("Updated Experiment", experiment.getName());
            assertEquals(taskTemplate, experiment.getTaskTemplate());
            assertEquals(ExperimentStatus.PAUSED, experiment.getStatus());
            assertEquals("{\"updated\": true}", experiment.getConfig());
            assertEquals(runs, experiment.getRuns());
        }

        @Test
        @DisplayName("Should allow setting status to all valid values")
        void shouldAllowSettingStatusToAllValidValues() {
            Experiment experiment = new Experiment();
            experiment.setName("Test");

            for (ExperimentStatus status : ExperimentStatus.values()) {
                experiment.setStatus(status);
                assertEquals(status, experiment.getStatus());
            }
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid fields")
        void shouldPassValidationWithValidFields() {
            Experiment experiment = createValidExperiment();

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should pass validation with only required fields")
        void shouldPassValidationWithOnlyRequiredFields() {
            Experiment experiment = new Experiment();
            experiment.setName("Minimal Experiment");

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when name is blank")
        void shouldFailValidationWhenNameIsBlank() {
            Experiment experiment = createValidExperiment();
            experiment.setName("");

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertEquals(1, violations.size());
            ConstraintViolation<Experiment> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertEquals("Name is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when name is null")
        void shouldFailValidationWhenNameIsNull() {
            Experiment experiment = createValidExperiment();
            experiment.setName(null);

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertEquals(1, violations.size());
            ConstraintViolation<Experiment> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when name exceeds 200 characters")
        void shouldFailValidationWhenNameExceedsMaxLength() {
            Experiment experiment = createValidExperiment();
            experiment.setName("a".repeat(201));

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertEquals(1, violations.size());
            ConstraintViolation<Experiment> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertEquals("Name must not exceed 200 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept name at maximum length boundary")
        void shouldAcceptNameAtMaximumLengthBoundary() {
            Experiment experiment = createValidExperiment();
            experiment.setName("a".repeat(200));

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null taskTemplate")
        void shouldAcceptNullTaskTemplate() {
            Experiment experiment = createValidExperiment();
            experiment.setTaskTemplate(null);

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null config")
        void shouldAcceptNullConfig() {
            Experiment experiment = createValidExperiment();
            experiment.setConfig(null);

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long config (LOB field)")
        void shouldAcceptVeryLongConfig() {
            Experiment experiment = createValidExperiment();
            experiment.setConfig("{\"data\": \"" + "a".repeat(50000) + "\"}");

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept empty JSON config")
        void shouldAcceptEmptyJsonConfig() {
            Experiment experiment = createValidExperiment();
            experiment.setConfig("{}");

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should default status to DRAFT")
        void shouldDefaultStatusToDraft() {
            Experiment experiment = new Experiment();
            experiment.setName("Test");

            assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());
        }

        @Test
        @DisplayName("Should initialise runs list to empty ArrayList")
        void shouldInitialiseRunsListToEmptyArrayList() {
            Experiment experiment = new Experiment();

            assertNotNull(experiment.getRuns());
            assertTrue(experiment.getRuns().isEmpty());
            assertTrue(experiment.getRuns() instanceof ArrayList);
        }
    }

    @Nested
    @DisplayName("Status Lifecycle Tests")
    class StatusLifecycleTests {

        @Test
        @DisplayName("Should allow transition from DRAFT to RUNNING")
        void shouldAllowTransitionFromDraftToRunning() {
            Experiment experiment = createValidExperiment();
            assertEquals(ExperimentStatus.DRAFT, experiment.getStatus());

            experiment.setStatus(ExperimentStatus.RUNNING);

            assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
        }

        @Test
        @DisplayName("Should allow transition from RUNNING to COMPLETED")
        void shouldAllowTransitionFromRunningToCompleted() {
            Experiment experiment = createValidExperiment();
            experiment.setStatus(ExperimentStatus.RUNNING);

            experiment.setStatus(ExperimentStatus.COMPLETED);

            assertEquals(ExperimentStatus.COMPLETED, experiment.getStatus());
        }

        @Test
        @DisplayName("Should allow transition from RUNNING to FAILED")
        void shouldAllowTransitionFromRunningToFailed() {
            Experiment experiment = createValidExperiment();
            experiment.setStatus(ExperimentStatus.RUNNING);

            experiment.setStatus(ExperimentStatus.FAILED);

            assertEquals(ExperimentStatus.FAILED, experiment.getStatus());
        }

        @Test
        @DisplayName("Should allow transition from RUNNING to PAUSED")
        void shouldAllowTransitionFromRunningToPaused() {
            Experiment experiment = createValidExperiment();
            experiment.setStatus(ExperimentStatus.RUNNING);

            experiment.setStatus(ExperimentStatus.PAUSED);

            assertEquals(ExperimentStatus.PAUSED, experiment.getStatus());
        }

        @Test
        @DisplayName("Should allow transition from PAUSED to RUNNING")
        void shouldAllowTransitionFromPausedToRunning() {
            Experiment experiment = createValidExperiment();
            experiment.setStatus(ExperimentStatus.PAUSED);

            experiment.setStatus(ExperimentStatus.RUNNING);

            assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods Tests")
    class LombokTests {

        @Test
        @DisplayName("Should generate equals method correctly")
        void shouldGenerateEqualsMethodCorrectly() {
            Experiment experiment1 = createValidExperiment();
            experiment1.setId(1L);

            Experiment experiment2 = createValidExperiment();
            experiment2.setId(1L);

            assertEquals(experiment1, experiment2);
        }

        @Test
        @DisplayName("Should generate hashCode method correctly")
        void shouldGenerateHashCodeMethodCorrectly() {
            Experiment experiment1 = createValidExperiment();
            experiment1.setId(1L);

            Experiment experiment2 = createValidExperiment();
            experiment2.setId(1L);

            assertEquals(experiment1.hashCode(), experiment2.hashCode());
        }

        @Test
        @DisplayName("Should generate toString method correctly")
        void shouldGenerateToStringMethodCorrectly() {
            Experiment experiment = createValidExperiment();
            experiment.setId(1L);

            String toStringResult = experiment.toString();

            assertNotNull(toStringResult);
            assertTrue(toStringResult.contains("Experiment"));
            assertTrue(toStringResult.contains("id=1"));
            assertTrue(toStringResult.contains("name=LLM Code Review Comparison"));
            assertTrue(toStringResult.contains("status=DRAFT"));
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should associate with TaskTemplate")
        void shouldAssociateWithTaskTemplate() {
            TaskTemplate taskTemplate =
                    TaskTemplate.builder()
                            .id(1L)
                            .name("Test Task")
                            .promptTemplate("Test prompt")
                            .build();

            Experiment experiment = createValidExperiment();
            experiment.setTaskTemplate(taskTemplate);

            assertEquals(taskTemplate, experiment.getTaskTemplate());
            assertEquals(1L, experiment.getTaskTemplate().getId());
        }

        @Test
        @DisplayName("Should manage runs collection")
        void shouldManageRunsCollection() {
            Experiment experiment = createValidExperiment();
            experiment.setId(1L);

            ExperimentRun run1 =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("qwen2.5-coder:7b")
                            .iteration(1)
                            .build();

            ExperimentRun run2 =
                    ExperimentRun.builder()
                            .id(2L)
                            .experiment(experiment)
                            .modelName("codellama:7b")
                            .iteration(1)
                            .build();

            experiment.getRuns().add(run1);
            experiment.getRuns().add(run2);

            assertEquals(2, experiment.getRuns().size());
            assertTrue(experiment.getRuns().contains(run1));
            assertTrue(experiment.getRuns().contains(run2));
        }

        @Test
        @DisplayName("Should allow clearing runs collection")
        void shouldAllowClearingRunsCollection() {
            Experiment experiment = createValidExperiment();

            ExperimentRun run =
                    ExperimentRun.builder()
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .build();

            experiment.getRuns().add(run);
            assertEquals(1, experiment.getRuns().size());

            experiment.getRuns().clear();

            assertTrue(experiment.getRuns().isEmpty());
        }
    }

    @Nested
    @DisplayName("JSON Config Tests")
    class JsonConfigTests {

        @Test
        @DisplayName("Should store complex JSON config")
        void shouldStoreComplexJsonConfig() {
            String complexConfig =
                    """
                    {
                      "models": ["qwen2.5-coder:7b", "codellama:7b"],
                      "embeddingModels": ["nomic-embed-text"],
                      "iterations": 3,
                      "contextMode": "RAG",
                      "hyperparameters": {
                        "temperature": 0.7,
                        "topP": 0.9,
                        "topK": 40
                      }
                    }""";

            Experiment experiment = createValidExperiment();
            experiment.setConfig(complexConfig);

            assertEquals(complexConfig, experiment.getConfig());
            assertTrue(experiment.getConfig().contains("\"models\""));
            assertTrue(experiment.getConfig().contains("\"hyperparameters\""));
        }

        @Test
        @DisplayName("Should accept config with special characters")
        void shouldAcceptConfigWithSpecialCharacters() {
            String configWithSpecialChars =
                    "{\"prompt\": \"Test with \\\"quotes\\\" and \\nnewlines\"}";

            Experiment experiment = createValidExperiment();
            experiment.setConfig(configWithSpecialChars);

            Set<ConstraintViolation<Experiment>> violations = validator.validate(experiment);

            assertTrue(violations.isEmpty());
            assertEquals(configWithSpecialChars, experiment.getConfig());
        }
    }

    /**
     * Creates a valid Experiment instance for testing.
     *
     * @return a fully populated Experiment with valid field values
     */
    private Experiment createValidExperiment() {
        Experiment experiment = new Experiment();
        experiment.setName("LLM Code Review Comparison");
        experiment.setConfig("{\"models\": [\"qwen2.5-coder:7b\"], \"iterations\": 3}");
        experiment.setRuns(new ArrayList<>());
        return experiment;
    }
}
