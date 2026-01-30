/**
 * useExperimentControl Hook
 *
 * Provides lifecycle management mutations for experiments (start, pause, resume, cancel).
 * Follows the API contract defined in docs/API-Contract.md.
 *
 * @module hooks/useExperimentControl
 */
import { useState, useCallback } from 'react';
import { api, ApiError } from '@/api/client';
import type { ExperimentResponse } from '@/types';

/**
 * Possible control actions for an experiment.
 */
export type ExperimentControlAction = 'start' | 'pause' | 'resume' | 'cancel';

/**
 * State returned by the useExperimentControl hook.
 */
export interface UseExperimentControlState {
  /** Loading state indicating if a control action is in progress. */
  isLoading: boolean;
  /** The specific action currently being executed. */
  pendingAction: ExperimentControlAction | null;
  /** Error from the last control operation. */
  error: Error | null;
}

/**
 * Actions returned by the useExperimentControl hook.
 */
export interface UseExperimentControlActions {
  /**
   * Start an experiment. Status must be DRAFT.
   * Changes status to RUNNING.
   */
  startExperiment: (id: number) => Promise<ExperimentResponse>;
  /**
   * Pause an experiment. Status must be RUNNING.
   * Pauses after the current run completes.
   */
  pauseExperiment: (id: number) => Promise<ExperimentResponse>;
  /**
   * Resume a paused experiment. Status must be PAUSED.
   * Changes status back to RUNNING.
   */
  resumeExperiment: (id: number) => Promise<ExperimentResponse>;
  /**
   * Cancel an experiment and mark remaining runs as failed.
   * Changes status to FAILED.
   */
  cancelExperiment: (id: number) => Promise<ExperimentResponse>;
  /** Clear any errors. */
  clearError: () => void;
}

/**
 * Return type for the useExperimentControl hook.
 */
export type UseExperimentControlReturn = UseExperimentControlState & UseExperimentControlActions;

/**
 * Hook for managing experiment lifecycle.
 *
 * Provides methods to start, pause, resume, and cancel experiments,
 * along with loading and error states.
 *
 * @param onSuccess - Optional callback invoked after a successful control action
 * @returns State and actions for experiment lifecycle management
 *
 * @example
 * ```tsx
 * const {
 *   isLoading,
 *   pendingAction,
 *   error,
 *   startExperiment,
 *   pauseExperiment,
 *   cancelExperiment,
 * } = useExperimentControl({
 *   onSuccess: (experiment) => {
 *     console.log('Experiment updated:', experiment.status);
 *   },
 * });
 *
 * // Start an experiment
 * await startExperiment(experimentId);
 *
 * // Pause a running experiment
 * await pauseExperiment(experimentId);
 *
 * // Cancel an experiment
 * await cancelExperiment(experimentId);
 * ```
 */
export const useExperimentControl = (options?: {
  onSuccess?: (experiment: ExperimentResponse, action: ExperimentControlAction) => void;
  onError?: (error: Error, action: ExperimentControlAction) => void;
}): UseExperimentControlReturn => {
  const [isLoading, setIsLoading] = useState(false);
  const [pendingAction, setPendingAction] = useState<ExperimentControlAction | null>(null);
  const [error, setError] = useState<Error | null>(null);

  const { onSuccess, onError } = options ?? {};

  /**
   * Clear any errors.
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  /**
   * Execute a control action on an experiment.
   */
  const executeAction = useCallback(
    async (id: number, action: ExperimentControlAction): Promise<ExperimentResponse> => {
      setIsLoading(true);
      setPendingAction(action);
      setError(null);

      try {
        const response = await api.post<ExperimentResponse>(`/experiments/${id}/${action}`);
        onSuccess?.(response, action);
        return response;
      } catch (err) {
        const error = err instanceof Error ? err : new Error(`Failed to ${action} experiment`);
        setError(error);
        onError?.(error, action);
        throw error;
      } finally {
        setIsLoading(false);
        setPendingAction(null);
      }
    },
    [onSuccess, onError]
  );

  /**
   * Start an experiment. Status must be DRAFT.
   */
  const startExperiment = useCallback(
    async (id: number): Promise<ExperimentResponse> => {
      return executeAction(id, 'start');
    },
    [executeAction]
  );

  /**
   * Pause an experiment. Status must be RUNNING.
   */
  const pauseExperiment = useCallback(
    async (id: number): Promise<ExperimentResponse> => {
      return executeAction(id, 'pause');
    },
    [executeAction]
  );

  /**
   * Resume a paused experiment. Status must be PAUSED.
   */
  const resumeExperiment = useCallback(
    async (id: number): Promise<ExperimentResponse> => {
      return executeAction(id, 'resume');
    },
    [executeAction]
  );

  /**
   * Cancel an experiment and mark remaining runs as failed.
   */
  const cancelExperiment = useCallback(
    async (id: number): Promise<ExperimentResponse> => {
      return executeAction(id, 'cancel');
    },
    [executeAction]
  );

  return {
    // State
    isLoading,
    pendingAction,
    error,
    // Actions
    startExperiment,
    pauseExperiment,
    resumeExperiment,
    cancelExperiment,
    clearError,
  };
};

// Export ApiError for consumers who need to check error types
export { ApiError };
