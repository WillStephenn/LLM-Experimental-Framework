# LocalLab - AI Coding Agent Instructions

## CRITICAL: Pre-Task Requirements

**Before starting ANY task, you MUST:**

1. **MUST READ (Mandatory):**
   - `docs/Specification.md` - Complete architecture, data models, UI design
   - `docs/API-Contract.md` - DTOs, validation rules, REST endpoints, WebSocket messages

2. **SHOULD READ (Context-Dependent):**
   - Relevant existing classes before creating new ones
   - Exception handlers: `GlobalExceptionHandler`, `ErrorResponse`
   - Any existing services, repositories, or controllers in the affected domain

3. **TEST CASES (Jacoco-Driven):**
   - Only write test cases for classes with custom logic OR classes that appear in Jacoco test coverage reports
   - Do NOT write tests for pure Lombok-generated classes (entities with only @Data, DTOs with no custom logic)
   - No task is complete until all tests pass
   - No task is complete until `mvn clean verify` passes (backend)
   - No task is complete until the full frontend CI chain passes: `npm ci && npm run lint && npm run format:check && npx tsc --noEmit && npm run test && npm run build` (frontend)

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

---

## Coding Agent Task Execution Protocol

**When working on any frontend issue (especially 7.* sub-issues), follow this protocol:**

### Pre-Implementation Checklist
1. **Read `docs/Specification.md`** - Understand the page/component requirements
2. **Read `docs/API-Contract.md`** - Know the exact DTOs and endpoints you'll consume
3. **Read `docs/ISSUES.md`** - Check dependencies and acceptance criteria for your specific sub-issue
4. **Review existing components** - Check `components/common/` for reusable primitives before creating new ones
5. **Verify API endpoints exist** - If backend endpoints don't exist yet, create mock data

### Implementation Checklist
1. **Create TypeScript types first** - Define interfaces in `types/` before writing components
2. **Follow component structure exactly** - Use the patterns in the Frontend Style Guide section
3. **Use existing common components** - `Button`, `Card`, `Input`, `Select`, `Table`, `Modal`, etc.
4. **Add loading and error states** - Every data-fetching component needs both
5. **Write tests alongside code** - Co-locate test files with components
6. **Run `npm run lint` and `npm run test`** before considering the task complete

### Quality Gates
- **Full CI Check**: Must pass `npm ci && npm run lint && npm run format:check && npx tsc --noEmit && npm run test && npm run build`
- **No TypeScript errors**: `npx tsc --noEmit` must pass
- **No lint errors**: `npm run lint` must pass
- **Formatting**: `npm run format:check` must pass
- **All tests pass**: `npm run test` must pass
- **Builds successfully**: `npm run build` must pass
- **Coverage met**: 80% line coverage for new code

### File Naming Conventions
| Type | Pattern | Example |
|------|---------|--------|
| Component | PascalCase.tsx | `ExperimentCard.tsx` |
| Test | PascalCase.test.tsx | `ExperimentCard.test.tsx` |
| Hook | camelCase.ts | `useExperiment.ts` |
| Type | camelCase.ts | `experiment.ts` |
| Utility | camelCase.ts | `formatters.ts` |
| Page | PascalCase.tsx | `ExperimentsPage.tsx` |
| Store | camelCase.ts | `experimentStore.ts` |

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
├── exception/     # GlobalExceptionHandler
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
└── exception/     # GlobalExceptionHandler
```

### Exception Handling (MUST USE)
The project uses Spring's standard exceptions for clearer semantics and self-documenting code. Always use these patterns:

**Standard Exceptions by Use Case:**

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `EntityNotFoundException` | 404 | Resource not found (e.g., collection, entity) |
| `IllegalArgumentException` | 400 | Invalid input or parameter values |
| `IllegalStateException` | 409 | Conflict (e.g., resource already exists, invalid state transition) |
| `ResponseStatusException` | Variable | External service errors, connection failures |

**Client Layer Examples (OllamaClient, ChromaClient):**
```java
// For NOT_FOUND errors - use EntityNotFoundException
throw new EntityNotFoundException("Collection not found: " + name);

// For CONFLICT errors - use IllegalStateException
throw new IllegalStateException("Collection already exists: " + name);

// For SERVICE_UNAVAILABLE or other status codes - use ResponseStatusException
throw new ResponseStatusException(
    HttpStatus.SERVICE_UNAVAILABLE,
    "Cannot connect to Chroma: " + e.getMessage(),
    e);

// For BAD_REQUEST from external services
throw new ResponseStatusException(
    HttpStatus.BAD_REQUEST,
    "Ollama generate failed: model 'invalid-model' not found",
    e);
```

**GlobalExceptionHandler** handles:
- `ResponseStatusException` - Returns appropriate HTTP status from exception
- `EntityNotFoundException` - 404 Not Found
- `IllegalArgumentException` - 400 Bad Request
- `IllegalStateException` - 409 Conflict
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
- Frontend: 80% line coverage target (enforced by Vitest)
- Coverage targets apply only to classes with custom logic (not Lombok-generated boilerplate)
- **No new code merged without meeting coverage thresholds**

**Frontend Testing Priorities:**
1. **Hooks with business logic** - useExperiment, useWebSocket, etc.
2. **Components with interactivity** - forms, buttons, modals
3. **Data transformation utilities** - formatters, validators
4. **Page components** - integration tests for user flows
5. **Pure presentational components** - lowest priority, test if time permits

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
├── api/             # Axios client + endpoint modules
│   ├── client.ts    # Axios instance with interceptors
│   ├── experiments.ts
│   ├── taskTemplates.ts
│   ├── documents.ts
│   └── ollama.ts
├── components/      # Reusable UI components
│   ├── common/      # Shared primitives (Button, Card, Input, Modal, etc.)
│   ├── layout/      # Layout components (Sidebar, Header, PageContainer)
│   ├── experiments/ # Experiment-specific components
│   ├── results/     # Results & analytics components
│   └── documents/   # Document management components
├── pages/           # Route-level page components (one per route)
├── hooks/           # Custom React hooks
│   ├── useOllama.ts
│   ├── useExperiment.ts
│   ├── useWebSocket.ts
│   └── useApi.ts    # Generic API hook with loading/error states
├── store/           # Zustand stores
│   ├── configStore.ts
│   └── experimentStore.ts
├── types/           # TypeScript interfaces (mirror API contract DTOs)
│   ├── api.ts       # Request/Response types
│   ├── models.ts    # Domain model types
│   └── common.ts    # Shared utility types
└── utils/           # Pure utility functions
    ├── formatters.ts
    ├── validators.ts
    └── constants.ts
```

### State Management
- **Zustand** for global config (model, temperature, topP, topK, etc.)
- **TanStack Query (React Query)** for server state with caching
- **WebSocket via STOMP** for experiment progress (`/topic/experiments/{id}/progress`)
- Local component state for UI-only concerns (form inputs, toggles, modals)

### UI Components
- **Radix UI** for accessible primitives (Dialog, Select, Tabs, etc.)
- **TailwindCSS** for styling (utility-first, no custom CSS unless absolutely necessary)
- **Recharts** for analytics visualisation (charts, graphs)
- **ReactFlow** for pipeline visualisation (static layout, future drag-drop)

---

## MANDATORY: Frontend Style Guide

**Before writing ANY frontend code, you MUST read `docs/Frontend-Style-Guide.md`.**

This style guide is LAW. All frontend components MUST adhere to these standards without exception. The guide contains:
- Design tokens and Tailwind configuration
- Component structure standards
- Tailwind class ordering rules
- Common component patterns (Button, Card, StatCard, etc.)
- Spacing and layout rules
- Typography scale
- Responsive design rules
- Accessibility requirements
- DRY/KISS principles with examples
- Extensibility patterns

**Do not proceed with frontend implementation until you have read and understood the style guide.**

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

### Frontend Page Patterns

**Page component structure (all pages MUST follow):**
```tsx
// pages/ExperimentsPage.tsx
import { PageContainer } from '@/components/layout/PageContainer';
import { PageHeader } from '@/components/layout/PageHeader';

export const ExperimentsPage = () => {
  // 1. Route params & navigation
  const { id } = useParams();
  const navigate = useNavigate();

  // 2. Data fetching
  const { data, isLoading, error } = useExperiments();

  // 3. Local state
  const [filter, setFilter] = useState('');

  // 4. Derived state
  const filteredData = useMemo(() => 
    data?.filter(e => e.name.includes(filter)), 
    [data, filter]
  );

  // 5. Error/Loading states
  if (error) return <ErrorState error={error} />;
  if (isLoading) return <LoadingState />;

  // 6. Render
  return (
    <PageContainer>
      <PageHeader
        title="Experiments"
        action={<Button onClick={() => navigate('/experiments/new')}>New Experiment</Button>}
      />
      <ExperimentsList experiments={filteredData} />
    </PageContainer>
  );
};
```

**API hook pattern:**
```tsx
// hooks/useExperiments.ts
export const useExperiments = () => {
  return useQuery({
    queryKey: ['experiments'],
    queryFn: () => api.experiments.getAll(),
    staleTime: 30_000, // 30 seconds
  });
};

export const useExperiment = (id: string) => {
  return useQuery({
    queryKey: ['experiments', id],
    queryFn: () => api.experiments.getById(id),
    enabled: !!id,
  });
};

export const useCreateExperiment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: api.experiments.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['experiments'] });
    },
  });
};
```

**Form pattern (React Hook Form):**
```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  description: z.string().max(500).optional(),
});

type FormData = z.infer<typeof schema>;

export const ExperimentForm = ({ onSubmit }: Props) => {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <FormField
        label="Name"
        error={errors.name?.message}
        {...register('name')}
      />
    </form>
  );
};
```

### WebSocket Integration Pattern
```tsx
// hooks/useExperimentProgress.ts
export const useExperimentProgress = (experimentId: string) => {
  const [progress, setProgress] = useState<ExperimentProgress | null>(null);
  const client = useRef<Client | null>(null);

  useEffect(() => {
    if (!experimentId) return;

    client.current = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      onConnect: () => {
        client.current?.subscribe(
          `/topic/experiments/${experimentId}/progress`,
          (message) => {
            const data = JSON.parse(message.body);
            setProgress(data);
          }
        );
      },
    });

    client.current.activate();

    return () => {
      client.current?.deactivate();
    };
  }, [experimentId]);

  return progress;
};
```

## Commands

```bash
# Backend
cd backend && mvn spring-boot:run          # Start on :8080
mvn clean verify                           # Build + test + coverage
mvn checkstyle:check                       # Style validation

# Frontend
cd frontend && npm install                 # Install dependencies
npm run dev                                # Start on :5173
npm run build                              # Production build
npm run lint                               # ESLint check
npm run lint:fix                           # ESLint auto-fix
npm run test                               # Run Vitest tests
npm run test:coverage                      # Run tests with coverage
npm run format:check                       # Check formatting (Prettier)
npx tsc --noEmit                           # TypeScript type checking

# Infrastructure
docker-compose up -d chroma                # Start Chroma on :8000
```

## Common Pitfalls

### Backend
- **Ollama must be running** before backend starts (check with `GET /api/ollama/status`)
- **CORS**: Backend allows `localhost:5173` in dev profile
- **H2 Console**: Access at `/h2-console` (JDBC URL: `jdbc:h2:file:./data/locallab`)
- **Experiment updates** only allowed when status is `DRAFT`
- **WebSocket** uses STOMP protocol - connect to `/ws`, subscribe to `/topic/experiments/{id}/progress`

### Frontend
- **TypeScript strict mode** is enabled - never use `any` type, always define proper interfaces
- **Never inline styles** - always use Tailwind classes
- **Never use `index` as React key** - use unique identifiers from data
- **Always handle loading and error states** - every API call needs both
- **Forms must have proper validation** - use Zod schemas with react-hook-form
- **All text must support UK English** - use "colour", "behaviour", "initialise"
- **Test IDs pattern**: `data-testid="component-name-element"` (e.g., `data-testid="experiment-card-title"`)
- **Import paths**: Use `@/` alias for src directory imports

### Testing Requirements (Frontend)
- **Component tests**: Use React Testing Library, test behaviour not implementation
- **API mocking**: Use MSW (Mock Service Worker) for API tests
- **Coverage target**: 80% line coverage
- **Test file location**: Co-located with component (e.g., `Button.tsx` + `Button.test.tsx`)
- **Naming convention**: `describe('ComponentName')` → `it('should do something')`
