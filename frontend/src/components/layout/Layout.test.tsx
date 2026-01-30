import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import type { RenderResult } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Layout } from './Layout';

/**
 * Test wrapper that provides routing context
 */
function renderWithRouter(
  ui: React.ReactElement,
  { route = '/' } = {}
): RenderResult {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

describe('Layout', () => {
  it('renders the layout with sidebar', () => {
    renderWithRouter(<Layout />);
    expect(screen.getByTestId('layout')).toBeInTheDocument();
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('main-content')).toBeInTheDocument();
  });

  it('renders the LocalLab brand', () => {
    renderWithRouter(<Layout />);
    expect(screen.getByText('LocalLab')).toBeInTheDocument();
    expect(screen.getByText('LLM Experimental Framework')).toBeInTheDocument();
  });

  it('renders all navigation items', () => {
    renderWithRouter(<Layout />);
    const nav = screen.getByRole('navigation');
    
    expect(within(nav).getByText('Sandbox')).toBeInTheDocument();
    expect(within(nav).getByText('Arena')).toBeInTheDocument();
    expect(within(nav).getByText('RAG Lab')).toBeInTheDocument();
    expect(within(nav).getByText('Task Library')).toBeInTheDocument();
    expect(within(nav).getByText('Experiment Builder')).toBeInTheDocument();
    expect(within(nav).getByText('Results Dashboard')).toBeInTheDocument();
    expect(within(nav).getByText('Embedding Analyser')).toBeInTheDocument();
  });

  it('renders the footer text', () => {
    renderWithRouter(<Layout />);
    expect(screen.getByText('Local-first LLM framework')).toBeInTheDocument();
  });
});
