/**
 * ModelSelector Component
 *
 * A dropdown component for selecting an Ollama model.
 * Displays available models fetched from the Ollama API and stores
 * the selection in the global config store.
 *
 * Features:
 * - Loading spinner while fetching models
 * - Error message if Ollama is unavailable
 * - Integration with config store for global state
 *
 * @module components/common/ModelSelector
 */
import * as Select from '@radix-ui/react-select';
import { useOllama } from '@/hooks/useOllama';
import { useConfigStore } from '@/store/configStore';

/**
 * Props for the ModelSelector component
 */
export interface ModelSelectorProps {
  /** Optional label for the selector */
  label?: string;
  /** Test ID for testing */
  'data-testid'?: string;
  /** Optional additional CSS classes */
  className?: string;
  /** Whether the selector is disabled */
  disabled?: boolean;
}

/**
 * Chevron icon for the select trigger
 */
function ChevronDownIcon({ className }: { className?: string }): React.JSX.Element {
  return (
    <svg
      className={className ?? 'w-4 h-4'}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      aria-hidden="true"
    >
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
    </svg>
  );
}

/**
 * Check icon for selected item
 */
function CheckIcon({ className }: { className?: string }): React.JSX.Element {
  return (
    <svg
      className={className ?? 'w-4 h-4'}
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      aria-hidden="true"
    >
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
    </svg>
  );
}

/**
 * Loading spinner component
 */
function LoadingSpinner({ className }: { className?: string }): React.JSX.Element {
  return (
    <svg
      className={className ?? 'w-5 h-5 animate-spin'}
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
      data-testid="model-selector-spinner"
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

/**
 * Alert/error icon
 */
function AlertIcon({ className }: { className?: string }): React.JSX.Element {
  return (
    <svg
      className={className ?? 'w-5 h-5'}
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

/**
 * Refresh icon for retry button
 */
function RefreshIcon({ className }: { className?: string }): React.JSX.Element {
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
        d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
      />
    </svg>
  );
}

/**
 * ModelSelector component for selecting Ollama models
 *
 * @param props - Component props
 * @returns JSX element
 *
 * @example
 * ```tsx
 * <ModelSelector label="Select Model" />
 * ```
 */
export function ModelSelector({
  label = 'Model',
  'data-testid': testId = 'model-selector',
  className,
  disabled = false,
}: ModelSelectorProps): React.JSX.Element {
  const { models, isLoading, error, isAvailable, refetch } = useOllama();
  const selectedModel = useConfigStore((state) => state.model);
  const setModel = useConfigStore((state) => state.setModel);

  const handleValueChange = (value: string): void => {
    setModel(value);
  };

  const handleRetry = (): void => {
    void refetch();
  };

  // Loading state
  if (isLoading) {
    return (
      <div className={`space-y-1 ${className ?? ''}`} data-testid={testId}>
        {label && <span className="text-sm font-medium text-gray-700">{label}</span>}
        <div
          className="flex items-center gap-2 px-3 py-2 border border-gray-300 rounded-md bg-gray-50"
          data-testid={`${testId}-loading`}
        >
          <LoadingSpinner className="w-4 h-4 animate-spin text-gray-500" />
          <span className="text-sm text-gray-500">Loading models...</span>
        </div>
      </div>
    );
  }

  // Error state
  if (error || !isAvailable) {
    const errorMessage = error?.message ?? 'Ollama service unavailable';
    return (
      <div className={`space-y-1 ${className ?? ''}`} data-testid={testId}>
        {label && <span className="text-sm font-medium text-gray-700">{label}</span>}
        <div
          className="flex items-center justify-between gap-2 px-3 py-2 border border-error rounded-md bg-red-50"
          data-testid={`${testId}-error`}
        >
          <div className="flex items-center gap-2">
            <AlertIcon className="w-4 h-4 text-error" />
            <span className="text-sm text-error">{errorMessage}</span>
          </div>
          <button
            type="button"
            onClick={handleRetry}
            className="flex items-center gap-1 px-2 py-1 text-xs font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-1"
            aria-label="Retry loading models"
            data-testid={`${testId}-retry`}
          >
            <RefreshIcon className="w-3 h-3" />
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Empty models state
  if (models.length === 0) {
    return (
      <div className={`space-y-1 ${className ?? ''}`} data-testid={testId}>
        {label && <span className="text-sm font-medium text-gray-700">{label}</span>}
        <div
          className="flex items-center gap-2 px-3 py-2 border border-warning rounded-md bg-amber-50"
          data-testid={`${testId}-empty`}
        >
          <AlertIcon className="w-4 h-4 text-warning" />
          <span className="text-sm text-warning">No models available in Ollama</span>
        </div>
      </div>
    );
  }

  // Normal state with models
  return (
    <div className={`space-y-1 ${className ?? ''}`} data-testid={testId}>
      {label && (
        <label className="text-sm font-medium text-gray-700" id={`${testId}-label`}>
          {label}
        </label>
      )}
      <Select.Root
        value={selectedModel ?? undefined}
        onValueChange={handleValueChange}
        disabled={disabled}
      >
        <Select.Trigger
          className="w-full flex items-center justify-between px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200 disabled:bg-gray-100 disabled:cursor-not-allowed"
          aria-labelledby={label ? `${testId}-label` : undefined}
          aria-label={label ? undefined : 'Model selector'}
          data-testid={`${testId}-trigger`}
        >
          <Select.Value placeholder="Select a model..." />
          <Select.Icon>
            <ChevronDownIcon className="w-4 h-4 text-gray-500" />
          </Select.Icon>
        </Select.Trigger>

        <Select.Portal>
          <Select.Content
            className="overflow-hidden bg-white rounded-md shadow-lg border border-gray-200 z-50"
            position="popper"
            sideOffset={4}
            data-testid={`${testId}-content`}
          >
            <Select.Viewport className="p-1 max-h-60 overflow-y-auto">
              {models.map((model) => (
                <Select.Item
                  key={model}
                  value={model}
                  className="relative flex items-center px-8 py-2 text-sm text-gray-900 rounded cursor-pointer select-none hover:bg-gray-100 focus:bg-gray-100 focus:outline-none data-[state=checked]:bg-brand-green/10"
                  data-testid={`${testId}-option-${model}`}
                >
                  <Select.ItemIndicator className="absolute left-2 flex items-center">
                    <CheckIcon className="w-4 h-4 text-brand-green" />
                  </Select.ItemIndicator>
                  <Select.ItemText>{model}</Select.ItemText>
                </Select.Item>
              ))}
            </Select.Viewport>
          </Select.Content>
        </Select.Portal>
      </Select.Root>
    </div>
  );
}
