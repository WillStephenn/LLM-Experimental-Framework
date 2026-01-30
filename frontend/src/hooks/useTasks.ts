/**
 * useTasks Hook
 *
 * Provides CRUD operations for TaskTemplate entities with loading and error states.
 *
 * @module hooks/useTasks
 * @see docs/API-Contract.md for the complete API specification
 */
import { useState, useCallback } from 'react';
import { api } from '@/api/client';
import type { TaskTemplateRequest, TaskTemplateResponse } from '@/types';

/**
 * Query parameters for filtering task templates.
 */
export interface TasksQueryParams {
  /** Filter by name (case-insensitive partial match). */
  search?: string;
  /** Filter by tag. */
  tag?: string;
}

/**
 * State and operations returned by the useTasks hook.
 */
export interface UseTasksReturn {
  /** List of task templates. */
  tasks: TaskTemplateResponse[];
  /** Currently selected task template. */
  task: TaskTemplateResponse | null;
  /** Whether a request is in progress. */
  isLoading: boolean;
  /** Error from the most recent operation. */
  error: Error | null;
  /** Fetch all task templates with optional filters. */
  fetchTasks: (params?: TasksQueryParams) => Promise<void>;
  /** Fetch a single task template by ID. */
  fetchTask: (id: number) => Promise<void>;
  /** Create a new task template. */
  createTask: (data: TaskTemplateRequest) => Promise<TaskTemplateResponse>;
  /** Update an existing task template. */
  updateTask: (id: number, data: TaskTemplateRequest) => Promise<TaskTemplateResponse>;
  /** Delete a task template. */
  deleteTask: (id: number) => Promise<void>;
  /** Clear the current error state. */
  clearError: () => void;
}

/**
 * Hook for managing TaskTemplate CRUD operations.
 *
 * Provides methods to fetch, create, update, and delete task templates
 * with built-in loading and error state management.
 *
 * @returns Object containing task data, loading state, error state, and CRUD methods
 *
 * @example
 * ```tsx
 * const { tasks, isLoading, error, fetchTasks, createTask } = useTasks();
 *
 * useEffect(() => {
 *   fetchTasks();
 * }, [fetchTasks]);
 *
 * const handleCreate = async () => {
 *   await createTask({ name: 'New Task', promptTemplate: '...' });
 * };
 * ```
 */
export const useTasks = (): UseTasksReturn => {
  const [tasks, setTasks] = useState<TaskTemplateResponse[]>([]);
  const [task, setTask] = useState<TaskTemplateResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  /**
   * Fetch all task templates with optional filters.
   */
  const fetchTasks = useCallback(async (params?: TasksQueryParams): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const queryParams: Record<string, string> = {};
      if (params?.search) {
        queryParams.search = params.search;
      }
      if (params?.tag) {
        queryParams.tag = params.tag;
      }

      const response = await api.get<TaskTemplateResponse[]>('/tasks', {
        params: Object.keys(queryParams).length > 0 ? queryParams : undefined,
      });
      setTasks(response);
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to fetch tasks');
      setError(errorInstance);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Fetch a single task template by ID.
   */
  const fetchTask = useCallback(async (id: number): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.get<TaskTemplateResponse>(`/tasks/${id}`);
      setTask(response);
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to fetch task');
      setError(errorInstance);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Create a new task template.
   */
  const createTask = useCallback(
    async (data: TaskTemplateRequest): Promise<TaskTemplateResponse> => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await api.post<TaskTemplateResponse>('/tasks', data);
        setTasks((prev) => [...prev, response]);
        return response;
      } catch (err) {
        const errorInstance = err instanceof Error ? err : new Error('Failed to create task');
        setError(errorInstance);
        throw errorInstance;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  /**
   * Update an existing task template.
   */
  const updateTask = useCallback(
    async (id: number, data: TaskTemplateRequest): Promise<TaskTemplateResponse> => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await api.put<TaskTemplateResponse>(`/tasks/${id}`, data);
        setTasks((prev) => prev.map((t) => (t.id === id ? response : t)));
        if (task?.id === id) {
          setTask(response);
        }
        return response;
      } catch (err) {
        const errorInstance = err instanceof Error ? err : new Error('Failed to update task');
        setError(errorInstance);
        throw errorInstance;
      } finally {
        setIsLoading(false);
      }
    },
    [task?.id]
  );

  /**
   * Delete a task template.
   */
  const deleteTask = useCallback(
    async (id: number): Promise<void> => {
      setIsLoading(true);
      setError(null);
      try {
        await api.delete(`/tasks/${id}`);
        setTasks((prev) => prev.filter((t) => t.id !== id));
        if (task?.id === id) {
          setTask(null);
        }
      } catch (err) {
        const errorInstance = err instanceof Error ? err : new Error('Failed to delete task');
        setError(errorInstance);
        throw errorInstance;
      } finally {
        setIsLoading(false);
      }
    },
    [task?.id]
  );

  /**
   * Clear the current error state.
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    tasks,
    task,
    isLoading,
    error,
    fetchTasks,
    fetchTask,
    createTask,
    updateTask,
    deleteTask,
    clearError,
  };
};
