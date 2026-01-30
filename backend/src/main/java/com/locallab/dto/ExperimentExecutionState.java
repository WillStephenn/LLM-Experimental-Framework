package com.locallab.dto;

import lombok.Data;

/**
 * Represents the runtime state of an executing experiment.
 *
 * <p>This class tracks the execution state of an experiment, including progress and control flags
 * for pause/cancel operations. It uses volatile fields to ensure thread-safe visibility across the
 * main executor thread and control threads.
 *
 * <h3>Thread Safety:</h3>
 *
 * <p>The {@code paused} and {@code cancelled} flags are marked as volatile to ensure proper
 * visibility when set by a control thread and read by the executor thread. The {@code
 * completedRuns} field is also volatile as it's updated during execution and read by progress
 * reporting.
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>{@code
 * ExperimentExecutionState state = new ExperimentExecutionState(experimentId);
 * state.setTotalRuns(12);
 *
 * // During execution
 * state.setCompletedRuns(state.getCompletedRuns() + 1);
 *
 * // From control thread
 * state.setPaused(true);
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.service.ExperimentExecutorService
 */
@Data
public class ExperimentExecutionState {

    /**
     * The identifier of the experiment being executed.
     *
     * <p>This is immutable after construction as it identifies the experiment throughout its
     * execution lifecycle.
     */
    private final Long experimentId;

    /**
     * Flag indicating whether the experiment is paused.
     *
     * <p>When set to true, the executor will stop processing after the current run completes and
     * update the experiment status to PAUSED. Marked as volatile for thread-safe visibility.
     */
    private volatile boolean paused;

    /**
     * Flag indicating whether the experiment has been cancelled.
     *
     * <p>When set to true, the executor will stop processing after the current run completes and
     * update the experiment status to FAILED. Marked as volatile for thread-safe visibility.
     */
    private volatile boolean cancelled;

    /**
     * The number of runs that have completed (successfully or with failure).
     *
     * <p>Updated after each run finishes. Used for progress calculation. Marked as volatile for
     * thread-safe visibility.
     */
    private volatile int completedRuns;

    /**
     * The number of runs that have failed.
     *
     * <p>Tracks failures across execution sessions (including resumed experiments). Marked as
     * volatile for thread-safe visibility.
     */
    private volatile int failedRuns;

    /**
     * The total number of runs to be executed.
     *
     * <p>Calculated from the run configuration matrix (models x embeddings x iterations). Set once
     * at the start of execution.
     */
    private int totalRuns;

    /**
     * Constructs a new execution state for the specified experiment.
     *
     * @param experimentId the identifier of the experiment to track
     */
    public ExperimentExecutionState(Long experimentId) {
        this.experimentId = experimentId;
        this.paused = false;
        this.cancelled = false;
        this.completedRuns = 0;
        this.failedRuns = 0;
        this.totalRuns = 0;
    }
}
