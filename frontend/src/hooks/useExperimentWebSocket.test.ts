/**
 * Tests for useExperimentWebSocket Hook
 *
 * @module hooks/useExperimentWebSocket.test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { StompSubscription, Client as StompClient } from '@stomp/stompjs';
import {
  useExperimentWebSocket,
  calculateReconnectDelay,
  type ExperimentWebSocketState,
} from './useExperimentWebSocket';
import type {
  ProgressPayload,
  RunStartedPayload,
  RunCompletedPayload,
  ExperimentCompletedPayload,
  ExperimentPausedPayload,
  ErrorPayload,
} from '@/types';

// Test types
type MessageHandler = (msg: { body: string }) => void;

interface ClientConfig {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onStompError?: (frame: { headers: Record<string, string>; body: string }) => void;
  onWebSocketClose?: () => void;
  onWebSocketError?: (event: Event) => void;
}

// Mock state
let mockConfig: ClientConfig = {};
let mockIsActive = false;
let capturedMessageHandler: MessageHandler | null = null;

// Create mock functions outside of vi.mock to have stable references
const mockUnsubscribe = vi.fn();
const mockActivate = vi.fn();
const mockDeactivate = vi.fn().mockResolvedValue(undefined);

const mockSubscription: StompSubscription = {
  id: 'test-subscription',
  unsubscribe: mockUnsubscribe,
};

const mockSubscribe = vi.fn((_topic: string, handler: MessageHandler) => {
  capturedMessageHandler = handler;
  return mockSubscription;
});

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation((config: ClientConfig) => {
    mockConfig = config;
    return {
      activate: mockActivate.mockImplementation(() => {
        mockIsActive = true;
      }),
      deactivate: mockDeactivate.mockImplementation(() => {
        mockIsActive = false;
        return Promise.resolve();
      }),
      subscribe: mockSubscribe,
      get active() {
        return mockIsActive;
      },
    } as unknown as StompClient;
  }),
}));

// Helper to trigger WebSocket events
const triggerConnect = (): void => mockConfig.onConnect?.();
const triggerDisconnect = (): void => mockConfig.onDisconnect?.();
const triggerStompError = (headers: Record<string, string>, body: string): void =>
  mockConfig.onStompError?.({ headers, body });
const triggerWebSocketClose = (): void => mockConfig.onWebSocketClose?.();
const triggerWebSocketError = (event: Event): void => mockConfig.onWebSocketError?.(event);

// Helper to send messages
const sendMessage = (message: unknown): void => {
  if (capturedMessageHandler) {
    capturedMessageHandler({ body: JSON.stringify(message) });
  }
};

describe('useExperimentWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockConfig = {};
    mockIsActive = false;
    capturedMessageHandler = null;
  });

  describe('initial state', () => {
    it('should have disconnected status initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.connectionStatus).toBe('disconnected');
    });

    it('should have null progress initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.progress).toBeNull();
    });

    it('should have null currentRun initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.currentRun).toBeNull();
    });

    it('should have null lastCompletedRun initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.lastCompletedRun).toBeNull();
    });

    it('should have null experimentCompleted initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.experimentCompleted).toBeNull();
    });

    it('should have null experimentPaused initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.experimentPaused).toBeNull();
    });

    it('should have null error initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.error).toBeNull();
    });

    it('should have null lastMessage initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.lastMessage).toBeNull();
    });

    it('should have zero reconnectAttempts initially', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));
      expect(result.current.state.reconnectAttempts).toBe(0);
    });
  });

  describe('connect', () => {
    it('should not connect when experimentId is null', () => {
      const { result } = renderHook(() => useExperimentWebSocket(null, { autoConnect: false }));

      act(() => {
        result.current.connect();
      });

      expect(mockActivate).not.toHaveBeenCalled();
    });

    it('should not auto-connect when autoConnect is false', () => {
      renderHook(() => useExperimentWebSocket(1, { autoConnect: false }));
      expect(mockActivate).not.toHaveBeenCalled();
    });

    it('should auto-connect when autoConnect is true and experimentId is provided', () => {
      renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));
      expect(mockActivate).toHaveBeenCalled();
    });

    it('should set connectionStatus to connecting when connect is called', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: false }));

      act(() => {
        result.current.connect();
      });

      expect(result.current.state.connectionStatus).toBe('connecting');
    });

    it('should set connectionStatus to connected when onConnect is called', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: false }));

      act(() => {
        result.current.connect();
      });

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');
    });

    it('should subscribe to the correct topic when connected', () => {
      renderHook(() => useExperimentWebSocket(123, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(mockSubscribe).toHaveBeenCalledWith(
        '/topic/experiments/123/progress',
        expect.any(Function)
      );
    });

    it('should call onConnectionStatusChange callback when status changes', () => {
      const onConnectionStatusChange = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: false,
          callbacks: { onConnectionStatusChange },
        })
      );

      act(() => {
        result.current.connect();
      });

      expect(onConnectionStatusChange).toHaveBeenCalledWith('connecting');

      act(() => {
        triggerConnect();
      });

      expect(onConnectionStatusChange).toHaveBeenCalledWith('connected');
    });
  });

  describe('disconnect', () => {
    it('should set connectionStatus to disconnected', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');

      act(() => {
        result.current.disconnect();
      });

      expect(result.current.state.connectionStatus).toBe('disconnected');
    });

    it('should unsubscribe from the topic', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(mockSubscribe).toHaveBeenCalled();

      act(() => {
        result.current.disconnect();
      });

      expect(mockUnsubscribe).toHaveBeenCalled();
    });

    it('should call onConnectionStatusChange callback with disconnected', () => {
      const onConnectionStatusChange = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onConnectionStatusChange },
        })
      );

      act(() => {
        triggerConnect();
      });

      expect(onConnectionStatusChange).toHaveBeenCalledWith('connected');

      act(() => {
        result.current.disconnect();
      });

      expect(onConnectionStatusChange).toHaveBeenCalledWith('disconnected');
    });
  });

  describe('resetState', () => {
    it('should reset all state to initial values', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');

      act(() => {
        result.current.resetState();
      });

      const expectedState: ExperimentWebSocketState = {
        connectionStatus: 'disconnected',
        progress: null,
        currentRun: null,
        lastCompletedRun: null,
        experimentCompleted: null,
        experimentPaused: null,
        error: null,
        lastMessage: null,
        reconnectAttempts: 0,
      };

      expect(result.current.state).toEqual(expectedState);
    });
  });

  describe('message handling', () => {
    it('should update progress when PROGRESS message is received', () => {
      const onProgress = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onProgress },
        })
      );

      act(() => {
        triggerConnect();
      });

      const progressPayload: ProgressPayload = {
        totalRuns: 10,
        completedRuns: 5,
        failedRuns: 1,
        percentComplete: 50,
        currentRunId: 6,
        estimatedTimeRemainingMs: 30000,
      };

      act(() => {
        sendMessage({
          type: 'PROGRESS',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: progressPayload,
        });
      });

      expect(result.current.state.progress).toEqual(progressPayload);
      expect(onProgress).toHaveBeenCalledWith(progressPayload);
    });

    it('should update currentRun when RUN_STARTED message is received', () => {
      const onRunStarted = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onRunStarted },
        })
      );

      act(() => {
        triggerConnect();
      });

      const runStartedPayload: RunStartedPayload = {
        runId: 6,
        modelName: 'qwen2.5-coder:7b',
        iteration: 2,
        embeddingModel: 'nomic-embed-text',
      };

      act(() => {
        sendMessage({
          type: 'RUN_STARTED',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: runStartedPayload,
        });
      });

      expect(result.current.state.currentRun).toEqual(runStartedPayload);
      expect(onRunStarted).toHaveBeenCalledWith(runStartedPayload);
    });

    it('should update lastCompletedRun and clear currentRun when RUN_COMPLETED message is received', () => {
      const onRunCompleted = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onRunCompleted },
        })
      );

      act(() => {
        triggerConnect();
      });

      const runCompletedPayload: RunCompletedPayload = {
        runId: 6,
        status: 'SUCCESS',
        durationMs: 2500,
        tokensPerSecond: 45.5,
        errorMessage: null,
      };

      act(() => {
        sendMessage({
          type: 'RUN_COMPLETED',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: runCompletedPayload,
        });
      });

      expect(result.current.state.lastCompletedRun).toEqual(runCompletedPayload);
      expect(result.current.state.currentRun).toBeNull();
      expect(onRunCompleted).toHaveBeenCalledWith(runCompletedPayload);
    });

    it('should update experimentCompleted when EXPERIMENT_COMPLETED message is received', () => {
      const onExperimentCompleted = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onExperimentCompleted },
        })
      );

      act(() => {
        triggerConnect();
      });

      const experimentCompletedPayload: ExperimentCompletedPayload = {
        finalStatus: 'COMPLETED',
        totalRuns: 18,
        successfulRuns: 17,
        failedRuns: 1,
        totalDurationMs: 45000,
      };

      act(() => {
        sendMessage({
          type: 'EXPERIMENT_COMPLETED',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: experimentCompletedPayload,
        });
      });

      expect(result.current.state.experimentCompleted).toEqual(experimentCompletedPayload);
      expect(onExperimentCompleted).toHaveBeenCalledWith(experimentCompletedPayload);
    });

    it('should update experimentPaused when EXPERIMENT_PAUSED message is received', () => {
      const onExperimentPaused = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onExperimentPaused },
        })
      );

      act(() => {
        triggerConnect();
      });

      const experimentPausedPayload: ExperimentPausedPayload = {
        completedRuns: 10,
        remainingRuns: 8,
      };

      act(() => {
        sendMessage({
          type: 'EXPERIMENT_PAUSED',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: experimentPausedPayload,
        });
      });

      expect(result.current.state.experimentPaused).toEqual(experimentPausedPayload);
      expect(onExperimentPaused).toHaveBeenCalledWith(experimentPausedPayload);
    });

    it('should update error when ERROR message is received', () => {
      const onError = vi.fn();
      const { result } = renderHook(() =>
        useExperimentWebSocket(1, {
          autoConnect: true,
          callbacks: { onError },
        })
      );

      act(() => {
        triggerConnect();
      });

      const errorPayload: ErrorPayload = {
        errorCode: 'OLLAMA_UNAVAILABLE',
        message: 'Failed to connect to Ollama service',
        recoverable: false,
      };

      act(() => {
        sendMessage({
          type: 'ERROR',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: errorPayload,
        });
      });

      expect(result.current.state.error).toEqual(errorPayload);
      expect(onError).toHaveBeenCalledWith(errorPayload);
    });

    it('should handle invalid JSON gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
        /* no-op */
      });
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      act(() => {
        capturedMessageHandler?.({ body: 'invalid json' });
      });

      expect(result.current.state.progress).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should handle message with missing required fields', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
        /* no-op */
      });
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      act(() => {
        sendMessage({ type: 'PROGRESS' }); // Missing experimentId and timestamp
      });

      expect(result.current.state.progress).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should update lastMessage for any received message', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      const message = {
        type: 'PROGRESS',
        experimentId: 1,
        timestamp: '2025-11-27T10:00:00Z',
        payload: {
          totalRuns: 10,
          completedRuns: 5,
          failedRuns: 0,
          percentComplete: 50,
          currentRunId: 6,
          estimatedTimeRemainingMs: 30000,
        },
      };

      act(() => {
        sendMessage(message);
      });

      expect(result.current.state.lastMessage).toEqual(message);
    });

    it('should handle unknown message types gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {
        /* no-op */
      });
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      act(() => {
        sendMessage({
          type: 'UNKNOWN_TYPE',
          experimentId: 1,
          timestamp: '2025-11-27T10:00:00Z',
          payload: {},
        });
      });

      expect(consoleSpy).toHaveBeenCalledWith('Unknown WebSocket message type:', 'UNKNOWN_TYPE');
      expect(result.current.state.progress).toBeNull();

      consoleSpy.mockRestore();
    });
  });

  describe('reconnection', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('should set connectionStatus to reconnecting when WebSocket closes', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');

      act(() => {
        triggerWebSocketClose();
      });

      expect(result.current.state.connectionStatus).toBe('reconnecting');
    });

    it('should increment reconnectAttempts on each reconnection attempt', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');

      act(() => {
        triggerWebSocketClose();
      });

      expect(result.current.state.reconnectAttempts).toBe(1);
    });

    it('should reset reconnectAttempts when successfully reconnected', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      // Simulate disconnect
      act(() => {
        triggerWebSocketClose();
      });

      expect(result.current.state.reconnectAttempts).toBe(1);

      // Advance timer and simulate successful reconnect
      act(() => {
        vi.advanceTimersByTime(1000);
      });

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.reconnectAttempts).toBe(0);
      expect(result.current.state.connectionStatus).toBe('connected');
    });

    it('should handle STOMP errors', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
        /* no-op */
      });
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      act(() => {
        triggerStompError({ message: 'test error' }, 'error body');
      });

      expect(result.current.state.connectionStatus).toBe('reconnecting');
      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should handle WebSocket errors', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
        /* no-op */
      });
      renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      act(() => {
        triggerWebSocketError(new Event('error'));
      });

      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });
  });

  describe('cleanup', () => {
    it('should unsubscribe and deactivate on unmount', () => {
      const { result, unmount } = renderHook(() =>
        useExperimentWebSocket(1, { autoConnect: true })
      );

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');

      unmount();

      expect(mockUnsubscribe).toHaveBeenCalled();
    });
  });

  describe('experimentId changes', () => {
    it('should resubscribe when experimentId changes', () => {
      const { rerender } = renderHook(
        ({ id }) => useExperimentWebSocket(id, { autoConnect: true }),
        {
          initialProps: { id: 1 },
        }
      );

      act(() => {
        triggerConnect();
      });

      expect(mockSubscribe).toHaveBeenCalledWith(
        '/topic/experiments/1/progress',
        expect.any(Function)
      );

      mockSubscribe.mockClear();

      rerender({ id: 2 });

      expect(mockSubscribe).toHaveBeenCalledWith(
        '/topic/experiments/2/progress',
        expect.any(Function)
      );
    });
  });

  describe('onDisconnect callback', () => {
    it('should set connectionStatus to disconnected when onDisconnect is called', () => {
      const { result } = renderHook(() => useExperimentWebSocket(1, { autoConnect: true }));

      act(() => {
        triggerConnect();
      });

      expect(result.current.state.connectionStatus).toBe('connected');

      act(() => {
        triggerDisconnect();
      });

      expect(result.current.state.connectionStatus).toBe('disconnected');
    });
  });
});

describe('calculateReconnectDelay', () => {
  it('should return base delay for attempt 0', () => {
    expect(calculateReconnectDelay(0)).toBe(1000);
  });

  it('should double delay for each attempt', () => {
    expect(calculateReconnectDelay(1)).toBe(2000);
    expect(calculateReconnectDelay(2)).toBe(4000);
    expect(calculateReconnectDelay(3)).toBe(8000);
  });

  it('should cap delay at maximum', () => {
    expect(calculateReconnectDelay(10)).toBe(30000);
    expect(calculateReconnectDelay(20)).toBe(30000);
  });

  it('should return max delay for attempt 5 (32000 > 30000)', () => {
    expect(calculateReconnectDelay(5)).toBe(30000);
  });
});
