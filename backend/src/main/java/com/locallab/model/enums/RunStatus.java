package com.locallab.model.enums;

/**
 * Represents the execution status of an individual ExperimentRun.
 *
 * <p>Each run within an experiment tracks its own status independently, allowing for granular
 * monitoring of experiment progress and identification of specific failures.
 *
 * <h3>State Transitions:</h3>
 *
 * <pre>
 * ┌─────────┐   execute   ┌─────────┐
 * │ PENDING │ ──────────► │ RUNNING │
 * └─────────┘             └────┬────┘
 *                              │
 *                   ┌──────────┴──────────┐
 *                   │ success             │ error
 *                   ▼                     ▼
 *             ┌─────────┐           ┌────────┐
 *             │ SUCCESS │           │ FAILED │
 *             └─────────┘           └────────┘
 * </pre>
 *
 * @see com.locallab.model.ExperimentRun
 */
public enum RunStatus {

    /**
     * Run is queued and awaiting execution. This is the initial state for all runs when an
     * experiment is started. The run has not yet been submitted to the model for processing.
     */
    PENDING,

    /**
     * Run is currently in progress. The model is actively generating a response. Metrics such as
     * time-to-first-token may be captured during this state.
     */
    RUNNING,

    /**
     * Run completed successfully. The model generated a response without errors. All metrics and
     * output are available for analysis.
     */
    SUCCESS,

    /**
     * Run terminated with an error. The model failed to generate a complete response. The
     * errorMessage field contains details about the failure.
     */
    FAILED
}
