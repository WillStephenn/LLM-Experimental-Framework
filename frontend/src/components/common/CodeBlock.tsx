/**
 * CodeBlock Component
 *
 * Displays code with syntax highlighting, optional line numbers, and copy-to-clipboard functionality.
 * Follows the Academic Modern design system.
 */
import { useState, useCallback } from 'react';
import { Highlight, themes } from 'prism-react-renderer';

/**
 * Supported languages for syntax highlighting
 */
export type SupportedLanguage =
  | 'javascript'
  | 'typescript'
  | 'java'
  | 'json'
  | 'python'
  | 'bash'
  | 'shell'
  | 'css'
  | 'html'
  | 'markdown'
  | 'yaml'
  | 'sql'
  | 'plaintext';

/**
 * Props for the CodeBlock component
 */
export interface CodeBlockProps {
  /** The code to display */
  code: string;
  /** Programming language for syntax highlighting */
  language?: SupportedLanguage;
  /** Whether to show line numbers */
  showLineNumbers?: boolean;
  /** Optional title for the code block */
  title?: string;
  /** Test ID for testing */
  'data-testid'?: string;
}

/**
 * Language display names for the header
 */
const languageDisplayNames: Record<SupportedLanguage, string> = {
  javascript: 'JavaScript',
  typescript: 'TypeScript',
  java: 'Java',
  json: 'JSON',
  python: 'Python',
  bash: 'Bash',
  shell: 'Shell',
  css: 'CSS',
  html: 'HTML',
  markdown: 'Markdown',
  yaml: 'YAML',
  sql: 'SQL',
  plaintext: 'Plain Text',
};

/**
 * Map language aliases to Prism-supported language names
 */
function mapLanguageToPrism(language: SupportedLanguage): string {
  const languageMap: Record<string, string> = {
    shell: 'bash',
    plaintext: 'markup',
  };
  return languageMap[language] ?? language;
}

/**
 * CopyIcon component for the copy button
 */
function CopyIcon({ className }: { className?: string }): React.JSX.Element {
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
        d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
      />
    </svg>
  );
}

/**
 * CheckIcon component for the copied state
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
 * CodeBlock component for displaying syntax-highlighted code
 *
 * @param props - Component props
 * @returns JSX element
 */
export function CodeBlock({
  code,
  language = 'plaintext',
  showLineNumbers = false,
  title,
  'data-testid': testId = 'code-block',
}: CodeBlockProps): React.JSX.Element {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback((): void => {
    const performCopy = async (): Promise<void> => {
      try {
        await navigator.clipboard.writeText(code);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch {
        // Fallback for browsers that don't support clipboard API
        const textArea = document.createElement('textarea');
        textArea.value = code;
        textArea.style.position = 'fixed';
        textArea.style.left = '-9999px';
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }
    };
    void performCopy();
  }, [code]);

  const prismLanguage = mapLanguageToPrism(language);
  const displayLanguage = languageDisplayNames[language];

  return (
    <div
      className="rounded-lg border border-gray-200 bg-gray-50 overflow-hidden font-mono text-sm"
      data-testid={testId}
    >
      {/* Header with title/language and copy button */}
      <div className="flex items-center justify-between px-4 py-2 bg-gray-100 border-b border-gray-200">
        <span className="text-xs font-medium text-gray-600" data-testid={`${testId}-language`}>
          {title ?? displayLanguage}
        </span>
        <button
          type="button"
          onClick={handleCopy}
          className="flex items-center gap-1.5 px-2 py-1 text-xs font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition-colors duration-150"
          aria-label={copied ? 'Copied to clipboard' : 'Copy code to clipboard'}
          data-testid={`${testId}-copy-button`}
        >
          {copied ? (
            <>
              <CheckIcon className="w-4 h-4 text-success" />
              <span className="text-success">Copied</span>
            </>
          ) : (
            <>
              <CopyIcon className="w-4 h-4" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>

      {/* Code content with syntax highlighting */}
      <div className="overflow-x-auto">
        <Highlight theme={themes.github} code={code.trim()} language={prismLanguage}>
          {({ className, style, tokens, getLineProps, getTokenProps }) => (
            <pre
              className={`${className} p-4 m-0 bg-transparent`}
              style={{ ...style, background: 'transparent' }}
              data-testid={`${testId}-pre`}
            >
              {tokens.map((line, lineIndex) => {
                const lineProps = getLineProps({ line, key: lineIndex });
                return (
                  <div
                    key={lineIndex}
                    {...lineProps}
                    className={`${lineProps.className ?? ''} table-row`}
                  >
                    {showLineNumbers && (
                      <span
                        className="table-cell pr-4 text-right text-gray-400 select-none w-8"
                        data-testid={`${testId}-line-number-${lineIndex + 1}`}
                      >
                        {lineIndex + 1}
                      </span>
                    )}
                    <span className="table-cell">
                      {line.map((token, tokenIndex) => {
                        const tokenProps = getTokenProps({ token, key: tokenIndex });
                        return <span key={tokenIndex} {...tokenProps} />;
                      })}
                    </span>
                  </div>
                );
              })}
            </pre>
          )}
        </Highlight>
      </div>
    </div>
  );
}
