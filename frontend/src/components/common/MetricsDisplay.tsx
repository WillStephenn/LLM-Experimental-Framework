/**
 * MetricsDisplay Component
 *
 * Displays generation metrics including TPS, duration, TTFT, and token counts.
 * Supports both compact and expanded views.
 * Follows the Academic Modern design system.
 */
import { useState, useMemo, useCallback } from 'react';

/**
 * Generation metrics data structure.
 * Matches the GenerationResponse from types/index.ts for durationMs, tokensPerSecond,
 * timeToFirstTokenMs, promptTokens, and completionTokens.
 */
export interface GenerationMetrics {
  /** Total duration of the generation in milliseconds. */
  durationMs: number | null;
  /** Generation throughput measured in tokens per second. */
  tokensPerSecond: number | null;
  /** Time to first token in milliseconds. */
  timeToFirstTokenMs: number | null;
  /** Number of tokens in the prompt that was sent. */
  promptTokens: number | null;
  /** Number of tokens generated in the response. */
  completionTokens: number | null;
}

/**
 * Display mode for the metrics component.
 */
export type MetricsDisplayMode = 'compact' | 'expanded';

/**
 * Props for the MetricsDisplay component.
 */
export interface MetricsDisplayProps {
  /** The metrics data to display. */
  metrics: GenerationMetrics;
  /** Display mode: 'compact' shows key metrics inline, 'expanded' shows all metrics. */
  mode?: MetricsDisplayMode;
  /** Whether to allow toggling between compact and expanded modes. */
  collapsible?: boolean;
  /** Optional CSS class name for additional styling. */
  className?: string;
  /** Test ID for testing. */
  'data-testid'?: string;
}

/**
 * Formats a duration in milliseconds to a human-readable string.
 *
 * @param ms - Duration in milliseconds
 * @returns Formatted duration string (e.g., "1.5s", "250ms")
 */
function formatDuration(ms: number | null): string {
  if (ms === null || ms === undefined) {
    return '—';
  }
  if (ms >= 1000) {
    return `${(ms / 1000).toFixed(2)}s`;
  }
  return `${ms.toFixed(0)}ms`;
}

/**
 * Formats tokens per second to a human-readable string.
 *
 * @param tps - Tokens per second
 * @returns Formatted TPS string (e.g., "45.5 tok/s")
 */
function formatTps(tps: number | null): string {
  if (tps === null || tps === undefined) {
    return '—';
  }
  return `${tps.toFixed(1)} tok/s`;
}

/**
 * Formats a token count to a human-readable string.
 *
 * @param count - Token count
 * @returns Formatted token count string
 */
function formatTokenCount(count: number | null): string {
  if (count === null || count === undefined) {
    return '—';
  }
  return count.toLocaleString();
}

/**
 * ChevronIcon component for the expand/collapse toggle.
 */
function ChevronIcon({
  className,
  expanded,
}: {
  className?: string;
  expanded: boolean;
}): React.JSX.Element {
  return (
    <svg
      className={`${className ?? 'w-4 h-4'} transition-transform duration-200 ${expanded ? 'rotate-180' : ''}`}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      aria-hidden="true"
    >
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
    </svg>
  );
}

/**
 * MetricItem component for displaying individual metrics.
 */
function MetricItem({
  label,
  value,
  testId,
}: {
  label: string;
  value: string;
  testId?: string;
}): React.JSX.Element {
  return (
    <div className="flex flex-col" data-testid={testId}>
      <span className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</span>
      <span className="text-sm font-semibold text-gray-900">{value}</span>
    </div>
  );
}

/**
 * MetricsDisplay component for displaying generation metrics.
 *
 * @param props - Component props
 * @returns JSX element
 */
export function MetricsDisplay({
  metrics,
  mode: initialMode = 'compact',
  collapsible = true,
  className = '',
  'data-testid': testId = 'metrics-display',
}: MetricsDisplayProps): React.JSX.Element {
  const [isExpanded, setIsExpanded] = useState(initialMode === 'expanded');

  const handleToggle = useCallback(() => {
    if (collapsible) {
      setIsExpanded((prev) => !prev);
    }
  }, [collapsible]);

  // Memoise formatted values
  const formattedMetrics = useMemo(
    () => ({
      duration: formatDuration(metrics.durationMs),
      tps: formatTps(metrics.tokensPerSecond),
      ttft: formatDuration(metrics.timeToFirstTokenMs),
      promptTokens: formatTokenCount(metrics.promptTokens),
      completionTokens: formatTokenCount(metrics.completionTokens),
      totalTokens: formatTokenCount(
        metrics.promptTokens !== null && metrics.completionTokens !== null
          ? metrics.promptTokens + metrics.completionTokens
          : null
      ),
    }),
    [metrics]
  );

  // Determine if any metric has data
  const hasData = useMemo(() => {
    return (
      metrics.durationMs !== null ||
      metrics.tokensPerSecond !== null ||
      metrics.timeToFirstTokenMs !== null ||
      metrics.promptTokens !== null ||
      metrics.completionTokens !== null
    );
  }, [metrics]);

  if (!hasData) {
    return (
      <div
        className={`rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 ${className}`}
        data-testid={testId}
      >
        <span className="text-sm text-gray-500">No metrics available</span>
      </div>
    );
  }

  return (
    <div
      className={`rounded-lg border border-gray-200 bg-gray-50 overflow-hidden ${className}`}
      data-testid={testId}
    >
      {/* Header with toggle */}
      <div
        className={`flex items-center justify-between px-4 py-2 bg-gray-100 border-b border-gray-200 ${collapsible ? 'cursor-pointer hover:bg-gray-150' : ''}`}
        onClick={handleToggle}
        role={collapsible ? 'button' : undefined}
        tabIndex={collapsible ? 0 : undefined}
        onKeyDown={
          collapsible
            ? (e: React.KeyboardEvent<HTMLDivElement>): void => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  handleToggle();
                }
              }
            : undefined
        }
        aria-expanded={collapsible ? isExpanded : undefined}
        aria-label={collapsible ? (isExpanded ? 'Collapse metrics' : 'Expand metrics') : undefined}
        data-testid={`${testId}-header`}
      >
        <span className="text-xs font-medium text-gray-600">Generation Metrics</span>
        {collapsible && (
          <span className="p-1 text-gray-500" aria-hidden="true" data-testid={`${testId}-toggle`}>
            <ChevronIcon expanded={isExpanded} />
          </span>
        )}
      </div>

      {/* Compact view - always visible */}
      <div className="px-4 py-3" data-testid={`${testId}-compact`}>
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
          <MetricItem
            label="Duration"
            value={formattedMetrics.duration}
            testId={`${testId}-duration`}
          />
          <MetricItem label="TPS" value={formattedMetrics.tps} testId={`${testId}-tps`} />
          <MetricItem label="TTFT" value={formattedMetrics.ttft} testId={`${testId}-ttft`} />
        </div>
      </div>

      {/* Expanded view - token details */}
      {isExpanded && (
        <div
          className="px-4 py-3 border-t border-gray-200 bg-white"
          data-testid={`${testId}-expanded`}
        >
          <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
            <MetricItem
              label="Prompt Tokens"
              value={formattedMetrics.promptTokens}
              testId={`${testId}-prompt-tokens`}
            />
            <MetricItem
              label="Completion Tokens"
              value={formattedMetrics.completionTokens}
              testId={`${testId}-completion-tokens`}
            />
            <MetricItem
              label="Total Tokens"
              value={formattedMetrics.totalTokens}
              testId={`${testId}-total-tokens`}
            />
          </div>
        </div>
      )}
    </div>
  );
}
