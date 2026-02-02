/**
 * Tests for useDocuments Hook
 *
 * @module hooks/useDocuments.test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useDocuments } from './useDocuments';
import { api } from '@/api/client';
import type { DocumentResponse } from '@/types';

vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockDocuments: DocumentResponse[] = [
  {
    id: 1,
    filename: 'architecture.pdf',
    chunkCount: 42,
    createdAt: '2025-11-27T10:00:00Z',
  },
  {
    id: 2,
    filename: 'notes.txt',
    chunkCount: 12,
    createdAt: '2025-11-27T10:05:00Z',
  },
];

describe('useDocuments', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns initial state', () => {
    const { result } = renderHook(() => useDocuments());
    expect(result.current.documents).toEqual([]);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('fetches documents', async () => {
    vi.mocked(api.get).mockResolvedValueOnce(mockDocuments);
    const { result } = renderHook(() => useDocuments());

    await act(async () => {
      await result.current.fetchDocuments();
    });

    expect(api.get).toHaveBeenCalledWith('/documents');
    expect(result.current.documents).toEqual(mockDocuments);
  });

  it('handles fetch errors', async () => {
    const error = new Error('Network error');
    vi.mocked(api.get).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useDocuments());

    await act(async () => {
      await result.current.fetchDocuments();
    });

    expect(result.current.error).toEqual(error);
  });

  it('uploads a document', async () => {
    vi.mocked(api.post).mockResolvedValueOnce(mockDocuments[0]);
    const { result } = renderHook(() => useDocuments());
    const file = new File(['content'], 'sample.pdf', { type: 'application/pdf' });

    await act(async () => {
      await result.current.uploadDocument(file);
    });

    expect(api.post).toHaveBeenCalledWith(
      '/documents',
      expect.any(FormData),
      expect.objectContaining({ headers: { 'Content-Type': 'multipart/form-data' } })
    );
    expect(result.current.documents).toEqual([mockDocuments[0]]);
  });

  it('throws when upload fails', async () => {
    const error = new Error('Upload failed');
    vi.mocked(api.post).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useDocuments());
    const file = new File(['content'], 'sample.pdf', { type: 'application/pdf' });

    await act(async () => {
      await expect(result.current.uploadDocument(file)).rejects.toThrow('Upload failed');
    });

    expect(result.current.error).toEqual(error);
  });

  it('deletes a document', async () => {
    vi.mocked(api.get).mockResolvedValueOnce(mockDocuments);
    vi.mocked(api.delete).mockResolvedValueOnce(undefined);
    const { result } = renderHook(() => useDocuments());

    await act(async () => {
      await result.current.fetchDocuments();
    });

    await act(async () => {
      await result.current.deleteDocument(1);
    });

    expect(api.delete).toHaveBeenCalledWith('/documents/1');
    expect(result.current.documents).toHaveLength(1);
  });

  it('throws when delete fails', async () => {
    const error = new Error('Delete failed');
    vi.mocked(api.delete).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useDocuments());

    await act(async () => {
      await expect(result.current.deleteDocument(1)).rejects.toThrow('Delete failed');
    });

    expect(result.current.error).toEqual(error);
  });

  it('clears errors', async () => {
    const error = new Error('Network error');
    vi.mocked(api.get).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useDocuments());

    await act(async () => {
      await result.current.fetchDocuments();
    });

    act(() => {
      result.current.clearError();
    });

    expect(result.current.error).toBeNull();
  });

  it('sets loading state during fetch', async () => {
    let resolvePromise: (value: DocumentResponse[]) => void;
    const promise = new Promise<DocumentResponse[]>((resolve) => {
      resolvePromise = resolve;
    });
    vi.mocked(api.get).mockReturnValueOnce(promise);
    const { result } = renderHook(() => useDocuments());

    void act(() => {
      void result.current.fetchDocuments();
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(true);
    });

    await act(async () => {
      resolvePromise!(mockDocuments);
      await promise;
    });
  });
});
