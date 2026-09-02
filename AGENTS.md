# Agent instructions
This is an experimental Fiji integration for LLM-assisted workflows. See
`README.md` and `doc/TECHNICAL_SUMMARY.md` for the project goals and current
architecture.

## Architecture
- Use SciJava plugins, services, and dependency injection where appropriate.
- Prefer the existing SciJava extension mechanisms over creating parallel
  registries or integration frameworks. In particular:
  - Extend `AbstractLLMProvider` for API-key-based providers.
  - Extend `AbstractOllamaProvider` or `AbstractSingletonOllamaProvider` for
    Ollama providers, as appropriate.
  - Extend `AbstractAiToolPlugin` when defining LangChain4j `@Tool` methods so
    that the reflective tool discovery is reused.
  - Implement `ContextItemSupplier` directly for UI context suppliers.
  - Use `ChatbotService` for chat UI integrations rather than creating a
    parallel service mechanism.
- Follow existing implementations as local architectural examples.
- If the appropriate SciJava extension mechanism is unclear, call out that
  uncertainty rather than inventing a project-specific substitute.
- Keep model connectivity separate from agent tools and application behavior.

## Implementation choices
- Before implementing new infrastructure, check whether SciJava, ImageJ, Fiji,
  langchain4j, MCP, or another well-maintained library already provides it.
- Prefer established APIs and extension points over de novo implementations.
- Avoid adding a dependency when a small, clear local implementation would be
  easier to maintain.
- Prefer small, focused methods with descriptive names.
- Consolidate duplicated logic when it represents a shared concept, but do not
  introduce abstractions solely to eliminate superficial repetition.
- For non-trivial feature work, do a focused verification pass before settling on
  an implementation path: confirm the relevant upstream or foundational library APIs,
  check whether the project already has a matching pattern, and compare a couple of
  reasonable approaches. Avoid broad speculative exploration, but do not assume a
  first idea is correct just because it seems plausible.
- Prefer the project’s existing architecture over “novel” framework usage. A small,
  well-placed local implementation is better than introducing a new abstraction or
  framework pattern unless the existing design clearly does not fit.

## Compatibility
- The project is experimental. Backward compatibility is not required unless
  the task explicitly requests it.
- Prefer simplifying or correcting Java APIs over adding compatibility layers.
- Treat persisted conversations, preferences, MCP interoperability, and
  update-site users as externally visible behavior. Call out changes affecting
  them even when Java API compatibility is not required.

## Validation
- Much of the project requires a running Fiji installation and may depend on
  installed plugins, UI interaction, MCP clients, and nondeterministic LLM
  behavior.
- Write and run focused automated tests where behavior can be isolated.
- Keep deterministic application logic separate from model-dependent behavior
  where practical.
- Do not assert exact model wording or assume that every model selects the same
  tools.
- Concisely report what was validated, what requires manual testing in Fiji, and what was
  not tested.

## Writing agent tools
- Keep tools narrowly scoped, with clear names, descriptions, inputs, and
  outputs.
- All discovered `AiToolPlugin` tools are intentionally exposed through the
  MCP, while `ToolScope` should be used in integrated chats to allow filtering.
- Prefer structured results over prose when results will be consumed by an LLM.
- Do not expose destructive or broadly state-changing operations without clear
  safeguards.
- Avoid embedding model-specific assumptions in tool implementations.
