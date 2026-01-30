/**
 * Tests for useExperimentControl hook.
 *
 * @module hooks/useExperimentControl.test
 */
import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useExperimentControl } from './useExperimentControl';
import { api, ApiError } from '@/api/client';
import type { ExperimentResponse } from '@/types';

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

const createMockExperiment = (
  status: 'DRAFT' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED'
): ExperimentResponse => ({
  id: 1,
  name: 'Test Experiment',
  taskTemplate: { id: 1, name: 'Test Template' },
  status,
  config: {
    models: ['llama3:8b'],
    iterations: 3,
    contextMode: 'NONE',
    hyperparameters: { temperature: 0.7 },
  },
  totalRuns: 3,
  completedRuns: status === 'COMPLETED' ? 3 : status === 'RUNNING' ? 1 : 0,
  createdAt: '2025-11-27T10:00:00Z',
});

describe('useExperimentControl', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset all mocks to reject by default to catch missing mock setups
    (api.get as Mock).mockReset();
    (api.post as Mock).mockReset();
    (api.put as Mock).mockReset();
    (api.delete as Mock).mockReset();
  });

  describe('initial state', () => {
    it('has isLoading false by default', () => {
      const { result } = renderHook(() => useExperimentControl());
      expect(result.current.isLoading).toBe(false);
    });

    it('has pendingAction null by default', () => {
      const { result } = renderHook(() => useExperimentControl());
      expect(result.current.pendingAction).toBeNull();
    });

    it('has null error by default', () => {
      const { result } = renderHook(() => useExperimentControl());
      expect(result.current.error).toBeNull();
    });
  });

  describe('startExperiment', () => {
    it('sends POST request to start endpoint', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      (api.post as Mock).mockResolvedValueOnce(runningExperiment);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        await result.current.startExperiment(1);
      });

      expect(api.post).toHaveBeenCalledWith('/experiments/1/start');
    });

    it('returns the updated experiment', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      (api.post as Mock).mockResolvedValueOnce(runningExperiment);

      const { result } = renderHook(() => useExperimentControl());

      let response: ExperimentResponse | undefined;
      await act(async () => {
        response = await result.current.startExperiment(1);
      });

      expect(response).toEqual(runningExperiment);
      expect(response?.status).toBe('RUNNING');
    });

    it('sets isLoading and pendingAction during request', async () => {
      let resolvePromise: (value: ExperimentResponse) => void;
      (api.post as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperimentControl());

      void act(() => {
        void result.current.startExperiment(1);
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.pendingAction).toBe('start');

      await act(async () => {
        resolvePromise!(createMockExperiment('RUNNING'));
        await Promise.resolve(); // flush promises
      });

      expect(result.current.isLoading).toBe(false);
      expect(result.current.pendingAction).toBeNull();
    });

    it('calls onSuccess callback on success', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      (api.post as Mock).mockResolvedValueOnce(runningExperiment);

      const onSuccess = vi.fn();
      const { result } = renderHook(() => useExperimentControl({ onSuccess }));

      await act(async () => {
        await result.current.startExperiment(1);
      });

      expect(onSuccess).toHaveBeenCalledWith(runningExperiment, 'start');
    });

    it('sets error on failure', async () => {
      const apiError = new ApiError({
        message: 'Cannot start non-draft experiment',
        status: 400,
        error: 'Bad Request',
        path: '/experiments/1/start',
        timestamp: new Date().toISOString(),
      });
      (api.post as Mock).mockRejectedValueOnce(apiError);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        try {
          await result.current.startExperiment(1);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBe(apiError);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.pendingAction).toBeNull();
    });

    it('calls onError callback on failure', async () => {
      const error = new Error('Start failed');
      (api.post as Mock).mockRejectedValueOnce(error);

      const onError = vi.fn();
      const { result } = renderHook(() => useExperimentControl({ onError }));

      await act(async () => {
        try {
          await result.current.startExperiment(1);
        } catch {
          // Expected
        }
      });

      expect(onError).toHaveBeenCalledWith(error, 'start');
    });
  });

  describe('pauseExperiment', () => {
    it('sends POST request to pause endpoint', async () => {
      const pausedExperiment = createMockExperiment('PAUSED');
      (api.post as Mock).mockResolvedValueOnce(pausedExperiment);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        await result.current.pauseExperiment(1);
      });

      expect(api.post).toHaveBeenCalledWith('/experiments/1/pause');
    });

    it('returns the updated experiment with PAUSED status', async () => {
      const pausedExperiment = createMockExperiment('PAUSED');
      (api.post as Mock).mockResolvedValueOnce(pausedExperiment);

      const { result } = renderHook(() => useExperimentControl());

      let response: ExperimentResponse | undefined;
      await act(async () => {
        response = await result.current.pauseExperiment(1);
      });

      expect(response?.status).toBe('PAUSED');
    });

    it('sets pendingAction to pause during request', async () => {
      let resolvePromise: (value: ExperimentResponse) => void;
      (api.post as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperimentControl());

      void act(() => {
        void result.current.pauseExperiment(1);
      });

      expect(result.current.pendingAction).toBe('pause');

      await act(async () => {
        resolvePromise!(createMockExperiment('PAUSED'));
        await Promise.resolve(); // flush promises
      });
    });

    it('calls onSuccess with pause action', async () => {
      const pausedExperiment = createMockExperiment('PAUSED');
      (api.post as Mock).mockResolvedValueOnce(pausedExperiment);

      const onSuccess = vi.fn();
      const { result } = renderHook(() => useExperimentControl({ onSuccess }));

      await act(async () => {
        await result.current.pauseExperiment(1);
      });

      expect(onSuccess).toHaveBeenCalledWith(pausedExperiment, 'pause');
    });
  });

  describe('resumeExperiment', () => {
    it('sends POST request to resume endpoint', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      (api.post as Mock).mockResolvedValueOnce(runningExperiment);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        await result.current.resumeExperiment(1);
      });

      expect(api.post).toHaveBeenCalledWith('/experiments/1/resume');
    });

    it('returns the updated experiment with RUNNING status', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      (api.post as Mock).mockResolvedValueOnce(runningExperiment);

      const { result } = renderHook(() => useExperimentControl());

      let response: ExperimentResponse | undefined;
      await act(async () => {
        response = await result.current.resumeExperiment(1);
      });

      expect(response?.status).toBe('RUNNING');
    });

    it('sets pendingAction to resume during request', async () => {
      let resolvePromise: (value: ExperimentResponse) => void;
      (api.post as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperimentControl());

      void act(() => {
        void result.current.resumeExperiment(1);
      });

      expect(result.current.pendingAction).toBe('resume');

      await act(async () => {
        resolvePromise!(createMockExperiment('RUNNING'));
        await Promise.resolve(); // flush promises
      });
    });

    it('calls onSuccess with resume action', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      (api.post as Mock).mockResolvedValueOnce(runningExperiment);

      const onSuccess = vi.fn();
      const { result } = renderHook(() => useExperimentControl({ onSuccess }));

      await act(async () => {
        await result.current.resumeExperiment(1);
      });

      expect(onSuccess).toHaveBeenCalledWith(runningExperiment, 'resume');
    });
  });

  describe('cancelExperiment', () => {
    it('sends POST request to cancel endpoint', async () => {
      const failedExperiment = createMockExperiment('FAILED');
      (api.post as Mock).mockResolvedValueOnce(failedExperiment);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        await result.current.cancelExperiment(1);
      });

      expect(api.post).toHaveBeenCalledWith('/experiments/1/cancel');
    });

    it('returns the updated experiment with FAILED status', async () => {
      const failedExperiment = createMockExperiment('FAILED');
      (api.post as Mock).mockResolvedValueOnce(failedExperiment);

      const { result } = renderHook(() => useExperimentControl());

      let response: ExperimentResponse | undefined;
      await act(async () => {
        response = await result.current.cancelExperiment(1);
      });

      expect(response?.status).toBe('FAILED');
    });

    it('sets pendingAction to cancel during request', async () => {
      let resolvePromise: (value: ExperimentResponse) => void;
      (api.post as Mock).mockReturnValueOnce(
        new Promise((resolve) => {
          resolvePromise = resolve;
        })
      );

      const { result } = renderHook(() => useExperimentControl());

      void act(() => {
        void result.current.cancelExperiment(1);
      });

      expect(result.current.pendingAction).toBe('cancel');

      await act(async () => {
        resolvePromise!(createMockExperiment('FAILED'));
        await Promise.resolve(); // flush promises
      });
    });

    it('calls onSuccess with cancel action', async () => {
      const failedExperiment = createMockExperiment('FAILED');
      (api.post as Mock).mockResolvedValueOnce(failedExperiment);

      const onSuccess = vi.fn();
      const { result } = renderHook(() => useExperimentControl({ onSuccess }));

      await act(async () => {
        await result.current.cancelExperiment(1);
      });

      expect(onSuccess).toHaveBeenCalledWith(failedExperiment, 'cancel');
    });
  });

  describe('clearError', () => {
    it('clears the error state', async () => {
      const error = new Error('Some error');
      (api.post as Mock).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        try {
          await result.current.startExperiment(1);
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

  describe('sequential actions', () => {
    it('handles multiple sequential actions correctly', async () => {
      const runningExperiment = createMockExperiment('RUNNING');
      const pausedExperiment = createMockExperiment('PAUSED');
      const resumedExperiment = createMockExperiment('RUNNING');

      (api.post as Mock)
        .mockResolvedValueOnce(runningExperiment)
        .mockResolvedValueOnce(pausedExperiment)
        .mockResolvedValueOnce(resumedExperiment);

      const { result } = renderHook(() => useExperimentControl());

      await act(async () => {
        await result.current.startExperiment(1);
      });
      expect(api.post).toHaveBeenLastCalledWith('/experiments/1/start');

      await act(async () => {
        await result.current.pauseExperiment(1);
      });
      expect(api.post).toHaveBeenLastCalledWith('/experiments/1/pause');

      await act(async () => {
        await result.current.resumeExperiment(1);
      });
      expect(api.post).toHaveBeenLastCalledWith('/experiments/1/resume');

      expect(api.post).toHaveBeenCalledTimes(3);
    });
  });
});
