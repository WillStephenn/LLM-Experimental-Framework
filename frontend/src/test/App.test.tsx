import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import App from '../App';

vi.mock(
  '@/hooks/useSystemPrompts',
  (): {
    useSystemPrompts: () => {
      systemPrompts: [];
      systemPrompt: null;
      isLoading: false;
      error: null;
      fetchSystemPrompts: () => void;
      fetchSystemPrompt: () => void;
      createSystemPrompt: () => void;
      updateSystemPrompt: () => void;
      deleteSystemPrompt: () => void;
      clearError: () => void;
    };
  } => ({
    useSystemPrompts: () => ({
      systemPrompts: [],
      systemPrompt: null,
      isLoading: false,
      error: null,
      fetchSystemPrompts: vi.fn(),
      fetchSystemPrompt: vi.fn(),
      createSystemPrompt: vi.fn(),
      updateSystemPrompt: vi.fn(),
      deleteSystemPrompt: vi.fn(),
      clearError: vi.fn(),
    }),
  })
);

vi.mock('@/hooks/useOllama', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/hooks/useOllama')>();
  return {
    ...actual,
    useOllama: vi.fn(() => ({
      models: ['llama3:8b'],
      isLoading: false,
      error: null,
      isAvailable: true,
      refetch: vi.fn(),
    })),
    useGenerate: vi.fn(() => ({
      response: null,
      isLoading: false,
      error: null,
      generate: vi.fn(),
      reset: vi.fn(),
    })),
  };
});

describe('App', () => {
  it('renders without crashing', () => {
    render(<App />);
    // Basic smoke test - ensure the app renders with layout
    expect(screen.getByTestId('layout')).toBeInTheDocument();
  });

  it('renders the sidebar with navigation', () => {
    render(<App />);
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByText('LocalLab')).toBeInTheDocument();
  });

  it('redirects to sandbox page by default', () => {
    render(<App />);
    // The default route should redirect to sandbox
    expect(screen.getByTestId('sandbox-page')).toBeInTheDocument();
  });

  it('renders all navigation links', () => {
    render(<App />);
    const sidebar = screen.getByTestId('sidebar');
    const navigation = within(sidebar).getByRole('navigation');

    // Check all navigation links exist within the sidebar
    expect(within(navigation).getByText('Sandbox')).toBeInTheDocument();
    expect(within(navigation).getByText('Arena')).toBeInTheDocument();
    expect(within(navigation).getByText('RAG Lab')).toBeInTheDocument();
    expect(within(navigation).getByText('Task Library')).toBeInTheDocument();
    expect(within(navigation).getByText('Experiment Builder')).toBeInTheDocument();
    expect(within(navigation).getByText('Results Dashboard')).toBeInTheDocument();
    expect(within(navigation).getByText('Embedding Analyser')).toBeInTheDocument();
  });
});
