/**
 * useDocuments Hook
 *
 * Provides document management operations for RAG workflows.
 *
 * @module hooks/useDocuments
 * @see docs/API-Contract.md for the complete API specification
 */
import { useState, useCallback } from 'react';
import { api } from '@/api/client';
import type { DocumentResponse } from '@/types';

/**
 * State and operations returned by the useDocuments hook.
 */
export interface UseDocumentsReturn {
  /** List of documents. */
  documents: DocumentResponse[];
  /** Whether a request is in progress. */
  isLoading: boolean;
  /** Error from the most recent operation. */
  error: Error | null;
  /** Fetch all documents. */
  fetchDocuments: () => Promise<void>;
  /** Upload a new document. */
  uploadDocument: (file: File) => Promise<DocumentResponse>;
  /** Delete a document by ID. */
  deleteDocument: (id: number) => Promise<void>;
  /** Clear the current error state. */
  clearError: () => void;
}

/**
 * Hook for managing document uploads and listings.
 */
export const useDocuments = (): UseDocumentsReturn => {
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchDocuments = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.get<DocumentResponse[]>('/documents');
      setDocuments(response);
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to fetch documents');
      setError(errorInstance);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const uploadDocument = useCallback(async (file: File): Promise<DocumentResponse> => {
    setIsLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await api.post<DocumentResponse>('/documents', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setDocuments((prev) => [response, ...prev]);
      return response;
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to upload document');
      setError(errorInstance);
      throw errorInstance;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const deleteDocument = useCallback(async (id: number): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      await api.delete(`/documents/${id}`);
      setDocuments((prev) => prev.filter((doc) => doc.id !== id));
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error('Failed to delete document');
      setError(errorInstance);
      throw errorInstance;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    documents,
    isLoading,
    error,
    fetchDocuments,
    uploadDocument,
    deleteDocument,
    clearError,
  };
};
