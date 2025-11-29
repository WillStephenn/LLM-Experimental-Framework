# LocalLab - AI Coding Agent Instructions

## CRITICAL: Pre-Task Requirements

**Before starting ANY task, you MUST:**

1. **MUST READ (Mandatory):**
   - `docs/Specification.md` - Complete architecture, data models, UI design
   - `docs/API-Contract.md` - DTOs, validation rules, REST endpoints, WebSocket messages

2. **SHOULD READ (Context-Dependent):**
   - Relevant existing classes before creating new ones
   - Exception handlers: `LocalLabException`, `GlobalExceptionHandler`, `ErrorResponse`
   - Any existing services, repositories, or controllers in the affected domain

3. **TEST CASES (Jacoco-Driven):**
   - Only write test cases for classes with custom logic OR classes that appear in Jacoco test coverage reports
   - Do NOT write tests for pure Lombok-generated classes (entities with only @Data, DTOs with no custom logic)
   - No task is complete until all tests pass
   - No task is complete until `mvn clean verify` passes (backend)
   - No task is complete until `npm run build` and `npm run test` pass (frontend)

**A task is NOT complete unless:**
- All existing tests pass
- Test cases are written for classes that Jacoco identifies as requiring coverage
- Code passes checkstyle validation
- Code compiles without errors
- Coverage targets are met (95% backend, 80% frontend) for classes that contain custom logic

## Project Overview

LocalLab is a local-first LLM experimental framework for benchmarking and comparing models via Ollama. The core workflow: **Task Template -> Experiment -> N models x M configs x X iterations -> Analytics**.

**Tech Stack:** Java 21 + Spring Boot 3.x (backend), React 18 + TypeScript + Vite (frontend), H2 database, Ollama (inference), Chroma (vector store).

## Architecture

```
React Frontend (localhost:5173)
       | REST API (JSON) + WebSocket (STOMP)
       v
Spring Boot Backend (localhost:8080)
  Controller -> Service -> Repository
       |            |
       v            v
  OllamaClient  ChromaClient  H2 Database
  (:11434)      (:8000)       (./data/locallab.mv.db)
```

**Key Design Principles:**
- Controller -> Service -> Repository layering (no shortcuts)
- Interfaces for all external clients (`OllamaClient`, `ChromaClient`) to enable testing
- Fail fast: validate at API boundaries, throw early
- 95% test coverage target (backend), 80% (frontend)

## Essential Documentation

Reference these files for implementation details:
- `docs/Specification.md` - Complete architecture, data models, UI design, all 8 pages
- `docs/API-Contract.md` - DTOs, validation rules, REST endpoints, WebSocket messages
- `docs/ISSUES.md` - Sub-issues with dependencies and acceptance criteria

## IMPORTANT: Git Restrictions

Only read-only git interactions are permitted. Do not perform any git write operations (commit, push, branch, merge, etc.) in this project. No updating, modifying or closing issues.

## Sub-Issue Push Workflow

When tasked with pushing sub-issues to the repo, always:
- Gather existing issues for style reference
- Convert to UK English
- Remove enumeration from the title (but maintain it in the label 'child-x')
- Assign it to WillStephenn

## Coding Standards

### Documentation
- **Javadoc on all public methods** - Include `@param`, `@return`, `@throws` tags
- **Docstrings for complex logic** - Explain the "why", not just the "what"
- **Inline comments** for non-obvious code paths

### Code Style
- **Legibility over brevity** - Prefer clear, well-spaced code over dense one-liners
- **UK English throughout** - Use "colour", "behaviour", "initialise", "serialisation"
- **Meaningful names** - `experimentRunRepository` not `expRunRepo`
- **No emojis** - The use of emojis at any level throughout this project is strictly banned
- **Lombok over boilerplate** - Always use Lombok annotations (`@Data`, `@Getter`, `@Setter`, `@Builder`) instead of manual getters, setters, constructors, and other boilerplate

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

### Package Structure (Current State)
```
com.locallab/
├── config/        # WebConfig, CorsProperties
├── dto/           # response/ subdirectory (ErrorResponse)
├── exception/     # LocalLabException, GlobalExceptionHandler
└── LocalLabApplication.java
```

### Package Structure (Target)
```
com.locallab/
├── controller/    # REST endpoints, @Valid on requests
├── service/       # Business logic, @Transactional
├── repository/    # Spring Data JPA interfaces
├── model/         # JPA entities + enums/
├── dto/           # request/ and response/ subdirectories (validated)
├── client/        # OllamaClient, ChromaClient interfaces + impls
├── config/        # WebConfig, WebSocketConfig, OllamaConfig, CorsProperties
└── exception/     # LocalLabException, GlobalExceptionHandler
```

### Exception Handling (MUST USE)
The project has established exception handling patterns. Always use these:

**LocalLabException** - Application-specific exceptions with HTTP status:
```java
// For specific HTTP status
throw new LocalLabException("Resource not found", HttpStatus.NOT_FOUND);

// For 500 Internal Server Error (default)
throw new LocalLabException("Something went wrong");

// With cause
throw new LocalLabException("Failed to process", cause, HttpStatus.BAD_REQUEST);
```

**GlobalExceptionHandler** handles:
- `LocalLabException` - Returns appropriate HTTP status with ErrorResponse
- `MethodArgumentNotValidException` - Bean validation errors (400)
- `MethodArgumentTypeMismatchException` - Type conversion errors (400)
- `NoHandlerFoundException` - Unknown endpoints (404)
- `Exception` - Catch-all for unexpected errors (500)

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

**Coverage Requirements:**
- Backend: 95% line coverage target
- Frontend: 80% line coverage target
- Coverage targets apply only to classes with custom logic (not Lombok-generated boilerplate)

**Jacoco-Driven Testing Approach:**
- **Let Jacoco guide test creation** - only write tests for classes that appear in Jacoco coverage reports
- **Skip pure Lombok classes** - entities/DTOs with only `@Data`/`@Getter`/`@Setter` and no custom logic do not need tests
- **Skip pure enums** - simple enums without custom methods do not need tests
- **Focus on custom logic** - test constructors, builders, validation logic, and business methods you write
- **Run `mvn verify` first** - check the Jacoco report to identify which classes need test coverage

**What NOT to Test:**
- Lombok-generated getters/setters/equals/hashCode/toString
- Pure JPA entities with no custom methods
- DTOs that are just data carriers with no logic
- Simple enums without custom behaviour
- Java language features (`valueOf()`, `name()`, `ordinal()`, etc.)

**What TO Test:**
- Controllers (request/response handling, validation)
- Services (business logic, error handling)
- Repositories (custom queries only)
- Clients (external integrations)
- Any class with custom constructors, builders, or methods
- Exception classes with custom logic

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
