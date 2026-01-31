/**
 * Tests for useLeaderboard hook.
 *
 * @module hooks/useLeaderboard.test
 */
import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { useLeaderboard } from './useLeaderboard';
import { getLeaderboard } from '@/api/analytics';
import type { LeaderboardResponse } from '@/types';

vi.mock('@/api/analytics', () => ({
  getLeaderboard: vi.fn(),
}));

const mockResponse: LeaderboardResponse = {
  entries: [
    {
      modelName: 'llama3:8b',
      runCount: 12,
      successCount: 10,
      successRate: 0.83,
      avgTokensPerSecond: 40.5,
      minTokensPerSecond: 35.1,
      maxTokensPerSecond: 48.2,
      avgDurationMs: 2500,
      minDurationMs: 2000,
      maxDurationMs: 3200,
      avgTimeToFirstTokenMs: 120,
    },
  ],
  totalRuns: 12,
  generatedAt: '2025-11-27T10:00:00Z',
};

describe('useLeaderboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (getLeaderboard as Mock).mockReset();
  });

  it('fetches leaderboard data', async () => {
    (getLeaderboard as Mock).mockResolvedValueOnce(mockResponse);
    const { result } = renderHook(() => useLeaderboard());

    await act(async () => {
      await result.current.fetchLeaderboard();
    });

    expect(getLeaderboard).toHaveBeenCalledWith(undefined);
    expect(result.current.leaderboard).toEqual(mockResponse);
  });

  it('sets error when fetch fails', async () => {
    (getLeaderboard as Mock).mockRejectedValueOnce(new Error('Fetch failed'));
    const { result } = renderHook(() => useLeaderboard());

    await act(async () => {
      try {
        await result.current.fetchLeaderboard();
      } catch {
        // Expected error
      }
    });

    expect(result.current.error).toBeInstanceOf(Error);
  });

  it('auto-fetches when initial filters are provided', async () => {
    (getLeaderboard as Mock).mockResolvedValueOnce(mockResponse);
    const { result } = renderHook(() =>
      useLeaderboard({ experimentId: 1, modelName: 'llama3:8b' })
    );

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(getLeaderboard).toHaveBeenCalledWith({ experimentId: 1, modelName: 'llama3:8b' });
  });
});
