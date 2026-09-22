/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.llm.guidance.application;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;

/** Guidance for extending and contributing to Fiji-LLM. */
@Plugin(type = AgentGuide.class)
public class ExtensionContributionGuide extends AbstractAgentGuide {

	private static final String CONTENT = """
			# Extending and Contributing to Fiji-LLM

			Fiji-LLM is itself a SciJava extension for Fiji. Use the official contribution
			and plugin-development documentation for general project, packaging, and plugin
			practices:

			- Fiji contribution guide: https://imagej.net/contribute/fiji
			- ImageJ plugin development: https://imagej.net/develop/plugins

			This guide describes how those extension mechanisms become an extensible LLM
			foundation in this project. SciJava discovers annotated plugins from the
			classpath, so a new contribution should use the existing extension point rather
			than create a parallel registry or client-specific integration.

			## Choose an extension point

			| Goal | Extension point | What it provides |
			|---|---|---|
			| Let an LLM inspect or change Fiji state | `AiToolPlugin` | LangChain4j `@Tool` methods discovered by the shared AI tool service and exposed to integrated chat and MCP |
			| Add domain state to model context | `ContextItem` and `ContextItemSupplier` | Structured context items that can be supplied to the chat system from the current Fiji state |
			| Teach an agent a stable workflow or concept | `AgentGuide` | Curated, searchable guidance read through the `fiji_guidance_*` tools |
			| Add a model backend | `LLMProvider` | A discoverable provider that creates the chat models used by the assistant |
			| Add a human chat integration | `ChatbotService` | The project service boundary for integrated chat UI behavior |

			## What to add

			- **A tool:** Implement `AiToolPlugin`, annotate it with
			  `@Plugin(type = AiToolPlugin.class)`, and expose narrowly scoped methods with
			  LangChain4j `@Tool` descriptions. Extend `AbstractAiToolPlugin` when its shared
			  reflective tool discovery and naming behavior applies. Prefer structured results,
			  explicit inputs, and verification-friendly operations. Use `ToolScope` when a
			  tool should be available only in a particular integrated-chat context.
			- **Context:** Implement a `ContextItem` for the model-facing representation and
			  a `ContextItemSupplier` plugin when the item should be offered from live Fiji
			  state or the chat UI. Keep context focused; do not inject large or sensitive
			  application state by default.
			- **Guidance:** Extend `AbstractAgentGuide`, annotate the class as an
			  `AgentGuide` plugin, and give it a stable Guide ID, useful topic terms, and
			  related Guide IDs. Guidance should route an agent to the right tools or other
			  guides instead of copying tool descriptions or general API documentation.
			- **A provider:** Implement `LLMProvider` and register it with
			  `@Plugin(type = LLMProvider.class)`. API-key-based providers normally extend
			  `AbstractLLMProvider`; Ollama providers normally extend
			  `AbstractOllamaProvider` or `AbstractSingletonOllamaProvider`. Keep model
			  connectivity separate from tools and application behavior.
			- **Chat UI behavior:** Use `ChatbotService` and the existing SciJava service and
			  command mechanisms. Do not create a second chat registry when the shared
			  service, tool, context, or guidance extension point already fits.

			## How an LLM can route a developer request

			| Developer request | First suggestion |
			|---|---|
			| “Let the assistant perform or inspect a Fiji operation” | Add a focused `AiToolPlugin`; the integrated chat and MCP can discover the same tool |
			| “Give the model the current state of my domain object” | Add a `ContextItem` and, when appropriate, a `ContextItemSupplier` |
			| “Help agents understand this workflow” | Add or update an `AgentGuide` with searchable topics and related Guide IDs |
			| “Support another model service” | Add an `LLMProvider` using the matching provider base class |
			| “Change the human chat experience” | Inspect `ChatbotService` and existing commands/services before adding UI code |

			A new tool is the usual route for agentic behavior. Because the integrated chat
			and MCP server use the shared discovered tool framework, one well-scoped tool can
			serve both interfaces. The exact exposed tool set remains dynamic: clients should
			discover the running Fiji instance rather than assume that every installation has
			the same extensions.

			## Contribution checklist

			Start from a nearby implementation, keep the extension narrow, and add focused
			tests for deterministic behavior. Use structured tool results and clear names and
			descriptions so both people and LLMs can select the capability. Verify state before
			mutating it and report enough context to make failures reproducible. When changing
			an extension contract or agent-facing tool, update the relevant guidance and
			documentation as well as the implementation.
			""".trim();

	public ExtensionContributionGuide() {
		super("extension-contribution", "Fiji Extension and Contribution", List.of(
			"application", "development", "extensions", "contribution", "llm"),
			Authority.PROJECT_AUTHORED, List.of("onboarding", "mcp-server", "integrated-chat",
			"services-and-context"));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
