import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ArenaPage } from './ArenaPage';

describe('ArenaPage', () => {
  it('renders the page with title and description', () => {
    render(<ArenaPage />);
    
    expect(screen.getByTestId('arena-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Arena' })).toBeInTheDocument();
    expect(screen.getByText('Compare models side-by-side')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<ArenaPage />);
    expect(screen.getByText(/Model comparison arena coming soon/)).toBeInTheDocument();
  });
});
