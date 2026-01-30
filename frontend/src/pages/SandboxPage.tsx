/**
 * Sandbox Page
 *
 * Interactive playground for experimenting with models in real-time.
 */
export function SandboxPage(): React.JSX.Element {
  return (
    <div data-testid="sandbox-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Sandbox</h1>
        <p className="text-sm text-gray-600 mt-1">
          Experiment with models in real-time
        </p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Interactive playground coming soon. Test prompts, adjust parameters,
          and explore model behaviour.
        </p>
      </div>
    </div>
  );
}
