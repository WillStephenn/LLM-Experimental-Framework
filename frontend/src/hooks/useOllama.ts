/**
 * useOllama Hook
 *
 * Custom React hook for interacting with the Ollama API.
 * Provides model fetching, status checking, and loading/error state management.
 *
 * @module hooks/useOllama
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { api } from '@/api/client';
import type { ModelsResponse, OllamaStatusResponse } from '@/types';

/**
 * State interface for the useOllama hook
 */
export interface UseOllamaState {
  /** List of available model names */
  models: string[];
  /** Whether models are currently being fetched */
  isLoading: boolean;
  /** Error message if fetching failed */
  error: string | null;
  /** Whether Ollama service is available */
  isAvailable: boolean;
}

/**
 * Return type for the useOllama hook
 */
export interface UseOllamaReturn extends UseOllamaState {
  /** Refresh the models list */
  refetch: () => Promise<void>;
}

/**
 * Fetches models from the Ollama API.
 * This function is separate from the hook to avoid triggering ESLint warnings
 * about setState in effects.
 */
async function fetchOllamaModels(): Promise<UseOllamaState> {
  try {
    // First check if Ollama is available
    const statusResponse = await api.get<OllamaStatusResponse>('/ollama/status');

    if (!statusResponse.available) {
      return {
        models: [],
        isLoading: false,
        error: statusResponse.message || 'Ollama service is not available',
        isAvailable: false,
      };
    }

    // Fetch models if Ollama is available
    const modelsResponse = await api.get<ModelsResponse>('/ollama/models');

    return {
      models: modelsResponse.models,
      isLoading: false,
      error: null,
      isAvailable: true,
    };
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : 'Failed to connect to Ollama service';

    return {
      models: [],
      isLoading: false,
      error: errorMessage,
      isAvailable: false,
    };
  }
}

/**
 * Custom hook for fetching and managing Ollama models.
 *
 * Fetches available models from the Ollama API and provides loading,
 * error, and availability states.
 *
 * @returns {UseOllamaReturn} Hook state and actions
 *
 * @example
 * ```tsx
 * const { models, isLoading, error, isAvailable, refetch } = useOllama();
 *
 * if (isLoading) return <Spinner />;
 * if (error) return <ErrorMessage message={error} />;
 * if (!isAvailable) return <OllamaUnavailable />;
 *
 * return <ModelList models={models} />;
 * ```
 */
export function useOllama(): UseOllamaReturn {
  const [state, setState] = useState<UseOllamaState>({
    models: [],
    isLoading: true,
    error: null,
    isAvailable: false,
  });

  const isMounted = useRef(true);

  const refetch = useCallback(async (): Promise<void> => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    const result = await fetchOllamaModels();
    if (isMounted.current) {
      setState(result);
    }
  }, []);

  useEffect(() => {
    isMounted.current = true;
    let isCurrentEffect = true;

    const loadModels = async (): Promise<void> => {
      const result = await fetchOllamaModels();
      if (isCurrentEffect && isMounted.current) {
        setState(result);
      }
    };

    void loadModels();

    return (): void => {
      isCurrentEffect = false;
      isMounted.current = false;
    };
  }, []);

  return {
    ...state,
    refetch,
  };
}
