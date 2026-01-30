/**
 * Layout Component
 *
 * Main layout wrapper that provides the sidebar navigation and content area.
 * Follows the Academic Modern design system.
 */
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

/**
 * Main layout component with sidebar and content area
 */
export function Layout(): React.JSX.Element {
  return (
    <div className="min-h-screen bg-gray-50" data-testid="layout">
      <div className="flex">
        {/* Sidebar Navigation */}
        <Sidebar />

        {/* Main Content Area */}
        <main className="flex-1 overflow-hidden" data-testid="main-content">
          <div className="p-6 overflow-auto h-screen">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
