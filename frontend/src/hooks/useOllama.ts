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

/**
 * State interface for the combined useOllama hook.
 * Combines status checking with model fetching for convenience.
 */
export interface UseOllamaState {
  /** List of available model names. */
  models: string[];
  /** Whether the request is currently loading. */
  isLoading: boolean;
  /** Error that occurred during fetching, if any. */
  error: Error | null;
  /** Whether Ollama service is available. */
  isAvailable: boolean;
}

/**
 * Return type for the combined useOllama hook.
 */
export interface UseOllamaReturn extends UseOllamaState {
  /** Refresh the models list. */
  refetch: () => Promise<void>;
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

  const fetchModels = useCallback(async (forceRefresh = false): Promise<void> => {
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

  const refetch = useCallback(() => {
    void fetchModels(true);
  }, [fetchModels]);

  useEffect(() => {
    isMountedRef.current = true;
    void fetchModels();

    return (): void => {
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

  const fetchStatus = useCallback(async (): Promise<void> => {
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

  const refetch = useCallback(() => {
    void fetchStatus();
  }, [fetchStatus]);

  useEffect(() => {
    isMountedRef.current = true;
    void fetchStatus();

    return (): void => {
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

    return (): void => {
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

  const reset = useCallback(() => {
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

/**
 * Fetches models from the Ollama API with a preceding status check.
 * Extracted from the hook so the fetch logic can be reused (e.g. in effects
 * and refetch handlers) and tested in isolation without React hook concerns.
 *
 * @param forceRefresh - If true, bypasses the shared models cache
 * @internal
 */
async function fetchOllamaWithStatus(forceRefresh = false): Promise<UseOllamaState> {
  try {
    // First check if Ollama is available
    const statusResponse = await api.get<OllamaStatusResponse>('/ollama/status');

    if (!statusResponse.available) {
      return {
        models: [],
        isLoading: false,
        error: new Error(statusResponse.message || 'Ollama service is not available'),
        isAvailable: false,
      };
    }

    // Use shared cache if valid and not forcing refresh
    if (!forceRefresh && isCacheValid(modelsCache)) {
      return {
        models: modelsCache!.data,
        isLoading: false,
        error: null,
        isAvailable: true,
      };
    }

    // Fetch models if Ollama is available
    const modelsResponse = await api.get<ModelsResponse>('/ollama/models');

    // Update shared cache
    modelsCache = {
      data: modelsResponse.models,
      timestamp: Date.now(),
    };

    return {
      models: modelsResponse.models,
      isLoading: false,
      error: null,
      isAvailable: true,
    };
  } catch (err) {
    const error = err instanceof Error ? err : new Error('Failed to connect to Ollama service');

    return {
      models: [],
      isLoading: false,
      error,
      isAvailable: false,
    };
  }
}

/**
 * Combined hook for fetching and managing Ollama models with status checking.
 *
 * This is a convenience hook that checks Ollama availability before fetching models.
 * It combines the functionality of useOllamaStatus and useOllamaModels into a single call.
 *
 * Use this hook when you need both status checking and model fetching together,
 * such as in the ModelSelector component.
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
  const requestIdRef = useRef(0);

  const refetch = useCallback(async (): Promise<void> => {
    const currentRequestId = ++requestIdRef.current;
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    const result = await fetchOllamaWithStatus(true); // Force refresh on manual refetch
    // Only update state if this is still the latest request and component is mounted
    if (isMounted.current && currentRequestId === requestIdRef.current) {
      setState(result);
    }
  }, []);

  useEffect(() => {
    isMounted.current = true;
    const currentRequestId = ++requestIdRef.current;

    const loadModels = async (): Promise<void> => {
      const result = await fetchOllamaWithStatus();
      // Only update state if this is still the latest request and component is mounted
      if (isMounted.current && currentRequestId === requestIdRef.current) {
        setState(result);
      }
    };

    void loadModels();

    return (): void => {
      isMounted.current = false;
    };
  }, []);

  return {
    ...state,
    refetch,
  };
}
