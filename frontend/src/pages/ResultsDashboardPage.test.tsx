import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ResultsDashboardPage } from './ResultsDashboardPage';

describe('ResultsDashboardPage', () => {
  it('renders the page with title and description', () => {
    render(<ResultsDashboardPage />);

    expect(screen.getByTestId('results-dashboard-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Results Dashboard' })).toBeInTheDocument();
    expect(screen.getByText('View and analyse experiment results')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<ResultsDashboardPage />);
    expect(screen.getByText(/Results dashboard coming soon/)).toBeInTheDocument();
  });
});
