/**
 * Results Dashboard Page
 *
 * View and analyse experiment results.
 */
export function ResultsDashboardPage(): React.JSX.Element {
  return (
    <div data-testid="results-dashboard-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">
          Results Dashboard
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          View and analyse experiment results
        </p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Results dashboard coming soon. Visualise metrics, compare runs, and
          export analysis reports.
        </p>
      </div>
    </div>
  );
}
