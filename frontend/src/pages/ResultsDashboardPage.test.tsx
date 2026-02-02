import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ResultsDashboardPage } from './ResultsDashboardPage';
import { useLeaderboard } from '@/hooks/useLeaderboard';
import type { LeaderboardResponse } from '@/types';

vi.mock('@/hooks/useLeaderboard', () => ({
  useLeaderboard: vi.fn(),
}));

const mockLeaderboard: LeaderboardResponse = {
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

describe('ResultsDashboardPage', () => {
  it('renders the page with title and description', () => {
    vi.mocked(useLeaderboard).mockReturnValue({
      leaderboard: mockLeaderboard,
      isLoading: false,
      error: null,
      fetchLeaderboard: vi.fn().mockResolvedValue(mockLeaderboard),
      refresh: vi.fn(),
      clearError: vi.fn(),
    });

    render(<ResultsDashboardPage />);

    expect(screen.getByTestId('results-dashboard-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Results Dashboard' })).toBeInTheDocument();
    expect(screen.getByText('View and analyse experiment results')).toBeInTheDocument();
  });

  it('renders leaderboard content', () => {
    vi.mocked(useLeaderboard).mockReturnValue({
      leaderboard: mockLeaderboard,
      isLoading: false,
      error: null,
      fetchLeaderboard: vi.fn().mockResolvedValue(mockLeaderboard),
      refresh: vi.fn(),
      clearError: vi.fn(),
    });

    render(<ResultsDashboardPage />);

    expect(screen.getByTestId('leaderboard')).toBeInTheDocument();
    expect(screen.getByTestId('leaderboard-table')).toBeInTheDocument();
  });

  it('renders loading state', () => {
    vi.mocked(useLeaderboard).mockReturnValue({
      leaderboard: null,
      isLoading: true,
      error: null,
      fetchLeaderboard: vi.fn().mockResolvedValue(mockLeaderboard),
      refresh: vi.fn(),
      clearError: vi.fn(),
    });

    render(<ResultsDashboardPage />);

    expect(screen.getByTestId('results-dashboard-loading')).toBeInTheDocument();
  });

  it('renders error state', () => {
    vi.mocked(useLeaderboard).mockReturnValue({
      leaderboard: null,
      isLoading: false,
      error: new Error('Unable to load leaderboard'),
      fetchLeaderboard: vi.fn().mockResolvedValue(mockLeaderboard),
      refresh: vi.fn(),
      clearError: vi.fn(),
    });

    render(<ResultsDashboardPage />);

    expect(screen.getByTestId('results-dashboard-error')).toBeInTheDocument();
  });
});
