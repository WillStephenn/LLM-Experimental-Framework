import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ExperimentBuilderPage } from './ExperimentBuilderPage';

describe('ExperimentBuilderPage', () => {
  it('renders the page with title and description', () => {
    render(<ExperimentBuilderPage />);

    expect(screen.getByTestId('experiment-builder-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Experiment Builder' })).toBeInTheDocument();
    expect(screen.getByText('Create and configure new experiments')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<ExperimentBuilderPage />);
    expect(screen.getByText(/Experiment builder coming soon/)).toBeInTheDocument();
  });
});
