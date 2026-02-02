import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ArenaPage } from './ArenaPage';
import { useGenerate, useOllama } from '@/hooks/useOllama';
import { useConfigStore, DEFAULT_CONFIG_STATE, DEFAULT_HYPERPARAMETERS } from '@/store/configStore';
import type { GenerationResponse } from '@/types';

vi.mock('@/hooks/useOllama', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/hooks/useOllama')>();
  return {
    ...actual,
    useGenerate: vi.fn(),
    useOllama: vi.fn(),
  };
});

const mockUseGenerate = vi.mocked(useGenerate);
const mockUseOllama = vi.mocked(useOllama);

const mockLeftResponse: GenerationResponse = {
  response: 'Left response.',
  model: 'llama3:8b',
  durationMs: 1000,
  tokensPerSecond: 40.5,
  timeToFirstTokenMs: 120,
  promptTokens: 15,
  completionTokens: 30,
};

const mockRightResponse: GenerationResponse = {
  response: 'Right response.',
  model: 'qwen2.5-coder:7b',
  durationMs: 950,
  tokensPerSecond: 42.1,
  timeToFirstTokenMs: 110,
  promptTokens: 15,
  completionTokens: 28,
};

describe('ArenaPage', () => {
  const mockGenerateLeft = vi.fn();
  const mockGenerateRight = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
    });

    mockUseOllama.mockReturnValue({
      models: ['llama3:8b', 'qwen2.5-coder:7b'],
      isLoading: false,
      error: null,
      isAvailable: true,
      refetch: vi.fn(),
    });

    let callIndex = 0;
    mockUseGenerate.mockImplementation(() => {
      const state =
        callIndex % 2 === 0
          ? {
              response: null,
              isLoading: false,
              error: null,
              generate: mockGenerateLeft,
              reset: vi.fn(),
            }
          : {
              response: null,
              isLoading: false,
              error: null,
              generate: mockGenerateRight,
              reset: vi.fn(),
            };
      callIndex += 1;
      return state;
    });
  });

  afterEach(() => {
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
    });
  });

  it('renders the page layout with panels and prompt input', () => {
    render(<ArenaPage />);

    expect(screen.getByTestId('arena-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Arena' })).toBeInTheDocument();
    expect(screen.getByTestId('arena-left-panel')).toBeInTheDocument();
    expect(screen.getByTestId('arena-right-panel')).toBeInTheDocument();
    expect(screen.getByTestId('arena-prompt-input')).toBeInTheDocument();
    expect(screen.getByTestId('arena-send-button')).toBeDisabled();
  });

  it('sends the shared prompt to both models', async () => {
    render(<ArenaPage />);

    fireEvent.change(screen.getByTestId('arena-left-model-select'), {
      target: { value: 'llama3:8b' },
    });
    fireEvent.change(screen.getByTestId('arena-right-model-select'), {
      target: { value: 'qwen2.5-coder:7b' },
    });
    fireEvent.change(screen.getByTestId('arena-prompt-input'), {
      target: { value: 'Compare this output.' },
    });
    fireEvent.click(screen.getByTestId('arena-send-button'));

    await waitFor(() => {
      expect(mockGenerateLeft).toHaveBeenCalledWith(
        expect.objectContaining({
          model: 'llama3:8b',
          prompt: 'Compare this output.',
          temperature: DEFAULT_HYPERPARAMETERS.temperature,
        })
      );
      expect(mockGenerateRight).toHaveBeenCalledWith(
        expect.objectContaining({
          model: 'qwen2.5-coder:7b',
          prompt: 'Compare this output.',
          topP: DEFAULT_HYPERPARAMETERS.topP,
        })
      );
    });
  });

  it('shows a warning when models are missing', async () => {
    render(<ArenaPage />);

    fireEvent.change(screen.getByTestId('arena-prompt-input'), {
      target: { value: 'Needs models.' },
    });
    fireEvent.click(screen.getByTestId('arena-send-button'));

    expect(await screen.findByTestId('arena-form-error')).toHaveTextContent(
      'Select a model for each side before sending a prompt.'
    );
    expect(mockGenerateLeft).not.toHaveBeenCalled();
    expect(mockGenerateRight).not.toHaveBeenCalled();
  });

  it('renders responses and metrics when available', () => {
    let callIndex = 0;
    mockUseGenerate.mockImplementation(() => {
      const state =
        callIndex % 2 === 0
          ? {
              response: mockLeftResponse,
              isLoading: false,
              error: null,
              generate: mockGenerateLeft,
              reset: vi.fn(),
            }
          : {
              response: mockRightResponse,
              isLoading: false,
              error: null,
              generate: mockGenerateRight,
              reset: vi.fn(),
            };
      callIndex += 1;
      return state;
    });

    render(<ArenaPage />);

    expect(screen.getByTestId('arena-left-response-content')).toHaveTextContent(
      mockLeftResponse.response
    );
    expect(screen.getByTestId('arena-right-response-content')).toHaveTextContent(
      mockRightResponse.response
    );
    expect(screen.getByTestId('arena-left-metrics')).toBeInTheDocument();
    expect(screen.getByTestId('arena-right-metrics')).toBeInTheDocument();
  });

  it('shows the Ollama unavailable banner when service is down', () => {
    mockUseOllama.mockReturnValue({
      models: [],
      isLoading: false,
      error: new Error('Network Error'),
      isAvailable: false,
      refetch: vi.fn(),
    });

    render(<ArenaPage />);

    expect(screen.getByTestId('arena-ollama-error')).toHaveTextContent('Ollama unavailable.');
    expect(screen.getByText('Network Error')).toBeInTheDocument();
  });

  it('renders loading and error states for responses', () => {
    let callIndex = 0;
    mockUseGenerate.mockImplementation(() => {
      const state =
        callIndex % 2 === 0
          ? {
              response: null,
              isLoading: true,
              error: null,
              generate: mockGenerateLeft,
              reset: vi.fn(),
            }
          : {
              response: null,
              isLoading: false,
              error: new Error('Generation failed'),
              generate: mockGenerateRight,
              reset: vi.fn(),
            };
      callIndex += 1;
      return state;
    });

    render(<ArenaPage />);

    expect(screen.getByTestId('arena-left-response-loading')).toHaveTextContent(
      'Generating response...'
    );
    expect(screen.getByTestId('arena-right-response-error')).toHaveTextContent('Generation failed');
  });
});
