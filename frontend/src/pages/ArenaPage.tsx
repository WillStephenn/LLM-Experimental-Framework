/**
 * Arena Page
 *
 * Side-by-side model comparison interface.
 */
import { useCallback, useMemo, useState } from 'react';
import { MetricsDisplay, type GenerationMetrics } from '@/components/common';
import { useGenerate, useOllama } from '@/hooks/useOllama';
import { useConfigStore } from '@/store/configStore';
import type { GenerationRequest, GenerationResponse } from '@/types';

const buildMetrics = (response: GenerationResponse): GenerationMetrics => ({
  durationMs: response.durationMs,
  tokensPerSecond: response.tokensPerSecond,
  timeToFirstTokenMs: response.timeToFirstTokenMs,
  promptTokens: response.promptTokens,
  completionTokens: response.completionTokens,
});

export function ArenaPage(): React.JSX.Element {
  const { hyperparameters } = useConfigStore();
  const { models, isLoading: modelsLoading, error, isAvailable } = useOllama();
  const leftGeneration = useGenerate();
  const rightGeneration = useGenerate();
  const [leftModel, setLeftModel] = useState('');
  const [rightModel, setRightModel] = useState('');
  const [prompt, setPrompt] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const isBusy = leftGeneration.isLoading || rightGeneration.isLoading;
  const promptLength = prompt.trim().length;

  const isSendDisabled = useMemo(() => {
    return isBusy || promptLength === 0;
  }, [isBusy, promptLength]);

  const buildRequest = useCallback(
    (model: string, requestPrompt: string): GenerationRequest => ({
      model,
      prompt: requestPrompt,
      temperature: hyperparameters.temperature,
      topP: hyperparameters.topP,
      topK: hyperparameters.topK,
      contextWindow: hyperparameters.contextWindow,
      maxTokens: hyperparameters.maxTokens ?? undefined,
    }),
    [hyperparameters]
  );

  const handleSend = useCallback(async (): Promise<void> => {
    const trimmedPrompt = prompt.trim();
    if (!trimmedPrompt || isBusy) {
      return;
    }

    if (!leftModel || !rightModel) {
      setFormError('Select a model for each side before sending a prompt.');
      return;
    }

    setFormError(null);
    setPrompt('');

    await Promise.all([
      leftGeneration.generate(buildRequest(leftModel, trimmedPrompt)),
      rightGeneration.generate(buildRequest(rightModel, trimmedPrompt)),
    ]);
  }, [buildRequest, isBusy, leftGeneration, leftModel, prompt, rightGeneration, rightModel]);

  const handleSendClick = (): void => {
    void handleSend();
  };

  const handlePromptKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>): void => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void handleSend();
    }
  };

  const handlePromptChange = (event: React.ChangeEvent<HTMLTextAreaElement>): void => {
    setPrompt(event.target.value);
    if (formError) {
      setFormError(null);
    }
  };

  const renderResponse = (
    response: GenerationResponse | null,
    isLoading: boolean,
    responseError: Error | null,
    testId: string
  ): React.JSX.Element => {
    if (isLoading) {
      return (
        <div
          className="flex items-center justify-center h-full text-sm text-gray-500 bg-gray-50 border border-dashed border-gray-200 rounded-md"
          data-testid={`${testId}-loading`}
        >
          Generating response...
        </div>
      );
    }

    if (responseError) {
      return (
        <div
          className="text-sm text-error bg-red-50 border border-error rounded-md p-3"
          data-testid={`${testId}-error`}
        >
          {responseError.message}
        </div>
      );
    }

    if (!response) {
      return (
        <div
          className="text-sm text-gray-500 bg-gray-50 border border-dashed border-gray-200 rounded-md p-4 text-center"
          data-testid={`${testId}-empty`}
        >
          No response yet. Send a prompt to compare outputs.
        </div>
      );
    }

    return (
      <div
        className="text-sm text-gray-700 whitespace-pre-wrap bg-gray-50 border border-gray-200 rounded-md p-4"
        data-testid={`${testId}-content`}
      >
        {response.response}
      </div>
    );
  };

  return (
    <div className="space-y-6" data-testid="arena-page">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Arena</h1>
        <p className="text-sm text-gray-600">
          Compare two models side-by-side with the same prompt and configuration.
        </p>
      </div>

      {!isAvailable && !modelsLoading && (
        <div
          className="flex items-start gap-2 rounded-md border border-error bg-red-50 px-4 py-3 text-sm text-error"
          data-testid="arena-ollama-error"
        >
          <span className="font-medium">Ollama unavailable.</span>
          <span>{error?.message ?? 'Start the Ollama service to fetch models.'}</span>
        </div>
      )}

      {formError && (
        <div
          className="rounded-md border border-warning bg-amber-50 px-4 py-3 text-sm text-warning"
          data-testid="arena-form-error"
        >
          {formError}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <section
          className="bg-white rounded-lg shadow p-6 flex flex-col gap-4"
          data-testid="arena-left-panel"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Left model</h2>
              <p className="text-xs text-gray-500">Choose the first model for comparison.</p>
            </div>
            <span className="text-xs font-medium text-gray-400 uppercase tracking-wide">A</span>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700" htmlFor="arena-left-model-select">
              Model
            </label>
            <select
              id="arena-left-model-select"
              value={leftModel}
              onChange={(event) => setLeftModel(event.target.value)}
              disabled={!isAvailable || modelsLoading}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200 disabled:bg-gray-100 disabled:cursor-not-allowed"
              data-testid="arena-left-model-select"
            >
              <option value="">Select a model...</option>
              {models.map((model) => (
                <option key={model} value={model}>
                  {model}
                </option>
              ))}
            </select>
            {modelsLoading && (
              <p className="text-xs text-gray-400" data-testid="arena-left-model-loading">
                Loading models...
              </p>
            )}
          </div>

          <div className="flex-1 min-h-[200px]">
            {renderResponse(
              leftGeneration.response,
              leftGeneration.isLoading,
              leftGeneration.error,
              'arena-left-response'
            )}
          </div>

          {leftGeneration.response && (
            <MetricsDisplay
              metrics={buildMetrics(leftGeneration.response)}
              mode="compact"
              className="mt-2"
              data-testid="arena-left-metrics"
            />
          )}
        </section>

        <section
          className="bg-white rounded-lg shadow p-6 flex flex-col gap-4"
          data-testid="arena-right-panel"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Right model</h2>
              <p className="text-xs text-gray-500">Choose the second model for comparison.</p>
            </div>
            <span className="text-xs font-medium text-gray-400 uppercase tracking-wide">B</span>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700" htmlFor="arena-right-model-select">
              Model
            </label>
            <select
              id="arena-right-model-select"
              value={rightModel}
              onChange={(event) => setRightModel(event.target.value)}
              disabled={!isAvailable || modelsLoading}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200 disabled:bg-gray-100 disabled:cursor-not-allowed"
              data-testid="arena-right-model-select"
            >
              <option value="">Select a model...</option>
              {models.map((model) => (
                <option key={model} value={model}>
                  {model}
                </option>
              ))}
            </select>
            {modelsLoading && (
              <p className="text-xs text-gray-400" data-testid="arena-right-model-loading">
                Loading models...
              </p>
            )}
          </div>

          <div className="flex-1 min-h-[200px]">
            {renderResponse(
              rightGeneration.response,
              rightGeneration.isLoading,
              rightGeneration.error,
              'arena-right-response'
            )}
          </div>

          {rightGeneration.response && (
            <MetricsDisplay
              metrics={buildMetrics(rightGeneration.response)}
              mode="compact"
              className="mt-2"
              data-testid="arena-right-metrics"
            />
          )}
        </section>
      </div>

      <section className="bg-white rounded-lg shadow p-6" data-testid="arena-prompt-panel">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">Shared prompt</h2>
            <p className="text-xs text-gray-500">
              Send the same prompt to both models to compare responses.
            </p>
          </div>
          <button
            type="button"
            className="bg-brand-green hover:bg-brand-green-dark text-white font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2 disabled:bg-gray-200 disabled:text-gray-500 disabled:cursor-not-allowed"
            onClick={handleSendClick}
            disabled={isSendDisabled}
            aria-label="Send prompt to both models"
            data-testid="arena-send-button"
          >
            {isBusy ? 'Generating...' : 'Send to both'}
          </button>
        </div>

        <div className="mt-4 space-y-2">
          <label className="text-sm font-medium text-gray-700" htmlFor="arena-prompt-input">
            Prompt
          </label>
          <textarea
            id="arena-prompt-input"
            value={prompt}
            onChange={handlePromptChange}
            onKeyDown={handlePromptKeyDown}
            rows={4}
            placeholder="Enter the prompt to compare responses..."
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
            data-testid="arena-prompt-input"
          />
          <div className="flex items-center justify-between text-xs text-gray-500">
            <span>Use line breaks to add additional context.</span>
            <span>{promptLength} characters</span>
          </div>
        </div>
      </section>
    </div>
  );
}
