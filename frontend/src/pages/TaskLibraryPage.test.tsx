import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TaskLibraryPage } from './TaskLibraryPage';
import type { TaskTemplateResponse, SystemPromptResponse } from '@/types';

const taskLibraryMocks = vi.hoisted(
  (): {
    mockTasks: TaskTemplateResponse[];
    mockSystemPrompts: SystemPromptResponse[];
    fetchTasks: ReturnType<typeof vi.fn>;
    fetchSystemPrompts: ReturnType<typeof vi.fn>;
    deleteTask: ReturnType<typeof vi.fn>;
    deleteSystemPrompt: ReturnType<typeof vi.fn>;
  } => {
    const mockTasks: TaskTemplateResponse[] = [
      {
        id: 1,
        name: 'Code Review Task',
        description: 'Review a small code snippet.',
        promptTemplate: 'Review this code.',
        tags: 'code,review',
        evaluationNotes: null,
        createdAt: '2025-01-10T10:00:00.000Z',
      },
      {
        id: 2,
        name: 'Summarisation Task',
        description: 'Summarise the content.',
        promptTemplate: 'Summarise the following.',
        tags: 'summary',
        evaluationNotes: null,
        createdAt: '2025-01-12T10:00:00.000Z',
      },
    ];

    const mockSystemPrompts: SystemPromptResponse[] = [
      {
        id: 11,
        alias: 'assistant',
        content: 'You are a helpful assistant.',
        createdAt: '2025-01-05T10:00:00.000Z',
      },
      {
        id: 12,
        alias: 'reviewer',
        content: 'Review code with care.',
        createdAt: '2025-01-06T10:00:00.000Z',
      },
    ];

    return {
      mockTasks,
      mockSystemPrompts,
      fetchTasks: vi.fn(),
      fetchSystemPrompts: vi.fn(),
      deleteTask: vi.fn(),
      deleteSystemPrompt: vi.fn(),
    };
  }
);

vi.mock('@/hooks/useTasks', (): { useTasks: () => ReturnType<typeof buildTasksHook> } => {
  const buildTasksHook = (): {
    tasks: TaskTemplateResponse[];
    task: TaskTemplateResponse | null;
    isLoading: boolean;
    error: null;
    fetchTasks: typeof taskLibraryMocks.fetchTasks;
    fetchTask: () => void;
    createTask: () => void;
    updateTask: () => void;
    deleteTask: typeof taskLibraryMocks.deleteTask;
    clearError: () => void;
  } => ({
    tasks: taskLibraryMocks.mockTasks,
    task: null,
    isLoading: false,
    error: null,
    fetchTasks: taskLibraryMocks.fetchTasks,
    fetchTask: vi.fn(),
    createTask: vi.fn(),
    updateTask: vi.fn(),
    deleteTask: taskLibraryMocks.deleteTask,
    clearError: vi.fn(),
  });

  return {
    useTasks: buildTasksHook,
  };
});

vi.mock(
  '@/hooks/useSystemPrompts',
  (): { useSystemPrompts: () => ReturnType<typeof buildSystemPromptsHook> } => {
    const buildSystemPromptsHook = (): {
      systemPrompts: SystemPromptResponse[];
      systemPrompt: SystemPromptResponse | null;
      isLoading: boolean;
      error: null;
      fetchSystemPrompts: typeof taskLibraryMocks.fetchSystemPrompts;
      fetchSystemPrompt: () => void;
      createSystemPrompt: () => void;
      updateSystemPrompt: () => void;
      deleteSystemPrompt: typeof taskLibraryMocks.deleteSystemPrompt;
      clearError: () => void;
    } => ({
      systemPrompts: taskLibraryMocks.mockSystemPrompts,
      systemPrompt: null,
      isLoading: false,
      error: null,
      fetchSystemPrompts: taskLibraryMocks.fetchSystemPrompts,
      fetchSystemPrompt: vi.fn(),
      createSystemPrompt: vi.fn(),
      updateSystemPrompt: vi.fn(),
      deleteSystemPrompt: taskLibraryMocks.deleteSystemPrompt,
      clearError: vi.fn(),
    });

    return {
      useSystemPrompts: buildSystemPromptsHook,
    };
  }
);

describe('TaskLibraryPage', () => {
  beforeEach(() => {
    taskLibraryMocks.fetchTasks.mockClear();
    taskLibraryMocks.fetchSystemPrompts.mockClear();
    taskLibraryMocks.deleteTask.mockClear();
    taskLibraryMocks.deleteSystemPrompt.mockClear();
  });

  it('renders the page with title and description', () => {
    render(<TaskLibraryPage />);

    expect(screen.getByTestId('task-library-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Task Library' })).toBeInTheDocument();
    expect(
      screen.getByText('Browse and manage reusable task templates and system prompts.')
    ).toBeInTheDocument();
  });

  it('switches tabs and displays system prompts', async () => {
    render(<TaskLibraryPage />);

    await userEvent.click(screen.getByTestId('task-library-tab-prompts'));

    expect(await screen.findByTestId('prompt-library-table')).toBeInTheDocument();
    expect(screen.getByText('assistant')).toBeInTheDocument();
    expect(screen.getByText('reviewer')).toBeInTheDocument();
  });

  it('filters tasks by tag and search input', () => {
    render(<TaskLibraryPage />);

    fireEvent.change(screen.getByTestId('task-library-tag-filter'), { target: { value: 'code' } });
    expect(screen.getByTestId('task-row-1')).toBeInTheDocument();
    expect(screen.queryByTestId('task-row-2')).not.toBeInTheDocument();

    fireEvent.change(screen.getByTestId('task-library-search'), {
      target: { value: 'Summarisation' },
    });
    expect(screen.queryByTestId('task-row-1')).not.toBeInTheDocument();
    expect(screen.queryByTestId('task-row-2')).not.toBeInTheDocument();
    expect(screen.getByTestId('task-library-empty')).toBeInTheDocument();
  });

  it('opens the delete modal and triggers delete handler', async () => {
    render(<TaskLibraryPage />);

    await userEvent.click(screen.getByTestId('task-delete-1'));
    expect(screen.getByTestId('task-library-modal')).toBeInTheDocument();

    await userEvent.click(screen.getByTestId('task-library-confirm-delete'));
    await waitFor(() => {
      expect(taskLibraryMocks.deleteTask).toHaveBeenCalledWith(1);
    });
  });

  it('shows the edit modal content for system prompts', async () => {
    render(<TaskLibraryPage />);

    await userEvent.click(screen.getByTestId('task-library-tab-prompts'));
    await screen.findByTestId('prompt-library-table');
    await userEvent.click(screen.getByTestId('prompt-edit-11'));

    const modal = screen.getByTestId('task-library-modal');
    expect(within(modal).getByText('Edit system prompt')).toBeInTheDocument();
    expect(within(modal).getByText('assistant')).toBeInTheDocument();
  });
});
