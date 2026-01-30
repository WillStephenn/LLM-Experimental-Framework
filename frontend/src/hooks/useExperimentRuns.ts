/**
 * useExperimentRuns Hook
 *
 * Provides functionality to fetch experiment runs for a specific experiment.
 * Follows the API contract defined in docs/API-Contract.md.
 *
 * @module hooks/useExperimentRuns
 */
import { useState, useCallback, useEffect } from 'react';
import { api, ApiError } from '@/api/client';
import type { ExperimentRunResponse, RunStatus, PaginationParams } from '@/types';

/**
 * Filter parameters for listing experiment runs.
 */
export interface ExperimentRunFilters {
  /** Filter by run status. */
  status?: RunStatus;
  /** Filter by model name. */
  modelName?: string;
}

/**
 * State returned by the useExperimentRuns hook.
 */
export interface UseExperimentRunsState {
  /** List of experiment runs. */
  runs: ExperimentRunResponse[];
  /** Currently selected run (for single fetch). */
  run: ExperimentRunResponse | null;
  /** Loading state for list operations. */
  isLoading: boolean;
  /** Loading state for single run operations. */
  isLoadingOne: boolean;
  /** Error from the last operation. */
  error: Error | null;
}

/**
 * Actions returned by the useExperimentRuns hook.
 */
export interface UseExperimentRunsActions {
  /** Fetch all runs for an experiment with optional filters. */
  fetchRuns: (
    experimentId: number,
    filters?: ExperimentRunFilters,
    pagination?: PaginationParams
  ) => Promise<ExperimentRunResponse[]>;
  /** Fetch a single run by ID. */
  fetchRun: (runId: number) => Promise<ExperimentRunResponse>;
  /** Clear any errors. */
  clearError: () => void;
  /** Refresh the runs list for the current experiment. */
  refresh: () => Promise<void>;
}

/**
 * Return type for the useExperimentRuns hook.
 */
export type UseExperimentRunsReturn = UseExperimentRunsState & UseExperimentRunsActions;

/**
 * Build query string from filters and pagination parameters.
 */
const buildQueryString = (
  filters?: ExperimentRunFilters,
  pagination?: PaginationParams
): string => {
  const params = new URLSearchParams();

  if (filters?.status) {
    params.append('status', filters.status);
  }

  if (filters?.modelName) {
    params.append('modelName', filters.modelName);
  }

  if (pagination?.page !== undefined) {
    params.append('page', pagination.page.toString());
  }

  if (pagination?.size !== undefined) {
    params.append('size', pagination.size.toString());
  }

  if (pagination?.sort) {
    params.append('sort', pagination.sort);
  }

  const queryString = params.toString();
  return queryString ? `?${queryString}` : '';
};

/**
 * Hook for fetching experiment runs.
 *
 * Provides methods to fetch runs for a specific experiment, along with
 * loading and error states.
 *
 * @param experimentId - Optional experiment ID to auto-fetch runs for on mount
 * @returns State and actions for experiment run management
 *
 * @example
 * ```tsx
 * // Auto-fetch runs for an experiment
 * const { runs, isLoading, error } = useExperimentRuns(experimentId);
 *
 * // Manual fetching with filters
 * const { runs, fetchRuns } = useExperimentRuns();
 * await fetchRuns(experimentId, { status: 'SUCCESS', modelName: 'llama3:8b' });
 *
 * // Fetch a single run
 * const { run, fetchRun } = useExperimentRuns();
 * await fetchRun(runId);
 * ```
 */
export const useExperimentRuns = (experimentId?: number): UseExperimentRunsReturn => {
  const [runs, setRuns] = useState<ExperimentRunResponse[]>([]);
  const [run, setRun] = useState<ExperimentRunResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingOne, setIsLoadingOne] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [lastExperimentId, setLastExperimentId] = useState<number | undefined>(experimentId);
  const [lastFilters, setLastFilters] = useState<ExperimentRunFilters | undefined>();
  const [lastPagination, setLastPagination] = useState<PaginationParams | undefined>();

  /**
   * Clear any errors.
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  /**
   * Fetch all runs for an experiment with optional filters and pagination.
   */
  const fetchRuns = useCallback(
    async (
      expId: number,
      filters?: ExperimentRunFilters,
      pagination?: PaginationParams
    ): Promise<ExperimentRunResponse[]> => {
      setIsLoading(true);
      setError(null);
      setLastExperimentId(expId);
      setLastFilters(filters);
      setLastPagination(pagination);

      try {
        const queryString = buildQueryString(filters, pagination);
        const response = await api.get<ExperimentRunResponse[]>(
          `/experiments/${expId}/runs${queryString}`
        );
        setRuns(response);
        return response;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to fetch experiment runs');
        setError(error);
        throw error;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  /**
   * Fetch a single run by ID.
   */
  const fetchRun = useCallback(async (runId: number): Promise<ExperimentRunResponse> => {
    setIsLoadingOne(true);
    setError(null);

    try {
      const response = await api.get<ExperimentRunResponse>(`/runs/${runId}`);
      setRun(response);
      return response;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch experiment run');
      setError(error);
      throw error;
    } finally {
      setIsLoadingOne(false);
    }
  }, []);

  /**
   * Refresh the runs list for the current experiment.
   */
  const refresh = useCallback(async (): Promise<void> => {
    if (lastExperimentId !== undefined) {
      await fetchRuns(lastExperimentId, lastFilters, lastPagination);
    }
  }, [fetchRuns, lastExperimentId, lastFilters, lastPagination]);

  // Auto-fetch on mount if experimentId is provided
  useEffect(() => {
    if (experimentId !== undefined) {
      fetchRuns(experimentId).catch(() => {
        // Error is already handled in state
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- fetchRuns is stable (only uses setters)
  }, [experimentId]);

  return {
    // State
    runs,
    run,
    isLoading,
    isLoadingOne,
    error,
    // Actions
    fetchRuns,
    fetchRun,
    clearError,
    refresh,
  };
};

// Export ApiError for consumers who need to check error types
export { ApiError };
