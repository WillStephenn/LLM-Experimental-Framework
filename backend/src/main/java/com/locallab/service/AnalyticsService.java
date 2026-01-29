package com.locallab.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.locallab.dto.LeaderboardData;
import com.locallab.dto.LeaderboardEntry;
import com.locallab.dto.LeaderboardFilter;
import com.locallab.dto.ModelMetrics;
import com.locallab.dto.RunSummary;
import com.locallab.model.ExperimentRun;
import com.locallab.model.enums.RunStatus;
import com.locallab.repository.ExperimentRunRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service for aggregating and analysing experiment run metrics.
 *
 * <p>Provides leaderboard generation, model-specific metrics, and cross-model comparison
 * capabilities. All operations are read-only and transactional.
 *
 * <h3>Key Features:</h3>
 *
 * <ul>
 *   <li>Leaderboard generation with filtering by experiment, model, and embedding model
 *   <li>Sorting by various metrics (TPS, duration, TTFT, success rate)
 *   <li>Detailed per-model metrics with iteration breakdowns
 *   <li>Cross-model comparison within experiments
 * </ul>
 *
 * <h3>Exception Handling:</h3>
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} - Thrown when no runs are found for a specified model
 * </ul>
 *
 * @author William Stephen
 * @see LeaderboardData
 * @see ModelMetrics
 * @see ExperimentRunRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsService.class);

    private final ExperimentRunRepository experimentRunRepository;

    /**
     * Generates leaderboard data based on filter criteria.
     *
     * <p>Aggregates metrics across experiment runs, grouping by model name. Results are sorted
     * according to the filter's sortBy and sortOrder parameters.
     *
     * <h3>Filtering:</h3>
     *
     * <ul>
     *   <li>If experimentId is provided, only runs from that experiment are included
     *   <li>If modelName is provided, only runs using that model are included
     *   <li>If embeddingModel is provided, only runs using that embedding model are included
     * </ul>
     *
     * @param filter the filter criteria for generating the leaderboard
     * @return leaderboard data with ranked models, or empty leaderboard if no runs match
     */
    public LeaderboardData getLeaderboard(LeaderboardFilter filter) {
        LOGGER.debug("Generating leaderboard with filter: {}", filter);

        List<ExperimentRun> runs = fetchFilteredRuns(filter);

        if (runs.isEmpty()) {
            LOGGER.debug("No runs found matching filter criteria");
            return LeaderboardData.builder().entries(Collections.emptyList()).totalRuns(0).build();
        }

        // Group by model and calculate metrics
        Map<String, List<ExperimentRun>> runsByModel =
                runs.stream()
                        .filter(r -> r.getStatus() == RunStatus.SUCCESS)
                        .collect(Collectors.groupingBy(ExperimentRun::getModelName));

        List<LeaderboardEntry> entries =
                runsByModel.entrySet().stream()
                        .map(
                                entry ->
                                        calculateLeaderboardEntry(
                                                entry.getKey(), entry.getValue(), runs))
                        .sorted(getComparator(filter.getSortBy(), filter.getSortOrder()))
                        .toList();

        LOGGER.debug(
                "Generated leaderboard with {} entries from {} total runs",
                entries.size(),
                runs.size());

        return LeaderboardData.builder().entries(entries).totalRuns(runs.size()).build();
    }

    /**
     * Gets detailed metrics for a specific model.
     *
     * <p>Calculates comprehensive metrics including run counts, success rates, average performance
     * metrics, and per-iteration breakdowns.
     *
     * @param modelName the model name to get metrics for
     * @param experimentId optional experiment filter; if null, metrics span all experiments
     * @return detailed model metrics
     * @throws EntityNotFoundException if no runs are found for the specified model
     */
    public ModelMetrics getModelMetrics(String modelName, Long experimentId) {
        LOGGER.debug("Getting metrics for model: {}, experimentId: {}", modelName, experimentId);

        List<ExperimentRun> runs;

        if (experimentId != null) {
            runs = experimentRunRepository.findByExperimentIdAndModelName(experimentId, modelName);
        } else {
            runs = experimentRunRepository.findByModelName(modelName);
        }

        if (runs.isEmpty()) {
            LOGGER.warn("No runs found for model: {}", modelName);
            throw new EntityNotFoundException("No runs found for model: " + modelName);
        }

        List<ExperimentRun> successRuns =
                runs.stream().filter(r -> r.getStatus() == RunStatus.SUCCESS).toList();

        int failedRuns = (int) runs.stream().filter(r -> r.getStatus() == RunStatus.FAILED).count();

        Map<Integer, List<RunSummary>> runsByIteration = groupByIteration(runs);

        LOGGER.debug(
                "Calculated metrics for model {} - {} total runs, {} successful, {} failed",
                modelName,
                runs.size(),
                successRuns.size(),
                failedRuns);

        return ModelMetrics.builder()
                .modelName(modelName)
                .totalRuns(runs.size())
                .successfulRuns(successRuns.size())
                .failedRuns(failedRuns)
                .successRate((double) successRuns.size() / runs.size() * 100)
                .avgTokensPerSecond(
                        calculateAverage(successRuns, ExperimentRun::getTokensPerSecond))
                .avgDurationMs(calculateAverageLong(successRuns, ExperimentRun::getDurationMs))
                .avgTimeToFirstTokenMs(
                        calculateAverageLong(successRuns, ExperimentRun::getTimeToFirstTokenMs))
                .runsByIteration(runsByIteration)
                .build();
    }

    /**
     * Compares all models within an experiment.
     *
     * <p>Returns metrics for each unique model used in the experiment, enabling side-by-side
     * comparison of model performance.
     *
     * @param experimentId the experiment to analyse
     * @return map of model name to metrics; empty map if no runs exist
     */
    public Map<String, ModelMetrics> compareModels(Long experimentId) {
        LOGGER.debug("Comparing models for experiment: {}", experimentId);

        List<ExperimentRun> runs =
                experimentRunRepository.findByExperimentIdOrderByIterationAsc(experimentId);

        if (runs.isEmpty()) {
            LOGGER.debug("No runs found for experiment: {}", experimentId);
            return Collections.emptyMap();
        }

        Set<String> modelNames =
                runs.stream().map(ExperimentRun::getModelName).collect(Collectors.toSet());

        Map<String, ModelMetrics> comparison = new LinkedHashMap<>();
        for (String modelName : modelNames) {
            comparison.put(modelName, getModelMetrics(modelName, experimentId));
        }

        LOGGER.debug("Compared {} models for experiment {}", comparison.size(), experimentId);
        return comparison;
    }

    /**
     * Fetches runs matching the filter criteria.
     *
     * @param filter the filter to apply
     * @return list of matching runs
     */
    private List<ExperimentRun> fetchFilteredRuns(LeaderboardFilter filter) {
        List<ExperimentRun> runs;

        if (filter.getExperimentId() != null) {
            runs =
                    experimentRunRepository.findByExperimentIdOrderByIterationAsc(
                            filter.getExperimentId());
        } else if (filter.getModelName() != null && !filter.getModelName().isBlank()) {
            runs = experimentRunRepository.findByModelName(filter.getModelName());
        } else {
            runs = experimentRunRepository.findAll();
        }

        // Apply additional filters
        if (filter.getModelName() != null
                && !filter.getModelName().isBlank()
                && filter.getExperimentId() != null) {
            runs =
                    runs.stream()
                            .filter(r -> filter.getModelName().equals(r.getModelName()))
                            .toList();
        }

        if (filter.getEmbeddingModel() != null && !filter.getEmbeddingModel().isBlank()) {
            runs =
                    runs.stream()
                            .filter(
                                    r ->
                                            r.getEmbeddingModel() != null
                                                    && filter.getEmbeddingModel()
                                                            .equals(
                                                                    r.getEmbeddingModel()
                                                                            .getOllamaModelName()))
                            .toList();
        }

        return runs;
    }

    /**
     * Calculates a leaderboard entry for a specific model.
     *
     * @param modelName the model name
     * @param successRuns list of successful runs for this model
     * @param allRuns list of all runs (for total count calculation)
     * @return the calculated leaderboard entry
     */
    private LeaderboardEntry calculateLeaderboardEntry(
            String modelName, List<ExperimentRun> successRuns, List<ExperimentRun> allRuns) {

        long totalRunsForModel =
                allRuns.stream().filter(r -> r.getModelName().equals(modelName)).count();

        DoubleSummaryStatistics tpsStats =
                successRuns.stream()
                        .filter(r -> r.getTokensPerSecond() != null)
                        .mapToDouble(ExperimentRun::getTokensPerSecond)
                        .summaryStatistics();

        LongSummaryStatistics durationStats =
                successRuns.stream()
                        .filter(r -> r.getDurationMs() != null)
                        .mapToLong(ExperimentRun::getDurationMs)
                        .summaryStatistics();

        LongSummaryStatistics ttftStats =
                successRuns.stream()
                        .filter(r -> r.getTimeToFirstTokenMs() != null)
                        .mapToLong(ExperimentRun::getTimeToFirstTokenMs)
                        .summaryStatistics();

        return LeaderboardEntry.builder()
                .modelName(modelName)
                .runCount((int) totalRunsForModel)
                .successCount(successRuns.size())
                .successRate((double) successRuns.size() / totalRunsForModel * 100)
                .avgTokensPerSecond(tpsStats.getCount() > 0 ? tpsStats.getAverage() : null)
                .minTokensPerSecond(tpsStats.getCount() > 0 ? tpsStats.getMin() : null)
                .maxTokensPerSecond(tpsStats.getCount() > 0 ? tpsStats.getMax() : null)
                .avgDurationMs(durationStats.getCount() > 0 ? durationStats.getAverage() : null)
                .minDurationMs(durationStats.getCount() > 0 ? durationStats.getMin() : null)
                .maxDurationMs(durationStats.getCount() > 0 ? durationStats.getMax() : null)
                .avgTimeToFirstTokenMs(ttftStats.getCount() > 0 ? ttftStats.getAverage() : null)
                .build();
    }

    /**
     * Gets a comparator for sorting leaderboard entries.
     *
     * @param sortBy the field to sort by
     * @param sortOrder the sort direction ("asc" or "desc")
     * @return comparator for sorting entries
     */
    private Comparator<LeaderboardEntry> getComparator(String sortBy, String sortOrder) {
        String effectiveSortBy = sortBy != null ? sortBy : "tps";
        Comparator<LeaderboardEntry> comparator;

        if ("duration".equals(effectiveSortBy)) {
            comparator =
                    Comparator.comparing(
                            LeaderboardEntry::getAvgDurationMs,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("ttft".equals(effectiveSortBy)) {
            comparator =
                    Comparator.comparing(
                            LeaderboardEntry::getAvgTimeToFirstTokenMs,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("successRate".equals(effectiveSortBy)) {
            comparator =
                    Comparator.comparing(
                            LeaderboardEntry::getSuccessRate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            comparator =
                    Comparator.comparing(
                            LeaderboardEntry::getAvgTokensPerSecond,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            return comparator;
        }
        return comparator.reversed();
    }

    /**
     * Calculates the average of a Double field across runs.
     *
     * @param runs the runs to aggregate
     * @param getter the getter function for the field
     * @return the average, or null if no valid values exist
     */
    private Double calculateAverage(
            List<ExperimentRun> runs, Function<ExperimentRun, Double> getter) {

        List<Double> values = runs.stream().map(getter).filter(v -> v != null).toList();

        if (values.isEmpty()) {
            return null;
        }

        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * Calculates the average of a Long field across runs, returning as Double.
     *
     * @param runs the runs to aggregate
     * @param getter the getter function for the field
     * @return the average as Double, or null if no valid values exist
     */
    private Double calculateAverageLong(
            List<ExperimentRun> runs, Function<ExperimentRun, Long> getter) {

        List<Long> values = runs.stream().map(getter).filter(v -> v != null).toList();

        if (values.isEmpty()) {
            return null;
        }

        return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * Groups runs by iteration number, converting to RunSummary objects.
     *
     * @param runs the runs to group
     * @return map of iteration number to list of run summaries
     */
    private Map<Integer, List<RunSummary>> groupByIteration(List<ExperimentRun> runs) {
        return runs.stream()
                .collect(
                        Collectors.groupingBy(
                                ExperimentRun::getIteration,
                                LinkedHashMap::new,
                                Collectors.mapping(this::toRunSummary, Collectors.toList())));
    }

    /**
     * Converts an ExperimentRun to a RunSummary.
     *
     * @param run the run to convert
     * @return the run summary
     */
    private RunSummary toRunSummary(ExperimentRun run) {
        return RunSummary.builder()
                .id(run.getId())
                .status(run.getStatus())
                .durationMs(run.getDurationMs())
                .tokensPerSecond(run.getTokensPerSecond())
                .build();
    }
}
