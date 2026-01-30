import { describe, it, expect, vi, beforeEach, afterEach, type Mock } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useOllama } from './useOllama';
import { api } from '@/api/client';

// Mock the API client
vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
  },
}));

const mockApi = api as unknown as { get: Mock };

describe('useOllama', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('initial state', () => {
    it('starts with loading state', () => {
      // Create a promise that never resolves to keep the loading state
      mockApi.get.mockImplementation(
        () =>
          new Promise<never>(() => {
            // Intentionally never resolves to test loading state
          })
      );

      const { result } = renderHook(() => useOllama());

      expect(result.current.isLoading).toBe(true);
      expect(result.current.models).toEqual([]);
      expect(result.current.error).toBeNull();
      expect(result.current.isAvailable).toBe(false);
    });
  });

  describe('successful fetch', () => {
    it('fetches models when Ollama is available', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 3,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.resolve({
            models: ['llama3:8b', 'codellama:7b', 'mistral:7b'],
          });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.models).toEqual(['llama3:8b', 'codellama:7b', 'mistral:7b']);
      expect(result.current.isAvailable).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('calls status endpoint before models endpoint', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 1,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.resolve({ models: ['llama3:8b'] });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      renderHook(() => useOllama());

      await waitFor(() => {
        expect(mockApi.get).toHaveBeenCalledWith('/ollama/status');
      });

      await waitFor(() => {
        expect(mockApi.get).toHaveBeenCalledWith('/ollama/models');
      });
    });
  });

  describe('Ollama unavailable', () => {
    it('sets error when Ollama status reports unavailable', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: false,
            baseUrl: 'http://localhost:11434',
            modelCount: 0,
            message: 'Cannot connect to Ollama service',
          });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isAvailable).toBe(false);
      expect(result.current.error).toBe('Cannot connect to Ollama service');
      expect(result.current.models).toEqual([]);
    });

    it('uses default error message when status message is empty', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: false,
            baseUrl: 'http://localhost:11434',
            modelCount: 0,
            message: '',
          });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBe('Ollama service is not available');
    });
  });

  describe('network errors', () => {
    it('handles network error on status check', async () => {
      mockApi.get.mockRejectedValue(new Error('Network error - unable to connect to the server'));

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isAvailable).toBe(false);
      expect(result.current.error).toBe('Network error - unable to connect to the server');
      expect(result.current.models).toEqual([]);
    });

    it('handles network error on models fetch', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 1,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.reject(new Error('Connection refused'));
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isAvailable).toBe(false);
      expect(result.current.error).toBe('Connection refused');
      expect(result.current.models).toEqual([]);
    });

    it('handles non-Error exceptions', async () => {
      mockApi.get.mockRejectedValue('String error');

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBe('Failed to connect to Ollama service');
    });
  });

  describe('refetch', () => {
    it('provides a refetch function', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 1,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.resolve({ models: ['llama3:8b'] });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(typeof result.current.refetch).toBe('function');
    });

    it('refetch resets loading state and fetches again', async () => {
      let callCount = 0;
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 1,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          callCount++;
          return Promise.resolve({
            models: callCount === 1 ? ['llama3:8b'] : ['llama3:8b', 'mistral:7b'],
          });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.models).toEqual(['llama3:8b']);

      // Call refetch
      await act(async () => {
        await result.current.refetch();
      });

      await waitFor(() => {
        expect(result.current.models).toEqual(['llama3:8b', 'mistral:7b']);
      });
    });

    it('refetch clears previous error', async () => {
      let shouldFail = true;
      mockApi.get.mockImplementation((url: string) => {
        if (shouldFail) {
          return Promise.reject(new Error('Connection failed'));
        }
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 1,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.resolve({ models: ['llama3:8b'] });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBe('Connection failed');

      // Fix the issue and refetch
      shouldFail = false;
      await act(async () => {
        await result.current.refetch();
      });

      await waitFor(() => {
        expect(result.current.error).toBeNull();
      });

      expect(result.current.models).toEqual(['llama3:8b']);
      expect(result.current.isAvailable).toBe(true);
    });
  });

  describe('empty models list', () => {
    it('handles empty models array', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 0,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.resolve({ models: [] });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.models).toEqual([]);
      expect(result.current.isAvailable).toBe(true);
      expect(result.current.error).toBeNull();
    });
  });
});
