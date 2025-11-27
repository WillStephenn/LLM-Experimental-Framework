/**
 * LocalLab - Main Application Component
 *
 * Root component that sets up routing and global layout.
 */
function App() {
  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-4xl font-bold text-center mb-8">LocalLab</h1>
        <p className="text-center text-gray-400 mb-8">
          Local-first LLM experimental framework for benchmarking and comparing models
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-gray-800 rounded-lg p-6 hover:bg-gray-700 transition-colors">
            <h2 className="text-xl font-semibold mb-2">🧪 Sandbox</h2>
            <p className="text-gray-400 text-sm">Experiment with models in real-time</p>
          </div>
          <div className="bg-gray-800 rounded-lg p-6 hover:bg-gray-700 transition-colors">
            <h2 className="text-xl font-semibold mb-2">⚔️ Arena</h2>
            <p className="text-gray-400 text-sm">Compare models side-by-side</p>
          </div>
          <div className="bg-gray-800 rounded-lg p-6 hover:bg-gray-700 transition-colors">
            <h2 className="text-xl font-semibold mb-2">📚 RAG Lab</h2>
            <p className="text-gray-400 text-sm">Test retrieval-augmented generation</p>
          </div>
          <div className="bg-gray-800 rounded-lg p-6 hover:bg-gray-700 transition-colors">
            <h2 className="text-xl font-semibold mb-2">📊 Analytics</h2>
            <p className="text-gray-400 text-sm">View experiment results</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
