import { describe, it, expect, beforeEach } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useConfigStore, DEFAULT_HYPERPARAMETERS, DEFAULT_CONFIG_STATE } from './configStore';

describe('configStore', () => {
  // Reset store before each test to ensure isolation
  beforeEach(() => {
    act(() => {
      useConfigStore.getState().reset();
    });
  });

  describe('initial state', () => {
    it('has null model by default', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.model));
      expect(result.current).toBeNull();
    });

    it('has default hyperparameters', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.hyperparameters));
      expect(result.current).toEqual(DEFAULT_HYPERPARAMETERS);
    });

    it('has correct default temperature', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.hyperparameters));
      expect(result.current.temperature).toBe(0.7);
    });

    it('has correct default topP', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.hyperparameters));
      expect(result.current.topP).toBe(0.9);
    });

    it('has correct default topK', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.hyperparameters));
      expect(result.current.topK).toBe(40);
    });

    it('has correct default contextWindow', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.hyperparameters));
      expect(result.current.contextWindow).toBe(4096);
    });

    it('has null maxTokens by default', () => {
      const { result } = renderHook(() => useConfigStore((state) => state.hyperparameters));
      expect(result.current.maxTokens).toBeNull();
    });
  });

  describe('setModel', () => {
    it('sets the model', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setModel('qwen2.5-coder:7b');
      });

      expect(result.current.model).toBe('qwen2.5-coder:7b');
    });

    it('can set model to null', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setModel('some-model');
      });
      expect(result.current.model).toBe('some-model');

      act(() => {
        result.current.setModel(null);
      });
      expect(result.current.model).toBeNull();
    });
  });

  describe('setTemperature', () => {
    it('sets temperature', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTemperature(1.5);
      });

      expect(result.current.hyperparameters.temperature).toBe(1.5);
    });

    it('clamps temperature to minimum of 0', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTemperature(-0.5);
      });

      expect(result.current.hyperparameters.temperature).toBe(0);
    });

    it('clamps temperature to maximum of 2', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTemperature(3.0);
      });

      expect(result.current.hyperparameters.temperature).toBe(2);
    });

    it('preserves other hyperparameters when setting temperature', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopK(50);
        result.current.setTemperature(1.2);
      });

      expect(result.current.hyperparameters.temperature).toBe(1.2);
      expect(result.current.hyperparameters.topK).toBe(50);
    });
  });

  describe('setTopP', () => {
    it('sets topP', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopP(0.5);
      });

      expect(result.current.hyperparameters.topP).toBe(0.5);
    });

    it('clamps topP to minimum of 0', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopP(-0.2);
      });

      expect(result.current.hyperparameters.topP).toBe(0);
    });

    it('clamps topP to maximum of 1', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopP(1.5);
      });

      expect(result.current.hyperparameters.topP).toBe(1);
    });

    it('preserves other hyperparameters when setting topP', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTemperature(1.0);
        result.current.setTopP(0.8);
      });

      expect(result.current.hyperparameters.topP).toBe(0.8);
      expect(result.current.hyperparameters.temperature).toBe(1.0);
    });
  });

  describe('setTopK', () => {
    it('sets topK', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopK(80);
      });

      expect(result.current.hyperparameters.topK).toBe(80);
    });

    it('clamps topK to minimum of 1', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopK(0);
      });

      expect(result.current.hyperparameters.topK).toBe(1);
    });

    it('clamps topK to maximum of 100', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopK(150);
      });

      expect(result.current.hyperparameters.topK).toBe(100);
    });

    it('rounds decimal topK values', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTopK(42.7);
      });

      expect(result.current.hyperparameters.topK).toBe(43);
    });

    it('preserves other hyperparameters when setting topK', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setContextWindow(8192);
        result.current.setTopK(60);
      });

      expect(result.current.hyperparameters.topK).toBe(60);
      expect(result.current.hyperparameters.contextWindow).toBe(8192);
    });
  });

  describe('setContextWindow', () => {
    it('sets contextWindow', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setContextWindow(16384);
      });

      expect(result.current.hyperparameters.contextWindow).toBe(16384);
    });

    it('clamps contextWindow to minimum of 512', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setContextWindow(100);
      });

      expect(result.current.hyperparameters.contextWindow).toBe(512);
    });

    it('clamps contextWindow to maximum of 128000', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setContextWindow(200000);
      });

      expect(result.current.hyperparameters.contextWindow).toBe(128000);
    });

    it('rounds decimal contextWindow values', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setContextWindow(8192.5);
      });

      expect(result.current.hyperparameters.contextWindow).toBe(8193);
    });

    it('preserves other hyperparameters when setting contextWindow', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setMaxTokens(1000);
        result.current.setContextWindow(32768);
      });

      expect(result.current.hyperparameters.contextWindow).toBe(32768);
      expect(result.current.hyperparameters.maxTokens).toBe(1000);
    });
  });

  describe('setMaxTokens', () => {
    it('sets maxTokens', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setMaxTokens(500);
      });

      expect(result.current.hyperparameters.maxTokens).toBe(500);
    });

    it('can set maxTokens to null', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setMaxTokens(1000);
      });
      expect(result.current.hyperparameters.maxTokens).toBe(1000);

      act(() => {
        result.current.setMaxTokens(null);
      });
      expect(result.current.hyperparameters.maxTokens).toBeNull();
    });

    it('clamps maxTokens to minimum of 1', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setMaxTokens(0);
      });

      expect(result.current.hyperparameters.maxTokens).toBe(1);
    });

    it('clamps negative maxTokens to 1', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setMaxTokens(-10);
      });

      expect(result.current.hyperparameters.maxTokens).toBe(1);
    });

    it('rounds decimal maxTokens values', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setMaxTokens(100.7);
      });

      expect(result.current.hyperparameters.maxTokens).toBe(101);
    });

    it('preserves other hyperparameters when setting maxTokens', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTemperature(0.5);
        result.current.setMaxTokens(2000);
      });

      expect(result.current.hyperparameters.maxTokens).toBe(2000);
      expect(result.current.hyperparameters.temperature).toBe(0.5);
    });
  });

  describe('reset', () => {
    it('resets model to null', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setModel('llama3:8b');
        result.current.reset();
      });

      expect(result.current.model).toBeNull();
    });

    it('resets hyperparameters to defaults', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setTemperature(1.5);
        result.current.setTopP(0.5);
        result.current.setTopK(100);
        result.current.setContextWindow(128000);
        result.current.setMaxTokens(4000);
        result.current.reset();
      });

      expect(result.current.hyperparameters).toEqual(DEFAULT_HYPERPARAMETERS);
    });

    it('resets all state to initial values', () => {
      const { result } = renderHook(() => useConfigStore());

      act(() => {
        result.current.setModel('codellama:7b');
        result.current.setTemperature(2.0);
        result.current.setTopP(0.1);
        result.current.setTopK(5);
        result.current.setContextWindow(512);
        result.current.setMaxTokens(100);
        result.current.reset();
      });

      expect(result.current.model).toBe(DEFAULT_CONFIG_STATE.model);
      expect(result.current.hyperparameters).toEqual(DEFAULT_CONFIG_STATE.hyperparameters);
    });
  });

  describe('exported constants', () => {
    it('exports DEFAULT_HYPERPARAMETERS with correct values', () => {
      expect(DEFAULT_HYPERPARAMETERS).toEqual({
        temperature: 0.7,
        topP: 0.9,
        topK: 40,
        contextWindow: 4096,
        maxTokens: null,
      });
    });

    it('exports DEFAULT_CONFIG_STATE with correct structure', () => {
      expect(DEFAULT_CONFIG_STATE).toEqual({
        model: null,
        hyperparameters: DEFAULT_HYPERPARAMETERS,
      });
    });
  });
});
