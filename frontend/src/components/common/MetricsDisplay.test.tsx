import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MetricsDisplay } from './MetricsDisplay';
import type { GenerationMetrics } from './MetricsDisplay';

describe('MetricsDisplay', () => {
  const mockMetrics: GenerationMetrics = {
    durationMs: 1500,
    tokensPerSecond: 45.5,
    timeToFirstTokenMs: 250,
    promptTokens: 100,
    completionTokens: 200,
  };

  describe('rendering', () => {
    it('renders with default test ID', () => {
      render(<MetricsDisplay metrics={mockMetrics} />);
      expect(screen.getByTestId('metrics-display')).toBeInTheDocument();
    });

    it('renders with custom test ID', () => {
      render(<MetricsDisplay metrics={mockMetrics} data-testid="custom-metrics" />);
      expect(screen.getByTestId('custom-metrics')).toBeInTheDocument();
    });

    it('renders the header with title', () => {
      render(<MetricsDisplay metrics={mockMetrics} />);
      expect(screen.getByTestId('metrics-display-header')).toHaveTextContent('Generation Metrics');
    });

    it('applies custom className', () => {
      render(<MetricsDisplay metrics={mockMetrics} className="custom-class" />);
      const container = screen.getByTestId('metrics-display');
      expect(container).toHaveClass('custom-class');
    });
  });

  describe('compact view', () => {
    it('displays duration metric', () => {
      render(<MetricsDisplay metrics={mockMetrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('1.50s');
    });

    it('displays TPS metric', () => {
      render(<MetricsDisplay metrics={mockMetrics} />);
      expect(screen.getByTestId('metrics-display-tps')).toHaveTextContent('45.5 tok/s');
    });

    it('displays TTFT metric', () => {
      render(<MetricsDisplay metrics={mockMetrics} />);
      expect(screen.getByTestId('metrics-display-ttft')).toHaveTextContent('250ms');
    });

    it('formats duration in milliseconds when under 1 second', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        durationMs: 500,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('500ms');
    });

    it('formats duration in seconds when 1 second or more', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        durationMs: 2500,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('2.50s');
    });
  });

  describe('expanded view', () => {
    it('does not show expanded section by default in compact mode', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" />);
      expect(screen.queryByTestId('metrics-display-expanded')).not.toBeInTheDocument();
    });

    it('shows expanded section when mode is expanded', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-expanded')).toBeInTheDocument();
    });

    it('displays prompt tokens in expanded view', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-prompt-tokens')).toHaveTextContent('100');
    });

    it('displays completion tokens in expanded view', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-completion-tokens')).toHaveTextContent('200');
    });

    it('displays total tokens in expanded view', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-total-tokens')).toHaveTextContent('300');
    });
  });

  describe('collapsible behaviour', () => {
    it('toggles from compact to expanded when header is clicked', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" collapsible />);

      expect(screen.queryByTestId('metrics-display-expanded')).not.toBeInTheDocument();

      fireEvent.click(screen.getByTestId('metrics-display-header'));

      expect(screen.getByTestId('metrics-display-expanded')).toBeInTheDocument();
    });

    it('toggles from expanded to compact when header is clicked', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="expanded" collapsible />);

      expect(screen.getByTestId('metrics-display-expanded')).toBeInTheDocument();

      fireEvent.click(screen.getByTestId('metrics-display-header'));

      expect(screen.queryByTestId('metrics-display-expanded')).not.toBeInTheDocument();
    });

    it('does not toggle when collapsible is false', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" collapsible={false} />);

      fireEvent.click(screen.getByTestId('metrics-display-header'));

      expect(screen.queryByTestId('metrics-display-expanded')).not.toBeInTheDocument();
    });

    it('shows toggle button when collapsible', () => {
      render(<MetricsDisplay metrics={mockMetrics} collapsible />);
      expect(screen.getByTestId('metrics-display-toggle')).toBeInTheDocument();
    });

    it('hides toggle button when not collapsible', () => {
      render(<MetricsDisplay metrics={mockMetrics} collapsible={false} />);
      expect(screen.queryByTestId('metrics-display-toggle')).not.toBeInTheDocument();
    });

    it('supports keyboard navigation with Enter key', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" collapsible />);

      expect(screen.queryByTestId('metrics-display-expanded')).not.toBeInTheDocument();

      fireEvent.keyDown(screen.getByTestId('metrics-display-header'), { key: 'Enter' });

      expect(screen.getByTestId('metrics-display-expanded')).toBeInTheDocument();
    });

    it('supports keyboard navigation with Space key', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" collapsible />);

      expect(screen.queryByTestId('metrics-display-expanded')).not.toBeInTheDocument();

      fireEvent.keyDown(screen.getByTestId('metrics-display-header'), { key: ' ' });

      expect(screen.getByTestId('metrics-display-expanded')).toBeInTheDocument();
    });

    it('has correct aria-expanded attribute', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" collapsible />);

      const header = screen.getByTestId('metrics-display-header');
      expect(header).toHaveAttribute('aria-expanded', 'false');

      fireEvent.click(header);

      expect(header).toHaveAttribute('aria-expanded', 'true');
    });

    it('has correct aria-label on header', () => {
      render(<MetricsDisplay metrics={mockMetrics} mode="compact" collapsible />);

      const header = screen.getByTestId('metrics-display-header');
      expect(header).toHaveAttribute('aria-label', 'Expand metrics');

      fireEvent.click(header);

      expect(header).toHaveAttribute('aria-label', 'Collapse metrics');
    });
  });

  describe('null values handling', () => {
    it('displays placeholder for null duration', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        durationMs: null,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('—');
    });

    it('displays placeholder for null TPS', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        tokensPerSecond: null,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-tps')).toHaveTextContent('—');
    });

    it('displays placeholder for null TTFT', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        timeToFirstTokenMs: null,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-ttft')).toHaveTextContent('—');
    });

    it('displays placeholder for null prompt tokens', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        promptTokens: null,
      };
      render(<MetricsDisplay metrics={metrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-prompt-tokens')).toHaveTextContent('—');
    });

    it('displays placeholder for null completion tokens', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        completionTokens: null,
      };
      render(<MetricsDisplay metrics={metrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-completion-tokens')).toHaveTextContent('—');
    });

    it('displays placeholder for total tokens when either is null', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        promptTokens: null,
      };
      render(<MetricsDisplay metrics={metrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-total-tokens')).toHaveTextContent('—');
    });

    it('shows "No metrics available" when all values are null', () => {
      const metrics: GenerationMetrics = {
        durationMs: null,
        tokensPerSecond: null,
        timeToFirstTokenMs: null,
        promptTokens: null,
        completionTokens: null,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByText('No metrics available')).toBeInTheDocument();
    });
  });

  describe('formatting', () => {
    it('formats large token counts with locale separator', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        promptTokens: 10000,
        completionTokens: 25000,
      };
      render(<MetricsDisplay metrics={metrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-prompt-tokens')).toHaveTextContent('10,000');
      expect(screen.getByTestId('metrics-display-completion-tokens')).toHaveTextContent('25,000');
      expect(screen.getByTestId('metrics-display-total-tokens')).toHaveTextContent('35,000');
    });

    it('formats TPS with one decimal place', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        tokensPerSecond: 100.123,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-tps')).toHaveTextContent('100.1 tok/s');
    });

    it('formats duration with two decimal places for seconds', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        durationMs: 1234,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('1.23s');
    });

    it('formats milliseconds without decimal places', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        durationMs: 123,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('123ms');
    });
  });

  describe('edge cases', () => {
    it('handles zero values correctly', () => {
      const metrics: GenerationMetrics = {
        durationMs: 0,
        tokensPerSecond: 0,
        timeToFirstTokenMs: 0,
        promptTokens: 0,
        completionTokens: 0,
      };
      render(<MetricsDisplay metrics={metrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('0ms');
      expect(screen.getByTestId('metrics-display-tps')).toHaveTextContent('0.0 tok/s');
      expect(screen.getByTestId('metrics-display-ttft')).toHaveTextContent('0ms');
      expect(screen.getByTestId('metrics-display-prompt-tokens')).toHaveTextContent('0');
      expect(screen.getByTestId('metrics-display-completion-tokens')).toHaveTextContent('0');
      expect(screen.getByTestId('metrics-display-total-tokens')).toHaveTextContent('0');
    });

    it('handles exactly 1000ms duration', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        durationMs: 1000,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-duration')).toHaveTextContent('1.00s');
    });

    it('handles very large token counts', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        promptTokens: 1000000,
        completionTokens: 2000000,
      };
      render(<MetricsDisplay metrics={metrics} mode="expanded" />);
      expect(screen.getByTestId('metrics-display-prompt-tokens')).toHaveTextContent('1,000,000');
      expect(screen.getByTestId('metrics-display-completion-tokens')).toHaveTextContent(
        '2,000,000'
      );
      expect(screen.getByTestId('metrics-display-total-tokens')).toHaveTextContent('3,000,000');
    });

    it('handles very small TPS values', () => {
      const metrics: GenerationMetrics = {
        ...mockMetrics,
        tokensPerSecond: 0.01,
      };
      render(<MetricsDisplay metrics={metrics} />);
      expect(screen.getByTestId('metrics-display-tps')).toHaveTextContent('0.0 tok/s');
    });
  });
});
