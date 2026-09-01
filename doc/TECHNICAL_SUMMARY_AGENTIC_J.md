# Agentic-J — Technical Summary

## Overview

A Docker-first Python application that runs an LLM-powered multi-agent system inside Fiji/ImageJ.
The primary deliverable is **executable Groovy/Python scripts** for bioimage analysis workflows —
agents write, test, debug, and iterate on scripts against a live Fiji instance rather than producing
chat-only answers.

- **Language**: Python 3.13
- **License**: Apache-2.0
- **Build/env**: conda (`environment.yml`) — OpenJDK 21 and Maven included in the conda env
- **Primary deployment**: Docker (`Dockerfile`, `docker-compose.yml`, GPU variant available)
- **GUI entry point**: `gui_runner.py` (PySide6 desktop app)

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│ PySide6 GUI (gui_runner.py)                                      │
│  Chat bubbles, per-conversation usage metrics, QA toggle         │
│  Thread-based agent invocation (QThread)                         │
├──────────────────────────────────────────────────────────────────┤
│ LangGraph Supervisor (stateful — SqliteSaver checkpoints)        │
│  Orchestrates handoffs to stateless subagents                    │
├──────────────────────────────────────────────────────────────────┤
│ Stateless Subagents (LangChain agents, module-level instances)   │
│  Coder | Debugger | Analyst | QA | Plugin Manager | Librarian   │
├──────────────────────────────────────────────────────────────────┤
│ Fiji/ImageJ (pyimagej + scyjava + jpype, interactive mode)       │
│  Groovy/Python scripts executed in-process in the JVM            │
└──────────────────────────────────────────────────────────────────┘
```

---

## LLM Integration

### Framework

**LangChain + LangGraph** is the core agentic framework, with the `deepagents` library
(`create_agent`, `create_deep_agent`) providing agent construction and tool-loop execution on top of it.
This is the opposite approach from CopilotJ, which implements its own ReAct loop; here the framework owns
the loop.

**LangSmith** is the observability backend (configured via `LANGCHAIN_*` env vars). A custom
`UsageTrackerCallback` (LangChain callback) aggregates per-session token usage across all agents and
emits signals to the GUI.

### Models

All LLM calls go through **OpenRouter** (preferred) or **OpenAI direct**, via `ChatOpenAI` with a
configurable `base_url`. Different models are assigned per agent role:

| LLM alias | Model | Used by |
|---|---|---|
| `llm_supervisor` | `gpt-5.2` (reasoning_effort=low) | Supervisor orchestrator |
| `llm_worker` | `gpt-5.3-codex` (reasoning_effort=low) | Coder + Debugger |
| `llm_analyst` | `gpt-5.2` (reasoning_effort=none) | Analyst + QA + Plugin Manager |
| `llm_nano` | `gpt-5.4-nano` | Fast / cheap subagent tasks |
| `llm_curator` | `gpt-5.4-mini` | Background Librarian (timeout=30s, max_retries=1) |

VLM support is fully scaffolded (VLM Judge agent, `capture_ij_window`, `build_compilation`,
`analyze_image` tools, `VLMHandoff` schema) but disabled via comments — toggling it requires
uncommenting and supplying a vision-capable model.

---

## Multi-Agent Architecture

### Supervisor (Stateful)

The supervisor is a LangGraph graph. State is persisted to **SQLite** via `SqliteSaver` (falls back to
in-memory `MemorySaver` if the sqlite checkpoint package is absent). This means conversation history
survives process restarts by default.

The supervisor orchestrates the overall workflow across pipeline stages and dispatches to subagents.

### Subagents (Stateless)

All subagents are created once at module load time as module-level instances. They are stateless —
context is passed in per-call, not retained. Each is constructed with `create_agent(model, tools,
system_prompt, response_format=...)` from `deepagents`.

| Agent | Model | Primary responsibility |
|---|---|---|
| `imagej_coder` | llm_worker | Writes new Groovy/Python scripts from scratch |
| `imagej_debugger` | llm_worker | Diagnoses and patches failing scripts |
| `python_data_analyst` | llm_analyst | Statistics and plotting scripts |
| `qa_reporter` | llm_analyst | Validates workflow artifacts against a checklist |
| `plugin_manager` | llm_analyst | Discovers, evaluates, and installs Fiji plugins |
| `librarian` | llm_curator | Background curation of the learned-memory wiki |

### Structured Handoffs

Subagents return **Pydantic models** as structured output (via `ToolStrategy(schema=..., handle_errors=True)`),
not free text. The supervisor receives typed handoffs:

- `ScriptHandoff` — script path, success/failure, stage (io_check | preprocessing | segmentation | etc.),
  and optional `lesson`/`failed_code`/`working_code` fields for the Librarian
- `AnalystHandoff` — stats CSV, figure paths, same lesson fields
- `QAHandoff` — checklist path, pass/fail counts, critical failures
- `PluginRecommendation` — plugin name, install status, skill folder, alternatives

### Recursion Safety

Each subagent is capped at `_RECURSION_LIMIT = 45` super-steps (configurable via env var). On cap hit,
the system attempts to salvage any script already written (success=True if file exists and is non-empty)
rather than failing hard — because in practice the cap is usually hit after a complete save during a
self-verification loop.

---

## Tool System

Tools are **`@tool`-decorated functions** (LangChain). Categories:

| Module | Tools |
|---|---|
| `script_tools` | `save_script`, `edit_script` (surgical patch), `load_script`, `get_script_history`, `get_script_info`, `copy_file` |
| `imagej_tools` | `show_in_imagej_gui`, `close_imagej_windows`, `inspect_all_ui_windows`, `capture_plugin_dialog`, `execute_script` |
| `plugin_tools` | `search_fiji_plugins`, `check_plugin_installed`, `install_fiji_plugin` |
| `rag_tools` | `rag_retrieve_docs` |
| `file_tools` | `inspect_folder_tree`, `smart_file_reader`, `inspect_csv_header`, `mkdir_copy`, `save_markdown` |
| `metadata_tools` | `extract_image_metadata`, `inspect_java_class` |
| `general_tools` | `internet_search` (DuckDuckGo) |
| `analyst_tools` | `setup_analysis_workspace`, `kill_running_processes` |
| `mcp_host_tools` | `get_mcp_tools` — dynamically loads tools from external MCP servers |
| `state_ledger` | `update_state_ledger`, `read_state_ledger`, `set_ledger_metadata`, `get_ledger_context` |
| `vision_tools` | `capture_ij_window`, `build_compilation`, `analyze_image` (all disabled) |
| `learned_memory` | `recall`, `register_pending_lesson`, `core_pitfalls`, `core_recipes`, `library_*` |
| `middleware` | `NarrationReminderMiddleware`, `PhaseGuardMiddleware` |

**`edit_script`** is deliberately withheld from the Analyst (gpt-5.2) — A/B testing showed it tripled
the loop rate; that model re-reads to verify and cascades. `edit_script` is kept on gpt-5.3-codex
(Coder/Debugger) which trusts its patches and terminates.

### State Ledger

A shared mutable key-value store (`state_ledger.py`) visible to all agents in a session. Used to pass
metadata and intermediate state that doesn't fit cleanly in handoff schemas.

### MCP Host

`mcp_host_tools.py` is a generic MCP client: it reads an `mcpServers` config JSON, calls `tools/list`
on each server, converts returned `inputSchema` to `StructuredTool` (LangChain), and exposes them as
regular tools. The MCP transport runs on a dedicated background asyncio thread (a persistent
`threading.Thread` with its own event loop) to bridge sync LangChain tool calls into async MCP I/O.

---

## Context Management (Middleware)

`deepagents` / `langchain.agents.middleware` provides a middleware chain per agent:

| Middleware | Effect |
|---|---|
| `FilesystemFileSearchMiddleware` | Ripgrep-based search scoped to `/app/skills/`; gives agents filesystem-aware lookup without full glob over the data directory |
| `ContextEditingMiddleware` + `ClearToolUsesEdit` | Drops old tool calls from context when token count exceeds 50 000 tokens (keeps last 10), preventing runaway context growth |
| `SkillsMiddleware` | Progressive disclosure — injects skill summaries from `SKILL.md` files and lets agents request full content on demand; used by Plugin Manager and Librarian |
| `NarrationReminderMiddleware` | Custom — reminds agents to narrate actions |
| `PhaseGuardMiddleware` | Custom — enforces pipeline phase ordering |

---

## Learned Memory (Persistent Wiki)

A file-based, language-partitioned wiki under `/data/learned/`:

```
pitfalls/
  CORE.<Lang>.md     # always injected (≤12 entries/language)
  <Lang>.md          # pulled via recall()
recipes/
  CORE.<Lang>.md     # always injected (≤5 entries/language)
  <Lang>.md          # full recipe library
  code/              # verified scripts (read on demand)
log.md               # append-only audit trail
```

**Retrieval** (`recall()`) is deterministic: CORE entries are injected as a fixed-size floor; the rest
are pulled by token overlap (with an optional LLM deep-search fallback gated behind `LEARNED_DEEP_RECALL`).
A recipe is flagged as a "strong match" (`RECIPE_STRONG_COVER = 0.5` token coverage) and reused verbatim
vs. used as a template for weaker matches.

**Curation** is handled by the background Librarian agent (fired in a daemon thread on every
verified-green script run, so the main task never waits). The Librarian acts only through deterministic
`library_*` tools. It runs a two-tier lint cadence: recent entries every 3 dispatches, a rolling shard
of the full library every 10 dispatches, with a persisted cursor so all entries are covered over time.

---

## RAG System

**Qdrant** (local, persistent on disk) is the vector store. Hybrid search:

- **Dense**: OpenAI `text-embedding-3-large` (via OpenRouter or OpenAI direct)
- **Sparse**: BM25 via `FastEmbedSparse` (`Qdrant/bm25`)
- **Fusion**: Reciprocal Rank Fusion (RRF) across both result sets

Document loading uses **Docling** `HybridChunker` for PDFs (with selective OCR) and falls back to
standard loaders for other file types. Deduplication is by SHA-256 file hash.

The `/skills/` directory contains extensive per-plugin documentation folders (Cellpose, StarDist,
TrackMate, BigStitcher, MorphoLibJ, CSBDeep, Ilastik, DeepImageJ, and ~20 more), plus a `workflow/`
folder — all indexed into Qdrant.

---

## ImageJ Integration

- **`pyimagej`** (`1.7.0`) + **`scyjava`** (`1.12.1`) + **`jpype`** (`1.6.0`) for the JVM bridge
- ImageJ initialized in **interactive mode** (headless is not used) via `imagej.init(FIJI_JAVA_HOME, mode='interactive')`
- JVM options: `-Xmx6g`, Maven fetch disabled (`fetch='never'`) to prevent network calls at startup
- Groovy scripts run via `ij.py.run_script()` in-process; Python scripts run as subprocesses
- ImageJ instance is a module-level singleton (`_ij_instance`)

---

## GUI

**PySide6** desktop application (`gui_runner.py`):
- Chat bubble interface with markdown rendering
- Per-conversation token/cost tracking via `UsageMetrics` + `MetricsSignalBridge` (Qt signals)
- QA toggle (enable/disable the QA Reporter subagent per session)
- `QThread`-based agent invocation to keep the UI responsive
- Benchmark mode hook (`benchmark_gui_hooks.py`) for automated testing

Chat history is persisted as per-thread JSON files under `/data/chats/`, indexed by a flat `index.json`
(thread ID → title + timestamps). No database — plain filesystem.

---

## Extension Points

| What to extend | How |
|---|---|
| New subagent role | Call `create_agent(model, tools, system_prompt, response_format=...)` and wire into the supervisor graph |
| New tool | Write a `@tool`-decorated function; add to the relevant agent's tool list |
| New plugin documentation | Add a `SKILL.md` + docs folder under `/app/skills/`; it is auto-indexed by Qdrant and picked up by `SkillsMiddleware` |
| New MCP server | Add an entry to the `mcpServers` config JSON; `get_mcp_tools` discovers it at runtime |
| Enable VLM support | Uncomment the VLM agent + handoff schema + vision tools; configure a vision-capable model |
| New learned-memory language | The CORE/library system is language-keyed; a new language gets its own files automatically |

---

## Key Dependencies Summary

| Library | Role |
|---|---|
| `langchain` / `langchain-core` | Tool definitions, agent middleware, callbacks |
| `langgraph` + `langgraph-checkpoint-sqlite` | Supervisor graph + stateful conversation persistence |
| `deepagents` | Agent construction (`create_agent`) and tool-loop execution |
| `langsmith` | LLM observability |
| `langchain-openai` | `ChatOpenAI` model client (OpenAI + OpenRouter) |
| `langchain-anthropic` | Anthropic model client (available, not wired to agents) |
| `langchain-qdrant` | Qdrant vector store integration |
| `qdrant-client` | Qdrant local persistence |
| `fastembed` | Sparse BM25 embeddings for hybrid RAG |
| `docling` | PDF chunking with OCR |
| `mcp` / `fastmcp` | MCP client transport |
| `pyimagej` / `scyjava` / `jpype` | JVM bridge to Fiji |
| `PySide6` | Desktop GUI |
| `pydantic` | Handoff schemas, structured output |
