/**
 * Ollama API Hooks
 *
 * React hooks for interacting with the Ollama API endpoints.
 * Provides hooks for fetching models, checking status, and generating responses.
 *
 * @module hooks/useOllama
 * @see docs/API-Contract.md for endpoint specifications
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { api } from '@/api/client';
import type {
  ModelsResponse,
  OllamaStatusResponse,
  GenerationRequest,
  GenerationResponse,
} from '@/types';

/**
 * State returned by useOllamaModels hook.
 */
export interface UseOllamaModelsState {
  /** List of available model names. */
  models: string[];
  /** Whether the request is currently loading. */
  isLoading: boolean;
  /** Error that occurred during fetching, if any. */
  error: Error | null;
  /** Manually refetch the models list. */
  refetch: () => void;
}

/**
 * State returned by useOllamaStatus hook.
 */
export interface UseOllamaStatusState {
  /** The status response from Ollama, or null if not yet loaded. */
  status: OllamaStatusResponse | null;
  /** Whether Ollama is available and connected. */
  isAvailable: boolean;
  /** Whether the request is currently loading. */
  isLoading: boolean;
  /** Error that occurred during checking, if any. */
  error: Error | null;
  /** Manually refetch the status. */
  refetch: () => void;
}

/**
 * State returned by useGenerate hook.
 */
export interface UseGenerateState {
  /** The generation response, or null if not yet generated. */
  response: GenerationResponse | null;
  /** Whether a generation request is currently in progress. */
  isLoading: boolean;
  /** Error that occurred during generation, if any. */
  error: Error | null;
  /** Execute a generation request. */
  generate: (request: GenerationRequest) => Promise<GenerationResponse | null>;
  /** Reset the state to initial values. */
  reset: () => void;
}

/** Cache duration for models in milliseconds (30 seconds). */
const MODELS_CACHE_DURATION_MS = 30000;

/** Internal cache for models response. */
interface ModelsCache {
  data: string[];
  timestamp: number;
}

let modelsCache: ModelsCache | null = null;

/**
 * Checks if the models cache is still valid.
 * @param cache - The cache to check
 * @returns True if the cache exists and hasn't expired
 */
const isCacheValid = (cache: ModelsCache | null): boolean => {
  if (!cache) return false;
  return Date.now() - cache.timestamp < MODELS_CACHE_DURATION_MS;
};

/**
 * Hook for fetching and caching the list of available Ollama models.
 *
 * Provides automatic caching with a 30-second TTL to minimise API calls.
 * The cache is shared across all instances of this hook.
 *
 * @returns {UseOllamaModelsState} State object containing models list, loading state, and error
 *
 * @example
 * ```tsx
 * const { models, isLoading, error, refetch } = useOllamaModels();
 *
 * if (isLoading) return <Spinner />;
 * if (error) return <Error message={error.message} />;
 *
 * return (
 *   <select>
 *     {models.map(model => (
 *       <option key={model} value={model}>{model}</option>
 *     ))}
 *   </select>
 * );
 * ```
 */
export const useOllamaModels = (): UseOllamaModelsState => {
  const [models, setModels] = useState<string[]>(() =>
    isCacheValid(modelsCache) ? modelsCache!.data : []
  );
  const [isLoading, setIsLoading] = useState<boolean>(!isCacheValid(modelsCache));
  const [error, setError] = useState<Error | null>(null);
  const isMountedRef = useRef<boolean>(true);

  const fetchModels = useCallback(async (forceRefresh = false) => {
    // Use cache if valid and not forcing refresh
    if (!forceRefresh && isCacheValid(modelsCache)) {
      setModels(modelsCache!.data);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await api.get<ModelsResponse>('/ollama/models');
      const modelList = response.models;

      // Update cache
      modelsCache = {
        data: modelList,
        timestamp: Date.now(),
      };

      if (isMountedRef.current) {
        setModels(modelList);
        setError(null);
      }
    } catch (err) {
      if (isMountedRef.current) {
        setError(err instanceof Error ? err : new Error('Failed to fetch models'));
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const refetch = useCallback((): void => {
    void fetchModels(true);
  }, [fetchModels]);

  useEffect(() => {
    isMountedRef.current = true;
    void fetchModels();

    return () => {
      isMountedRef.current = false;
    };
  }, [fetchModels]);

  return { models, isLoading, error, refetch };
};

/**
 * Hook for checking Ollama service connectivity and status.
 *
 * Fetches the current status of the Ollama service, including
 * availability, base URL, and model count.
 *
 * @returns {UseOllamaStatusState} State object containing status, availability flag, loading state, and error
 *
 * @example
 * ```tsx
 * const { status, isAvailable, isLoading, error, refetch } = useOllamaStatus();
 *
 * if (isLoading) return <Spinner />;
 *
 * return (
 *   <div>
 *     <StatusIndicator available={isAvailable} />
 *     {status && (
 *       <p>{status.message} - {status.modelCount} models available</p>
 *     )}
 *   </div>
 * );
 * ```
 */
export const useOllamaStatus = (): UseOllamaStatusState => {
  const [status, setStatus] = useState<OllamaStatusResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);
  const isMountedRef = useRef<boolean>(true);

  const fetchStatus = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await api.get<OllamaStatusResponse>('/ollama/status');

      if (isMountedRef.current) {
        setStatus(response);
        setError(null);
      }
    } catch (err) {
      if (isMountedRef.current) {
        setError(err instanceof Error ? err : new Error('Failed to check Ollama status'));
        setStatus(null);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const refetch = useCallback((): void => {
    void fetchStatus();
  }, [fetchStatus]);

  useEffect(() => {
    isMountedRef.current = true;
    void fetchStatus();

    return () => {
      isMountedRef.current = false;
    };
  }, [fetchStatus]);

  const isAvailable = status?.available ?? false;

  return { status, isAvailable, isLoading, error, refetch };
};

/**
 * Hook for executing generation requests against Ollama.
 *
 * Provides a mutation-style interface for generating text responses.
 * The generation function returns the response and also updates the hook state.
 *
 * @returns {UseGenerateState} State object containing response, loading state, error, and generate function
 *
 * @example
 * ```tsx
 * const { response, isLoading, error, generate, reset } = useGenerate();
 *
 * const handleSubmit = async () => {
 *   const result = await generate({
 *     model: 'llama3:8b',
 *     prompt: 'Explain quantum computing',
 *     temperature: 0.7,
 *   });
 *
 *   if (result) {
 *     console.log('Generated:', result.response);
 *   }
 * };
 *
 * return (
 *   <div>
 *     <button onClick={handleSubmit} disabled={isLoading}>
 *       {isLoading ? 'Generating...' : 'Generate'}
 *     </button>
 *     {error && <p className="error">{error.message}</p>}
 *     {response && <p>{response.response}</p>}
 *   </div>
 * );
 * ```
 */
export const useGenerate = (): UseGenerateState => {
  const [response, setResponse] = useState<GenerationResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);
  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;

    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const generate = useCallback(
    async (request: GenerationRequest): Promise<GenerationResponse | null> => {
      setIsLoading(true);
      setError(null);

      try {
        const result = await api.post<GenerationResponse>('/ollama/generate', request);

        if (isMountedRef.current) {
          setResponse(result);
          setError(null);
        }

        return result;
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Generation request failed');

        if (isMountedRef.current) {
          setError(error);
          setResponse(null);
        }

        return null;
      } finally {
        if (isMountedRef.current) {
          setIsLoading(false);
        }
      }
    },
    []
  );

  const reset = useCallback((): void => {
    setResponse(null);
    setError(null);
    setIsLoading(false);
  }, []);

  return { response, isLoading, error, generate, reset };
};

/**
 * Clears the models cache.
 * Useful for testing or when forcing a fresh fetch is needed.
 * @internal Exported for testing purposes
 */
export const clearModelsCache = (): void => {
  modelsCache = null;
};
