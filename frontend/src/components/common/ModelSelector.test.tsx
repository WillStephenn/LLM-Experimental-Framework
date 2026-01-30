import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ModelSelector } from './ModelSelector';
import { useOllama } from '@/hooks/useOllama';
import { useConfigStore, DEFAULT_CONFIG_STATE, DEFAULT_HYPERPARAMETERS } from '@/store/configStore';
import type { ConfigActions, ConfigState } from '@/store/configStore';

// Mock the useOllama hook
vi.mock('@/hooks/useOllama', () => ({
  useOllama: vi.fn(() => ({
    models: [],
    isLoading: false,
    error: null,
    isAvailable: true,
    refetch: vi.fn(),
  })),
}));

const mockUseOllama = vi.mocked(useOllama);

// Store original actions for restoration
let originalSetModel: ConfigActions['setModel'];

describe('ModelSelector', () => {
  const mockRefetch = vi.fn();
  const mockSetModel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    // Save original setModel before overwriting
    originalSetModel = useConfigStore.getState().setModel;
    // Reset the config store state
    useConfigStore.setState({ model: null });
    // Override setModel to use mock
    useConfigStore.setState({
      setModel: mockSetModel,
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
    // Restore original store state and actions
    useConfigStore.setState({
      ...DEFAULT_CONFIG_STATE,
      hyperparameters: { ...DEFAULT_HYPERPARAMETERS },
      setModel: originalSetModel,
    } as Partial<ConfigState & ConfigActions>);
  });

  describe('loading state', () => {
    it('displays loading spinner while fetching models', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: true,
        error: null,
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      expect(screen.getByTestId('model-selector-loading')).toBeInTheDocument();
      expect(screen.getByText('Loading models...')).toBeInTheDocument();
      expect(screen.getByTestId('model-selector-spinner')).toBeInTheDocument();
    });

    it('displays custom label in loading state', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: true,
        error: null,
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector label="Choose Model" />);

      expect(screen.getByText('Choose Model')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('displays error message when Ollama is unavailable', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: false,
        error: new Error('Cannot connect to Ollama service'),
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      expect(screen.getByTestId('model-selector-error')).toBeInTheDocument();
      expect(screen.getByText('Cannot connect to Ollama service')).toBeInTheDocument();
    });

    it('displays default error message when error is null but not available', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: false,
        error: null,
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      expect(screen.getByTestId('model-selector-error')).toBeInTheDocument();
      expect(screen.getByText('Ollama service unavailable')).toBeInTheDocument();
    });

    it('displays retry button in error state', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: false,
        error: new Error('Connection failed'),
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      const retryButton = screen.getByTestId('model-selector-retry');
      expect(retryButton).toBeInTheDocument();
      expect(retryButton).toHaveTextContent('Retry');
    });

    it('calls refetch when retry button is clicked', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: false,
        error: new Error('Connection failed'),
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      const retryButton = screen.getByTestId('model-selector-retry');
      fireEvent.click(retryButton);

      expect(mockRefetch).toHaveBeenCalledTimes(1);
    });
  });

  describe('empty state', () => {
    it('displays message when no models are available', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: false,
        error: null,
        isAvailable: true,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      expect(screen.getByTestId('model-selector-empty')).toBeInTheDocument();
      expect(screen.getByText('No models available in Ollama')).toBeInTheDocument();
    });
  });

  describe('normal state with models', () => {
    const mockModels = ['llama3:8b', 'codellama:7b', 'mistral:7b'];

    beforeEach(() => {
      mockUseOllama.mockReturnValue({
        models: mockModels,
        isLoading: false,
        error: null,
        isAvailable: true,
        refetch: mockRefetch,
      });
    });

    it('renders the select trigger', () => {
      render(<ModelSelector />);

      expect(screen.getByTestId('model-selector-trigger')).toBeInTheDocument();
    });

    it('displays placeholder when no model is selected', () => {
      render(<ModelSelector />);

      expect(screen.getByText('Select a model...')).toBeInTheDocument();
    });

    it('displays label when provided', () => {
      render(<ModelSelector label="Select Model" />);

      expect(screen.getByText('Select Model')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      render(<ModelSelector className="custom-class" />);

      const container = screen.getByTestId('model-selector');
      expect(container).toHaveClass('custom-class');
    });

    it('uses custom testId', () => {
      render(<ModelSelector data-testid="custom-selector" />);

      expect(screen.getByTestId('custom-selector')).toBeInTheDocument();
    });

    it('opens dropdown when trigger is clicked', async () => {
      render(<ModelSelector />);

      const trigger = screen.getByTestId('model-selector-trigger');
      fireEvent.click(trigger);

      await waitFor(() => {
        expect(screen.getByTestId('model-selector-content')).toBeInTheDocument();
      });
    });

    it('displays all models in dropdown', async () => {
      render(<ModelSelector />);

      const trigger = screen.getByTestId('model-selector-trigger');
      fireEvent.click(trigger);

      await waitFor(() => {
        mockModels.forEach((model) => {
          expect(screen.getByText(model)).toBeInTheDocument();
        });
      });
    });

    it('calls setModel when a model is selected', async () => {
      render(<ModelSelector />);

      const trigger = screen.getByTestId('model-selector-trigger');
      fireEvent.click(trigger);

      await waitFor(() => {
        expect(screen.getByTestId('model-selector-content')).toBeInTheDocument();
      });

      const modelOption = screen.getByTestId('model-selector-option-llama3:8b');
      fireEvent.click(modelOption);

      expect(mockSetModel).toHaveBeenCalledWith('llama3:8b');
    });

    it('displays currently selected model from store', () => {
      useConfigStore.setState({ model: 'codellama:7b' });

      render(<ModelSelector />);

      expect(screen.getByText('codellama:7b')).toBeInTheDocument();
    });
  });

  describe('disabled state', () => {
    beforeEach(() => {
      mockUseOllama.mockReturnValue({
        models: ['llama3:8b', 'codellama:7b'],
        isLoading: false,
        error: null,
        isAvailable: true,
        refetch: mockRefetch,
      });
    });

    it('disables the select when disabled prop is true', () => {
      render(<ModelSelector disabled />);

      const trigger = screen.getByTestId('model-selector-trigger');
      expect(trigger).toBeDisabled();
    });
  });

  describe('accessibility', () => {
    beforeEach(() => {
      mockUseOllama.mockReturnValue({
        models: ['llama3:8b', 'codellama:7b'],
        isLoading: false,
        error: null,
        isAvailable: true,
        refetch: mockRefetch,
      });
    });

    it('has accessible label for retry button', () => {
      mockUseOllama.mockReturnValue({
        models: [],
        isLoading: false,
        error: new Error('Error'),
        isAvailable: false,
        refetch: mockRefetch,
      });

      render(<ModelSelector />);

      const retryButton = screen.getByTestId('model-selector-retry');
      expect(retryButton).toHaveAttribute('aria-label', 'Retry loading models');
    });

    it('associates label with select', () => {
      render(<ModelSelector label="Model Selection" />);

      const trigger = screen.getByTestId('model-selector-trigger');
      expect(trigger).toHaveAttribute('aria-labelledby', 'model-selector-label');
    });
  });
});
