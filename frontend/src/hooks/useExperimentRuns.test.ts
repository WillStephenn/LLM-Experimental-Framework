/**
 * Tests for useExperimentRuns hook.
 *
 * @module hooks/useExperimentRuns.test
 */
import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { useExperimentRuns } from './useExperimentRuns';
import { api, ApiError } from '@/api/client';
import type { ExperimentRunResponse } from '@/types';

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

const mockRun1: ExperimentRunResponse = {
  id: 1,
  experimentId: 1,
  modelName: 'llama3:8b',
  embeddingModel: null,
  systemPrompt: { id: 1, alias: 'default' },
  iteration: 1,
  config: '{"temperature": 0.7}',
  status: 'SUCCESS',
  output: 'Generated output text',
  durationMs: 2500,
  tokensPerSecond: 45.5,
  timeToFirstTokenMs: 120,
  retrievedChunks: null,
  errorMessage: null,
  timestamp: '2025-11-27T10:00:00Z',
};

const mockRun2: ExperimentRunResponse = {
  id: 2,
  experimentId: 1,
  modelName: 'qwen2.5-coder:7b',
  embeddingModel: { id: 1, name: 'nomic-embed-text' },
  systemPrompt: null,
  iteration: 1,
  config: '{"temperature": 0.5}',
  status: 'RUNNING',
  output: null,
  durationMs: null,
  tokensPerSecond: null,
  timeToFirstTokenMs: null,
  retrievedChunks: [{ content: 'chunk text', distance: 0.15, chunkIndex: 0 }],
  errorMessage: null,
  timestamp: '2025-11-27T10:05:00Z',
};

const mockRun3: ExperimentRunResponse = {
  id: 3,
  experimentId: 1,
  modelName: 'llama3:8b',
  embeddingModel: null,
  systemPrompt: null,
  iteration: 2,
  config: '{"temperature": 0.7}',
  status: 'FAILED',
  output: null,
  durationMs: 500,
  tokensPerSecond: null,
  timeToFirstTokenMs: null,
  retrievedChunks: null,
  errorMessage: 'Model ran out of memory',
  timestamp: '2025-11-27T10:10:00Z',
};

describe('useExperimentRuns', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset all mocks to reject by default to catch missing mock setups
    (api.get as Mock).mockReset();
    (api.post as Mock).mockReset();
    (api.put as Mock).mockReset();
    (api.delete as Mock).mockReset();
  });

  describe('initial state', () => {
    it('has empty runs array by default', () => {
      const { result } = renderHook(() => useExperimentRuns());
      expect(result.current.runs).toEqual([]);
    });

    it('has null run by default', () => {
      const { result } = renderHook(() => useExperimentRuns());
      expect(result.current.run).toBeNull();
    });

    it('has isLoading false by default', () => {
      const { result } = renderHook(() => useExperimentRuns());
      expect(result.current.isLoading).toBe(false);
    });

    it('has isLoadingOne false by default', () => {
      const { result } = renderHook(() => useExperimentRuns());
      expect(result.current.isLoadingOne).toBe(false);
    });

    it('has null error by default', () => {
      const { result } = renderHook(() => useExperimentRuns());
      expect(result.current.error).toBeNull();
    });
  });

  describe('auto-fetch with experimentId', () => {
    it('fetches runs automatically when experimentId is provided', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun1, mockRun2]);

      const { result } = renderHook(() => useExperimentRuns(1));

      await waitFor(() => {
        expect(result.current.runs).toEqual([mockRun1, mockRun2]);
      });

      expect(api.get).toHaveBeenCalledWith('/experiments/1/runs');
      expect(api.get).toHaveBeenCalledTimes(1);
    });

    it('does not fetch automatically when experimentId is not provided', () => {
      const { result } = renderHook(() => useExperimentRuns());

      expect(api.get).not.toHaveBeenCalled();
      expect(result.current.runs).toEqual([]);
    });
  });

  describe('fetchRuns', () => {
    it('fetches all runs for an experiment', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun1, mockRun2, mockRun3]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1);
      });

      expect(api.get).toHaveBeenCalledWith('/experiments/1/runs');
      expect(result.current.runs).toEqual([mockRun1, mockRun2, mockRun3]);
    });

    it('sets isLoading during fetch', async () => {
      let resolvePromise: (value: ExperimentRunResponse[]) => void;
      (api.get as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperimentRuns());

      void act(() => {
        void result.current.fetchRuns(1);
      });

      expect(result.current.isLoading).toBe(true);

      await act(async () => {
        resolvePromise!([mockRun1]);
        await Promise.resolve(); // flush promises
      });

      expect(result.current.isLoading).toBe(false);
    });

    it('applies status filter', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun1]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1, { status: 'SUCCESS' });
      });

      expect(api.get).toHaveBeenCalledWith('/experiments/1/runs?status=SUCCESS');
    });

    it('applies modelName filter', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun1, mockRun3]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1, { modelName: 'llama3:8b' });
      });

      expect(api.get).toHaveBeenCalledWith('/experiments/1/runs?modelName=llama3%3A8b');
    });

    it('combines multiple filters', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun1]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1, { status: 'SUCCESS', modelName: 'llama3:8b' });
      });

      expect(api.get).toHaveBeenCalledWith(
        '/experiments/1/runs?status=SUCCESS&modelName=llama3%3A8b'
      );
    });

    it('applies pagination parameters', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun1]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1, undefined, {
          page: 0,
          size: 10,
          sort: 'timestamp,desc',
        });
      });

      expect(api.get).toHaveBeenCalledWith(
        '/experiments/1/runs?page=0&size=10&sort=timestamp%2Cdesc'
      );
    });

    it('combines filters and pagination', async () => {
      (api.get as Mock).mockResolvedValueOnce([mockRun2]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1, { status: 'RUNNING' }, { page: 0, size: 5 });
      });

      expect(api.get).toHaveBeenCalledWith('/experiments/1/runs?status=RUNNING&page=0&size=5');
    });

    it('sets error on failure', async () => {
      const apiError = new ApiError({
        message: 'Experiment not found',
        status: 404,
        error: 'Not Found',
        path: '/experiments/999/runs',
        timestamp: new Date().toISOString(),
      });
      (api.get as Mock).mockRejectedValueOnce(apiError);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        try {
          await result.current.fetchRuns(999);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(apiError);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('fetchRun', () => {
    it('fetches a single run by ID', async () => {
      (api.get as Mock).mockResolvedValueOnce(mockRun1);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRun(1);
      });

      expect(api.get).toHaveBeenCalledWith('/runs/1');
      expect(result.current.run).toEqual(mockRun1);
    });

    it('sets isLoadingOne during fetch', async () => {
      let resolvePromise: (value: ExperimentRunResponse) => void;
      (api.get as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperimentRuns());

      void act(() => {
        void result.current.fetchRun(1);
      });

      expect(result.current.isLoadingOne).toBe(true);

      await act(async () => {
        resolvePromise!(mockRun1);
        await Promise.resolve(); // flush promises
      });

      expect(result.current.isLoadingOne).toBe(false);
    });

    it('sets error on failure', async () => {
      const error = new Error('Run not found');
      (api.get as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        try {
          await result.current.fetchRun(999);
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

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        try {
          await result.current.fetchRuns(1);
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
    it('re-fetches with the last used experiment ID and filters', async () => {
      (api.get as Mock).mockResolvedValue([mockRun1]);

      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.fetchRuns(1, { status: 'SUCCESS' }, { page: 0, size: 10 });
      });

      expect(api.get).toHaveBeenCalledTimes(1);

      await act(async () => {
        await result.current.refresh();
      });

      expect(api.get).toHaveBeenCalledTimes(2);
      expect(api.get).toHaveBeenLastCalledWith('/experiments/1/runs?status=SUCCESS&page=0&size=10');
    });

    it('does nothing if no experiment ID has been set', async () => {
      const { result } = renderHook(() => useExperimentRuns());

      await act(async () => {
        await result.current.refresh();
      });

      expect(api.get).not.toHaveBeenCalled();
    });
  });
});
