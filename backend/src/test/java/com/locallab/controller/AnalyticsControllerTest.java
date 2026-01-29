package com.locallab.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.locallab.dto.LeaderboardData;
import com.locallab.dto.LeaderboardEntry;
import com.locallab.dto.LeaderboardFilter;
import com.locallab.dto.ModelMetrics;
import com.locallab.dto.RunSummary;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.model.Experiment;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;
import com.locallab.service.AnalyticsService;
import com.locallab.service.ExperimentService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link AnalyticsController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The {@link
 * AnalyticsService} and {@link ExperimentService} dependencies are mocked to verify controller
 * behaviour and request/response handling.
 *
 * @see AnalyticsController
 * @see AnalyticsService
 * @see ExperimentService
 */
@WebMvcTest(controllers = AnalyticsController.class)
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
@DisplayName("AnalyticsController")
class AnalyticsControllerTest {

    private static final String LEADERBOARD_URL = "/api/analytics/leaderboard";
    private static final String MODEL_METRICS_URL = "/api/analytics/models";
    private static final String COMPARISON_URL = "/api/experiments";

    @Autowired private MockMvc mockMvc;

    @MockBean private AnalyticsService analyticsService;

    @MockBean private ExperimentService experimentService;

    private LeaderboardEntry qwenEntry;
    private LeaderboardEntry codelamaEntry;
    private LeaderboardData leaderboardData;
    private ModelMetrics modelMetrics;
    private Experiment experiment;

    @BeforeEach
    void setUp() {
        qwenEntry =
                LeaderboardEntry.builder()
                        .modelName("qwen2.5-coder:7b")
                        .runCount(50)
                        .successCount(48)
                        .successRate(96.0)
                        .avgTokensPerSecond(45.5)
                        .minTokensPerSecond(35.0)
                        .maxTokensPerSecond(55.0)
                        .avgDurationMs(2500.0)
                        .minDurationMs(1800L)
                        .maxDurationMs(3200L)
                        .avgTimeToFirstTokenMs(120.0)
                        .build();

        codelamaEntry =
                LeaderboardEntry.builder()
                        .modelName("codellama:7b")
                        .runCount(30)
                        .successCount(28)
                        .successRate(93.3)
                        .avgTokensPerSecond(38.0)
                        .minTokensPerSecond(30.0)
                        .maxTokensPerSecond(45.0)
                        .avgDurationMs(3000.0)
                        .minDurationMs(2200L)
                        .maxDurationMs(3800L)
                        .avgTimeToFirstTokenMs(150.0)
                        .build();

        leaderboardData =
                LeaderboardData.builder()
                        .entries(Arrays.asList(qwenEntry, codelamaEntry))
                        .totalRuns(80)
                        .build();

        Map<Integer, List<RunSummary>> runsByIteration = new LinkedHashMap<>();
        runsByIteration.put(
                1,
                Collections.singletonList(
                        RunSummary.builder()
                                .id(1L)
                                .status(RunStatus.SUCCESS)
                                .durationMs(2500L)
                                .tokensPerSecond(45.0)
                                .build()));

        modelMetrics =
                ModelMetrics.builder()
                        .modelName("qwen2.5-coder:7b")
                        .totalRuns(18)
                        .successfulRuns(17)
                        .failedRuns(1)
                        .successRate(94.4)
                        .avgTokensPerSecond(45.5)
                        .avgDurationMs(2500.0)
                        .avgTimeToFirstTokenMs(120.0)
                        .runsByIteration(runsByIteration)
                        .build();

        experiment =
                Experiment.builder()
                        .id(1L)
                        .name("LLM Code Review Comparison")
                        .status(ExperimentStatus.COMPLETED)
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();
    }

    @Nested
    @DisplayName("GET /api/analytics/leaderboard")
    class GetLeaderboardTests {

        @Test
        @DisplayName("Should return leaderboard with default parameters")
        void shouldReturnLeaderboardWithDefaultParameters() throws Exception {
            when(analyticsService.getLeaderboard(any(LeaderboardFilter.class)))
                    .thenReturn(leaderboardData);

            mockMvc.perform(get(LEADERBOARD_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries", hasSize(2)))
                    .andExpect(jsonPath("$.entries[0].modelName", is("qwen2.5-coder:7b")))
                    .andExpect(jsonPath("$.entries[0].runCount", is(50)))
                    .andExpect(jsonPath("$.entries[0].successCount", is(48)))
                    .andExpect(jsonPath("$.entries[0].successRate", is(96.0)))
                    .andExpect(jsonPath("$.entries[0].avgTokensPerSecond", is(45.5)))
                    .andExpect(jsonPath("$.entries[1].modelName", is("codellama:7b")))
                    .andExpect(jsonPath("$.generatedAt").exists());

            verify(analyticsService).getLeaderboard(any(LeaderboardFilter.class));
        }

        @Test
        @DisplayName("Should return empty leaderboard when no runs exist")
        void shouldReturnEmptyLeaderboardWhenNoRunsExist() throws Exception {
            LeaderboardData emptyData =
                    LeaderboardData.builder().entries(Collections.emptyList()).totalRuns(0).build();

            when(analyticsService.getLeaderboard(any(LeaderboardFilter.class)))
                    .thenReturn(emptyData);

            mockMvc.perform(get(LEADERBOARD_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries", hasSize(0)))
                    .andExpect(jsonPath("$.generatedAt").exists());
        }

        @Test
        @DisplayName("Should filter leaderboard by experimentId")
        void shouldFilterLeaderboardByExperimentId() throws Exception {
            when(analyticsService.getLeaderboard(any(LeaderboardFilter.class)))
                    .thenReturn(leaderboardData);

            mockMvc.perform(
                            get(LEADERBOARD_URL)
                                    .param("experimentId", "1")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries", hasSize(2)));

            verify(analyticsService).getLeaderboard(any(LeaderboardFilter.class));
        }

        @Test
        @DisplayName("Should filter leaderboard by modelName")
        void shouldFilterLeaderboardByModelName() throws Exception {
            LeaderboardData singleModelData =
                    LeaderboardData.builder()
                            .entries(Collections.singletonList(qwenEntry))
                            .totalRuns(50)
                            .build();

            when(analyticsService.getLeaderboard(any(LeaderboardFilter.class)))
                    .thenReturn(singleModelData);

            mockMvc.perform(
                            get(LEADERBOARD_URL)
                                    .param("modelName", "qwen2.5-coder:7b")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries", hasSize(1)))
                    .andExpect(jsonPath("$.entries[0].modelName", is("qwen2.5-coder:7b")));
        }

        @Test
        @DisplayName("Should apply sorting parameters")
        void shouldApplySortingParameters() throws Exception {
            when(analyticsService.getLeaderboard(any(LeaderboardFilter.class)))
                    .thenReturn(leaderboardData);

            mockMvc.perform(
                            get(LEADERBOARD_URL)
                                    .param("sortBy", "duration")
                                    .param("sortOrder", "asc")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries", hasSize(2)));

            verify(analyticsService).getLeaderboard(any(LeaderboardFilter.class));
        }

        @Test
        @DisplayName("Should include all response fields")
        void shouldIncludeAllResponseFields() throws Exception {
            when(analyticsService.getLeaderboard(any(LeaderboardFilter.class)))
                    .thenReturn(leaderboardData);

            mockMvc.perform(get(LEADERBOARD_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries[0].modelName").exists())
                    .andExpect(jsonPath("$.entries[0].runCount").exists())
                    .andExpect(jsonPath("$.entries[0].successCount").exists())
                    .andExpect(jsonPath("$.entries[0].successRate").exists())
                    .andExpect(jsonPath("$.entries[0].avgTokensPerSecond").exists())
                    .andExpect(jsonPath("$.entries[0].minTokensPerSecond").exists())
                    .andExpect(jsonPath("$.entries[0].maxTokensPerSecond").exists())
                    .andExpect(jsonPath("$.entries[0].avgDurationMs").exists())
                    .andExpect(jsonPath("$.entries[0].minDurationMs").exists())
                    .andExpect(jsonPath("$.entries[0].maxDurationMs").exists())
                    .andExpect(jsonPath("$.entries[0].avgTimeToFirstTokenMs").exists())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/models/{modelName}")
    class GetModelMetricsTests {

        @Test
        @DisplayName("Should return model metrics when model exists")
        void shouldReturnModelMetricsWhenModelExists() throws Exception {
            when(analyticsService.getModelMetrics("qwen2.5-coder:7b", null))
                    .thenReturn(modelMetrics);

            mockMvc.perform(
                            get(MODEL_METRICS_URL + "/qwen2.5-coder:7b")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modelName", is("qwen2.5-coder:7b")))
                    .andExpect(jsonPath("$.totalRuns", is(18)))
                    .andExpect(jsonPath("$.successfulRuns", is(17)))
                    .andExpect(jsonPath("$.failedRuns", is(1)))
                    .andExpect(jsonPath("$.successRate", is(94.4)))
                    .andExpect(jsonPath("$.avgTokensPerSecond", is(45.5)))
                    .andExpect(jsonPath("$.avgDurationMs", is(2500.0)))
                    .andExpect(jsonPath("$.avgTimeToFirstTokenMs", is(120.0)))
                    .andExpect(jsonPath("$.runsByIteration").exists());

            verify(analyticsService).getModelMetrics("qwen2.5-coder:7b", null);
        }

        @Test
        @DisplayName("Should return model metrics scoped to experiment")
        void shouldReturnModelMetricsScopedToExperiment() throws Exception {
            when(analyticsService.getModelMetrics("qwen2.5-coder:7b", 1L)).thenReturn(modelMetrics);

            mockMvc.perform(
                            get(MODEL_METRICS_URL + "/qwen2.5-coder:7b")
                                    .param("experimentId", "1")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modelName", is("qwen2.5-coder:7b")));

            verify(analyticsService).getModelMetrics("qwen2.5-coder:7b", 1L);
        }

        @Test
        @DisplayName("Should return 404 when model not found")
        void shouldReturn404WhenModelNotFound() throws Exception {
            when(analyticsService.getModelMetrics("nonexistent-model", null))
                    .thenThrow(
                            new EntityNotFoundException(
                                    "No runs found for model: nonexistent-model"));

            mockMvc.perform(
                            get(MODEL_METRICS_URL + "/nonexistent-model")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(
                            jsonPath(
                                    "$.message", is("No runs found for model: nonexistent-model")));
        }

        @Test
        @DisplayName("Should include runsByIteration in response")
        void shouldIncludeRunsByIterationInResponse() throws Exception {
            when(analyticsService.getModelMetrics("qwen2.5-coder:7b", null))
                    .thenReturn(modelMetrics);

            mockMvc.perform(
                            get(MODEL_METRICS_URL + "/qwen2.5-coder:7b")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.runsByIteration['1']", hasSize(1)))
                    .andExpect(jsonPath("$.runsByIteration['1'][0].id", is(1)))
                    .andExpect(jsonPath("$.runsByIteration['1'][0].status", is("SUCCESS")));
        }
    }

    @Nested
    @DisplayName("GET /api/experiments/{id}/comparison")
    class CompareModelsTests {

        @Test
        @DisplayName("Should return model comparison for experiment")
        void shouldReturnModelComparisonForExperiment() throws Exception {
            Map<String, ModelMetrics> comparison = new LinkedHashMap<>();
            comparison.put("qwen2.5-coder:7b", modelMetrics);

            when(experimentService.findById(1L)).thenReturn(experiment);
            when(analyticsService.compareModels(1L)).thenReturn(comparison);

            mockMvc.perform(
                            get(COMPARISON_URL + "/1/comparison")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.experimentId", is(1)))
                    .andExpect(jsonPath("$.experimentName", is("LLM Code Review Comparison")))
                    .andExpect(jsonPath("$.models['qwen2.5-coder:7b']").exists())
                    .andExpect(jsonPath("$.models['qwen2.5-coder:7b'].totalRuns", is(18)))
                    .andExpect(jsonPath("$.generatedAt").exists());

            verify(experimentService).findById(1L);
            verify(analyticsService).compareModels(1L);
        }

        @Test
        @DisplayName("Should return empty models when experiment has no runs")
        void shouldReturnEmptyModelsWhenExperimentHasNoRuns() throws Exception {
            when(experimentService.findById(1L)).thenReturn(experiment);
            when(analyticsService.compareModels(1L)).thenReturn(Collections.emptyMap());

            mockMvc.perform(
                            get(COMPARISON_URL + "/1/comparison")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.experimentId", is(1)))
                    .andExpect(jsonPath("$.experimentName", is("LLM Code Review Comparison")))
                    .andExpect(jsonPath("$.models").isEmpty())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }

        @Test
        @DisplayName("Should return 404 when experiment not found")
        void shouldReturn404WhenExperimentNotFound() throws Exception {
            when(experimentService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Experiment not found: 999"));

            mockMvc.perform(
                            get(COMPARISON_URL + "/999/comparison")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));

            verify(experimentService).findById(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid experiment ID format")
        void shouldReturn400ForInvalidExperimentIdFormat() throws Exception {
            mockMvc.perform(
                            get(COMPARISON_URL + "/invalid/comparison")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));
        }

        @Test
        @DisplayName("Should include multiple models in comparison")
        void shouldIncludeMultipleModelsInComparison() throws Exception {
            ModelMetrics codelamaMetrics =
                    ModelMetrics.builder()
                            .modelName("codellama:7b")
                            .totalRuns(15)
                            .successfulRuns(14)
                            .failedRuns(1)
                            .successRate(93.3)
                            .avgTokensPerSecond(38.0)
                            .avgDurationMs(3000.0)
                            .avgTimeToFirstTokenMs(150.0)
                            .runsByIteration(Collections.emptyMap())
                            .build();

            Map<String, ModelMetrics> comparison = new LinkedHashMap<>();
            comparison.put("qwen2.5-coder:7b", modelMetrics);
            comparison.put("codellama:7b", codelamaMetrics);

            when(experimentService.findById(1L)).thenReturn(experiment);
            when(analyticsService.compareModels(1L)).thenReturn(comparison);

            mockMvc.perform(
                            get(COMPARISON_URL + "/1/comparison")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.models['qwen2.5-coder:7b'].totalRuns", is(18)))
                    .andExpect(jsonPath("$.models['codellama:7b'].totalRuns", is(15)));
        }
    }
}
