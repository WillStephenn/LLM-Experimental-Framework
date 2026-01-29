package com.locallab.dto;

import com.locallab.model.enums.RunStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A summary view of an experiment run for use in analytics.
 *
 * <p>Provides a lightweight representation of a run's key metrics without the full output or
 * configuration details. Used in iteration-based groupings within {@link ModelMetrics}.
 *
 * @see ModelMetrics
 * @see com.locallab.model.ExperimentRun
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunSummary {

    /** The unique identifier of the run. */
    private Long id;

    /**
     * The status of the run.
     *
     * @see RunStatus
     */
    private RunStatus status;

    /**
     * The total duration of the run in milliseconds.
     *
     * <p>May be null for pending or failed runs.
     */
    private Long durationMs;

    /**
     * The tokens per second throughput metric.
     *
     * <p>May be null for pending or failed runs.
     */
    private Double tokensPerSecond;
}
