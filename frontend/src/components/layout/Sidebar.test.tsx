import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { RenderResult } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Sidebar } from './Sidebar';

/**
 * Test wrapper that provides routing context
 */
function renderWithRouter(
  ui: React.ReactElement,
  { route = '/' } = {}
): RenderResult {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

describe('Sidebar', () => {
  it('renders the sidebar with brand header', () => {
    renderWithRouter(<Sidebar />);
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByText('LocalLab')).toBeInTheDocument();
  });

  it('renders all navigation links with correct hrefs', () => {
    renderWithRouter(<Sidebar />);
    
    const sandboxLink = screen.getByTestId('nav-sandbox');
    expect(sandboxLink).toHaveAttribute('href', '/sandbox');

    const arenaLink = screen.getByTestId('nav-arena');
    expect(arenaLink).toHaveAttribute('href', '/arena');

    const ragLink = screen.getByTestId('nav-rag');
    expect(ragLink).toHaveAttribute('href', '/rag');

    const tasksLink = screen.getByTestId('nav-tasks');
    expect(tasksLink).toHaveAttribute('href', '/tasks');

    const experimentsNewLink = screen.getByTestId('nav-experiments-new');
    expect(experimentsNewLink).toHaveAttribute('href', '/experiments/new');

    const resultsLink = screen.getByTestId('nav-results');
    expect(resultsLink).toHaveAttribute('href', '/results');

    const embeddingsLink = screen.getByTestId('nav-embeddings');
    expect(embeddingsLink).toHaveAttribute('href', '/embeddings');
  });

  it('highlights the active route', () => {
    renderWithRouter(<Sidebar />, { route: '/sandbox' });
    
    const sandboxLink = screen.getByTestId('nav-sandbox');
    expect(sandboxLink).toHaveClass('bg-brand-orange');
    
    const arenaLink = screen.getByTestId('nav-arena');
    expect(arenaLink).not.toHaveClass('bg-brand-orange');
  });

  it('renders navigation as an accessible landmark', () => {
    renderWithRouter(<Sidebar />);
    const nav = screen.getByRole('navigation');
    expect(nav).toHaveAttribute('aria-label', 'Main navigation');
  });
});
