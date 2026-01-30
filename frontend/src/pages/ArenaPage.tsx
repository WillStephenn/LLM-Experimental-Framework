/**
 * Arena Page
 *
 * Side-by-side model comparison interface.
 */
export function ArenaPage(): React.JSX.Element {
  return (
    <div data-testid="arena-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Arena</h1>
        <p className="text-sm text-gray-600 mt-1">Compare models side-by-side</p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Model comparison arena coming soon. Run the same prompt across
          multiple models and compare outputs.
        </p>
      </div>
    </div>
  );
}
