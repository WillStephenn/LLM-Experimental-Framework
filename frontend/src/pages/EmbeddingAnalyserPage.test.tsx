import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { EmbeddingAnalyserPage } from './EmbeddingAnalyserPage';

describe('EmbeddingAnalyserPage', () => {
  it('renders the page with title and description', () => {
    render(<EmbeddingAnalyserPage />);

    expect(screen.getByTestId('embedding-analyser-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Embedding Analyser' })).toBeInTheDocument();
    expect(screen.getByText('Analyse and visualise embeddings')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<EmbeddingAnalyserPage />);
    expect(screen.getByText(/Embedding analyser coming soon/)).toBeInTheDocument();
  });
});
