# Proposed Foundation Issues

These issues describe reusable capabilities that would let other projects build
agentic applications on top of fiji-llm. They are intentionally phrased as
GitHub issues rather than as an implementation plan.

LangChain4j already provides much of the model, tool-loop, memory, and MCP
machinery. The goal is to expose stable Fiji-aware contracts around those
capabilities, not to reimplement them in fiji-llm.

## Recommended Order

1. Define model capabilities and execution events.
2. Make tools cancellable, observable, and optionally confirmable.
3. Expose a reusable agent runner.
4. Add workflow composition and typed handoffs.
5. Persist runs and artifacts.
6. Add context budgeting and pluggable retrieval.
7. Add observability, external MCP clients, and multimodal content.

The issues below can be split into separate GitHub issues. The numbers are
ordering hints, not required issue numbers.

## Issue 1: Describe Model Capabilities Explicitly

**Title:** Add capability metadata to LLM providers and models

**Problem:** A consumer can select a provider and model, but cannot reliably
discover whether the model supports tool calling, streaming, structured output,
vision, or the relevant context size before creating an assistant.

**Goal:** Add a provider-neutral capability descriptor, populated by
`LLMProvider` implementations where possible and exposed through
`ProviderService`.

**Acceptance criteria:**

- Consumers can query capabilities before starting a run.
- Capabilities cover at least tool calling, streaming, structured output,
  multimodal input, context limits, and model/provider identity.
- Unknown capabilities are represented as unknown rather than guessed.
- Existing providers continue to work with sensible defaults.
- The descriptor is independent of the UI and usable by headless clients.

**Non-goal:** Implement model routing or a pricing database in this issue.

**Depends on:** None.

## Issue 2: Define a Common Run and Event API

**Title:** Expose structured assistant run events

**Problem:** `FijiAssistant` exposes a final response or token stream, but
downstream UIs and services also need to observe tool calls, tool results,
errors, retries, handoffs, and completion state.

**Goal:** Define a transport-neutral run identifier, event types, timestamps,
and correlation fields. Adapt LangChain4j callbacks or listeners into this API
where possible.

**Acceptance criteria:**

- A consumer can subscribe to token, tool, error, retry, progress, and
  completion events.
- Events identify the run, conversation, tool, and agent when applicable.
- Events are usable by Swing, web, MCP, and headless consumers.
- Event delivery does not require the Fiji chat UI.
- Cancellation and failure are represented explicitly.

**Non-goal:** Choose a network protocol or prescribe a frontend.

**Depends on:** Issue 1 is recommended but not strictly required.

## Issue 3: Expand the Tool Execution Contract

**Title:** Add progress, cancellation, confirmation, and limits to AI tools

**Problem:** `AiToolPlugin` provides discovery and execution, but long-running
or potentially destructive tools need lifecycle controls that a simple tool
return value does not provide.

**Goal:** Extend the tool contract, or add a companion interface, for progress
reporting, cancellation, timeouts, resource limits, dry runs, and optional human
approval. Preserve the current annotation-based path for simple tools.

**Acceptance criteria:**

- Tools can declare whether they are read-only, mutating, long-running, or
  approval-required.
- A run can cancel a tool and enforce a timeout.
- Tools can report progress through the common event API.
- Approval decisions are explicit and auditable.
- Existing `AiToolPlugin` implementations remain usable without changes.

**Non-goal:** Build a general operating-system sandbox.

**Depends on:** Issue 2.

## Issue 4: Provide a Reusable Agent Runner

**Title:** Add a headless agent execution service with bounded tool loops

**Problem:** Creating an `AiServices` assistant is not the same as providing a
reusable agent runtime. Consumers currently need to implement their own loop
control, retry policy, cancellation, and progress handling around LangChain4j.

**Goal:** Provide a headless service that runs an assistant or agent with
configurable step limits, timeout, retry policy, cancellation, and event
publication. Delegate model calls and tool execution to LangChain4j wherever
it already supports the behavior.

**Acceptance criteria:**

- A consumer can start, cancel, and await a run without using the Fiji UI.
- Step, time, and tool-use limits are configurable per run.
- Model, tool, argument, and execution failures have separate failure types.
- Retry behavior is configurable and does not retry unsafe mutations by
  default.
- The runner can use the existing `AssistantService` and `AiToolService`.

**Non-goal:** Ship predefined Coder, Debugger, Analyst, or Librarian agents.

**Depends on:** Issues 1-3.

## Issue 5: Support Composable Workflows and Typed Handoffs

**Title:** Add a workflow API for sequential and delegated agent steps

**Problem:** CopilotJ and Agentic-J need to compose specialized steps, but the
current API exposes an assistant rather than a workflow with typed intermediate
results.

**Goal:** Define small workflow primitives for sequential steps, branching,
delegation, and handoff results. Allow consumers to define their own agent
roles and schemas without adding those roles to fiji-llm.

**Acceptance criteria:**

- A workflow can pass typed state and artifacts between steps.
- A step can succeed, fail, request input, or delegate to another step.
- Handoff results identify the producing step and contain a machine-readable
  status and failure details.
- Workflows use the common run and event API.
- A simple one-agent workflow remains easy to define.

**Non-goal:** Add a graphical workflow editor or domain-specific bioimage
workflow definitions.

**Depends on:** Issue 4.

## Issue 6: Persist Runs, Checkpoints, and Artifacts

**Title:** Add resumable execution state and artifact references

**Problem:** Conversation persistence does not preserve an in-progress agent
run, intermediate workflow state, generated scripts, result tables, or figures.

**Goal:** Define storage interfaces for checkpoints, run metadata, workflow
state, and artifacts. Provide a small local filesystem implementation suitable
for Fiji, while allowing downstream applications to provide other stores.

**Acceptance criteria:**

- A failed or interrupted workflow can resume from a checkpoint when supported
  by the workflow.
- Runs record status, timestamps, model identity, tool activity, and errors.
- Artifacts have stable identifiers, type, location, and provenance metadata.
- Conversations and execution runs remain separate concepts.
- Storage failures are reported without silently losing run state.

**Non-goal:** Decide whether a consuming application uses JSON, SQLite, or a
cloud database internally.

**Depends on:** Issue 5.

## Issue 7: Add Context Budgeting and Memory Adapters

**Title:** Provide pluggable context compaction and ChatMemory persistence

**Problem:** LangChain4j supplies `ChatMemory`, but consumers still need a
consistent way to persist it, summarize or compact old context, account for
tool results, and attach Fiji context within a token budget.

**Goal:** Add adapters and policies around existing LangChain4j memory
interfaces. Make context selection, compaction, and persistence configurable
per assistant or run.

**Acceptance criteria:**

- A consumer can select a memory implementation and persistence strategy.
- Context assembly reports or enforces a configured budget.
- Compaction preserves system instructions, current task state, and relevant
  tool results according to an explicit policy.
- Fiji `ContextItem` and application context can be included without relying
  on chat-window code.
- Memory failures are visible to the caller.

**Non-goal:** Implement semantic retrieval of all past conversations here.

**Depends on:** Issues 2 and 6.

## Issue 8: Define a Retrieval Provider SPI

**Title:** Add a provider-neutral retrieval interface for Fiji knowledge

**Problem:** RAG is useful for plugin documentation, macros, and learned
workflow patterns, but a particular vector database or embedding service should
not become a core fiji-llm requirement.

**Goal:** Define interfaces for document ingestion, retrieval, result metadata,
and optional deletion/update. Allow consumers to provide keyword, vector, or
hybrid implementations and inject retrieved context into a run.

**Acceptance criteria:**

- Retrieval requests and results have stable Java types.
- Results include source, score or ranking information, and enough metadata for
  provenance.
- Retrieval can be used as a normal tool or as a context-building stage.
- Implementations can be local and do not require a cloud service.
- The core artifact does not depend on Qdrant, FAISS, or a specific embedding
  provider.

**Non-goal:** Ship a comprehensive Fiji plugin knowledge base.

**Depends on:** Issue 7.

## Issue 9: Add Usage and Execution Observability Hooks

**Title:** Expose token, latency, cost, and tool metrics for assistant runs

**Problem:** Downstream applications cannot consistently measure model usage,
latency, tool duration, or failures across providers.

**Goal:** Add optional metrics callbacks or events with provider/model identity,
token usage when available, durations, retry counts, and tool outcomes.

**Acceptance criteria:**

- Metrics correlate to the common run identifier.
- Missing provider data is represented as unavailable rather than fabricated.
- Consumers can collect metrics without enabling a hosted observability
  service.
- Instrumentation is optional and has low overhead when unused.

**Non-goal:** Make LangSmith, Langfuse, or another hosted service a dependency.

**Depends on:** Issue 2.

## Issue 10: Support External MCP Servers as Tool Sources

**Title:** Add an MCP client adapter for external tool servers

**Problem:** fiji-llm can expose Fiji tools through MCP, but a reusable agent
runtime also needs to consume tools hosted by other MCP servers.

**Goal:** Provide a configurable MCP client adapter that discovers external
tools, maps their schemas into the existing tool contract, and manages their
lifecycle and errors.

**Acceptance criteria:**

- Consumers can configure one or more external MCP servers.
- Discovered tools can participate in normal assistant and workflow runs.
- Tool names, descriptions, schemas, and errors are preserved clearly.
- Server connection failures and shutdown are observable.
- Credentials and server configuration are supplied by the consuming
  application rather than hard-coded in the core library.

**Non-goal:** Replace the existing Fiji MCP server.

**Depends on:** Issues 3 and 4.

## Issue 11: Represent Multimodal Inputs and Generated Artifacts

**Title:** Add first-class media and artifact content to assistant requests

**Problem:** Current context is primarily serialized text or JSON. Consumers
that use vision-capable models need to pass image data or references without
encoding everything into a prompt string.

**Goal:** Add provider-neutral content blocks or attachments for images and
other supported media, plus references to scripts, tables, plots, and result
images. Use model capability metadata to reject unsupported requests early.

**Acceptance criteria:**

- Requests can carry text plus media or artifact references.
- Providers can translate supported content to their native LangChain4j
  representation.
- Unsupported media produces a clear capability error.
- Artifact references retain provenance and do not require copying large data
  into every prompt.
- Existing text-only assistants continue to work.

**Non-goal:** Implement image interpretation or a vision agent in the core.

**Depends on:** Issues 1 and 6.

## What Should Stay Outside the Core

The following are valuable features for consuming applications, but should not
be required features of the foundational library:

- Predefined Coder, Debugger, Analyst, QA, or Librarian roles
- A specific RAG index of Fiji plugins
- Python scientific libraries, plotting, or GPU execution
- A web, PySide, or Swing application beyond reusable event and service APIs
- A graphical workflow builder
- Application-specific learning policies

Those projects should be able to build these features using the contracts above
and Fiji's existing SciJava extension points.