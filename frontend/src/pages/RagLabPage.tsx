/**
 * RAG Lab Page
 *
 * Retrieval-Augmented Generation testing interface.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDocuments, useEmbeddingModels } from '@/hooks';
import { useRagConfigStore } from '@/store/ragConfigStore';
import type { DocumentResponse } from '@/types';

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const ACCEPTED_TYPES = ['application/pdf', 'text/plain'];

const formatFileError = (file: File): string | null => {
  if (!ACCEPTED_TYPES.includes(file.type)) {
    return 'Only PDF or TXT files are supported.';
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return 'File must be 10MB or smaller.';
  }
  return null;
};

export function RagLabPage(): React.JSX.Element {
  const {
    documents,
    isLoading: documentsLoading,
    error: documentsError,
    fetchDocuments,
    uploadDocument,
    deleteDocument,
  } = useDocuments();
  const {
    embeddingModels,
    isLoading: embeddingModelsLoading,
    error: embeddingModelsError,
    fetchEmbeddingModels,
  } = useEmbeddingModels();
  const {
    embeddingModel,
    chunkSize,
    chunkOverlap,
    setEmbeddingModel,
    setChunkSize,
    setChunkOverlap,
  } = useRagConfigStore();
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    void fetchDocuments();
    void fetchEmbeddingModels();
  }, [fetchDocuments, fetchEmbeddingModels]);

  useEffect(() => {
    if (!embeddingModel && embeddingModels.length > 0) {
      setEmbeddingModel(embeddingModels[0].ollamaModelName);
    }
  }, [embeddingModel, embeddingModels, setEmbeddingModel]);

  const handleFileUpload = useCallback(
    async (file: File): Promise<void> => {
      const validationError = formatFileError(file);
      if (validationError) {
        setUploadError(validationError);
        return;
      }
      setUploadError(null);
      try {
        await uploadDocument(file);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Document upload failed.';
        setUploadError(message);
      }
    },
    [uploadDocument]
  );

  const handleDrop = useCallback(
    (event: React.DragEvent<HTMLDivElement>): void => {
      event.preventDefault();
      setIsDragging(false);
      const [file] = Array.from(event.dataTransfer.files);
      if (file) {
        void handleFileUpload(file);
      }
    },
    [handleFileUpload]
  );

  const handleDragOver = useCallback((event: React.DragEvent<HTMLDivElement>): void => {
    event.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((event: React.DragEvent<HTMLDivElement>): void => {
    const relatedTarget = event.relatedTarget as Node | null;
    if (relatedTarget && event.currentTarget.contains(relatedTarget)) {
      return;
    }
    setIsDragging(false);
  }, []);

  const handleFileInputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>): void => {
      const [file] = Array.from(event.target.files ?? []);
      if (file) {
        void handleFileUpload(file);
      }
      event.target.value = '';
    },
    [handleFileUpload]
  );

  const handleDelete = useCallback(
    async (document: DocumentResponse): Promise<void> => {
      setUploadError(null);
      try {
        await deleteDocument(document.id);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Failed to delete document.';
        setUploadError(message);
      }
    },
    [deleteDocument]
  );

  const handleRefreshDocuments = useCallback(() => {
    void fetchDocuments();
  }, [fetchDocuments]);

  const uploadStatus = useMemo(() => {
    if (documentsLoading) {
      return 'Uploading or fetching documents...';
    }
    if (documentsError) {
      return documentsError.message;
    }
    return null;
  }, [documentsError, documentsLoading]);

  return (
    <div className="space-y-6" data-testid="rag-lab-page">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">RAG Lab</h1>
        <p className="text-sm text-gray-600">
          Upload documents, configure chunking, and manage embeddings for retrieval tests.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="space-y-6">
          <div className="bg-white rounded-lg shadow p-6 space-y-4" data-testid="rag-upload-card">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Document Ingestion</h2>
              <p className="text-sm text-gray-600">
                Drag and drop PDF or TXT files to ingest them into the RAG pipeline.
              </p>
            </div>
            <div
              className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors duration-200 ${
                isDragging ? 'border-brand-green bg-gray-50' : 'border-gray-200 bg-white'
              }`}
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              data-testid="rag-upload-dropzone"
            >
              <input
                id="rag-file-input"
                type="file"
                accept=".pdf,.txt"
                className="hidden"
                onChange={handleFileInputChange}
                data-testid="rag-upload-input"
              />
              <label
                htmlFor="rag-file-input"
                className="flex flex-col items-center gap-2 cursor-pointer"
              >
                <span className="text-sm font-medium text-gray-900">Upload a document</span>
                <span className="text-xs text-gray-500">
                  PDF or TXT up to 10MB. Drag files here or click to browse.
                </span>
              </label>
            </div>
            {uploadError && (
              <p className="text-sm text-error" data-testid="rag-upload-error">
                {uploadError}
              </p>
            )}
            {uploadStatus && !uploadError && (
              <p
                className={`text-xs ${documentsError ? 'text-error' : 'text-gray-500'}`}
                data-testid="rag-upload-status"
              >
                {uploadStatus}
              </p>
            )}
          </div>

          <div className="bg-white rounded-lg shadow p-6" data-testid="rag-documents-card">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">Documents</h2>
                <p className="text-sm text-gray-600">Manage ingested documents and chunks.</p>
              </div>
              <button
                type="button"
                onClick={handleRefreshDocuments}
                className="text-sm font-medium text-gray-600 hover:text-gray-900"
                aria-label="Refresh documents list"
                data-testid="rag-refresh-documents"
              >
                Refresh
              </button>
            </div>
            {documents.length === 0 ? (
              <p className="text-sm text-gray-500" data-testid="rag-documents-empty">
                No documents uploaded yet.
              </p>
            ) : (
              <div className="space-y-3" data-testid="rag-documents-list">
                {documents.map((document) => (
                  <div
                    key={document.id}
                    className="flex items-center justify-between rounded-md border border-gray-200 px-4 py-3"
                    data-testid={`rag-document-${document.id}`}
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-900">{document.filename}</p>
                      <p className="text-xs text-gray-500">
                        {document.chunkCount} chunks processed
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => void handleDelete(document)}
                      className="text-xs font-medium text-error hover:text-red-600"
                      aria-label={`Delete ${document.filename}`}
                      data-testid={`rag-document-delete-${document.id}`}
                    >
                      Delete
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

        <aside className="space-y-6">
          <div className="bg-white rounded-lg shadow p-6" data-testid="rag-config-card">
            <div className="mb-4">
              <h2 className="text-lg font-semibold text-gray-900">RAG Configuration</h2>
              <p className="text-sm text-gray-600">Tune embedding and chunking settings.</p>
            </div>

            <div className="space-y-4">
              <div className="space-y-1">
                <label
                  className="text-sm font-medium text-gray-700"
                  htmlFor="embedding-model-select"
                >
                  Embedding Model
                </label>
                {embeddingModelsLoading ? (
                  <div className="px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-sm text-gray-500">
                    Loading embedding models...
                  </div>
                ) : (
                  <select
                    id="embedding-model-select"
                    value={embeddingModel ?? ''}
                    onChange={(event) =>
                      setEmbeddingModel(event.target.value ? event.target.value : null)
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                    data-testid="rag-embedding-model-select"
                  >
                    {embeddingModels.length === 0 && (
                      <option value="">No embedding models available</option>
                    )}
                    {embeddingModels.map((model) => (
                      <option key={model.id} value={model.ollamaModelName}>
                        {model.name} ({model.ollamaModelName})
                      </option>
                    ))}
                  </select>
                )}
                {embeddingModelsError && (
                  <p className="text-xs text-error" data-testid="rag-embedding-error">
                    {embeddingModelsError.message}
                  </p>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-sm font-medium text-gray-700" htmlFor="chunk-size-input">
                  Chunk Size
                </label>
                <input
                  id="chunk-size-input"
                  type="number"
                  min={100}
                  max={2000}
                  step={1}
                  value={chunkSize}
                  onChange={(event) => setChunkSize(Number(event.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                  data-testid="rag-chunk-size-input"
                />
                <p className="text-xs text-gray-500">Controls the size of each document chunk.</p>
              </div>

              <div className="space-y-1">
                <label className="text-sm font-medium text-gray-700" htmlFor="chunk-overlap-input">
                  Chunk Overlap
                </label>
                <input
                  id="chunk-overlap-input"
                  type="number"
                  min={0}
                  max={500}
                  step={1}
                  value={chunkOverlap}
                  onChange={(event) => setChunkOverlap(Number(event.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-green focus:border-brand-green transition-colors duration-200"
                  data-testid="rag-chunk-overlap-input"
                />
                <p className="text-xs text-gray-500">
                  Overlap should be smaller than the chunk size.
                </p>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
