/**
 * RAG Lab Page
 *
 * Retrieval-Augmented Generation testing interface.
 */
export function RagLabPage(): React.JSX.Element {
  return (
    <div data-testid="rag-lab-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">RAG Lab</h1>
        <p className="text-sm text-gray-600 mt-1">Test retrieval-augmented generation</p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          RAG testing interface coming soon. Upload documents, configure chunking, and test
          retrieval quality.
        </p>
      </div>
    </div>
  );
}
