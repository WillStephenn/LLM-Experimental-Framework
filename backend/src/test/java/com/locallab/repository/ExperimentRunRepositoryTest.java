package com.locallab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.locallab.model.Experiment;
import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.model.enums.RunStatus;

/**
 * Integration tests for {@link ExperimentRunRepository}.
 *
 * <p>Uses {@code @DataJpaTest} to configure an in-memory H2 database and auto-configure Spring Data
 * JPA repositories. Tests verify custom query methods only; standard JPA CRUD operations are not
 * tested as they are already tested by Spring Data JPA.
 *
 * @see ExperimentRunRepository
 * @see ExperimentRun
 */
@DataJpaTest
class ExperimentRunRepositoryTest {

    @Autowired private ExperimentRunRepository experimentRunRepository;

    @Autowired private TestEntityManager entityManager;

    private Experiment experiment1;
    private Experiment experiment2;
    private ExperimentRun successRun1;
    private ExperimentRun successRun2;
    private ExperimentRun successRun3;
    private ExperimentRun failedRun;
    private ExperimentRun pendingRun;
    private ExperimentRun runningRun;
    private ExperimentRun experiment2Run;

    @BeforeEach
    void setUp() {
        experiment1 =
                Experiment.builder()
                        .name("Test Experiment 1")
                        .status(ExperimentStatus.RUNNING)
                        .config("{\"models\": [\"qwen2.5-coder:7b\", \"codellama:7b\"]}")
                        .build();

        experiment2 =
                Experiment.builder()
                        .name("Test Experiment 2")
                        .status(ExperimentStatus.COMPLETED)
                        .config("{\"models\": [\"mistral\"]}")
                        .build();

        entityManager.persist(experiment1);
        entityManager.persist(experiment2);

        // Experiment 1 runs
        successRun1 =
                ExperimentRun.builder()
                        .experiment(experiment1)
                        .modelName("qwen2.5-coder:7b")
                        .iteration(1)
                        .status(RunStatus.SUCCESS)
                        .durationMs(2500L)
                        .tokensPerSecond(45.5)
                        .output("Generated output 1")
                        .build();

        successRun2 =
                ExperimentRun.builder()
                        .experiment(experiment1)
                        .modelName("qwen2.5-coder:7b")
                        .iteration(2)
                        .status(RunStatus.SUCCESS)
                        .durationMs(2300L)
                        .tokensPerSecond(48.0)
                        .output("Generated output 2")
                        .build();

        successRun3 =
                ExperimentRun.builder()
                        .experiment(experiment1)
                        .modelName("codellama:7b")
                        .iteration(1)
                        .status(RunStatus.SUCCESS)
                        .durationMs(2700L)
                        .tokensPerSecond(42.0)
                        .output("Generated output 3")
                        .build();

        failedRun =
                ExperimentRun.builder()
                        .experiment(experiment1)
                        .modelName("codellama:7b")
                        .iteration(2)
                        .status(RunStatus.FAILED)
                        .errorMessage("Model timeout")
                        .build();

        pendingRun =
                ExperimentRun.builder()
                        .experiment(experiment1)
                        .modelName("qwen2.5-coder:7b")
                        .iteration(3)
                        .status(RunStatus.PENDING)
                        .build();

        runningRun =
                ExperimentRun.builder()
                        .experiment(experiment1)
                        .modelName("codellama:7b")
                        .iteration(3)
                        .status(RunStatus.RUNNING)
                        .build();

        // Experiment 2 runs
        experiment2Run =
                ExperimentRun.builder()
                        .experiment(experiment2)
                        .modelName("mistral")
                        .iteration(1)
                        .status(RunStatus.SUCCESS)
                        .durationMs(3000L)
                        .tokensPerSecond(35.0)
                        .output("Mistral output")
                        .build();

        entityManager.persist(successRun1);
        entityManager.persist(successRun2);
        entityManager.persist(successRun3);
        entityManager.persist(failedRun);
        entityManager.persist(pendingRun);
        entityManager.persist(runningRun);
        entityManager.persist(experiment2Run);
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByExperimentIdOrderByIterationAsc Tests")
    class FindByExperimentIdOrderByIterationAscTests {

        @Test
        @DisplayName("Should find all runs for experiment ordered by iteration")
        void shouldFindAllRunsForExperimentOrderedByIteration() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdOrderByIterationAsc(
                            experiment1.getId());

            assertEquals(6, results.size());
            // Verify ordering by iteration
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).getIteration() <= results.get(i + 1).getIteration());
            }
        }

        @Test
        @DisplayName("Should find runs for different experiment")
        void shouldFindRunsForDifferentExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdOrderByIterationAsc(
                            experiment2.getId());

            assertEquals(1, results.size());
            assertEquals("mistral", results.getFirst().getModelName());
        }

        @Test
        @DisplayName("Should return empty list for non-existent experiment")
        void shouldReturnEmptyListForNonExistentExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdOrderByIterationAsc(99999L);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should include runs with all statuses")
        void shouldIncludeRunsWithAllStatuses() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdOrderByIterationAsc(
                            experiment1.getId());

            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.SUCCESS));
            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.FAILED));
            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.PENDING));
            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.RUNNING));
        }

        @Test
        @DisplayName(
                "Should return runs ordered by iteration ascending with same iterations grouped")
        void shouldReturnRunsOrderedByIterationAscendingWithSameIterationsGrouped() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdOrderByIterationAsc(
                            experiment1.getId());

            // Iteration 1 runs should come before iteration 2 runs
            int lastSeenIteration = 0;
            for (ExperimentRun run : results) {
                assertTrue(run.getIteration() >= lastSeenIteration);
                lastSeenIteration = run.getIteration();
            }
        }
    }

    @Nested
    @DisplayName("findByExperimentIdAndStatus Tests")
    class FindByExperimentIdAndStatusTests {

        @Test
        @DisplayName("Should find successful runs for experiment")
        void shouldFindSuccessfulRunsForExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(
                            experiment1.getId(), RunStatus.SUCCESS);

            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(r -> r.getStatus() == RunStatus.SUCCESS));
        }

        @Test
        @DisplayName("Should find failed runs for experiment")
        void shouldFindFailedRunsForExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(
                            experiment1.getId(), RunStatus.FAILED);

            assertEquals(1, results.size());
            assertEquals(RunStatus.FAILED, results.getFirst().getStatus());
            assertEquals("codellama:7b", results.getFirst().getModelName());
        }

        @Test
        @DisplayName("Should find pending runs for experiment")
        void shouldFindPendingRunsForExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(
                            experiment1.getId(), RunStatus.PENDING);

            assertEquals(1, results.size());
            assertEquals(RunStatus.PENDING, results.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should find running runs for experiment")
        void shouldFindRunningRunsForExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(
                            experiment1.getId(), RunStatus.RUNNING);

            assertEquals(1, results.size());
            assertEquals(RunStatus.RUNNING, results.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should return empty list when no runs match status")
        void shouldReturnEmptyListWhenNoRunsMatchStatus() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(
                            experiment2.getId(), RunStatus.FAILED);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for non-existent experiment")
        void shouldReturnEmptyListForNonExistentExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(99999L, RunStatus.SUCCESS);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should only return runs from specified experiment")
        void shouldOnlyReturnRunsFromSpecifiedExperiment() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByExperimentIdAndStatus(
                            experiment2.getId(), RunStatus.SUCCESS);

            assertEquals(1, results.size());
            assertEquals(experiment2.getId(), results.getFirst().getExperiment().getId());
        }
    }

    @Nested
    @DisplayName("findByModelName Tests")
    class FindByModelNameTests {

        @Test
        @DisplayName("Should find all runs for qwen model")
        void shouldFindAllRunsForQwenModel() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByModelName("qwen2.5-coder:7b");

            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(r -> r.getModelName().equals("qwen2.5-coder:7b")));
        }

        @Test
        @DisplayName("Should find all runs for codellama model")
        void shouldFindAllRunsForCodellamaModel() {
            List<ExperimentRun> results = experimentRunRepository.findByModelName("codellama:7b");

            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(r -> r.getModelName().equals("codellama:7b")));
        }

        @Test
        @DisplayName("Should find runs across multiple experiments")
        void shouldFindRunsAcrossMultipleExperiments() {
            // Add another run with qwen model in experiment2
            ExperimentRun crossExperimentRun =
                    ExperimentRun.builder()
                            .experiment(experiment2)
                            .modelName("qwen2.5-coder:7b")
                            .iteration(1)
                            .status(RunStatus.SUCCESS)
                            .build();
            entityManager.persist(crossExperimentRun);
            entityManager.flush();

            List<ExperimentRun> results =
                    experimentRunRepository.findByModelName("qwen2.5-coder:7b");

            assertEquals(4, results.size());
            assertTrue(
                    results.stream()
                            .anyMatch(r -> r.getExperiment().getId().equals(experiment1.getId())));
            assertTrue(
                    results.stream()
                            .anyMatch(r -> r.getExperiment().getId().equals(experiment2.getId())));
        }

        @Test
        @DisplayName("Should return empty list for non-existent model")
        void shouldReturnEmptyListForNonExistentModel() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByModelName("non-existent-model");

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should be case-sensitive for model names")
        void shouldBeCaseSensitiveForModelNames() {
            List<ExperimentRun> results =
                    experimentRunRepository.findByModelName("QWEN2.5-CODER:7B");

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should include runs with all statuses")
        void shouldIncludeRunsWithAllStatuses() {
            List<ExperimentRun> results = experimentRunRepository.findByModelName("codellama:7b");

            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.SUCCESS));
            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.FAILED));
            assertTrue(results.stream().anyMatch(r -> r.getStatus() == RunStatus.RUNNING));
        }
    }

    @Nested
    @DisplayName("getAverageTokensPerSecond Tests")
    class GetAverageTokensPerSecondTests {

        @Test
        @DisplayName("Should calculate average tokens per second for successful runs")
        void shouldCalculateAverageTokensPerSecondForSuccessfulRuns() {
            Double avgTps = experimentRunRepository.getAverageTokensPerSecond(experiment1.getId());

            assertNotNull(avgTps);
            // Average of 45.5, 48.0, and 42.0 = 135.5 / 3 = 45.166...
            assertEquals(45.166, avgTps, 0.01);
        }

        @Test
        @DisplayName("Should return null when no successful runs exist")
        void shouldReturnNullWhenNoSuccessfulRunsExist() {
            // Create an experiment with only failed runs
            Experiment failedExperiment =
                    Experiment.builder()
                            .name("Failed Experiment")
                            .status(ExperimentStatus.FAILED)
                            .config("{}")
                            .build();
            entityManager.persist(failedExperiment);

            ExperimentRun failedOnly =
                    ExperimentRun.builder()
                            .experiment(failedExperiment)
                            .modelName("test-model")
                            .iteration(1)
                            .status(RunStatus.FAILED)
                            .errorMessage("Test failure")
                            .build();
            entityManager.persist(failedOnly);
            entityManager.flush();

            Double avgTps =
                    experimentRunRepository.getAverageTokensPerSecond(failedExperiment.getId());

            assertNull(avgTps);
        }

        @Test
        @DisplayName("Should return null for non-existent experiment")
        void shouldReturnNullForNonExistentExperiment() {
            Double avgTps = experimentRunRepository.getAverageTokensPerSecond(99999L);

            assertNull(avgTps);
        }

        @Test
        @DisplayName("Should exclude failed runs from calculation")
        void shouldExcludeFailedRunsFromCalculation() {
            // The failed run has no tokensPerSecond, but should be excluded anyway
            Double avgTps = experimentRunRepository.getAverageTokensPerSecond(experiment1.getId());

            assertNotNull(avgTps);
            // Should only include the 3 successful runs
            assertEquals(45.166, avgTps, 0.01);
        }

        @Test
        @DisplayName("Should handle single successful run")
        void shouldHandleSingleSuccessfulRun() {
            Double avgTps = experimentRunRepository.getAverageTokensPerSecond(experiment2.getId());

            assertNotNull(avgTps);
            assertEquals(35.0, avgTps, 0.01);
        }

        @Test
        @DisplayName("Should exclude pending and running runs from calculation")
        void shouldExcludePendingAndRunningRunsFromCalculation() {
            // Pending and running runs should not affect the average
            Double avgTps = experimentRunRepository.getAverageTokensPerSecond(experiment1.getId());

            assertNotNull(avgTps);
            // Only 3 successful runs should be included
            assertEquals(45.166, avgTps, 0.01);
        }
    }

    @Nested
    @DisplayName("getAverageDuration Tests")
    class GetAverageDurationTests {

        @Test
        @DisplayName("Should calculate average duration for successful runs")
        void shouldCalculateAverageDurationForSuccessfulRuns() {
            Double avgDuration = experimentRunRepository.getAverageDuration(experiment1.getId());

            assertNotNull(avgDuration);
            // Average of 2500, 2300, and 2700 = 7500 / 3 = 2500.0
            assertEquals(2500.0, avgDuration, 0.01);
        }

        @Test
        @DisplayName("Should return null when no successful runs exist")
        void shouldReturnNullWhenNoSuccessfulRunsExist() {
            // Create an experiment with only pending runs
            Experiment pendingExperiment =
                    Experiment.builder()
                            .name("Pending Experiment")
                            .status(ExperimentStatus.DRAFT)
                            .config("{}")
                            .build();
            entityManager.persist(pendingExperiment);

            ExperimentRun pendingOnly =
                    ExperimentRun.builder()
                            .experiment(pendingExperiment)
                            .modelName("test-model")
                            .iteration(1)
                            .status(RunStatus.PENDING)
                            .build();
            entityManager.persist(pendingOnly);
            entityManager.flush();

            Double avgDuration =
                    experimentRunRepository.getAverageDuration(pendingExperiment.getId());

            assertNull(avgDuration);
        }

        @Test
        @DisplayName("Should return null for non-existent experiment")
        void shouldReturnNullForNonExistentExperiment() {
            Double avgDuration = experimentRunRepository.getAverageDuration(99999L);

            assertNull(avgDuration);
        }

        @Test
        @DisplayName("Should exclude failed runs from calculation")
        void shouldExcludeFailedRunsFromCalculation() {
            Double avgDuration = experimentRunRepository.getAverageDuration(experiment1.getId());

            assertNotNull(avgDuration);
            // Should only include the 3 successful runs
            assertEquals(2500.0, avgDuration, 0.01);
        }

        @Test
        @DisplayName("Should handle single successful run")
        void shouldHandleSingleSuccessfulRun() {
            Double avgDuration = experimentRunRepository.getAverageDuration(experiment2.getId());

            assertNotNull(avgDuration);
            assertEquals(3000.0, avgDuration, 0.01);
        }

        @Test
        @DisplayName("Should exclude pending and running runs from calculation")
        void shouldExcludePendingAndRunningRunsFromCalculation() {
            Double avgDuration = experimentRunRepository.getAverageDuration(experiment1.getId());

            assertNotNull(avgDuration);
            // Only 3 successful runs should be included
            assertEquals(2500.0, avgDuration, 0.01);
        }

        @Test
        @DisplayName("Should return null when experiment has no runs")
        void shouldReturnNullWhenExperimentHasNoRuns() {
            Experiment emptyExperiment =
                    Experiment.builder()
                            .name("Empty Experiment")
                            .status(ExperimentStatus.DRAFT)
                            .config("{}")
                            .build();
            entityManager.persist(emptyExperiment);
            entityManager.flush();

            Double avgDuration =
                    experimentRunRepository.getAverageDuration(emptyExperiment.getId());

            assertNull(avgDuration);
        }
    }
}
