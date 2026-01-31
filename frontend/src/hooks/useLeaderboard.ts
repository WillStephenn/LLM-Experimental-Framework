/**
 * useLeaderboard Hook
 *
 * Provides leaderboard analytics data with loading and error states.
 *
 * @module hooks/useLeaderboard
 */
import { useCallback, useEffect, useState } from 'react';
import { getLeaderboard } from '@/api/analytics';
import type { LeaderboardFilter, LeaderboardResponse } from '@/types';

/**
 * State returned by the useLeaderboard hook.
 */
export interface UseLeaderboardState {
  /** Leaderboard response data. */
  leaderboard: LeaderboardResponse | null;
  /** Loading state for leaderboard fetch. */
  isLoading: boolean;
  /** Error from the last operation. */
  error: Error | null;
}

/**
 * Actions returned by the useLeaderboard hook.
 */
export interface UseLeaderboardActions {
  /** Fetch leaderboard data with optional filters. */
  fetchLeaderboard: (filters?: LeaderboardFilter) => Promise<LeaderboardResponse>;
  /** Refresh the leaderboard using the last filters. */
  refresh: () => Promise<void>;
  /** Clear any errors. */
  clearError: () => void;
}

/**
 * Return type for the useLeaderboard hook.
 */
export type UseLeaderboardReturn = UseLeaderboardState & UseLeaderboardActions;

/**
 * Hook for fetching leaderboard analytics data.
 *
 * @param initialFilters - Optional filters to auto-fetch on mount
 * @returns State and actions for leaderboard data
 */
export const useLeaderboard = (initialFilters?: LeaderboardFilter): UseLeaderboardReturn => {
  const [leaderboard, setLeaderboard] = useState<LeaderboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [lastFilters, setLastFilters] = useState<LeaderboardFilter | undefined>(initialFilters);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const fetchLeaderboard = useCallback(async (filters?: LeaderboardFilter) => {
    setIsLoading(true);
    setError(null);
    setLastFilters(filters);

    try {
      const response = await getLeaderboard(filters);
      setLeaderboard(response);
      return response;
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to fetch leaderboard');
      setError(errorInstance);
      throw errorInstance;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const refresh = useCallback(async (): Promise<void> => {
    await fetchLeaderboard(lastFilters);
  }, [fetchLeaderboard, lastFilters]);

  useEffect(() => {
    if (initialFilters) {
      fetchLeaderboard(initialFilters).catch(() => {
        // Error handled in state
      });
    }
  }, [fetchLeaderboard, initialFilters]);

  return {
    leaderboard,
    isLoading,
    error,
    fetchLeaderboard,
    refresh,
    clearError,
  };
};
