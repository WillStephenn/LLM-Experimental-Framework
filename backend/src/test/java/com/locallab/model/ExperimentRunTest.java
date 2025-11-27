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

import com.locallab.model.enums.RunStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit tests for {@link ExperimentRun} entity.
 *
 * <p>These tests verify the entity's structure, validation constraints, builder pattern, default
 * values, and relationships as defined in the specification and API contract. Database-level
 * constraints (e.g., foreign key relationships) are tested in repository integration tests.
 *
 * @see ExperimentRun
 * @see RunStatus
 */
class ExperimentRunTest {

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
        @DisplayName("Should create ExperimentRun with all fields via builder")
        void shouldCreateExperimentRunWithAllFieldsViaBuilder() {
            Experiment experiment = createValidExperiment();
            EmbeddingModel embeddingModel = createValidEmbeddingModel();
            SystemPrompt systemPrompt = createValidSystemPrompt();

            ExperimentRun run =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("qwen2.5-coder:7b")
                            .embeddingModel(embeddingModel)
                            .systemPrompt(systemPrompt)
                            .iteration(1)
                            .config("{\"temperature\": 0.7}")
                            .status(RunStatus.SUCCESS)
                            .output("Generated response text")
                            .durationMs(2500L)
                            .tokensPerSecond(45.5)
                            .timeToFirstTokenMs(120L)
                            .retrievedChunks("[{\"id\": \"chunk-1\", \"content\": \"test\"}]")
                            .errorMessage(null)
                            .build();

            assertEquals(1L, run.getId());
            assertEquals(experiment, run.getExperiment());
            assertEquals("qwen2.5-coder:7b", run.getModelName());
            assertEquals(embeddingModel, run.getEmbeddingModel());
            assertEquals(systemPrompt, run.getSystemPrompt());
            assertEquals(1, run.getIteration());
            assertEquals("{\"temperature\": 0.7}", run.getConfig());
            assertEquals(RunStatus.SUCCESS, run.getStatus());
            assertEquals("Generated response text", run.getOutput());
            assertEquals(2500L, run.getDurationMs());
            assertEquals(45.5, run.getTokensPerSecond());
            assertEquals(120L, run.getTimeToFirstTokenMs());
            assertEquals(
                    "[{\"id\": \"chunk-1\", \"content\": \"test\"}]", run.getRetrievedChunks());
            assertNull(run.getErrorMessage());
        }

        @Test
        @DisplayName("Should create ExperimentRun with minimal required fields via builder")
        void shouldCreateExperimentRunWithMinimalFieldsViaBuilder() {
            Experiment experiment = createValidExperiment();

            ExperimentRun run =
                    ExperimentRun.builder()
                            .experiment(experiment)
                            .modelName("qwen2.5-coder:7b")
                            .iteration(1)
                            .build();

            assertNull(run.getId());
            assertEquals(experiment, run.getExperiment());
            assertEquals("qwen2.5-coder:7b", run.getModelName());
            assertNull(run.getEmbeddingModel());
            assertNull(run.getSystemPrompt());
            assertEquals(1, run.getIteration());
            assertNull(run.getConfig());
            assertEquals(RunStatus.PENDING, run.getStatus());
            assertNull(run.getOutput());
            assertNull(run.getDurationMs());
            assertNull(run.getTokensPerSecond());
            assertNull(run.getTimeToFirstTokenMs());
            assertNull(run.getRetrievedChunks());
            assertNull(run.getErrorMessage());
        }

        @Test
        @DisplayName("Should default status to PENDING via builder")
        void shouldDefaultStatusToPendingViaBuilder() {
            Experiment experiment = createValidExperiment();

            ExperimentRun run =
                    ExperimentRun.builder()
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .build();

            assertEquals(RunStatus.PENDING, run.getStatus());
        }

        @Test
        @DisplayName("Should allow null optional fields via builder")
        void shouldAllowNullOptionalFieldsViaBuilder() {
            Experiment experiment = createValidExperiment();

            ExperimentRun run =
                    ExperimentRun.builder()
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .embeddingModel(null)
                            .systemPrompt(null)
                            .config(null)
                            .output(null)
                            .build();

            assertNull(run.getEmbeddingModel());
            assertNull(run.getSystemPrompt());
            assertNull(run.getConfig());
            assertNull(run.getOutput());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty ExperimentRun with no-args constructor")
        void shouldCreateEmptyExperimentRunWithNoArgsConstructor() {
            ExperimentRun run = new ExperimentRun();

            assertNull(run.getId());
            assertNull(run.getExperiment());
            assertNull(run.getModelName());
            assertNull(run.getEmbeddingModel());
            assertNull(run.getSystemPrompt());
            assertNull(run.getIteration());
            assertNull(run.getConfig());
            assertEquals(RunStatus.PENDING, run.getStatus());
            assertNull(run.getOutput());
            assertNull(run.getDurationMs());
            assertNull(run.getTokensPerSecond());
            assertNull(run.getTimeToFirstTokenMs());
            assertNull(run.getRetrievedChunks());
            assertNull(run.getErrorMessage());
            assertNull(run.getTimestamp());
        }

        @Test
        @DisplayName("Should create ExperimentRun with all-args constructor")
        void shouldCreateExperimentRunWithAllArgsConstructor() {
            Experiment experiment = createValidExperiment();
            EmbeddingModel embeddingModel = createValidEmbeddingModel();
            SystemPrompt systemPrompt = createValidSystemPrompt();

            ExperimentRun run =
                    new ExperimentRun(
                            1L,
                            experiment,
                            "qwen2.5-coder:7b",
                            embeddingModel,
                            systemPrompt,
                            2,
                            "{\"temperature\": 0.8}",
                            RunStatus.SUCCESS,
                            "Test output",
                            3000L,
                            50.0,
                            100L,
                            "[{\"chunk\": \"data\"}]",
                            null,
                            null);

            assertEquals(1L, run.getId());
            assertEquals(experiment, run.getExperiment());
            assertEquals("qwen2.5-coder:7b", run.getModelName());
            assertEquals(embeddingModel, run.getEmbeddingModel());
            assertEquals(systemPrompt, run.getSystemPrompt());
            assertEquals(2, run.getIteration());
            assertEquals("{\"temperature\": 0.8}", run.getConfig());
            assertEquals(RunStatus.SUCCESS, run.getStatus());
            assertEquals("Test output", run.getOutput());
            assertEquals(3000L, run.getDurationMs());
            assertEquals(50.0, run.getTokensPerSecond());
            assertEquals(100L, run.getTimeToFirstTokenMs());
            assertEquals("[{\"chunk\": \"data\"}]", run.getRetrievedChunks());
            assertNull(run.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void shouldUpdateFieldsViaSetters() {
            ExperimentRun run = new ExperimentRun();
            Experiment experiment = createValidExperiment();
            EmbeddingModel embeddingModel = createValidEmbeddingModel();
            SystemPrompt systemPrompt = createValidSystemPrompt();

            run.setId(99L);
            run.setExperiment(experiment);
            run.setModelName("updated-model:7b");
            run.setEmbeddingModel(embeddingModel);
            run.setSystemPrompt(systemPrompt);
            run.setIteration(5);
            run.setConfig("{\"updated\": true}");
            run.setStatus(RunStatus.RUNNING);
            run.setOutput("Updated output");
            run.setDurationMs(5000L);
            run.setTokensPerSecond(60.0);
            run.setTimeToFirstTokenMs(80L);
            run.setRetrievedChunks("[{\"updated\": \"chunks\"}]");
            run.setErrorMessage("Test error");

            assertEquals(99L, run.getId());
            assertEquals(experiment, run.getExperiment());
            assertEquals("updated-model:7b", run.getModelName());
            assertEquals(embeddingModel, run.getEmbeddingModel());
            assertEquals(systemPrompt, run.getSystemPrompt());
            assertEquals(5, run.getIteration());
            assertEquals("{\"updated\": true}", run.getConfig());
            assertEquals(RunStatus.RUNNING, run.getStatus());
            assertEquals("Updated output", run.getOutput());
            assertEquals(5000L, run.getDurationMs());
            assertEquals(60.0, run.getTokensPerSecond());
            assertEquals(80L, run.getTimeToFirstTokenMs());
            assertEquals("[{\"updated\": \"chunks\"}]", run.getRetrievedChunks());
            assertEquals("Test error", run.getErrorMessage());
        }

        @Test
        @DisplayName("Should allow setting status to all valid values")
        void shouldAllowSettingStatusToAllValidValues() {
            ExperimentRun run = createValidExperimentRun();

            for (RunStatus status : RunStatus.values()) {
                run.setStatus(status);
                assertEquals(status, run.getStatus());
            }
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid fields")
        void shouldPassValidationWithValidFields() {
            ExperimentRun run = createValidExperimentRun();

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should pass validation with only required fields")
        void shouldPassValidationWithOnlyRequiredFields() {
            Experiment experiment = createValidExperiment();
            ExperimentRun run = new ExperimentRun();
            run.setExperiment(experiment);
            run.setModelName("test-model");
            run.setIteration(1);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when experiment is null")
        void shouldFailValidationWhenExperimentIsNull() {
            ExperimentRun run = createValidExperimentRun();
            run.setExperiment(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertEquals(1, violations.size());
            ConstraintViolation<ExperimentRun> violation = violations.iterator().next();
            assertEquals("experiment", violation.getPropertyPath().toString());
            assertEquals("Experiment is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when modelName is blank")
        void shouldFailValidationWhenModelNameIsBlank() {
            ExperimentRun run = createValidExperimentRun();
            run.setModelName("");

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertEquals(1, violations.size());
            ConstraintViolation<ExperimentRun> violation = violations.iterator().next();
            assertEquals("modelName", violation.getPropertyPath().toString());
            assertEquals("Model name is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when modelName is null")
        void shouldFailValidationWhenModelNameIsNull() {
            ExperimentRun run = createValidExperimentRun();
            run.setModelName(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertEquals(1, violations.size());
            ConstraintViolation<ExperimentRun> violation = violations.iterator().next();
            assertEquals("modelName", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when modelName exceeds 100 characters")
        void shouldFailValidationWhenModelNameExceedsMaxLength() {
            ExperimentRun run = createValidExperimentRun();
            run.setModelName("a".repeat(101));

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertEquals(1, violations.size());
            ConstraintViolation<ExperimentRun> violation = violations.iterator().next();
            assertEquals("modelName", violation.getPropertyPath().toString());
            assertEquals("Model name must not exceed 100 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept modelName at maximum length boundary")
        void shouldAcceptModelNameAtMaximumLengthBoundary() {
            ExperimentRun run = createValidExperimentRun();
            run.setModelName("a".repeat(100));

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when iteration is null")
        void shouldFailValidationWhenIterationIsNull() {
            ExperimentRun run = createValidExperimentRun();
            run.setIteration(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertEquals(1, violations.size());
            ConstraintViolation<ExperimentRun> violation = violations.iterator().next();
            assertEquals("iteration", violation.getPropertyPath().toString());
            assertEquals("Iteration is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept null config")
        void shouldAcceptNullConfig() {
            ExperimentRun run = createValidExperimentRun();
            run.setConfig(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null embeddingModel")
        void shouldAcceptNullEmbeddingModel() {
            ExperimentRun run = createValidExperimentRun();
            run.setEmbeddingModel(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null systemPrompt")
        void shouldAcceptNullSystemPrompt() {
            ExperimentRun run = createValidExperimentRun();
            run.setSystemPrompt(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null output")
        void shouldAcceptNullOutput() {
            ExperimentRun run = createValidExperimentRun();
            run.setOutput(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null metrics")
        void shouldAcceptNullMetrics() {
            ExperimentRun run = createValidExperimentRun();
            run.setDurationMs(null);
            run.setTokensPerSecond(null);
            run.setTimeToFirstTokenMs(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept null errorMessage")
        void shouldAcceptNullErrorMessage() {
            ExperimentRun run = createValidExperimentRun();
            run.setErrorMessage(null);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long output (LOB field)")
        void shouldAcceptVeryLongOutput() {
            ExperimentRun run = createValidExperimentRun();
            run.setOutput("a".repeat(100000));

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long config (LOB field)")
        void shouldAcceptVeryLongConfig() {
            ExperimentRun run = createValidExperimentRun();
            run.setConfig("{\"data\": \"" + "a".repeat(50000) + "\"}");

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long retrievedChunks (LOB field)")
        void shouldAcceptVeryLongRetrievedChunks() {
            ExperimentRun run = createValidExperimentRun();
            run.setRetrievedChunks("[{\"content\": \"" + "a".repeat(50000) + "\"}]");

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long errorMessage (LOB field)")
        void shouldAcceptVeryLongErrorMessage() {
            ExperimentRun run = createValidExperimentRun();
            run.setErrorMessage("Error: " + "a".repeat(10000));

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation with multiple invalid fields")
        void shouldFailValidationWithMultipleInvalidFields() {
            ExperimentRun run = new ExperimentRun();

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertEquals(3, violations.size());
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should default status to PENDING")
        void shouldDefaultStatusToPending() {
            ExperimentRun run = new ExperimentRun();

            assertEquals(RunStatus.PENDING, run.getStatus());
        }

        @Test
        @DisplayName("Should default status to PENDING via builder")
        void shouldDefaultStatusToPendingViaBuilder() {
            ExperimentRun run =
                    ExperimentRun.builder()
                            .experiment(createValidExperiment())
                            .modelName("test")
                            .iteration(1)
                            .build();

            assertEquals(RunStatus.PENDING, run.getStatus());
        }
    }

    @Nested
    @DisplayName("Status Lifecycle Tests")
    class StatusLifecycleTests {

        @Test
        @DisplayName("Should allow transition from PENDING to RUNNING")
        void shouldAllowTransitionFromPendingToRunning() {
            ExperimentRun run = createValidExperimentRun();
            assertEquals(RunStatus.PENDING, run.getStatus());

            run.setStatus(RunStatus.RUNNING);

            assertEquals(RunStatus.RUNNING, run.getStatus());
        }

        @Test
        @DisplayName("Should allow transition from RUNNING to SUCCESS")
        void shouldAllowTransitionFromRunningToSuccess() {
            ExperimentRun run = createValidExperimentRun();
            run.setStatus(RunStatus.RUNNING);

            run.setStatus(RunStatus.SUCCESS);

            assertEquals(RunStatus.SUCCESS, run.getStatus());
        }

        @Test
        @DisplayName("Should allow transition from RUNNING to FAILED")
        void shouldAllowTransitionFromRunningToFailed() {
            ExperimentRun run = createValidExperimentRun();
            run.setStatus(RunStatus.RUNNING);

            run.setStatus(RunStatus.FAILED);

            assertEquals(RunStatus.FAILED, run.getStatus());
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods Tests")
    class LombokTests {

        @Test
        @DisplayName("Should generate equals method correctly")
        void shouldGenerateEqualsMethodCorrectly() {
            Experiment experiment = createValidExperiment();
            experiment.setId(1L);

            ExperimentRun run1 =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .build();

            ExperimentRun run2 =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .build();

            assertEquals(run1, run2);
        }

        @Test
        @DisplayName("Should generate hashCode method correctly")
        void shouldGenerateHashCodeMethodCorrectly() {
            Experiment experiment = createValidExperiment();
            experiment.setId(1L);

            ExperimentRun run1 =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .build();

            ExperimentRun run2 =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .build();

            assertEquals(run1.hashCode(), run2.hashCode());
        }

        @Test
        @DisplayName("Should generate toString method correctly")
        void shouldGenerateToStringMethodCorrectly() {
            Experiment experiment = createValidExperiment();
            experiment.setId(1L);

            ExperimentRun run =
                    ExperimentRun.builder()
                            .id(1L)
                            .experiment(experiment)
                            .modelName("qwen2.5-coder:7b")
                            .iteration(2)
                            .status(RunStatus.SUCCESS)
                            .build();

            String toStringResult = run.toString();

            assertNotNull(toStringResult);
            assertTrue(toStringResult.contains("ExperimentRun"));
            assertTrue(toStringResult.contains("id=1"));
            assertTrue(toStringResult.contains("modelName=qwen2.5-coder:7b"));
            assertTrue(toStringResult.contains("iteration=2"));
            assertTrue(toStringResult.contains("status=SUCCESS"));
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should associate with Experiment")
        void shouldAssociateWithExperiment() {
            Experiment experiment = createValidExperiment();
            experiment.setId(1L);

            ExperimentRun run = createValidExperimentRun();
            run.setExperiment(experiment);

            assertEquals(experiment, run.getExperiment());
            assertEquals(1L, run.getExperiment().getId());
        }

        @Test
        @DisplayName("Should associate with EmbeddingModel")
        void shouldAssociateWithEmbeddingModel() {
            EmbeddingModel embeddingModel = createValidEmbeddingModel();
            embeddingModel.setId(1L);

            ExperimentRun run = createValidExperimentRun();
            run.setEmbeddingModel(embeddingModel);

            assertEquals(embeddingModel, run.getEmbeddingModel());
            assertEquals(1L, run.getEmbeddingModel().getId());
            assertEquals("nomic-embed-text", run.getEmbeddingModel().getOllamaModelName());
        }

        @Test
        @DisplayName("Should associate with SystemPrompt")
        void shouldAssociateWithSystemPrompt() {
            SystemPrompt systemPrompt = createValidSystemPrompt();
            systemPrompt.setId(1L);

            ExperimentRun run = createValidExperimentRun();
            run.setSystemPrompt(systemPrompt);

            assertEquals(systemPrompt, run.getSystemPrompt());
            assertEquals(1L, run.getSystemPrompt().getId());
            assertEquals("code-assistant", run.getSystemPrompt().getAlias());
        }

        @Test
        @DisplayName("Should allow null EmbeddingModel for non-RAG runs")
        void shouldAllowNullEmbeddingModelForNonRagRuns() {
            ExperimentRun run = createValidExperimentRun();
            run.setEmbeddingModel(null);

            assertNull(run.getEmbeddingModel());

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should allow null SystemPrompt")
        void shouldAllowNullSystemPromptRelationship() {
            ExperimentRun run = createValidExperimentRun();
            run.setSystemPrompt(null);

            assertNull(run.getSystemPrompt());

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("JSON Field Tests")
    class JsonFieldTests {

        @Test
        @DisplayName("Should store complex JSON config")
        void shouldStoreComplexJsonConfig() {
            String complexConfig =
                    """
                    {
                      "temperature": 0.7,
                      "topP": 0.9,
                      "topK": 40,
                      "contextWindow": 4096
                    }""";

            ExperimentRun run = createValidExperimentRun();
            run.setConfig(complexConfig);

            assertEquals(complexConfig, run.getConfig());
            assertTrue(run.getConfig().contains("\"temperature\""));
            assertTrue(run.getConfig().contains("\"topP\""));
        }

        @Test
        @DisplayName("Should store complex JSON retrievedChunks")
        void shouldStoreComplexJsonRetrievedChunks() {
            String retrievedChunks =
                    """
                    [
                      {
                        "id": "chunk-uuid-1",
                        "content": "Relevant context...",
                        "distance": 0.15,
                        "metadata": {
                          "documentId": 1,
                          "chunkIndex": 3
                        }
                      },
                      {
                        "id": "chunk-uuid-2",
                        "content": "More context...",
                        "distance": 0.25,
                        "metadata": {
                          "documentId": 1,
                          "chunkIndex": 5
                        }
                      }
                    ]""";

            ExperimentRun run = createValidExperimentRun();
            run.setRetrievedChunks(retrievedChunks);

            assertEquals(retrievedChunks, run.getRetrievedChunks());
            assertTrue(run.getRetrievedChunks().contains("chunk-uuid-1"));
            assertTrue(run.getRetrievedChunks().contains("\"distance\""));
        }

        @Test
        @DisplayName("Should accept config with special characters")
        void shouldAcceptConfigWithSpecialCharacters() {
            String configWithSpecialChars =
                    "{\"prompt\": \"Test with \\\"quotes\\\" and \\nnewlines\"}";

            ExperimentRun run = createValidExperimentRun();
            run.setConfig(configWithSpecialChars);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
            assertEquals(configWithSpecialChars, run.getConfig());
        }

        @Test
        @DisplayName("Should accept empty JSON config")
        void shouldAcceptEmptyJsonConfig() {
            ExperimentRun run = createValidExperimentRun();
            run.setConfig("{}");

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept empty JSON array for retrievedChunks")
        void shouldAcceptEmptyJsonArrayForRetrievedChunks() {
            ExperimentRun run = createValidExperimentRun();
            run.setRetrievedChunks("[]");

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should store all metric values")
        void shouldStoreAllMetricValues() {
            ExperimentRun run = createValidExperimentRun();
            run.setDurationMs(2500L);
            run.setTokensPerSecond(45.5);
            run.setTimeToFirstTokenMs(120L);

            assertEquals(2500L, run.getDurationMs());
            assertEquals(45.5, run.getTokensPerSecond());
            assertEquals(120L, run.getTimeToFirstTokenMs());
        }

        @Test
        @DisplayName("Should accept zero metric values")
        void shouldAcceptZeroMetricValues() {
            ExperimentRun run = createValidExperimentRun();
            run.setDurationMs(0L);
            run.setTokensPerSecond(0.0);
            run.setTimeToFirstTokenMs(0L);

            Set<ConstraintViolation<ExperimentRun>> violations = validator.validate(run);

            assertTrue(violations.isEmpty());
            assertEquals(0L, run.getDurationMs());
            assertEquals(0.0, run.getTokensPerSecond());
            assertEquals(0L, run.getTimeToFirstTokenMs());
        }

        @Test
        @DisplayName("Should accept high precision tokensPerSecond")
        void shouldAcceptHighPrecisionTokensPerSecond() {
            ExperimentRun run = createValidExperimentRun();
            run.setTokensPerSecond(45.123456789);

            assertEquals(45.123456789, run.getTokensPerSecond());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should store error message for failed runs")
        void shouldStoreErrorMessageForFailedRuns() {
            ExperimentRun run = createValidExperimentRun();
            run.setStatus(RunStatus.FAILED);
            run.setErrorMessage("Model not found: invalid-model");

            assertEquals(RunStatus.FAILED, run.getStatus());
            assertEquals("Model not found: invalid-model", run.getErrorMessage());
        }

        @Test
        @DisplayName("Should accept multiline error messages")
        void shouldAcceptMultilineErrorMessages() {
            String multilineError = "Error occurred:\n- Model unavailable\n- Timeout exceeded";

            ExperimentRun run = createValidExperimentRun();
            run.setErrorMessage(multilineError);

            assertEquals(multilineError, run.getErrorMessage());
        }
    }

    /**
     * Creates a valid Experiment instance for testing.
     *
     * @return a fully populated Experiment with valid field values
     */
    private Experiment createValidExperiment() {
        return Experiment.builder()
                .name("Test Experiment")
                .config("{\"models\": [\"test-model\"]}")
                .build();
    }

    /**
     * Creates a valid EmbeddingModel instance for testing.
     *
     * @return a fully populated EmbeddingModel with valid field values
     */
    private EmbeddingModel createValidEmbeddingModel() {
        EmbeddingModel model = new EmbeddingModel();
        model.setName("Nomic Embed Text");
        model.setOllamaModelName("nomic-embed-text");
        model.setDimensions(768);
        return model;
    }

    /**
     * Creates a valid SystemPrompt instance for testing.
     *
     * @return a fully populated SystemPrompt with valid field values
     */
    private SystemPrompt createValidSystemPrompt() {
        return SystemPrompt.builder()
                .alias("code-assistant")
                .content("You are an expert code reviewer...")
                .build();
    }

    /**
     * Creates a valid ExperimentRun instance for testing.
     *
     * @return a fully populated ExperimentRun with valid field values
     */
    private ExperimentRun createValidExperimentRun() {
        return ExperimentRun.builder()
                .experiment(createValidExperiment())
                .modelName("qwen2.5-coder:7b")
                .iteration(1)
                .build();
    }
}
