import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { CodeBlock } from './CodeBlock';

describe('CodeBlock', () => {
  afterEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();
  });

  describe('rendering', () => {
    it('renders code content', () => {
      render(<CodeBlock code="const x = 1;" language="typescript" />);
      expect(screen.getByText(/const/)).toBeInTheDocument();
    });

    it('renders with default test ID', () => {
      render(<CodeBlock code="test code" />);
      expect(screen.getByTestId('code-block')).toBeInTheDocument();
    });

    it('renders with custom test ID', () => {
      render(<CodeBlock code="test code" data-testid="custom-code" />);
      expect(screen.getByTestId('custom-code')).toBeInTheDocument();
    });

    it('displays language name in header', () => {
      render(<CodeBlock code="const x = 1;" language="typescript" />);
      expect(screen.getByTestId('code-block-language')).toHaveTextContent('TypeScript');
    });

    it('displays title instead of language when provided', () => {
      render(<CodeBlock code="const x = 1;" language="typescript" title="Example Code" />);
      expect(screen.getByTestId('code-block-language')).toHaveTextContent('Example Code');
    });

    it('defaults to plaintext when no language provided', () => {
      render(<CodeBlock code="some text" />);
      expect(screen.getByTestId('code-block-language')).toHaveTextContent('Plain Text');
    });
  });

  describe('syntax highlighting', () => {
    it('renders Java code with syntax highlighting', () => {
      const javaCode = 'public class Main { }';
      render(<CodeBlock code={javaCode} language="java" />);
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });

    it('renders TypeScript code with syntax highlighting', () => {
      const tsCode = 'interface User { name: string; }';
      render(<CodeBlock code={tsCode} language="typescript" />);
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });

    it('renders JSON code with syntax highlighting', () => {
      const jsonCode = '{ "key": "value" }';
      render(<CodeBlock code={jsonCode} language="json" />);
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });
  });

  describe('line numbers', () => {
    it('does not show line numbers by default', () => {
      const code = `line 1
line 2`;
      render(<CodeBlock code={code} language="typescript" />);
      expect(screen.queryByTestId('code-block-line-number-1')).not.toBeInTheDocument();
    });

    it('shows line numbers when showLineNumbers is true', () => {
      const code = `line 1
line 2`;
      render(<CodeBlock code={code} language="typescript" showLineNumbers />);
      expect(screen.getByTestId('code-block-line-number-1')).toHaveTextContent('1');
      expect(screen.getByTestId('code-block-line-number-2')).toHaveTextContent('2');
    });

    it('displays correct line numbers for multi-line code', () => {
      const multiLineCode = `line 1
line 2
line 3
line 4
line 5`;
      render(<CodeBlock code={multiLineCode} language="typescript" showLineNumbers />);

      for (let i = 1; i <= 5; i++) {
        expect(screen.getByTestId(`code-block-line-number-${i}`)).toHaveTextContent(String(i));
      }
    });
  });

  describe('copy to clipboard', () => {
    const mockWriteText = vi.fn().mockResolvedValue(undefined);

    beforeEach(() => {
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: mockWriteText },
        writable: true,
        configurable: true,
      });
    });

    it('renders copy button', () => {
      render(<CodeBlock code="test code" />);
      const copyButton = screen.getByTestId('code-block-copy-button');
      expect(copyButton).toBeInTheDocument();
      expect(copyButton).toHaveTextContent('Copy');
    });

    it('copies code to clipboard when copy button is clicked', async () => {
      const testCode = 'const x = 1;';
      render(<CodeBlock code={testCode} language="typescript" />);

      const copyButton = screen.getByTestId('code-block-copy-button');

      act(() => {
        fireEvent.click(copyButton);
      });

      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalledWith(testCode);
      });
    });

    it('shows "Copied" state after clicking copy', async () => {
      render(<CodeBlock code="test code" />);

      const copyButton = screen.getByTestId('code-block-copy-button');

      act(() => {
        fireEvent.click(copyButton);
      });

      await waitFor(() => {
        expect(copyButton).toHaveTextContent('Copied');
      });
    });

    it('has accessible label for copy button', () => {
      render(<CodeBlock code="test code" />);
      const copyButton = screen.getByTestId('code-block-copy-button');
      expect(copyButton).toHaveAttribute('aria-label', 'Copy code to clipboard');
    });

    it('updates aria-label when copied', async () => {
      render(<CodeBlock code="test code" />);

      const copyButton = screen.getByTestId('code-block-copy-button');

      act(() => {
        fireEvent.click(copyButton);
      });

      await waitFor(() => {
        expect(copyButton).toHaveAttribute('aria-label', 'Copied to clipboard');
      });
    });
  });

  describe('supported languages', () => {
    const languages = [
      { language: 'javascript' as const, display: 'JavaScript' },
      { language: 'typescript' as const, display: 'TypeScript' },
      { language: 'java' as const, display: 'Java' },
      { language: 'json' as const, display: 'JSON' },
      { language: 'python' as const, display: 'Python' },
      { language: 'bash' as const, display: 'Bash' },
      { language: 'shell' as const, display: 'Shell' },
      { language: 'css' as const, display: 'CSS' },
      { language: 'html' as const, display: 'HTML' },
      { language: 'markdown' as const, display: 'Markdown' },
      { language: 'yaml' as const, display: 'YAML' },
      { language: 'sql' as const, display: 'SQL' },
      { language: 'plaintext' as const, display: 'Plain Text' },
    ];

    languages.forEach(({ language, display }) => {
      it(`renders ${language} with correct display name`, () => {
        render(<CodeBlock code="test" language={language} />);
        expect(screen.getByTestId('code-block-language')).toHaveTextContent(display);
      });
    });
  });

  describe('code trimming', () => {
    it('trims whitespace from code', () => {
      render(<CodeBlock code="   const x = 1;   " language="typescript" />);
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('handles empty string code', () => {
      render(<CodeBlock code="" language="typescript" />);
      expect(screen.getByTestId('code-block')).toBeInTheDocument();
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });

    it('handles code with only whitespace', () => {
      render(<CodeBlock code="   \n\n   " language="typescript" />);
      expect(screen.getByTestId('code-block')).toBeInTheDocument();
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });

    it('handles code with special HTML characters', () => {
      const codeWithSpecialChars = '<div class="test">&amp;</div>';
      render(<CodeBlock code={codeWithSpecialChars} language="html" />);
      expect(screen.getByTestId('code-block-pre')).toBeInTheDocument();
    });

    it('handles very long single-line code', () => {
      const longCode = 'const x = ' + '"a"'.repeat(1000);
      render(<CodeBlock code={longCode} language="typescript" />);
      expect(screen.getByTestId('code-block')).toBeInTheDocument();
    });
  });

  describe('clipboard fallback', () => {
    it('uses fallback when clipboard API fails', async () => {
      // Mock clipboard API to reject
      const mockFailingWriteText = vi.fn().mockRejectedValue(new Error('Clipboard not available'));
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: mockFailingWriteText },
        writable: true,
        configurable: true,
      });

      // Mock execCommand
      const mockExecCommand = vi.fn().mockReturnValue(true);
      Object.defineProperty(document, 'execCommand', {
        value: mockExecCommand,
        writable: true,
        configurable: true,
      });

      render(<CodeBlock code="test code" />);
      const copyButton = screen.getByTestId('code-block-copy-button');

      act(() => {
        fireEvent.click(copyButton);
      });

      await waitFor(() => {
        expect(mockExecCommand).toHaveBeenCalledWith('copy');
      });

      await waitFor(() => {
        expect(copyButton).toHaveTextContent('Copied');
      });
    });

    it('does not show copied state when fallback fails', async () => {
      // Mock clipboard API to reject
      const mockFailingWriteText = vi.fn().mockRejectedValue(new Error('Clipboard not available'));
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: mockFailingWriteText },
        writable: true,
        configurable: true,
      });

      // Mock execCommand to return false (failure)
      const mockExecCommand = vi.fn().mockReturnValue(false);
      Object.defineProperty(document, 'execCommand', {
        value: mockExecCommand,
        writable: true,
        configurable: true,
      });

      render(<CodeBlock code="test code" />);
      const copyButton = screen.getByTestId('code-block-copy-button');

      act(() => {
        fireEvent.click(copyButton);
      });

      // Wait for the async operation to complete
      await waitFor(
        () => {
          expect(mockExecCommand).toHaveBeenCalledWith('copy');
        },
        { timeout: 1000 }
      );

      // Button should still show "Copy" since fallback failed
      expect(copyButton).toHaveTextContent('Copy');
    });
  });
});
