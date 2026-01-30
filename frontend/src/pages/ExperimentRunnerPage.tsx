/**
 * Experiment Runner Page
 *
 * Execute and monitor running experiments.
 */
import { useParams } from 'react-router-dom';

export function ExperimentRunnerPage(): React.JSX.Element {
  const { id } = useParams<{ id: string }>();

  return (
    <div data-testid="experiment-runner-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Experiment Runner</h1>
        <p className="text-sm text-gray-600 mt-1">
          {id ? `Running experiment: ${id}` : 'Execute and monitor experiments'}
        </p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Experiment runner coming soon. Monitor progress, view real-time results, and control
          experiment execution.
        </p>
      </div>
    </div>
  );
}
