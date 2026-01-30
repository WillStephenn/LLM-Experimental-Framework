/**
 * Hooks Module
 *
 * Re-exports all custom React hooks for the LocalLab application.
 *
 * @module hooks
 */

// Experiment hooks
export { useExperiments } from './useExperiments';
export type {
  ExperimentFilters,
  UseExperimentsState,
  UseExperimentsActions,
  UseExperimentsReturn,
} from './useExperiments';

export { useExperimentRuns } from './useExperimentRuns';
export type {
  ExperimentRunFilters,
  UseExperimentRunsState,
  UseExperimentRunsActions,
  UseExperimentRunsReturn,
} from './useExperimentRuns';

export { useExperimentControl } from './useExperimentControl';
export type {
  ExperimentControlAction,
  UseExperimentControlState,
  UseExperimentControlActions,
  UseExperimentControlReturn,
} from './useExperimentControl';

// Re-export ApiError for convenience
export { ApiError } from '@/api/client';
