/**
 * Task Library Page
 *
 * Browse and manage task templates.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import * as Tabs from '@radix-ui/react-tabs';
import { useTasks } from '@/hooks/useTasks';
import { useSystemPrompts } from '@/hooks/useSystemPrompts';
import type { TaskTemplateResponse, SystemPromptResponse } from '@/types';

type LibraryTab = 'tasks' | 'prompts';

type ModalState =
  | { type: 'edit-task'; task: TaskTemplateResponse }
  | { type: 'delete-task'; task: TaskTemplateResponse }
  | { type: 'edit-prompt'; prompt: SystemPromptResponse }
  | { type: 'delete-prompt'; prompt: SystemPromptResponse };

const formatDate = (value: string): string => {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value));
};

const parseTags = (tags: string | null): string[] => {
  if (!tags) {
    return [];
  }
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0);
};

export function TaskLibraryPage(): React.JSX.Element {
  const { tasks, isLoading: tasksLoading, error: tasksError, fetchTasks, deleteTask } = useTasks();
  const {
    systemPrompts,
    isLoading: promptsLoading,
    error: promptsError,
    fetchSystemPrompts,
    deleteSystemPrompt,
  } = useSystemPrompts();
  const [activeTab, setActiveTab] = useState<LibraryTab>('tasks');
  const [searchValue, setSearchValue] = useState('');
  const [tagFilter, setTagFilter] = useState('all');
  const [modalState, setModalState] = useState<ModalState | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  useEffect(() => {
    void fetchTasks();
    void fetchSystemPrompts();
  }, [fetchTasks, fetchSystemPrompts]);

  const availableTags = useMemo(() => {
    const tagSet = new Set<string>();
    tasks.forEach((task) => {
      parseTags(task.tags).forEach((tag) => tagSet.add(tag));
    });
    return Array.from(tagSet).sort((a, b) => a.localeCompare(b));
  }, [tasks]);

  const filteredTasks = useMemo(() => {
    const normalisedSearch = searchValue.trim().toLowerCase();
    const normalisedTag = tagFilter.toLowerCase();
    return tasks.filter((task) => {
      const matchesSearch =
        normalisedSearch.length === 0 ||
        task.name.toLowerCase().includes(normalisedSearch) ||
        (task.description ?? '').toLowerCase().includes(normalisedSearch);
      const taskTags = parseTags(task.tags).map((tag) => tag.toLowerCase());
      const matchesTag =
        normalisedTag === 'all' || taskTags.some((tag) => tag === normalisedTag);
      return matchesSearch && matchesTag;
    });
  }, [searchValue, tagFilter, tasks]);

  const filteredPrompts = useMemo(() => {
    const normalisedSearch = searchValue.trim().toLowerCase();
    return systemPrompts.filter((prompt) => {
      if (normalisedSearch.length === 0) {
        return true;
      }
      return (
        prompt.alias.toLowerCase().includes(normalisedSearch) ||
        prompt.content.toLowerCase().includes(normalisedSearch)
      );
    });
  }, [searchValue, systemPrompts]);

  const closeModal = useCallback(() => {
    setModalState(null);
    setModalError(null);
  }, []);

  const openModal = useCallback((state: ModalState) => {
    setModalState(state);
    setModalError(null);
  }, []);

  const handleDeleteConfirm = useCallback(async (): Promise<void> => {
    if (!modalState) {
      return;
    }
    try {
      if (modalState.type === 'delete-task') {
        await deleteTask(modalState.task.id);
      }
      if (modalState.type === 'delete-prompt') {
        await deleteSystemPrompt(modalState.prompt.id);
      }
      closeModal();
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Unable to complete the delete action.';
      setModalError(message);
    }
  }, [closeModal, deleteSystemPrompt, deleteTask, modalState]);

  const isLoading = activeTab === 'tasks' ? tasksLoading : promptsLoading;
  const error = activeTab === 'tasks' ? tasksError : promptsError;

  const renderTaskRows = (): React.JSX.Element => {
    if (filteredTasks.length === 0) {
      return (
        <div className="text-center py-12" data-testid="task-library-empty">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 text-gray-400">
            0
          </div>
          <h3 className="mt-4 text-lg font-medium text-gray-900">No task templates found</h3>
          <p className="mt-2 text-sm text-gray-500">
            Try adjusting your search or tag filters.
          </p>
        </div>
      );
    }

    return (
      <div className="overflow-x-auto" data-testid="task-library-table">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Name
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Tags
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredTasks.map((task) => {
              const tags = parseTags(task.tags);
              return (
                <tr
                  key={task.id}
                  className="hover:bg-gray-50 transition-colors"
                  data-testid={`task-row-${task.id}`}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    <div className="font-medium text-gray-900">{task.name}</div>
                    <div className="text-xs text-gray-500 truncate max-w-xs">
                      {task.description ?? 'No description provided'}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {tags.length > 0 ? (
                      <div className="flex flex-wrap gap-2">
                        {tags.map((tag) => (
                          <span
                            key={`${task.id}-${tag}`}
                            className="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-md"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-xs text-gray-400">No tags</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatDate(task.createdAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right space-x-3">
                    <button
                      type="button"
                      className="text-gray-600 hover:text-gray-900 font-medium transition-colors"
                      onClick={() => openModal({ type: 'edit-task', task })}
                      data-testid={`task-edit-${task.id}`}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="text-red-500 hover:text-red-600 font-medium transition-colors"
                      onClick={() => openModal({ type: 'delete-task', task })}
                      data-testid={`task-delete-${task.id}`}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  };

  const renderPromptRows = (): React.JSX.Element => {
    if (filteredPrompts.length === 0) {
      return (
        <div className="text-center py-12" data-testid="prompt-library-empty">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-gray-100 text-gray-400">
            0
          </div>
          <h3 className="mt-4 text-lg font-medium text-gray-900">No system prompts found</h3>
          <p className="mt-2 text-sm text-gray-500">
            Try adjusting your search to locate a system prompt.
          </p>
        </div>
      );
    }

    return (
      <div className="overflow-x-auto" data-testid="prompt-library-table">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Alias
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredPrompts.map((prompt) => (
              <tr
                key={prompt.id}
                className="hover:bg-gray-50 transition-colors"
                data-testid={`prompt-row-${prompt.id}`}
              >
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <div className="font-medium text-gray-900">{prompt.alias}</div>
                  <div className="text-xs text-gray-500 truncate max-w-xs">
                    {prompt.content}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {formatDate(prompt.createdAt)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right space-x-3">
                  <button
                    type="button"
                    className="text-gray-600 hover:text-gray-900 font-medium transition-colors"
                    onClick={() => openModal({ type: 'edit-prompt', prompt })}
                    data-testid={`prompt-edit-${prompt.id}`}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="text-red-500 hover:text-red-600 font-medium transition-colors"
                    onClick={() => openModal({ type: 'delete-prompt', prompt })}
                    data-testid={`prompt-delete-${prompt.id}`}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  const isDeleteModal =
    modalState?.type === 'delete-task' || modalState?.type === 'delete-prompt';
  const modalTitle = useMemo(() => {
    if (!modalState) {
      return '';
    }
    switch (modalState.type) {
      case 'edit-task':
        return 'Edit task template';
      case 'delete-task':
        return 'Delete task template';
      case 'edit-prompt':
        return 'Edit system prompt';
      case 'delete-prompt':
        return 'Delete system prompt';
      default:
        return '';
    }
  }, [modalState]);

  return (
    <div className="space-y-6" data-testid="task-library-page">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Task Library</h1>
        <p className="text-sm text-gray-600">
          Browse and manage reusable task templates and system prompts.
        </p>
      </div>

      <Tabs.Root
        value={activeTab}
        onValueChange={(value) => setActiveTab(value as LibraryTab)}
        className="space-y-6"
      >
        <Tabs.List className="inline-flex rounded-lg bg-gray-100 p-1" aria-label="Task library tabs">
          <Tabs.Trigger
            value="tasks"
            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
              activeTab === 'tasks'
                ? 'bg-brand-orange text-white shadow-md'
                : 'text-gray-600 hover:text-gray-900'
            }`}
            data-testid="task-library-tab-tasks"
          >
            Task Templates
          </Tabs.Trigger>
          <Tabs.Trigger
            value="prompts"
            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
              activeTab === 'prompts'
                ? 'bg-brand-orange text-white shadow-md'
                : 'text-gray-600 hover:text-gray-900'
            }`}
            data-testid="task-library-tab-prompts"
          >
            System Prompts
          </Tabs.Trigger>
        </Tabs.List>

        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div className="w-full md:flex-1">
              <label htmlFor="task-library-search" className="text-sm font-medium text-gray-700">
                Search
              </label>
              <input
                id="task-library-search"
                type="text"
                value={searchValue}
                onChange={(event) => setSearchValue(event.target.value)}
                className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                placeholder={
                  activeTab === 'tasks' ? 'Search task templates' : 'Search system prompts'
                }
                data-testid="task-library-search"
              />
            </div>
            {activeTab === 'tasks' && (
              <div className="w-full md:w-60">
                <label htmlFor="task-library-tag" className="text-sm font-medium text-gray-700">
                  Tag filter
                </label>
                <select
                  id="task-library-tag"
                  value={tagFilter}
                  onChange={(event) => setTagFilter(event.target.value)}
                  className="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                  data-testid="task-library-tag-filter"
                >
                  <option value="all">All tags</option>
                  {availableTags.map((tag) => (
                    <option key={tag} value={tag}>
                      {tag}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {error && (
            <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
              {error.message}
            </div>
          )}

          {isLoading ? (
            <div className="text-sm text-gray-500" data-testid="task-library-loading">
              Loading library data...
            </div>
          ) : (
            <>
              <Tabs.Content value="tasks" className="space-y-4">
                {renderTaskRows()}
              </Tabs.Content>
              <Tabs.Content value="prompts" className="space-y-4">
                {renderPromptRows()}
              </Tabs.Content>
            </>
          )}
        </div>
      </Tabs.Root>

      {modalState && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="task-library-modal-title"
          data-testid="task-library-modal"
        >
          <div className="bg-white rounded-lg shadow-lg w-full max-w-lg">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2
                id="task-library-modal-title"
                className="text-xl font-semibold text-gray-900"
              >
                {modalTitle}
              </h2>
            </div>
            <div className="px-6 py-4 space-y-3 text-sm text-gray-600">
              {modalState.type === 'edit-task' && (
                <>
                  <div>
                    <span className="font-medium text-gray-900">Name:</span> {modalState.task.name}
                  </div>
                  <div>
                    <span className="font-medium text-gray-900">Description:</span>{' '}
                    {modalState.task.description ?? 'No description provided'}
                  </div>
                  <div>
                    <span className="font-medium text-gray-900">Tags:</span>{' '}
                    {modalState.task.tags ?? 'No tags'}
                  </div>
                </>
              )}
              {modalState.type === 'edit-prompt' && (
                <>
                  <div>
                    <span className="font-medium text-gray-900">Alias:</span>{' '}
                    {modalState.prompt.alias}
                  </div>
                  <div>
                    <span className="font-medium text-gray-900">Content:</span>{' '}
                    {modalState.prompt.content}
                  </div>
                </>
              )}
              {isDeleteModal && (
                <p>
                  This action cannot be undone. Confirm that you want to delete{' '}
                  {modalState.type === 'delete-task'
                    ? `"${modalState.task.name}"`
                    : `"${modalState.prompt.alias}"`}
                  .
                </p>
              )}
              {modalError && <p className="text-red-600">{modalError}</p>}
            </div>
            <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <button
                type="button"
                className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2"
                onClick={closeModal}
              >
                Cancel
              </button>
              {isDeleteModal ? (
                <button
                  type="button"
                  className="bg-red-500 hover:bg-red-600 text-white font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
                  onClick={() => void handleDeleteConfirm()}
                  data-testid="task-library-confirm-delete"
                >
                  Delete
                </button>
              ) : (
                <button
                  type="button"
                  className="bg-brand-green hover:bg-brand-green-dark text-white font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2"
                  onClick={closeModal}
                >
                  Close
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
