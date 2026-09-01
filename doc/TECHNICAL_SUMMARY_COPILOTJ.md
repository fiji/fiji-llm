# CopilotJ — Technical Summary

## Overview

A multi-agent bioimage analysis assistant that connects an LLM-powered Python backend to Fiji/ImageJ.
Natural-language requests are translated into executable image analysis workflows via a coordinated agent system,
bridging the Python scientific stack with the Fiji plugin ecosystem.

- **Primary language**: Python 3.12 (strict upper bound `<3.13`)
- **Secondary**: Java (Maven, ImageJ plugin)
- **Frontend**: TypeScript / Vue 3 SPA
- **License**: Apache-2.0
- **Build tooling**: `uv` + Nix flake (reproducible Python env), Maven (Java plugin), Vite (frontend)
- **Task runner**: `just`

---

## Three-Component Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│ Web Frontend (Vue 3 SPA)                                         │
│  REST + NDJSON streaming ↕                                        │
├──────────────────────────────────────────────────────────────────┤
│ Bridge Server (Python / aiohttp)                                 │
│  Agent orchestration, LLM calls, event streaming                 │
│  WebSocket ↕                                                     │
├──────────────────────────────────────────────────────────────────┤
│ ImageJ Plugin (Java / Fiji)                                      │
│  Script execution, UI state snapshots, AWT widget interaction    │
└──────────────────────────────────────────────────────────────────┘
```

The bridge server optionally runs inside Fiji via **Appose** (managed mode), where the main thread is the
Appose stdin/stdout loop and the asyncio event loop runs on a daemon thread — imposing constraints on any
code that requires the main thread (signal handlers, tkinter, etc.).

---

## LLM Integration

### ModelClient Abstraction

CopilotJ uses a **custom `ModelClient` protocol** rather than a third-party LLM framework. Each provider
implements async streaming and non-streaming chat completion. Supported providers:

| Client | Backend |
|---|---|
| `OpenAIChatCompletionClient` | OpenAI Chat Completions API |
| `OpenAIResponseClient` | OpenAI Responses API |
| `AnthropicChatCompletionClient` | Anthropic Messages API |
| `GeminiChatCompletionClient` | Google GenAI SDK |
| `OllamaChatCompletionClient` | Ollama local HTTP API |
| `OpenRouterChatCompletionClient` | OpenRouter (multi-model gateway) |

Provider selection is fully config-driven via environment variables (`COPILOTJ_LLM_*`). A separate VLM
(vision model) can be configured independently and is opt-in (`COPILOTJ_VISION_ENABLED=1`).

`ReActChatCompletionClient` is a wrapper that parses raw text output in ReAct format into structured
`ToolCall` objects, enabling ReAct-style reasoning with providers that lack native tool-call support.

### Chat Memory and Summarization

`LeaderAgent` maintains dialog-level conversation history in a plain Python list. History is periodically
compressed: when the list exceeds a threshold (`PROMPT_CHAT_HISTORY_LIMIT = 8`), a summarization call
is made to the LLM and older turns are replaced with the summary. There is no external memory library.

### Observability

**Langfuse** (`>=4.0.0`) is integrated for LLM call tracing. OpenTelemetry instrumentation packages are
included for Anthropic and Google GenAI providers. Enabled via `LANGFUSE_SECRET_KEY` / `LANGFUSE_PUBLIC_KEY`.

---

## Agentic Architecture

### Core Abstractions (`copilotj/core/`)

| Class | Role |
|---|---|
| `Agent` | Base class. Holds name/description, message handler registry, runtime reference. |
| `ChatAgent` | Extends `Agent`. Adds LLM streaming, ReAct loop, retry logic with exponential backoff. |
| `Runtime` | Shared logging and UI dispatch layer for all agents in a pattern. |
| `Pattern` | Groups agents, owns the shared `Runtime`, routes messages. Agent assignment via attribute set triggers auto-registration. |

### ReAct Loop

Agents operate in a **Reason → Act** loop. The LLM produces free-text reasoning followed by a structured
JSON action block (`{"name": "...", "args": {...}}`). The agent executes the tool, appends the result as
an observation, and loops until the model produces a final answer or reaches the iteration limit.
`ChatAgent` handles retries (up to `max_tool_retry`) on parse errors and execution failures.

### Multi-Agent Pattern: `LeaderDriven`

The primary orchestration pattern. Creates:

- **`LeaderAgent`** — orchestrator that perceives ImageJ state, reasons over the request, runs macros and
  Python scripts directly, and delegates complex sub-tasks to specialized `Executor` agents via handoff tools.
  Manages dialog-level history with summarization.

- **`Executor` agents** — generic specialized agents loaded from TOML config files at startup via `agent_loader.py`
  (uses `importlib` + glob). Each has its own system prompt and tool list. ReAct loop with error recovery.
  Active configs:
  - `tool_agent.toml` — bioimage tool specialist (Cellpose, StarDist, BiaPy, etc.)
  - `research_agent.toml` — literature/web research
  - `success_case.toml` — retrieves successful past workflow patterns
  - `imagej_macro_help.toml` — macro syntax and function help

  Disabled configs (`.disabled.toml`) are skipped by the loader without deletion.

**Adding a new specialized agent** requires only a new TOML file in `agent_configs/` — no code changes.

---

## Tool System

### Tool Model

`Tool` is a **Pydantic `BaseModel`** with abstract `name`, `description`, `json_schema`, `args_type()`, and
`async run(args)`. JSON schemas are auto-generated from Pydantic models, with `jsonref` resolving `$ref`
pointers before sending to LLMs.

`FunctionTool` wraps a plain Python async function: it introspects the signature using `inspect` and builds
the `ToolSchema` from type annotations, `Annotated[T, description]` metadata, and `Field` defaults. Both sync
and async callables are supported.

`HandoffFunctionTool` is a `FunctionTool` subclass that, when called, emits a `UIEventHandoff` event to the
frontend before executing — enabling the UI to show handoff transitions between agents.

### Leader Tools (built-in)

- **ImageJ perception** — snapshots UI state (open images, active window, AWT widget tree)
- **Run macro** — executes ImageJ macros via the plugin bridge
- **Execute Python script** — runs arbitrary Python in the ImageJ context via pyimagej
- **Knowledge bank retrieve** — semantic search over the RAG index
- **Save/load/delete/list workflows** — workflow persistence
- **Batch QC / folder summary** — batch processing helpers
- **User manipulation** — requests the user to interact with ImageJ directly (with confirmation)

---

## ImageJ Plugin Bridge

### Python Side (`copilotj/plugin/`)

`PluginAPI` is a typed interface for communicating with the Java plugin. Implementations:

- `HTTPPluginAPI` — direct HTTP to the plugin (non-managed mode)
- `BridgePluginAPI` — goes through the aiohttp bridge server via WebSocket

Methods include `take_snapshot()`, `run_script()`, `capture_image()`, `call_action()`.

The Python side mirrors the Java AWT widget tree in `copilotj/plugin/awt/`, providing typed Python objects
(buttons, checkboxes, sliders, etc.) for UI interaction without reflection.

### Java Side (`plugin/`)

Maven project using SciJava/ImageJ2 APIs:

| Class | Role |
|---|---|
| `DefaultCopilotJBridgeService` | WebSocket client connecting to the Python bridge |
| `SnapshotManager` | Captures UI state (open images, AWT widget tree) |
| `ScriptRunner` | Executes macros and scripts in the ImageJ script engine |
| `ImagejListener` | Tracks ImageJ operation history |

---

## Knowledge Bank (RAG)

TOML-based document store with two subdirectories:
- `macro/` — ImageJ macro snippets
- `research/` — dialog-derived workflow insights

**Vector index**: FAISS via `langchain-community`, with **sentence-transformers** embeddings. Index is rebuilt
from JSONL exports (gzip-compressed) keyed by a content hash to avoid unnecessary rebuilds.

**Ingestion**: documents are loaded from TOML, PDF (PyMuPDF), and web (Wikipedia API, Selenium + BeautifulSoup)
sources. When `COPILOTJ_KB_AUTOSAVE=1`, dialog summaries are automatically ingested after each session.

**Retrieval**: semantic search over the FAISS index; used by `LeaderAgent` as a tool call to prime context
before macro or tool execution.

---

## HTTP Server and Streaming

`Server` (`copilotj/server/`) is an **aiohttp** application with CORS via `aiohttp-cors`.

| Route | Purpose |
|---|---|
| `GET/POST/DELETE /api/threads` | Conversation thread CRUD |
| `POST /api/threads/{id}/posts` | Chat — streams `UIEvent`s as **NDJSON** |
| `WS /api/plugins` | WebSocket hub for ImageJ plugin clients |
| `GET /api/config` | Exposes runtime configuration to frontend |

Each thread creates a `LeaderDriven` pattern as a background asyncio task. UI events
(`UIEventPost`, `UIEventToolCall`, `UIEventHandoff`, `UIEventRetry`, etc.) are Pydantic models
serialized line-by-line as NDJSON. The frontend parses the stream incrementally for real-time rendering.

---

## Web Frontend (`web/`)

Vue 3 SPA with:
- **PrimeVue** — UI component library
- **Tailwind CSS** — utility-first styling
- **Pinia** — state management
- **Vue Router** — routing
- **`@tabler/icons-vue`** — icon set (sole icon dependency)

Views: `Chat.vue` (main), `Manual.vue`, `About.vue`, `Home.vue`.
NDJSON stream parsing in `web/src/apis/`.

---

## Domain-Specific Bioimage Capabilities

CopilotJ carries a large scientific Python dependency set as first-class runtime tools:

| Category | Libraries |
|---|---|
| Deep learning segmentation | Cellpose, StarDist, BiaPy, CSBDeep |
| GPU-accelerated image processing | pyclesperanto-prototype |
| General image analysis | scikit-image, OpenCV |
| Tracking | trackpy |
| Neuroscience | suite2p |
| DL frameworks | PyTorch, TensorFlow, timm |
| Visualization | matplotlib, seaborn, plotly, Altair, HoloViews, Bokeh |
| Web research | Tavily, DuckDuckGo Search, Wikipedia, Selenium |

---

## Extension Points

| What to extend | How |
|---|---|
| New specialized agent | Add a `*_agent.toml` to `copilotj/multiagent/agent_configs/` |
| New tool | Implement `FunctionTool(func, description)` or subclass `Tool` |
| New LLM provider | Implement `ModelClient` protocol; wire in `new_model_client()` |
| New plugin bridge method | Add to `PluginAPI` interface + Java `DefaultCopilotJBridgeService` |
| New knowledge bank source | Add ingestion logic; re-run FAISS index rebuild |
| Disable an agent without deleting | Rename config to `*.disabled.toml` |

---

## Key Dependencies Summary

| Library | Version | Role |
|---|---|---|
| `aiohttp` | ≥3.14.1 | Async HTTP server + WebSocket bridge |
| `pydantic` | ≥2.10.6 | Tool schemas, event models, config |
| `appose` | ≥0.5.1 | Managed Python-in-JVM execution mode |
| `openai` | ≥1.63.2 | OpenAI API client |
| `anthropic` | ≥0.52.0 | Anthropic API client |
| `google-genai` | ≥1.0.0 | Gemini API client |
| `langchain-community` | ≥0.4.2 | FAISS vector store |
| `sentence-transformers` | ≥5.5.1 | RAG embeddings |
| `faiss-cpu` | ≥1.14.2 | Vector similarity search |
| `langfuse` | ≥4.0.0 | LLM observability |
| `pyimagej` | ≥1.5.0 | Python-ImageJ interop |
| `rich` / `textual-image` | — | Terminal CLI fallback UI |
