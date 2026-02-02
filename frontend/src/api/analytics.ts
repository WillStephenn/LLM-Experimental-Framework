/**
 * Analytics API Client
 *
 * API wrappers for analytics-related endpoints.
 *
 * @module api/analytics
 */
import { api } from './client';
import type { LeaderboardFilter, LeaderboardResponse } from '@/types';

/**
 * Fetch leaderboard analytics data with optional filters.
 *
 * @param filters - Optional leaderboard filters
 * @returns Leaderboard response data
 */
export const getLeaderboard = async (filters?: LeaderboardFilter): Promise<LeaderboardResponse> => {
  const params = new URLSearchParams();

  if (filters?.experimentId !== undefined) {
    params.append('experimentId', filters.experimentId.toString());
  }

  if (filters?.modelName) {
    params.append('modelName', filters.modelName);
  }

  if (filters?.embeddingModel) {
    params.append('embeddingModel', filters.embeddingModel);
  }

  if (filters?.minSuccessRate !== undefined) {
    params.append('minSuccessRate', filters.minSuccessRate.toString());
  }

  const queryString = params.toString();
  const endpoint = queryString ? `/analytics/leaderboard?${queryString}` : '/analytics/leaderboard';

  return api.get<LeaderboardResponse>(endpoint);
};
