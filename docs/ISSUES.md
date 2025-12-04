# LocalLab - GitHub Issues Breakdown

> **Total Estimated Issues**: 8 Parent Issues, 67 Sub-Issues  
> **Project**: LocalLab v2.0  
> **Generated**: 26 November 2025

---

## Overview

This document outlines all issues required to build the LocalLab application. Issues are organized hierarchically with parent issues representing major milestones and sub-issues representing individual tasks.

### Label Reference
- `parent-X` - Parent issue identifier
- `child-X` - Sub-issue belonging to parent X
- `backend` - Java/Spring Boot work
- `frontend` - React/TypeScript work
- `infra` - Infrastructure/DevOps work
- `blocked` - Has dependencies that must complete first
- `parallel` - Can be developed in parallel with siblings

### Dependency Legend
- **Depends on**: Must wait for specified issue(s)
-  **Parallel**: Can be developed alongside other parallel issues
- 🚀 **Starter**: No dependencies, can begin immediately

---

## Parent Issues

| # | Title | Description | Sub-Issues |
|---|-------|-------------|------------|
| 1 | Project Infrastructure & Setup | Initialize project structure, build tools, and development environment | 1.1 - 1.8 |
| 2 | Data Model & Entity Layer | Create all JPA entities, enums, and database schema | 2.1 - 2.8 |
| 3 | Repository Layer | Implement Spring Data JPA repositories | 3.1 - 3.6 |
| 4 | External Client Integrations | Build Ollama and Chroma client interfaces | 4.1 - 4.6 |
| 5 | Service Layer | Implement business logic services | 5.1 - 5.8 |
| 6 | Controller Layer & API | Build REST controllers and WebSocket endpoints | 6.1 - 6.10 |
| 7 | Frontend Foundation & Common Components | React setup, routing, shared components | 7.1 - 7.12 |
| 8 | Frontend Feature Pages | Implement all application pages | 8.1 - 8.16 |

---

## Parent Issue 1: Project Infrastructure & Setup
**Label**: `parent-1`, `infra`  
**Description**: Initialize the complete project structure including backend (Spring Boot), frontend (React/Vite), and infrastructure (Docker).

### Sub-Issues

#### 1.1 Initialize Git Repository and Root Structure
**Labels**: `child-1`, `infra`  
**Status**:  
**Description**:
- Create root `locallab/` directory
- Initialize git repository
- Create `.gitignore` with Java, Node, IDE exclusions
- Create root `README.md` with project overview and setup instructions

**Acceptance Criteria**:
- [ ] Git repository initialized
- [ ] `.gitignore` covers all necessary patterns
- [ ] README includes project description, tech stack, and quick start placeholder

---

#### 1.2 Create Docker Compose Configuration
**Labels**: `child-1`, `infra`  
**Status**: Parallel with 1.3, 1.4  
**Description**:
- Create `docker-compose.yml` at project root
- Configure Chroma vector database service
- Configure volume mounts for data persistence
- Add health checks

**Acceptance Criteria**:
- [ ] `docker-compose up -d chroma` starts Chroma successfully
- [ ] Data persists across container restarts
- [ ] Health check endpoint responds

**File**: `docker-compose.yml`
```yaml
version: '3.8'
services:
  chroma:
    image: chromadb/chroma:latest
    ports:
      - "8000:8000"
    volumes:
      - chroma_data:/chroma/chroma
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/api/v1/heartbeat"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  chroma_data:
```

---

#### 1.3 Initialize Spring Boot Backend Project
**Labels**: `child-1`, `backend`, `infra`  
**Status**: Parallel with 1.2, 1.4  
**Description**:
- Create `backend/` directory structure
- Initialize Maven project with `pom.xml`
- Configure Spring Boot 3.x with Java 21
- Add core dependencies: Spring Web, Spring Data JPA, H2, Validation, WebSocket
- Add `ollama4j` dependency
- Create main application class `LocalLabApplication.java`

**Acceptance Criteria**:
- [ ] `mvn clean compile` succeeds
- [ ] Application starts on port 8080
- [ ] H2 console accessible at `/h2-console`

**Dependencies (pom.xml)**:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-websocket
- spring-boot-starter-validation
- h2 (runtime)
- ollama4j
- lombok
- jackson-databind

---

#### 1.4 Initialize React Frontend Project
**Labels**: `child-1`, `frontend`, `infra`  
**Status**: Parallel with 1.2, 1.3  
**Description**:
- Create `frontend/` directory
- Initialize Vite + React + TypeScript project
- Install core dependencies: React Router, Axios, Zustand, TailwindCSS
- Install UI dependencies: Radix UI, Recharts, ReactFlow
- Configure TypeScript strict mode

**Acceptance Criteria**:
- [ ] `npm run dev` starts development server
- [ ] TypeScript compilation succeeds
- [ ] TailwindCSS classes apply correctly

**Dependencies (package.json)**:
- react, react-dom
- react-router-dom
- axios
- zustand
- tailwindcss, postcss, autoprefixer
- @radix-ui/react-* (dialog, dropdown, tabs, etc.)
- recharts
- reactflow
- @types/* for all libraries

---

#### 1.5 Configure Backend Code Quality Tools
**Labels**: `child-1`, `backend`, `infra`  
**Status**: Depends on: 1.3  
**Description**:
- Create `checkstyle.xml` with Google Java Style (modified)
- Configure Checkstyle Maven plugin
- Configure JaCoCo for coverage reporting (95% target)
- Add Spotless for auto-formatting
- Configure build to fail on style violations

**Acceptance Criteria**:
- [ ] `mvn checkstyle:check` runs without errors on clean code
- [ ] `mvn verify` generates JaCoCo coverage report
- [ ] Build fails when coverage < 95% (excluding configured exclusions)

---

#### 1.6 Configure Frontend Code Quality Tools
**Labels**: `child-1`, `frontend`, `infra`  
**Status**: Depends on: 1.4  
**Description**:
- Create `.eslintrc.cjs` with strict TypeScript rules
- Create `.prettierrc` configuration
- Configure Husky for pre-commit hooks
- Add lint-staged for staged file linting
- Configure Vitest for testing

**Acceptance Criteria**:
- [ ] `npm run lint` passes on clean code
- [ ] Pre-commit hook runs linter automatically
- [ ] `npm run test` executes Vitest

**Files**:
- `.eslintrc.cjs`
- `.prettierrc`
- `.husky/pre-commit`
- `vitest.config.ts`

---

#### 1.7 Create Backend Application Configuration
**Labels**: `child-1`, `backend`  
**Status**: Depends on: 1.3  
**Description**:
- Create `application.yml` with default configuration
- Create `application-dev.yml` with development overrides
- Configure H2 database settings (file-based persistence)
- Configure Ollama connection defaults
- Configure CORS for local development

**Acceptance Criteria**:
- [ ] Application starts with default profile
- [ ] H2 database file created at `./data/locallab.mv.db`
- [ ] CORS allows requests from `http://localhost:5173`

**Configuration Items**:
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/locallab
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: update

ollama:
  base-url: http://localhost:11434

chroma:
  base-url: http://localhost:8000
```

---

#### 1.8 Create Base Exception Handling Structure
**Labels**: `child-1`, `backend`  
**Status**: Depends on: 1.3  
**Description**:
- Create `GlobalExceptionHandler` with `@ControllerAdvice`
- Define standard error response DTO
- Handle common exceptions using standard Spring/Java exceptions

**Standard Exception Mapping**:
| Scenario | Exception |
|----------|----------|
| Resource not found | `EntityNotFoundException` |
| State/conflict errors | `IllegalStateException` |
| Validation/input errors | `IllegalArgumentException` |
| Other HTTP status codes | `ResponseStatusException(status, ...)` |

**Acceptance Criteria**:
- [ ] All exceptions return consistent JSON structure
- [ ] Validation errors return field-level details
- [ ] 404 errors return meaningful messages
- [ ] `EntityNotFoundException` handled with 404 response
- [ ] `IllegalStateException` handled with 409 response
- [ ] `IllegalArgumentException` handled with 400 response
- [ ] `ResponseStatusException` handled with appropriate status

**Files**:
- `exception/GlobalExceptionHandler.java`
- `dto/response/ErrorResponse.java`

---

## Parent Issue 2: Data Model & Entity Layer
**Label**: `parent-2`, `backend`  
**Description**: Create all JPA entities, enums, and establish the database schema.  
**Depends on**: Parent Issue 1 (specifically 1.3, 1.7)

### Sub-Issues

#### 2.1 Create Enum Definitions
**Labels**: `child-2`, `backend`  
**Status**:  (after Parent 1)Parallel with 2.2-2.8  
**Description**:
- Create `ExperimentStatus` enum: `DRAFT`, `RUNNING`, `COMPLETED`, `FAILED`
- Create `RunStatus` enum: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`

**Acceptance Criteria**:
- [ ] Enums compile without errors
- [ ] Enums include descriptive Javadoc

**Files**:
- `model/enums/ExperimentStatus.java`
- `model/enums/RunStatus.java`

---

#### 2.2 Create TaskTemplate Entity
**Labels**: `child-2`, `backend`  
**Status**:  Parallel with 2.1, 2.3-2.8  
**Description**:
- Create JPA entity with all fields per specification
- Add validation annotations (`@NotBlank`, `@Size`, etc.)
- Configure `@Lob` for text fields (promptTemplate, evaluationNotes)
- Add `@CreationTimestamp` for createdAt
- Include Javadoc for all fields

**Acceptance Criteria**:
- [ ] Entity creates table on application start
- [ ] All constraints enforced at database level
- [ ] Lombok `@Data`, `@Entity` annotations present

**Fields**:
| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | @Id, @GeneratedValue |
| name | String | @NotBlank, @Size(max=100) |
| description | String | @Lob |
| promptTemplate | String | @NotBlank, @Lob |
| tags | String | nullable |
| evaluationNotes | String | @Lob, nullable |
| createdAt | LocalDateTime | @CreationTimestamp |

---

#### 2.3 Create SystemPrompt Entity
**Labels**: `child-2`, `backend`  
**Status**:  Parallel with 2.1, 2.2, 2.4-2.8  
**Description**:
- Create JPA entity for reusable system prompts
- Add validation annotations
- Configure timestamps

**Acceptance Criteria**:
- [ ] Entity creates table on application start
- [ ] Alias field is unique

**Fields**:
| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | @Id, @GeneratedValue |
| alias | String | @NotBlank, @Column(unique=true) |
| content | String | @NotBlank, @Lob |
| createdAt | LocalDateTime | @CreationTimestamp |

---

#### 2.4 Create EmbeddingModel Entity
**Labels**: `child-2`, `backend`  
**Status**:  Parallel with 2.1-2.3, 2.5-2.8  
**Description**:
- Create JPA entity for tracking embedding models
- Store Ollama model name and vector dimensions

**Acceptance Criteria**:
- [ ] Entity creates table on application start
- [ ] Name is unique

**Fields**:
| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | @Id, @GeneratedValue |
| name | String | @NotBlank, @Column(unique=true) |
| ollamaModelName | String | @NotBlank |
| dimensions | Integer | @NotNull |

---

#### 2.5 Create Document Entity
**Labels**: `child-2`, `backend`  
**Status**:  Parallel with 2.1-2.4, 2.6-2.8  
**Description**:
- Create JPA entity for uploaded documents
- Track filename, content, and chunk count
- Add timestamp

**Acceptance Criteria**:
- [ ] Entity creates table on application start
- [ ] Content stored as LOB

**Fields**:
| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | @Id, @GeneratedValue |
| filename | String | @NotBlank |
| content | String | @Lob |
| chunkCount | Integer | default 0 |
| createdAt | LocalDateTime | @CreationTimestamp |

---

#### 2.6 Create Experiment Entity
**Labels**: `child-2`, `backend`  
**Status**:  Parallel with 2.1-2.5, 2.7-2.8  
**Description**:
- Create JPA entity for experiments
- Configure relationship to TaskTemplate (ManyToOne)
- Store configuration as JSON string
- Use ExperimentStatus enum

**Acceptance Criteria**:
- [ ] Entity creates table with foreign key to task_template
- [ ] Config field stores JSON
- [ ] Status defaults to DRAFT

**Fields**:
| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | @Id, @GeneratedValue |
| name | String | @NotBlank |
| taskTemplate | TaskTemplate | @ManyToOne, nullable |
| status | ExperimentStatus | @Enumerated, default DRAFT |
| config | String | @Lob (JSON) |
| createdAt | LocalDateTime | @CreationTimestamp |

---

#### 2.7 Create ExperimentRun Entity
**Labels**: `child-2`, `backend`  
**Status**:  Parallel with 2.1-2.6, 2.8  
**Description**:
- Create JPA entity for individual experiment runs
- Configure relationships to Experiment, EmbeddingModel, SystemPrompt
- Store all metrics and output fields
- Use RunStatus enum

**Acceptance Criteria**:
- [ ] Entity creates table with all foreign keys
- [ ] All nullable fields properly annotated
- [ ] JSON fields use @Lob

**Fields**:
| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | @Id, @GeneratedValue |
| experiment | Experiment | @ManyToOne, @NotNull |
| modelName | String | @NotBlank |
| embeddingModel | EmbeddingModel | @ManyToOne, nullable |
| systemPrompt | SystemPrompt | @ManyToOne, nullable |
| iteration | Integer | @NotNull |
| config | String | @Lob (JSON) |
| status | RunStatus | @Enumerated, default PENDING |
| output | String | @Lob, nullable |
| durationMs | Long | nullable |
| tokensPerSecond | Double | nullable |
| timeToFirstTokenMs | Long | nullable |
| retrievedChunks | String | @Lob (JSON), nullable |
| errorMessage | String | @Lob, nullable |
| timestamp | LocalDateTime | @CreationTimestamp |

---

#### 2.8 Create Seed Data SQL Script
**Labels**: `child-2`, `backend`  
**Status**: Depends on: 2.1-2.7  
**Description**:
- Create `data.sql` with sample data
- Include sample TaskTemplates
- Include sample SystemPrompts
- Include sample EmbeddingModels

**Acceptance Criteria**:
- [ ] Data loads on application start (if tables empty)
- [ ] Sample data demonstrates all entity features

**File**: `resources/data.sql`

---

## Parent Issue 3: Repository Layer
**Label**: `parent-3`, `backend`  
**Description**: Implement Spring Data JPA repositories for all entities.  
**Depends on**: Parent Issue 2

### Sub-Issues

#### 3.1 Create TaskTemplateRepository
**Labels**: `child-3`, `backend`  
**Status**:  Parallel with 3.2-3.6  
**Description**:
- Extend `JpaRepository<TaskTemplate, Long>`
- Add custom query methods for tag filtering
- Add search by name method

**Acceptance Criteria**:
- [ ] CRUD operations work
- [ ] `findByTagsContaining` returns matching templates
- [ ] `findByNameContainingIgnoreCase` supports search

**Methods**:
```java
List<TaskTemplate> findByTagsContaining(String tag);
List<TaskTemplate> findByNameContainingIgnoreCase(String name);
```

---

#### 3.2 Create SystemPromptRepository
**Labels**: `child-3`, `backend`  
**Status**:  Parallel with 3.1, 3.3-3.6  
**Description**:
- Extend `JpaRepository<SystemPrompt, Long>`
- Add find by alias method

**Acceptance Criteria**:
- [ ] CRUD operations work
- [ ] `findByAlias` returns optional

**Methods**:
```java
Optional<SystemPrompt> findByAlias(String alias);
```

---

#### 3.3 Create EmbeddingModelRepository
**Labels**: `child-3`, `backend`  
**Status**:  Parallel with 3.1-3.2, 3.4-3.6  
**Description**:
- Extend `JpaRepository<EmbeddingModel, Long>`
- Add find by name method

**Acceptance Criteria**:
- [ ] CRUD operations work
- [ ] `findByName` returns optional

---

#### 3.4 Create DocumentRepository
**Labels**: `child-3`, `backend`  
**Status**:  Parallel with 3.1-3.3, 3.5-3.6  
**Description**:
- Extend `JpaRepository<Document, Long>`
- Add find by filename method

**Acceptance Criteria**:
- [ ] CRUD operations work
- [ ] `findByFilename` returns list

---

#### 3.5 Create ExperimentRepository
**Labels**: `child-3`, `backend`  
**Status**:  Parallel with 3.1-3.4, 3.6  
**Description**:
- Extend `JpaRepository<Experiment, Long>`
- Add find by status method
- Add find by task template method
- Add ordering by created date

**Acceptance Criteria**:
- [ ] CRUD operations work
- [ ] Status filtering works
- [ ] Results ordered by creation date descending

**Methods**:
```java
List<Experiment> findByStatusOrderByCreatedAtDesc(ExperimentStatus status);
List<Experiment> findByTaskTemplateIdOrderByCreatedAtDesc(Long taskTemplateId);
List<Experiment> findAllByOrderByCreatedAtDesc();
```

---

#### 3.6 Create ExperimentRunRepository
**Labels**: `child-3`, `backend`  
**Status**:  Parallel with 3.1-3.5  
**Description**:
- Extend `JpaRepository<ExperimentRun, Long>`
- Add find by experiment method
- Add find by status method
- Add aggregation queries for analytics

**Acceptance Criteria**:
- [ ] CRUD operations work
- [ ] Experiment filtering works
- [ ] Aggregation queries return correct results

**Methods**:
```java
List<ExperimentRun> findByExperimentIdOrderByIterationAsc(Long experimentId);
List<ExperimentRun> findByExperimentIdAndStatus(Long experimentId, RunStatus status);
List<ExperimentRun> findByModelName(String modelName);

@Query("SELECT AVG(r.tokensPerSecond) FROM ExperimentRun r WHERE r.experiment.id = :expId AND r.status = 'SUCCESS'")
Double getAverageTokensPerSecond(@Param("expId") Long experimentId);

@Query("SELECT AVG(r.durationMs) FROM ExperimentRun r WHERE r.experiment.id = :expId AND r.status = 'SUCCESS'")
Double getAverageDuration(@Param("expId") Long experimentId);
```

---

## Parent Issue 4: External Client Integrations
**Label**: `parent-4`, `backend`  
**Description**: Build client interfaces for Ollama and Chroma external services.  
**Depends on**: Parent Issue 1 (1.3, 1.7)

### Sub-Issues

#### 4.1 Create OllamaClient Interface
**Labels**: `child-4`, `backend`  
**Status**:  (after Parent 1)Parallel with 4.3, 4.4  
**Description**:
- Define interface for Ollama operations
- Support dependency injection for testing

**Acceptance Criteria**:
- [ ] Interface defines all required operations
- [ ] Javadoc explains each method

**Methods**:
```java
List<String> listModels();
boolean isAvailable();
GenerationResponse generate(GenerationRequest request);
EmbeddingResponse embed(EmbeddingRequest request);
```

---

#### 4.2 Implement OllamaClient with ollama4j
**Labels**: `child-4`, `backend`  
**Status**: Depends on: 4.1  
**Description**:
- Implement OllamaClient interface using ollama4j library
- Handle connection errors gracefully
- Implement retry logic with exponential backoff
- Log all requests and responses at DEBUG level

**Acceptance Criteria**:
- [ ] Successfully lists models from running Ollama instance
- [ ] Generation returns complete response with metrics
- [ ] Connection failures throw descriptive exceptions
- [ ] Timeout configurable via application properties

---

#### 4.3 Create DTOs for Ollama Operations
**Labels**: `child-4`, `backend`  
**Status**:  Parallel with 4.1, 4.4  
**Description**:
- Create request/response DTOs for generation
- Create request/response DTOs for embedding
- Include all configuration options

**Files**:
- `dto/request/GenerationRequest.java`
- `dto/response/GenerationResponse.java`
- `dto/request/EmbeddingRequest.java`
- `dto/response/EmbeddingResponse.java`

**GenerationRequest Fields**:
- model, prompt, systemPrompt
- temperature, topP, topK
- contextWindow, maxTokens
- stream (boolean)

**GenerationResponse Fields**:
- response, model
- durationMs, tokensPerSecond, timeToFirstTokenMs
- promptTokens, completionTokens

---

#### 4.4 Create ChromaClient Interface
**Labels**: `child-4`, `backend`  
**Status**:  Parallel with 4.1, 4.3  
**Description**:
- Define interface for Chroma vector store operations
- Support collection management
- Support document insertion and querying

**Methods**:
```java
void createCollection(String name, int dimensions);
void deleteCollection(String name);
boolean collectionExists(String name);
void addDocuments(String collection, List<ChromaDocument> documents);
List<ChromaQueryResult> query(String collection, float[] embedding, int topK);
```

---

#### 4.5 Implement ChromaClient with REST API
**Labels**: `child-4`, `backend`  
**Status**: Depends on: 4.4  
**Description**:
- Implement ChromaClient using RestTemplate/WebClient
- Handle HTTP errors appropriately
- Support all CRUD operations on collections
- Implement query with similarity scores

**Acceptance Criteria**:
- [ ] Can create and delete collections
- [ ] Can insert documents with embeddings
- [ ] Query returns documents with distance scores
- [ ] Connection failures handled gracefully

---

#### 4.6 Create DTOs for Chroma Operations
**Labels**: `child-4`, `backend`  
**Status**:  Parallel with 4.4  
**Description**:
- Create DTOs for Chroma documents
- Create DTOs for query results

**Files**:
- `dto/ChromaDocument.java` (id, content, embedding, metadata)
- `dto/ChromaQueryResult.java` (id, content, distance, metadata)

---

## Parent Issue 5: Service Layer
**Label**: `parent-5`, `backend`  
**Description**: Implement all business logic services.  
**Depends on**: Parent Issues 2, 3, 4

### Sub-Issues

#### 5.1 Create OllamaService
**Labels**: `child-5`, `backend`  
**Status**:  (after Parents 2-4)Parallel with 5.2-5.4  
**Description**:
- Inject OllamaClient
- Provide high-level methods for model listing
- Provide generation with configuration mapping
- Handle errors using standard exceptions

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| Connectivity issues | `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ...)` |
| Invalid request parameters | `IllegalArgumentException(...)` |
| Resource not found | `EntityNotFoundException(...)` |

**Acceptance Criteria**:
- [ ] `getAvailableModels()` returns list of model names
- [ ] `checkConnectivity()` returns status DTO
- [ ] `generate()` accepts config DTO and returns response DTO
- [ ] All errors wrapped in appropriate standard exceptions

**Methods**:
```java
List<String> getAvailableModels();
OllamaStatusResponse checkConnectivity();
GenerationResponse generate(GenerationRequest request);
```

---

#### 5.2 Create TaskService
**Labels**: `child-5`, `backend`  
**Status**:  Parallel with 5.1, 5.3-5.4  
**Description**:
- CRUD operations for TaskTemplate
- Variable extraction from prompt templates
- Tag management and filtering
- Search functionality

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| Template not found | `EntityNotFoundException("Task template not found: " + id)` |
| Validation failures | `IllegalArgumentException(...)` |

**Acceptance Criteria**:
- [ ] All CRUD operations work
- [ ] `extractVariables()` parses `{{variable}}` syntax
- [ ] Search returns partial matches
- [ ] Tag filtering works

**Methods**:
```java
List<TaskTemplate> findAll();
TaskTemplate findById(Long id);
TaskTemplate create(TaskTemplateRequest request);
TaskTemplate update(Long id, TaskTemplateRequest request);
void delete(Long id);
List<String> extractVariables(String promptTemplate);
List<TaskTemplate> searchByName(String query);
List<TaskTemplate> filterByTag(String tag);
```

---

#### 5.3 Create SystemPromptService
**Labels**: `child-5`, `backend`  
**Status**:  Parallel with 5.1-5.2, 5.4  
**Description**:
- CRUD operations for SystemPrompt
- Alias uniqueness validation

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| System prompt not found | `EntityNotFoundException("System prompt not found: " + id)` |
| Alias already exists | `IllegalStateException("System prompt with alias '...' already exists")` |

**Acceptance Criteria**:
- [ ] All CRUD operations work
- [ ] Duplicate alias throws `IllegalStateException`

---

#### 5.4 Create DocumentService (Part 1 - Basic CRUD)
**Labels**: `child-5`, `backend`  
**Status**:  Parallel with 5.1-5.3  
**Description**:
- Upload document (PDF/TXT support)
- Store document content in database
- List and delete documents

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| Document not found | `EntityNotFoundException("Document not found: " + id)` |
| Unsupported file type | `IllegalArgumentException("Unsupported file type: ...")` |
| Extraction failures | `ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ...)` |

**Acceptance Criteria**:
- [ ] Can upload text files
- [ ] Can upload PDF files (text extraction)
- [ ] Documents persist in H2
- [ ] Delete removes document
- [ ] Unsupported file types throw `IllegalArgumentException`

---

#### 5.5 Create RagService
**Labels**: `child-5`, `backend`  
**Status**: Depends on: 5.4, 4.2, 4.5  
**Description**:
- Document chunking logic (configurable size/overlap)
- Embedding generation via Ollama
- Vector storage via Chroma
- Query and retrieval logic
- Context assembly for LLM

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| Document/embedding model not found | `EntityNotFoundException(...)` |
| Invalid chunk parameters | `IllegalArgumentException("Chunk overlap must be less than chunk size")` |

**Acceptance Criteria**:
- [ ] `chunkDocument()` splits text correctly
- [ ] `embedAndStore()` creates vectors in Chroma
- [ ] `query()` returns top-K relevant chunks with scores
- [ ] `buildContext()` assembles retrieved chunks into prompt context
- [ ] Chunk overlap validation throws `IllegalArgumentException`

**Methods**:
```java
List<String> chunkDocument(String content, int chunkSize, int overlap);
void embedAndStore(Long documentId, String embeddingModel, int chunkSize, int overlap);
List<RetrievedChunk> query(String collectionName, String query, String embeddingModel, int topK);
String buildContext(List<RetrievedChunk> chunks);
```

---

#### 5.6 Create ExperimentService
**Labels**: `child-5`, `backend`  
**Status**: Depends on: 5.1-5.3  
**Description**:
- CRUD operations for Experiment
- Experiment configuration validation
- Status management
- Calculate total run count (models × embeddings × iterations)

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| Experiment not found | `EntityNotFoundException("Experiment not found: " + id)` |
| Invalid status transition | `IllegalStateException("Cannot update/delete experiment with status: ...")` |
| Validation failures | `IllegalArgumentException(...)` |

**Acceptance Criteria**:
- [ ] All CRUD operations work
- [ ] Config JSON validates correctly
- [ ] Status transitions enforced via `IllegalStateException`
- [ ] Run matrix calculation correct

**Methods**:
```java
List<Experiment> findAll();
Experiment findById(Long id);
Experiment create(ExperimentRequest request);
Experiment update(Long id, ExperimentRequest request);
void delete(Long id);
int calculateTotalRuns(ExperimentConfig config);
void updateStatus(Long id, ExperimentStatus status);
```

---

#### 5.7 Create ExperimentExecutorService
**Labels**: `child-5`, `backend`  
**Status**: Depends on: 5.1, 5.5, 5.6  
**Description**:
- Execute experiment runs asynchronously
- Generate all run combinations from config
- Execute each run sequentially (respect local resources)
- Persist results immediately after each run
- Support pause/resume/cancel
- Publish progress via WebSocket

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| Invalid status transition | `IllegalStateException("Experiment must be in DRAFT/PAUSED status...")` |

**Acceptance Criteria**:
- [ ] Experiment starts and runs all combinations
- [ ] Each run persists immediately
- [ ] Failed runs log error and continue
- [ ] Pause stops after current run
- [ ] Cancel aborts and marks remaining as cancelled
- [ ] Progress updates sent via WebSocket
- [ ] Invalid status throws `IllegalStateException`

**Methods**:
```java
void startExperiment(Long experimentId);
void pauseExperiment(Long experimentId);
void cancelExperiment(Long experimentId);
void resumeExperiment(Long experimentId);
ExperimentProgress getProgress(Long experimentId);
```

---

#### 5.8 Create AnalyticsService
**Labels**: `child-5`, `backend`  
**Status**: Depends on: 3.6  
**Description**:
- Aggregate metrics across experiment runs
- Calculate averages, min, max for TPS, latency
- Calculate success rates per model
- Generate leaderboard data
- Support filtering by experiment, model, embedding

**Exception Handling**:
| Scenario | Exception |
|----------|----------|
| No runs found for model | `EntityNotFoundException("No runs found for model: " + modelName)` |

**Acceptance Criteria**:
- [ ] Returns correct aggregated metrics
- [ ] Filtering works correctly
- [ ] Leaderboard sorted correctly
- [ ] Empty model results throw `EntityNotFoundException`

**Methods**:
```java
LeaderboardData getLeaderboard(LeaderboardFilter filter);
ModelMetrics getModelMetrics(String modelName, Long experimentId);
Map<String, ModelMetrics> compareModels(Long experimentId);
```

---

## Parent Issue 6: Controller Layer & API
**Label**: `parent-6`, `backend`  
**Description**: Build all REST controllers and WebSocket endpoints.  
**Depends on**: Parent Issue 5

### Sub-Issues

#### 6.1 Create WebConfig for CORS
**Labels**: `child-6`, `backend`  
**Status**:  (after Parent 5)Parallel with 6.2-6.10  
**Description**:
- Configure CORS for local development
- Allow all origins in dev profile
- Restrict in production

**File**: `config/WebConfig.java`

---

#### 6.2 Create WebSocketConfig
**Labels**: `child-6`, `backend`  
**Status**:  Parallel with 6.1, 6.3-6.10  
**Description**:
- Configure STOMP over WebSocket
- Define message broker
- Configure `/ws` endpoint

**File**: `config/WebSocketConfig.java`

---

#### 6.3 Create OllamaController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel with 6.1-6.2, 6.4-6.10  
**Description**:
- `GET /api/ollama/models` - List available models
- `GET /api/ollama/status` - Check connectivity
- `POST /api/ollama/generate` - Single generation

**Acceptance Criteria**:
- [ ] All endpoints return correct status codes
- [ ] Validation errors return 400
- [ ] Ollama errors return 503

---

#### 6.4 Create TaskController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel  
**Description**:
- Full REST CRUD for TaskTemplate
- Search and filter endpoints

**Endpoints**:
- `GET /api/tasks` - List all (optional query params for search/filter)
- `GET /api/tasks/{id}` - Get single
- `POST /api/tasks` - Create
- `PUT /api/tasks/{id}` - Update
- `DELETE /api/tasks/{id}` - Delete

---

#### 6.5 Create SystemPromptController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel  
**Description**:
- Full REST CRUD for SystemPrompt

**Endpoints**:
- `GET /api/system-prompts`
- `GET /api/system-prompts/{id}`
- `POST /api/system-prompts`
- `PUT /api/system-prompts/{id}`
- `DELETE /api/system-prompts/{id}`

---

#### 6.6 Create DocumentController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel  
**Description**:
- Document upload (multipart)
- List documents
- Delete document
- Query with RAG

**Endpoints**:
- `POST /api/documents` - Upload (multipart/form-data)
- `GET /api/documents` - List all
- `GET /api/documents/{id}` - Get single
- `DELETE /api/documents/{id}` - Delete
- `POST /api/documents/{id}/query` - RAG query
- `GET /api/embedding-models` - List embedding models

---

#### 6.7 Create ExperimentController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel  
**Description**:
- CRUD for Experiment
- Execution control endpoints

**Endpoints**:
- `GET /api/experiments` - List all
- `GET /api/experiments/{id}` - Get with config
- `POST /api/experiments` - Create
- `PUT /api/experiments/{id}` - Update
- `DELETE /api/experiments/{id}` - Delete
- `POST /api/experiments/{id}/start` - Begin execution
- `POST /api/experiments/{id}/pause` - Pause
- `POST /api/experiments/{id}/resume` - Resume
- `POST /api/experiments/{id}/cancel` - Cancel

---

#### 6.8 Create ResultController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel  
**Description**:
- Get experiment runs
- Get single run details
- Export functionality

**Endpoints**:
- `GET /api/experiments/{id}/runs` - List runs for experiment
- `GET /api/runs/{id}` - Get single run
- `GET /api/experiments/{id}/export?format=csv|json` - Export

---

#### 6.9 Create AnalyticsController
**Labels**: `child-6`, `backend`  
**Status**:  Parallel  
**Description**:
- Leaderboard data endpoint
- Model comparison endpoint

**Endpoints**:
- `GET /api/analytics/leaderboard` - Get leaderboard (query params for filters)
- `GET /api/analytics/models/{modelName}` - Get model metrics
- `GET /api/experiments/{id}/comparison` - Compare models in experiment

---

#### 6.10 Create ExperimentWebSocketHandler
**Labels**: `child-6`, `backend`  
**Status**: Depends on: 6.2  
**Description**:
- Handle WebSocket connections for experiment progress
- Broadcast run completion events
- Broadcast progress updates

**Endpoint**: `/ws/experiments/{id}`

**Message Types**:
- `PROGRESS` - Overall progress percentage
- `RUN_STARTED` - Run beginning
- `RUN_COMPLETED` - Run finished (success/fail)
- `EXPERIMENT_COMPLETED` - All runs done

---

## Parent Issue 7: Frontend Foundation & Common Components
**Label**: `parent-7`, `frontend`  
**Description**: Set up React application structure, routing, state management, and shared components.  
**Depends on**: Parent Issue 1 (1.4, 1.6)

### Sub-Issues

#### 7.1 Configure React Router and Layout
**Labels**: `child-7`, `frontend`  
**Status**:  (after Parent 1)Parallel with 7.2-7.4  
**Description**:
- Set up React Router v6
- Create layout component with navigation sidebar
- Define routes for all pages

**Routes**:
- `/` - Redirect to `/sandbox`
- `/sandbox` - Sandbox page
- `/arena` - Arena page
- `/rag` - RAG Lab page
- `/tasks` - Task Library page
- `/experiments/new` - Experiment Builder
- `/experiments/:id/run` - Experiment Runner
- `/results` - Results Dashboard
- `/embeddings` - Embedding Analyser

**Files**:
- `App.tsx`
- `components/Layout.tsx`
- `components/Sidebar.tsx`

---

#### 7.2 Create Axios API Client
**Labels**: `child-7`, `frontend`  
**Status**:  Parallel with 7.1, 7.3-7.4  
**Description**:
- Configure Axios instance with base URL
- Add request/response interceptors
- Handle errors globally
- Add TypeScript types for responses

**File**: `api/client.ts`

---

#### 7.3 Create Zustand Config Store
**Labels**: `child-7`, `frontend`  
**Status**:  Parallel with 7.1-7.2, 7.4  
**Description**:
- Global state for model configuration
- Persist selected model, temperature, etc.
- Actions to update configuration

**File**: `store/configStore.ts`

**State Shape**:
```typescript
interface ConfigState {
  model: string | null;
  temperature: number;
  topP: number;
  topK: number;
  contextWindow: number;
  maxTokens: number | null;
  systemPromptId: number | null;
  customSystemPrompt: string;
  
  setModel: (model: string) => void;
  setTemperature: (temp: number) => void;
  // ... other setters
  resetToDefaults: () => void;
}
```

---

#### 7.4 Create TypeScript Type Definitions
**Labels**: `child-7`, `frontend`  
**Status**:  Parallel with 7.1-7.3  
**Description**:
- Define all API response types
- Define entity types
- Define enum types

**File**: `types/index.ts`

**Types**:
- TaskTemplate, SystemPrompt, Document, Experiment, ExperimentRun
- ExperimentStatus, RunStatus
- GenerationRequest, GenerationResponse
- All DTO types matching backend

---

#### 7.5 Create ModelSelector Component
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.2, 7.4  
**Description**:
- Dropdown populated from `/api/ollama/models`
- Loading and error states
- Store selection in config store

**Acceptance Criteria**:
- [ ] Shows loading spinner while fetching
- [ ] Displays available models
- [ ] Updates global config on selection
- [ ] Shows error message if Ollama unavailable

**File**: `components/common/ModelSelector.tsx`

---

#### 7.6 Create ConfigPanel Component
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.3, 7.5  
**Description**:
- Collapsible panel with all configuration options
- Model selector
- Temperature slider (0-2, step 0.1)
- Top P slider (0-1, step 0.05)
- Top K number input (1-100)
- Context window input
- Max tokens input (optional)
- System prompt selector and custom input

**Acceptance Criteria**:
- [ ] All controls update config store
- [ ] Collapsible with smooth animation
- [ ] Preset system prompts load from API
- [ ] Custom system prompt textarea

**File**: `components/common/ConfigPanel.tsx`

---

#### 7.7 Create MetricsDisplay Component
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.4  
**Description**:
- Display generation metrics
- TPS, duration, TTFT
- Token counts
- Compact and expanded views

**File**: `components/common/MetricsDisplay.tsx`

---

#### 7.8 Create CodeBlock Component
**Labels**: `child-7`, `frontend`  
**Status**:  Parallel  
**Description**:
- Syntax highlighted code display
- Copy to clipboard button
- Language detection
- Line numbers (optional)

**File**: `components/common/CodeBlock.tsx`

---

#### 7.9 Create API Hooks - Ollama
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.2, 7.4  
**Description**:
- `useOllamaModels` - Fetch and cache model list
- `useOllamaStatus` - Check connectivity
- `useGenerate` - Generation mutation

**File**: `hooks/useOllama.ts`

---

#### 7.10 Create API Hooks - Tasks & Prompts
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.2, 7.4  
**Description**:
- `useTasks` - CRUD operations for TaskTemplate
- `useSystemPrompts` - CRUD for SystemPrompt

**Files**:
- `api/tasks.ts`
- `api/systemPrompts.ts`
- `hooks/useTasks.ts`
- `hooks/useSystemPrompts.ts`

---

#### 7.11 Create API Hooks - Experiments
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.2, 7.4  
**Description**:
- `useExperiments` - CRUD operations
- `useExperimentRuns` - Fetch runs for experiment
- `useExperimentControl` - Start/pause/cancel mutations

**Files**:
- `api/experiments.ts`
- `hooks/useExperiment.ts`

---

#### 7.12 Create WebSocket Hook
**Labels**: `child-7`, `frontend`  
**Status**: Depends on: 7.4  
**Description**:
- `useExperimentWebSocket` - Connect to experiment progress
- Handle reconnection
- Parse message types
- Update local state on messages

**File**: `hooks/useWebSocket.ts`

---

## Parent Issue 8: Frontend Feature Pages
**Label**: `parent-8`, `frontend`  
**Description**: Implement all application pages and their specific components.  
**Depends on**: Parent Issue 7

### Sub-Issues

#### 8.1 Implement Sandbox Page - Basic Chat
**Labels**: `child-8`, `frontend`  
**Status**:  (after Parent 7)Parallel with other 8.x  
**Description**:
- Chat interface with message history
- Input area with send button
- Message bubbles (user/assistant)
- Full config panel integration
- Metrics display per response

**Acceptance Criteria**:
- [ ] Messages display correctly
- [ ] Generation calls API with config
- [ ] Metrics shown after response
- [ ] History persists during session

**File**: `pages/Sandbox.tsx`

---

#### 8.2 Implement Sandbox Page - Advanced Features
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.1  
**Description**:
- JSON mode toggle
- Code syntax highlighting in responses
- Copy code button
- "Save as Task Template" action
- Clear history button

**Acceptance Criteria**:
- [ ] JSON mode sends correct parameter
- [ ] Code blocks render with syntax highlighting
- [ ] Can save interaction as new TaskTemplate

---

#### 8.3 Implement Arena Page - Layout
**Labels**: `child-8`, `frontend`  
**Status**:  Parallel  
**Description**:
- Two-column split view
- Independent model selector per column
- Shared prompt input at bottom
- Send button triggers both simultaneously

**File**: `pages/Arena.tsx`

---

#### 8.4 Implement Arena Page - Comparison Features
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.3  
**Description**:
- Diff view toggle showing textual differences
- Human evaluation buttons: "Left wins", "Right wins", "Tie"
- Evaluation logging to session storage
- Metrics comparison display
- Export evaluation results

---

#### 8.5 Implement Task Library Page - List View
**Labels**: `child-8`, `frontend`  
**Status**:  Parallel  
**Description**:
- Tabbed view: Task Templates | System Prompts
- Data table with columns: Name, Tags, Created, Actions
- Inline actions: Edit, Delete
- Search input
- Tag filter dropdown

**File**: `pages/TaskLibrary.tsx`

---

#### 8.6 Implement Task Library Page - Create/Edit Modal
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.5  
**Description**:
- Modal form for creating/editing TaskTemplate
- Fields: Name, Description, Prompt Template, Tags, Evaluation Notes
- Variable syntax highlighting (`{{variable}}`)
- Variable preview panel
- Validation feedback

---

#### 8.7 Implement Task Library Page - Import/Export
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.5  
**Description**:
- Export all as JSON button
- Import from JSON file
- Conflict resolution (skip/overwrite)

---

#### 8.8 Implement RAG Lab Page - Document Management
**Labels**: `child-8`, `frontend`  
**Status**:  Parallel  
**Description**:
- Document upload area (drag & drop)
- Document list with chunk count
- Delete document action
- Embedding model selector
- Chunk size/overlap configuration

**File**: `pages/RagLab.tsx`

---

#### 8.9 Implement RAG Lab Page - Query Interface
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.8  
**Description**:
- Query input
- Retrieved chunks display (X-Ray panel)
- Relevance scores with visual indicator
- Context preview (what's sent to LLM)
- Response display with metrics

---

#### 8.10 Implement Experiment Builder Page - Task Selection
**Labels**: `child-8`, `frontend`  
**Status**:  Parallel  
**Description**:
- Step 1: Select Task Template
- Task template dropdown/search
- Preview of selected task
- Variable inputs if task has variables

**File**: `pages/ExperimentBuilder.tsx`

---

#### 8.11 Implement Experiment Builder Page - Configuration
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.10  
**Description**:
- Step 2: Context Mode (None/RAG/Full Context)
- Step 3: Model Pool (multi-select)
- Step 4: Embedding Pool (if RAG, multi-select)
- Step 5: Iterations count
- Step 6: Hyperparameters (fixed or ranges)

---

#### 8.12 Implement Experiment Builder Page - Pipeline Visualiser
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.11  
**Description**:
- Visual node graph: Task → Context → Model → Output
- Nodes reflect configuration
- Static layout (not drag-drop)
- Experiment matrix preview showing total runs

**File**: `components/experiments/PipelineVisualiser.tsx`

---

#### 8.13 Implement Experiment Runner Page
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 7.12  
**Description**:
- Experiment queue list
- Progress dashboard: overall progress bar, current run
- Live log stream (WebSocket)
- Pause/Resume/Cancel buttons
- Run-by-run status indicators

**File**: `pages/ExperimentRunner.tsx`

---

#### 8.14 Implement Results Dashboard - Leaderboard
**Labels**: `child-8`, `frontend`  
**Status**:  Parallel  
**Description**:
- Bar charts: Average TPS, Average Latency, Success Rate
- Filter by experiment, model, embedding
- Sortable columns
- Export buttons (CSV/JSON)

**File**: `pages/ResultsDashboard.tsx`
**Component**: `components/results/Leaderboard.tsx`

---

#### 8.15 Implement Results Dashboard - Comparison Matrix & Deep Dive
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.14  
**Description**:
- Pivot table: Rows = Tasks, Columns = Models
- Truncated output with expand
- Color coding by metrics
- Deep Dive Modal showing full details

**Components**:
- `components/results/ComparisonMatrix.tsx`
- `components/results/DeepDiveModal.tsx`

---

#### 8.16 Implement Embedding Analyser Page
**Labels**: `child-8`, `frontend`  
**Status**: Depends on: 8.9  
**Description**:
- Document selector
- Query input
- Multi-embedding model selector
- Side-by-side chunk comparison
- Relevance score visualisation
- Overlap analysis display

**File**: `pages/EmbeddingAnalyser.tsx`

---

## Execution Order Summary

### Phase 1: Foundation (Issues 1.1-1.8)
All infrastructure issues can largely be done in parallel. Start with 1.1-1.4 simultaneously, then 1.5-1.8.

### Phase 2: Data Layer (Issues 2.1-2.8, 3.1-3.6)
Entities (2.x) can all be built in parallel. Repositories (3.x) can also be parallel but should wait for their entity.

### Phase 3: Integration & Services (Issues 4.1-4.6, 5.1-5.8)
Clients (4.x) can begin once backend foundation exists. Services (5.x) depend on repositories and clients.

### Phase 4: API Layer (Issues 6.1-6.10)
Controllers can be built in parallel once their services exist.

### Phase 5: Frontend Foundation (Issues 7.1-7.12)
Base setup (7.1-7.4) parallel. Components and hooks follow.

### Phase 6: Frontend Features (Issues 8.1-8.16)
Pages can largely be built in parallel. Within each page, basic → advanced features.

---

## Recommended Team Distribution

If working with multiple developers:

| Developer | Focus | Issues |
|-----------|-------|--------|
| Dev 1 (Backend) | Infrastructure + Data | 1.1-1.3, 1.5, 1.7-1.8, 2.x, 3.x |
| Dev 2 (Backend) | Clients + Services | 4.x, 5.x |
| Dev 3 (Backend) | Controllers + WebSocket | 6.x |
| Dev 4 (Frontend) | Foundation + Components | 1.4, 1.6, 7.x |
| Dev 5 (Frontend) | Feature Pages | 8.x |

For a single developer, follow phases sequentially with the parallel groupings within each phase.

---

## Progress Tracking Template

```markdown
## Sprint Progress

### Parent Issue 1: Infrastructure ⬜ 0/8
- [ ] 1.1 Git Repository Setup
- [ ] 1.2 Docker Compose
- [ ] 1.3 Spring Boot Init
- [ ] 1.4 React Init
- [ ] 1.5 Backend Quality Tools
- [ ] 1.6 Frontend Quality Tools
- [ ] 1.7 Backend Config
- [ ] 1.8 Exception Handling

### Parent Issue 2: Data Model ⬜ 0/8
- [ ] 2.1 Enums
- [ ] 2.2 TaskTemplate Entity
- [ ] 2.3 SystemPrompt Entity
- [ ] 2.4 EmbeddingModel Entity
- [ ] 2.5 Document Entity
- [ ] 2.6 Experiment Entity
- [ ] 2.7 ExperimentRun Entity
- [ ] 2.8 Seed Data

### Parent Issue 3: Repository Layer ⬜ 0/6
- [ ] 3.1 TaskTemplateRepository
- [ ] 3.2 SystemPromptRepository
- [ ] 3.3 EmbeddingModelRepository
- [ ] 3.4 DocumentRepository
- [ ] 3.5 ExperimentRepository
- [ ] 3.6 ExperimentRunRepository

### Parent Issue 4: External Clients ⬜ 0/6
- [ ] 4.1 OllamaClient Interface
- [ ] 4.2 OllamaClient Implementation
- [ ] 4.3 Ollama DTOs
- [ ] 4.4 ChromaClient Interface
- [ ] 4.5 ChromaClient Implementation
- [ ] 4.6 Chroma DTOs

### Parent Issue 5: Service Layer ⬜ 0/8
- [ ] 5.1 OllamaService
- [ ] 5.2 TaskService
- [ ] 5.3 SystemPromptService
- [ ] 5.4 DocumentService
- [ ] 5.5 RagService
- [ ] 5.6 ExperimentService
- [ ] 5.7 ExperimentExecutorService
- [ ] 5.8 AnalyticsService

### Parent Issue 6: Controller Layer ⬜ 0/10
- [ ] 6.1 WebConfig
- [ ] 6.2 WebSocketConfig
- [ ] 6.3 OllamaController
- [ ] 6.4 TaskController
- [ ] 6.5 SystemPromptController
- [ ] 6.6 DocumentController
- [ ] 6.7 ExperimentController
- [ ] 6.8 ResultController
- [ ] 6.9 AnalyticsController
- [ ] 6.10 WebSocket Handler

### Parent Issue 7: Frontend Foundation ⬜ 0/12
- [ ] 7.1 Router & Layout
- [ ] 7.2 API Client
- [ ] 7.3 Config Store
- [ ] 7.4 Type Definitions
- [ ] 7.5 ModelSelector
- [ ] 7.6 ConfigPanel
- [ ] 7.7 MetricsDisplay
- [ ] 7.8 CodeBlock
- [ ] 7.9 Ollama Hooks
- [ ] 7.10 Task/Prompt Hooks
- [ ] 7.11 Experiment Hooks
- [ ] 7.12 WebSocket Hook

### Parent Issue 8: Feature Pages ⬜ 0/16
- [ ] 8.1 Sandbox - Basic
- [ ] 8.2 Sandbox - Advanced
- [ ] 8.3 Arena - Layout
- [ ] 8.4 Arena - Comparison
- [ ] 8.5 Task Library - List
- [ ] 8.6 Task Library - Modal
- [ ] 8.7 Task Library - Import/Export
- [ ] 8.8 RAG Lab - Documents
- [ ] 8.9 RAG Lab - Query
- [ ] 8.10 Experiment Builder - Task
- [ ] 8.11 Experiment Builder - Config
- [ ] 8.12 Experiment Builder - Pipeline
- [ ] 8.13 Experiment Runner
- [ ] 8.14 Results - Leaderboard
- [ ] 8.15 Results - Matrix & Modal
- [ ] 8.16 Embedding Analyser

**Total: 0/74 Complete**
```
