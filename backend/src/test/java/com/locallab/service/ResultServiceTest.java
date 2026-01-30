package com.locallab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.locallab.model.EmbeddingModel;
import com.locallab.model.Experiment;
import com.locallab.model.ExperimentRun;
import com.locallab.model.SystemPrompt;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;
import com.locallab.repository.ExperimentRepository;
import com.locallab.repository.ExperimentRunRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link ResultService}.
 *
 * <p>Uses Mockito to mock the repository dependencies. Tests cover run retrieval, filtering, export
 * functionality, and error handling scenarios.
 *
 * @see ResultService
 * @see ExperimentRunRepository
 * @see ExperimentRepository
 */
@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock private ExperimentRunRepository experimentRunRepository;

    @Mock private ExperimentRepository experimentRepository;

    @Spy private ObjectMapper objectMapper = createObjectMapper();

    @InjectMocks private ResultService resultService;

    private Experiment experiment;
    private ExperimentRun run1;
    private ExperimentRun run2;
    private EmbeddingModel embeddingModel;
    private SystemPrompt systemPrompt;

    @BeforeEach
    void setUp() {
        experiment =
                Experiment.builder()
                        .id(1L)
                        .name("Test Experiment")
                        .status(ExperimentStatus.COMPLETED)
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        embeddingModel =
                EmbeddingModel.builder()
                        .id(1L)
                        .name("Nomic Embed Text")
                        .ollamaModelName("nomic-embed-text")
                        .dimensions(768)
                        .createdAt(LocalDateTime.now())
                        .build();

        systemPrompt =
                SystemPrompt.builder()
                        .id(1L)
                        .alias("code-assistant")
                        .content("You are an expert code reviewer")
                        .createdAt(LocalDateTime.now())
                        .build();

        run1 =
                ExperimentRun.builder()
                        .id(1L)
                        .experiment(experiment)
                        .modelName("qwen2.5-coder:7b")
                        .embeddingModel(embeddingModel)
                        .systemPrompt(systemPrompt)
                        .iteration(1)
                        .status(RunStatus.SUCCESS)
                        .output("Test output 1")
                        .durationMs(2500L)
                        .tokensPerSecond(45.5)
                        .timeToFirstTokenMs(120L)
                        .timestamp(LocalDateTime.of(2025, 11, 27, 10, 5))
                        .build();

        run2 =
                ExperimentRun.builder()
                        .id(2L)
                        .experiment(experiment)
                        .modelName("codellama:7b")
                        .iteration(2)
                        .status(RunStatus.FAILED)
                        .errorMessage("Model timeout")
                        .timestamp(LocalDateTime.of(2025, 11, 27, 10, 10))
                        .build();
    }

    @Nested
    @DisplayName("findRunsByExperimentId Tests")
    class FindRunsByExperimentIdTests {

        @Test
        @DisplayName("Should return all runs for experiment")
        void shouldReturnAllRunsForExperiment() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(run1, run2));

            List<ExperimentRun> results = resultService.findRunsByExperimentId(1L);

            assertEquals(2, results.size());
            assertEquals("qwen2.5-coder:7b", results.get(0).getModelName());
            assertEquals("codellama:7b", results.get(1).getModelName());
            verify(experimentRepository).existsById(1L);
            verify(experimentRunRepository).findByExperimentIdOrderByIterationAsc(1L);
        }

        @Test
        @DisplayName("Should return empty list when no runs exist")
        void shouldReturnEmptyListWhenNoRunsExist() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Collections.emptyList());

            List<ExperimentRun> results = resultService.findRunsByExperimentId(1L);

            assertTrue(results.isEmpty());
            verify(experimentRunRepository).findByExperimentIdOrderByIterationAsc(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when experiment not found")
        void shouldThrowEntityNotFoundExceptionWhenExperimentNotFound() {
            when(experimentRepository.existsById(999L)).thenReturn(false);

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> resultService.findRunsByExperimentId(999L));

            assertEquals("Experiment not found: 999", exception.getMessage());
            verify(experimentRepository).existsById(999L);
            verify(experimentRunRepository, never()).findByExperimentIdOrderByIterationAsc(999L);
        }
    }

    @Nested
    @DisplayName("findRunsByExperimentIdAndStatus Tests")
    class FindRunsByExperimentIdAndStatusTests {

        @Test
        @DisplayName("Should return runs filtered by status")
        void shouldReturnRunsFilteredByStatus() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdAndStatus(1L, RunStatus.SUCCESS))
                    .thenReturn(Collections.singletonList(run1));

            List<ExperimentRun> results =
                    resultService.findRunsByExperimentIdAndStatus(1L, RunStatus.SUCCESS);

            assertEquals(1, results.size());
            assertEquals(RunStatus.SUCCESS, results.get(0).getStatus());
            verify(experimentRunRepository).findByExperimentIdAndStatus(1L, RunStatus.SUCCESS);
        }

        @Test
        @DisplayName("Should return empty list when no runs match status")
        void shouldReturnEmptyListWhenNoRunsMatchStatus() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdAndStatus(1L, RunStatus.PENDING))
                    .thenReturn(Collections.emptyList());

            List<ExperimentRun> results =
                    resultService.findRunsByExperimentIdAndStatus(1L, RunStatus.PENDING);

            assertTrue(results.isEmpty());
            verify(experimentRunRepository).findByExperimentIdAndStatus(1L, RunStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when experiment not found")
        void shouldThrowEntityNotFoundExceptionWhenExperimentNotFound() {
            when(experimentRepository.existsById(999L)).thenReturn(false);

            assertThrows(
                    EntityNotFoundException.class,
                    () -> resultService.findRunsByExperimentIdAndStatus(999L, RunStatus.SUCCESS));

            verify(experimentRepository).existsById(999L);
        }
    }

    @Nested
    @DisplayName("findRunsByExperimentIdAndModelName Tests")
    class FindRunsByExperimentIdAndModelNameTests {

        @Test
        @DisplayName("Should return runs filtered by model name")
        void shouldReturnRunsFilteredByModelName() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b"))
                    .thenReturn(Collections.singletonList(run1));

            List<ExperimentRun> results =
                    resultService.findRunsByExperimentIdAndModelName(1L, "qwen2.5-coder:7b");

            assertEquals(1, results.size());
            assertEquals("qwen2.5-coder:7b", results.get(0).getModelName());
            verify(experimentRunRepository).findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b");
        }

        @Test
        @DisplayName("Should return empty list when no runs match model name")
        void shouldReturnEmptyListWhenNoRunsMatchModelName() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "nonexistent"))
                    .thenReturn(Collections.emptyList());

            List<ExperimentRun> results =
                    resultService.findRunsByExperimentIdAndModelName(1L, "nonexistent");

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("findRunById Tests")
    class FindRunByIdTests {

        @Test
        @DisplayName("Should return run when found")
        void shouldReturnRunWhenFound() {
            when(experimentRunRepository.findById(1L)).thenReturn(Optional.of(run1));

            ExperimentRun result = resultService.findRunById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("qwen2.5-coder:7b", result.getModelName());
            verify(experimentRunRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when run not found")
        void shouldThrowEntityNotFoundExceptionWhenRunNotFound() {
            when(experimentRunRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class, () -> resultService.findRunById(999L));

            assertEquals("Experiment run not found: 999", exception.getMessage());
            verify(experimentRunRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("exportToCsv Tests")
    class ExportToCsvTests {

        @Test
        @DisplayName("Should export runs to CSV format")
        void shouldExportRunsToCsvFormat() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(run1, run2));

            String csv = resultService.exportToCsv(1L);

            assertNotNull(csv);
            assertTrue(csv.startsWith("id,experimentId,modelName,embeddingModel,"));
            assertTrue(csv.contains("qwen2.5-coder:7b"));
            assertTrue(csv.contains("codellama:7b"));
            assertTrue(csv.contains("Nomic Embed Text"));
            assertTrue(csv.contains("code-assistant"));
            assertTrue(csv.contains("SUCCESS"));
            assertTrue(csv.contains("FAILED"));
        }

        @Test
        @DisplayName("Should return only header when no runs exist")
        void shouldReturnOnlyHeaderWhenNoRunsExist() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Collections.emptyList());

            String csv = resultService.exportToCsv(1L);

            assertNotNull(csv);
            assertTrue(csv.startsWith("id,experimentId,modelName,"));
            // Should only have the header line
            assertEquals(1, csv.lines().count());
        }

        @Test
        @DisplayName("Should escape special characters in CSV")
        void shouldEscapeSpecialCharactersInCsv() {
            run1.setErrorMessage("Error with, comma");
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Collections.singletonList(run1));

            String csv = resultService.exportToCsv(1L);

            assertTrue(csv.contains("\"Error with, comma\""));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when experiment not found")
        void shouldThrowEntityNotFoundExceptionWhenExperimentNotFound() {
            when(experimentRepository.existsById(999L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> resultService.exportToCsv(999L));
        }
    }

    @Nested
    @DisplayName("exportToJson Tests")
    class ExportToJsonTests {

        @Test
        @DisplayName("Should export runs to JSON format")
        void shouldExportRunsToJsonFormat() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(run1, run2));

            String json = resultService.exportToJson(1L);

            assertNotNull(json);
            assertTrue(json.contains("\"id\" : 1"));
            assertTrue(json.contains("\"modelName\" : \"qwen2.5-coder:7b\""));
            assertTrue(json.contains("\"modelName\" : \"codellama:7b\""));
            assertTrue(json.contains("\"status\" : \"SUCCESS\""));
            assertTrue(json.contains("\"status\" : \"FAILED\""));
        }

        @Test
        @DisplayName("Should return empty array when no runs exist")
        void shouldReturnEmptyArrayWhenNoRunsExist() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Collections.emptyList());

            String json = resultService.exportToJson(1L);

            assertNotNull(json);
            assertEquals("[ ]", json.trim());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when experiment not found")
        void shouldThrowEntityNotFoundExceptionWhenExperimentNotFound() {
            when(experimentRepository.existsById(999L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> resultService.exportToJson(999L));
        }

        @Test
        @DisplayName("Should include nested objects in JSON export")
        void shouldIncludeNestedObjectsInJsonExport() {
            when(experimentRepository.existsById(1L)).thenReturn(true);
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Collections.singletonList(run1));

            String json = resultService.exportToJson(1L);

            assertTrue(json.contains("\"embeddingModel\""));
            assertTrue(json.contains("\"name\" : \"Nomic Embed Text\""));
            assertTrue(json.contains("\"systemPrompt\""));
            assertTrue(json.contains("\"alias\" : \"code-assistant\""));
        }
    }

    /**
     * Creates an ObjectMapper configured for Java 8 date/time types.
     *
     * @return configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
