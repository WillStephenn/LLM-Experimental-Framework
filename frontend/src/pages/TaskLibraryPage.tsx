/**
 * Task Library Page
 *
 * Browse and manage task templates.
 */
export function TaskLibraryPage(): React.JSX.Element {
  return (
    <div data-testid="task-library-page">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 font-sans">Task Library</h1>
        <p className="text-sm text-gray-600 mt-1">Browse and manage task templates</p>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Task template library coming soon. Create, edit, and organise reusable prompt templates.
        </p>
      </div>
    </div>
  );
}
