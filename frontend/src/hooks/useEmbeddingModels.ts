/**
 * useEmbeddingModels Hook
 *
 * Provides access to available embedding models for RAG configuration.
 *
 * @module hooks/useEmbeddingModels
 * @see docs/API-Contract.md for the complete API specification
 */
import { useState, useCallback } from 'react';
import { api } from '@/api/client';
import type { EmbeddingModelResponse } from '@/types';

/**
 * State and operations returned by the useEmbeddingModels hook.
 */
export interface UseEmbeddingModelsReturn {
  /** List of embedding models. */
  embeddingModels: EmbeddingModelResponse[];
  /** Whether a request is in progress. */
  isLoading: boolean;
  /** Error from the most recent operation. */
  error: Error | null;
  /** Fetch all embedding models. */
  fetchEmbeddingModels: () => Promise<void>;
  /** Clear the current error state. */
  clearError: () => void;
}

/**
 * Hook for fetching embedding model configurations.
 */
export const useEmbeddingModels = (): UseEmbeddingModelsReturn => {
  const [embeddingModels, setEmbeddingModels] = useState<EmbeddingModelResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchEmbeddingModels = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.get<EmbeddingModelResponse[]>('/embedding-models');
      setEmbeddingModels(response);
    } catch (err) {
      const errorInstance =
        err instanceof Error ? err : new Error('Failed to fetch embedding models');
      setError(errorInstance);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    embeddingModels,
    isLoading,
    error,
    fetchEmbeddingModels,
    clearError,
  };
};
