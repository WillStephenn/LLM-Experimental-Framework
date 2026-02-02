import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Leaderboard } from './Leaderboard';
import { leaderboardDefaults } from './leaderboardDefaults';
import type { LeaderboardResponse } from '@/types';

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

describe('Leaderboard', () => {
  it('renders filters and table content', () => {
    render(
      <Leaderboard
        data={mockLeaderboard}
        filters={{ ...leaderboardDefaults }}
        onFiltersChange={vi.fn()}
        onRefresh={vi.fn()}
        onExportCsv={vi.fn()}
        onExportJson={vi.fn()}
      />
    );

    expect(screen.getByTestId('leaderboard')).toBeInTheDocument();
    expect(screen.getByTestId('leaderboard-table')).toBeInTheDocument();
    expect(screen.getByLabelText('Experiment ID')).toBeInTheDocument();
    expect(screen.getByLabelText('Model name')).toBeInTheDocument();
    expect(screen.getByText('llama3:8b')).toBeInTheDocument();
  });

  it('notifies when the model filter changes', () => {
    const handleFiltersChange = vi.fn();

    render(
      <Leaderboard
        data={mockLeaderboard}
        filters={{ ...leaderboardDefaults }}
        onFiltersChange={handleFiltersChange}
        onRefresh={vi.fn()}
        onExportCsv={vi.fn()}
        onExportJson={vi.fn()}
      />
    );

    fireEvent.change(screen.getByLabelText('Model name'), {
      target: { value: 'llama' },
    });

    expect(handleFiltersChange).toHaveBeenCalledWith(
      expect.objectContaining({ modelName: 'llama' })
    );
  });
});
