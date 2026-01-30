/**
 * ComponentGalleryPage
 *
 * A "secret" page to display all shared components for visual inspection.
 */
import React from 'react';
import { CodeBlock } from '@/components/common/CodeBlock';
import { ConfigPanel } from '@/components/common/ConfigPanel';
import { MetricsDisplay, type GenerationMetrics } from '@/components/common/MetricsDisplay';
import { ModelSelector } from '@/components/common/ModelSelector';

/**
 * Section component for grouping component examples
 */
function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}): React.JSX.Element {
  return (
    <section className="mb-12">
      <h2 className="text-2xl font-bold text-gray-800 mb-6 pb-2 border-b border-gray-200">
        {title}
      </h2>
      <div className="space-y-8">{children}</div>
    </section>
  );
}

/**
 * Example wrapper to show component context
 */
function Example({
  title,
  children,
  description,
}: {
  title: string;
  children: React.ReactNode;
  description?: string;
}): React.JSX.Element {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
      <h3 className="text-lg font-semibold text-gray-700 mb-2">{title}</h3>
      {description && <p className="text-gray-500 text-sm mb-4">{description}</p>}
      <div className="p-4 bg-gray-50 rounded border border-gray-100 border-dashed">{children}</div>
    </div>
  );
}

export function ComponentGalleryPage(): React.JSX.Element {
  // Dummy metrics data
  const dummyMetrics: GenerationMetrics = {
    durationMs: 1250,
    tokensPerSecond: 45.5,
    timeToFirstTokenMs: 150,
    promptTokens: 120,
    completionTokens: 350,
  };

  const emptyMetrics: GenerationMetrics = {
    durationMs: null,
    tokensPerSecond: null,
    timeToFirstTokenMs: null,
    promptTokens: null,
    completionTokens: null,
  };

  // Example code for CodeBlock
  const exampleCodeTS = `interface User {
  id: number;
  name: string;
  role: 'admin' | 'user';
}

function greet(user: User): string {
  return \`Hello, \${user.name} (\${user.role})\`;
}`;

  const exampleCodePython = `def calculate_fibonacci(n):
    if n <= 1:
        return n
    else:
        return calculate_fibonacci(n-1) + calculate_fibonacci(n-2)

print(calculate_fibonacci(10))`;

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      <div className="mb-10">
        <h1 className="text-3xl font-bold text-gray-900">Component Gallery</h1>
        <p className="text-gray-600 mt-2">
          Visual inspection of shared components in the LocalLab design system.
        </p>
      </div>

      {/* CodeBlock Examples */}
      <Section title="CodeBlock">
        <Example
          title="TypeScript with Line Numbers"
          description="Standard code block with line numbers enabled."
        >
          <CodeBlock
            code={exampleCodeTS}
            language="typescript"
            showLineNumbers={true}
            title="types.ts"
          />
        </Example>

        <Example title="Python Simple" description="Code block without line numbers.">
          <CodeBlock code={exampleCodePython} language="python" />
        </Example>
      </Section>

      {/* MetricsDisplay Examples */}
      <Section title="MetricsDisplay">
        <Example title="Compact Mode" description="Shows key metrics in a single line.">
          <MetricsDisplay metrics={dummyMetrics} mode="compact" />
        </Example>

        <Example title="Expanded Mode" description="Shows all metrics including token details.">
          <MetricsDisplay metrics={dummyMetrics} mode="expanded" />
        </Example>

        <Example title="Non-Collapsible" description="Fixed display mode.">
          <MetricsDisplay metrics={dummyMetrics} collapsible={false} />
        </Example>

        <Example title="No Data" description="Display when metrics are null.">
          <MetricsDisplay metrics={emptyMetrics} />
        </Example>
      </Section>

      {/* ModelSelector Examples */}
      <Section title="ModelSelector">
        <Example
          title="Default State"
          description="Interactive model selector (connected to store)."
        >
          <div className="max-w-md">
            <ModelSelector />
          </div>
        </Example>

        <Example title="Disabled State" description="Selector in disabled state.">
          <div className="max-w-md">
            <ModelSelector label="Target Model" disabled={true} />
          </div>
        </Example>
      </Section>

      {/* ConfigPanel Examples */}
      <Section title="ConfigPanel">
        <Example
          title="Default Configuration Panel"
          description="Full configuration panel with all settings."
        >
          <div className="max-w-md">
            <ConfigPanel />
          </div>
        </Example>

        <Example title="Collapsed by Default" description="Panel starts in collapsed state.">
          <div className="max-w-md">
            <ConfigPanel defaultCollapsed={true} />
          </div>
        </Example>
      </Section>
    </div>
  );
}
