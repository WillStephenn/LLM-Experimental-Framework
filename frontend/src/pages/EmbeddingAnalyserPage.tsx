/**
 * Embedding Analyser Page
 *
 * Analyse and visualise embeddings.
 */
export function EmbeddingAnalyserPage(): React.JSX.Element {
  return (
    <div data-testid="embedding-analyser-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">
          Embedding Analyser
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          Analyse and visualise embeddings
        </p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Embedding analyser coming soon. Explore vector spaces, measure
          similarity, and debug retrieval quality.
        </p>
      </div>
    </div>
  );
}
