/**
 * Leaderboard Component
 *
 * Displays leaderboard analytics with filters, charts, and sortable metrics.
 */
import { useCallback, useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { LeaderboardEntry, LeaderboardFilter, LeaderboardResponse } from '@/types';

/**
 * Sorting keys supported by the leaderboard table.
 */
type SortKey = 'modelName' | 'successRate' | 'avgTokensPerSecond' | 'avgDurationMs' | 'runCount';

/**
 * Props for the Leaderboard component.
 */
export interface LeaderboardProps {
  /** Leaderboard data to display. */
  data: LeaderboardResponse;
  /** Current filter state. */
  filters: LeaderboardFilter;
  /** Callback when filters are updated. */
  onFiltersChange: (filters: LeaderboardFilter) => void;
  /** Refresh action for leaderboard data. */
  onRefresh: () => Promise<void> | void;
  /** Callback to export CSV data. */
  onExportCsv: () => void;
  /** Callback to export JSON data. */
  onExportJson: () => void;
  /** Whether export actions are disabled. */
  isExportDisabled?: boolean;
  /** Loading state for refresh actions. */
  isLoading?: boolean;
}

const formatPercent = (value: number | null | undefined): string => {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${(value * 100).toFixed(1)}%`;
};

const formatNumber = (value: number | null | undefined, decimals = 1): string => {
  if (value === null || value === undefined) {
    return '—';
  }
  return value.toFixed(decimals);
};

const formatMilliseconds = (value: number | null | undefined): string => {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${Math.round(value)} ms`;
};

const getSuccessRateValue = (entry: LeaderboardEntry): number | null => {
  if (entry.successRate === null || entry.successRate === undefined) {
    return null;
  }
  return entry.successRate * 100;
};

const buildChartData = (entries: LeaderboardEntry[]): Record<string, string | number>[] => {
  return entries.map((entry) => ({
    model: entry.modelName,
    avgTokensPerSecond: entry.avgTokensPerSecond ?? 0,
    avgDurationMs: entry.avgDurationMs ?? 0,
    successRate: getSuccessRateValue(entry) ?? 0,
  }));
};

/**
 * Leaderboard component.
 */
export function Leaderboard({
  data,
  filters,
  onFiltersChange,
  onRefresh,
  onExportCsv,
  onExportJson,
  isExportDisabled = false,
  isLoading = false,
}: LeaderboardProps): React.JSX.Element {
  const [sortKey, setSortKey] = useState<SortKey>('successRate');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');

  const chartData = useMemo(() => buildChartData(data.entries), [data.entries]);

  const handleSort = useCallback(
    (key: SortKey) => {
      setSortDirection((prev) => (key === sortKey ? (prev === 'asc' ? 'desc' : 'asc') : 'desc'));
      setSortKey(key);
    },
    [sortKey]
  );

  const sortedEntries = useMemo(() => {
    const entries = [...data.entries];
    const normalise = (value: number | null | undefined): number => {
      if (value === null || value === undefined) {
        return sortDirection === 'asc' ? Number.POSITIVE_INFINITY : Number.NEGATIVE_INFINITY;
      }
      return value;
    };

    entries.sort((a, b) => {
      if (sortKey === 'modelName') {
        return sortDirection === 'asc'
          ? a.modelName.localeCompare(b.modelName)
          : b.modelName.localeCompare(a.modelName);
      }

      const valueA = normalise(
        sortKey === 'successRate'
          ? getSuccessRateValue(a)
          : sortKey === 'avgTokensPerSecond'
            ? a.avgTokensPerSecond
            : sortKey === 'avgDurationMs'
              ? a.avgDurationMs
              : a.runCount
      );
      const valueB = normalise(
        sortKey === 'successRate'
          ? getSuccessRateValue(b)
          : sortKey === 'avgTokensPerSecond'
            ? b.avgTokensPerSecond
            : sortKey === 'avgDurationMs'
              ? b.avgDurationMs
              : b.runCount
      );

      return sortDirection === 'asc' ? valueA - valueB : valueB - valueA;
    });

    return entries;
  }, [data.entries, sortDirection, sortKey]);

  const handleFiltersUpdate = useCallback(
    (nextFilters: Partial<LeaderboardFilter>) => {
      onFiltersChange({ ...filters, ...nextFilters });
    },
    [filters, onFiltersChange]
  );

  const handleExperimentChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = event.target.value.trim();
    if (!value) {
      handleFiltersUpdate({ experimentId: undefined });
      return;
    }
    const parsed = Number.parseInt(value, 10);
    if (!Number.isNaN(parsed)) {
      handleFiltersUpdate({ experimentId: parsed });
    }
  };

  const handleModelNameChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = event.target.value;
    handleFiltersUpdate({ modelName: value.trim() ? value : undefined });
  };

  const handleEmbeddingModelChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = event.target.value;
    handleFiltersUpdate({ embeddingModel: value.trim() ? value : undefined });
  };

  const handleSuccessRateChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = event.target.value;
    if (!value) {
      handleFiltersUpdate({ minSuccessRate: undefined });
      return;
    }
    const parsed = Number.parseFloat(value);
    if (!Number.isNaN(parsed)) {
      const clamped = Math.max(0, Math.min(100, parsed));
      handleFiltersUpdate({ minSuccessRate: clamped / 100 });
    }
  };

  return (
    <div className="space-y-6" data-testid="leaderboard">
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Leaderboard</h2>
            <p className="text-xs text-gray-500">
              Total runs analysed: {data.totalRuns}. Updated{' '}
              {new Date(data.generatedAt).toLocaleString()}.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => {
                void onRefresh();
              }}
              className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2"
              disabled={isLoading}
            >
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </button>
            <button
              type="button"
              onClick={onExportCsv}
              className="bg-brand-green hover:bg-brand-green-dark text-white font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2 disabled:opacity-60 disabled:cursor-not-allowed"
              disabled={isExportDisabled}
            >
              Export CSV
            </button>
            <button
              type="button"
              onClick={onExportJson}
              className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2 disabled:opacity-60 disabled:cursor-not-allowed"
              disabled={isExportDisabled}
            >
              Export JSON
            </button>
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="space-y-1">
            <label
              className="text-sm font-medium text-gray-700"
              htmlFor="leaderboard-experiment-filter"
            >
              Experiment ID
            </label>
            <input
              id="leaderboard-experiment-filter"
              type="number"
              min={1}
              value={filters.experimentId ?? ''}
              onChange={handleExperimentChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
              placeholder="All experiments"
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-gray-700" htmlFor="leaderboard-model-filter">
              Model name
            </label>
            <input
              id="leaderboard-model-filter"
              type="text"
              value={filters.modelName ?? ''}
              onChange={handleModelNameChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
              placeholder="All models"
            />
          </div>
          <div className="space-y-1">
            <label
              className="text-sm font-medium text-gray-700"
              htmlFor="leaderboard-embedding-filter"
            >
              Embedding model
            </label>
            <input
              id="leaderboard-embedding-filter"
              type="text"
              value={filters.embeddingModel ?? ''}
              onChange={handleEmbeddingModelChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
              placeholder="All embeddings"
            />
          </div>
          <div className="space-y-1">
            <label
              className="text-sm font-medium text-gray-700"
              htmlFor="leaderboard-success-filter"
            >
              Minimum success rate (%)
            </label>
            <input
              id="leaderboard-success-filter"
              type="number"
              min={0}
              max={100}
              value={
                filters.minSuccessRate !== undefined
                  ? (filters.minSuccessRate * 100).toString()
                  : ''
              }
              onChange={handleSuccessRateChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
              placeholder="0 - 100"
            />
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="bg-white rounded-lg shadow p-4">
          <h3 className="text-sm font-medium text-gray-700 mb-3">Average TPS</h3>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="model" tick={{ fontSize: 10 }} interval={0} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip />
                <Bar dataKey="avgTokensPerSecond" fill="#2D5A3D" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <h3 className="text-sm font-medium text-gray-700 mb-3">Average Latency (ms)</h3>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="model" tick={{ fontSize: 10 }} interval={0} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip />
                <Bar dataKey="avgDurationMs" fill="#E07A3D" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <h3 className="text-sm font-medium text-gray-700 mb-3">Success Rate (%)</h3>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="model" tick={{ fontSize: 10 }} interval={0} />
                <YAxis tick={{ fontSize: 10 }} domain={[0, 100]} />
                <Tooltip />
                <Bar dataKey="successRate" fill="#1E3D2A" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200" data-testid="leaderboard-table">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <button
                    type="button"
                    className="flex items-center gap-2"
                    onClick={() => handleSort('modelName')}
                  >
                    Model
                  </button>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <button
                    type="button"
                    className="flex items-center gap-2"
                    onClick={() => handleSort('successRate')}
                  >
                    Success rate
                  </button>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <button
                    type="button"
                    className="flex items-center gap-2"
                    onClick={() => handleSort('avgTokensPerSecond')}
                  >
                    Avg TPS
                  </button>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <button
                    type="button"
                    className="flex items-center gap-2"
                    onClick={() => handleSort('avgDurationMs')}
                  >
                    Avg latency
                  </button>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <button
                    type="button"
                    className="flex items-center gap-2"
                    onClick={() => handleSort('runCount')}
                  >
                    Runs
                  </button>
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {sortedEntries.length === 0 ? (
                <tr>
                  <td className="px-6 py-6 text-sm text-gray-500" colSpan={5}>
                    No leaderboard entries match the current filters.
                  </td>
                </tr>
              ) : (
                sortedEntries.map((entry) => (
                  <tr key={entry.modelName} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {entry.modelName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatPercent(entry.successRate)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatNumber(entry.avgTokensPerSecond)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatMilliseconds(entry.avgDurationMs)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {entry.runCount}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <div className="mt-4 text-xs text-gray-500">
          Sorted by {sortKey} ({sortDirection}).
        </div>
      </div>
    </div>
  );
}
