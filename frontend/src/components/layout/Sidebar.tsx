/**
 * Sidebar Component
 *
 * Primary navigation sidebar for the LocalLab application.
 * Follows the Academic Modern design system with brand green background
 * and brand orange active states.
 */
import { NavLink } from 'react-router-dom';

/**
 * Navigation item configuration
 */
interface NavItem {
  /** Display label */
  label: string;
  /** Route path */
  path: string;
  /** Icon name identifier for NavIcon component */
  icon: string;
}

/**
 * Navigation items for the sidebar
 */
const navItems: NavItem[] = [
  { label: 'Sandbox', path: '/sandbox', icon: 'flask' },
  { label: 'Arena', path: '/arena', icon: 'swords' },
  { label: 'RAG Lab', path: '/rag', icon: 'book' },
  { label: 'Task Library', path: '/tasks', icon: 'list' },
  { label: 'Experiment Builder', path: '/experiments/new', icon: 'plus' },
  { label: 'Results Dashboard', path: '/results', icon: 'chart' },
  { label: 'Embedding Analyser', path: '/embeddings', icon: 'scatter' },
];

/**
 * Icon component that renders appropriate SVG icons
 */
function NavIcon({
  name,
  className,
}: {
  name: string;
  className?: string;
}): React.JSX.Element {
  const iconClass = className ?? 'w-5 h-5';

  switch (name) {
    case 'flask':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"
          />
        </svg>
      );
    case 'swords':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      );
    case 'book':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
          />
        </svg>
      );
    case 'list':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
          />
        </svg>
      );
    case 'plus':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      );
    case 'chart':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
          />
        </svg>
      );
    case 'scatter':
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4v16"
          />
        </svg>
      );
    default:
      return (
        <svg
          className={iconClass}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M4 6h16M4 12h16M4 18h16"
          />
        </svg>
      );
  }
}

/**
 * Sidebar component providing main application navigation
 */
export function Sidebar(): React.JSX.Element {
  return (
    <aside
      className="w-64 min-h-screen bg-brand-green shadow-xl z-20 flex flex-col"
      data-testid="sidebar"
    >
      {/* Brand Header */}
      <div className="p-6 border-b border-white/10">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
            <span
              className="text-white font-serif text-lg font-extrabold"
              style={{
                fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0",
              }}
            >
              L
            </span>
          </div>
          <div>
            <h1
              className="font-serif text-xl font-extrabold text-white tracking-tight leading-none"
              style={{
                fontVariationSettings: "'opsz' 24, 'SOFT' 0, 'WONK' 0",
              }}
            >
              LocalLab
            </h1>
            <p className="text-xs text-white/70 font-sans">
              LLM Experimental Framework
            </p>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4" aria-label="Main navigation">
        <ul className="space-y-1">
          {navItems.map((item) => (
            <li key={item.path}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  `w-full flex items-center px-4 py-3 rounded-md text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-brand-orange text-white shadow-md'
                      : 'text-gray-100 hover:bg-white/10 hover:text-white'
                  }`
                }
                data-testid={`nav-${item.path.replace(/\//g, '-').slice(1) || 'home'}`}
              >
                <NavIcon name={item.icon} className="w-5 h-5 mr-3" />
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-white/10">
        <p className="text-xs text-white/50 text-center">
          Local-first LLM framework
        </p>
      </div>
    </aside>
  );
}
