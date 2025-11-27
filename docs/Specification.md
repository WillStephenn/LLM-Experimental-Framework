
# Project Specification: LLM Experimental Framework

**Version:** 2.0  
**Codename:** LocalLab  
**Target:** Small, locally-runnable LLMs via Ollama

---

## 1. Executive Summary

LocalLab is a local-first experimental framework for rigorously testing, benchmarking, and comparing small Large Language Models (LLMs). Unlike chat playgrounds, this system prioritises **reproducibility**, **transparency**, and **comprehensive experimentation**.

### Core Value Proposition
Define a task → Run it across N models × M configurations × X iterations → Analyse results → Determine the optimal model for your use case.

### Key Differentiators
- **Visual Pipeline Builder**: Configure experiments as visual node graphs
- **RAG Embedding Comparison**: Test retrieval quality across different embedding models
- **Full Transparency**: Every stage of processing is inspectable
- **Experiment-First Design**: Built for systematic evaluation, not casual chat

---

## 2. System Architecture

### 2.1 Technology Stack

| Layer | Technology | Rationale |
|-------|------------|-----------|
| **Backend** | Java 21 + Spring Boot 3.x | Type safety, mature ecosystem, familiar to maintainer |
| **Frontend** | React 18 + TypeScript | Modern UI capabilities, learning opportunity |
| **Database** | H2 (embedded) | Zero-config, Spring Data JPA compatible |
| **Inference** | Ollama (via `ollama4j`) | Local-first, simple API |
| **Vector Store** | Chroma (via REST API) | Local embeddings, multiple embedding model support |
| **Build** | Maven (backend) + Vite (frontend) | Industry standard tooling |
| **Quality** | Checkstyle + JaCoCo + ESLint + Prettier | Enforced code standards |

### 2.2 Architecture Principles

```
┌─────────────────────────────────────────────────────────────┐
│                      React Frontend                         │
│  (TypeScript, Recharts, ReactFlow, TailwindCSS)            │
└─────────────────────┬───────────────────────────────────────┘
                      │ REST API (JSON)
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Controller  │→ │  Service    │→ │ Repository  │         │
│  │   Layer     │  │   Layer     │  │   Layer     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│         │                │                │                 │
│         ▼                ▼                ▼                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Ollama    │  │   Chroma    │  │     H2      │         │
│  │   Client    │  │   Client    │  │  Database   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 Design Principles

1. **Separation of Concerns**: Controller → Service → Repository pattern throughout
2. **Fail Fast**: Validate inputs at boundaries; throw early, catch at appropriate levels
3. **DRY**: Extract shared logic into utility classes; avoid copy-paste
4. **Extensibility**: Use interfaces for all external integrations (Ollama, Chroma)
5. **Documentation**: Every public method has Javadoc; complex logic has inline comments

---

## 3. Data Model

### 3.1 Entity Relationship Diagram

```
┌──────────────────┐       ┌──────────────────┐
│   TaskTemplate   │       │   SystemPrompt   │
├──────────────────┤       ├──────────────────┤
│ id (PK)          │       │ id (PK)          │
│ name             │       │ alias            │
│ description      │       │ content          │
│ promptTemplate   │       │ createdAt        │
│ tags             │       └──────────────────┘
│ evaluationNotes  │
│ createdAt        │
└────────┬─────────┘
         │ 1
         │
         │ *
┌────────▼─────────┐       ┌──────────────────┐
│    Experiment    │       │  EmbeddingModel  │
├──────────────────┤       ├──────────────────┤
│ id (PK)          │       │ id (PK)          │
│ name             │       │ name             │
│ taskTemplateId   │───┐   │ ollamaModelName  │
│ status           │   │   │ dimensions       │
│ config (JSON)    │   │   └──────────────────┘
│ createdAt        │   │
└────────┬─────────┘   │
         │ 1           │
         │             │
         │ *           │
┌────────▼─────────┐   │   ┌──────────────────┐
│ ExperimentRun    │   │   │    Document      │
├──────────────────┤   │   ├──────────────────┤
│ id (PK)          │   │   │ id (PK)          │
│ experimentId     │   │   │ filename         │
│ modelName        │   │   │ content          │
│ embeddingModel   │───┘   │ chunkCount       │
│ systemPromptId   │       │ createdAt        │
│ iteration        │       └──────────────────┘
│ config (JSON)    │
│ status           │
│ output           │
│ durationMs       │
│ tokensPerSecond  │
│ timeToFirstToken │
│ retrievedChunks  │
│ errorMessage     │
│ timestamp        │
└──────────────────┘
```

### 3.2 Entity Definitions

#### TaskTemplate
Reusable task definitions that can be applied across multiple experiments.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `name` | String | Short identifier (e.g., "Code Review Task") |
| `description` | Text | What this task evaluates |
| `promptTemplate` | Text | The prompt with `{{variable}}` placeholders |
| `tags` | String | Comma-separated categories |
| `evaluationNotes` | Text | Optional human reference notes for evaluation criteria |
| `createdAt` | Timestamp | Creation time |

#### Experiment
A configured test scenario linking a task to execution parameters.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `name` | String | Experiment name |
| `taskTemplateId` | Long | FK to TaskTemplate |
| `status` | Enum | `DRAFT`, `RUNNING`, `COMPLETED`, `FAILED` |
| `config` | JSON | Hyperparameters, model list, iteration count |
| `createdAt` | Timestamp | Creation time |

#### ExperimentRun
Individual execution result within an experiment.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `experimentId` | Long | FK to Experiment |
| `modelName` | String | Ollama model used |
| `embeddingModelId` | Long | FK to EmbeddingModel (nullable) |
| `systemPromptId` | Long | FK to SystemPrompt (nullable) |
| `iteration` | Integer | Run number (1 to N) |
| `config` | JSON | Exact parameters used |
| `status` | Enum | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED` |
| `output` | Text | Model response |
| `durationMs` | Long | Total generation time |
| `tokensPerSecond` | Double | TPS metric |
| `timeToFirstTokenMs` | Long | TTFT metric |
| `retrievedChunks` | JSON | RAG chunks used (if applicable) |
| `errorMessage` | Text | Error details (if failed) |
| `timestamp` | Timestamp | Execution time |

---

## 4. Global UI Configuration

Every page must include access to these configurable options via a collapsible settings panel:

### 4.1 Model Configuration
- **Model Selector**: Dropdown populated from `GET /api/ollama/models`
- **Temperature**: Slider (0.0 – 2.0, step 0.1)
- **Top P**: Slider (0.0 – 1.0, step 0.05)
- **Top K**: Number input (1 – 100)
- **Context Window**: Number input (512 – 128000)
- **Max Tokens**: Number input (optional cap on response length)

### 4.2 System Prompt Configuration
- **Preset Selector**: Dropdown from saved SystemPrompts
- **Custom Input**: Text area for ad-hoc system prompts
- **Preview**: Rendered view of the active system prompt

### 4.3 RAG Configuration (where applicable)
- **Embedding Model**: Dropdown of available embedding models
- **Chunk Size**: Slider (100 – 2000 tokens)
- **Chunk Overlap**: Slider (0 – 500 tokens)
- **Top K Retrieval**: Slider (1 – 20)
- **Context Injection Mode**: Toggle between "Retrieved chunks only" and "Full document"

---

## 5. Functional Modules

### 5.1 Page: Sandbox
**Purpose**: Quick, ad-hoc interactions for sanity checks.

**Features**:
- Chat interface with message history
- Full configuration panel (model, temperature, system prompt)
- Metrics display per response (TPS, duration, TTFT)
- JSON mode toggle for structured output
- Code syntax highlighting with copy button
- Option to save interaction as a new TaskTemplate

### 5.2 Page: Arena
**Purpose**: Side-by-side qualitative comparison of two models.

**Layout**: Two-column split view

**Features**:
- Independent model selection per column
- Shared prompt input (sends to both simultaneously)
- Diff view highlighting textual differences
- Human evaluation buttons: "Left wins", "Right wins", "Tie"
- Evaluation saved to session log for later export
- Full configuration panel per model

### 5.3 Page: RAG Lab
**Purpose**: Test retrieval-augmented generation with full transparency.

**Workflow**:
1. **Ingest**: Upload PDF/TXT → chunk → embed → store in Chroma
2. **Configure**: Select embedding model, chunk size, retrieval K
3. **Query**: Ask questions against the document

**Transparency Features**:
- **X-Ray Panel**: Expandable section showing exact retrieved chunks
- **Relevance Scores**: Vector distance for each chunk
- **Embedding Model Comparison**: Run same query with different embedders, compare retrieved chunks
- **Context Preview**: See exactly what is sent to the LLM

### 5.4 Page: Task Library
**Purpose**: Manage reusable task templates and system prompts.

**Features**:
- Tabbed view: Task Templates | System Prompts
- Data table with inline editing
- Create/Edit modal with:
  - Name, description, tags
  - Prompt template with `{{variable}}` syntax
  - Evaluation notes (optional reference for human assessment)
- Import/Export as JSON
- Tag filtering and search

### 5.5 Page: Experiment Builder (Visual Pipeline)
**Purpose**: Configure comprehensive experiments visually.

**Pipeline Visualisation**:
```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  Task   │ → │ Context │ → │  Model  │ → │ Output  │
│ Template│    │  (RAG?) │    │  Pool   │    │ Collect │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
```

**Configuration Steps**:
1. **Select Task**: Choose from TaskTemplate library
2. **Context Mode**: 
   - None (prompt only)
   - RAG (select document + embedding model)
   - Full Context (paste/upload complete context)
3. **Model Pool**: Multi-select models to test
4. **Embedding Pool** (if RAG): Multi-select embedding models to compare
5. **Iterations**: Number of runs per combination
6. **Hyperparameters**: Set ranges or fixed values

**Experiment Matrix Preview**:
Shows total runs: `Tasks × Models × Embeddings × Iterations`

**Extensibility Note**: The pipeline visualisation currently uses a static node layout. The component architecture should support future enhancement to a full drag-and-drop node editor using ReactFlow.

### 5.6 Page: Experiment Runner
**Purpose**: Execute experiments with real-time feedback.

**Features**:
- Experiment queue management
- Real-time progress dashboard:
  - Overall progress bar
  - Current run details
  - Live log stream
- Pause/Resume/Cancel controls
- Auto-save: Each run persists immediately upon completion
- Error resilience: Failed runs are logged and execution continues

### 5.7 Page: Results Dashboard
**Purpose**: Analyse experiment outcomes comprehensively.

**Views**:

#### Leaderboard View
- Bar charts: Average TPS, Average Latency, Success Rate
- Filterable by experiment, model, embedding model
- Sortable columns

#### Comparison Matrix
- Pivot table: Rows = Prompts/Tasks, Columns = Models
- Cell content: Truncated output with expand-on-click
- Colour coding by performance metrics

#### Deep Dive Modal
Clicking any result opens a modal showing:
- Full prompt sent (with resolved variables)
- Complete model output
- System prompt used
- All configuration parameters
- Performance metrics
- Retrieved chunks (if RAG)
- Error details (if failed)

#### Export Options
- CSV export (filtered or full)
- JSON export (complete experiment data)

### 5.8 Page: Embedding Analyser
**Purpose**: Compare retrieval quality across embedding models.

**Features**:
- Select a document and query
- Run retrieval with multiple embedding models
- Side-by-side comparison of retrieved chunks
- Relevance score visualisation
- Overlap analysis: Which chunks appear across all embeddings?

---

## 6. API Design

### 6.1 REST Endpoints

#### Ollama Integration
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ollama/models` | List available models |
| GET | `/api/ollama/status` | Check Ollama connectivity |
| POST | `/api/ollama/generate` | Single generation request |

#### Task Templates
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all task templates |
| GET | `/api/tasks/{id}` | Get single task |
| POST | `/api/tasks` | Create task template |
| PUT | `/api/tasks/{id}` | Update task template |
| DELETE | `/api/tasks/{id}` | Delete task template |

#### System Prompts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/system-prompts` | List all |
| POST | `/api/system-prompts` | Create |
| PUT | `/api/system-prompts/{id}` | Update |
| DELETE | `/api/system-prompts/{id}` | Delete |

#### Experiments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/experiments` | List experiments |
| GET | `/api/experiments/{id}` | Get with all runs |
| POST | `/api/experiments` | Create experiment |
| POST | `/api/experiments/{id}/start` | Begin execution |
| POST | `/api/experiments/{id}/pause` | Pause execution |
| POST | `/api/experiments/{id}/cancel` | Cancel execution |
| DELETE | `/api/experiments/{id}` | Delete experiment |

#### Results
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/experiments/{id}/runs` | Get all runs |
| GET | `/api/runs/{id}` | Get single run details |
| GET | `/api/experiments/{id}/export` | Export as CSV/JSON |

#### RAG
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/documents` | Upload and process document |
| GET | `/api/documents` | List documents |
| DELETE | `/api/documents/{id}` | Remove document |
| POST | `/api/documents/{id}/query` | Query with RAG |
| GET | `/api/embedding-models` | List available embedders |

### 6.2 WebSocket Endpoints
| Endpoint | Description |
|----------|-------------|
| `/ws/experiments/{id}` | Real-time experiment progress updates |

---

## 7. UI Design Guidelines

### 7.1 Design Philosophy
- **Bright & Airy**: Generous whitespace, light backgrounds
- **Simplistic**: One primary action per view, progressive disclosure
- **Modern**: Rounded corners, subtle shadows, smooth animations
- **Functional**: Every element serves a purpose

### 7.2 Colour Palette
```
Primary:     #0066FF (Vibrant blue)
Secondary:   #00C853 (Success green)
Accent:      #FF6B35 (Warm orange)
Background:  #FAFBFC (Off-white)
Surface:     #FFFFFF (Pure white)
Text:        #1A1A2E (Near black)
Muted:       #6B7280 (Grey)
Error:       #EF4444 (Red)
```

### 7.3 Typography
- **Font Family**: Inter (headings), JetBrains Mono (code)
- **Scale**: 12px base, 1.25 ratio

### 7.4 Component Library
Use a combination of:
- **Radix UI**: Accessible primitives
- **TailwindCSS**: Utility-first styling
- **Recharts**: Data visualisation
- **ReactFlow**: Pipeline visualisation (future drag-and-drop)

### 7.5 Responsive Behaviour
- Primary target: Desktop (1440px+)
- Functional at: 1024px minimum
- Mobile: Not a priority (local development tool)

---

## 8. Project Structure

```
locallab/
├── README.md
├── docker-compose.yml          # Orchestrates backend + Chroma
├── .gitignore
│
├── backend/
│   ├── pom.xml
│   ├── checkstyle.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/locallab/
│   │   │   │   ├── LocalLabApplication.java
│   │   │   │   │
│   │   │   │   ├── config/
│   │   │   │   │   ├── WebConfig.java
│   │   │   │   │   ├── WebSocketConfig.java
│   │   │   │   │   └── OllamaConfig.java
│   │   │   │   │
│   │   │   │   ├── controller/
│   │   │   │   │   ├── OllamaController.java
│   │   │   │   │   ├── TaskController.java
│   │   │   │   │   ├── SystemPromptController.java
│   │   │   │   │   ├── ExperimentController.java
│   │   │   │   │   ├── ResultController.java
│   │   │   │   │   └── DocumentController.java
│   │   │   │   │
│   │   │   │   ├── service/
│   │   │   │   │   ├── OllamaService.java
│   │   │   │   │   ├── TaskService.java
│   │   │   │   │   ├── ExperimentService.java
│   │   │   │   │   ├── ExperimentExecutorService.java
│   │   │   │   │   ├── RagService.java
│   │   │   │   │   └── AnalyticsService.java
│   │   │   │   │
│   │   │   │   ├── repository/
│   │   │   │   │   ├── TaskTemplateRepository.java
│   │   │   │   │   ├── SystemPromptRepository.java
│   │   │   │   │   ├── ExperimentRepository.java
│   │   │   │   │   ├── ExperimentRunRepository.java
│   │   │   │   │   └── DocumentRepository.java
│   │   │   │   │
│   │   │   │   ├── model/
│   │   │   │   │   ├── TaskTemplate.java
│   │   │   │   │   ├── SystemPrompt.java
│   │   │   │   │   ├── Experiment.java
│   │   │   │   │   ├── ExperimentRun.java
│   │   │   │   │   ├── Document.java
│   │   │   │   │   └── enums/
│   │   │   │   │       ├── ExperimentStatus.java
│   │   │   │   │       └── RunStatus.java
│   │   │   │   │
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   └── response/
│   │   │   │   │
│   │   │   │   ├── client/
│   │   │   │   │   ├── OllamaClient.java
│   │   │   │   │   └── ChromaClient.java
│   │   │   │   │
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       └── LocalLabException.java
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── data.sql              # Seed data
│   │   │
│   │   └── test/
│   │       └── java/com/locallab/
│   │           ├── controller/
│   │           ├── service/
│   │           └── integration/
│   │
│   └── target/
│
└── frontend/
    ├── package.json
    ├── tsconfig.json
    ├── vite.config.ts
    ├── tailwind.config.js
    ├── .eslintrc.cjs
    ├── .prettierrc
    │
    ├── public/
    │
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── index.css
        │
        ├── api/
        │   ├── client.ts             # Axios instance
        │   ├── ollama.ts
        │   ├── tasks.ts
        │   ├── experiments.ts
        │   └── documents.ts
        │
        ├── components/
        │   ├── common/
        │   │   ├── ConfigPanel.tsx
        │   │   ├── ModelSelector.tsx
        │   │   ├── MetricsDisplay.tsx
        │   │   └── CodeBlock.tsx
        │   ├── sandbox/
        │   ├── arena/
        │   ├── rag/
        │   ├── experiments/
        │   │   ├── PipelineVisualiser.tsx
        │   │   └── ExperimentMatrix.tsx
        │   └── results/
        │       ├── Leaderboard.tsx
        │       ├── ComparisonMatrix.tsx
        │       └── DeepDiveModal.tsx
        │
        ├── pages/
        │   ├── Sandbox.tsx
        │   ├── Arena.tsx
        │   ├── RagLab.tsx
        │   ├── TaskLibrary.tsx
        │   ├── ExperimentBuilder.tsx
        │   ├── ExperimentRunner.tsx
        │   ├── ResultsDashboard.tsx
        │   └── EmbeddingAnalyser.tsx
        │
        ├── hooks/
        │   ├── useOllama.ts
        │   ├── useExperiment.ts
        │   └── useWebSocket.ts
        │
        ├── store/
        │   └── configStore.ts        # Zustand for global config
        │
        └── types/
            └── index.ts
```

---

## 9. Quality Assurance

### 9.1 Backend (Java)

#### Checkstyle
- Configuration: Google Java Style Guide (modified)
- Enforcement: Build fails on violations
- IDE integration: IntelliJ/VSCode plugins

#### JaCoCo Coverage
- **Target: 95% line coverage**
- Exclusions: DTOs, configuration classes, generated code
- Report: Generated on `mvn verify`

> **Note for Contributors**: Prefer reducing the coverage target over fabricating meaningless tests. If a class is genuinely untestable or test value is negligible, document the exclusion rationale.

#### Testing Strategy
| Layer | Approach |
|-------|----------|
| Controller | MockMvc integration tests |
| Service | Unit tests with Mockito |
| Repository | @DataJpaTest with H2 |
| Client | WireMock for external APIs |

### 9.2 Frontend (TypeScript)

#### ESLint + Prettier
- Strict TypeScript rules
- Auto-format on save
- Pre-commit hooks via Husky

#### Testing
- Vitest for unit tests
- React Testing Library for components
- Target: 80% coverage

---

## 10. Development Workflow

### 10.1 Prerequisites
- Java 21+
- Node.js 20+
- Ollama installed and running
- Docker (for Chroma)

### 10.2 Quick Start
```bash
# Clone repository
git clone <repo-url>
cd locallab

# Start Chroma
docker-compose up -d chroma

# Backend
cd backend
mvn spring-boot:run

# Frontend (new terminal)
cd frontend
npm install
npm run dev
```

### 10.3 Build Commands
```bash
# Backend
mvn clean verify              # Build + test + coverage
mvn checkstyle:check          # Style validation

# Frontend
npm run build                 # Production build
npm run lint                  # ESLint check
npm run test                  # Run tests
```

---

## 11. Non-Functional Requirements

### 11.1 Performance
- Single generation: < 100ms overhead (excluding model inference)
- UI responsiveness: < 50ms for interactions
- Experiment queue: Process sequentially to respect local resources

### 11.2 Error Handling
- **Fail Fast**: Validate all inputs at API boundaries
- **Graceful Degradation**: Failed experiment runs log error and continue
- **User Feedback**: Clear error messages with actionable guidance

### 11.3 Data Persistence
- All experiment data persists in H2
- H2 file stored in `./data/locallab.mv.db`
- Export functionality for backup/migration

### 11.4 Security
- Local-only by default (binds to localhost)
- No authentication required (single-user tool)
- Input sanitisation for all user-provided content

---

## 12. Future Considerations

The following are explicitly out of scope for v1 but the architecture should not preclude them:

1. **Drag-and-Drop Pipeline Editor**: Full ReactFlow implementation
2. **Automated Evaluation**: LLM-as-judge for response quality scoring
3. **Multi-User Support**: Authentication and workspace isolation
4. **Cloud Deployment**: Containerised deployment to cloud providers
5. **Additional Inference Backends**: llama.cpp, vLLM, LM Studio
6. **Prompt Versioning**: Git-like history for task templates

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| **Task Template** | A reusable prompt definition with optional variables |
| **Experiment** | A configured test scenario with defined models and parameters |
| **Experiment Run** | A single execution instance within an experiment |
| **Pipeline** | The visual representation of data flow through an experiment |
| **Embedding Model** | A model that converts text to vector representations for RAG |

---

## Appendix B: Example Experiment Flow

1. **Create Task Template**: "Summarise this code and explain its purpose"
2. **Create Experiment**:
   - Task: Code Summarisation
   - Models: `qwen2.5-coder:7b`, `codellama:7b`, `deepseek-coder:6.7b`
   - Context: RAG with uploaded codebase
   - Embeddings: `nomic-embed-text`, `mxbai-embed-large`
   - Iterations: 3
3. **Execute**: 3 models × 2 embeddings × 3 iterations = 18 runs
4. **Analyse**: Compare outputs, latency, and retrieval quality
5. **Decide**: Select optimal model for production use