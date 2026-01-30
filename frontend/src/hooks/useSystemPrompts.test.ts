/**
 * Tests for useSystemPrompts Hook
 *
 * @module hooks/useSystemPrompts.test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useSystemPrompts } from './useSystemPrompts';
import { api } from '@/api/client';
import type { SystemPromptResponse, SystemPromptRequest } from '@/types';

// Mock the API client
vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockSystemPrompts: SystemPromptResponse[] = [
  {
    id: 1,
    alias: 'code-assistant',
    content: 'You are an expert code reviewer...',
    createdAt: '2025-11-27T10:00:00Z',
  },
  {
    id: 2,
    alias: 'documentation-helper',
    content: 'You are a technical writer...',
    createdAt: '2025-11-27T11:00:00Z',
  },
];

const mockSystemPrompt: SystemPromptResponse = mockSystemPrompts[0];

describe('useSystemPrompts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('initial state', () => {
    it('should have empty initial state', () => {
      const { result } = renderHook(() => useSystemPrompts());

      expect(result.current.systemPrompts).toEqual([]);
      expect(result.current.systemPrompt).toBeNull();
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });

  describe('fetchSystemPrompts', () => {
    it('should fetch all system prompts successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockSystemPrompts);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompts();
      });

      expect(api.get).toHaveBeenCalledWith('/system-prompts');
      expect(result.current.systemPrompts).toEqual(mockSystemPrompts);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle fetch error', async () => {
      const error = new Error('Network error');
      vi.mocked(api.get).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompts();
      });

      expect(result.current.systemPrompts).toEqual([]);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection', async () => {
      vi.mocked(api.get).mockRejectedValueOnce('Unknown error');

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompts();
      });

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Failed to fetch system prompts');
    });

    it('should set loading state during fetch', async () => {
      let resolvePromise: (value: SystemPromptResponse[]) => void;
      const promise = new Promise<SystemPromptResponse[]>((resolve) => {
        resolvePromise = resolve;
      });
      vi.mocked(api.get).mockReturnValueOnce(promise);

      const { result } = renderHook(() => useSystemPrompts());

      void act(() => {
        void result.current.fetchSystemPrompts();
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(true);
      });

      await act(async () => {
        resolvePromise!(mockSystemPrompts);
        await promise;
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });
  });

  describe('fetchSystemPrompt', () => {
    it('should fetch a single system prompt successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockSystemPrompt);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompt(1);
      });

      expect(api.get).toHaveBeenCalledWith('/system-prompts/1');
      expect(result.current.systemPrompt).toEqual(mockSystemPrompt);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle fetch single system prompt error', async () => {
      const error = new Error('Not found');
      vi.mocked(api.get).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompt(999);
      });

      expect(result.current.systemPrompt).toBeNull();
      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for single fetch', async () => {
      vi.mocked(api.get).mockRejectedValueOnce('Not found');

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompt(999);
      });

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Failed to fetch system prompt');
    });
  });

  describe('createSystemPrompt', () => {
    const newPromptRequest: SystemPromptRequest = {
      alias: 'new-assistant',
      content: 'You are a new assistant...',
    };

    const createdPrompt: SystemPromptResponse = {
      id: 3,
      alias: 'new-assistant',
      content: 'You are a new assistant...',
      createdAt: '2025-11-27T12:00:00Z',
    };

    it('should create a system prompt successfully', async () => {
      vi.mocked(api.post).mockResolvedValueOnce(createdPrompt);

      const { result } = renderHook(() => useSystemPrompts());

      let response: SystemPromptResponse | undefined;
      await act(async () => {
        response = await result.current.createSystemPrompt(newPromptRequest);
      });

      expect(api.post).toHaveBeenCalledWith('/system-prompts', newPromptRequest);
      expect(response).toEqual(createdPrompt);
      expect(result.current.systemPrompts).toContainEqual(createdPrompt);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle create error', async () => {
      const error = new Error('Alias already exists');
      vi.mocked(api.post).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await expect(result.current.createSystemPrompt(newPromptRequest)).rejects.toThrow(
          'Alias already exists'
        );
      });

      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for create', async () => {
      vi.mocked(api.post).mockRejectedValueOnce('Validation failed');

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await expect(result.current.createSystemPrompt(newPromptRequest)).rejects.toThrow(
          'Failed to create system prompt'
        );
      });

      expect(result.current.error?.message).toBe('Failed to create system prompt');
    });
  });

  describe('updateSystemPrompt', () => {
    const updateRequest: SystemPromptRequest = {
      alias: 'updated-assistant',
      content: 'Updated content...',
    };

    const updatedPrompt: SystemPromptResponse = {
      ...mockSystemPrompt,
      alias: 'updated-assistant',
      content: 'Updated content...',
    };

    it('should update a system prompt successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockSystemPrompts);
      vi.mocked(api.put).mockResolvedValueOnce(updatedPrompt);

      const { result } = renderHook(() => useSystemPrompts());

      // First fetch prompts to populate the list
      await act(async () => {
        await result.current.fetchSystemPrompts();
      });

      let response: SystemPromptResponse | undefined;
      await act(async () => {
        response = await result.current.updateSystemPrompt(1, updateRequest);
      });

      expect(api.put).toHaveBeenCalledWith('/system-prompts/1', updateRequest);
      expect(response).toEqual(updatedPrompt);
      expect(result.current.systemPrompts.find((sp) => sp.id === 1)).toEqual(updatedPrompt);
    });

    it('should update the current system prompt if it matches', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockSystemPrompt);
      vi.mocked(api.put).mockResolvedValueOnce(updatedPrompt);

      const { result } = renderHook(() => useSystemPrompts());

      // First fetch the single prompt
      await act(async () => {
        await result.current.fetchSystemPrompt(1);
      });

      await act(async () => {
        await result.current.updateSystemPrompt(1, updateRequest);
      });

      expect(result.current.systemPrompt).toEqual(updatedPrompt);
    });

    it('should handle update error', async () => {
      const error = new Error('Update failed');
      vi.mocked(api.put).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await expect(result.current.updateSystemPrompt(1, updateRequest)).rejects.toThrow(
          'Update failed'
        );
      });

      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for update', async () => {
      vi.mocked(api.put).mockRejectedValueOnce('Update failed');

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await expect(result.current.updateSystemPrompt(1, updateRequest)).rejects.toThrow(
          'Failed to update system prompt'
        );
      });
    });
  });

  describe('deleteSystemPrompt', () => {
    it('should delete a system prompt successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockSystemPrompts);
      vi.mocked(api.delete).mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useSystemPrompts());

      // First fetch prompts to populate the list
      await act(async () => {
        await result.current.fetchSystemPrompts();
      });

      await act(async () => {
        await result.current.deleteSystemPrompt(1);
      });

      expect(api.delete).toHaveBeenCalledWith('/system-prompts/1');
      expect(result.current.systemPrompts.find((sp) => sp.id === 1)).toBeUndefined();
      expect(result.current.systemPrompts).toHaveLength(1);
    });

    it('should clear the current system prompt if it matches the deleted one', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockSystemPrompt);
      vi.mocked(api.delete).mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useSystemPrompts());

      // First fetch the single prompt
      await act(async () => {
        await result.current.fetchSystemPrompt(1);
      });

      expect(result.current.systemPrompt).toEqual(mockSystemPrompt);

      await act(async () => {
        await result.current.deleteSystemPrompt(1);
      });

      expect(result.current.systemPrompt).toBeNull();
    });

    it('should handle delete error', async () => {
      const error = new Error('Delete failed');
      vi.mocked(api.delete).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await expect(result.current.deleteSystemPrompt(1)).rejects.toThrow('Delete failed');
      });

      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for delete', async () => {
      vi.mocked(api.delete).mockRejectedValueOnce('Delete failed');

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await expect(result.current.deleteSystemPrompt(1)).rejects.toThrow(
          'Failed to delete system prompt'
        );
      });
    });
  });

  describe('clearError', () => {
    it('should clear the error state', async () => {
      const error = new Error('Test error');
      vi.mocked(api.get).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useSystemPrompts());

      await act(async () => {
        await result.current.fetchSystemPrompts();
      });

      expect(result.current.error).toEqual(error);

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});
