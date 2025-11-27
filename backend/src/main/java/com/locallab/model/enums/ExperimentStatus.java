package com.locallab.model.enums;

/**
 * Represents the lifecycle status of an Experiment.
 *
 * <p>An Experiment progresses through various states during its lifecycle, from initial creation
 * through execution to completion or failure.
 *
 * <h3>State Transitions:</h3>
 *
 * <pre>
 * ┌─────────┐   start    ┌─────────┐
 * │  DRAFT  │ ─────────► │ RUNNING │
 * └─────────┘            └────┬────┘
 *                             │
 *               ┌─────────────┼─────────────┐
 *               │ pause       │ complete    │ cancel/error
 *               ▼             ▼             ▼
 *         ┌─────────┐   ┌───────────┐   ┌────────┐
 *         │ PAUSED  │   │ COMPLETED │   │ FAILED │
 *         └────┬────┘   └───────────┘   └────────┘
 *              │ resume
 *              ▼
 *         ┌─────────┐
 *         │ RUNNING │
 *         └─────────┘
 * </pre>
 *
 * @see com.locallab.model.Experiment
 */
public enum ExperimentStatus {

    /**
     * Experiment has been created but not yet started. This is the initial state for all new
     * experiments. The experiment configuration can only be modified in this state.
     */
    DRAFT,

    /**
     * Experiment is currently executing. Individual runs are being processed and results are being
     * collected. Can transition to PAUSED, COMPLETED, or FAILED.
     */
    RUNNING,

    /**
     * Experiment execution has been temporarily suspended. The experiment can be resumed to
     * continue processing remaining runs. No new runs are being executed while in this state.
     */
    PAUSED,

    /**
     * All experiment runs have finished successfully. This is a terminal state; the experiment
     * cannot be restarted. Results are available for analysis.
     */
    COMPLETED,

    /**
     * Experiment terminated due to a critical error or cancellation. This is a terminal state; the
     * experiment cannot be restarted. Partial results may be available for completed runs prior to
     * failure.
     */
    FAILED
}
