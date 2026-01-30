/**
 * Tests for useExperiments hook.
 *
 * @module hooks/useExperiments.test
 */
import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { useExperiments } from './useExperiments';
import { api, ApiError } from '@/api/client';
import type { ExperimentResponse, ExperimentRequest } from '@/types';

// Mock the API client
vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
  ApiError: class ApiError extends Error {
    status: number;
    errorType: string;
    path: string;
    timestamp: string;
    constructor(errorResponse: {
      message: string;
      status: number;
      error: string;
      path: string;
      timestamp: string;
    }) {
      super(errorResponse.message);
      this.name = 'ApiError';
      this.status = errorResponse.status;
      this.errorType = errorResponse.error;
      this.path = errorResponse.path;
      this.timestamp = errorResponse.timestamp;
    }
  },
}));

const mockExperiment: ExperimentResponse = {
  id: 1,
  name: 'Test Experiment',
  taskTemplate: { id: 1, name: 'Test Template' },
  status: 'DRAFT',
  config: {
    models: ['llama3:8b'],
    iterations: 3,
    contextMode: 'NONE',
    hyperparameters: { temperature: 0.7 },
  },
  totalRuns: 3,
  completedRuns: 0,
  createdAt: '2025-11-27T10:00:00Z',
};

const mockExperiment2: ExperimentResponse = {
  id: 2,
  name: 'Another Experiment',
  taskTemplate: null,
  status: 'RUNNING',
  config: {
    models: ['qwen2.5-coder:7b'],
    iterations: 5,
    contextMode: 'RAG',
    hyperparameters: {},
  },
  totalRuns: 5,
  completedRuns: 2,
  createdAt: '2025-11-28T10:00:00Z',
};

describe('useExperiments', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset all mocks to reject by default to catch missing mock setups
    (api.get as Mock).mockReset();
    (api.post as Mock).mockReset();
    (api.put as Mock).mockReset();
    (api.delete as Mock).mockReset();
  });

  describe('initial state', () => {
    it('has empty experiments array by default', () => {
      (api.get as Mock).mockResolvedValueOnce([]);
      const { result } = renderHook(() => useExperiments(false));
      expect(result.current.experiments).toEqual([]);
    });

    it('has null experiment by default', () => {
      (api.get as Mock).mockResolvedValueOnce([]);
      const { result } = renderHook(() => useExperiments(false));
      expect(result.current.experiment).toBeNull();
    });

    it('has isLoading false by default', () => {
      (api.get as Mock).mockResolvedValueOnce([]);
      const { result } = renderHook(() => useExperiments(false));
      expect(result.current.isLoading).toBe(false);
    });

    it('has isLoadingOne false by default', () => {
      (api.get as Mock).mockResolvedValueOnce([]);
      const { result } = renderHook(() => useExperiments(false));
      expect(result.current.isLoadingOne).toBe(false);
    });

    it('has null error by default', () => {
      (api.get as Mock).mockResolvedValueOnce([]);
      const { result } = renderHook(() => useExperiments(false));
      expect(result.current.error).toBeNull();
    });
  });

  describe('auto-fetch', () => {
    it('fetches experiments automatically when autoFetch is true', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockExperiment]);

      const { result } = renderHook(() => useExperiments(true));

      await waitFor(() => {
        expect(result.current.experiments).toEqual([mockExperiment]);
      });

      expect(api.get).toHaveBeenCalledWith('/experiments');
      expect(api.get).toHaveBeenCalledTimes(1);
    });

    it('does not fetch automatically when autoFetch is false', () => {
      const { result } = renderHook(() => useExperiments(false));

      expect(api.get).not.toHaveBeenCalled();
      expect(result.current.experiments).toEqual([]);
    });
  });

  describe('fetchExperiments', () => {
    it('fetches all experiments', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockExperiment, mockExperiment2]);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments();
      });

      expect(api.get).toHaveBeenCalledWith('/experiments');
      expect(result.current.experiments).toEqual([mockExperiment, mockExperiment2]);
    });

    it('sets isLoading during fetch', async () => {
      let resolvePromise: (value: ExperimentResponse[]) => void;
      (api.get as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperiments(false));

      void act(() => {
        void result.current.fetchExperiments();
      });

      expect(result.current.isLoading).toBe(true);

      await act(async () => {
        resolvePromise!([mockExperiment]);
        await Promise.resolve(); // flush promises
      });

      expect(result.current.isLoading).toBe(false);
    });

    it('applies status filter', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockExperiment]);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments({ status: 'DRAFT' });
      });

      expect(api.get).toHaveBeenCalledWith('/experiments?status=DRAFT');
    });

    it('applies pagination parameters', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockExperiment]);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments(undefined, {
          page: 0,
          size: 10,
          sort: 'createdAt,desc',
        });
      });

      expect(api.get).toHaveBeenCalledWith('/experiments?page=0&size=10&sort=createdAt%2Cdesc');
    });

    it('combines filters and pagination', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockExperiment]);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments({ status: 'RUNNING' }, { page: 1, size: 20 });
      });

      expect(api.get).toHaveBeenCalledWith('/experiments?status=RUNNING&page=1&size=20');
    });

    it('handles paginated response', async () => {
      (api.get as Mock).mockResolvedValueOnce({
        content: [mockExperiment],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      });

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments();
      });

      expect(result.current.experiments).toEqual([mockExperiment]);
    });

    it('sets error on failure', async () => {
      const apiError = new ApiError({
        message: 'Server error',
        status: 500,
        error: 'Internal Server Error',
        path: '/experiments',
        timestamp: new Date().toISOString(),
      });
      (api.get as Mock).mockRejectedValueOnce(apiError);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        try {
          await result.current.fetchExperiments();
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(apiError);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('fetchExperiment', () => {
    it('fetches a single experiment by ID', async () => {
      (api.get as Mock).mockResolvedValueOnce(mockExperiment);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiment(1);
      });

      expect(api.get).toHaveBeenCalledWith('/experiments/1');
      expect(result.current.experiment).toEqual(mockExperiment);
    });

    it('sets isLoadingOne during fetch', async () => {
      let resolvePromise: (value: ExperimentResponse) => void;
      (api.get as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperiments(false));

      void act(() => {
        void result.current.fetchExperiment(1);
      });

      expect(result.current.isLoadingOne).toBe(true);

      await act(async () => {
        resolvePromise!(mockExperiment);
        await Promise.resolve(); // flush promises
      });

      expect(result.current.isLoadingOne).toBe(false);
    });

    it('sets error on failure', async () => {
      const error = new Error('Not found');
      (api.get as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        try {
          await result.current.fetchExperiment(999);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(error);
    });
  });

  describe('createExperiment', () => {
    it('creates a new experiment and adds it to the list', async () => {
      (api.post as Mock).mockResolvedValueOnce(mockExperiment);

      const { result } = renderHook(() => useExperiments(false));

      const request: ExperimentRequest = {
        name: 'Test Experiment',
        config: {
          models: ['llama3:8b'],
          iterations: 3,
          contextMode: 'NONE',
          hyperparameters: { temperature: 0.7 },
        },
      };

      await act(async () => {
        await result.current.createExperiment(request);
      });

      expect(api.post).toHaveBeenCalledWith('/experiments', request);
      expect(result.current.experiments).toContainEqual(mockExperiment);
    });

    it('prepends new experiment to the list', async () => {
      // Set up existing experiments
      (api.get as Mock).mockResolvedValueOnce([mockExperiment2]);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments();
      });

      // Create new experiment
      (api.post as Mock).mockResolvedValueOnce(mockExperiment);

      await act(async () => {
        await result.current.createExperiment({
          name: 'Test Experiment',
          config: {
            models: ['llama3:8b'],
            iterations: 3,
            contextMode: 'NONE',
            hyperparameters: {},
          },
        });
      });

      expect(result.current.experiments[0]).toEqual(mockExperiment);
      expect(result.current.experiments[1]).toEqual(mockExperiment2);
    });

    it('sets error on failure', async () => {
      const error = new Error('Validation failed');
      (api.post as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        try {
          await result.current.createExperiment({
            name: '',
            config: {
              models: [],
              iterations: 0,
              contextMode: 'NONE',
              hyperparameters: {},
            },
          });
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(error);
    });
  });

  describe('updateExperiment', () => {
    it('updates an existing experiment', async () => {
      const updatedExperiment = { ...mockExperiment, name: 'Updated Name' };
      (api.get as Mock).mockResolvedValueOnce([mockExperiment]);
      (api.put as Mock).mockResolvedValueOnce(updatedExperiment);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments();
      });

      await act(async () => {
        await result.current.updateExperiment(1, {
          name: 'Updated Name',
          config: mockExperiment.config,
        });
      });

      expect(api.put).toHaveBeenCalledWith('/experiments/1', {
        name: 'Updated Name',
        config: mockExperiment.config,
      });
      expect(result.current.experiments[0].name).toBe('Updated Name');
    });

    it('updates single experiment state if it matches', async () => {
      const updatedExperiment = { ...mockExperiment, name: 'Updated Name' };
      (api.get as Mock).mockResolvedValueOnce(mockExperiment);
      (api.put as Mock).mockResolvedValueOnce(updatedExperiment);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiment(1);
      });

      await act(async () => {
        await result.current.updateExperiment(1, {
          name: 'Updated Name',
          config: mockExperiment.config,
        });
      });

      expect(result.current.experiment?.name).toBe('Updated Name');
    });

    it('sets error on failure', async () => {
      const error = new Error('Cannot update non-draft experiment');
      (api.put as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        try {
          await result.current.updateExperiment(1, {
            name: 'Test',
            config: mockExperiment.config,
          });
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(error);
    });
  });

  describe('deleteExperiment', () => {
    it('deletes an experiment and removes it from the list', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockExperiment, mockExperiment2]);
      (api.delete as Mock).mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments();
      });

      await act(async () => {
        await result.current.deleteExperiment(1);
      });

      expect(api.delete).toHaveBeenCalledWith('/experiments/1');
      expect(result.current.experiments).toHaveLength(1);
      expect(result.current.experiments[0].id).toBe(2);
    });

    it('clears single experiment if it matches', async () => {
      (api.get as Mock).mockResolvedValueOnce(mockExperiment);
      (api.delete as Mock).mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiment(1);
      });

      await act(async () => {
        await result.current.deleteExperiment(1);
      });

      expect(result.current.experiment).toBeNull();
    });

    it('sets error on failure', async () => {
      const error = new Error('Delete failed');
      (api.delete as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        try {
          await result.current.deleteExperiment(1);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(error);
    });
  });

  describe('clearError', () => {
    it('clears the error state', async () => {
      const error = new Error('Some error');
      (api.get as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        try {
          await result.current.fetchExperiments();
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(error);

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('refresh', () => {
    it('re-fetches with the last used filters', async () => {
      (api.get as Mock).mockResolvedValue([mockExperiment]);

      const { result } = renderHook(() => useExperiments(false));

      await act(async () => {
        await result.current.fetchExperiments({ status: 'DRAFT' }, { page: 0, size: 10 });
      });

      expect(api.get).toHaveBeenCalledTimes(1);

      await act(async () => {
        await result.current.refresh();
      });

      expect(api.get).toHaveBeenCalledTimes(2);
      expect(api.get).toHaveBeenLastCalledWith('/experiments?status=DRAFT&page=0&size=10');
    });
  });
});
