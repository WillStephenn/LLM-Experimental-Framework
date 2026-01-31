/**
 * Tests for analytics API wrappers.
 *
 * @module api/analytics.test
 */
import { describe, it, expect, vi, type Mock } from 'vitest';
import { getLeaderboard } from './analytics';
import { api } from './client';
import type { LeaderboardResponse } from '@/types';

vi.mock('./client', () => ({
  api: {
    get: vi.fn(),
  },
}));

const mockResponse: LeaderboardResponse = {
  entries: [],
  totalRuns: 0,
  generatedAt: '2025-11-27T10:00:00Z',
};

describe('getLeaderboard', () => {
  it('calls the leaderboard endpoint without filters', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockResponse);

    await getLeaderboard();

    expect(api.get).toHaveBeenCalledWith('/analytics/leaderboard');
  });

  it('builds query parameters for filters', async () => {
    (api.get as Mock).mockResolvedValueOnce(mockResponse);

    await getLeaderboard({
      experimentId: 2,
      modelName: 'llama3:8b',
      embeddingModel: 'nomic-embed-text',
      minSuccessRate: 0.75,
    });

    expect(api.get).toHaveBeenCalledWith(
      '/analytics/leaderboard?experimentId=2&modelName=llama3%3A8b&embeddingModel=nomic-embed-text&minSuccessRate=0.75'
    );
  });
});
