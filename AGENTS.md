# Agent instructions
This is an experimental Fiji integration for LLM-assisted workflows. See
`README.md` and `doc/TECHNICAL_SUMMARY.md` for the project goals and current
architecture.

## Coding Style
- After creating a new Java source file, run `mvn license:update-file-header`.
- Do not manually create or edit license headers.
- Add Javadoc to every public and protected class, interface, enum, constructor, method, and field.
- Document package-private or private code when its behavior or contract is non-obvious.
- Use `@param`, `@return`, and `@throws` where applicable.
- Always use import statements when referencing classes from other packages.
- Never use fully qualified names like `java.util.List` in code (exception: strings for reflection)
- Never add trailing whitespace to any line
- Empty lines must be completely blank (no spaces or tabs)

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
- Prefer simplifying or correcting Java APIs over adding compatibility layers.
- Prefer small, focused methods with descriptive names.
- Consolidate duplicated logic when it represents a shared concept, but do not
  introduce abstractions solely to eliminate superficial repetition.
- For non-trivial feature work, do a focused verification pass before settling on
  an implementation path: confirm the relevant upstream or foundational library APIs,
  check whether the project already has a matching pattern. Avoid broad speculative
  exploration. If multiple plausible options are available, propose them with a pros
  and cons list before making a choice.

## Compatibility
- Backward compatibility is not required unless the task explicitly requests it.
- When changing an API or agent-facing tool contract, update Javadoc, guides, and documentation in the same change.
- Inspect the affected files under:
  - `src/main/java/sc/fiji/llm/guidance/`
  - `doc/INTEGRATION_TESTS.md`
  - `doc/TECHNICAL_SUMMARY.md`
  - `README.md`
  - `.github/agents/`
  - `.github/skills/`
- Update only the files whose documented behavior or contract is affected

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
- Prefer structured results over prose when results will be consumed by an LLM.
- Do not expose destructive or broadly state-changing operations without clear
  safeguards.
- Avoid embedding model-specific assumptions in tool implementations.
- `AiToolPlugin`s are the single source of tools, but can have distinct views through MCP or integrated chat.