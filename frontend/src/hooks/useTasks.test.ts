/**
 * Tests for useTasks Hook
 *
 * @module hooks/useTasks.test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useTasks } from './useTasks';
import { api } from '@/api/client';
import type { TaskTemplateResponse, TaskTemplateRequest } from '@/types';

// Mock the API client
vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockTasks: TaskTemplateResponse[] = [
  {
    id: 1,
    name: 'Code Review Task',
    description: 'Review code for best practices',
    promptTemplate: 'Review the following code:\n\n{{code}}',
    tags: 'code,review',
    evaluationNotes: 'Look for correctness',
    createdAt: '2025-11-27T10:00:00Z',
  },
  {
    id: 2,
    name: 'Documentation Task',
    description: 'Generate documentation',
    promptTemplate: 'Document the following:\n\n{{content}}',
    tags: 'docs',
    evaluationNotes: null,
    createdAt: '2025-11-27T11:00:00Z',
  },
];

const mockTask: TaskTemplateResponse = mockTasks[0];

describe('useTasks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('initial state', () => {
    it('should have empty initial state', () => {
      const { result } = renderHook(() => useTasks());

      expect(result.current.tasks).toEqual([]);
      expect(result.current.task).toBeNull();
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });

  describe('fetchTasks', () => {
    it('should fetch all tasks successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockTasks);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks();
      });

      expect(api.get).toHaveBeenCalledWith('/tasks', { params: undefined });
      expect(result.current.tasks).toEqual(mockTasks);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should fetch tasks with search parameter', async () => {
      vi.mocked(api.get).mockResolvedValueOnce([mockTasks[0]]);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks({ search: 'Code' });
      });

      expect(api.get).toHaveBeenCalledWith('/tasks', { params: { search: 'Code' } });
      expect(result.current.tasks).toEqual([mockTasks[0]]);
    });

    it('should fetch tasks with tag parameter', async () => {
      vi.mocked(api.get).mockResolvedValueOnce([mockTasks[0]]);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks({ tag: 'code' });
      });

      expect(api.get).toHaveBeenCalledWith('/tasks', { params: { tag: 'code' } });
    });

    it('should fetch tasks with both search and tag parameters', async () => {
      vi.mocked(api.get).mockResolvedValueOnce([mockTasks[0]]);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks({ search: 'Code', tag: 'review' });
      });

      expect(api.get).toHaveBeenCalledWith('/tasks', { params: { search: 'Code', tag: 'review' } });
    });

    it('should handle fetch error', async () => {
      const error = new Error('Network error');
      vi.mocked(api.get).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks();
      });

      expect(result.current.tasks).toEqual([]);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection', async () => {
      vi.mocked(api.get).mockRejectedValueOnce('Unknown error');

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks();
      });

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Failed to fetch tasks');
    });

    it('should set loading state during fetch', async () => {
      let resolvePromise: (value: TaskTemplateResponse[]) => void;
      const promise = new Promise<TaskTemplateResponse[]>((resolve) => {
        resolvePromise = resolve;
      });
      vi.mocked(api.get).mockReturnValueOnce(promise);

      const { result } = renderHook(() => useTasks());

      void act(() => {
        void result.current.fetchTasks();
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(true);
      });

      await act(async () => {
        resolvePromise!(mockTasks);
        await promise;
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });
    });
  });

  describe('fetchTask', () => {
    it('should fetch a single task successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockTask);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTask(1);
      });

      expect(api.get).toHaveBeenCalledWith('/tasks/1');
      expect(result.current.task).toEqual(mockTask);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle fetch single task error', async () => {
      const error = new Error('Not found');
      vi.mocked(api.get).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTask(999);
      });

      expect(result.current.task).toBeNull();
      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for single task fetch', async () => {
      vi.mocked(api.get).mockRejectedValueOnce('Not found');

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTask(999);
      });

      expect(result.current.error).toBeInstanceOf(Error);
      expect(result.current.error?.message).toBe('Failed to fetch task');
    });
  });

  describe('createTask', () => {
    const newTaskRequest: TaskTemplateRequest = {
      name: 'New Task',
      promptTemplate: 'New prompt template',
      description: 'A new task',
      tags: 'new',
      evaluationNotes: 'Notes',
    };

    const createdTask: TaskTemplateResponse = {
      id: 3,
      name: 'New Task',
      promptTemplate: 'New prompt template',
      description: 'A new task',
      tags: 'new',
      evaluationNotes: 'Notes',
      createdAt: '2025-11-27T12:00:00Z',
    };

    it('should create a task successfully', async () => {
      vi.mocked(api.post).mockResolvedValueOnce(createdTask);

      const { result } = renderHook(() => useTasks());

      let response: TaskTemplateResponse | undefined;
      await act(async () => {
        response = await result.current.createTask(newTaskRequest);
      });

      expect(api.post).toHaveBeenCalledWith('/tasks', newTaskRequest);
      expect(response).toEqual(createdTask);
      expect(result.current.tasks).toContainEqual(createdTask);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle create error', async () => {
      const error = new Error('Validation failed');
      vi.mocked(api.post).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await expect(result.current.createTask(newTaskRequest)).rejects.toThrow(
          'Validation failed'
        );
      });

      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for create', async () => {
      vi.mocked(api.post).mockRejectedValueOnce('Validation failed');

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await expect(result.current.createTask(newTaskRequest)).rejects.toThrow(
          'Failed to create task'
        );
      });

      expect(result.current.error?.message).toBe('Failed to create task');
    });
  });

  describe('updateTask', () => {
    const updateRequest: TaskTemplateRequest = {
      name: 'Updated Task',
      promptTemplate: 'Updated prompt',
    };

    const updatedTask: TaskTemplateResponse = {
      ...mockTask,
      name: 'Updated Task',
      promptTemplate: 'Updated prompt',
    };

    it('should update a task successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockTasks);
      vi.mocked(api.put).mockResolvedValueOnce(updatedTask);

      const { result } = renderHook(() => useTasks());

      // First fetch tasks to populate the list
      await act(async () => {
        await result.current.fetchTasks();
      });

      let response: TaskTemplateResponse | undefined;
      await act(async () => {
        response = await result.current.updateTask(1, updateRequest);
      });

      expect(api.put).toHaveBeenCalledWith('/tasks/1', updateRequest);
      expect(response).toEqual(updatedTask);
      expect(result.current.tasks.find((t) => t.id === 1)).toEqual(updatedTask);
    });

    it('should update the current task if it matches', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockTask);
      vi.mocked(api.put).mockResolvedValueOnce(updatedTask);

      const { result } = renderHook(() => useTasks());

      // First fetch the single task
      await act(async () => {
        await result.current.fetchTask(1);
      });

      await act(async () => {
        await result.current.updateTask(1, updateRequest);
      });

      expect(result.current.task).toEqual(updatedTask);
    });

    it('should handle update error', async () => {
      const error = new Error('Update failed');
      vi.mocked(api.put).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await expect(result.current.updateTask(1, updateRequest)).rejects.toThrow('Update failed');
      });

      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for update', async () => {
      vi.mocked(api.put).mockRejectedValueOnce('Update failed');

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await expect(result.current.updateTask(1, updateRequest)).rejects.toThrow(
          'Failed to update task'
        );
      });
    });
  });

  describe('deleteTask', () => {
    it('should delete a task successfully', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockTasks);
      vi.mocked(api.delete).mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useTasks());

      // First fetch tasks to populate the list
      await act(async () => {
        await result.current.fetchTasks();
      });

      await act(async () => {
        await result.current.deleteTask(1);
      });

      expect(api.delete).toHaveBeenCalledWith('/tasks/1');
      expect(result.current.tasks.find((t) => t.id === 1)).toBeUndefined();
      expect(result.current.tasks).toHaveLength(1);
    });

    it('should clear the current task if it matches the deleted one', async () => {
      vi.mocked(api.get).mockResolvedValueOnce(mockTask);
      vi.mocked(api.delete).mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useTasks());

      // First fetch the single task
      await act(async () => {
        await result.current.fetchTask(1);
      });

      expect(result.current.task).toEqual(mockTask);

      await act(async () => {
        await result.current.deleteTask(1);
      });

      expect(result.current.task).toBeNull();
    });

    it('should handle delete error', async () => {
      const error = new Error('Delete failed');
      vi.mocked(api.delete).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await expect(result.current.deleteTask(1)).rejects.toThrow('Delete failed');
      });

      expect(result.current.error).toEqual(error);
    });

    it('should handle non-Error rejection for delete', async () => {
      vi.mocked(api.delete).mockRejectedValueOnce('Delete failed');

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await expect(result.current.deleteTask(1)).rejects.toThrow('Failed to delete task');
      });
    });
  });

  describe('clearError', () => {
    it('should clear the error state', async () => {
      const error = new Error('Test error');
      vi.mocked(api.get).mockRejectedValueOnce(error);

      const { result } = renderHook(() => useTasks());

      await act(async () => {
        await result.current.fetchTasks();
      });

      expect(result.current.error).toEqual(error);

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});
