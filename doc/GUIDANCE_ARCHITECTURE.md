# Shared Guidance Architecture

## Status

This document defines the planned boundary and architecture for Fiji-specific
knowledge made available to the integrated Fiji chatbot and external MCP
clients. It is a design contract for the guidance system, not a complete
implementation specification.

## Purpose

Fiji-LLM needs to provide detailed, task-appropriate instructions without
placing the entire Fiji documentation corpus into every model request. The
system should provide a small shared baseline, retrieve detailed guidance only
when relevant, and inspect the running Fiji application whenever a fact depends
on the current installation or session.

The core rule is:

> Shared guidance explains how Fiji works. Live tools report what is true in
> the current Fiji session. Client files explain how a particular AI client
> should interact with Fiji.

The integrated chatbot and MCP server should use the same guidance model,
content, and retrieval behavior. They may present that information differently
because their protocol and client capabilities differ.

## Goals

- Give models accurate Fiji-specific guidance for script, macro, and analysis
  workflows.
- Make the same domain knowledge available through integrated chat and MCP.
- Keep normal requests small by retrieving detailed guidance on demand.
- Preserve stable identities, controlled topics, authority, and related-document
  links.
- Keep static documentation separate from live Fiji state.
- Prefer curated, maintainable content over broad unverified ingestion.
- Support deterministic retrieval before introducing embeddings or a vector
  database.

## Non-goals

The initial guidance system will not:

- Replace Fiji tools that inspect current application state.
- Mirror the complete ImageJ wiki or image.sc forum.
- Provide general-purpose Python, Java, or shell documentation.
- Automatically inject every guidance document into every request.
- Treat generated or community material as authoritative API documentation.
- Make MCP resources or prompts a prerequisite for guidance access.
- Add a vector database or external embedding service before retrieval evidence
  justifies the additional complexity.

## Knowledge Boundary

Every candidate piece of information should be classified into one of four
layers.

### Shared Fiji guidance

This layer contains reusable, Fiji-specific knowledge that affects how an
agent should reason or act. It includes information that is not adequately
represented by tool schemas and cannot be determined reliably from the current
application state alone.

Examples include:

- Fiji Script Editor language behavior and hidden conventions.
- Jython limitations and the distinction between Jython and Appose Python.
- Appose Python lifecycle, mode, and restart concepts.
- Script parameter annotations, implicit imports, and service injection.
- Script and macro authoring, execution, debugging, and verification patterns.
- ImageJ 1.x and ImageJ2 concepts that affect workflow choices.
- How to interpret Fiji execution and environment reports.

### Live Fiji state

This layer is provided by tools or application context, not by static guidance.

Examples include:

- Available scripting engines in the current Fiji installation.
- Current Fiji mode, version, and restart requirements.
- Open images, active windows, scripts, dialogs, logs, tables, and ROIs.
- Installed commands, plugins, and update sites.
- The current contents and execution result of a script.

Guidance may explain what a live fact means, but a live tool must determine
whether that fact is currently true.

### Client-specific behavior

This layer belongs in the integrated UI, MCP client configuration, VS Code
agents, and VS Code skills.

Examples include:

- VS Code frontmatter and tool declarations.
- Slash-command or menu behavior.
- Client-specific planning and reporting conventions.
- Instructions for selecting or displaying MCP tools.
- Capabilities and limitations of a particular host application.

Client files may reference shared guidance topics, but they are not the
canonical source for Fiji domain knowledge.

### External references

External documentation should initially be represented by curated links and
bounded summaries. Each reference must identify its source and authority.

Preferred sources are official ImageJ, Fiji, and SciJava documentation. Image.sc
and other community sources may provide valuable troubleshooting information,
but they must be labelled as community guidance and should not silently override
normative project or API documentation.

## Inclusion Test

A candidate document belongs in the shared guidance corpus when most of these
conditions are true:

1. It is specifically about Fiji, ImageJ, SciJava, or Fiji-LLM.
2. It can cause an agent to choose the wrong workflow if omitted.
3. It is not already adequately expressed by a tool description.
4. It applies across integrated chat and MCP, or can be tagged for both.
5. It can be written as a focused, bounded document or section.
6. Its source, authority, version, and applicability can be identified.
7. It is maintainable for a meaningful period of time.

Information that fails these tests should generally be improved in the tool
schema, exposed through a live capability tool, kept as client-specific
instructions, or linked as external documentation instead.

## Exclusion Test

The corpus should exclude:

- General programming language documentation.
- Current application state or user-specific state.
- Information already obvious from a tool schema.
- Unverified model-generated advice.
- Large undifferentiated wiki or forum dumps.
- Client implementation details.
- User preferences and conversation-specific instructions.
- Untagged examples that may be mistaken for universally valid code.
- Full copies of external documents when a source link is sufficient.

## Guidance Document Contract

Each document should have a stable identity and explicit topic scope. The
initial logical model is intentionally small:

```text
id: python-runtime-selection
title: Choosing a Fiji Python Runtime
topics: [scripts, python, runtime]
authority: project-authored
related_documents: []
content: ...
```

The implementation uses singleton SciJava plugins and a singleton service:
`AgentGuide` represents one guide and directly returns its metadata and text,
while `AgentGuidanceService` discovers and indexes all `AgentGuide` instances.
`AgentGuideMetadata` represents catalog and search results, while the
`AgentGuide` instance owns the full guide text. The service's read operation
returns only bounded text. There is no separate JSON index or packaged-resource
loader. Built-in guides use the explicit `AgentGuide.Topic` vocabulary to avoid
typos in canonical topic declarations, while metadata remains string-based so
third-party guide plugins can add topic keywords.

A useful document or section should answer as many of these questions as
applicable:

1. What is the concept or workflow?
2. When does it apply?
3. What should the agent inspect first?
4. What action is recommended?
5. What common mistakes should be avoided?
6. Which live tool confirms uncertain facts?
7. Which related document supports the guidance?

## Retrieval Contract

The guidance service should provide three conceptual operations:

- List the available topic keywords and document summaries.
- Search guidance using one exact topic keyword.
- Read one bounded document or section by stable identifier.

The initial implementation uses packaged, curated resources and deterministic
topic matching. Topic matching is case-insensitive but otherwise exact. A
document with multiple topics matches one search for each topic it contains.
Results are ordered by stable identifier so behavior is testable and
understandable.

Search and read results should be structured. Topic search returns
`AgentGuideMetadata`; it does not return guide content. A search result
should include:

- Stable document or section identifier.
- Title and topics.
- Authority.
- Related documents or a follow-up identifier.

The read operation returns bounded text for the selected `AgentGuide` and
enforces a maximum content size. A caller should not be able to bypass
context limits by requesting an unbounded document.

## Delivery Paths

### Integrated chat

The integrated chatbot should receive:

1. A compact shared system baseline.
2. The same guidance retrieval tools available to the MCP route.
3. Live Fiji state only when supplied through existing context or tools.

The system baseline directs the model to read the `onboarding` document before
using other Fiji tools for an unfamiliar Fiji-specific workflow. Detailed
guidance should not be placed in the system message by default.

### MCP server

The MCP server should provide:

1. The same compact shared baseline through server instructions.
2. The same guidance retrieval tools and executors.
3. Existing Fiji tools for live state and actions.

Its server instructions retain MCP and live-session behavior, while directing
clients to the shared `onboarding` document for Fiji workflow guidance.

MCP resources and prompts are future presentation options. They may improve
discoverability for particular clients, but the guidance tools remain the
initial compatibility path and must not depend on those optional primitives.

### VS Code agents and skills

The `.github` agent and skill files should remain client-specific adapters. They
should contain the behavior needed by VS Code and concise workflow entry points,
while reusable Fiji facts move into the shared guidance corpus. Their metadata,
tool declarations, and client-specific reporting rules remain in `.github`.

## Static Guidance and Live Capability Discovery

Static guidance and capability discovery are complementary:

```text
Guidance: Appose Python may require a particular Fiji mode and restart.
Capability tool: This Fiji instance is or is not in that mode, and this engine
                 is or is not available now.
Script tool:     This is the active script and its current content.
Execution tool:  This run produced this output, error, dialog, and state change.
```

The live capability tool should be read-only and report only facts available

- Add a shared onboarding document and direct integrated chat and MCP clients
  to retrieve it before unfamiliar Fiji workflows.
- `project-authored`: Fiji-LLM guidance based on project behavior.
- `example`: illustrative code or workflow, not a universal guarantee.
- `community`: useful field experience, but potentially version-specific or
  unverified.

When sources disagree, the assistant should prefer current runtime inspection,
then authoritative documentation, then project-authored guidance, then
community material. Version and last-reviewed metadata are part of this
comparison.

## Context Budget Policy

The default request should contain only the compact baseline, relevant tool
schemas, and necessary live context. Detailed guidance is retrieved when:

- The request concerns Fiji-specific scripting or workflow behavior.
- The model is uncertain about a runtime or editor convention.
- A tool result indicates a known diagnostic or execution condition.
- The user explicitly requests documentation or an example.

Topic search should remain a lightweight catalog operation. Callers should
read only the selected documents, and the read limit should remain configurable
rather than relying on a fixed assumption about every model's context window.

## Roadmap

### Initial implementation

- Define the guidance document model and metadata.
- Inventory existing prompts, tool descriptions, skills, README material, and
  technical documentation.
- Add curated packaged guidance resources.
- Add deterministic search and bounded read operations.
- Expose `fiji_guidance_search` and `fiji_guidance_read` as read-only tools.
- Replace duplicated integrated-chat and MCP baseline instructions with a
  shared composition service.

### Next implementation

- Add live script capability discovery.
- Add focused guidance for Python runtimes, Script Editor conventions,
  parameters, execution, and diagnostics.
- Align `.github` skills and agent content with the shared corpus.
- Add retrieval evaluation fixtures and representative task tests.

### Future directions

- Plugin-contributed guidance when external Fiji distributions need to extend
  the corpus without modifying Fiji-LLM.
- Curated official external documentation and source links.
- Carefully labelled community troubleshooting references.
- MCP resources and user-controlled MCP prompts.
- Embeddings or a local vector index only if retrieval evaluation demonstrates
  that deterministic search is insufficient.

## Evaluation Criteria

The guidance system is successful when representative tasks can be completed
with relevant, bounded, and correctly scoped information:

- Choose between Jython and Appose Python.
- Write a script using Script Editor parameters and injected services.
- Diagnose a script execution failure.
- Create and verify an ImageJ macro.
- Search for and execute a Fiji command safely.
- Interpret an environment report without confusing stale context for current
  state.

Evaluation should measure relevance, provenance, applicability, context size,
and live-state separation. Tests should not require exact model wording.

## Related Documentation

- [Technical Summary](TECHNICAL_SUMMARY.md)
- [README](../README.md)
- [Fiji MCP agent](../.github/agents/fiji-mcp.agent.md)
- [Fiji script workflow](../.github/skills/fiji-script-workflow/SKILL.md)
- [Fiji macro workflow](../.github/skills/fiji-macro-workflow/SKILL.md)
