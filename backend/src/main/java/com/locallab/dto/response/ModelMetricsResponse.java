package com.locallab.dto.response;

import java.util.List;
import java.util.Map;

import com.locallab.dto.ModelMetrics;
import com.locallab.dto.RunSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for detailed model metrics returned to clients.
 *
 * <p>Provides comprehensive performance statistics for a specific model, optionally scoped to an
 * experiment. Includes run counts, success rates, average performance metrics, and per-iteration
 * breakdowns.
 *
 * @see ModelMetrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetricsResponse {

    /**
     * The name of the model these metrics are for.
     *
     * <p>Corresponds to the Ollama model name (e.g., "qwen2.5-coder:7b").
     */
    private String modelName;

    /**
     * The total number of runs for this model.
     *
     * <p>Includes all runs regardless of status.
     */
    private int totalRuns;

    /**
     * The number of successful runs.
     *
     * <p>Runs with status SUCCESS.
     */
    private int successfulRuns;

    /**
     * The number of failed runs.
     *
     * <p>Runs with status FAILED.
     */
    private int failedRuns;

    /**
     * The success rate as a percentage (0-100).
     *
     * <p>Calculated as: (successfulRuns / totalRuns) * 100
     */
    private Double successRate;

    /**
     * The average tokens per second across successful runs.
     *
     * <p>May be null if no successful runs have TPS data.
     */
    private Double avgTokensPerSecond;

    /**
     * The average duration in milliseconds across successful runs.
     *
     * <p>May be null if no successful runs have duration data.
     */
    private Double avgDurationMs;

    /**
     * The average time to first token in milliseconds across successful runs.
     *
     * <p>May be null if no successful runs have TTFT data.
     */
    private Double avgTimeToFirstTokenMs;

    /**
     * Runs grouped by iteration number.
     *
     * <p>The key is the iteration number (1-indexed), and the value is a list of run summaries for
     * that iteration.
     */
    private Map<Integer, List<RunSummary>> runsByIteration;

    /**
     * Creates a ModelMetricsResponse from ModelMetrics.
     *
     * @param metrics the model metrics from the analytics service
     * @return a new ModelMetricsResponse with the metrics data
     */
    public static ModelMetricsResponse fromMetrics(ModelMetrics metrics) {
        return ModelMetricsResponse.builder()
                .modelName(metrics.getModelName())
                .totalRuns(metrics.getTotalRuns())
                .successfulRuns(metrics.getSuccessfulRuns())
                .failedRuns(metrics.getFailedRuns())
                .successRate(metrics.getSuccessRate())
                .avgTokensPerSecond(metrics.getAvgTokensPerSecond())
                .avgDurationMs(metrics.getAvgDurationMs())
                .avgTimeToFirstTokenMs(metrics.getAvgTimeToFirstTokenMs())
                .runsByIteration(metrics.getRunsByIteration())
                .build();
    }
}
