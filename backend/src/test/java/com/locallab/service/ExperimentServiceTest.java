package com.locallab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.dto.ExperimentConfig;
import com.locallab.dto.Hyperparameters;
import com.locallab.dto.request.ExperimentRequest;
import com.locallab.model.Experiment;
import com.locallab.model.TaskTemplate;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.repository.ExperimentRepository;
import com.locallab.repository.TaskTemplateRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link ExperimentService}.
 *
 * <p>Uses Mockito to mock the {@link ExperimentRepository} and {@link TaskTemplateRepository}
 * dependencies. Tests cover all CRUD operations, status management, run calculation, configuration
 * validation, and error handling scenarios.
 *
 * @see ExperimentService
 * @see ExperimentRepository
 */
@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock private ExperimentRepository experimentRepository;

    @Mock private TaskTemplateRepository taskTemplateRepository;

    private ObjectMapper objectMapper;

    @InjectMocks private ExperimentService experimentService;

    private Experiment draftExperiment;
    private Experiment runningExperiment;
    private Experiment completedExperiment;
    private TaskTemplate taskTemplate;
    private ExperimentRequest createRequest;
    private ExperimentConfig validConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        experimentService =
                new ExperimentService(experimentRepository, taskTemplateRepository, objectMapper);

        taskTemplate =
                TaskTemplate.builder()
                        .id(1L)
                        .name("Code Review Task")
                        .promptTemplate("Review the following code: {{code}}")
                        .createdAt(LocalDateTime.now())
                        .build();

        String configJson =
                "{\"models\":[\"qwen2.5-coder:7b\",\"codellama:7b\"],"
                        + "\"iterations\":3,"
                        + "\"contextMode\":\"NONE\","
                        + "\"hyperparameters\":{\"temperature\":0.7}}";

        draftExperiment =
                Experiment.builder()
                        .id(1L)
                        .name("Draft Experiment")
                        .taskTemplate(taskTemplate)
                        .status(ExperimentStatus.DRAFT)
                        .config(configJson)
                        .createdAt(LocalDateTime.now())
                        .build();

        runningExperiment =
                Experiment.builder()
                        .id(2L)
                        .name("Running Experiment")
                        .taskTemplate(taskTemplate)
                        .status(ExperimentStatus.RUNNING)
                        .config(configJson)
                        .createdAt(LocalDateTime.now())
                        .build();

        completedExperiment =
                Experiment.builder()
                        .id(3L)
                        .name("Completed Experiment")
                        .taskTemplate(taskTemplate)
                        .status(ExperimentStatus.COMPLETED)
                        .config(configJson)
                        .createdAt(LocalDateTime.now())
                        .build();

        validConfig =
                ExperimentConfig.builder()
                        .models(Arrays.asList("qwen2.5-coder:7b", "codellama:7b"))
                        .iterations(3)
                        .contextMode("NONE")
                        .hyperparameters(Hyperparameters.builder().temperature(0.7).build())
                        .build();

        createRequest =
                ExperimentRequest.builder()
                        .name("New Experiment")
                        .taskTemplateId(1L)
                        .config(validConfig)
                        .build();
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all experiments ordered by creation date")
        void shouldReturnAllExperimentsOrderedByCreationDate() {
            when(experimentRepository.findAllByOrderByCreatedAtDesc())
                    .thenReturn(Arrays.asList(draftExperiment, runningExperiment));

            List<Experiment> results = experimentService.findAll();

            assertEquals(2, results.size());
            verify(experimentRepository).findAllByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("Should return empty list when no experiments exist")
        void shouldReturnEmptyListWhenNoExperimentsExist() {
            when(experimentRepository.findAllByOrderByCreatedAtDesc())
                    .thenReturn(Collections.emptyList());

            List<Experiment> results = experimentService.findAll();

            assertTrue(results.isEmpty());
            verify(experimentRepository).findAllByOrderByCreatedAtDesc();
        }
    }

    @Nested
    @DisplayName("findByStatus Tests")
    class FindByStatusTests {

        @Test
        @DisplayName("Should return experiments with specified status")
        void shouldReturnExperimentsWithSpecifiedStatus() {
            when(experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.DRAFT))
                    .thenReturn(Collections.singletonList(draftExperiment));

            List<Experiment> results = experimentService.findByStatus(ExperimentStatus.DRAFT);

            assertEquals(1, results.size());
            assertEquals(ExperimentStatus.DRAFT, results.get(0).getStatus());
            verify(experimentRepository).findByStatusOrderByCreatedAtDesc(ExperimentStatus.DRAFT);
        }

        @Test
        @DisplayName("Should return empty list when no experiments match status")
        void shouldReturnEmptyListWhenNoExperimentsMatchStatus() {
            when(experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.PAUSED))
                    .thenReturn(Collections.emptyList());

            List<Experiment> results = experimentService.findByStatus(ExperimentStatus.PAUSED);

            assertTrue(results.isEmpty());
            verify(experimentRepository).findByStatusOrderByCreatedAtDesc(ExperimentStatus.PAUSED);
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return experiment when found")
        void shouldReturnExperimentWhenFound() {
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(draftExperiment));

            Experiment result = experimentService.findById(1L);

            assertNotNull(result);
            assertEquals("Draft Experiment", result.getName());
            verify(experimentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class, () -> experimentService.findById(999L));

            assertEquals("Experiment not found: 999", exception.getMessage());
            verify(experimentRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create experiment with all fields")
        void shouldCreateExperimentWithAllFields() {
            when(taskTemplateRepository.findById(1L)).thenReturn(Optional.of(taskTemplate));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(
                            invocation -> {
                                Experiment exp = invocation.getArgument(0);
                                exp.setId(10L);
                                return exp;
                            });

            Experiment result = experimentService.create(createRequest);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("New Experiment", result.getName());
            assertEquals(ExperimentStatus.DRAFT, result.getStatus());
            assertEquals(taskTemplate, result.getTaskTemplate());

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository).save(captor.capture());

            Experiment captured = captor.getValue();
            assertEquals("New Experiment", captured.getName());
            assertEquals(ExperimentStatus.DRAFT, captured.getStatus());
        }

        @Test
        @DisplayName("Should create experiment without task template")
        void shouldCreateExperimentWithoutTaskTemplate() {
            ExperimentRequest requestWithoutTemplate =
                    ExperimentRequest.builder()
                            .name("No Template Experiment")
                            .config(validConfig)
                            .build();

            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(
                            invocation -> {
                                Experiment exp = invocation.getArgument(0);
                                exp.setId(11L);
                                return exp;
                            });

            Experiment result = experimentService.create(requestWithoutTemplate);

            assertNotNull(result);
            assertEquals("No Template Experiment", result.getName());
            assertNull(result.getTaskTemplate());

            verify(taskTemplateRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when task template not found")
        void shouldThrowEntityNotFoundExceptionWhenTaskTemplateNotFound() {
            when(taskTemplateRepository.findById(999L)).thenReturn(Optional.empty());

            ExperimentRequest requestWithInvalidTemplate =
                    ExperimentRequest.builder()
                            .name("Invalid Template Experiment")
                            .taskTemplateId(999L)
                            .config(validConfig)
                            .build();

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> experimentService.create(requestWithInvalidTemplate));

            assertEquals("Task template not found: 999", exception.getMessage());
            verify(experimentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when no models specified")
        void shouldThrowIllegalArgumentExceptionWhenNoModelsSpecified() {
            ExperimentConfig invalidConfig =
                    ExperimentConfig.builder()
                            .models(Collections.emptyList())
                            .iterations(3)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            ExperimentRequest invalidRequest =
                    ExperimentRequest.builder()
                            .name("Invalid Experiment")
                            .config(invalidConfig)
                            .build();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> experimentService.create(invalidRequest));

            assertEquals("At least one model must be selected", exception.getMessage());
            verify(experimentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when models is null")
        void shouldThrowIllegalArgumentExceptionWhenModelsIsNull() {
            ExperimentConfig invalidConfig =
                    ExperimentConfig.builder()
                            .models(null)
                            .iterations(3)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            ExperimentRequest invalidRequest =
                    ExperimentRequest.builder()
                            .name("Invalid Experiment")
                            .config(invalidConfig)
                            .build();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> experimentService.create(invalidRequest));

            assertEquals("At least one model must be selected", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when iterations less than 1")
        void shouldThrowIllegalArgumentExceptionWhenIterationsLessThan1() {
            ExperimentConfig invalidConfig =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1"))
                            .iterations(0)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            ExperimentRequest invalidRequest =
                    ExperimentRequest.builder()
                            .name("Invalid Experiment")
                            .config(invalidConfig)
                            .build();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> experimentService.create(invalidRequest));

            assertEquals("Iterations must be at least 1", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when RAG mode without embedding models")
        void shouldThrowIllegalArgumentExceptionWhenRagModeWithoutEmbeddingModels() {
            ExperimentConfig ragConfig =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1"))
                            .iterations(3)
                            .contextMode("RAG")
                            .documentId(1L)
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            ExperimentRequest ragRequest =
                    ExperimentRequest.builder().name("RAG Experiment").config(ragConfig).build();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> experimentService.create(ragRequest));

            assertEquals(
                    "Embedding models must be specified for RAG context mode",
                    exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when RAG mode without document ID")
        void shouldThrowIllegalArgumentExceptionWhenRagModeWithoutDocumentId() {
            ExperimentConfig ragConfig =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1"))
                            .embeddingModels(Arrays.asList("embed1"))
                            .iterations(3)
                            .contextMode("RAG")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            ExperimentRequest ragRequest =
                    ExperimentRequest.builder().name("RAG Experiment").config(ragConfig).build();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> experimentService.create(ragRequest));

            assertEquals(
                    "Document ID must be specified for RAG context mode", exception.getMessage());
        }

        @Test
        @DisplayName("Should create experiment with valid RAG configuration")
        void shouldCreateExperimentWithValidRagConfiguration() {
            ExperimentConfig ragConfig =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1"))
                            .embeddingModels(Arrays.asList("embed1", "embed2"))
                            .iterations(2)
                            .contextMode("RAG")
                            .documentId(1L)
                            .hyperparameters(Hyperparameters.builder().temperature(0.5).build())
                            .build();

            ExperimentRequest ragRequest =
                    ExperimentRequest.builder()
                            .name("Valid RAG Experiment")
                            .config(ragConfig)
                            .build();

            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(
                            invocation -> {
                                Experiment exp = invocation.getArgument(0);
                                exp.setId(12L);
                                return exp;
                            });

            Experiment result = experimentService.create(ragRequest);

            assertNotNull(result);
            assertEquals("Valid RAG Experiment", result.getName());
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update draft experiment successfully")
        void shouldUpdateDraftExperimentSuccessfully() {
            ExperimentRequest updateRequest =
                    ExperimentRequest.builder()
                            .name("Updated Experiment")
                            .taskTemplateId(1L)
                            .config(validConfig)
                            .build();

            when(experimentRepository.findById(1L)).thenReturn(Optional.of(draftExperiment));
            when(taskTemplateRepository.findById(1L)).thenReturn(Optional.of(taskTemplate));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Experiment result = experimentService.update(1L, updateRequest);

            assertNotNull(result);
            assertEquals("Updated Experiment", result.getName());
            verify(experimentRepository).save(any(Experiment.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when updating running experiment")
        void shouldThrowIllegalStateExceptionWhenUpdatingRunningExperiment() {
            when(experimentRepository.findById(2L)).thenReturn(Optional.of(runningExperiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentService.update(2L, createRequest));

            assertEquals("Cannot update experiment with status: RUNNING", exception.getMessage());
            verify(experimentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when updating completed experiment")
        void shouldThrowIllegalStateExceptionWhenUpdatingCompletedExperiment() {
            when(experimentRepository.findById(3L)).thenReturn(Optional.of(completedExperiment));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> experimentService.update(3L, createRequest));

            assertEquals("Cannot update experiment with status: COMPLETED", exception.getMessage());
            verify(experimentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating non-existent experiment")
        void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentExperiment() {
            when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> experimentService.update(999L, createRequest));

            assertEquals("Experiment not found: 999", exception.getMessage());
            verify(experimentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update experiment to remove task template")
        void shouldUpdateExperimentToRemoveTaskTemplate() {
            ExperimentRequest updateRequest =
                    ExperimentRequest.builder()
                            .name("Updated Without Template")
                            .taskTemplateId(null)
                            .config(validConfig)
                            .build();

            when(experimentRepository.findById(1L)).thenReturn(Optional.of(draftExperiment));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Experiment result = experimentService.update(1L, updateRequest);

            assertNotNull(result);
            assertNull(result.getTaskTemplate());
            verify(taskTemplateRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete draft experiment successfully")
        void shouldDeleteDraftExperimentSuccessfully() {
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(draftExperiment));

            experimentService.delete(1L);

            verify(experimentRepository).delete(draftExperiment);
        }

        @Test
        @DisplayName("Should delete completed experiment successfully")
        void shouldDeleteCompletedExperimentSuccessfully() {
            when(experimentRepository.findById(3L)).thenReturn(Optional.of(completedExperiment));

            experimentService.delete(3L);

            verify(experimentRepository).delete(completedExperiment);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when deleting running experiment")
        void shouldThrowIllegalStateExceptionWhenDeletingRunningExperiment() {
            when(experimentRepository.findById(2L)).thenReturn(Optional.of(runningExperiment));

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> experimentService.delete(2L));

            assertEquals("Cannot delete a running experiment", exception.getMessage());
            verify(experimentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existent experiment")
        void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentExperiment() {
            when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class, () -> experimentService.delete(999L));

            assertEquals("Experiment not found: 999", exception.getMessage());
            verify(experimentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should allow deleting paused experiment")
        void shouldAllowDeletingPausedExperiment() {
            Experiment pausedExperiment =
                    Experiment.builder()
                            .id(4L)
                            .name("Paused Experiment")
                            .status(ExperimentStatus.PAUSED)
                            .build();

            when(experimentRepository.findById(4L)).thenReturn(Optional.of(pausedExperiment));

            experimentService.delete(4L);

            verify(experimentRepository).delete(pausedExperiment);
        }

        @Test
        @DisplayName("Should allow deleting failed experiment")
        void shouldAllowDeletingFailedExperiment() {
            Experiment failedExperiment =
                    Experiment.builder()
                            .id(5L)
                            .name("Failed Experiment")
                            .status(ExperimentStatus.FAILED)
                            .build();

            when(experimentRepository.findById(5L)).thenReturn(Optional.of(failedExperiment));

            experimentService.delete(5L);

            verify(experimentRepository).delete(failedExperiment);
        }
    }

    @Nested
    @DisplayName("updateStatus Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update status to RUNNING")
        void shouldUpdateStatusToRunning() {
            when(experimentRepository.findById(1L)).thenReturn(Optional.of(draftExperiment));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            experimentService.updateStatus(1L, ExperimentStatus.RUNNING);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository).save(captor.capture());
            assertEquals(ExperimentStatus.RUNNING, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should update status to COMPLETED")
        void shouldUpdateStatusToCompleted() {
            when(experimentRepository.findById(2L)).thenReturn(Optional.of(runningExperiment));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            experimentService.updateStatus(2L, ExperimentStatus.COMPLETED);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository).save(captor.capture());
            assertEquals(ExperimentStatus.COMPLETED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should update status to PAUSED")
        void shouldUpdateStatusToPaused() {
            when(experimentRepository.findById(2L)).thenReturn(Optional.of(runningExperiment));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            experimentService.updateStatus(2L, ExperimentStatus.PAUSED);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository).save(captor.capture());
            assertEquals(ExperimentStatus.PAUSED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should update status to FAILED")
        void shouldUpdateStatusToFailed() {
            when(experimentRepository.findById(2L)).thenReturn(Optional.of(runningExperiment));
            when(experimentRepository.save(any(Experiment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            experimentService.updateStatus(2L, ExperimentStatus.FAILED);

            ArgumentCaptor<Experiment> captor = ArgumentCaptor.forClass(Experiment.class);
            verify(experimentRepository).save(captor.capture());
            assertEquals(ExperimentStatus.FAILED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when experiment not found")
        void shouldThrowEntityNotFoundExceptionWhenExperimentNotFound() {
            when(experimentRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> experimentService.updateStatus(999L, ExperimentStatus.RUNNING));

            assertEquals("Experiment not found: 999", exception.getMessage());
            verify(experimentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("calculateTotalRuns Tests")
    class CalculateTotalRunsTests {

        @Test
        @DisplayName("Should calculate runs for single model without embeddings")
        void shouldCalculateRunsForSingleModelWithoutEmbeddings() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1"))
                            .iterations(5)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(5, totalRuns); // 1 model × 1 embedding × 5 iterations
        }

        @Test
        @DisplayName("Should calculate runs for multiple models without embeddings")
        void shouldCalculateRunsForMultipleModelsWithoutEmbeddings() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1", "model2", "model3"))
                            .iterations(3)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(9, totalRuns); // 3 models × 1 embedding × 3 iterations
        }

        @Test
        @DisplayName("Should calculate runs with embedding models")
        void shouldCalculateRunsWithEmbeddingModels() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1", "model2"))
                            .embeddingModels(Arrays.asList("embed1", "embed2", "embed3"))
                            .iterations(2)
                            .contextMode("RAG")
                            .documentId(1L)
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(12, totalRuns); // 2 models × 3 embeddings × 2 iterations
        }

        @Test
        @DisplayName("Should treat empty embedding list as non-RAG mode")
        void shouldTreatEmptyEmbeddingListAsNonRagMode() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1", "model2"))
                            .embeddingModels(Collections.emptyList())
                            .iterations(4)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(8, totalRuns); // 2 models × 1 embedding × 4 iterations
        }

        @Test
        @DisplayName("Should treat null embedding list as non-RAG mode")
        void shouldTreatNullEmbeddingListAsNonRagMode() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1"))
                            .embeddingModels(null)
                            .iterations(10)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(10, totalRuns); // 1 model × 1 embedding × 10 iterations
        }

        @Test
        @DisplayName("Should default to 1 iteration when null")
        void shouldDefaultTo1IterationWhenNull() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Arrays.asList("model1", "model2"))
                            .iterations(null)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(2, totalRuns); // 2 models × 1 embedding × 1 iteration
        }

        @Test
        @DisplayName("Should return 0 when no models")
        void shouldReturn0WhenNoModels() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(Collections.emptyList())
                            .iterations(5)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(0, totalRuns); // 0 models × 1 embedding × 5 iterations
        }

        @Test
        @DisplayName("Should return 0 when models is null")
        void shouldReturn0WhenModelsIsNull() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(null)
                            .iterations(5)
                            .contextMode("NONE")
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(0, totalRuns); // 0 models × 1 embedding × 5 iterations
        }

        @Test
        @DisplayName("Should calculate complex configuration correctly")
        void shouldCalculateComplexConfigurationCorrectly() {
            ExperimentConfig config =
                    ExperimentConfig.builder()
                            .models(
                                    Arrays.asList(
                                            "qwen2.5-coder:7b",
                                            "codellama:7b",
                                            "deepseek-coder:6.7b"))
                            .embeddingModels(Arrays.asList("nomic-embed-text", "mxbai-embed-large"))
                            .iterations(3)
                            .contextMode("RAG")
                            .documentId(1L)
                            .hyperparameters(Hyperparameters.builder().build())
                            .build();

            int totalRuns = experimentService.calculateTotalRuns(config);

            assertEquals(18, totalRuns); // 3 models × 2 embeddings × 3 iterations
        }
    }

    @Nested
    @DisplayName("parseConfig Tests")
    class ParseConfigTests {

        @Test
        @DisplayName("Should parse valid JSON config")
        void shouldParseValidJsonConfig() {
            String json =
                    "{\"models\":[\"model1\"],\"iterations\":3,\"contextMode\":\"NONE\","
                            + "\"hyperparameters\":{\"temperature\":0.7}}";

            ExperimentConfig config = experimentService.parseConfig(json);

            assertNotNull(config);
            assertEquals(1, config.getModels().size());
            assertEquals("model1", config.getModels().get(0));
            assertEquals(3, config.getIterations());
            assertEquals("NONE", config.getContextMode());
            assertEquals(0.7, config.getHyperparameters().getTemperature());
        }

        @Test
        @DisplayName("Should return null for null JSON")
        void shouldReturnNullForNullJson() {
            ExperimentConfig config = experimentService.parseConfig(null);

            assertNull(config);
        }

        @Test
        @DisplayName("Should return null for empty JSON")
        void shouldReturnNullForEmptyJson() {
            ExperimentConfig config = experimentService.parseConfig("");

            assertNull(config);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid JSON")
        void shouldThrowIllegalArgumentExceptionForInvalidJson() {
            String invalidJson = "not valid json";

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> experimentService.parseConfig(invalidJson));

            assertEquals("Invalid experiment configuration JSON", exception.getMessage());
        }

        @Test
        @DisplayName("Should parse config with all fields")
        void shouldParseConfigWithAllFields() {
            String json =
                    "{\"models\":[\"model1\",\"model2\"],"
                            + "\"embeddingModels\":[\"embed1\"],"
                            + "\"iterations\":5,"
                            + "\"contextMode\":\"RAG\","
                            + "\"documentId\":1,"
                            + "\"systemPromptId\":2,"
                            + "\"hyperparameters\":{\"temperature\":0.8,\"topP\":0.9,\"topK\":50},"
                            + "\"variableValues\":{\"code\":\"test code\"}}";

            ExperimentConfig config = experimentService.parseConfig(json);

            assertNotNull(config);
            assertEquals(2, config.getModels().size());
            assertEquals(1, config.getEmbeddingModels().size());
            assertEquals(5, config.getIterations());
            assertEquals("RAG", config.getContextMode());
            assertEquals(1L, config.getDocumentId());
            assertEquals(2L, config.getSystemPromptId());
            assertEquals(0.8, config.getHyperparameters().getTemperature());
            assertEquals(0.9, config.getHyperparameters().getTopP());
            assertEquals(50, config.getHyperparameters().getTopK());
            assertEquals("test code", config.getVariableValues().get("code"));
        }

        @Test
        @DisplayName("Should handle variable values map")
        void shouldHandleVariableValuesMap() {
            String json =
                    "{\"models\":[\"model1\"],\"iterations\":1,\"contextMode\":\"NONE\","
                            + "\"hyperparameters\":{},"
                            + "\"variableValues\":{\"key1\":\"value1\",\"key2\":\"value2\"}}";

            ExperimentConfig config = experimentService.parseConfig(json);

            assertNotNull(config);
            Map<String, String> variables = config.getVariableValues();
            assertNotNull(variables);
            assertEquals(2, variables.size());
            assertEquals("value1", variables.get("key1"));
            assertEquals("value2", variables.get("key2"));
        }
    }
}
