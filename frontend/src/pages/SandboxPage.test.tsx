import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SandboxPage } from './SandboxPage';

describe('SandboxPage', () => {
  it('renders the page with title and description', () => {
    render(<SandboxPage />);

    expect(screen.getByTestId('sandbox-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Sandbox' })).toBeInTheDocument();
    expect(screen.getByText('Experiment with models in real-time')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<SandboxPage />);
    expect(screen.getByText(/Interactive playground coming soon/)).toBeInTheDocument();
  });
});
