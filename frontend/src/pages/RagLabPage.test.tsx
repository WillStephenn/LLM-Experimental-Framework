import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RagLabPage } from './RagLabPage';

describe('RagLabPage', () => {
  it('renders the page with title and description', () => {
    render(<RagLabPage />);

    expect(screen.getByTestId('rag-lab-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'RAG Lab' })).toBeInTheDocument();
    expect(screen.getByText('Test retrieval-augmented generation')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<RagLabPage />);
    expect(screen.getByText(/RAG testing interface coming soon/)).toBeInTheDocument();
  });
});
