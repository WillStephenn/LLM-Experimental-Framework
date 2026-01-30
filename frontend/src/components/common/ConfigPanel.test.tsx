import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ConfigPanel } from './ConfigPanel';
import { useConfigStore, DEFAULT_CONFIG_STATE, DEFAULT_HYPERPARAMETERS } from '@/store/configStore';
import { useSystemPrompts } from '@/hooks/useSystemPrompts';
import type { ConfigActions, ConfigState } from '@/store/configStore';

// Mock useOllama hook
vi.mock('@/hooks/useOllama', () => ({
  useOllama: vi.fn(() => ({
    models: ['llama3:8b', 'codellama:7b'],
    isLoading: false,
    error: null,
    isAvailable: true,
    refetch: vi.fn(),
  })),
}));

// Mock useSystemPrompts hook
vi.mock('@/hooks/useSystemPrompts', () => ({
  useSystemPrompts: vi.fn(() => ({
    systemPrompts: [],
    systemPrompt: null,
    isLoading: false,
    error: null,
    fetchSystemPrompts: vi.fn(),
    fetchSystemPrompt: vi.fn(),
    createSystemPrompt: vi.fn(),
    updateSystemPrompt: vi.fn(),
    deleteSystemPrompt: vi.fn(),
    clearError: vi.fn(),
  })),
}));

const mockUseSystemPrompts = vi.mocked(useSystemPrompts);

// Store original actions for restoration
let originalSetTemperature: ConfigActions['setTemperature'];
let originalSetTopP: ConfigActions['setTopP'];
let originalSetTopK: ConfigActions['setTopK'];
let originalSetContextWindow: ConfigActions['setContextWindow'];
let originalSetMaxTokens: ConfigActions['setMaxTokens'];

describe('ConfigPanel', () => {
  const mockFetchSystemPrompts = vi.fn();
  const mockSetTemperature = vi.fn();
  const mockSetTopP = vi.fn();
  const mockSetTopK = vi.fn();
  const mockSetContextWindow = vi.fn();
  const mockSetMaxTokens = vi.fn();
  const mockOnSystemPromptChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    // Save original actions
    originalSetTemperature = useConfigStore.getState().setTemperature;
    originalSetTopP = useConfigStore.getState().setTopP;
    originalSetTopK = useConfigStore.getState().setTopK;
    originalSetContextWindow = useConfigStore.getState().setContextWindow;
    originalSetMaxTokens = useConfigStore.getState().setMaxTokens;

    // Reset store state to defaults
    useConfigStore.setState({
      model: null,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
      setTemperature: mockSetTemperature,
      setTopP: mockSetTopP,
      setTopK: mockSetTopK,
      setContextWindow: mockSetContextWindow,
      setMaxTokens: mockSetMaxTokens,
    });

    // Default mock for useSystemPrompts
    mockUseSystemPrompts.mockReturnValue({
      systemPrompts: [],
      systemPrompt: null,
      isLoading: false,
      error: null,
      fetchSystemPrompts: mockFetchSystemPrompts,
      fetchSystemPrompt: vi.fn(),
      createSystemPrompt: vi.fn(),
      updateSystemPrompt: vi.fn(),
      deleteSystemPrompt: vi.fn(),
      clearError: vi.fn(),
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
    // Restore original store state and actions
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
      setTemperature: originalSetTemperature,
      setTopP: originalSetTopP,
      setTopK: originalSetTopK,
      setContextWindow: originalSetContextWindow,
      setMaxTokens: originalSetMaxTokens,
    } as Partial<ConfigState & ConfigActions>);
  });

  describe('rendering', () => {
    it('renders the panel with trigger button', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel')).toBeInTheDocument();
      expect(screen.getByTestId('config-panel-trigger')).toBeInTheDocument();
      expect(screen.getByText('Configuration')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      render(<ConfigPanel className="custom-class" />);

      const panel = screen.getByTestId('config-panel');
      expect(panel).toHaveClass('custom-class');
    });

    it('uses custom testId', () => {
      render(<ConfigPanel data-testid="custom-panel" />);

      expect(screen.getByTestId('custom-panel')).toBeInTheDocument();
      expect(screen.getByTestId('custom-panel-trigger')).toBeInTheDocument();
    });

    it('starts expanded by default', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-content')).toBeInTheDocument();
    });

    it('starts collapsed when defaultCollapsed is true', () => {
      render(<ConfigPanel defaultCollapsed />);

      const content = screen.getByTestId('config-panel-content');
      expect(content).toHaveAttribute('hidden');
    });
  });

  describe('collapsible behaviour', () => {
    it('toggles content visibility when trigger is clicked', async () => {
      render(<ConfigPanel />);

      const content = screen.getByTestId('config-panel-content');
      // Initially expanded (no hidden attribute)
      expect(content).not.toHaveAttribute('hidden');

      // Click to collapse
      const trigger = screen.getByTestId('config-panel-trigger');
      fireEvent.click(trigger);

      await waitFor(() => {
        expect(screen.getByTestId('config-panel-content')).toHaveAttribute('hidden');
      });

      // Click to expand again
      fireEvent.click(trigger);

      await waitFor(() => {
        expect(screen.getByTestId('config-panel-content')).not.toHaveAttribute('hidden');
      });
    });

    it('has correct aria-expanded attribute', () => {
      render(<ConfigPanel />);

      const trigger = screen.getByTestId('config-panel-trigger');
      expect(trigger).toHaveAttribute('aria-expanded', 'true');

      fireEvent.click(trigger);
      expect(trigger).toHaveAttribute('aria-expanded', 'false');
    });
  });

  describe('model selector integration', () => {
    it('renders ModelSelector component', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-model-selector')).toBeInTheDocument();
    });
  });

  describe('temperature slider', () => {
    it('renders temperature slider with label and value', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-temperature')).toBeInTheDocument();
      expect(screen.getByText('Temperature')).toBeInTheDocument();
      expect(screen.getByText('0.7')).toBeInTheDocument();
    });

    it('displays helper text for temperature', () => {
      render(<ConfigPanel />);

      expect(
        screen.getByText('Controls randomness. Lower is more focused, higher is more creative.')
      ).toBeInTheDocument();
    });
  });

  describe('top P slider', () => {
    it('renders top P slider with label and value', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-top-p')).toBeInTheDocument();
      expect(screen.getByText('Top P')).toBeInTheDocument();
      expect(screen.getByText('0.90')).toBeInTheDocument();
    });

    it('displays helper text for top P', () => {
      render(<ConfigPanel />);

      expect(screen.getByText('Nucleus sampling probability threshold.')).toBeInTheDocument();
    });
  });

  describe('top K input', () => {
    it('renders top K input with label and value', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-top-k')).toBeInTheDocument();
      const input = screen.getByTestId('config-panel-top-k-input');
      expect(input).toHaveValue(40);
    });

    it('calls setTopK when value changes', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-top-k-input');
      fireEvent.change(input, { target: { value: '60' } });

      expect(mockSetTopK).toHaveBeenCalledWith(60);
    });

    it('does not call setTopK for non-numeric input', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-top-k-input');
      fireEvent.change(input, { target: { value: 'abc' } });

      expect(mockSetTopK).not.toHaveBeenCalled();
    });

    it('passes values to store which handles clamping', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-top-k-input');
      // Store handles clamping, so we just verify the value is passed
      fireEvent.change(input, { target: { value: '150' } });
      expect(mockSetTopK).toHaveBeenCalledWith(150);
    });

    it('displays helper text for top K', () => {
      render(<ConfigPanel />);

      expect(screen.getByText('Limits sampling to top K tokens (1-100).')).toBeInTheDocument();
    });
  });

  describe('context window input', () => {
    it('renders context window input with label and value', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-context-window')).toBeInTheDocument();
      const input = screen.getByTestId('config-panel-context-window-input');
      expect(input).toHaveValue(4096);
    });

    it('calls setContextWindow when value changes', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-context-window-input');
      fireEvent.change(input, { target: { value: '8192' } });

      expect(mockSetContextWindow).toHaveBeenCalledWith(8192);
    });

    it('does not call setContextWindow for non-numeric input', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-context-window-input');
      fireEvent.change(input, { target: { value: 'abc' } });

      expect(mockSetContextWindow).not.toHaveBeenCalled();
    });

    it('passes values to store which handles clamping', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-context-window-input');
      // Store handles clamping, so we just verify the value is passed
      fireEvent.change(input, { target: { value: '200000' } });
      expect(mockSetContextWindow).toHaveBeenCalledWith(200000);
    });

    it('displays helper text for context window', () => {
      render(<ConfigPanel />);

      expect(screen.getByText('Context window size in tokens (512-128000).')).toBeInTheDocument();
    });
  });

  describe('max tokens input', () => {
    it('renders max tokens input with label', () => {
      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-max-tokens')).toBeInTheDocument();
      const input = screen.getByTestId('config-panel-max-tokens-input');
      expect(input).toHaveValue(null);
      expect(input).toHaveAttribute('placeholder', 'No limit');
    });

    it('calls setMaxTokens when value changes', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-max-tokens-input');
      fireEvent.change(input, { target: { value: '1000' } });

      expect(mockSetMaxTokens).toHaveBeenCalledWith(1000);
    });

    it('does not call setMaxTokens for non-numeric input', () => {
      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-max-tokens-input');
      fireEvent.change(input, { target: { value: 'abc' } });

      // Should not call setMaxTokens for non-numeric input (NaN check)
      expect(mockSetMaxTokens).not.toHaveBeenCalled();
    });

    it('calls setMaxTokens with null when input is cleared', () => {
      // First set a value
      useConfigStore.setState({
        hyperparameters: { ...DEFAULT_HYPERPARAMETERS, maxTokens: 500 },
      });

      render(<ConfigPanel />);

      const input = screen.getByTestId('config-panel-max-tokens-input');
      fireEvent.change(input, { target: { value: '' } });

      expect(mockSetMaxTokens).toHaveBeenCalledWith(null);
    });

    it('displays helper text for max tokens', () => {
      render(<ConfigPanel />);

      expect(
        screen.getByText('Maximum tokens to generate. Leave empty for no limit.')
      ).toBeInTheDocument();
    });
  });

  describe('system prompt section', () => {
    it('fetches system prompts on mount', () => {
      render(<ConfigPanel />);

      expect(mockFetchSystemPrompts).toHaveBeenCalledTimes(1);
    });

    it('displays loading state when fetching prompts', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [],
        systemPrompt: null,
        isLoading: true,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel />);

      expect(screen.getByTestId('config-panel-system-prompt-loading')).toBeInTheDocument();
      expect(screen.getByText('Loading prompts...')).toBeInTheDocument();
    });

    it('renders system prompt select when loaded', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [
          { id: 1, alias: 'code-assistant', content: 'You are a code assistant.', createdAt: '' },
          { id: 2, alias: 'writer', content: 'You are a writer.', createdAt: '' },
        ],
        systemPrompt: null,
        isLoading: false,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel />);

      const select = screen.getByTestId('config-panel-system-prompt-select');
      expect(select).toBeInTheDocument();

      // Check options
      expect(screen.getByText('No system prompt')).toBeInTheDocument();
      expect(screen.getByText('code-assistant')).toBeInTheDocument();
      expect(screen.getByText('writer')).toBeInTheDocument();
      expect(screen.getByText('Custom prompt...')).toBeInTheDocument();
    });

    it('shows custom prompt textarea when custom option is selected', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [{ id: 1, alias: 'test-prompt', content: 'Test content', createdAt: '' }],
        systemPrompt: null,
        isLoading: false,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel />);

      const select = screen.getByTestId('config-panel-system-prompt-select');
      fireEvent.change(select, { target: { value: 'custom' } });

      expect(screen.getByTestId('config-panel-custom-prompt')).toBeInTheDocument();
      expect(screen.getByTestId('config-panel-custom-prompt-textarea')).toBeInTheDocument();
    });

    it('shows prompt preview when a preset is selected', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [
          {
            id: 1,
            alias: 'code-assistant',
            content: 'You are a helpful code assistant.',
            createdAt: '',
          },
        ],
        systemPrompt: null,
        isLoading: false,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel />);

      const select = screen.getByTestId('config-panel-system-prompt-select');
      fireEvent.change(select, { target: { value: '1' } });

      expect(screen.getByTestId('config-panel-prompt-preview')).toBeInTheDocument();
      expect(screen.getByText('You are a helpful code assistant.')).toBeInTheDocument();
    });

    it('calls onSystemPromptChange when preset is selected', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [
          { id: 1, alias: 'code-assistant', content: 'You are a code assistant.', createdAt: '' },
        ],
        systemPrompt: null,
        isLoading: false,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel onSystemPromptChange={mockOnSystemPromptChange} />);

      const select = screen.getByTestId('config-panel-system-prompt-select');
      fireEvent.change(select, { target: { value: '1' } });

      expect(mockOnSystemPromptChange).toHaveBeenCalledWith(1, 'You are a code assistant.');
    });

    it('calls onSystemPromptChange with null when none is selected', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [
          { id: 1, alias: 'code-assistant', content: 'You are a code assistant.', createdAt: '' },
        ],
        systemPrompt: null,
        isLoading: false,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel onSystemPromptChange={mockOnSystemPromptChange} />);

      const select = screen.getByTestId('config-panel-system-prompt-select');

      // First select a preset
      fireEvent.change(select, { target: { value: '1' } });

      // Then select none
      fireEvent.change(select, { target: { value: 'none' } });

      expect(mockOnSystemPromptChange).toHaveBeenLastCalledWith(null, '');
    });

    it('calls onSystemPromptChange with custom prompt content', () => {
      mockUseSystemPrompts.mockReturnValue({
        systemPrompts: [],
        systemPrompt: null,
        isLoading: false,
        error: null,
        fetchSystemPrompts: mockFetchSystemPrompts,
        fetchSystemPrompt: vi.fn(),
        createSystemPrompt: vi.fn(),
        updateSystemPrompt: vi.fn(),
        deleteSystemPrompt: vi.fn(),
        clearError: vi.fn(),
      });

      render(<ConfigPanel onSystemPromptChange={mockOnSystemPromptChange} />);

      const select = screen.getByTestId('config-panel-system-prompt-select');
      fireEvent.change(select, { target: { value: 'custom' } });

      const textarea = screen.getByTestId('config-panel-custom-prompt-textarea');
      fireEvent.change(textarea, { target: { value: 'My custom prompt' } });

      expect(mockOnSystemPromptChange).toHaveBeenLastCalledWith(null, 'My custom prompt');
    });
  });

  describe('section headers', () => {
    it('displays Hyperparameters section header', () => {
      render(<ConfigPanel />);

      expect(screen.getByText('Hyperparameters')).toBeInTheDocument();
    });

    it('displays System Prompt section header', () => {
      render(<ConfigPanel />);

      expect(screen.getByText('System Prompt')).toBeInTheDocument();
    });
  });
});
