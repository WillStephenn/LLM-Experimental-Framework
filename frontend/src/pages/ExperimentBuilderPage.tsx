/**
 * Experiment Builder Page
 *
 * Create and configure new experiments.
 */
import { useEffect, useMemo, useState } from 'react';
import { useTasks } from '@/hooks/useTasks';
import type { TaskTemplateResponse } from '@/types';

const extractVariables = (promptTemplate: string): string[] => {
  const matches = promptTemplate.matchAll(/\{\{(\w+)\}\}/g);
  const variables = new Set<string>();
  for (const match of matches) {
    if (match[1]) {
      variables.add(match[1]);
    }
  }
  return Array.from(variables);
};

const resolvePromptTemplate = (
  promptTemplate: string,
  variableValues: Record<string, string>
): string => {
  return promptTemplate.replace(/\{\{(\w+)\}\}/g, (match, variable) => {
    return variableValues[variable] || match;
  });
};

function LoadingSpinner({ className }: { className?: string }): React.JSX.Element {
  return (
    <svg
      className={className ?? 'w-4 h-4 animate-spin'}
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
  );
}

function AlertIcon({ className }: { className?: string }): React.JSX.Element {
  return (
    <svg
      className={className ?? 'w-4 h-4'}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
      />
    </svg>
  );
}

export function ExperimentBuilderPage(): React.JSX.Element {
  const { tasks, isLoading, error, fetchTasks } = useTasks();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTask, setSelectedTask] = useState<TaskTemplateResponse | null>(null);
  const [variableValues, setVariableValues] = useState<Record<string, string>>({});

  useEffect(() => {
    void fetchTasks();
  }, [fetchTasks]);

  const filteredTasks = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return tasks;
    }
    return tasks.filter((task) => {
      const nameMatch = task.name.toLowerCase().includes(query);
      const descriptionMatch = (task.description ?? '').toLowerCase().includes(query);
      const tagsMatch = (task.tags ?? '').toLowerCase().includes(query);
      return nameMatch || descriptionMatch || tagsMatch;
    });
  }, [searchQuery, tasks]);

  const variableNames = useMemo(() => {
    if (!selectedTask) {
      return [];
    }
    return extractVariables(selectedTask.promptTemplate);
  }, [selectedTask]);

  useEffect(() => {
    if (variableNames.length === 0) {
      setVariableValues({});
      return;
    }
    setVariableValues((prev) => {
      const nextValues: Record<string, string> = {};
      variableNames.forEach((name) => {
        nextValues[name] = prev[name] ?? '';
      });
      return nextValues;
    });
  }, [variableNames]);

  const tagList = useMemo(() => {
    if (!selectedTask?.tags) {
      return [];
    }
    return selectedTask.tags
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean);
  }, [selectedTask]);

  const resolvedPrompt = useMemo(() => {
    if (!selectedTask) {
      return '';
    }
    return resolvePromptTemplate(selectedTask.promptTemplate, variableValues);
  }, [selectedTask, variableValues]);

  const handleTaskChange = (value: string): void => {
    if (!value) {
      setSelectedTask(null);
      return;
    }
    const selectedId = Number(value);
    const task = tasks.find((item) => item.id === selectedId) ?? null;
    setSelectedTask(task);
  };

  return (
    <div className="space-y-6" data-testid="experiment-builder-page">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Experiment Builder</h1>
        <p className="text-sm text-gray-600">Create and configure new experiments</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <section className="space-y-6">
          <div className="bg-white rounded-lg shadow p-6 space-y-6">
            <div>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-lg font-semibold text-gray-900">Step 1: Select Task Template</h2>
                <span className="text-xs font-medium text-gray-500">Required</span>
              </div>
              <p className="text-sm text-gray-600 mt-1">
                Choose a task template to define the base prompt for your experiment.
              </p>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-gray-700" htmlFor="task-template-search">
                Search tasks
              </label>
              <input
                id="task-template-search"
                type="text"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Search by name, description, or tag..."
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                data-testid="experiment-builder-task-search"
                aria-label="Search task templates"
              />
            </div>

            <div className="space-y-1" data-testid="experiment-builder-task-select-section">
              <label className="text-sm font-medium text-gray-700" htmlFor="task-template-select">
                Task template
              </label>
              {isLoading ? (
                <div
                  className="flex items-center gap-2 px-3 py-2 border border-gray-300 rounded-md bg-gray-50"
                  data-testid="experiment-builder-task-loading"
                >
                  <LoadingSpinner className="w-4 h-4 text-gray-500" />
                  <span className="text-sm text-gray-500">Loading task templates...</span>
                </div>
              ) : error ? (
                <div
                  className="flex items-center gap-2 px-3 py-2 border border-error rounded-md bg-red-50"
                  data-testid="experiment-builder-task-error"
                >
                  <AlertIcon className="w-4 h-4 text-error" />
                  <span className="text-sm text-error">
                    {error.message || 'Unable to load task templates.'}
                  </span>
                </div>
              ) : filteredTasks.length === 0 ? (
                <div
                  className="flex items-center gap-2 px-3 py-2 border border-gray-200 rounded-md bg-gray-50 text-sm text-gray-500"
                  data-testid="experiment-builder-task-empty"
                >
                  No task templates match your search.
                </div>
              ) : (
                <select
                  id="task-template-select"
                  value={selectedTask?.id.toString() ?? ''}
                  onChange={(event) => handleTaskChange(event.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                  data-testid="experiment-builder-task-select"
                >
                  <option value="">Select a task template...</option>
                  {filteredTasks.map((task) => (
                    <option key={task.id} value={task.id.toString()}>
                      {task.name}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div className="space-y-4" data-testid="experiment-builder-task-variables">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium text-gray-700">Task variables</h3>
                {variableNames.length > 0 && (
                  <span className="text-xs text-gray-500">{variableNames.length} variables</span>
                )}
              </div>

              {!selectedTask ? (
                <div className="text-sm text-gray-500 bg-gray-50 border border-dashed border-gray-200 rounded-lg px-4 py-3">
                  Select a task template to configure its variables.
                </div>
              ) : variableNames.length === 0 ? (
                <div className="text-sm text-gray-500 bg-gray-50 border border-dashed border-gray-200 rounded-lg px-4 py-3">
                  This task template has no variables to fill in.
                </div>
              ) : (
                <div className="grid gap-4 sm:grid-cols-2">
                  {variableNames.map((variable) => (
                    <div key={variable} className="space-y-1">
                      <label
                        className="text-sm font-medium text-gray-700"
                        htmlFor={`task-variable-${variable}`}
                      >
                        {`{{${variable}}}`}
                      </label>
                      <input
                        id={`task-variable-${variable}`}
                        type="text"
                        value={variableValues[variable] ?? ''}
                        onChange={(event) =>
                          setVariableValues((prev) => ({
                            ...prev,
                            [variable]: event.target.value,
                          }))
                        }
                        placeholder={`Enter ${variable} value`}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                        data-testid={`experiment-builder-variable-${variable}`}
                      />
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </section>

        <aside className="space-y-6" data-testid="experiment-builder-task-preview">
          <div className="bg-white rounded-lg shadow p-6 space-y-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Task preview</h2>
              <p className="text-sm text-gray-600 mt-1">Review the selected task details.</p>
            </div>

            {!selectedTask ? (
              <div className="text-sm text-gray-500 bg-gray-50 border border-dashed border-gray-200 rounded-lg px-4 py-6 text-center">
                Select a task template to preview its prompt and metadata.
              </div>
            ) : (
              <div className="space-y-4">
                <div className="space-y-1">
                  <span className="text-sm font-medium text-gray-700">Name</span>
                  <p className="text-sm text-gray-900">{selectedTask.name}</p>
                </div>

                {selectedTask.description && (
                  <div className="space-y-1">
                    <span className="text-sm font-medium text-gray-700">Description</span>
                    <p className="text-sm text-gray-600">{selectedTask.description}</p>
                  </div>
                )}

                <div className="space-y-2">
                  <span className="text-sm font-medium text-gray-700">Tags</span>
                  {tagList.length === 0 ? (
                    <span className="text-sm text-gray-500">No tags added.</span>
                  ) : (
                    <div className="flex flex-wrap gap-2">
                      {tagList.map((tag) => (
                        <span
                          key={tag}
                          className="inline-flex items-center px-2 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-600"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="space-y-2">
                  <span className="text-sm font-medium text-gray-700">Prompt preview</span>
                  <div
                    className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-700 whitespace-pre-wrap max-h-64 overflow-y-auto"
                    data-testid="experiment-builder-task-preview-prompt"
                  >
                    {resolvedPrompt}
                  </div>
                </div>

                {selectedTask.evaluationNotes && (
                  <div className="space-y-2">
                    <span className="text-sm font-medium text-gray-700">Evaluation notes</span>
                    <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-700 whitespace-pre-wrap">
                      {selectedTask.evaluationNotes}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
