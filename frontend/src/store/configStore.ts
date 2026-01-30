import { create } from 'zustand';
import type { HyperparametersState, ConfigState } from '@/types';

// Re-export types for backward compatibility
export type { HyperparametersState, ConfigState, Hyperparameters } from '@/types';

/**
 * Actions for updating configuration state.
 */
export interface ConfigActions {
  /** Set the selected model */
  setModel: (model: string | null) => void;
  /** Set temperature (0.0 - 2.0) */
  setTemperature: (temperature: number) => void;
  /** Set top-p (0.0 - 1.0) */
  setTopP: (topP: number) => void;
  /** Set top-k (1 - 100) */
  setTopK: (topK: number) => void;
  /** Set context window size (512 - 128000) */
  setContextWindow: (contextWindow: number) => void;
  /** Set max tokens (null = no limit) */
  setMaxTokens: (maxTokens: number | null) => void;
  /** Reset all configuration to defaults */
  reset: () => void;
}

/**
 * Default hyperparameters matching API contract defaults.
 */
export const DEFAULT_HYPERPARAMETERS: HyperparametersState = {
  temperature: 0.7,
  topP: 0.9,
  topK: 40,
  contextWindow: 4096,
  maxTokens: null,
};

/**
 * Clamps a value to the specified range.
 * @param value - The value to clamp
 * @param min - Minimum allowed value
 * @param max - Maximum allowed value
 * @returns The clamped value
 */
const clamp = (value: number, min: number, max: number): number =>
  Math.max(min, Math.min(max, value));

/**
 * Default configuration state.
 */
export const DEFAULT_CONFIG_STATE: ConfigState = {
  model: null,
  hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
};

/**
 * Zustand store for global model configuration.
 *
 * Manages the selected model and hyperparameters used across the application.
 * Configuration persists during the session and can be reset to defaults.
 *
 * @example
 * ```tsx
 * const model = useConfigStore((state) => state.model);
 * const setModel = useConfigStore((state) => state.setModel);
 * const { temperature, topP } = useConfigStore((state) => state.hyperparameters);
 * ```
 */
export const useConfigStore = create<ConfigState & ConfigActions>()((set) => ({
  // Initial state
  model: DEFAULT_CONFIG_STATE.model,
  hyperparameters: { ...DEFAULT_CONFIG_STATE.hyperparameters },

  // Actions
  setModel: (model: string | null): void => set({ model }),

  setTemperature: (temperature: number): void =>
    set((state) => ({
      hyperparameters: { ...state.hyperparameters, temperature: clamp(temperature, 0, 2) },
    })),

  setTopP: (topP: number): void =>
    set((state) => ({
      hyperparameters: { ...state.hyperparameters, topP: clamp(topP, 0, 1) },
    })),

  setTopK: (topK: number): void =>
    set((state) => ({
      hyperparameters: { ...state.hyperparameters, topK: clamp(Math.round(topK), 1, 100) },
    })),

  setContextWindow: (contextWindow: number): void =>
    set((state) => ({
      hyperparameters: {
        ...state.hyperparameters,
        contextWindow: clamp(Math.round(contextWindow), 512, 128000),
      },
    })),

  setMaxTokens: (maxTokens: number | null): void =>
    set((state) => ({
      hyperparameters: {
        ...state.hyperparameters,
        maxTokens: maxTokens === null ? null : Math.max(1, Math.round(maxTokens)),
      },
    })),

  reset: (): void =>
    set({
      model: DEFAULT_CONFIG_STATE.model,
      hyperparameters: { ...DEFAULT_CONFIG_STATE.hyperparameters },
    }),
}));
