import { create } from 'zustand';
import type { RagQueryRequest } from '@/types';

/**
 * Global RAG configuration state.
 */
export interface RagConfigState {
  /** Selected embedding model name (Ollama model). */
  embeddingModel: RagQueryRequest['embeddingModel'] | null;
  /** Chunk size for document processing (100-2000). */
  chunkSize: number;
  /** Chunk overlap size (0-500, must be smaller than chunkSize). */
  chunkOverlap: number;
}

/**
 * Actions for updating RAG configuration state.
 */
export interface RagConfigActions {
  /** Set the embedding model name. */
  setEmbeddingModel: (embeddingModel: RagQueryRequest['embeddingModel'] | null) => void;
  /** Set chunk size (100-2000). */
  setChunkSize: (chunkSize: number) => void;
  /** Set chunk overlap (0-500, less than chunk size). */
  setChunkOverlap: (chunkOverlap: number) => void;
  /** Reset configuration to defaults. */
  reset: () => void;
}

/**
 * Default RAG configuration values.
 */
export const DEFAULT_RAG_CONFIG: RagConfigState = {
  embeddingModel: null,
  chunkSize: 500,
  chunkOverlap: 50,
};

const clamp = (value: number, min: number, max: number): number =>
  Math.max(min, Math.min(max, value));

const getMaxOverlap = (chunkSize: number): number => Math.min(500, Math.max(0, chunkSize - 1));

/**
 * Zustand store for RAG configuration state.
 */
export const useRagConfigStore = create<RagConfigState & RagConfigActions>()((set) => ({
  ...DEFAULT_RAG_CONFIG,
  setEmbeddingModel: (embeddingModel: RagQueryRequest['embeddingModel'] | null): void =>
    set({ embeddingModel }),
  setChunkSize: (chunkSize: number): void =>
    set((state) => {
      const clampedSize = clamp(Math.round(chunkSize), 100, 2000);
      const maxOverlap = getMaxOverlap(clampedSize);
      return {
        chunkSize: clampedSize,
        chunkOverlap: Math.min(state.chunkOverlap, maxOverlap),
      };
    }),
  setChunkOverlap: (chunkOverlap: number): void =>
    set((state) => {
      const maxOverlap = getMaxOverlap(state.chunkSize);
      return { chunkOverlap: clamp(Math.round(chunkOverlap), 0, maxOverlap) };
    }),
  reset: (): void => set({ ...DEFAULT_RAG_CONFIG }),
}));
