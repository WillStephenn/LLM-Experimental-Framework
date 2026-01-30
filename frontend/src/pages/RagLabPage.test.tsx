import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { RagLabPage } from './RagLabPage';

const fetchDocumentsMock = vi.fn();
const uploadDocumentMock = vi.fn();
const deleteDocumentMock = vi.fn();
const fetchEmbeddingModelsMock = vi.fn();
const setEmbeddingModelMock = vi.fn();
const setChunkSizeMock = vi.fn();
const setChunkOverlapMock = vi.fn();

const uploadResponse = {
  id: 2,
  filename: 'sample.txt',
  chunkCount: 2,
  createdAt: '2025-11-27T10:00:00Z',
};

uploadDocumentMock.mockResolvedValue(uploadResponse);
deleteDocumentMock.mockResolvedValue(undefined);

vi.mock('@/hooks', (): { useDocuments: () => object; useEmbeddingModels: () => object } => ({
  useDocuments: () => ({
    documents: [
      { id: 1, filename: 'architecture.pdf', chunkCount: 4, createdAt: '2025-11-27T10:00:00Z' },
    ],
    isLoading: false,
    error: null,
    fetchDocuments: fetchDocumentsMock,
    uploadDocument: uploadDocumentMock,
    deleteDocument: deleteDocumentMock,
  }),
  useEmbeddingModels: () => ({
    embeddingModels: [
      {
        id: 10,
        name: 'Nomic Embed Text',
        ollamaModelName: 'nomic-embed-text',
        dimensions: 768,
        createdAt: '2025-11-27T10:00:00Z',
      },
    ],
    isLoading: false,
    error: null,
    fetchEmbeddingModels: fetchEmbeddingModelsMock,
  }),
}));

vi.mock('@/store/ragConfigStore', (): { useRagConfigStore: () => object } => ({
  useRagConfigStore: () => ({
    embeddingModel: 'nomic-embed-text',
    chunkSize: 500,
    chunkOverlap: 50,
    setEmbeddingModel: setEmbeddingModelMock,
    setChunkSize: setChunkSizeMock,
    setChunkOverlap: setChunkOverlapMock,
  }),
}));

describe('RagLabPage', () => {
  it('renders the page with title and description', (): void => {
    render(<RagLabPage />);

    expect(screen.getByTestId('rag-lab-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'RAG Lab' })).toBeInTheDocument();
    expect(
      screen.getByText(
        'Upload documents, configure chunking, and manage embeddings for retrieval tests.'
      )
    ).toBeInTheDocument();
  });

  it('renders document and configuration sections', (): void => {
    render(<RagLabPage />);
    expect(screen.getByTestId('rag-upload-card')).toBeInTheDocument();
    expect(screen.getByTestId('rag-documents-card')).toBeInTheDocument();
    expect(screen.getByTestId('rag-config-card')).toBeInTheDocument();
    expect(screen.getByText('architecture.pdf')).toBeInTheDocument();
  });

  it('shows an error for invalid upload', (): void => {
    render(<RagLabPage />);
    const input = screen.getByTestId('rag-upload-input');
    const file = new File(['content'], 'sample.csv', { type: 'text/csv' });

    fireEvent.change(input, { target: { files: [file] } });

    expect(screen.getByTestId('rag-upload-error')).toHaveTextContent(
      'Only PDF or TXT files are supported.'
    );
  });

  it('triggers upload on drop', (): void => {
    render(<RagLabPage />);
    const dropzone = screen.getByTestId('rag-upload-dropzone');
    const file = new File(['content'], 'sample.txt', { type: 'text/plain' });

    fireEvent.drop(dropzone, {
      dataTransfer: { files: [file] },
    });

    expect(uploadDocumentMock).toHaveBeenCalledWith(file);
  });

  it('refreshes documents list', (): void => {
    render(<RagLabPage />);
    fireEvent.click(screen.getByTestId('rag-refresh-documents'));
    expect(fetchDocumentsMock).toHaveBeenCalled();
  });

  it('deletes a document', (): void => {
    render(<RagLabPage />);
    fireEvent.click(screen.getByTestId('rag-document-delete-1'));
    expect(deleteDocumentMock).toHaveBeenCalledWith(1);
  });

  it('updates configuration inputs', (): void => {
    render(<RagLabPage />);
    fireEvent.change(screen.getByTestId('rag-embedding-model-select'), {
      target: { value: 'nomic-embed-text' },
    });
    fireEvent.change(screen.getByTestId('rag-chunk-size-input'), { target: { value: '600' } });
    fireEvent.change(screen.getByTestId('rag-chunk-overlap-input'), { target: { value: '60' } });

    expect(setEmbeddingModelMock).toHaveBeenCalledWith('nomic-embed-text');
    expect(setChunkSizeMock).toHaveBeenCalledWith(600);
    expect(setChunkOverlapMock).toHaveBeenCalledWith(60);
  });
});
