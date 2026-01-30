import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TaskLibraryPage } from './TaskLibraryPage';

describe('TaskLibraryPage', () => {
  it('renders the page with title and description', () => {
    render(<TaskLibraryPage />);
    
    expect(screen.getByTestId('task-library-page')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Task Library' })).toBeInTheDocument();
    expect(screen.getByText('Browse and manage task templates')).toBeInTheDocument();
  });

  it('renders the placeholder content', () => {
    render(<TaskLibraryPage />);
    expect(screen.getByText(/Task template library coming soon/)).toBeInTheDocument();
  });
});
