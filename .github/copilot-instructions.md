# LocalLab - AI Coding Agent Instructions

## Project Overview

LocalLab is a local-first LLM experimental framework for benchmarking and comparing models via Ollama. The core workflow: **Task Template → Experiment → N models × M configs × X iterations → Analytics**.

**Tech Stack:** Java 21 + Spring Boot 3.x (backend), React 18 + TypeScript + Vite (frontend), H2 database, Ollama (inference), Chroma (vector store).

## Architecture

```
React Frontend (localhost:5173)
       │ REST API (JSON) + WebSocket (STOMP)
       ▼
Spring Boot Backend (localhost:8080)
  Controller → Service → Repository
       │            │
       ▼            ▼
  OllamaClient  ChromaClient  H2 Database
  (:11434)      (:8000)       (./data/locallab.mv.db)
```

**Key Design Principles:**
- Controller → Service → Repository layering (no shortcuts)
- Interfaces for all external clients (`OllamaClient`, `ChromaClient`) to enable testing
- Fail fast: validate at API boundaries, throw early
- 95% test coverage target (backend), 80% (frontend)

## Essential Documentation

Reference these files for implementation details:
- `docs/Specification.md` - Complete architecture, data models, UI design, all 8 pages
- `docs/API-Contract.md` - DTOs, validation rules, REST endpoints, WebSocket messages
- `docs/ISSUES.md` - 67 sub-issues with dependencies and acceptance criteria

## Coding Standards

### Documentation
# IMPORTANT: ALWAYS READ DOCUMENTATION BEFORE ANY TASK
- **Javadoc on all public methods** - Include `@param`, `@return`, `@throws` tags
- **Docstrings for complex logic** - Explain the "why", not just the "what"
- **Inline comments** for non-obvious code paths

### Code Style
- **Legibility over brevity** - Prefer clear, well-spaced code over dense one-liners
- **UK English throughout** - Use "colour", "behaviour", "initialise", "serialisation"
- **Meaningful names** - `experimentRunRepository` not `expRunRepo`
- **Emoji Ban** - The use of emojis at any level throughout this project is strictly banned.

### Validation (Spring)
- **DTOs**: Use `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern` on request fields
- **Controllers**: Add `@Valid` on `@RequestBody` parameters
- **Custom validators**: Implement `ConstraintValidator` for complex rules (e.g., `chunkOverlap < chunkSize`)
- **Error responses**: `GlobalExceptionHandler` converts `MethodArgumentNotValidException` to field-level errors

Example DTO validation:
```java
public class TaskTemplateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Prompt template is required")
    @Size(max = 50000, message = "Prompt template must not exceed 50,000 characters")
    private String promptTemplate;
}
```

## Backend Conventions

### Package Structure
```
com.locallab/
├── controller/    # REST endpoints, @Valid on requests, @ControllerAdvice
├── service/       # Business logic, @Transactional
├── repository/    # Spring Data JPA interfaces
├── model/         # JPA entities + enums/
├── dto/           # request/ and response/ subdirectories (validated)
├── client/        # OllamaClient, ChromaClient interfaces + impls
├── config/        # WebConfig, WebSocketConfig, OllamaConfig
└── exception/     # LocalLabException, GlobalExceptionHandler
```

### Entity Patterns
- All entities use `@Data` (Lombok), `@Entity`, `@Id @GeneratedValue`
- Timestamps: `@CreationTimestamp` for `createdAt` fields
- JSON storage: `@Lob` for config fields storing JSON strings
- Enums: `ExperimentStatus` (DRAFT, RUNNING, PAUSED, COMPLETED, FAILED), `RunStatus` (PENDING, RUNNING, SUCCESS, FAILED)

### Testing Strategy
| Layer | Approach |
|-------|----------|
| Controller | `@WebMvcTest` + MockMvc |
| Service | Unit tests with `@Mock` + Mockito |
| Repository | `@DataJpaTest` with H2 |
| Client | WireMock for Ollama/Chroma |

## Frontend Conventions

### Directory Structure
```
src/
├── api/           # Axios client + endpoint modules
├── components/    # common/, experiments/, results/
├── pages/         # Route-level components
├── hooks/         # useOllama, useExperiment, useWebSocket
├── store/         # Zustand stores (configStore)
└── types/         # TypeScript interfaces matching API contract
```

### State Management
- **Zustand** for global config (model, temperature, topP, topK, etc.)
- React Query or custom hooks for server state
- WebSocket via STOMP for experiment progress (`/topic/experiments/{id}/progress`)

### UI Components
- **Radix UI** for accessible primitives
- **TailwindCSS** for styling (utility-first)
- **Recharts** for analytics visualisation
- **ReactFlow** for pipeline visualisation (static layout, future drag-drop)

## Key Implementation Patterns

### Prompt Templates
Use `{{variable}}` syntax for placeholders in `TaskTemplate.promptTemplate`. Extract variables with regex: `/\{\{(\w+)\}\}/g`

### Experiment Execution Flow
1. Create `Experiment` (status: DRAFT)
2. `POST /experiments/{id}/start` → status: RUNNING
3. `ExperimentExecutorService` generates run matrix: models × embeddings × iterations
4. Each `ExperimentRun` persists immediately after completion
5. WebSocket broadcasts: `RUN_STARTED`, `RUN_COMPLETED`, `PROGRESS`, `EXPERIMENT_COMPLETED`

### RAG Pipeline
1. Upload document → extract text → chunk (configurable size/overlap)
2. Generate embeddings via Ollama (`/api/embeddings`)
3. Store in Chroma collection (`doc-{id}-{embeddingModel}`)
4. Query: embed query → vector search → retrieve top-K chunks → inject into prompt

## Commands

```bash
# Backend
cd backend && mvn spring-boot:run          # Start on :8080
mvn clean verify                           # Build + test + coverage
mvn checkstyle:check                       # Style validation

# Frontend
cd frontend && npm run dev                 # Start on :5173
npm run build                              # Production build
npm run lint && npm run test               # Quality checks

# Infrastructure
docker-compose up -d chroma                # Start Chroma on :8000
```

## Common Pitfalls

- **Ollama must be running** before backend starts (check with `GET /api/ollama/status`)
- **CORS**: Backend allows `localhost:5173` in dev profile
- **H2 Console**: Access at `/h2-console` (JDBC URL: `jdbc:h2:file:./data/locallab`)
- **Experiment updates** only allowed when status is `DRAFT`
- **WebSocket** uses STOMP protocol - connect to `/ws`, subscribe to `/topic/experiments/{id}/progress`
