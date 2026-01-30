/**
 * ConfigPanel Component
 *
 * A collapsible panel containing all configuration options for model parameters
 * and system prompts. Provides controls for temperature, top-p, top-k, context
 * window, max tokens, and system prompt selection.
 *
 * Features:
 * - Collapsible with smooth animation
 * - Model selector integration
 * - Temperature slider (0-2, step 0.1)
 * - Top P slider (0-1, step 0.05)
 * - Top K number input (1-100)
 * - Context window input (512-128000)
 * - Max tokens input (optional)
 * - System prompt preset selector
 * - Custom system prompt textarea
 *
 * @module components/common/ConfigPanel
 */
import { useState, useEffect } from 'react';
import * as Collapsible from '@radix-ui/react-collapsible';
import * as Slider from '@radix-ui/react-slider';
import { useConfigStore } from '@/store/configStore';
import { useSystemPrompts } from '@/hooks/useSystemPrompts';
import { ModelSelector } from './ModelSelector';

/**
 * Props for the ConfigPanel component.
 */
export interface ConfigPanelProps {
  /** Optional initial collapsed state. Defaults to false (expanded). */
  defaultCollapsed?: boolean;
  /** Callback when a system prompt is selected. */
  onSystemPromptChange?: (systemPromptId: number | null, content: string) => void;
  /** Test ID for testing. */
  'data-testid'?: string;
  /** Optional additional CSS classes. */
  className?: string;
}

/**
 * Chevron icon for the collapsible trigger.
 */
function ChevronIcon({
  className,
  isOpen,
}: {
  className?: string;
  isOpen: boolean;
}): React.JSX.Element {
  return (
    <svg
      className={`${className ?? 'w-5 h-5'} transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}
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
 * Settings/gear icon for the panel header.
 */
function SettingsIcon({ className }: { className?: string }): React.JSX.Element {
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
        d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
      />
    </svg>
  );
}

/**
 * Loading spinner component.
 */
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

/**
 * ConfigPanel component providing all configuration options.
 *
 * @param props - Component props
 * @returns JSX element
 *
 * @example
 * ```tsx
 * <ConfigPanel
 *   defaultCollapsed={false}
 *   onSystemPromptChange={(id, content) => console.log('System prompt changed:', id, content)}
 * />
 * ```
 */
export function ConfigPanel({
  defaultCollapsed = false,
  onSystemPromptChange,
  'data-testid': testId = 'config-panel',
  className,
}: ConfigPanelProps): React.JSX.Element {
  const [isOpen, setIsOpen] = useState(!defaultCollapsed);
  const [selectedSystemPromptId, setSelectedSystemPromptId] = useState<number | null>(null);
  const [customSystemPrompt, setCustomSystemPrompt] = useState('');
  const [useCustomPrompt, setUseCustomPrompt] = useState(false);

  // Config store
  const { hyperparameters, setTemperature, setTopP, setTopK, setContextWindow, setMaxTokens } =
    useConfigStore();

  // System prompts
  const { systemPrompts, isLoading: systemPromptsLoading, fetchSystemPrompts } = useSystemPrompts();

  // Fetch system prompts on mount
  useEffect(() => {
    void fetchSystemPrompts();
  }, [fetchSystemPrompts]);

  // Handle system prompt selection
  const handleSystemPromptSelect = (value: string): void => {
    if (value === 'custom') {
      setUseCustomPrompt(true);
      setSelectedSystemPromptId(null);
      onSystemPromptChange?.(null, customSystemPrompt);
    } else if (value === 'none') {
      setUseCustomPrompt(false);
      setSelectedSystemPromptId(null);
      onSystemPromptChange?.(null, '');
    } else {
      const id = parseInt(value, 10);
      setUseCustomPrompt(false);
      setSelectedSystemPromptId(id);
      const selectedPrompt = systemPrompts.find((sp) => sp.id === id);
      onSystemPromptChange?.(id, selectedPrompt?.content ?? '');
    }
  };

  // Handle custom prompt change
  const handleCustomPromptChange = (event: React.ChangeEvent<HTMLTextAreaElement>): void => {
    const value = event.target.value;
    setCustomSystemPrompt(value);
    if (useCustomPrompt) {
      onSystemPromptChange?.(null, value);
    }
  };

  // Handle max tokens change - convert empty string to null
  const handleMaxTokensChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = event.target.value;
    if (value === '') {
      setMaxTokens(null);
    } else {
      const numValue = parseInt(value, 10);
      if (!isNaN(numValue)) {
        setMaxTokens(numValue);
      }
    }
  };

  // Handle context window change
  const handleContextWindowChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = parseInt(event.target.value, 10);
    if (!isNaN(value)) {
      setContextWindow(value);
    }
  };

  // Handle top K change
  const handleTopKChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const value = parseInt(event.target.value, 10);
    if (!isNaN(value)) {
      setTopK(value);
    }
  };

  return (
    <Collapsible.Root
      open={isOpen}
      onOpenChange={setIsOpen}
      className={`bg-white rounded-lg shadow border border-gray-200 ${className ?? ''}`}
      data-testid={testId}
    >
      {/* Header / Trigger */}
      <Collapsible.Trigger asChild>
        <button
          type="button"
          className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-gray-50 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-inset rounded-lg"
          data-testid={`${testId}-trigger`}
          aria-expanded={isOpen}
        >
          <div className="flex items-center gap-2">
            <SettingsIcon className="w-5 h-5 text-gray-500" />
            <span className="text-sm font-medium text-gray-900">Configuration</span>
          </div>
          <ChevronIcon className="w-5 h-5 text-gray-500" isOpen={isOpen} />
        </button>
      </Collapsible.Trigger>

      {/* Content */}
      <Collapsible.Content
        className="overflow-hidden data-[state=open]:animate-slideDown data-[state=closed]:animate-slideUp"
        data-testid={`${testId}-content`}
      >
        <div className="px-4 pb-4 space-y-6 border-t border-gray-100">
          {/* Model Selection */}
          <div className="pt-4">
            <ModelSelector label="Model" data-testid={`${testId}-model-selector`} />
          </div>

          {/* Hyperparameters Section */}
          <div className="space-y-4">
            <h4 className="text-sm font-medium text-gray-700">Hyperparameters</h4>

            {/* Temperature Slider */}
            <div className="space-y-2" data-testid={`${testId}-temperature`}>
              <div className="flex items-center justify-between">
                <label className="text-sm text-gray-600" htmlFor="temperature-slider">
                  Temperature
                </label>
                <span className="text-sm font-medium text-gray-900">
                  {hyperparameters.temperature.toFixed(1)}
                </span>
              </div>
              <Slider.Root
                id="temperature-slider"
                className="relative flex items-center select-none touch-none w-full h-5"
                value={[hyperparameters.temperature]}
                onValueChange={(values) => setTemperature(values[0])}
                min={0}
                max={2}
                step={0.1}
                aria-label="Temperature"
                data-testid={`${testId}-temperature-slider`}
              >
                <Slider.Track className="bg-gray-200 relative grow rounded-full h-1.5">
                  <Slider.Range className="absolute bg-brand-green rounded-full h-full" />
                </Slider.Track>
                <Slider.Thumb className="block w-4 h-4 bg-white shadow border border-gray-300 rounded-full hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2 transition-colors" />
              </Slider.Root>
              <p className="text-xs text-gray-400">
                Controls randomness. Lower is more focused, higher is more creative.
              </p>
            </div>

            {/* Top P Slider */}
            <div className="space-y-2" data-testid={`${testId}-top-p`}>
              <div className="flex items-center justify-between">
                <label className="text-sm text-gray-600" htmlFor="top-p-slider">
                  Top P
                </label>
                <span className="text-sm font-medium text-gray-900">
                  {hyperparameters.topP.toFixed(2)}
                </span>
              </div>
              <Slider.Root
                id="top-p-slider"
                className="relative flex items-center select-none touch-none w-full h-5"
                value={[hyperparameters.topP]}
                onValueChange={(values) => setTopP(values[0])}
                min={0}
                max={1}
                step={0.05}
                aria-label="Top P"
                data-testid={`${testId}-top-p-slider`}
              >
                <Slider.Track className="bg-gray-200 relative grow rounded-full h-1.5">
                  <Slider.Range className="absolute bg-brand-green rounded-full h-full" />
                </Slider.Track>
                <Slider.Thumb className="block w-4 h-4 bg-white shadow border border-gray-300 rounded-full hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2 transition-colors" />
              </Slider.Root>
              <p className="text-xs text-gray-400">Nucleus sampling probability threshold.</p>
            </div>

            {/* Top K Input */}
            <div className="space-y-1" data-testid={`${testId}-top-k`}>
              <label className="text-sm text-gray-600" htmlFor="top-k-input">
                Top K
              </label>
              <input
                id="top-k-input"
                type="number"
                min={1}
                max={100}
                value={hyperparameters.topK}
                onChange={handleTopKChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                data-testid={`${testId}-top-k-input`}
              />
              <p className="text-xs text-gray-400">Limits sampling to top K tokens (1-100).</p>
            </div>

            {/* Context Window Input */}
            <div className="space-y-1" data-testid={`${testId}-context-window`}>
              <label className="text-sm text-gray-600" htmlFor="context-window-input">
                Context Window
              </label>
              <input
                id="context-window-input"
                type="number"
                min={512}
                max={128000}
                value={hyperparameters.contextWindow}
                onChange={handleContextWindowChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                data-testid={`${testId}-context-window-input`}
              />
              <p className="text-xs text-gray-400">Context window size in tokens (512-128000).</p>
            </div>

            {/* Max Tokens Input */}
            <div className="space-y-1" data-testid={`${testId}-max-tokens`}>
              <label className="text-sm text-gray-600" htmlFor="max-tokens-input">
                Max Tokens (Optional)
              </label>
              <input
                id="max-tokens-input"
                type="number"
                min={1}
                value={hyperparameters.maxTokens ?? ''}
                onChange={handleMaxTokensChange}
                placeholder="No limit"
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                data-testid={`${testId}-max-tokens-input`}
              />
              <p className="text-xs text-gray-400">
                Maximum tokens to generate. Leave empty for no limit.
              </p>
            </div>
          </div>

          {/* System Prompt Section */}
          <div className="space-y-4">
            <h4 className="text-sm font-medium text-gray-700">System Prompt</h4>

            {/* Preset Selector */}
            <div className="space-y-1" data-testid={`${testId}-system-prompt`}>
              <label className="text-sm text-gray-600" htmlFor="system-prompt-select">
                Preset
              </label>
              {systemPromptsLoading ? (
                <div
                  className="flex items-center gap-2 px-3 py-2 border border-gray-300 rounded-md bg-gray-50"
                  data-testid={`${testId}-system-prompt-loading`}
                >
                  <LoadingSpinner className="w-4 h-4 text-gray-500" />
                  <span className="text-sm text-gray-500">Loading prompts...</span>
                </div>
              ) : (
                <select
                  id="system-prompt-select"
                  value={
                    useCustomPrompt
                      ? 'custom'
                      : selectedSystemPromptId !== null
                        ? selectedSystemPromptId.toString()
                        : 'none'
                  }
                  onChange={(e) => handleSystemPromptSelect(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                  data-testid={`${testId}-system-prompt-select`}
                >
                  <option value="none">No system prompt</option>
                  {systemPrompts.map((prompt) => (
                    <option key={prompt.id} value={prompt.id.toString()}>
                      {prompt.alias}
                    </option>
                  ))}
                  <option value="custom">Custom prompt...</option>
                </select>
              )}
            </div>

            {/* Custom System Prompt Textarea */}
            {useCustomPrompt && (
              <div className="space-y-1" data-testid={`${testId}-custom-prompt`}>
                <label className="text-sm text-gray-600" htmlFor="custom-prompt-textarea">
                  Custom System Prompt
                </label>
                <textarea
                  id="custom-prompt-textarea"
                  value={customSystemPrompt}
                  onChange={handleCustomPromptChange}
                  placeholder="Enter your custom system prompt..."
                  rows={4}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200 resize-y"
                  data-testid={`${testId}-custom-prompt-textarea`}
                />
                <p className="text-xs text-gray-400">Enter a custom system prompt for the model.</p>
              </div>
            )}

            {/* Display Selected Preset Content */}
            {!useCustomPrompt && selectedSystemPromptId !== null && (
              <div className="space-y-1" data-testid={`${testId}-prompt-preview`}>
                <span className="text-sm text-gray-600">Preview</span>
                <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-700 max-h-32 overflow-y-auto">
                  {systemPrompts.find((sp) => sp.id === selectedSystemPromptId)?.content ??
                    'No content available'}
                </div>
              </div>
            )}
          </div>
        </div>
      </Collapsible.Content>
    </Collapsible.Root>
  );
}
