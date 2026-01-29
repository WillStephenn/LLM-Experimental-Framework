package com.locallab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single entry in the leaderboard for a specific model.
 *
 * <p>Contains aggregated metrics for a model including run counts, success rates, and performance
 * statistics (tokens per second, duration, time to first token).
 *
 * <h3>Metric Calculations:</h3>
 *
 * <ul>
 *   <li>Success rate is calculated as: (successCount / runCount) * 100
 *   <li>Average metrics are calculated only from successful runs
 *   <li>Min/max values represent the extremes observed in successful runs
 * </ul>
 *
 * @see LeaderboardData
 * @see com.locallab.service.AnalyticsService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    /**
     * The name of the model this entry represents.
     *
     * <p>This corresponds to the Ollama model name (e.g., "qwen2.5-coder:7b").
     */
    private String modelName;

    /**
     * The total number of runs for this model.
     *
     * <p>Includes both successful and failed runs.
     */
    private int runCount;

    /**
     * The number of successful runs for this model.
     *
     * <p>Only runs with status SUCCESS are counted.
     */
    private int successCount;

    /**
     * The success rate as a percentage (0-100).
     *
     * <p>Calculated as: (successCount / runCount) * 100
     */
    private Double successRate;

    /**
     * The average tokens per second across successful runs.
     *
     * <p>May be null if no successful runs have TPS data.
     */
    private Double avgTokensPerSecond;

    /**
     * The minimum tokens per second observed in successful runs.
     *
     * <p>May be null if no successful runs have TPS data.
     */
    private Double minTokensPerSecond;

    /**
     * The maximum tokens per second observed in successful runs.
     *
     * <p>May be null if no successful runs have TPS data.
     */
    private Double maxTokensPerSecond;

    /**
     * The average duration in milliseconds across successful runs.
     *
     * <p>May be null if no successful runs have duration data.
     */
    private Double avgDurationMs;

    /**
     * The minimum duration in milliseconds observed in successful runs.
     *
     * <p>May be null if no successful runs have duration data.
     */
    private Long minDurationMs;

    /**
     * The maximum duration in milliseconds observed in successful runs.
     *
     * <p>May be null if no successful runs have duration data.
     */
    private Long maxDurationMs;

    /**
     * The average time to first token in milliseconds across successful runs.
     *
     * <p>May be null if no successful runs have TTFT data.
     */
    private Double avgTimeToFirstTokenMs;
}
