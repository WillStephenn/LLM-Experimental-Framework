/**
 * Sandbox Page
 *
 * Interactive playground for experimenting with models in real-time.
 */
import { useCallback, useMemo, useRef, useState } from 'react';
import { ConfigPanel, MetricsDisplay, type GenerationMetrics } from '@/components/common';
import { useGenerate } from '@/hooks/useOllama';
import { useConfigStore } from '@/store/configStore';
import type { GenerationRequest, GenerationResponse } from '@/types';

type ChatRole = 'user' | 'assistant';

interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  metrics?: GenerationMetrics;
  timestamp: string;
}

const createMessageId = (): string => {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `msg-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const buildMetrics = (response: GenerationResponse): GenerationMetrics => ({
  durationMs: response.durationMs,
  tokensPerSecond: response.tokensPerSecond,
  timeToFirstTokenMs: response.timeToFirstTokenMs,
  promptTokens: response.promptTokens,
  completionTokens: response.completionTokens,
});

export function SandboxPage(): React.JSX.Element {
  const { model, hyperparameters } = useConfigStore();
  const { generate, isLoading } = useGenerate();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [prompt, setPrompt] = useState('');
  const [systemPrompt, setSystemPrompt] = useState('');
  const [systemPromptId, setSystemPromptId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const endOfMessagesRef = useRef<HTMLDivElement | null>(null);

  const handleSystemPromptChange = useCallback((id: number | null, content: string): void => {
    setSystemPromptId(id);
    setSystemPrompt(content);
  }, []);

  const isSendDisabled = useMemo(() => {
    return isLoading || prompt.trim().length === 0;
  }, [isLoading, prompt]);

  const modelLabel = model ?? 'Select a model';
  const systemPromptLabel = systemPrompt
    ? systemPromptId
      ? 'Preset active'
      : 'Custom active'
    : 'None';

  const handleSend = async (overridePrompt?: string): Promise<void> => {
    const promptValue = overridePrompt ?? prompt;
    const trimmedPrompt = promptValue.trim();
    if (!trimmedPrompt || isLoading) {
      return;
    }

    if (!model) {
      setErrorMessage('Please select a model before sending a prompt.');
      return;
    }

    if (errorMessage) {
      setErrorMessage(null);
    }

    const userMessage: ChatMessage = {
      id: createMessageId(),
      role: 'user',
      content: trimmedPrompt,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => {
      const updated = [...prev, userMessage];
      return updated.slice(-50);
    });
    endOfMessagesRef.current?.scrollIntoView({ behavior: 'smooth' });
    setPrompt('');

    const request: GenerationRequest = {
      model,
      prompt: trimmedPrompt,
      systemPrompt: systemPrompt || undefined,
      temperature: hyperparameters.temperature,
      topP: hyperparameters.topP,
      topK: hyperparameters.topK,
      contextWindow: hyperparameters.contextWindow,
      maxTokens: hyperparameters.maxTokens ?? undefined,
    };

    try {
      const response = await generate(request);

      if (response) {
        const assistantMessage: ChatMessage = {
          id: createMessageId(),
          role: 'assistant',
          content: response.response,
          metrics: buildMetrics(response),
          timestamp: new Date().toISOString(),
        };
        setMessages((prev) => {
          const updated = [...prev, assistantMessage];
          return updated.slice(-50);
        });
        endOfMessagesRef.current?.scrollIntoView({ behavior: 'smooth' });
      }
    } catch (generationError) {
      const errorText =
        generationError instanceof Error ? generationError.message : 'Generation request failed';
      setErrorMessage(errorText);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>): void => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      const submittedValue = event.currentTarget.value;
      void handleSend(submittedValue);
    }
  };

  return (
    <div className="space-y-6" data-testid="sandbox-page">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Sandbox</h1>
        <p className="text-sm text-gray-600">
          Experiment with models in real-time and review response metrics instantly.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="bg-white rounded-lg shadow p-6 flex flex-col min-h-[520px]">
          <div className="flex flex-wrap items-start justify-between gap-4 mb-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Chat</h2>
              <p className="text-xs text-gray-500">
                Messages stay in this session until you refresh the page.
              </p>
            </div>
            <div className="text-xs text-gray-500 text-right" data-testid="sandbox-session-status">
              <div>Model: {modelLabel}</div>
              <div>System prompt: {systemPromptLabel}</div>
            </div>
          </div>

          <div
            className="flex-1 space-y-4 overflow-y-auto pr-2"
            role="log"
            aria-live="polite"
            data-testid="sandbox-message-list"
          >
            {messages.length === 0 ? (
              <div
                className="text-sm text-gray-500 bg-gray-50 border border-dashed border-gray-200 rounded-lg px-4 py-6 text-center"
                data-testid="sandbox-empty-state"
              >
                No messages yet. Start by sending a prompt to the model.
              </div>
            ) : (
              messages.map((message) => {
                const isUser = message.role === 'user';
                return (
                  <div
                    key={message.id}
                    className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}
                    data-testid={`sandbox-message-${message.role}`}
                  >
                    <span className="text-xs text-gray-500 mb-1">
                      {isUser ? 'You' : 'Assistant'}
                    </span>
                    <div
                      className={`max-w-[80%] rounded-lg px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap ${
                        isUser
                          ? 'bg-brand-green text-white'
                          : 'bg-gray-50 border border-gray-200 text-gray-900'
                      }`}
                    >
                      {message.content}
                    </div>
                    {!isUser && message.metrics && (
                      <MetricsDisplay
                        metrics={message.metrics}
                        mode="compact"
                        className="mt-2 w-full max-w-[80%]"
                      />
                    )}
                  </div>
                );
              })
            )}
            <div ref={endOfMessagesRef} />
          </div>

          <div className="mt-6 border-t border-gray-100 pt-4 space-y-3">
            {errorMessage && (
              <div
                className="text-sm text-error bg-red-50 border border-red-200 rounded-md px-3 py-2"
                data-testid="sandbox-error"
              >
                {errorMessage}
              </div>
            )}
            <div className="space-y-2">
              <label className="text-sm font-medium text-gray-700" htmlFor="sandbox-prompt">
                Prompt
              </label>
              <textarea
                id="sandbox-prompt"
                value={prompt}
                onChange={(event) => {
                  setPrompt(event.target.value);
                }}
                onKeyDown={handleKeyDown}
                rows={4}
                placeholder="Type a prompt for the model..."
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200 resize-none"
                data-testid="sandbox-prompt-input"
              />
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <span className="text-xs text-gray-500">
                Press Enter to send, Shift+Enter for a new line.
              </span>
              <button
                type="button"
                onClick={() => void handleSend()}
                disabled={isSendDisabled}
                className="inline-flex items-center justify-center bg-brand-green hover:bg-brand-green-dark text-white text-sm font-medium px-4 py-2 rounded-md transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-green focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                data-testid="sandbox-send-button"
                aria-label="Send prompt to model"
              >
                {isLoading ? 'Generating...' : 'Send'}
              </button>
            </div>
          </div>
        </section>

        <aside className="space-y-4">
          <ConfigPanel
            onSystemPromptChange={handleSystemPromptChange}
            data-testid="sandbox-config-panel"
          />
        </aside>
      </div>
    </div>
  );
}
