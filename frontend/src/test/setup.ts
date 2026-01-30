import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

// Mock ResizeObserver for Radix UI components
class MockResizeObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}
window.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;

// Mock scrollIntoView for Radix UI components
Element.prototype.scrollIntoView = function (): void {
  // Do nothing - mock for JSDOM
};

// Mock hasPointerCapture for Radix UI components
Element.prototype.hasPointerCapture = function (): boolean {
  return false;
};

// Mock releasePointerCapture for Radix UI components
Element.prototype.releasePointerCapture = function (): void {
  // Do nothing - mock for JSDOM
};

// Mock setPointerCapture for Radix UI components
Element.prototype.setPointerCapture = function (): void {
  // Do nothing - mock for JSDOM
};

// Cleanup after each test case
afterEach(() => {
  cleanup();
});
