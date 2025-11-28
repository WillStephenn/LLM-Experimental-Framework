package com.locallab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.RunStatus;

/**
 * Repository interface for {@link ExperimentRun} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} along with custom query methods
 * for experiment filtering, status filtering, model filtering, and analytics aggregations. Spring
 * Data JPA auto-implements the derived query methods.
 *
 * <p>The custom query methods enable:
 *
 * <ul>
 *   <li><strong>Experiment filtering:</strong> Find runs belonging to a specific experiment,
 *       ordered by iteration ascending.
 *   <li><strong>Status filtering:</strong> Find runs by experiment ID and run status for monitoring
 *       progress.
 *   <li><strong>Model filtering:</strong> Find all runs that used a specific model name for
 *       cross-experiment analysis.
 *   <li><strong>Analytics aggregations:</strong> Calculate average tokens per second and duration
 *       for successful runs within an experiment.
 * </ul>
 *
 * @see ExperimentRun
 * @see RunStatus
 */
@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {

    /**
     * Finds all runs for a specific experiment, ordered by iteration ascending.
     *
     * <p>This method retrieves all runs associated with an experiment in iteration order, which is
     * useful for displaying run history and tracking experiment progress.
     *
     * <p>For example, to retrieve all runs for an experiment:
     *
     * <pre>{@code
     * List<ExperimentRun> runs =
     *     experimentRunRepository.findByExperimentIdOrderByIterationAsc(experiment.getId());
     * }</pre>
     *
     * @param experimentId the ID of the experiment to filter by
     * @return a list of experiment runs ordered by iteration ascending, or an empty list if none
     *     found
     */
    List<ExperimentRun> findByExperimentIdOrderByIterationAsc(Long experimentId);

    /**
     * Finds all runs for a specific experiment with a given status.
     *
     * <p>This method enables filtering runs by both experiment and status, which is useful for
     * monitoring experiment progress (e.g., finding all completed or failed runs).
     *
     * <p>For example, to find all successful runs in an experiment:
     *
     * <pre>{@code
     * List<ExperimentRun> successfulRuns =
     *     experimentRunRepository.findByExperimentIdAndStatus(
     *         experiment.getId(), RunStatus.SUCCESS);
     * }</pre>
     *
     * @param experimentId the ID of the experiment to filter by
     * @param status the run status to filter by
     * @return a list of experiment runs matching both criteria, or an empty list if none found
     */
    List<ExperimentRun> findByExperimentIdAndStatus(Long experimentId, RunStatus status);

    /**
     * Finds all runs that used a specific model name.
     *
     * <p>This method enables cross-experiment analysis by model, allowing comparison of a model's
     * performance across different experiments and configurations.
     *
     * <p>For example, to find all runs using a specific model:
     *
     * <pre>{@code
     * List<ExperimentRun> modelRuns =
     *     experimentRunRepository.findByModelName("qwen2.5-coder:7b");
     * }</pre>
     *
     * @param modelName the name of the model to filter by
     * @return a list of experiment runs using the specified model, or an empty list if none found
     */
    List<ExperimentRun> findByModelName(String modelName);

    /**
     * Calculates the average tokens per second for successful runs in an experiment.
     *
     * <p>This aggregation query computes the mean throughput metric across all successful runs,
     * providing insight into overall model performance within an experiment. Only runs with status
     * {@link RunStatus#SUCCESS} are included.
     *
     * <p>For example, to get average throughput for an experiment:
     *
     * <pre>{@code
     * Double avgTps = experimentRunRepository.getAverageTokensPerSecond(experiment.getId());
     * if (avgTps != null) {
     *     System.out.println("Average TPS: " + avgTps);
     * }
     * }</pre>
     *
     * @param expId the ID of the experiment to aggregate
     * @return the average tokens per second, or {@code null} if no successful runs exist
     */
    @Query(
            "SELECT AVG(r.tokensPerSecond) FROM ExperimentRun r "
                    + "WHERE r.experiment.id = :expId AND r.status = 'SUCCESS'")
    Double getAverageTokensPerSecond(@Param("expId") Long expId);

    /**
     * Calculates the average duration in milliseconds for successful runs in an experiment.
     *
     * <p>This aggregation query computes the mean generation time across all successful runs,
     * providing insight into overall timing performance within an experiment. Only runs with status
     * {@link RunStatus#SUCCESS} are included.
     *
     * <p>For example, to get average duration for an experiment:
     *
     * <pre>{@code
     * Double avgDuration = experimentRunRepository.getAverageDuration(experiment.getId());
     * if (avgDuration != null) {
     *     System.out.println("Average duration: " + avgDuration + " ms");
     * }
     * }</pre>
     *
     * @param expId the ID of the experiment to aggregate
     * @return the average duration in milliseconds, or {@code null} if no successful runs exist
     */
    @Query(
            "SELECT AVG(r.durationMs) FROM ExperimentRun r "
                    + "WHERE r.experiment.id = :expId AND r.status = 'SUCCESS'")
    Double getAverageDuration(@Param("expId") Long expId);
}
