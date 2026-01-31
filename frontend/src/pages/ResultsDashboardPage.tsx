/**
 * Results Dashboard Page
 *
 * View and analyse experiment results.
 */
import { useCallback, useEffect, useState } from 'react';
import { Leaderboard, leaderboardDefaults } from '@/components/results/Leaderboard';
import { useLeaderboard } from '@/hooks/useLeaderboard';
import type { LeaderboardFilter } from '@/types';

const buildCsv = (rows: string[][]): string => {
  return rows
    .map((row) => row.map((value) => `"${value.replace(/"/g, '""')}"`).join(','))
    .join('\n');
};

const downloadFile = (filename: string, content: string, type: string): void => {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

export function ResultsDashboardPage(): React.JSX.Element {
  const [filters, setFilters] = useState<LeaderboardFilter>({ ...leaderboardDefaults });
  const { leaderboard, isLoading, error, fetchLeaderboard, refresh, clearError } = useLeaderboard();

  useEffect(() => {
    fetchLeaderboard(filters).catch(() => {
      // Error handled in state
    });
  }, [fetchLeaderboard, filters]);

  const handleFiltersChange = useCallback((nextFilters: LeaderboardFilter): void => {
    setFilters(nextFilters);
  }, []);

  const handleExportCsv = useCallback(() => {
    if (!leaderboard) {
      return;
    }
    const rows = [
      ['Model', 'Success rate (%)', 'Avg TPS', 'Avg latency (ms)', 'Runs'],
      ...leaderboard.entries.map((entry) => [
        entry.modelName,
        entry.successRate !== null && entry.successRate !== undefined
          ? (entry.successRate * 100).toFixed(1)
          : '',
        entry.avgTokensPerSecond?.toString() ?? '',
        entry.avgDurationMs?.toString() ?? '',
        entry.runCount.toString(),
      ]),
    ];
    downloadFile('leaderboard.csv', buildCsv(rows), 'text/csv;charset=utf-8;');
  }, [leaderboard]);

  const handleExportJson = useCallback(() => {
    if (!leaderboard) {
      return;
    }
    downloadFile('leaderboard.json', JSON.stringify(leaderboard, null, 2), 'application/json');
  }, [leaderboard]);

  return (
    <div className="space-y-6" data-testid="results-dashboard-page">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Results Dashboard</h1>
        <p className="text-sm text-gray-600">View and analyse experiment results</p>
      </div>

      {error ? (
        <div className="bg-white rounded-lg shadow p-6" data-testid="results-dashboard-error">
          <div className="flex flex-col gap-3">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Leaderboard unavailable</h2>
              <p className="text-sm text-gray-600">{error.message}</p>
            </div>
            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={clearError}
                className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2"
              >
                Dismiss
              </button>
              <button
                type="button"
                onClick={refresh}
                className="bg-brand-green hover:bg-brand-green-dark text-white font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2"
              >
                Retry
              </button>
            </div>
          </div>
        </div>
      ) : !leaderboard ? (
        <div className="bg-white rounded-lg shadow p-6" data-testid="results-dashboard-loading">
          <p className="text-sm text-gray-600">Loading leaderboard analytics...</p>
        </div>
      ) : (
        <Leaderboard
          data={leaderboard}
          filters={filters}
          onFiltersChange={handleFiltersChange}
          onRefresh={refresh}
          onExportCsv={handleExportCsv}
          onExportJson={handleExportJson}
          isExportDisabled={leaderboard.entries.length === 0 || isLoading}
          isLoading={isLoading}
        />
      )}
    </div>
  );
}
