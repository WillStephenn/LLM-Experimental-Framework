import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SandboxPage } from './SandboxPage';
import { useGenerate } from '@/hooks/useOllama';
import { useTasks } from '@/hooks/useTasks';
import { useConfigStore, DEFAULT_CONFIG_STATE, DEFAULT_HYPERPARAMETERS } from '@/store/configStore';
import type { GenerationResponse } from '@/types';

vi.mock('@/hooks/useOllama', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/hooks/useOllama')>();
  return {
    ...actual,
    useGenerate: vi.fn(() => ({
      response: null,
      isLoading: false,
      error: null,
      generate: vi.fn(),
      reset: vi.fn(),
    })),
    useOllama: vi.fn(() => ({
      models: ['llama3:8b'],
      isLoading: false,
      error: null,
      isAvailable: true,
      refetch: vi.fn(),
    })),
  };
});

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

vi.mock('@/hooks/useTasks', () => ({
  useTasks: vi.fn(() => ({
    tasks: [],
    task: null,
    isLoading: false,
    error: null,
    fetchTasks: vi.fn(),
    fetchTask: vi.fn(),
    createTask: vi.fn(),
    updateTask: vi.fn(),
    deleteTask: vi.fn(),
    clearError: vi.fn(),
  })),
}));

const mockUseGenerate = vi.mocked(useGenerate);
const mockUseTasks = vi.mocked(useTasks);

const mockGenerationResponse: GenerationResponse = {
  response: 'Hello from the assistant.',
  model: 'llama3:8b',
  durationMs: 1200,
  tokensPerSecond: 45.5,
  timeToFirstTokenMs: 120,
  promptTokens: 12,
  completionTokens: 24,
};

describe('SandboxPage', () => {
  const mockGenerate = vi.fn();
  const mockCreateTask = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
      model: 'llama3:8b',
    });
    mockGenerate.mockResolvedValue(mockGenerationResponse);
    mockUseGenerate.mockReturnValue({
      response: null,
      isLoading: false,
      error: null,
      generate: mockGenerate,
      reset: vi.fn(),
    });
    mockUseTasks.mockReturnValue({
      tasks: [],
      task: null,
      isLoading: false,
      error: null,
      fetchTasks: vi.fn(),
      fetchTask: vi.fn(),
      createTask: mockCreateTask,
      updateTask: vi.fn(),
      deleteTask: vi.fn(),
      clearError: vi.fn(),
    });
  });

  afterEach(() => {
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
    });
  });

  it('renders the page with title and description', () => {
    render(<SandboxPage />);

    expect(screen.getByTestId('sandbox-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Sandbox' })).toBeInTheDocument();
    expect(
      screen.getByText('Experiment with models in real-time and review response metrics instantly.')
    ).toBeInTheDocument();
  });

  it('renders the empty chat state', () => {
    render(<SandboxPage />);
    expect(screen.getByTestId('sandbox-empty-state')).toBeInTheDocument();
    expect(
      screen.getByText('No messages yet. Start by sending a prompt to the model.')
    ).toBeInTheDocument();
  });

  it('sends a prompt and renders the response with metrics', async () => {
    render(<SandboxPage />);

    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Hello there' },
    });
    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    expect(mockGenerate).toHaveBeenCalledWith(
      expect.objectContaining({
        model: 'llama3:8b',
        prompt: 'Hello there',
      })
    );

    await waitFor(() => {
      expect(screen.getAllByTestId('sandbox-message-user')).toHaveLength(1);
      expect(screen.getAllByTestId('sandbox-message-assistant')).toHaveLength(1);
    });

    expect(screen.getByText('Hello there')).toBeInTheDocument();
    expect(screen.getByText('Hello from the assistant.')).toBeInTheDocument();
    expect(screen.getByTestId('metrics-display')).toBeInTheDocument();
  });

  it('shows an error when sending without a selected model', async () => {
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
      model: null,
    });

    render(<SandboxPage />);

    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Hello there' },
    });
    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    expect(await screen.findByTestId('sandbox-error')).toHaveTextContent(
      'Please select a model before sending a prompt.'
    );
    expect(mockGenerate).not.toHaveBeenCalled();
  });

  it('submits the prompt when pressing Enter', () => {
    render(<SandboxPage />);

    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Enter prompt' },
    });
    fireEvent.keyDown(screen.getByTestId('sandbox-prompt-input'), {
      key: 'Enter',
      code: 'Enter',
      charCode: 13,
    });

    expect(mockGenerate).toHaveBeenCalledWith(
      expect.objectContaining({
        model: 'llama3:8b',
        prompt: 'Enter prompt',
      })
    );
  });

  it('does not send when the prompt is empty', () => {
    render(<SandboxPage />);

    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    expect(mockGenerate).not.toHaveBeenCalled();
  });

  it('shows generation error messages from the hook', async () => {
    mockGenerate.mockRejectedValueOnce(new Error('Generation failed'));

    render(<SandboxPage />);

    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Hello there' },
    });
    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    expect(await screen.findByTestId('sandbox-error')).toHaveTextContent('Generation failed');
  });

  it('includes jsonMode in generation request when enabled', async () => {
    render(<SandboxPage />);

    fireEvent.click(screen.getByTestId('sandbox-json-toggle'));
    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Return JSON' },
    });
    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    expect(mockGenerate).toHaveBeenCalledWith(
      expect.objectContaining({
        model: 'llama3:8b',
        prompt: 'Return JSON',
        jsonMode: true,
      })
    );
  });

  it('saves assistant responses as task templates', async () => {
    mockCreateTask.mockResolvedValue({
      id: 1,
      name: 'Sandbox Response 30/01/2026, 20:00:00',
      description: 'Generated from model llama3:8b.',
      promptTemplate: 'Hello from the assistant.',
      tags: null,
      evaluationNotes: null,
      createdAt: '2026-01-30T20:00:00Z',
    });

    render(<SandboxPage />);

    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Hello there' },
    });
    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    await waitFor(() => {
      expect(screen.getAllByTestId('sandbox-message-assistant')).toHaveLength(1);
    });

    fireEvent.click(screen.getByTestId('sandbox-save-task'));

    await waitFor(() => {
      expect(mockCreateTask).toHaveBeenCalledWith(
        expect.objectContaining({
          promptTemplate: 'Hello from the assistant.',
        })
      );
    });
    expect(screen.getByTestId('sandbox-task-status')).toHaveTextContent(
      'Saved to Task Library.'
    );
  });

  it('clears history when clear button is clicked', async () => {
    render(<SandboxPage />);

    fireEvent.change(screen.getByTestId('sandbox-prompt-input'), {
      target: { value: 'Hello there' },
    });
    fireEvent.click(screen.getByTestId('sandbox-send-button'));

    await waitFor(() => {
      expect(screen.getAllByTestId('sandbox-message-user')).toHaveLength(1);
      expect(screen.getAllByTestId('sandbox-message-assistant')).toHaveLength(1);
    });

    fireEvent.click(screen.getByTestId('sandbox-clear-history'));

    expect(screen.getByTestId('sandbox-empty-state')).toBeInTheDocument();
  });
});
