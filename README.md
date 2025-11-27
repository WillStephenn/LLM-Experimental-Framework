# LocalLab - LLM Experimental Framework

A local-first experimental framework for rigorously testing, benchmarking, and comparing small Large Language Models (LLMs) via Ollama.

## Overview

LocalLab enables systematic evaluation of LLMs with full reproducibility and transparency. Unlike chat playgrounds, this system is built for experimentation:

**Define a task → Run it across N models × M configurations × X iterations → Analyse results → Determine the optimal model for your use case.**

## Key Features

- **Visual Pipeline Builder** - Configure experiments as visual node graphs
- **RAG Embedding Comparison** - Test retrieval quality across different embedding models
- **Full Transparency** - Every stage of processing is inspectable
- **Experiment-First Design** - Built for systematic evaluation, not casual chat

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21 + Spring Boot 3.x |
| Frontend | React 18 + TypeScript + Vite |
| Database | H2 (embedded) |
| Inference | Ollama (via ollama4j) |
| Vector Store | Chroma |

## License

MIT
