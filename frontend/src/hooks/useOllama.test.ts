/**
 * Tests for Ollama API Hooks
 *
 * @module hooks/useOllama.test
 */
import { describe, it, expect, beforeEach, afterEach, vi, type Mock } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { api } from '@/api/client';
import type { ModelsResponse, OllamaStatusResponse, GenerationResponse } from '@/types';
import {
  useOllamaModels,
  useOllamaStatus,
  useGenerate,
  useOllama,
  clearModelsCache,
} from './useOllama';

// Mock the API client
vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('useOllamaModels', () => {
  const mockModelsResponse: ModelsResponse = {
    models: ['llama3:8b', 'qwen2.5-coder:7b', 'codellama:7b'],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    clearModelsCache();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch models on mount', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockModelsResponse);

    const { result } = renderHook(() => useOllamaModels());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.models).toEqual([]);
    expect(result.current.error).toBeNull();

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.models).toEqual(mockModelsResponse.models);
    expect(result.current.error).toBeNull();
    expect(api.get).toHaveBeenCalledWith('/ollama/models');
  });

  it('should handle fetch error', async () => {
    const mockError = new Error('Network error');
    (api.get as Mock).mockRejectedValueOnce(mockError);

    const { result } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.models).toEqual([]);
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Network error');
  });

  it('should handle non-Error rejection', async () => {
    (api.get as Mock).mockRejectedValueOnce('String error');

    const { result } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Failed to fetch models');
  });

  it('should use cached data on subsequent calls', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockModelsResponse);

    const { result: result1 } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result1.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(1);

    // Second hook should use cache
    const { result: result2 } = renderHook(() => useOllamaModels());

    // Should start with cached data and not loading
    expect(result2.current.isLoading).toBe(false);
    expect(result2.current.models).toEqual(mockModelsResponse.models);
    expect(api.get).toHaveBeenCalledTimes(1);
  });

  it('should force refetch when calling refetch', async () => {
    (api.get as Mock).mockResolvedValue(mockModelsResponse);

    const { result } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(1);

    // Call refetch
    act(() => {
      result.current.refetch();
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it('should provide refetch function', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockModelsResponse);

    const { result } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(typeof result.current.refetch).toBe('function');
  });
});

describe('useOllamaStatus', () => {
  const mockStatusResponse: OllamaStatusResponse = {
    available: true,
    baseUrl: 'http://localhost:11434',
    modelCount: 5,
    message: 'Ollama is running',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch status on mount', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockStatusResponse);

    const { result } = renderHook(() => useOllamaStatus());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.status).toBeNull();
    expect(result.current.isAvailable).toBe(false);
    expect(result.current.error).toBeNull();

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.status).toEqual(mockStatusResponse);
    expect(result.current.isAvailable).toBe(true);
    expect(result.current.error).toBeNull();
    expect(api.get).toHaveBeenCalledWith('/ollama/status');
  });

  it('should handle unavailable status', async () => {
    const unavailableStatus: OllamaStatusResponse = {
      available: false,
      baseUrl: 'http://localhost:11434',
      modelCount: 0,
      message: 'Ollama is not running',
    };
    (api.get as Mock).mockResolvedValueOnce(unavailableStatus);

    const { result } = renderHook(() => useOllamaStatus());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.status).toEqual(unavailableStatus);
    expect(result.current.isAvailable).toBe(false);
  });

  it('should handle fetch error', async () => {
    const mockError = new Error('Service unavailable');
    (api.get as Mock).mockRejectedValueOnce(mockError);

    const { result } = renderHook(() => useOllamaStatus());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.status).toBeNull();
    expect(result.current.isAvailable).toBe(false);
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Service unavailable');
  });

  it('should handle non-Error rejection', async () => {
    (api.get as Mock).mockRejectedValueOnce('String error');

    const { result } = renderHook(() => useOllamaStatus());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Failed to check Ollama status');
  });

  it('should refetch status when calling refetch', async () => {
    (api.get as Mock).mockResolvedValue(mockStatusResponse);

    const { result } = renderHook(() => useOllamaStatus());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(1);

    act(() => {
      result.current.refetch();
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it('should provide refetch function', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockStatusResponse);

    const { result } = renderHook(() => useOllamaStatus());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(typeof result.current.refetch).toBe('function');
  });
});

describe('useGenerate', () => {
  const mockGenerationResponse: GenerationResponse = {
    response: 'This is a generated response about quantum computing.',
    model: 'llama3:8b',
    durationMs: 1500,
    tokensPerSecond: 25.5,
    timeToFirstTokenMs: 100,
    promptTokens: 10,
    completionTokens: 20,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should initialise with correct state', () => {
    const { result } = renderHook(() => useGenerate());

    expect(result.current.response).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(typeof result.current.generate).toBe('function');
    expect(typeof result.current.reset).toBe('function');
  });

  it('should generate response successfully', async () => {
    (api.post as Mock).mockResolvedValueOnce(mockGenerationResponse);

    const { result } = renderHook(() => useGenerate());

    let generateResult: GenerationResponse | null = null;

    await act(async () => {
      generateResult = await result.current.generate({
        model: 'llama3:8b',
        prompt: 'Explain quantum computing',
        temperature: 0.7,
      });
    });

    expect(result.current.response).toEqual(mockGenerationResponse);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(generateResult).toEqual(mockGenerationResponse);
    expect(api.post).toHaveBeenCalledWith('/ollama/generate', {
      model: 'llama3:8b',
      prompt: 'Explain quantum computing',
      temperature: 0.7,
    });
  });

  it('should set loading state during generation', async () => {
    let resolvePromise: (value: GenerationResponse) => void;
    const pendingPromise = new Promise<GenerationResponse>((resolve) => {
      resolvePromise = resolve;
    });
    (api.post as Mock).mockReturnValueOnce(pendingPromise);

    const { result } = renderHook(() => useGenerate());

    expect(result.current.isLoading).toBe(false);

    let generatePromise: Promise<GenerationResponse | null>;

    act(() => {
      generatePromise = result.current.generate({
        model: 'llama3:8b',
        prompt: 'Test prompt',
      });
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(true);
    });

    await act(async () => {
      resolvePromise!(mockGenerationResponse);
      await generatePromise;
    });

    expect(result.current.isLoading).toBe(false);
  });

  it('should handle generation error', async () => {
    const mockError = new Error('Generation failed');
    (api.post as Mock).mockRejectedValueOnce(mockError);

    const { result } = renderHook(() => useGenerate());

    let generateResult: GenerationResponse | null = null;

    await act(async () => {
      generateResult = await result.current.generate({
        model: 'llama3:8b',
        prompt: 'Test prompt',
      });
    });

    expect(result.current.response).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Generation failed');
    expect(generateResult).toBeNull();
  });

  it('should handle non-Error rejection', async () => {
    (api.post as Mock).mockRejectedValueOnce('String error');

    const { result } = renderHook(() => useGenerate());

    await act(async () => {
      await result.current.generate({
        model: 'llama3:8b',
        prompt: 'Test prompt',
      });
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Generation request failed');
  });

  it('should reset state when calling reset', async () => {
    (api.post as Mock).mockResolvedValueOnce(mockGenerationResponse);

    const { result } = renderHook(() => useGenerate());

    await act(async () => {
      await result.current.generate({
        model: 'llama3:8b',
        prompt: 'Test prompt',
      });
    });

    expect(result.current.response).toEqual(mockGenerationResponse);

    act(() => {
      result.current.reset();
    });

    expect(result.current.response).toBeNull();
    expect(result.current.error).toBeNull();
    expect(result.current.isLoading).toBe(false);
  });

  it('should clear error on new generation request', async () => {
    const mockError = new Error('First request failed');
    (api.post as Mock).mockRejectedValueOnce(mockError);

    const { result } = renderHook(() => useGenerate());

    await act(async () => {
      await result.current.generate({
        model: 'llama3:8b',
        prompt: 'Test prompt',
      });
    });

    expect(result.current.error).not.toBeNull();

    (api.post as Mock).mockResolvedValueOnce(mockGenerationResponse);

    await act(async () => {
      await result.current.generate({
        model: 'llama3:8b',
        prompt: 'Test prompt',
      });
    });

    expect(result.current.error).toBeNull();
    expect(result.current.response).toEqual(mockGenerationResponse);
  });

  it('should pass all request parameters correctly', async () => {
    (api.post as Mock).mockResolvedValueOnce(mockGenerationResponse);

    const { result } = renderHook(() => useGenerate());

    const request = {
      model: 'qwen2.5-coder:7b',
      prompt: 'Write a function',
      systemPrompt: 'You are a helpful assistant',
      temperature: 0.5,
      topP: 0.9,
      topK: 40,
      contextWindow: 4096,
      maxTokens: 1000,
      stream: false,
    };

    await act(async () => {
      await result.current.generate(request);
    });

    expect(api.post).toHaveBeenCalledWith('/ollama/generate', request);
  });
});

describe('clearModelsCache', () => {
  const mockModelsResponse: ModelsResponse = {
    models: ['llama3:8b'],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    clearModelsCache();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should clear the cache and force refetch', async () => {
    (api.get as Mock).mockResolvedValue(mockModelsResponse);

    // First fetch
    const { result: result1 } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result1.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(1);

    // Clear cache
    clearModelsCache();

    // New hook should fetch again
    const { result: result2 } = renderHook(() => useOllamaModels());

    await waitFor(() => {
      expect(result2.current.isLoading).toBe(false);
    });

    expect(api.get).toHaveBeenCalledTimes(2);
  });
});

const mockApi = api as unknown as { get: Mock };

describe('useOllama', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearModelsCache(); // Clear shared cache between tests
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

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(mockApi.get).toHaveBeenCalledTimes(2);
      expect(mockApi.get).toHaveBeenNthCalledWith(1, '/ollama/status');
      expect(mockApi.get).toHaveBeenNthCalledWith(2, '/ollama/models');
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
      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Cannot connect to Ollama service');
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

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Ollama service is not available');
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
      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Network error - unable to connect to the server');
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
      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Connection refused');
      expect(result.current.models).toEqual([]);
    });

    it('handles non-Error exceptions', async () => {
      mockApi.get.mockRejectedValue('String error');

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Failed to connect to Ollama service');
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

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Connection failed');

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

  describe('caching', () => {
    it('uses shared cache from useOllamaModels', async () => {
      mockApi.get.mockImplementation((url: string) => {
        if (url === '/ollama/status') {
          return Promise.resolve({
            available: true,
            baseUrl: 'http://localhost:11434',
            modelCount: 2,
            message: 'Ollama is running',
          });
        }
        if (url === '/ollama/models') {
          return Promise.resolve({ models: ['llama3:8b', 'codellama:7b'] });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      // First call populates cache
      const { result: result1 } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result1.current.isLoading).toBe(false);
      });

      expect(result1.current.models).toEqual(['llama3:8b', 'codellama:7b']);
      // Called twice: status + models
      expect(mockApi.get).toHaveBeenCalledTimes(2);

      // Second hook should use cached models (only calls status)
      const { result: result2 } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result2.current.isLoading).toBe(false);
      });

      expect(result2.current.models).toEqual(['llama3:8b', 'codellama:7b']);
      // Only status was called again, models came from cache
      expect(mockApi.get).toHaveBeenCalledTimes(3); // 2 from first + 1 status from second
    });

    it('bypasses cache on refetch', async () => {
      let modelCallCount = 0;
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
          modelCallCount++;
          return Promise.resolve({
            models: modelCallCount === 1 ? ['llama3:8b'] : ['llama3:8b', 'mistral:7b'],
          });
        }
        return Promise.reject(new Error('Unknown endpoint'));
      });

      const { result } = renderHook(() => useOllama());

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.models).toEqual(['llama3:8b']);
      expect(modelCallCount).toBe(1);

      // Refetch should bypass cache and fetch fresh data
      await act(async () => {
        await result.current.refetch();
      });

      await waitFor(() => {
        expect(result.current.models).toEqual(['llama3:8b', 'mistral:7b']);
      });

      expect(modelCallCount).toBe(2);
    });
  });
});
