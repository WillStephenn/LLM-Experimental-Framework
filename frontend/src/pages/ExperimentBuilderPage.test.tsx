import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ExperimentBuilderPage } from './ExperimentBuilderPage';
import { useTasks } from '@/hooks/useTasks';

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

const mockUseTasks = vi.mocked(useTasks);

describe('ExperimentBuilderPage', () => {
  const fetchTasks = vi.fn();
  const baseTask = {
    id: 1,
    name: 'Code Review Task',
    description: 'Review code for best practices',
    promptTemplate: 'Review the following code:\n\n{{code}}\n\nProvide feedback.',
    tags: 'code,review',
    evaluationNotes: 'Focus on clarity and correctness.',
    createdAt: '2025-01-01T00:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockUseTasks.mockReturnValue({
      tasks: [baseTask],
      task: null,
      isLoading: false,
      error: null,
      fetchTasks,
      fetchTask: vi.fn(),
      createTask: vi.fn(),
      updateTask: vi.fn(),
      deleteTask: vi.fn(),
      clearError: vi.fn(),
    });
  });

  it('renders the page with title and description', () => {
    render(<ExperimentBuilderPage />);

    expect(screen.getByTestId('experiment-builder-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Experiment Builder' })).toBeInTheDocument();
    expect(screen.getByText('Create and configure new experiments')).toBeInTheDocument();
  });

  it('renders the task selection section', () => {
    render(<ExperimentBuilderPage />);

    expect(screen.getByText('Step 1: Select Task Template')).toBeInTheDocument();
    expect(screen.getByLabelText('Search tasks')).toBeInTheDocument();
    expect(screen.getByTestId('experiment-builder-task-select')).toBeInTheDocument();
  });

  it('loads task templates on mount', () => {
    render(<ExperimentBuilderPage />);

    expect(fetchTasks).toHaveBeenCalledTimes(1);
  });

  it('filters task templates by search query', () => {
    render(<ExperimentBuilderPage />);

    const searchInput = screen.getByTestId('experiment-builder-task-search');
    fireEvent.change(searchInput, { target: { value: 'non-match' } });

    expect(screen.getByTestId('experiment-builder-task-empty')).toBeInTheDocument();
  });

  it('renders variable inputs and preview when a task is selected', () => {
    render(<ExperimentBuilderPage />);

    fireEvent.change(screen.getByTestId('experiment-builder-task-select'), {
      target: { value: '1' },
    });

    expect(screen.getByTestId('experiment-builder-variable-code')).toBeInTheDocument();
    expect(screen.getByTestId('experiment-builder-task-preview-prompt')).toBeInTheDocument();
    expect(screen.getByText('Code Review Task')).toBeInTheDocument();
  });

  it('updates preview when variable values change', () => {
    render(<ExperimentBuilderPage />);

    fireEvent.change(screen.getByTestId('experiment-builder-task-select'), {
      target: { value: '1' },
    });

    fireEvent.change(screen.getByTestId('experiment-builder-variable-code'), {
      target: { value: 'const value = 1;' },
    });

    expect(screen.getByText(/const value = 1;/)).toBeInTheDocument();
  });
});
