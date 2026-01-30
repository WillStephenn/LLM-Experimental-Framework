import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ExperimentRunnerPage } from './ExperimentRunnerPage';

describe('ExperimentRunnerPage', () => {
  it('renders the page with title and description', () => {
    render(
      <MemoryRouter initialEntries={['/experiments/123/run']}>
        <Routes>
          <Route path="/experiments/:id/run" element={<ExperimentRunnerPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByTestId('experiment-runner-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Experiment Runner' })).toBeInTheDocument();
  });

  it('displays the experiment ID in the description', () => {
    render(
      <MemoryRouter initialEntries={['/experiments/test-123/run']}>
        <Routes>
          <Route path="/experiments/:id/run" element={<ExperimentRunnerPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Running experiment: test-123/)).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(
      <MemoryRouter initialEntries={['/experiments/123/run']}>
        <Routes>
          <Route path="/experiments/:id/run" element={<ExperimentRunnerPage />} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText(/Experiment runner coming soon/)).toBeInTheDocument();
  });
});
