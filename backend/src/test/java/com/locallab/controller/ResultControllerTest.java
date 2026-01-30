package com.locallab.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.locallab.config.CorsProperties;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.model.EmbeddingModel;
import com.locallab.model.Experiment;
import com.locallab.model.ExperimentRun;
import com.locallab.model.SystemPrompt;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;
import com.locallab.service.ResultService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link ResultController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The {@link
 * ResultService} dependency is mocked to verify controller behaviour and request/response handling.
 *
 * @see ResultController
 * @see ResultService
 */
@WebMvcTest(controllers = ResultController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(
        properties = {
            "cors.allowed-origins=http://localhost:5173",
            "cors.allowed-methods=GET,POST,PUT,DELETE",
            "cors.allowed-headers=*",
            "cors.allow-credentials=true",
            "cors.max-age=3600"
        })
@DisplayName("ResultController")
class ResultControllerTest {

    private static final String EXPERIMENTS_BASE_URL = "/api/experiments";
    private static final String RUNS_BASE_URL = "/api/runs";

    @Autowired private MockMvc mockMvc;

    @MockBean private ResultService resultService;

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
    @DisplayName("GET /api/experiments/{id}/runs")
    class GetExperimentRunsTests {

        @Test
        @DisplayName("Should return all runs for experiment when no filters provided")
        void shouldReturnAllRunsForExperimentWhenNoFiltersProvided() throws Exception {
            List<ExperimentRun> runs = Arrays.asList(run1, run2);
            when(resultService.findRunsByExperimentId(1L)).thenReturn(runs);

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/runs")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].modelName", is("qwen2.5-coder:7b")))
                    .andExpect(jsonPath("$[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].modelName", is("codellama:7b")))
                    .andExpect(jsonPath("$[1].status", is("FAILED")));

            verify(resultService).findRunsByExperimentId(1L);
        }

        @Test
        @DisplayName("Should return empty list when no runs exist")
        void shouldReturnEmptyListWhenNoRunsExist() throws Exception {
            when(resultService.findRunsByExperimentId(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/runs")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(resultService).findRunsByExperimentId(1L);
        }

        @Test
        @DisplayName("Should filter runs by status when status param provided")
        void shouldFilterRunsByStatusWhenStatusParamProvided() throws Exception {
            when(resultService.findRunsByExperimentIdAndStatus(1L, RunStatus.SUCCESS))
                    .thenReturn(Collections.singletonList(run1));

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/runs")
                                    .param("status", "SUCCESS")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("SUCCESS")));

            verify(resultService).findRunsByExperimentIdAndStatus(1L, RunStatus.SUCCESS);
            verify(resultService, never()).findRunsByExperimentId(any());
        }

        @Test
        @DisplayName("Should filter runs by model name when modelName param provided")
        void shouldFilterRunsByModelNameWhenModelNameParamProvided() throws Exception {
            when(resultService.findRunsByExperimentIdAndModelName(1L, "qwen2.5-coder:7b"))
                    .thenReturn(Collections.singletonList(run1));

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/runs")
                                    .param("modelName", "qwen2.5-coder:7b")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].modelName", is("qwen2.5-coder:7b")));

            verify(resultService).findRunsByExperimentIdAndModelName(1L, "qwen2.5-coder:7b");
        }

        @Test
        @DisplayName("Should return 404 when experiment not found")
        void shouldReturn404WhenExperimentNotFound() throws Exception {
            when(resultService.findRunsByExperimentId(999L))
                    .thenThrow(new EntityNotFoundException("Experiment not found: 999"));

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/999/runs")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/invalid/runs")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(resultService, never()).findRunsByExperimentId(any());
        }

        @Test
        @DisplayName("Should include all response fields")
        void shouldIncludeAllResponseFields() throws Exception {
            when(resultService.findRunsByExperimentId(1L))
                    .thenReturn(Collections.singletonList(run1));

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/runs")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].experimentId", is(1)))
                    .andExpect(jsonPath("$[0].modelName", is("qwen2.5-coder:7b")))
                    .andExpect(jsonPath("$[0].embeddingModel.id", is(1)))
                    .andExpect(jsonPath("$[0].embeddingModel.name", is("Nomic Embed Text")))
                    .andExpect(jsonPath("$[0].systemPrompt.id", is(1)))
                    .andExpect(jsonPath("$[0].systemPrompt.alias", is("code-assistant")))
                    .andExpect(jsonPath("$[0].iteration", is(1)))
                    .andExpect(jsonPath("$[0].status", is("SUCCESS")))
                    .andExpect(jsonPath("$[0].output", is("Test output 1")))
                    .andExpect(jsonPath("$[0].durationMs", is(2500)))
                    .andExpect(jsonPath("$[0].tokensPerSecond", is(45.5)))
                    .andExpect(jsonPath("$[0].timeToFirstTokenMs", is(120)))
                    .andExpect(jsonPath("$[0].timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/runs/{id}")
    class GetRunByIdTests {

        @Test
        @DisplayName("Should return run when found")
        void shouldReturnRunWhenFound() throws Exception {
            when(resultService.findRunById(1L)).thenReturn(run1);

            mockMvc.perform(get(RUNS_BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.modelName", is("qwen2.5-coder:7b")))
                    .andExpect(jsonPath("$.status", is("SUCCESS")));

            verify(resultService).findRunById(1L);
        }

        @Test
        @DisplayName("Should return 404 when run not found")
        void shouldReturn404WhenRunNotFound() throws Exception {
            when(resultService.findRunById(999L))
                    .thenThrow(new EntityNotFoundException("Experiment run not found: 999"));

            mockMvc.perform(get(RUNS_BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Experiment run not found: 999")));

            verify(resultService).findRunById(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(get(RUNS_BASE_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(resultService, never()).findRunById(any());
        }
    }

    @Nested
    @DisplayName("GET /api/experiments/{id}/export")
    class ExportExperimentTests {

        @Test
        @DisplayName("Should export to CSV format")
        void shouldExportToCsvFormat() throws Exception {
            String csvContent = "id,experimentId,modelName\n1,1,qwen2.5-coder:7b";
            when(resultService.exportToCsv(1L)).thenReturn(csvContent);

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/export")
                                    .param("format", "csv")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"))
                    .andExpect(
                            header().string(
                                            "Content-Disposition",
                                            "form-data; name=\"attachment\"; "
                                                    + "filename=\"experiment-1-results.csv\""))
                    .andExpect(content().string(csvContent));

            verify(resultService).exportToCsv(1L);
        }

        @Test
        @DisplayName("Should export to JSON format")
        void shouldExportToJsonFormat() throws Exception {
            String jsonContent = "[{\"id\":1,\"modelName\":\"qwen2.5-coder:7b\"}]";
            when(resultService.exportToJson(1L)).thenReturn(jsonContent);

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/export")
                                    .param("format", "json")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().string(jsonContent));

            verify(resultService).exportToJson(1L);
        }

        @Test
        @DisplayName("Should handle case-insensitive format parameter")
        void shouldHandleCaseInsensitiveFormatParameter() throws Exception {
            String csvContent = "id,experimentId,modelName\n1,1,qwen2.5-coder:7b";
            when(resultService.exportToCsv(1L)).thenReturn(csvContent);

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/export")
                                    .param("format", "CSV")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"));

            verify(resultService).exportToCsv(1L);
        }

        @Test
        @DisplayName("Should return 400 for unsupported format")
        void shouldReturn400ForUnsupportedFormat() throws Exception {
            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/export")
                                    .param("format", "xml")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is("Unsupported export format: xml. Use 'csv' or 'json'")));

            verify(resultService, never()).exportToCsv(any());
            verify(resultService, never()).exportToJson(any());
        }

        @Test
        @DisplayName("Should return 400 when format parameter missing")
        void shouldReturn400WhenFormatParameterMissing() throws Exception {
            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/1/export")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when experiment not found")
        void shouldReturn404WhenExperimentNotFound() throws Exception {
            when(resultService.exportToCsv(999L))
                    .thenThrow(new EntityNotFoundException("Experiment not found: 999"));

            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/999/export")
                                    .param("format", "csv")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(
                            get(EXPERIMENTS_BASE_URL + "/invalid/export")
                                    .param("format", "csv")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(resultService, never()).exportToCsv(any());
        }
    }
}
