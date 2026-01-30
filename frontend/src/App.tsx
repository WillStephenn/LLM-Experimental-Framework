/**
 * LocalLab - Main Application Component
 *
 * Root component that sets up routing and global layout.
 */
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from '@/components/layout';
import {
  SandboxPage,
  ArenaPage,
  RagLabPage,
  TaskLibraryPage,
  ExperimentBuilderPage,
  ExperimentRunnerPage,
  ResultsDashboardPage,
  EmbeddingAnalyserPage,
} from '@/pages';

/**
 * Main application component with routing configuration
 */
function App(): React.JSX.Element {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          {/* Default redirect to sandbox */}
          <Route index element={<Navigate to="/sandbox" replace />} />

          {/* Main application routes */}
          <Route path="sandbox" element={<SandboxPage />} />
          <Route path="arena" element={<ArenaPage />} />
          <Route path="rag" element={<RagLabPage />} />
          <Route path="tasks" element={<TaskLibraryPage />} />
          <Route path="experiments/new" element={<ExperimentBuilderPage />} />
          <Route path="experiments/:id/run" element={<ExperimentRunnerPage />} />
          <Route path="results" element={<ResultsDashboardPage />} />
          <Route path="embeddings" element={<EmbeddingAnalyserPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
