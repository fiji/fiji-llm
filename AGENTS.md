# Agent instructions
This is an experimental Fiji integration for LLM-assisted workflows. See
`README.md` and `doc/TECHNICAL_SUMMARY.md` for the project goals and current
architecture.

## Coding Style
- Do not manually create or edit license headers.
- After creating a new Java source file, run `mvn license:update-file-header`.
- Add Javadoc with appropriate @ tags to new or changed public and protected API.
- Document private methods when their behavior is non-obvious.
- Always use import statements instead of fully qualified names when referencing classes from other packages (exception: strings for reflection)
- Never add trailing whitespace to any line
- Empty lines must not have spaces or tabs

## Architecture
- Keep model connectivity separate from agent tools and application behavior.
- Before implementing new infrastructure, check whether SciJava, ImageJ, Fiji,
  langchain4j, MCP, or another well-maintained library already provides it.
- Prefer small, focused methods with descriptive names.
- Consolidate duplicated logic when it represents a shared concept

## Compatibility
- Do not preserve backward compatibility unless the task explicitly requests it.
- When changing an API or agent-facing tool contract, update affected Javadoc, guides, skills, and `.md` docs in the same change.

## Validation
- This code is meant to run in a live Fiji app and interact with arbitrary LLMs.
- Keep isolated unit tests fast and deterministic
- Use focused integration tests for Fiji behavior

## Writing agent tools
- See tool guidance in `README.md`
- Do not expose destructive or broadly state-changing operations without clear
  safeguards.
- Do not embed model-specific assumptions in tool implementations.
- `AiToolPlugin` implementations are the single source of tools, but can have distinct views through MCP or integrated chat.