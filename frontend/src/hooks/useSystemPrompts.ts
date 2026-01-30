/**
 * useSystemPrompts Hook
 *
 * Provides CRUD operations for SystemPrompt entities with loading and error states.
 *
 * @module hooks/useSystemPrompts
 * @see docs/API-Contract.md for the complete API specification
 */
import { useState, useCallback } from 'react';
import { api } from '@/api/client';
import type { SystemPromptRequest, SystemPromptResponse } from '@/types';

/**
 * State and operations returned by the useSystemPrompts hook.
 */
export interface UseSystemPromptsReturn {
  /** List of system prompts. */
  systemPrompts: SystemPromptResponse[];
  /** Currently selected system prompt. */
  systemPrompt: SystemPromptResponse | null;
  /** Whether a request is in progress. */
  isLoading: boolean;
  /** Error from the most recent operation. */
  error: Error | null;
  /** Fetch all system prompts. */
  fetchSystemPrompts: () => Promise<void>;
  /** Fetch a single system prompt by ID. */
  fetchSystemPrompt: (id: number) => Promise<void>;
  /** Create a new system prompt. */
  createSystemPrompt: (data: SystemPromptRequest) => Promise<SystemPromptResponse>;
  /** Update an existing system prompt. */
  updateSystemPrompt: (id: number, data: SystemPromptRequest) => Promise<SystemPromptResponse>;
  /** Delete a system prompt. */
  deleteSystemPrompt: (id: number) => Promise<void>;
  /** Clear the current error state. */
  clearError: () => void;
}

/**
 * Hook for managing SystemPrompt CRUD operations.
 *
 * Provides methods to fetch, create, update, and delete system prompts
 * with built-in loading and error state management.
 *
 * @returns Object containing system prompt data, loading state, error state, and CRUD methods
 *
 * @example
 * ```tsx
 * const { systemPrompts, isLoading, error, fetchSystemPrompts, createSystemPrompt } = useSystemPrompts();
 *
 * useEffect(() => {
 *   fetchSystemPrompts();
 * }, [fetchSystemPrompts]);
 *
 * const handleCreate = async () => {
 *   await createSystemPrompt({ alias: 'code-assistant', content: '...' });
 * };
 * ```
 */
export const useSystemPrompts = (): UseSystemPromptsReturn => {
  const [systemPrompts, setSystemPrompts] = useState<SystemPromptResponse[]>([]);
  const [systemPrompt, setSystemPrompt] = useState<SystemPromptResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  /**
   * Fetch all system prompts.
   */
  const fetchSystemPrompts = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.get<SystemPromptResponse[]>('/system-prompts');
      setSystemPrompts(response);
    } catch (err) {
      const errorInstance =
        err instanceof Error ? err : new Error('Failed to fetch system prompts');
      setError(errorInstance);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Fetch a single system prompt by ID.
   */
  const fetchSystemPrompt = useCallback(async (id: number): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.get<SystemPromptResponse>(`/system-prompts/${id}`);
      setSystemPrompt(response);
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to fetch system prompt');
      setError(errorInstance);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Create a new system prompt.
   */
  const createSystemPrompt = useCallback(
    async (data: SystemPromptRequest): Promise<SystemPromptResponse> => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await api.post<SystemPromptResponse>('/system-prompts', data);
        setSystemPrompts((prev) => [...prev, response]);
        return response;
      } catch (err) {
        const errorInstance =
          err instanceof Error ? err : new Error('Failed to create system prompt');
        setError(errorInstance);
        throw errorInstance;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  /**
   * Update an existing system prompt.
   */
  const updateSystemPrompt = useCallback(
    async (id: number, data: SystemPromptRequest): Promise<SystemPromptResponse> => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await api.put<SystemPromptResponse>(`/system-prompts/${id}`, data);
        setSystemPrompts((prev) => prev.map((sp) => (sp.id === id ? response : sp)));
        if (systemPrompt?.id === id) {
          setSystemPrompt(response);
        }
        return response;
      } catch (err) {
        const errorInstance =
          err instanceof Error ? err : new Error('Failed to update system prompt');
        setError(errorInstance);
        throw errorInstance;
      } finally {
        setIsLoading(false);
      }
    },
    [systemPrompt?.id]
  );

  /**
   * Delete a system prompt.
   */
  const deleteSystemPrompt = useCallback(
    async (id: number): Promise<void> => {
      setIsLoading(true);
      setError(null);
      try {
        await api.delete(`/system-prompts/${id}`);
        setSystemPrompts((prev) => prev.filter((sp) => sp.id !== id));
        if (systemPrompt?.id === id) {
          setSystemPrompt(null);
        }
      } catch (err) {
        const errorInstance =
          err instanceof Error ? err : new Error('Failed to delete system prompt');
        setError(errorInstance);
        throw errorInstance;
      } finally {
        setIsLoading(false);
      }
    },
    [systemPrompt?.id]
  );

  /**
   * Clear the current error state.
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    systemPrompts,
    systemPrompt,
    isLoading,
    error,
    fetchSystemPrompts,
    fetchSystemPrompt,
    createSystemPrompt,
    updateSystemPrompt,
    deleteSystemPrompt,
    clearError,
  };
};
