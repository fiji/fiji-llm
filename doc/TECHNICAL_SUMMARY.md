# fiji-llm — Technical Summary

## Overview

A Fiji/ImageJ plugin that integrates large language models into the bioimage analysis platform. Exposes LLM chat and agentic tool-use capabilities directly within the ImageJ desktop application.

- **Language**: Java 17+
- **Build**: Maven, parent POM `org.scijava:pom-scijava:43.0.0`
- **License**: BSD-2-Clause

---

## Guidance and Knowledge

The planned shared guidance architecture defines how Fiji-specific knowledge
is made available to the integrated chatbot and external MCP clients without
loading the full documentation corpus into every request. A compact baseline
is used by both routes, while detailed guidance is retrieved on demand through
read-only tools. Both routes direct unfamiliar Fiji-specific workflows to the
curated `onboarding` document before other Fiji tools are used. Current
installation and session facts remain the responsibility of live Fiji tools
and application context.

The knowledge boundary, document metadata, authority rules, retrieval contract,
and future directions are described in the [Shared Guidance Architecture](GUIDANCE_ARCHITECTURE.md).
Built-in guide plugins use the explicit `AgentGuide.Topic` vocabulary when
declaring topics. Metadata remains string-based so third-party guide plugins can
add topic keywords without extending the enum.

---

## Core LLM Integration

### Abstraction Layer

**LangChain4j** (`1.13.1`) is the sole LLM integration framework. It provides:
- `ChatModel` / `StreamingChatModel` interfaces per provider
- `AiServices` builder for wiring models, memory, and tools into a typed assistant
- `TokenWindowChatMemory` for sliding-window context management
- `@Tool` annotation-driven tool specification and execution

### Provider Plugin System

`LLMProvider` is a **SciJava `SingletonPlugin`** interface. Each provider creates LangChain4j model instances on demand. Providers are discovered at runtime via SciJava's annotation-based plugin registry.

| Provider | Backend | Local/Cloud |
|---|---|---|
| `OpenAIProvider` | `langchain4j-open-ai` | Cloud |
| `AnthropicProvider` | `langchain4j-anthropic` | Cloud |
| `GeminiProvider` | `langchain4j-google-ai-gemini` | Cloud |
| `OllamaProvider` | `langchain4j-ollama` | Local |
| `Gemma4Provider*`, `GlimmerProviderIQ2` | Ollama (managed subprocess) | Local |

Ollama providers optionally manage the Ollama process lifecycle via `OllamaProcessManager`. `AbstractSingletonOllamaProvider` bakes in a specific model name, allowing each local model variant to be its own discoverable plugin.

Model preparation is asynchronous. `LLMProvider.prepare()` completes with an optional user-facing message while preparation failures complete exceptionally. Ollama uses `/api/ps` after preparation to compare `size_vram` with `size`; partial GPU residency produces a performance warning in the chat UI.

Cloud providers extend `AbstractLLMProvider`, which retrieves API keys from `APIKeyService` (stored via SciJava `PrefService`).

Providers report model image-input support through `LLMProvider.VisionSupport`. Hosted providers maintain this classification for their fixed model lists. Ollama queries `/api/show` after preparing a model and caches the reported capabilities; unavailable capability metadata is represented as `UNKNOWN`.

### Assistant Construction

`DefaultAssistantService.createAssistant()` wires together a typed assistant using LangChain4j's `AiServices` builder:

```
AiServices.builder(FijiAssistant.class)
  .chatModel(provider.createChatModel(modelName))
  .streamingChatModel(provider.createStreamingChatModel(modelName))
  .toolProvider(mcpService.getToolProvider())
  .chatMemory(chatMemory)
  .chatRequestTransformer(...)  // injects default parameters (temp, penalties)
  .build()
```

`FijiAssistant` exposes both blocking (`chat()`) and streaming (`chatStreaming()` → `TokenStream`) modes.

---

## Tool Use

### AiToolPlugin

`AiToolPlugin` is a SciJava `SingletonPlugin` interface. Implementations annotate methods with LangChain4j's `@Tool` to define callable tools. `AiToolService` aggregates all discovered `AiToolPlugin` instances and exposes their `ToolSpecification` / `ToolExecutor` maps.

Tool behavior and usage guidance are declared in each `@Tool` description.
The plugin contract does not include a separate namespace-level usage string;
shared Fiji workflow guidance is maintained in the packaged guidance catalog.

Tools are scoped via a `ToolScope` string (e.g. `MACRO`) to allow context-sensitive filtering.

Built-in tools include:
- **`ImageJMacroToolPlugin`** — macro recorder integration, macro function discovery
- **`CommandUseToolPlugin`** — ImageJ command discovery and execution with a lightweight before/after environment report
- **`ScriptEditorToolPlugin`** — script editor interaction
- **`ImageToolPlugin`** — open image listing, metadata, and rendered image content
- **`ResultsTableToolPlugin`** — read-only Results Table inspection
- **`RoiManagerToolPlugin`** — read-only ROI Manager inspection
- **`SystemInfoToolPlugin`** — read-only host, JVM, application version, and update-site inspection
- **`LogToolPlugin`** — ImageJ and SciJava log inspection
- **`UiToolPlugin`** — visible AWT and Swing window/control inspection, screenshots, dialog inspection, and exact button responses
- **`ScriptExecutionService`** — shared Script Editor execution, timeout handling, state snapshots, and dialog-aware run status for scripts and `.ijm` macros

`.ijm` macros are executed only by calling `TextEditor.runText()` on the active tab in a visible Script Editor. `fiji_script_run` rejects active `.ijm` tabs and directs callers to `fiji_macro_run`. Script and macro runs return one of `success`, `finished_with_errors`, `blocked_by_dialog`, `timed_out`, or `infrastructure_error`; the timeout result distinguishes the requested timeout from the actual termination state via `timeout_requested`, `execution_terminated`, `termination_status`, and `termination_failure`, so callers can detect when the Script Editor refused or failed to terminate the task. A blocked script or macro run remains addressable by its `run_id` through the matching status tool, `fiji_script_run_status` or `fiji_macro_run_status`, which are read-only and direct callers to `fiji_ui_dialog_respond` for the intentional UI action.

`fiji_macro_recorder_state` is a read-only snapshot of the current recorder,
including whether it is open, whether it is recording, its script mode, and the
current buffer text.

`fiji_ui_windows_read` and `fiji_ui_controls_read` inspect visible Java-owned
AWT and Swing UI state on the event-dispatch thread. Control paths are snapshot
selectors and should be re-read before any future state-changing UI action.
`fiji_ui_screenshot` captures the current screen pixels for a visible window or
showing component as an MCP image content block. Another window may overlap or
occlude the target, and native UI is not guaranteed to be captured semantically
or visually. `activate_and_restore` requests activation and focus restoration,
but both are best effort and do not guarantee an unobstructed capture or
successful restoration; inspect the returned metadata for the actual result.

`fiji_command_run`, `fiji_script_run`, and `fiji_macro_run` use `ExecutionEnvironmentSnapshotService` for lightweight operation-impact reporting. The report contains `before` and `after` metadata for open images, the active image, the Results table, and visible dialogs, plus `changes` arrays/flags, ImageJ/SciJava log deltas, and run-scoped ConsoleService stdout/stderr. Audited command and script captures also include bounded SHA-256 fingerprints of ImgLib2 native storage or pixel values; live snapshots defer pixel hashing and report `pixel_changes: "deferred"`. Large samples report `pixel_changes: "inconclusive"` when their sampled prefix matches, and lazy cell-backed images are skipped. No image pixels are copied into the report. Script execution also includes captured stderr in its `errors` field.

`fiji_results_read` returns structured Results Table metadata and numeric
rows, including `present`, `title`, `row_count`, `column_count`, `columns`, and
`rows`. `fiji_rois_read` returns the current manager availability and
ROI summaries including index, name, selection state, and type. Both tools are
read-only. ImageJ 1.x access for these tools and image compatibility helpers is
centralized in the high-priority `ImageJ1HelperService`.

`fiji_system_read` returns ImageJ 1.x and application versions, the Java
version, operating system details, JVM memory values in bytes, and active update
sites with their names and URLs. Its
`available_jvm_memory_bytes` value is the estimated remaining JVM heap based on
the configured maximum heap, not a measurement of physical system RAM.
`fiji_system_list_update_sites` lists all available update sites with their active
status, names, and URLs.

### MCP (Model Context Protocol) Bridge

`DefaultMCPService` runs an **embedded Jetty HTTP server** (default port 9090) that exposes `AiToolPlugin` tools as a MCP server using `io.modelcontextprotocol.sdk` (`1.1.2`). A LangChain4j `McpClient` then connects back to this server over `StreamableHttpMcpTransport`, and the resulting `McpToolProvider` is injected into `AiServices`.

This self-loopback MCP pattern allows the same tools to be accessed by external MCP-compatible clients (e.g., Claude Desktop) as well as the internal LangChain4j assistant. The bridge preserves text and base64-backed image tool results as MCP content blocks; `fiji_image_view` returns plain PNG image content, while `fiji_image_view_annotated` returns PNG image content plus structured render metadata for visible ROI and overlay state.

The MCP server instructions retain only MCP and live-session behavior and
direct clients to read the shared `onboarding` guidance document for
unfamiliar Fiji workflows. The integrated chat system message uses the same
onboarding entry point while retaining chat role and context framing.

---

## Context Injection

`ContextItem` is a JSON-serializable interface for attaching domain-specific context to chat messages. Implementations:

- **`ImageMetaContextItem`** — active image metadata (dimensions, type, calibration)
- **`ScriptContextItem`** — script editor content with line range selection

`ContextItem` supports merging (multiple items of the same type collapse into one) before being serialized into the user message payload.
Context items may also provide tooltip text for the chat attachment menu.

`ImageRenderingService` is the separate transient image payload layer. It copies the
currently rendered `DatasetView` plane, preserving the active display LUT,
channel ranges, color mode, and non-XY position, then bounds and encodes it as a
PNG-backed LangChain4j `ImageContent`. `RenderedImageResult` keeps that payload
separate from `ImageRenderMetadata`; optional ROI drawing uses the active legacy
ImageJ ROI when the legacy bridge is available.

---

## Conversation Management

`Conversation` holds a list of `Message` pairs — a **display string** (rendered in the UI) and a **`ChatMessage`** (stored in LangChain4j memory). This dual representation allows UI formatting to diverge from what the model sees.

`ConversationService` persists conversations to disk as JSON (`SerializedConversation`/`SerializedMessage`) and manages their lifecycle (create, load, delete).

---

## Extension Points

| Extension Point | Mechanism |
|---|---|
| New LLM provider | Implement `LLMProvider`, add `@Plugin(type = LLMProvider.class)` |
| New tool | Implement `AiToolPlugin`, add `@Plugin(type = AiToolPlugin.class)` |
| New context type | Implement `ContextItem` |
| New conversation serialization format | Implement `SerializedConversation` |

All extension discovery is handled by SciJava's classpath-scanning plugin registry — no manual registration required.

---

## UI and Entry Points

Swing-based UI integrated into Fiji as SciJava `Command` plugins:
- **`Fiji_Chat`** — main chat window
- **`Manage_Keys`** — API key management dialog
- **`Manage_MCP`** — MCP server status and configuration

Markdown responses are rendered in the chat UI via **flexmark** (`0.64.8`).

---

## Key Dependencies Summary

| Library | Version | Role |
|---|---|---|
| `langchain4j` | 1.13.1 | LLM abstraction, tool use, memory, streaming |
| `io.modelcontextprotocol.sdk:mcp` | 1.1.2 | MCP server implementation |
| `langchain4j-mcp` | 1.13.1-beta23 | MCP client + `McpToolProvider` |
| `scijava-common` | (pom-scijava) | Plugin system, services, DI |
| `imagej-legacy` | (pom-scijava) | ImageJ1 macro/command interop |
| `jsoup` | 1.21.2 | HTML parsing |
| `jackson` (2 + 3) | 2.19.2 / 3.0.3 | JSON serialization |
| `jetty-server` | 11.0.20 | Embedded MCP HTTP server |
| `flexmark` | 0.64.8 | Markdown rendering |
