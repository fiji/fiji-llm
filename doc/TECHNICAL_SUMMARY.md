# fiji-llm — Technical Summary

## Overview

A Fiji/ImageJ plugin that integrates large language models into the bioimage analysis platform. Exposes LLM chat and agentic tool-use capabilities directly within the ImageJ desktop application.

- **Language**: Java 17+
- **Build**: Maven, parent POM `org.scijava:pom-scijava:43.0.0`
- **License**: BSD-2-Clause

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

Cloud providers extend `AbstractLLMProvider`, which retrieves API keys from `APIKeyService` (stored via SciJava `PrefService`).

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

Tools are scoped via a `ToolScope` string (e.g. `MACRO`) to allow context-sensitive filtering.

Built-in tools include:
- **`ImageJMacroTool`** — macro recorder integration, macro function discovery
- **`CommandInteractionTool`** — ImageJ command execution
- **`ScriptEditorTool`** — script editor interaction
- **`ImageTool`** — active image metadata access

### MCP (Model Context Protocol) Bridge

`DefaultMCPService` runs an **embedded Jetty HTTP server** (default port 9090) that exposes `AiToolPlugin` tools as a MCP server using `io.modelcontextprotocol.sdk` (`1.1.2`). A LangChain4j `McpClient` then connects back to this server over `StreamableHttpMcpTransport`, and the resulting `McpToolProvider` is injected into `AiServices`.

This self-loopback MCP pattern allows the same tools to be accessed by external MCP-compatible clients (e.g., Claude Desktop) as well as the internal LangChain4j assistant.

---

## Context Injection

`ContextItem` is a JSON-serializable interface for attaching domain-specific context to chat messages. Implementations:

- **`ImageMetaContextItem`** — active image metadata (dimensions, type, calibration)
- **`ScriptContextItem`** — script editor content with line range selection

`ContextItem` supports merging (multiple items of the same type collapse into one) before being serialized into the user message payload.

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
| `jackson` (2 + 3) | 2.19.2 / 3.0.3 | JSON serialization |
| `jetty-server` | 11.0.20 | Embedded MCP HTTP server |
| `flexmark` | 0.64.8 | Markdown rendering |
