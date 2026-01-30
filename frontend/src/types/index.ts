/**
 * LocalLab TypeScript Type Definitions
 *
 * This file contains all TypeScript type definitions for the LocalLab application,
 * matching the backend entities and DTOs as defined in the API contract.
 *
 * @see docs/API-Contract.md for the complete API specification
 */

// ============================================================================
// Enumeration Types
// ============================================================================

/**
 * Represents the lifecycle status of an Experiment.
 *
 * State Transitions:
 * - DRAFT → RUNNING (start)
 * - RUNNING → PAUSED (pause) | COMPLETED (complete) | FAILED (cancel/error)
 * - PAUSED → RUNNING (resume)
 */
export type ExperimentStatus = 'DRAFT' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED';

/**
 * Represents the execution status of an individual ExperimentRun.
 *
 * State Transitions:
 * - PENDING → RUNNING (execute)
 * - RUNNING → SUCCESS (success) | FAILED (error)
 */
export type RunStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

/**
 * Determines how context is provided to the model during experiment execution.
 */
export type ContextMode = 'NONE' | 'RAG' | 'FULL_CONTEXT';

/**
 * Defines the types of messages sent via WebSocket during experiment execution.
 */
export type WebSocketMessageType =
  | 'PROGRESS'
  | 'RUN_STARTED'
  | 'RUN_COMPLETED'
  | 'EXPERIMENT_COMPLETED'
  | 'EXPERIMENT_PAUSED'
  | 'ERROR';

// ============================================================================
// Common Types
// ============================================================================

/**
 * Pagination parameters for paginated requests.
 */
export interface PaginationParams {
  /** Page number (zero-indexed). */
  page?: number;
  /** Items per page. */
  size?: number;
  /** Sort field and direction (e.g., 'createdAt,desc'). */
  sort?: string;
}

/**
 * Paginated response wrapper.
 */
export interface PaginatedResponse<T> {
  /** The content items for the current page. */
  content: T[];
  /** Total number of elements across all pages. */
  totalElements: number;
  /** Total number of pages. */
  totalPages: number;
  /** Current page number (zero-indexed). */
  number: number;
  /** Number of items per page. */
  size: number;
}

/**
 * Standard error response structure.
 */
export interface ErrorResponse {
  /** ISO 8601 timestamp of when the error occurred. */
  timestamp: string;
  /** HTTP status code. */
  status: number;
  /** HTTP status reason phrase (e.g., 'Bad Request', 'Not Found'). */
  error: string;
  /** Human-readable error message describing what went wrong. */
  message: string;
  /** The request path that triggered the error. */
  path: string;
  /** List of field-level validation errors. */
  fieldErrors?: FieldError[];
}

/**
 * Represents a single field-level validation error.
 */
export interface FieldError {
  /** The name of the field that failed validation. */
  field: string;
  /** The validation error message for this field. */
  message: string;
}

// ============================================================================
// Hyperparameters
// ============================================================================

/**
 * Hyperparameters controlling model generation behaviour.
 */
export interface Hyperparameters {
  /** Controls randomness in generation (0.0 to 2.0). */
  temperature?: number;
  /** Nucleus sampling probability threshold (0.0 to 1.0). */
  topP?: number;
  /** Top-k sampling parameter (1 to 100). */
  topK?: number;
  /** Context window size in tokens (512 to 128000). */
  contextWindow?: number;
  /** Maximum number of tokens to generate. */
  maxTokens?: number;
}

// ============================================================================
// Task Template Types
// ============================================================================

/**
 * Request DTO for creating and updating task templates.
 */
export interface TaskTemplateRequest {
  /** Short, descriptive name for the task template (max 100 characters). */
  name: string;
  /** Detailed description of what this task evaluates (max 5000 characters). */
  description?: string;
  /** The main prompt template with {{variable}} placeholders (max 50000 characters). */
  promptTemplate: string;
  /** Comma-separated list of tags (max 500 characters). */
  tags?: string;
  /** Notes describing evaluation criteria (max 10000 characters). */
  evaluationNotes?: string;
}

/**
 * Response DTO for TaskTemplate entities.
 */
export interface TaskTemplateResponse {
  /** Unique identifier for the task template. */
  id: number;
  /** Short, descriptive name for the task template. */
  name: string;
  /** Detailed description of what this task evaluates. */
  description: string | null;
  /** The main prompt template with {{variable}} placeholders. */
  promptTemplate: string;
  /** Comma-separated list of tags. */
  tags: string | null;
  /** Notes describing evaluation criteria. */
  evaluationNotes: string | null;
  /** Timestamp when this task template was created. */
  createdAt: string;
}

/**
 * Summary DTO for task template information.
 */
export interface TaskTemplateSummary {
  /** Unique identifier for the task template. */
  id: number;
  /** Name of the task template. */
  name: string;
}

// ============================================================================
// System Prompt Types
// ============================================================================

/**
 * Request DTO for creating and updating system prompts.
 */
export interface SystemPromptRequest {
  /** Short, unique alias for the system prompt (max 50 characters). */
  alias: string;
  /** The full content of the system prompt (max 50000 characters). */
  content: string;
}

/**
 * Response DTO for SystemPrompt entities.
 */
export interface SystemPromptResponse {
  /** Unique identifier for the system prompt. */
  id: number;
  /** Short, unique alias for the system prompt. */
  alias: string;
  /** The full content of the system prompt. */
  content: string;
  /** Timestamp when this system prompt was created. */
  createdAt: string;
}

/**
 * Summary DTO for system prompt information.
 */
export interface SystemPromptSummary {
  /** Unique identifier for the system prompt. */
  id: number;
  /** Short alias for the system prompt. */
  alias: string;
}

// ============================================================================
// Embedding Model Types
// ============================================================================

/**
 * Request DTO for creating embedding model configurations.
 */
export interface EmbeddingModelRequest {
  /** Display name for the embedding model (max 100 characters). */
  name: string;
  /** The Ollama model name used for API calls (max 100 characters). */
  ollamaModelName: string;
  /** The number of dimensions in the embedding vectors (1 to 8192). */
  dimensions: number;
}

/**
 * Response DTO for EmbeddingModel entities.
 */
export interface EmbeddingModelResponse {
  /** Unique identifier for the embedding model. */
  id: number;
  /** Display name for the embedding model. */
  name: string;
  /** The Ollama model name used for API calls. */
  ollamaModelName: string;
  /** The number of dimensions in the embedding vectors. */
  dimensions: number;
  /** Timestamp when this embedding model configuration was created. */
  createdAt: string;
}

/**
 * Summary DTO for embedding model information.
 */
export interface EmbeddingModelSummary {
  /** Unique identifier for the embedding model. */
  id: number;
  /** Display name for the embedding model. */
  name: string;
}

// ============================================================================
// Document Types
// ============================================================================

/**
 * Response DTO for Document entities.
 * Note: The content field is intentionally excluded to reduce payload size.
 */
export interface DocumentResponse {
  /** Unique identifier for the document. */
  id: number;
  /** Original filename of the uploaded document. */
  filename: string;
  /** Number of chunks generated from this document. */
  chunkCount: number;
  /** Timestamp when this document was uploaded. */
  createdAt: string;
}

// ============================================================================
// Experiment Types
// ============================================================================

/**
 * Configuration for an experiment.
 */
export interface ExperimentConfig {
  /** List of model names to test in the experiment. */
  models: string[];
  /** List of embedding model names for RAG-based experiments. */
  embeddingModels?: string[];
  /** Number of iterations to run for each model/embedding combination (1-100). */
  iterations: number;
  /** Determines how context is provided to the model. */
  contextMode: ContextMode;
  /** ID of the document to use for RAG-based experiments. */
  documentId?: number;
  /** ID of the system prompt to use for all runs. */
  systemPromptId?: number;
  /** Hyperparameters controlling model generation behaviour. */
  hyperparameters: Hyperparameters;
  /** Variable values to substitute into the task template prompt. */
  variableValues?: Record<string, string>;
}

/**
 * Request DTO for creating and updating experiments.
 */
export interface ExperimentRequest {
  /** Name of the experiment (max 200 characters). */
  name: string;
  /** ID of the task template to use for this experiment. */
  taskTemplateId?: number;
  /** Configuration for the experiment. */
  config: ExperimentConfig;
}

/**
 * Response DTO for Experiment entities.
 */
export interface ExperimentResponse {
  /** Unique identifier for the experiment. */
  id: number;
  /** Name of the experiment. */
  name: string;
  /** Summary of the associated task template, or null if none. */
  taskTemplate: TaskTemplateSummary | null;
  /** Current status of the experiment. */
  status: ExperimentStatus;
  /** Configuration object for the experiment. */
  config: ExperimentConfig;
  /** Total number of runs calculated from the configuration. */
  totalRuns: number;
  /** Number of completed runs (both successful and failed). */
  completedRuns: number;
  /** Timestamp when this experiment was created. */
  createdAt: string;
}

// ============================================================================
// Experiment Run Types
// ============================================================================

/**
 * Represents a chunk of text retrieved from a RAG query.
 */
export interface RetrievedChunk {
  /** The text content of the retrieved chunk. */
  content: string;
  /** Distance score from the query embedding (lower = more similar). */
  distance: number;
  /** The position of this chunk within the source document (zero-indexed). */
  chunkIndex: number;
}

/**
 * Response DTO for ExperimentRun entities.
 */
export interface ExperimentRunResponse {
  /** Unique identifier for the experiment run. */
  id: number;
  /** The identifier of the parent experiment. */
  experimentId: number;
  /** The name of the Ollama model used for this run. */
  modelName: string;
  /** Summary of the embedding model used (if applicable). */
  embeddingModel: EmbeddingModelSummary | null;
  /** Summary of the system prompt used (if applicable). */
  systemPrompt: SystemPromptSummary | null;
  /** The iteration number within the experiment (1-indexed). */
  iteration: number;
  /** JSON configuration string containing the exact parameters used. */
  config: string;
  /** Current status of the run. */
  status: RunStatus;
  /** The complete output generated by the model. */
  output: string | null;
  /** Total generation time in milliseconds. */
  durationMs: number | null;
  /** Throughput metric: tokens generated per second. */
  tokensPerSecond: number | null;
  /** Time to first token in milliseconds. */
  timeToFirstTokenMs: number | null;
  /** List of retrieved document chunks for RAG runs. */
  retrievedChunks: RetrievedChunk[] | null;
  /** Error message if the run failed. */
  errorMessage: string | null;
  /** Timestamp when this run was executed. */
  timestamp: string;
}

/**
 * A summary view of an experiment run for use in analytics.
 */
export interface RunSummary {
  /** The unique identifier of the run. */
  id: number;
  /** The status of the run. */
  status: RunStatus;
  /** The total duration of the run in milliseconds. */
  durationMs: number | null;
  /** The tokens per second throughput metric. */
  tokensPerSecond: number | null;
}

// ============================================================================
// Ollama Types
// ============================================================================

/**
 * Request DTO for text generation operations via Ollama.
 */
export interface GenerationRequest {
  /** The name of the Ollama model to use for generation. */
  model: string;
  /** The prompt text to send to the model for generation (max 100000 characters). */
  prompt: string;
  /** Optional system prompt to set the model's behaviour (max 50000 characters). */
  systemPrompt?: string;
  /** Controls randomness in generation (0.0 to 2.0). */
  temperature?: number;
  /** Nucleus sampling parameter (0.0 to 1.0). */
  topP?: number;
  /** Limits generation to the top K most likely tokens (1 to 100). */
  topK?: number;
  /** The maximum context window size in tokens (512 to 128000). */
  contextWindow?: number;
  /** Optional maximum number of tokens to generate. */
  maxTokens?: number;
  /** Whether to stream the response. */
  stream?: boolean;
}

/**
 * Response DTO for text generation operations.
 */
export interface GenerationResponse {
  /** The generated text response from the model. */
  response: string;
  /** The name of the model that generated this response. */
  model: string;
  /** Total duration of the generation in milliseconds. */
  durationMs: number;
  /** Generation throughput measured in tokens per second. */
  tokensPerSecond: number | null;
  /** Time to first token in milliseconds. */
  timeToFirstTokenMs: number | null;
  /** Number of tokens in the prompt that was sent. */
  promptTokens: number | null;
  /** Number of tokens generated in the response. */
  completionTokens: number | null;
}

/**
 * Response DTO for Ollama service status.
 */
export interface OllamaStatusResponse {
  /** Indicates whether the Ollama service is available and responding. */
  available: boolean;
  /** The base URL of the Ollama service. */
  baseUrl: string;
  /** The number of models available in the Ollama instance. */
  modelCount: number;
  /** A human-readable status message describing the current state. */
  message: string;
}

/**
 * Response DTO for the list of available Ollama models.
 */
export interface ModelsResponse {
  /** The list of available model names in the Ollama instance. */
  models: string[];
}

// ============================================================================
// RAG Types
// ============================================================================

/**
 * Request DTO for RAG (Retrieval-Augmented Generation) queries.
 */
export interface RagQueryRequest {
  /** The query text to search for in the document (max 10000 characters). */
  query: string;
  /** The Ollama embedding model to use for the query. */
  embeddingModel: string;
  /** Number of top matching chunks to retrieve (1 to 20, default 5). */
  topK?: number;
  /** Size of each chunk in characters (100 to 2000, default 500). */
  chunkSize?: number;
  /** Number of overlapping characters between chunks (0 to 500, default 50). */
  chunkOverlap?: number;
}

/**
 * Response DTO for RAG queries.
 */
export interface RagQueryResponse {
  /** The original query text that was searched. */
  query: string;
  /** The list of retrieved chunks ordered by relevance. */
  retrievedChunks: RetrievedChunk[];
  /** The assembled context string ready for injection into an LLM prompt. */
  assembledContext: string;
  /** The embedding model used for the query. */
  embeddingModel: string;
}

// ============================================================================
// Analytics Types
// ============================================================================

/**
 * Filter parameters for leaderboard queries.
 */
export interface LeaderboardFilter {
  /** Filter by specific experiment. */
  experimentId?: number;
  /** Filter by model name. */
  modelName?: string;
  /** Filter by embedding model. */
  embeddingModel?: string;
  /** Minimum success rate threshold (0.0-1.0). */
  minSuccessRate?: number;
}

/**
 * A single entry in the leaderboard for a specific model.
 */
export interface LeaderboardEntry {
  /** The name of the model this entry represents. */
  modelName: string;
  /** The total number of runs for this model. */
  runCount: number;
  /** The number of successful runs for this model. */
  successCount: number;
  /** The success rate as a percentage (0-100). */
  successRate: number | null;
  /** The average tokens per second across successful runs. */
  avgTokensPerSecond: number | null;
  /** The minimum tokens per second observed. */
  minTokensPerSecond: number | null;
  /** The maximum tokens per second observed. */
  maxTokensPerSecond: number | null;
  /** The average duration in milliseconds. */
  avgDurationMs: number | null;
  /** The minimum duration in milliseconds. */
  minDurationMs: number | null;
  /** The maximum duration in milliseconds. */
  maxDurationMs: number | null;
  /** The average time to first token in milliseconds. */
  avgTimeToFirstTokenMs: number | null;
}

/**
 * Response DTO for leaderboard data.
 */
export interface LeaderboardResponse {
  /** List of leaderboard entries, one per model. */
  entries: LeaderboardEntry[];
  /** The total number of runs included in this leaderboard analysis. */
  totalRuns: number;
  /** Timestamp when this leaderboard was generated. */
  generatedAt: string;
}

/**
 * Detailed metrics for a specific model.
 */
export interface ModelMetrics {
  /** The name of the model these metrics are for. */
  modelName: string;
  /** The total number of runs for this model. */
  totalRuns: number;
  /** The number of successful runs. */
  successfulRuns: number;
  /** The number of failed runs. */
  failedRuns: number;
  /** The success rate as a percentage (0-100). */
  successRate: number | null;
  /** The average tokens per second across successful runs. */
  avgTokensPerSecond: number | null;
  /** The average duration in milliseconds. */
  avgDurationMs: number | null;
  /** The average time to first token in milliseconds. */
  avgTimeToFirstTokenMs: number | null;
  /** Runs grouped by iteration number. */
  runsByIteration: Record<number, RunSummary[]>;
}

/**
 * Response DTO for detailed model metrics.
 */
export interface ModelMetricsResponse {
  /** The name of the model these metrics are for. */
  modelName: string;
  /** The total number of runs for this model. */
  totalRuns: number;
  /** The number of successful runs. */
  successfulRuns: number;
  /** The number of failed runs. */
  failedRuns: number;
  /** The success rate as a percentage (0-100). */
  successRate: number | null;
  /** The average tokens per second across successful runs. */
  avgTokensPerSecond: number | null;
  /** The average duration in milliseconds. */
  avgDurationMs: number | null;
  /** The average time to first token in milliseconds. */
  avgTimeToFirstTokenMs: number | null;
  /** Runs grouped by iteration number. */
  runsByIteration: Record<number, RunSummary[]>;
}

/**
 * Response DTO for model comparison data within an experiment.
 */
export interface ModelComparisonResponse {
  /** The unique identifier of the experiment. */
  experimentId: number;
  /** The name of the experiment. */
  experimentName: string;
  /** Metrics for each model in the experiment. */
  models: Record<string, ModelMetrics>;
  /** Timestamp when this comparison was generated. */
  generatedAt: string;
}

// ============================================================================
// WebSocket Types
// ============================================================================

/**
 * Base structure for all WebSocket messages.
 */
export interface WebSocketMessage<T = unknown> {
  /** The type of message being sent. */
  type: WebSocketMessageType;
  /** The unique identifier of the experiment this message relates to. */
  experimentId: number;
  /** The message payload. */
  payload: T;
  /** The timestamp when this message was created. */
  timestamp: string;
}

/**
 * Payload for PROGRESS WebSocket messages.
 */
export interface ProgressPayload {
  /** The total number of runs to be executed. */
  totalRuns: number;
  /** The number of runs that have completed. */
  completedRuns: number;
  /** The number of runs that have failed. */
  failedRuns: number;
  /** The percentage of the experiment that has completed (0-100). */
  percentComplete: number;
  /** The identifier of the next run about to be executed. */
  currentRunId: number | null;
  /** Estimated time remaining in milliseconds. */
  estimatedTimeRemainingMs: number | null;
}

/**
 * Payload for RUN_STARTED WebSocket messages.
 */
export interface RunStartedPayload {
  /** The unique identifier of the run. */
  runId: number | null;
  /** The name of the model being used for this run. */
  modelName: string;
  /** The iteration number of this run (1-indexed). */
  iteration: number;
  /** The embedding model being used for RAG context. */
  embeddingModel: string | null;
}

/**
 * Payload for RUN_COMPLETED WebSocket messages.
 */
export interface RunCompletedPayload {
  /** The unique identifier of the completed run. */
  runId: number;
  /** The final status of the run. */
  status: RunStatus;
  /** The total duration of the run in milliseconds. */
  durationMs: number | null;
  /** The tokens per second throughput metric. */
  tokensPerSecond: number | null;
  /** Error message describing the failure reason. */
  errorMessage: string | null;
}

/**
 * Payload for EXPERIMENT_COMPLETED WebSocket messages.
 */
export interface ExperimentCompletedPayload {
  /** The final status of the experiment. */
  finalStatus: ExperimentStatus;
  /** The total number of runs originally planned. */
  totalRuns: number;
  /** The number of runs that completed successfully. */
  successfulRuns: number;
  /** The number of runs that failed. */
  failedRuns: number;
  /** The total duration of the experiment in milliseconds. */
  totalDurationMs: number | null;
}

/**
 * Payload for EXPERIMENT_PAUSED WebSocket messages.
 */
export interface ExperimentPausedPayload {
  /** The number of runs that completed before the pause. */
  completedRuns: number;
  /** The number of runs remaining to be executed. */
  remainingRuns: number;
}

/**
 * Standard error codes for WebSocket ERROR messages.
 */
export type WebSocketErrorCode =
  | 'OLLAMA_UNAVAILABLE'
  | 'CHROMA_UNAVAILABLE'
  | 'MODEL_NOT_FOUND'
  | 'OUT_OF_MEMORY'
  | 'GENERATION_TIMEOUT';

/**
 * Payload for ERROR WebSocket messages.
 */
export interface ErrorPayload {
  /** Error code identifying the type of error. */
  errorCode: WebSocketErrorCode;
  /** Human-readable description of the error. */
  message: string;
  /** Indicates whether the error is recoverable. */
  recoverable: boolean;
}

// ============================================================================
// Typed WebSocket Messages
// ============================================================================

/** WebSocket message for progress updates. */
export type ProgressMessage = WebSocketMessage<ProgressPayload>;

/** WebSocket message for run started events. */
export type RunStartedMessage = WebSocketMessage<RunStartedPayload>;

/** WebSocket message for run completed events. */
export type RunCompletedMessage = WebSocketMessage<RunCompletedPayload>;

/** WebSocket message for experiment completed events. */
export type ExperimentCompletedMessage = WebSocketMessage<ExperimentCompletedPayload>;

/** WebSocket message for experiment paused events. */
export type ExperimentPausedMessage = WebSocketMessage<ExperimentPausedPayload>;

/** WebSocket message for error events. */
export type ErrorMessage = WebSocketMessage<ErrorPayload>;

/** Union type for all WebSocket message types. */
export type AnyWebSocketMessage =
  | ProgressMessage
  | RunStartedMessage
  | RunCompletedMessage
  | ExperimentCompletedMessage
  | ExperimentPausedMessage
  | ErrorMessage;
