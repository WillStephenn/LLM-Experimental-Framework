/**
 * useExperiments Hook
 *
 * Provides CRUD operations for experiments with async state management.
 * Follows the API contract defined in docs/API-Contract.md.
 *
 * @module hooks/useExperiments
 */
import { useState, useCallback, useEffect } from 'react';
import { api, ApiError } from '@/api/client';
import type {
  ExperimentResponse,
  ExperimentRequest,
  ExperimentStatus,
  PaginatedResponse,
  PaginationParams,
} from '@/types';

/**
 * Filter parameters for listing experiments.
 */
export interface ExperimentFilters {
  /** Filter by experiment status. */
  status?: ExperimentStatus;
}

/**
 * State returned by the useExperiments hook.
 */
export interface UseExperimentsState {
  /** List of experiments. */
  experiments: ExperimentResponse[];
  /** Currently selected experiment (for single fetch). */
  experiment: ExperimentResponse | null;
  /** Loading state for list operations. */
  isLoading: boolean;
  /** Loading state for single experiment operations. */
  isLoadingOne: boolean;
  /** Error from the last operation. */
  error: Error | null;
}

/**
 * Actions returned by the useExperiments hook.
 */
export interface UseExperimentsActions {
  /** Fetch all experiments with optional filters. */
  fetchExperiments: (
    filters?: ExperimentFilters,
    pagination?: PaginationParams
  ) => Promise<ExperimentResponse[]>;
  /** Fetch a single experiment by ID. */
  fetchExperiment: (id: number) => Promise<ExperimentResponse>;
  /** Create a new experiment. */
  createExperiment: (data: ExperimentRequest) => Promise<ExperimentResponse>;
  /** Update an existing experiment (only allowed when status is DRAFT). */
  updateExperiment: (id: number, data: ExperimentRequest) => Promise<ExperimentResponse>;
  /** Delete an experiment and all associated runs. */
  deleteExperiment: (id: number) => Promise<void>;
  /** Clear any errors. */
  clearError: () => void;
  /** Refresh the experiments list. */
  refresh: () => Promise<void>;
}

/**
 * Return type for the useExperiments hook.
 */
export type UseExperimentsReturn = UseExperimentsState & UseExperimentsActions;

/**
 * Build query string from filters and pagination parameters.
 */
const buildQueryString = (filters?: ExperimentFilters, pagination?: PaginationParams): string => {
  const params = new URLSearchParams();

  if (filters?.status) {
    params.append('status', filters.status);
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
 * Hook for managing experiments with CRUD operations.
 *
 * Provides methods to fetch, create, update, and delete experiments,
 * along with loading and error states.
 *
 * @param autoFetch - Whether to automatically fetch experiments on mount (default: true)
 * @returns State and actions for experiment management
 *
 * @example
 * ```tsx
 * const {
 *   experiments,
 *   isLoading,
 *   error,
 *   fetchExperiments,
 *   createExperiment,
 *   deleteExperiment,
 * } = useExperiments();
 *
 * // Filter by status
 * await fetchExperiments({ status: 'RUNNING' });
 *
 * // Create a new experiment
 * const newExperiment = await createExperiment({
 *   name: 'My Experiment',
 *   config: { models: ['llama3:8b'], iterations: 3, contextMode: 'NONE', hyperparameters: {} },
 * });
 * ```
 */
export const useExperiments = (autoFetch = true): UseExperimentsReturn => {
  const [experiments, setExperiments] = useState<ExperimentResponse[]>([]);
  const [experiment, setExperiment] = useState<ExperimentResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingOne, setIsLoadingOne] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [lastFilters, setLastFilters] = useState<ExperimentFilters | undefined>();
  const [lastPagination, setLastPagination] = useState<PaginationParams | undefined>();

  /**
   * Clear any errors.
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  /**
   * Fetch all experiments with optional filters and pagination.
   */
  const fetchExperiments = useCallback(
    async (
      filters?: ExperimentFilters,
      pagination?: PaginationParams
    ): Promise<ExperimentResponse[]> => {
      setIsLoading(true);
      setError(null);
      setLastFilters(filters);
      setLastPagination(pagination);

      try {
        const queryString = buildQueryString(filters, pagination);
        const response = await api.get<
          ExperimentResponse[] | PaginatedResponse<ExperimentResponse>
        >(`/experiments${queryString}`);

        // Handle both array and paginated responses
        const experimentList = Array.isArray(response) ? response : response.content;
        setExperiments(experimentList);
        return experimentList;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to fetch experiments');
        setError(error);
        throw error;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  /**
   * Fetch a single experiment by ID.
   */
  const fetchExperiment = useCallback(async (id: number): Promise<ExperimentResponse> => {
    setIsLoadingOne(true);
    setError(null);

    try {
      const response = await api.get<ExperimentResponse>(`/experiments/${id}`);
      setExperiment(response);
      return response;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to fetch experiment');
      setError(error);
      throw error;
    } finally {
      setIsLoadingOne(false);
    }
  }, []);

  /**
   * Create a new experiment.
   */
  const createExperiment = useCallback(
    async (data: ExperimentRequest): Promise<ExperimentResponse> => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await api.post<ExperimentResponse>('/experiments', data);
        // Add to the list
        setExperiments((prev) => [response, ...prev]);
        return response;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to create experiment');
        setError(error);
        throw error;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  /**
   * Update an existing experiment (only allowed when status is DRAFT).
   */
  const updateExperiment = useCallback(
    async (id: number, data: ExperimentRequest): Promise<ExperimentResponse> => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await api.put<ExperimentResponse>(`/experiments/${id}`, data);
        // Update in the list
        setExperiments((prev) => prev.map((exp) => (exp.id === id ? response : exp)));
        // Update single experiment if it matches
        setExperiment((prev) => (prev?.id === id ? response : prev));
        return response;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to update experiment');
        setError(error);
        throw error;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  /**
   * Delete an experiment and all associated runs.
   */
  const deleteExperiment = useCallback(async (id: number): Promise<void> => {
    setIsLoading(true);
    setError(null);

    try {
      await api.delete<void>(`/experiments/${id}`);
      // Remove from the list
      setExperiments((prev) => prev.filter((exp) => exp.id !== id));
      // Clear single experiment if it matches
      setExperiment((prev) => (prev?.id === id ? null : prev));
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to delete experiment');
      setError(error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Refresh the experiments list with the last used filters.
   */
  const refresh = useCallback(async (): Promise<void> => {
    await fetchExperiments(lastFilters, lastPagination);
  }, [fetchExperiments, lastFilters, lastPagination]);

  // Auto-fetch on mount if enabled
  useEffect(() => {
    if (autoFetch) {
      fetchExperiments().catch(() => {
        // Error is already handled in state
      });
    }
  }, [autoFetch, fetchExperiments]);

  return {
    // State
    experiments,
    experiment,
    isLoading,
    isLoadingOne,
    error,
    // Actions
    fetchExperiments,
    fetchExperiment,
    createExperiment,
    updateExperiment,
    deleteExperiment,
    clearError,
    refresh,
  };
};

// Export ApiError for consumers who need to check error types
export { ApiError };
