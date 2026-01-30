/**
 * WebSocket Hook for Experiment Progress Updates
 *
 * This hook provides real-time experiment progress tracking via WebSocket/STOMP protocol.
 * It connects to the backend WebSocket endpoint and subscribes to experiment progress updates.
 *
 * @module hooks/useExperimentWebSocket
 */
import { useEffect, useRef, useCallback, useState } from 'react';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import type {
  WebSocketMessageType,
  ProgressPayload,
  RunStartedPayload,
  RunCompletedPayload,
  ExperimentCompletedPayload,
  ExperimentPausedPayload,
  ErrorPayload,
  AnyWebSocketMessage,
} from '@/types';

/**
 * Get the WebSocket URL based on the current environment.
 * Uses VITE_WS_URL environment variable if set, otherwise constructs
 * the URL based on the current page location.
 */
function getWebSocketUrl(): string {
  // Use environment variable if provided
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL as string;
  }

  // In development, default to localhost backend
  if (import.meta.env.DEV) {
    return 'ws://localhost:8080/ws';
  }

  // In production, construct URL from current location
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

/** WebSocket endpoint URL */
const WEBSOCKET_URL = getWebSocketUrl();

/** Maximum number of reconnection attempts */
const MAX_RECONNECT_ATTEMPTS = 10;

/** Base delay in milliseconds for reconnection (doubles with each attempt) */
const RECONNECT_BASE_DELAY_MS = 1000;

/** Maximum delay in milliseconds for reconnection */
const RECONNECT_MAX_DELAY_MS = 30000;

/**
 * Connection status states for the WebSocket connection.
 */
export type ConnectionStatus =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'error';

/**
 * State returned by the useExperimentWebSocket hook.
 */
export interface ExperimentWebSocketState {
  /** Current connection status. */
  connectionStatus: ConnectionStatus;
  /** Current progress information, if available. */
  progress: ProgressPayload | null;
  /** Information about the currently running run. */
  currentRun: RunStartedPayload | null;
  /** The most recent run completion information. */
  lastCompletedRun: RunCompletedPayload | null;
  /** Experiment completion information, if the experiment has completed. */
  experimentCompleted: ExperimentCompletedPayload | null;
  /** Experiment paused information, if the experiment has been paused. */
  experimentPaused: ExperimentPausedPayload | null;
  /** The most recent error, if any. */
  error: ErrorPayload | null;
  /** The most recent message received. */
  lastMessage: AnyWebSocketMessage | null;
  /** The number of reconnection attempts made. */
  reconnectAttempts: number;
}

/**
 * Callbacks for WebSocket events.
 */
export interface ExperimentWebSocketCallbacks {
  /** Called when a progress message is received. */
  onProgress?: (payload: ProgressPayload) => void;
  /** Called when a run starts. */
  onRunStarted?: (payload: RunStartedPayload) => void;
  /** Called when a run completes. */
  onRunCompleted?: (payload: RunCompletedPayload) => void;
  /** Called when the experiment completes. */
  onExperimentCompleted?: (payload: ExperimentCompletedPayload) => void;
  /** Called when the experiment is paused. */
  onExperimentPaused?: (payload: ExperimentPausedPayload) => void;
  /** Called when an error is received. */
  onError?: (payload: ErrorPayload) => void;
  /** Called when the connection status changes. */
  onConnectionStatusChange?: (status: ConnectionStatus) => void;
}

/**
 * Options for configuring the WebSocket connection.
 */
export interface UseExperimentWebSocketOptions {
  /** Whether to automatically connect when the hook mounts. Defaults to true. */
  autoConnect?: boolean;
  /** Callbacks for WebSocket events. */
  callbacks?: ExperimentWebSocketCallbacks;
}

/**
 * Return type for the useExperimentWebSocket hook.
 */
export interface UseExperimentWebSocketReturn {
  /** Current state of the WebSocket connection and experiment data. */
  state: ExperimentWebSocketState;
  /** Manually connect to the WebSocket. */
  connect: () => void;
  /** Manually disconnect from the WebSocket. */
  disconnect: () => void;
  /** Reset all state to initial values. */
  resetState: () => void;
}

/**
 * Initial state for the WebSocket hook.
 */
const INITIAL_STATE: ExperimentWebSocketState = {
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

/**
 * Parse a WebSocket message from the server.
 *
 * @param messageBody - The raw message body from the WebSocket
 * @returns The parsed message or null if parsing fails
 */
function parseMessage(messageBody: string): AnyWebSocketMessage | null {
  try {
    const parsed = JSON.parse(messageBody) as AnyWebSocketMessage;
    // Validate required fields
    if (!parsed.type || typeof parsed.experimentId !== 'number' || !parsed.timestamp) {
      console.error('Invalid WebSocket message structure:', parsed);
      return null;
    }
    return parsed;
  } catch (error) {
    console.error('Failed to parse WebSocket message:', error);
    return null;
  }
}

/**
 * Calculate the reconnection delay using exponential backoff.
 *
 * @param attempt - The current reconnection attempt number (0-indexed)
 * @returns The delay in milliseconds before the next reconnection attempt
 */
export function calculateReconnectDelay(attempt: number): number {
  const delay = RECONNECT_BASE_DELAY_MS * Math.pow(2, attempt);
  return Math.min(delay, RECONNECT_MAX_DELAY_MS);
}

/**
 * Hook for managing WebSocket connections to receive real-time experiment progress updates.
 *
 * @param experimentId - The ID of the experiment to subscribe to. Pass null or 0 to skip connection.
 * @param options - Configuration options for the WebSocket connection
 * @returns Object containing state and control functions
 *
 * @example
 * ```tsx
 * const { state, connect, disconnect, resetState } = useExperimentWebSocket(experimentId, {
 *   autoConnect: true,
 *   callbacks: {
 *     onProgress: (payload) => console.log('Progress:', payload),
 *     onRunCompleted: (payload) => console.log('Run completed:', payload),
 *   },
 * });
 * ```
 */
export function useExperimentWebSocket(
  experimentId: number | null,
  options: UseExperimentWebSocketOptions = {}
): UseExperimentWebSocketReturn {
  const { autoConnect = true, callbacks = {} } = options;

  const [state, setState] = useState<ExperimentWebSocketState>(INITIAL_STATE);

  // Refs for stable references across renders
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const callbacksRef = useRef(callbacks);
  const experimentIdRef = useRef(experimentId);

  // Keep callbacks ref up to date
  useEffect(() => {
    callbacksRef.current = callbacks;
  }, [callbacks]);

  // Keep experimentId ref up to date
  useEffect(() => {
    experimentIdRef.current = experimentId;
  }, [experimentId]);

  /**
   * Update state with partial values.
   */
  const updateState = useCallback((updates: Partial<ExperimentWebSocketState>) => {
    setState((prev) => ({ ...prev, ...updates }));
  }, []);

  /**
   * Handle incoming WebSocket messages.
   */
  const handleMessage = useCallback(
    (message: IMessage) => {
      const parsed = parseMessage(message.body);
      if (!parsed) return;

      // Update last message
      updateState({ lastMessage: parsed });

      // Handle message based on type
      const messageType: WebSocketMessageType = parsed.type;

      switch (messageType) {
        case 'PROGRESS': {
          const payload = parsed.payload as ProgressPayload;
          updateState({ progress: payload });
          callbacksRef.current.onProgress?.(payload);
          break;
        }
        case 'RUN_STARTED': {
          const payload = parsed.payload as RunStartedPayload;
          updateState({ currentRun: payload });
          callbacksRef.current.onRunStarted?.(payload);
          break;
        }
        case 'RUN_COMPLETED': {
          const payload = parsed.payload as RunCompletedPayload;
          updateState({ lastCompletedRun: payload, currentRun: null });
          callbacksRef.current.onRunCompleted?.(payload);
          break;
        }
        case 'EXPERIMENT_COMPLETED': {
          const payload = parsed.payload as ExperimentCompletedPayload;
          updateState({ experimentCompleted: payload, currentRun: null });
          callbacksRef.current.onExperimentCompleted?.(payload);
          break;
        }
        case 'EXPERIMENT_PAUSED': {
          const payload = parsed.payload as ExperimentPausedPayload;
          updateState({ experimentPaused: payload, currentRun: null });
          callbacksRef.current.onExperimentPaused?.(payload);
          break;
        }
        case 'ERROR': {
          const payload = parsed.payload as ErrorPayload;
          updateState({ error: payload });
          callbacksRef.current.onError?.(payload);
          break;
        }
        default: {
          console.warn('Unknown WebSocket message type:', messageType);
        }
      }
    },
    [updateState]
  );

  /**
   * Clear any pending reconnection timeout.
   */
  const clearReconnectTimeout = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
  }, []);

  /**
   * Subscribe to the experiment progress topic.
   */
  const subscribe = useCallback(() => {
    const currentExperimentId = experimentIdRef.current;
    if (!clientRef.current || !currentExperimentId) return;

    // Unsubscribe from previous subscription if exists
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    const topic = `/topic/experiments/${currentExperimentId}/progress`;
    subscriptionRef.current = clientRef.current.subscribe(topic, handleMessage);
  }, [handleMessage]);

  /**
   * Attempt to reconnect to the WebSocket.
   */
  const attemptReconnect = useCallback(() => {
    if (reconnectAttemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
      updateState({
        connectionStatus: 'error',
        reconnectAttempts: reconnectAttemptsRef.current,
      });
      callbacksRef.current.onConnectionStatusChange?.('error');
      return;
    }

    const delay = calculateReconnectDelay(reconnectAttemptsRef.current);
    reconnectAttemptsRef.current += 1;

    updateState({
      connectionStatus: 'reconnecting',
      reconnectAttempts: reconnectAttemptsRef.current,
    });
    callbacksRef.current.onConnectionStatusChange?.('reconnecting');

    reconnectTimeoutRef.current = setTimeout(() => {
      if (clientRef.current) {
        clientRef.current.activate();
      }
    }, delay);
  }, [updateState]);

  /**
   * Connect to the WebSocket server.
   */
  const connect = useCallback(() => {
    // Don't connect if no experiment ID
    if (!experimentIdRef.current) {
      return;
    }

    // Deactivate existing client if any
    if (clientRef.current?.active) {
      void clientRef.current.deactivate();
    }

    clearReconnectTimeout();
    reconnectAttemptsRef.current = 0;

    updateState({
      connectionStatus: 'connecting',
      reconnectAttempts: 0,
    });
    callbacksRef.current.onConnectionStatusChange?.('connecting');

    const client = new Client({
      brokerURL: WEBSOCKET_URL,
      reconnectDelay: 0, // We handle reconnection ourselves

      onConnect: (): void => {
        reconnectAttemptsRef.current = 0;
        updateState({
          connectionStatus: 'connected',
          reconnectAttempts: 0,
          error: null,
        });
        callbacksRef.current.onConnectionStatusChange?.('connected');
        subscribe();
      },

      onDisconnect: (): void => {
        updateState({ connectionStatus: 'disconnected' });
        callbacksRef.current.onConnectionStatusChange?.('disconnected');
      },

      onStompError: (frame): void => {
        console.error('STOMP error:', frame.headers.message, frame.body);
        attemptReconnect();
      },

      onWebSocketClose: (): void => {
        // Only attempt reconnect if we had a valid experiment ID
        if (experimentIdRef.current) {
          attemptReconnect();
        }
      },

      onWebSocketError: (event): void => {
        console.error('WebSocket error:', event);
        // onWebSocketClose will also fire, which will trigger reconnect
      },
    });

    clientRef.current = client;
    client.activate();
  }, [clearReconnectTimeout, subscribe, attemptReconnect, updateState]);

  /**
   * Disconnect from the WebSocket server.
   */
  const disconnect = useCallback(() => {
    clearReconnectTimeout();
    reconnectAttemptsRef.current = 0;

    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (clientRef.current) {
      void clientRef.current.deactivate();
      clientRef.current = null;
    }

    updateState({
      connectionStatus: 'disconnected',
      reconnectAttempts: 0,
    });
    callbacksRef.current.onConnectionStatusChange?.('disconnected');
  }, [clearReconnectTimeout, updateState]);

  /**
   * Reset all state to initial values.
   */
  const resetState = useCallback(() => {
    setState(INITIAL_STATE);
  }, []);

  // Effect for auto-connect and cleanup
  useEffect(() => {
    if (autoConnect && experimentId) {
      connect();
    }

    return (): void => {
      clearReconnectTimeout();
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
      if (clientRef.current) {
        void clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
    // NOTE: We intentionally only depend on experimentId, not on connect/autoConnect.
    // The connect function uses refs internally which always have the latest values.
    // Adding connect to dependencies would cause reconnections on every render
    // since connect's identity changes when its dependencies change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [experimentId]);

  // Effect to update subscription when experimentId changes while connected
  useEffect(() => {
    if (clientRef.current?.active && experimentId) {
      subscribe();
    }
    // NOTE: We intentionally only depend on experimentId, not on subscribe.
    // The subscribe function uses refs internally which always have the latest values.
    // Adding subscribe to dependencies would cause unnecessary resubscriptions.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [experimentId]);

  return {
    state,
    connect,
    disconnect,
    resetState,
  };
}

export default useExperimentWebSocket;
