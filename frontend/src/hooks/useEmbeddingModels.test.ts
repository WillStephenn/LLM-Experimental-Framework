/**
 * Tests for useEmbeddingModels Hook
 *
 * @module hooks/useEmbeddingModels.test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useEmbeddingModels } from './useEmbeddingModels';
import { api } from '@/api/client';
import type { EmbeddingModelResponse } from '@/types';

vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
  },
}));

const mockModels: EmbeddingModelResponse[] = [
  {
    id: 1,
    name: 'Nomic Embed Text',
    ollamaModelName: 'nomic-embed-text',
    dimensions: 768,
    createdAt: '2025-11-27T10:00:00Z',
  },
];

describe('useEmbeddingModels', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns initial state', () => {
    const { result } = renderHook(() => useEmbeddingModels());
    expect(result.current.embeddingModels).toEqual([]);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('fetches embedding models', async () => {
    vi.mocked(api.get).mockResolvedValueOnce(mockModels);
    const { result } = renderHook(() => useEmbeddingModels());

    await act(async () => {
      await result.current.fetchEmbeddingModels();
    });

    expect(api.get).toHaveBeenCalledWith('/embedding-models');
    expect(result.current.embeddingModels).toEqual(mockModels);
  });

  it('handles fetch errors', async () => {
    const error = new Error('Network error');
    vi.mocked(api.get).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useEmbeddingModels());

    await act(async () => {
      await result.current.fetchEmbeddingModels();
    });

    expect(result.current.error).toEqual(error);
  });

  it('clears errors', async () => {
    const error = new Error('Network error');
    vi.mocked(api.get).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useEmbeddingModels());

    await act(async () => {
      await result.current.fetchEmbeddingModels();
    });

    act(() => {
      result.current.clearError();
    });

    expect(result.current.error).toBeNull();
  });
});
