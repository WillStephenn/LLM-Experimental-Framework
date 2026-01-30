# LocalLab API Contract

This document defines the communication protocols, data structures, and validation rules between the React Frontend and Spring Boot Backend.

**Last Updated:** 27 November 2025

---

## Related Documentation

| Document | Description |
|----------|-------------|
| [Specification.md](./Specification.md) | Project architecture, features, and technical design |
| [ISSUES.md](./ISSUES.md) | GitHub issues breakdown for implementation |

---

## Overview

This API contract defines the data exchange formats and validation requirements for all communication within the LocalLab application. It serves as the authoritative reference for implementing both frontend API clients and backend controllers.

### Architecture Summary

The system uses a **REST + WebSocket hybrid approach**:

- **HTTP (REST)**: For all CRUD operations, data retrieval, and generation requests. Messages are transmitted in JSON format.
- **WebSocket (STOMP)**: For real-time experiment progress updates and live event streaming.

**Roles:**
- **Server:** Spring Boot Backend (Java 21)
- **Client:** React Frontend (TypeScript)

**Ports:**
- **HTTP Port:** `8080`
- **WebSocket Endpoint:** `/ws`

**Base URL (HTTP):** `http://localhost:8080/api`

---

## 1. Common Standards

### 1.1. Request Headers

All HTTP requests must include:

| Header | Value | Description |
|--------|-------|-------------|
| `Content-Type` | `application/json` | Required for POST/PUT requests with JSON body |
| `Accept` | `application/json` | Expected response format |

### 1.2. Response Envelope

All successful responses return the resource directly (no wrapper). Error responses follow a consistent structure.

#### Success Response (Example)
```json
{
  "id": 1,
  "name": "Code Review Task",
  "createdAt": "2025-11-27T10:00:00Z"
}
```

#### Error Response Structure
```json
{
  "timestamp": "2025-11-27T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

### 1.3. HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| `200` | OK | Successful GET, PUT |
| `201` | Created | Successful POST (resource creation) |
| `204` | No Content | Successful DELETE |
| `400` | Bad Request | Validation failure, malformed request |
| `404` | Not Found | Resource does not exist |
| `409` | Conflict | Duplicate resource (e.g., unique constraint violation) |
| `503` | Service Unavailable | External service (Ollama/Chroma) unreachable |

### 1.4. Timestamp Format

All timestamps use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss.sssZ`

### 1.5. Pagination (Where Applicable)

Endpoints supporting pagination accept query parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | `0` | Page number (zero-indexed) |
| `size` | Integer | `20` | Items per page |
| `sort` | String | varies | Sort field and direction (e.g., `createdAt,desc`) |

Paginated responses include:
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

---

## 2. Enumeration Types

### 2.1. ExperimentStatus

| Value | Description |
|-------|-------------|
| `DRAFT` | Experiment created but not yet started |
| `RUNNING` | Experiment currently executing |
| `PAUSED` | Experiment paused, awaiting resume |
| `COMPLETED` | All runs finished successfully |
| `FAILED` | Experiment terminated due to critical error |

#### State Transitions

```
┌─────────┐   start    ┌─────────┐
│  DRAFT  │ ─────────► │ RUNNING │
└─────────┘            └────┬────┘
                            │
              ┌─────────────┼─────────────┐
              │ pause       │ complete    │ cancel/error
              ▼             ▼             ▼
        ┌─────────┐   ┌───────────┐   ┌────────┐
        │ PAUSED  │   │ COMPLETED │   │ FAILED │
        └────┬────┘   └───────────┘   └────────┘
             │ resume
             ▼
        ┌─────────┐
        │ RUNNING │
        └─────────┘
```

### 2.2. RunStatus

| Value | Description |
|-------|-------------|
| `PENDING` | Run queued, awaiting execution |
| `RUNNING` | Run currently in progress |
| `SUCCESS` | Run completed successfully |
| `FAILED` | Run terminated with error |

### 2.3. ContextMode

| Value | Description |
|-------|-------------|
| `NONE` | No additional context; prompt only |
| `RAG` | Retrieval-augmented generation using document chunks |
| `FULL_CONTEXT` | Complete context provided inline |

---

## 3. Data Transfer Objects (DTOs)

### 3.1. Task Template

#### TaskTemplateRequest (Client → Server)

Used for creating and updating task templates.

```json
{
  "name": "string",
  "description": "string",
  "promptTemplate": "string",
  "tags": "string",
  "evaluationNotes": "string"
}
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | Yes | Not blank, max 100 characters |
| `description` | String | No | Max 5000 characters |
| `promptTemplate` | String | Yes | Not blank, max 50000 characters |
| `tags` | String | No | Comma-separated, max 500 characters |
| `evaluationNotes` | String | No | Max 10000 characters |

#### TaskTemplateResponse (Server → Client)

```json
{
  "id": 1,
  "name": "Code Review Task",
  "description": "Review code for best practices",
  "promptTemplate": "Review the following code:\n\n{{code}}\n\nProvide feedback on...",
  "tags": "code,review,quality",
  "evaluationNotes": "Look for correctness, style, and performance",
  "createdAt": "2025-11-27T10:00:00Z"
}
```

---

### 3.2. System Prompt

#### SystemPromptRequest (Client → Server)

```json
{
  "alias": "string",
  "content": "string"
}
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `alias` | String | Yes | Not blank, max 50 characters, unique |
| `content` | String | Yes | Not blank, max 50000 characters |

#### SystemPromptResponse (Server → Client)

```json
{
  "id": 1,
  "alias": "code-assistant",
  "content": "You are an expert code reviewer...",
  "createdAt": "2025-11-27T10:00:00Z"
}
```

---

### 3.3. Embedding Model

#### EmbeddingModelRequest (Client → Server)

```json
{
  "name": "string",
  "ollamaModelName": "string",
  "dimensions": 768
}
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | Yes | Not blank, max 100 characters, unique |
| `ollamaModelName` | String | Yes | Not blank, max 100 characters |
| `dimensions` | Integer | Yes | Not null, min 1, max 8192 |

#### EmbeddingModelResponse (Server → Client)

```json
{
  "id": 1,
  "name": "Nomic Embed Text",
  "ollamaModelName": "nomic-embed-text",
  "dimensions": 768,
  "createdAt": "2025-11-27T10:00:00Z"
}
```

---

### 3.4. Document

#### DocumentUploadRequest (Client → Server)

**Content-Type:** `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | PDF or TXT file, max 10MB |

#### DocumentResponse (Server → Client)

```json
{
  "id": 1,
  "filename": "architecture.pdf",
  "chunkCount": 45,
  "createdAt": "2025-11-27T10:00:00Z"
}
```

**Note:** The `content` field is intentionally excluded from responses to reduce payload size. Use specific endpoints to retrieve document content if required.

---

### 3.5. Experiment

#### ExperimentRequest (Client → Server)

```json
{
  "name": "string",
  "taskTemplateId": 1,
  "config": {
    "models": ["string"],
    "embeddingModels": ["string"],
    "iterations": 3,
    "contextMode": "NONE",
    "documentId": null,
    "systemPromptId": null,
    "hyperparameters": {
      "temperature": 0.7,
      "topP": 0.9,
      "topK": 40,
      "contextWindow": 4096,
      "maxTokens": null
    },
    "variableValues": {
      "code": "function example() { ... }"
    }
  }
}
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | Yes | Not blank, max 200 characters |
| `taskTemplateId` | Long | No | Must reference existing TaskTemplate if provided |
| `config` | Object | Yes | Valid ExperimentConfig object |

#### ExperimentConfig Validation:

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `models` | String[] | Yes | At least one model, each must be non-blank |
| `embeddingModels` | String[] | No | Required if `contextMode` is `RAG` |
| `iterations` | Integer | Yes | Min 1, max 100 |
| `contextMode` | String | Yes | One of: `NONE`, `RAG`, `FULL_CONTEXT` |
| `documentId` | Long | No | Required if `contextMode` is `RAG` |
| `systemPromptId` | Long | No | Must reference existing SystemPrompt if provided |
| `hyperparameters` | Object | Yes | Valid Hyperparameters object |
| `variableValues` | Object | No | Key-value pairs for template variables |

#### Hyperparameters Validation:

| Field | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `temperature` | Double | No | `0.7` | Min 0.0, max 2.0 |
| `topP` | Double | No | `0.9` | Min 0.0, max 1.0 |
| `topK` | Integer | No | `40` | Min 1, max 100 |
| `contextWindow` | Integer | No | `4096` | Min 512, max 128000 |
| `maxTokens` | Integer | No | `null` | Min 1 if provided |

#### ExperimentResponse (Server → Client)

```json
{
  "id": 1,
  "name": "LLM Code Review Comparison",
  "taskTemplate": {
    "id": 1,
    "name": "Code Review Task"
  },
  "status": "DRAFT",
  "config": { ... },
  "totalRuns": 18,
  "completedRuns": 0,
  "createdAt": "2025-11-27T10:00:00Z"
}
```

---

### 3.6. Experiment Run

#### ExperimentRunResponse (Server → Client)

```json
{
  "id": 1,
  "experimentId": 1,
  "modelName": "qwen2.5-coder:7b",
  "embeddingModel": {
    "id": 1,
    "name": "Nomic Embed Text"
  },
  "systemPrompt": {
    "id": 1,
    "alias": "code-assistant"
  },
  "iteration": 1,
  "config": { ... },
  "status": "SUCCESS",
  "output": "The code looks well-structured...",
  "durationMs": 2500,
  "tokensPerSecond": 45.5,
  "timeToFirstTokenMs": 120,
  "retrievedChunks": [
    {
      "id": "chunk-1",
      "content": "Relevant context...",
      "distance": 0.23
    }
  ],
  "errorMessage": null,
  "timestamp": "2025-11-27T10:05:00Z"
}
```

---

### 3.7. Ollama Operations

#### GenerationRequest (Client → Server)

```json
{
  "model": "string",
  "prompt": "string",
  "systemPrompt": "string",
  "temperature": 0.7,
  "topP": 0.9,
  "topK": 40,
  "contextWindow": 4096,
  "maxTokens": null,
  "stream": false,
  "jsonMode": false
}
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `model` | String | Yes | Not blank |
| `prompt` | String | Yes | Not blank, max 100000 characters |
| `systemPrompt` | String | No | Max 50000 characters |
| `temperature` | Double | No | Min 0.0, max 2.0 |
| `topP` | Double | No | Min 0.0, max 1.0 |
| `topK` | Integer | No | Min 1, max 100 |
| `contextWindow` | Integer | No | Min 512, max 128000 |
| `maxTokens` | Integer | No | Min 1 if provided |
| `stream` | Boolean | No | Default `false` |
| `jsonMode` | Boolean | No | Default `false` |

#### GenerationResponse (Server → Client)

```json
{
  "response": "string",
  "model": "qwen2.5-coder:7b",
  "durationMs": 2500,
  "tokensPerSecond": 45.5,
  "timeToFirstTokenMs": 120,
  "promptTokens": 150,
  "completionTokens": 200
}
```

#### OllamaStatusResponse (Server → Client)

```json
{
  "available": true,
  "baseUrl": "http://localhost:11434",
  "modelCount": 5,
  "message": "Ollama is running"
}
```

---

### 3.8. RAG Operations

#### RagQueryRequest (Client → Server)

```json
{
  "query": "string",
  "embeddingModel": "string",
  "topK": 5,
  "chunkSize": 500,
  "chunkOverlap": 50
}
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `query` | String | Yes | Not blank, max 10000 characters |
| `embeddingModel` | String | Yes | Not blank |
| `topK` | Integer | No | Default 5, min 1, max 20 |
| `chunkSize` | Integer | No | Default 500, min 100, max 2000 |
| `chunkOverlap` | Integer | No | Default 50, min 0, max 500 |

#### RagQueryResponse (Server → Client)

```json
{
  "query": "What is the architecture pattern?",
  "retrievedChunks": [
    {
      "id": "chunk-uuid-1",
      "content": "The system uses a layered architecture...",
      "distance": 0.15,
      "metadata": {
        "documentId": 1,
        "chunkIndex": 3
      }
    }
  ],
  "assembledContext": "Context:\n---\nThe system uses a layered architecture...\n---",
  "embeddingModel": "nomic-embed-text"
}
```

#### RetrievedChunk

```json
{
  "id": "string",
  "content": "string",
  "distance": 0.0,
  "metadata": {
    "documentId": 1,
    "chunkIndex": 0
  }
}
```

---

### 3.9. Analytics

#### LeaderboardFilter (Client → Server)

Query parameters for filtering leaderboard data.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `experimentId` | Long | No | Filter by specific experiment |
| `modelName` | String | No | Filter by model name |
| `embeddingModel` | String | No | Filter by embedding model |
| `minSuccessRate` | Double | No | Minimum success rate threshold (0.0-1.0) |

#### LeaderboardResponse (Server → Client)

```json
{
  "entries": [
    {
      "modelName": "qwen2.5-coder:7b",
      "totalRuns": 50,
      "successfulRuns": 48,
      "successRate": 0.96,
      "averageTps": 45.5,
      "averageDurationMs": 2500,
      "averageTimeToFirstTokenMs": 120,
      "minTps": 35.0,
      "maxTps": 55.0
    }
  ],
  "generatedAt": "2025-11-27T10:00:00Z"
}
```

#### ModelMetricsResponse (Server → Client)

```json
{
  "modelName": "qwen2.5-coder:7b",
  "experimentId": 1,
  "totalRuns": 18,
  "successfulRuns": 17,
  "failedRuns": 1,
  "successRate": 0.944,
  "metrics": {
    "tokensPerSecond": {
      "average": 45.5,
      "min": 35.0,
      "max": 55.0,
      "standardDeviation": 5.2
    },
    "durationMs": {
      "average": 2500,
      "min": 1800,
      "max": 3200,
      "standardDeviation": 350
    },
    "timeToFirstTokenMs": {
      "average": 120,
      "min": 80,
      "max": 180,
      "standardDeviation": 25
    }
  },
  "byIteration": [
    { "iteration": 1, "averageTps": 44.0 },
    { "iteration": 2, "averageTps": 46.0 },
    { "iteration": 3, "averageTps": 46.5 }
  ]
}
```

---

## 4. REST Endpoints

### 4.1. Ollama Integration

#### List Available Models
Retrieves all models available in the connected Ollama instance.

- **Endpoint:** `GET /api/ollama/models`
- **Response:** `200 OK`
```json
{
  "models": [
    "qwen2.5-coder:7b",
    "codellama:7b",
    "deepseek-coder:6.7b",
    "nomic-embed-text",
    "mxbai-embed-large"
  ]
}
```

#### Check Ollama Status
Verifies connectivity with the Ollama service.

- **Endpoint:** `GET /api/ollama/status`
- **Response:** `200 OK` → `OllamaStatusResponse`
- **Response:** `503 Service Unavailable` → Error Response

#### Generate Response
Executes a single generation request.

- **Endpoint:** `POST /api/ollama/generate`
- **Body:** `GenerationRequest`
- **Response:** `200 OK` → `GenerationResponse`
- **Response:** `400 Bad Request` → Validation errors
- **Response:** `503 Service Unavailable` → Ollama unreachable

---

### 4.2. Task Templates

#### List All Tasks
- **Endpoint:** `GET /api/tasks`
- **Query Parameters:**
  | Parameter | Type | Description |
  |-----------|------|-------------|
  | `search` | String | Filter by name (case-insensitive partial match) |
  | `tag` | String | Filter by tag |
- **Response:** `200 OK` → `TaskTemplateResponse[]`

#### Get Single Task
- **Endpoint:** `GET /api/tasks/{id}`
- **Response:** `200 OK` → `TaskTemplateResponse`
- **Response:** `404 Not Found` → Resource not found

#### Create Task
- **Endpoint:** `POST /api/tasks`
- **Body:** `TaskTemplateRequest`
- **Response:** `201 Created` → `TaskTemplateResponse`
- **Response:** `400 Bad Request` → Validation errors

#### Update Task
- **Endpoint:** `PUT /api/tasks/{id}`
- **Body:** `TaskTemplateRequest`
- **Response:** `200 OK` → `TaskTemplateResponse`
- **Response:** `404 Not Found` → Resource not found
- **Response:** `400 Bad Request` → Validation errors

#### Delete Task
- **Endpoint:** `DELETE /api/tasks/{id}`
- **Response:** `204 No Content`
- **Response:** `404 Not Found` → Resource not found

---

### 4.3. System Prompts

#### List All System Prompts
- **Endpoint:** `GET /api/system-prompts`
- **Response:** `200 OK` → `SystemPromptResponse[]`

#### Get Single System Prompt
- **Endpoint:** `GET /api/system-prompts/{id}`
- **Response:** `200 OK` → `SystemPromptResponse`
- **Response:** `404 Not Found` → Resource not found

#### Create System Prompt
- **Endpoint:** `POST /api/system-prompts`
- **Body:** `SystemPromptRequest`
- **Response:** `201 Created` → `SystemPromptResponse`
- **Response:** `400 Bad Request` → Validation errors
- **Response:** `409 Conflict` → Alias already exists

#### Update System Prompt
- **Endpoint:** `PUT /api/system-prompts/{id}`
- **Body:** `SystemPromptRequest`
- **Response:** `200 OK` → `SystemPromptResponse`
- **Response:** `404 Not Found` → Resource not found
- **Response:** `409 Conflict` → Alias already exists

#### Delete System Prompt
- **Endpoint:** `DELETE /api/system-prompts/{id}`
- **Response:** `204 No Content`
- **Response:** `404 Not Found` → Resource not found

---

### 4.4. Documents & RAG

#### Upload Document
- **Endpoint:** `POST /api/documents`
- **Content-Type:** `multipart/form-data`
- **Body:** File upload (PDF or TXT, max 10MB)
- **Response:** `201 Created` → `DocumentResponse`
- **Response:** `400 Bad Request` → Invalid file type or size

#### List Documents
- **Endpoint:** `GET /api/documents`
- **Response:** `200 OK` → `DocumentResponse[]`

#### Get Single Document
- **Endpoint:** `GET /api/documents/{id}`
- **Response:** `200 OK` → `DocumentResponse`
- **Response:** `404 Not Found` → Resource not found

#### Delete Document
- **Endpoint:** `DELETE /api/documents/{id}`
- **Response:** `204 No Content`
- **Response:** `404 Not Found` → Resource not found

#### Query Document with RAG
- **Endpoint:** `POST /api/documents/{id}/query`
- **Body:** `RagQueryRequest`
- **Response:** `200 OK` → `RagQueryResponse`
- **Response:** `404 Not Found` → Document not found
- **Response:** `503 Service Unavailable` → Chroma or Ollama unreachable

#### List Embedding Models
- **Endpoint:** `GET /api/embedding-models`
- **Response:** `200 OK` → `EmbeddingModelResponse[]`

#### Create Embedding Model
- **Endpoint:** `POST /api/embedding-models`
- **Body:** `EmbeddingModelRequest`
- **Response:** `201 Created` → `EmbeddingModelResponse`
- **Response:** `400 Bad Request` → Validation errors
- **Response:** `409 Conflict` → Name already exists

#### Delete Embedding Model
- **Endpoint:** `DELETE /api/embedding-models/{id}`
- **Response:** `204 No Content`
- **Response:** `404 Not Found` → Resource not found

---

### 4.5. Experiments

#### List Experiments
- **Endpoint:** `GET /api/experiments`
- **Query Parameters:**
  | Parameter | Type | Description |
  |-----------|------|-------------|
  | `status` | String | Filter by ExperimentStatus |
- **Response:** `200 OK` → `ExperimentResponse[]`

#### Get Single Experiment
- **Endpoint:** `GET /api/experiments/{id}`
- **Response:** `200 OK` → `ExperimentResponse`
- **Response:** `404 Not Found` → Resource not found

#### Create Experiment
- **Endpoint:** `POST /api/experiments`
- **Body:** `ExperimentRequest`
- **Response:** `201 Created` → `ExperimentResponse`
- **Response:** `400 Bad Request` → Validation errors

#### Update Experiment
Only allowed when status is `DRAFT`.

- **Endpoint:** `PUT /api/experiments/{id}`
- **Body:** `ExperimentRequest`
- **Response:** `200 OK` → `ExperimentResponse`
- **Response:** `404 Not Found` → Resource not found
- **Response:** `400 Bad Request` → Cannot update non-draft experiment

#### Delete Experiment
Deletes experiment and all associated runs.

- **Endpoint:** `DELETE /api/experiments/{id}`
- **Response:** `204 No Content`
- **Response:** `404 Not Found` → Resource not found

#### Start Experiment
Begins experiment execution. Status must be `DRAFT`.

- **Endpoint:** `POST /api/experiments/{id}/start`
- **Response:** `200 OK` → `ExperimentResponse` (status: `RUNNING`)
- **Response:** `400 Bad Request` → Invalid state transition
- **Response:** `503 Service Unavailable` → Ollama unavailable

#### Pause Experiment
Pauses after current run completes. Status must be `RUNNING`.

- **Endpoint:** `POST /api/experiments/{id}/pause`
- **Response:** `200 OK` → `ExperimentResponse`
- **Response:** `400 Bad Request` → Invalid state transition

#### Resume Experiment
Resumes a paused experiment.

- **Endpoint:** `POST /api/experiments/{id}/resume`
- **Response:** `200 OK` → `ExperimentResponse` (status: `RUNNING`)
- **Response:** `400 Bad Request` → Invalid state transition

#### Cancel Experiment
Cancels experiment and marks remaining runs as failed.

- **Endpoint:** `POST /api/experiments/{id}/cancel`
- **Response:** `200 OK` → `ExperimentResponse` (status: `FAILED`)
- **Response:** `400 Bad Request` → Invalid state transition

---

### 4.6. Results

#### List Experiment Runs
- **Endpoint:** `GET /api/experiments/{id}/runs`
- **Query Parameters:**
  | Parameter | Type | Description |
  |-----------|------|-------------|
  | `status` | String | Filter by RunStatus |
  | `modelName` | String | Filter by model |
- **Response:** `200 OK` → `ExperimentRunResponse[]`
- **Response:** `404 Not Found` → Experiment not found

#### Get Single Run
- **Endpoint:** `GET /api/runs/{id}`
- **Response:** `200 OK` → `ExperimentRunResponse`
- **Response:** `404 Not Found` → Run not found

#### Export Experiment Results
- **Endpoint:** `GET /api/experiments/{id}/export`
- **Query Parameters:**
  | Parameter | Type | Required | Description |
  |-----------|------|----------|-------------|
  | `format` | String | Yes | `csv` or `json` |
- **Response:** `200 OK`
  - For `csv`: `Content-Type: text/csv`, `Content-Disposition: attachment`
  - For `json`: `Content-Type: application/json`
- **Response:** `404 Not Found` → Experiment not found

---

### 4.7. Analytics

#### Get Leaderboard
- **Endpoint:** `GET /api/analytics/leaderboard`
- **Query Parameters:** See `LeaderboardFilter`
- **Response:** `200 OK` → `LeaderboardResponse`

#### Get Model Metrics
- **Endpoint:** `GET /api/analytics/models/{modelName}`
- **Query Parameters:**
  | Parameter | Type | Description |
  |-----------|------|-------------|
  | `experimentId` | Long | Scope to specific experiment |
- **Response:** `200 OK` → `ModelMetricsResponse`

#### Compare Models in Experiment
- **Endpoint:** `GET /api/experiments/{id}/comparison`
- **Response:** `200 OK`
```json
{
  "experimentId": 1,
  "experimentName": "LLM Code Review Comparison",
  "models": {
    "qwen2.5-coder:7b": { ... },
    "codellama:7b": { ... }
  },
  "generatedAt": "2025-11-27T10:00:00Z"
}
```
- **Response:** `404 Not Found` → Experiment not found

---

## 5. WebSocket Protocol

### 5.1. Connection

**Endpoint:** `ws://localhost:8080/ws`

The WebSocket uses STOMP protocol for message routing. Clients subscribe to specific topics to receive real-time updates.

### 5.2. Subscription Topics

#### Experiment Progress
- **Topic:** `/topic/experiments/{experimentId}/progress`
- **Description:** Receives progress updates for a specific experiment

### 5.3. Message Types

All WebSocket messages follow this structure:

```json
{
  "type": "MESSAGE_TYPE",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:05:00Z",
  "payload": { ... }
}
```

#### PROGRESS
Overall experiment progress update.

```json
{
  "type": "PROGRESS",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:05:00Z",
  "payload": {
    "totalRuns": 18,
    "completedRuns": 5,
    "failedRuns": 0,
    "percentComplete": 27.8,
    "currentRunId": 6,
    "estimatedTimeRemainingMs": 32500
  }
}
```

#### RUN_STARTED
Indicates a new run has begun.

```json
{
  "type": "RUN_STARTED",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:05:00Z",
  "payload": {
    "runId": 6,
    "modelName": "qwen2.5-coder:7b",
    "iteration": 2,
    "embeddingModel": "nomic-embed-text"
  }
}
```

#### RUN_COMPLETED
Indicates a run has finished (success or failure).

```json
{
  "type": "RUN_COMPLETED",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:05:30Z",
  "payload": {
    "runId": 6,
    "status": "SUCCESS",
    "durationMs": 2500,
    "tokensPerSecond": 45.5,
    "errorMessage": null
  }
}
```

#### EXPERIMENT_COMPLETED
Indicates all runs have finished.

```json
{
  "type": "EXPERIMENT_COMPLETED",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:30:00Z",
  "payload": {
    "finalStatus": "COMPLETED",
    "totalRuns": 18,
    "successfulRuns": 17,
    "failedRuns": 1,
    "totalDurationMs": 45000
  }
}
```

#### EXPERIMENT_PAUSED
Indicates experiment has been paused.

```json
{
  "type": "EXPERIMENT_PAUSED",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:15:00Z",
  "payload": {
    "completedRuns": 10,
    "remainingRuns": 8
  }
}
```

#### ERROR
Indicates a critical error occurred.

```json
{
  "type": "ERROR",
  "experimentId": 1,
  "timestamp": "2025-11-27T10:05:00Z",
  "payload": {
    "errorCode": "OLLAMA_UNAVAILABLE",
    "message": "Failed to connect to Ollama service",
    "recoverable": false
  }
}
```

### 5.4. Error Codes

| Code | Description | Recoverable |
|------|-------------|-------------|
| `OLLAMA_UNAVAILABLE` | Cannot connect to Ollama | Yes (can retry) |
| `CHROMA_UNAVAILABLE` | Cannot connect to Chroma | Yes (can retry) |
| `MODEL_NOT_FOUND` | Requested model not available | No |
| `OUT_OF_MEMORY` | Insufficient memory for model | No |
| `GENERATION_TIMEOUT` | Model generation timed out | Yes (can retry) |

---

## 6. Validation Summary

### 6.1. Frontend Validation (Pre-submission)

The frontend should validate all user inputs before submission:

1. **Required fields** - Check all required fields are populated
2. **String length** - Enforce max length constraints
3. **Numeric ranges** - Validate min/max values
4. **Format validation** - Email, URL patterns where applicable
5. **Cross-field validation** - e.g., `chunkOverlap` < `chunkSize`

### 6.2. Backend Validation (Server-side)

The backend performs identical validation plus:

1. **Database constraints** - Unique fields, foreign key references
2. **Business rules** - State transitions, cascading deletes
3. **External service validation** - Model availability, service connectivity

### 6.3. Validation Error Response

When validation fails, the response includes field-level details:

```json
{
  "timestamp": "2025-11-27T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/experiments",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be blank"
    },
    {
      "field": "config.iterations",
      "message": "must be greater than or equal to 1"
    },
    {
      "field": "config.hyperparameters.temperature",
      "message": "must be less than or equal to 2.0"
    }
  ]
}
```

---

## 7. Entity Identification

**Critical:** Entity IDs must be persistent and consistent.

- **Server-generated IDs:** All entities (TaskTemplate, SystemPrompt, Experiment, etc.) have IDs assigned by the backend upon creation.
- **Client must preserve IDs:** When updating or referencing entities, clients must use the ID provided by the server.
- **UUID for chunks:** RAG chunks use UUID strings for identification within Chroma.

---

## 8. Security Considerations

LocalLab is designed as a local-only, single-user tool. However, the following safeguards are implemented:

1. **Input sanitisation** - All user inputs are sanitised to prevent injection attacks
2. **CORS configuration** - Restricted to `localhost` origins by default
3. **No authentication** - By design for local development use
4. **File upload limits** - Maximum 10MB per document upload
5. **Rate limiting** - Not implemented (local use only)

---

## 9. Versioning

The API follows semantic versioning. Breaking changes will increment the major version.

**Current Version:** v1 (implied in base URL structure)

Future versions would be accessed via `/api/v2/...`

---

## Appendix A: Complete Type Reference

### TypeScript Interfaces

```typescript
// Enums
type ExperimentStatus = 'DRAFT' | 'RUNNING' | 'COMPLETED' | 'FAILED';
type RunStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
type ContextMode = 'NONE' | 'RAG' | 'FULL_CONTEXT';

// Base entities
interface TaskTemplate {
  id: number;
  name: string;
  description?: string;
  promptTemplate: string;
  tags?: string;
  evaluationNotes?: string;
  createdAt: string;
}

interface SystemPrompt {
  id: number;
  alias: string;
  content: string;
  createdAt: string;
}

interface EmbeddingModel {
  id: number;
  name: string;
  ollamaModelName: string;
  dimensions: number;
}

interface Document {
  id: number;
  filename: string;
  chunkCount: number;
  createdAt: string;
}

// Configuration
interface Hyperparameters {
  temperature?: number;
  topP?: number;
  topK?: number;
  contextWindow?: number;
  maxTokens?: number;
}

interface ExperimentConfig {
  models: string[];
  embeddingModels?: string[];
  iterations: number;
  contextMode: ContextMode;
  documentId?: number;
  systemPromptId?: number;
  hyperparameters: Hyperparameters;
  variableValues?: Record<string, string>;
}

// Experiments
interface Experiment {
  id: number;
  name: string;
  taskTemplate?: { id: number; name: string };
  status: ExperimentStatus;
  config: ExperimentConfig;
  totalRuns: number;
  completedRuns: number;
  createdAt: string;
}

interface ExperimentRun {
  id: number;
  experimentId: number;
  modelName: string;
  embeddingModel?: { id: number; name: string };
  systemPrompt?: { id: number; alias: string };
  iteration: number;
  config: Hyperparameters;
  status: RunStatus;
  output?: string;
  durationMs?: number;
  tokensPerSecond?: number;
  timeToFirstTokenMs?: number;
  retrievedChunks?: RetrievedChunk[];
  errorMessage?: string;
  timestamp: string;
}

// RAG
interface RetrievedChunk {
  id: string;
  content: string;
  distance: number;
  metadata: {
    documentId: number;
    chunkIndex: number;
  };
}

// WebSocket
interface WebSocketMessage<T> {
  type: string;
  experimentId: number;
  timestamp: string;
  payload: T;
}

interface ProgressPayload {
  totalRuns: number;
  completedRuns: number;
  failedRuns: number;
  percentComplete: number;
  currentRunId?: number;
  estimatedTimeRemainingMs?: number;
}
```

---

## Appendix B: Example Request/Response Flows

### B.1. Creating and Running an Experiment

**Step 1: Create Experiment**
```http
POST /api/experiments
Content-Type: application/json

{
  "name": "Code Review Model Comparison",
  "taskTemplateId": 1,
  "config": {
    "models": ["qwen2.5-coder:7b", "codellama:7b"],
    "iterations": 3,
    "contextMode": "NONE",
    "hyperparameters": {
      "temperature": 0.7
    }
  }
}
```

**Response:**
```json
{
  "id": 5,
  "name": "Code Review Model Comparison",
  "status": "DRAFT",
  "totalRuns": 6,
  "completedRuns": 0,
  "createdAt": "2025-11-27T10:00:00Z"
}
```

**Step 2: Start Experiment**
```http
POST /api/experiments/5/start
```

**Response:**
```json
{
  "id": 5,
  "status": "RUNNING",
  ...
}
```

**Step 3: Subscribe to WebSocket**
```
SUBSCRIBE /topic/experiments/5/progress
```

**Step 4: Receive Progress Updates**
```json
{
  "type": "RUN_COMPLETED",
  "experimentId": 5,
  "timestamp": "2025-11-27T10:02:00Z",
  "payload": {
    "runId": 1,
    "status": "SUCCESS",
    "durationMs": 2500
  }
}
```

### B.2. RAG Query Flow

**Step 1: Upload Document**
```http
POST /api/documents
Content-Type: multipart/form-data

file: [architecture.pdf]
```

**Response:**
```json
{
  "id": 1,
  "filename": "architecture.pdf",
  "chunkCount": 45,
  "createdAt": "2025-11-27T10:00:00Z"
}
```

**Step 2: Query Document**
```http
POST /api/documents/1/query
Content-Type: application/json

{
  "query": "What design patterns are used?",
  "embeddingModel": "nomic-embed-text",
  "topK": 5
}
```

**Response:**
```json
{
  "query": "What design patterns are used?",
  "retrievedChunks": [
    {
      "id": "chunk-1",
      "content": "The application implements the Repository pattern...",
      "distance": 0.18
    }
  ],
  "assembledContext": "Context:\n---\nThe application implements the Repository pattern...\n---",
  "embeddingModel": "nomic-embed-text"
}
```

---

*This document is the authoritative reference for all API communication within LocalLab. Any deviations from this contract must be documented and approved.*

---

## Appendix C: Chroma Vector Store Operations

The backend communicates with Chroma via REST API. These internal operations are documented for completeness.

### C.1. Collection Management

#### Create Collection
- **Internal Operation:** `POST http://localhost:8000/api/v1/collections`
- **Body:**
```json
{
  "name": "doc-{documentId}-{embeddingModel}",
  "metadata": {
    "documentId": 1,
    "embeddingModel": "nomic-embed-text",
    "dimensions": 768
  }
}
```

#### Delete Collection
- **Internal Operation:** `DELETE http://localhost:8000/api/v1/collections/{collectionName}`

### C.2. Document Operations

#### Add Embeddings
- **Internal Operation:** `POST http://localhost:8000/api/v1/collections/{collectionName}/add`
- **Body:**
```json
{
  "ids": ["chunk-0", "chunk-1", "chunk-2"],
  "embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...], [0.5, 0.6, ...]],
  "documents": ["chunk content 0", "chunk content 1", "chunk content 2"],
  "metadatas": [
    { "chunkIndex": 0, "documentId": 1 },
    { "chunkIndex": 1, "documentId": 1 },
    { "chunkIndex": 2, "documentId": 1 }
  ]
}
```

#### Query Embeddings
- **Internal Operation:** `POST http://localhost:8000/api/v1/collections/{collectionName}/query`
- **Body:**
```json
{
  "query_embeddings": [[0.1, 0.2, ...]],
  "n_results": 5,
  "include": ["documents", "distances", "metadatas"]
}
```

---

## Appendix D: Glossary

| Term | Definition |
|------|------------|
| **Chunk** | A segment of a document created during RAG preprocessing |
| **Context Mode** | The method by which additional context is provided to the LLM |
| **Embedding** | A vector representation of text used for semantic similarity |
| **Experiment** | A configured test scenario with defined models and parameters |
| **Experiment Run** | A single execution instance within an experiment |
| **Hyperparameters** | Configuration values that control model generation behaviour |
| **Iteration** | A numbered repetition of a run configuration for statistical validity |
| **RAG** | Retrieval-Augmented Generation; enhancing prompts with retrieved context |
| **Task Template** | A reusable prompt definition with optional variable placeholders |
| **TTFT** | Time to First Token; latency before the model begins responding |
| **TPS** | Tokens Per Second; throughput metric for model generation |
