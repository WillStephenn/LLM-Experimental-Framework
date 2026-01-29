package com.locallab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.locallab.dto.LeaderboardData;
import com.locallab.dto.LeaderboardEntry;
import com.locallab.dto.LeaderboardFilter;
import com.locallab.dto.ModelMetrics;
import com.locallab.dto.RunSummary;
import com.locallab.model.EmbeddingModel;
import com.locallab.model.Experiment;
import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;
import com.locallab.repository.ExperimentRunRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link AnalyticsService}.
 *
 * <p>Uses Mockito to mock the {@link ExperimentRunRepository} dependency. Tests cover leaderboard
 * generation, model metrics calculation, model comparison, and various edge cases.
 *
 * @see AnalyticsService
 * @see ExperimentRunRepository
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private ExperimentRunRepository experimentRunRepository;

    @InjectMocks private AnalyticsService analyticsService;

    private Experiment experiment;
    private EmbeddingModel embeddingModel;
    private ExperimentRun successRun1;
    private ExperimentRun successRun2;
    private ExperimentRun successRun3;
    private ExperimentRun failedRun;
    private ExperimentRun pendingRun;

    @BeforeEach
    void setUp() {
        experiment =
                Experiment.builder()
                        .id(1L)
                        .name("Test Experiment")
                        .status(ExperimentStatus.COMPLETED)
                        .createdAt(LocalDateTime.now())
                        .build();

        embeddingModel =
                EmbeddingModel.builder()
                        .id(1L)
                        .name("Nomic Embed Text")
                        .ollamaModelName("nomic-embed-text")
                        .dimensions(768)
                        .createdAt(LocalDateTime.now())
                        .build();

        successRun1 =
                ExperimentRun.builder()
                        .id(1L)
                        .experiment(experiment)
                        .modelName("qwen2.5-coder:7b")
                        .iteration(1)
                        .status(RunStatus.SUCCESS)
                        .durationMs(2500L)
                        .tokensPerSecond(45.5)
                        .timeToFirstTokenMs(120L)
                        .timestamp(LocalDateTime.now())
                        .build();

        successRun2 =
                ExperimentRun.builder()
                        .id(2L)
                        .experiment(experiment)
                        .modelName("qwen2.5-coder:7b")
                        .iteration(2)
                        .status(RunStatus.SUCCESS)
                        .durationMs(2200L)
                        .tokensPerSecond(50.0)
                        .timeToFirstTokenMs(100L)
                        .timestamp(LocalDateTime.now())
                        .build();

        successRun3 =
                ExperimentRun.builder()
                        .id(3L)
                        .experiment(experiment)
                        .modelName("codellama:7b")
                        .iteration(1)
                        .status(RunStatus.SUCCESS)
                        .durationMs(3000L)
                        .tokensPerSecond(35.0)
                        .timeToFirstTokenMs(150L)
                        .timestamp(LocalDateTime.now())
                        .build();

        failedRun =
                ExperimentRun.builder()
                        .id(4L)
                        .experiment(experiment)
                        .modelName("qwen2.5-coder:7b")
                        .iteration(3)
                        .status(RunStatus.FAILED)
                        .errorMessage("Connection timeout")
                        .timestamp(LocalDateTime.now())
                        .build();

        pendingRun =
                ExperimentRun.builder()
                        .id(5L)
                        .experiment(experiment)
                        .modelName("codellama:7b")
                        .iteration(2)
                        .status(RunStatus.PENDING)
                        .timestamp(LocalDateTime.now())
                        .build();
    }

    @Nested
    @DisplayName("getLeaderboard Tests")
    class GetLeaderboardTests {

        @Test
        @DisplayName("Should return empty leaderboard when no runs exist")
        void shouldReturnEmptyLeaderboardWhenNoRunsExist() {
            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll()).thenReturn(Collections.emptyList());

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertTrue(result.getEntries().isEmpty());
            assertEquals(0, result.getTotalRuns());
            verify(experimentRunRepository).findAll();
        }

        @Test
        @DisplayName("Should generate leaderboard with correct metrics")
        void shouldGenerateLeaderboardWithCorrectMetrics() {
            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3, failedRun));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(4, result.getTotalRuns());
            assertEquals(2, result.getEntries().size());

            // Find qwen entry
            LeaderboardEntry qwenEntry =
                    result.getEntries().stream()
                            .filter(e -> "qwen2.5-coder:7b".equals(e.getModelName()))
                            .findFirst()
                            .orElseThrow();

            assertEquals(3, qwenEntry.getRunCount());
            assertEquals(2, qwenEntry.getSuccessCount());
            assertEquals(47.75, qwenEntry.getAvgTokensPerSecond(), 0.01);
            assertEquals(45.5, qwenEntry.getMinTokensPerSecond(), 0.01);
            assertEquals(50.0, qwenEntry.getMaxTokensPerSecond(), 0.01);
        }

        @Test
        @DisplayName("Should filter by experiment ID")
        void shouldFilterByExperimentId() {
            LeaderboardFilter filter = LeaderboardFilter.builder().experimentId(1L).build();
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(successRun1, successRun2));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getTotalRuns());
            verify(experimentRunRepository).findByExperimentIdOrderByIterationAsc(1L);
        }

        @Test
        @DisplayName("Should filter by model name")
        void shouldFilterByModelName() {
            LeaderboardFilter filter =
                    LeaderboardFilter.builder().modelName("qwen2.5-coder:7b").build();
            when(experimentRunRepository.findByModelName("qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2, failedRun));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(3, result.getTotalRuns());
            assertEquals(1, result.getEntries().size());
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
        }

        @Test
        @DisplayName("Should filter by embedding model")
        void shouldFilterByEmbeddingModel() {
            ExperimentRun runWithEmbedding =
                    ExperimentRun.builder()
                            .id(10L)
                            .experiment(experiment)
                            .modelName("qwen2.5-coder:7b")
                            .embeddingModel(embeddingModel)
                            .iteration(1)
                            .status(RunStatus.SUCCESS)
                            .durationMs(2000L)
                            .tokensPerSecond(40.0)
                            .timestamp(LocalDateTime.now())
                            .build();

            LeaderboardFilter filter =
                    LeaderboardFilter.builder().embeddingModel("nomic-embed-text").build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, runWithEmbedding));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(1, result.getTotalRuns());
        }

        @Test
        @DisplayName("Should sort by TPS descending by default")
        void shouldSortByTpsDescendingByDefault() {
            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getEntries().size());
            // qwen has higher TPS (47.75) than codellama (35.0)
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
            assertEquals("codellama:7b", result.getEntries().get(1).getModelName());
        }

        @Test
        @DisplayName("Should sort by duration ascending")
        void shouldSortByDurationAscending() {
            LeaderboardFilter filter =
                    LeaderboardFilter.builder().sortBy("duration").sortOrder("asc").build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getEntries().size());
            // qwen has lower avg duration (2350) than codellama (3000)
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
            assertEquals("codellama:7b", result.getEntries().get(1).getModelName());
        }

        @Test
        @DisplayName("Should sort by success rate descending")
        void shouldSortBySuccessRateDescending() {
            LeaderboardFilter filter =
                    LeaderboardFilter.builder().sortBy("successRate").sortOrder("desc").build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3, failedRun));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getEntries().size());
            // codellama has 100% success rate, qwen has 66.67%
            assertEquals("codellama:7b", result.getEntries().get(0).getModelName());
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(1).getModelName());
        }

        @Test
        @DisplayName("Should sort by TTFT")
        void shouldSortByTtft() {
            LeaderboardFilter filter =
                    LeaderboardFilter.builder().sortBy("ttft").sortOrder("asc").build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getEntries().size());
            // qwen has lower avg TTFT (110) than codellama (150)
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
        }

        @Test
        @DisplayName("Should calculate correct success rate")
        void shouldCalculateCorrectSuccessRate() {
            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun2, failedRun));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            LeaderboardEntry entry = result.getEntries().get(0);
            assertEquals(3, entry.getRunCount());
            assertEquals(2, entry.getSuccessCount());
            assertEquals(66.67, entry.getSuccessRate(), 0.01);
        }

        @Test
        @DisplayName("Should handle runs with null metrics")
        void shouldHandleRunsWithNullMetrics() {
            ExperimentRun runWithNullMetrics =
                    ExperimentRun.builder()
                            .id(10L)
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .status(RunStatus.SUCCESS)
                            .timestamp(LocalDateTime.now())
                            .build();

            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Collections.singletonList(runWithNullMetrics));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(1, result.getEntries().size());
            LeaderboardEntry entry = result.getEntries().get(0);
            assertNull(entry.getAvgTokensPerSecond());
            assertNull(entry.getMinTokensPerSecond());
            assertNull(entry.getAvgDurationMs());
        }

        @Test
        @DisplayName("Should only include successful runs in entry statistics")
        void shouldOnlyIncludeSuccessfulRunsInStatistics() {
            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, failedRun, pendingRun));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            // Failed and pending runs should not contribute to the leaderboard entries
            // since we only group successful runs
            assertEquals(1, result.getEntries().size());
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
        }
    }

    @Nested
    @DisplayName("getModelMetrics Tests")
    class GetModelMetricsTests {

        @Test
        @DisplayName("Should return model metrics when runs exist")
        void shouldReturnModelMetricsWhenRunsExist() {
            when(experimentRunRepository.findByModelName("qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2, failedRun));

            ModelMetrics result = analyticsService.getModelMetrics("qwen2.5-coder:7b", null);

            assertNotNull(result);
            assertEquals("qwen2.5-coder:7b", result.getModelName());
            assertEquals(3, result.getTotalRuns());
            assertEquals(2, result.getSuccessfulRuns());
            assertEquals(1, result.getFailedRuns());
            assertEquals(66.67, result.getSuccessRate(), 0.01);
            assertEquals(47.75, result.getAvgTokensPerSecond(), 0.01);
            assertEquals(2350.0, result.getAvgDurationMs(), 0.01);
            assertEquals(110.0, result.getAvgTimeToFirstTokenMs(), 0.01);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when no runs found")
        void shouldThrowEntityNotFoundExceptionWhenNoRunsFound() {
            when(experimentRunRepository.findByModelName("nonexistent-model"))
                    .thenReturn(Collections.emptyList());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> analyticsService.getModelMetrics("nonexistent-model", null));

            assertEquals("No runs found for model: nonexistent-model", exception.getMessage());
        }

        @Test
        @DisplayName("Should filter by experiment ID when provided")
        void shouldFilterByExperimentIdWhenProvided() {
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2));

            ModelMetrics result = analyticsService.getModelMetrics("qwen2.5-coder:7b", 1L);

            assertNotNull(result);
            assertEquals(2, result.getTotalRuns());
            verify(experimentRunRepository).findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b");
        }

        @Test
        @DisplayName("Should group runs by iteration")
        void shouldGroupRunsByIteration() {
            when(experimentRunRepository.findByModelName("qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2, failedRun));

            ModelMetrics result = analyticsService.getModelMetrics("qwen2.5-coder:7b", null);

            assertNotNull(result.getRunsByIteration());
            assertEquals(3, result.getRunsByIteration().size());

            // Iteration 1
            List<RunSummary> iteration1 = result.getRunsByIteration().get(1);
            assertNotNull(iteration1);
            assertEquals(1, iteration1.size());
            assertEquals(1L, iteration1.get(0).getId());
            assertEquals(RunStatus.SUCCESS, iteration1.get(0).getStatus());

            // Iteration 2
            List<RunSummary> iteration2 = result.getRunsByIteration().get(2);
            assertNotNull(iteration2);
            assertEquals(1, iteration2.size());
            assertEquals(2L, iteration2.get(0).getId());

            // Iteration 3 (failed)
            List<RunSummary> iteration3 = result.getRunsByIteration().get(3);
            assertNotNull(iteration3);
            assertEquals(1, iteration3.size());
            assertEquals(RunStatus.FAILED, iteration3.get(0).getStatus());
        }

        @Test
        @DisplayName("Should handle runs with null metrics gracefully")
        void shouldHandleRunsWithNullMetricsGracefully() {
            ExperimentRun runWithNullMetrics =
                    ExperimentRun.builder()
                            .id(10L)
                            .experiment(experiment)
                            .modelName("test-model")
                            .iteration(1)
                            .status(RunStatus.SUCCESS)
                            .timestamp(LocalDateTime.now())
                            .build();

            when(experimentRunRepository.findByModelName("test-model"))
                    .thenReturn(Collections.singletonList(runWithNullMetrics));

            ModelMetrics result = analyticsService.getModelMetrics("test-model", null);

            assertNotNull(result);
            assertEquals(1, result.getTotalRuns());
            assertEquals(1, result.getSuccessfulRuns());
            assertNull(result.getAvgTokensPerSecond());
            assertNull(result.getAvgDurationMs());
            assertNull(result.getAvgTimeToFirstTokenMs());
        }

        @Test
        @DisplayName("Should calculate averages only from successful runs")
        void shouldCalculateAveragesOnlyFromSuccessfulRuns() {
            when(experimentRunRepository.findByModelName("qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2, failedRun));

            ModelMetrics result = analyticsService.getModelMetrics("qwen2.5-coder:7b", null);

            // avgTokensPerSecond should be (45.5 + 50.0) / 2 = 47.75
            assertEquals(47.75, result.getAvgTokensPerSecond(), 0.01);

            // Failed run should not contribute to averages
            assertEquals(2, result.getSuccessfulRuns());
        }

        @Test
        @DisplayName("Should handle all failed runs")
        void shouldHandleAllFailedRuns() {
            ExperimentRun failedRun2 =
                    ExperimentRun.builder()
                            .id(20L)
                            .experiment(experiment)
                            .modelName("failing-model")
                            .iteration(1)
                            .status(RunStatus.FAILED)
                            .errorMessage("Error")
                            .timestamp(LocalDateTime.now())
                            .build();

            when(experimentRunRepository.findByModelName("failing-model"))
                    .thenReturn(Collections.singletonList(failedRun2));

            ModelMetrics result = analyticsService.getModelMetrics("failing-model", null);

            assertNotNull(result);
            assertEquals(1, result.getTotalRuns());
            assertEquals(0, result.getSuccessfulRuns());
            assertEquals(1, result.getFailedRuns());
            assertEquals(0.0, result.getSuccessRate(), 0.01);
            assertNull(result.getAvgTokensPerSecond());
        }
    }

    @Nested
    @DisplayName("compareModels Tests")
    class CompareModelsTests {

        @Test
        @DisplayName("Should return empty map when no runs exist")
        void shouldReturnEmptyMapWhenNoRunsExist() {
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Collections.emptyList());

            Map<String, ModelMetrics> result = analyticsService.compareModels(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return metrics for all models in experiment")
        void shouldReturnMetricsForAllModelsInExperiment() {
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3));
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2));
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "codellama:7b"))
                    .thenReturn(Collections.singletonList(successRun3));

            Map<String, ModelMetrics> result = analyticsService.compareModels(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsKey("qwen2.5-coder:7b"));
            assertTrue(result.containsKey("codellama:7b"));

            ModelMetrics qwenMetrics = result.get("qwen2.5-coder:7b");
            assertEquals(2, qwenMetrics.getTotalRuns());
            assertEquals(47.75, qwenMetrics.getAvgTokensPerSecond(), 0.01);

            ModelMetrics llamaMetrics = result.get("codellama:7b");
            assertEquals(1, llamaMetrics.getTotalRuns());
            assertEquals(35.0, llamaMetrics.getAvgTokensPerSecond(), 0.01);
        }

        @Test
        @DisplayName("Should handle single model experiment")
        void shouldHandleSingleModelExperiment() {
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(successRun1, successRun2));
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, successRun2));

            Map<String, ModelMetrics> result = analyticsService.compareModels(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsKey("qwen2.5-coder:7b"));
        }

        @Test
        @DisplayName("Should include failed and pending runs in model list")
        void shouldIncludeFailedAndPendingRunsInModelList() {
            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(successRun1, failedRun, pendingRun));
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "qwen2.5-coder:7b"))
                    .thenReturn(Arrays.asList(successRun1, failedRun));
            when(experimentRunRepository.findByExperimentIdAndModelName(1L, "codellama:7b"))
                    .thenReturn(Collections.singletonList(pendingRun));

            Map<String, ModelMetrics> result = analyticsService.compareModels(1L);

            assertNotNull(result);
            assertEquals(2, result.size());

            ModelMetrics qwenMetrics = result.get("qwen2.5-coder:7b");
            assertEquals(2, qwenMetrics.getTotalRuns());
            assertEquals(1, qwenMetrics.getSuccessfulRuns());
            assertEquals(1, qwenMetrics.getFailedRuns());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle filter with both experiment and model name")
        void shouldHandleFilterWithBothExperimentAndModelName() {
            LeaderboardFilter filter =
                    LeaderboardFilter.builder()
                            .experimentId(1L)
                            .modelName("qwen2.5-coder:7b")
                            .build();

            when(experimentRunRepository.findByExperimentIdOrderByIterationAsc(1L))
                    .thenReturn(Arrays.asList(successRun1, successRun2, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            // Should only include qwen runs
            assertEquals(2, result.getTotalRuns());
            assertEquals(1, result.getEntries().size());
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
        }

        @Test
        @DisplayName("Should handle null sort parameters gracefully")
        void shouldHandleNullSortParametersGracefully() {
            LeaderboardFilter filter =
                    LeaderboardFilter.builder().sortBy(null).sortOrder(null).build();

            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            // Should default to TPS descending
            assertEquals("qwen2.5-coder:7b", result.getEntries().get(0).getModelName());
        }

        @Test
        @DisplayName("Should handle empty model name filter")
        void shouldHandleEmptyModelNameFilter() {
            LeaderboardFilter filter = LeaderboardFilter.builder().modelName("").build();

            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getTotalRuns());
        }

        @Test
        @DisplayName("Should handle whitespace model name filter")
        void shouldHandleWhitespaceModelNameFilter() {
            LeaderboardFilter filter = LeaderboardFilter.builder().modelName("   ").build();

            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, successRun3));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getTotalRuns());
        }

        @Test
        @DisplayName("Should handle runs with same TPS for sorting")
        void shouldHandleRunsWithSameTpsForSorting() {
            ExperimentRun runSameTps =
                    ExperimentRun.builder()
                            .id(10L)
                            .experiment(experiment)
                            .modelName("same-tps-model")
                            .iteration(1)
                            .status(RunStatus.SUCCESS)
                            .tokensPerSecond(45.5)
                            .timestamp(LocalDateTime.now())
                            .build();

            LeaderboardFilter filter = LeaderboardFilter.builder().build();
            when(experimentRunRepository.findAll())
                    .thenReturn(Arrays.asList(successRun1, runSameTps));

            LeaderboardData result = analyticsService.getLeaderboard(filter);

            assertNotNull(result);
            assertEquals(2, result.getEntries().size());
        }

        @Test
        @DisplayName("Should handle very large numbers in metrics")
        void shouldHandleVeryLargeNumbersInMetrics() {
            ExperimentRun largeMetricsRun =
                    ExperimentRun.builder()
                            .id(10L)
                            .experiment(experiment)
                            .modelName("large-metrics-model")
                            .iteration(1)
                            .status(RunStatus.SUCCESS)
                            .durationMs(Long.MAX_VALUE / 2)
                            .tokensPerSecond(Double.MAX_VALUE / 2)
                            .timeToFirstTokenMs(Long.MAX_VALUE / 2)
                            .timestamp(LocalDateTime.now())
                            .build();

            when(experimentRunRepository.findByModelName("large-metrics-model"))
                    .thenReturn(Collections.singletonList(largeMetricsRun));

            ModelMetrics result = analyticsService.getModelMetrics("large-metrics-model", null);

            assertNotNull(result);
            assertNotNull(result.getAvgTokensPerSecond());
            assertNotNull(result.getAvgDurationMs());
        }

        @Test
        @DisplayName("Should correctly convert RunSummary from ExperimentRun")
        void shouldCorrectlyConvertRunSummaryFromExperimentRun() {
            when(experimentRunRepository.findByModelName("qwen2.5-coder:7b"))
                    .thenReturn(Collections.singletonList(successRun1));

            ModelMetrics result = analyticsService.getModelMetrics("qwen2.5-coder:7b", null);

            RunSummary summary = result.getRunsByIteration().get(1).get(0);
            assertEquals(1L, summary.getId());
            assertEquals(RunStatus.SUCCESS, summary.getStatus());
            assertEquals(2500L, summary.getDurationMs());
            assertEquals(45.5, summary.getTokensPerSecond(), 0.01);
        }
    }

    @Nested
    @DisplayName("RunSummary Conversion Tests")
    class RunSummaryConversionTests {

        @Test
        @DisplayName("Should preserve all fields when converting to RunSummary")
        void shouldPreserveAllFieldsWhenConvertingToRunSummary() {
            when(experimentRunRepository.findByModelName("qwen2.5-coder:7b"))
                    .thenReturn(Collections.singletonList(successRun1));

            ModelMetrics result = analyticsService.getModelMetrics("qwen2.5-coder:7b", null);

            List<RunSummary> summaries = result.getRunsByIteration().get(1);
            assertNotNull(summaries);
            assertEquals(1, summaries.size());

            RunSummary summary = summaries.get(0);
            assertEquals(successRun1.getId(), summary.getId());
            assertEquals(successRun1.getStatus(), summary.getStatus());
            assertEquals(successRun1.getDurationMs(), summary.getDurationMs());
            assertEquals(successRun1.getTokensPerSecond(), summary.getTokensPerSecond());
        }

        @Test
        @DisplayName("Should handle null fields in RunSummary")
        void shouldHandleNullFieldsInRunSummary() {
            ExperimentRun pendingWithNulls =
                    ExperimentRun.builder()
                            .id(100L)
                            .experiment(experiment)
                            .modelName("pending-model")
                            .iteration(1)
                            .status(RunStatus.PENDING)
                            .timestamp(LocalDateTime.now())
                            .build();

            when(experimentRunRepository.findByModelName("pending-model"))
                    .thenReturn(Collections.singletonList(pendingWithNulls));

            ModelMetrics result = analyticsService.getModelMetrics("pending-model", null);

            RunSummary summary = result.getRunsByIteration().get(1).get(0);
            assertEquals(100L, summary.getId());
            assertEquals(RunStatus.PENDING, summary.getStatus());
            assertNull(summary.getDurationMs());
            assertNull(summary.getTokensPerSecond());
        }
    }
}
