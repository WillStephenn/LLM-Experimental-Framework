import { describe, it, expect, beforeEach } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { DEFAULT_RAG_CONFIG, useRagConfigStore } from './ragConfigStore';

describe('ragConfigStore', () => {
  beforeEach(() => {
    act(() => {
      useRagConfigStore.getState().reset();
    });
  });

  it('uses default configuration values', () => {
    const { result } = renderHook(() => useRagConfigStore());
    expect(result.current.embeddingModel).toBeNull();
    expect(result.current.chunkSize).toBe(DEFAULT_RAG_CONFIG.chunkSize);
    expect(result.current.chunkOverlap).toBe(DEFAULT_RAG_CONFIG.chunkOverlap);
  });

  it('updates the embedding model', () => {
    const { result } = renderHook(() => useRagConfigStore());
    act(() => {
      result.current.setEmbeddingModel('nomic-embed-text');
    });
    expect(result.current.embeddingModel).toBe('nomic-embed-text');
  });

  it('clamps chunk size within bounds', () => {
    const { result } = renderHook(() => useRagConfigStore());
    act(() => {
      result.current.setChunkSize(50);
    });
    expect(result.current.chunkSize).toBe(100);
    act(() => {
      result.current.setChunkSize(2500);
    });
    expect(result.current.chunkSize).toBe(2000);
  });

  it('adjusts overlap when chunk size decreases', () => {
    const { result } = renderHook(() => useRagConfigStore());
    act(() => {
      result.current.setChunkOverlap(400);
    });
    expect(result.current.chunkOverlap).toBe(400);
    act(() => {
      result.current.setChunkSize(300);
    });
    expect(result.current.chunkOverlap).toBe(299);
  });

  it('clamps overlap to valid range', () => {
    const { result } = renderHook(() => useRagConfigStore());
    act(() => {
      result.current.setChunkOverlap(600);
    });
    expect(result.current.chunkOverlap).toBe(499);
    act(() => {
      result.current.setChunkOverlap(-10);
    });
    expect(result.current.chunkOverlap).toBe(0);
  });

  it('resets to defaults', () => {
    const { result } = renderHook(() => useRagConfigStore());
    act(() => {
      result.current.setEmbeddingModel('nomic-embed-text');
      result.current.setChunkSize(800);
      result.current.setChunkOverlap(120);
    });

    act(() => {
      result.current.reset();
    });

    expect(result.current.embeddingModel).toBeNull();
    expect(result.current.chunkSize).toBe(DEFAULT_RAG_CONFIG.chunkSize);
    expect(result.current.chunkOverlap).toBe(DEFAULT_RAG_CONFIG.chunkOverlap);
  });
});
