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
import sc.fiji.llm.guidance.OnboardingGuide;
import sc.fiji.llm.guidance.data.ServicesAndContextGuide;

/** Guidance for extending and contributing to Fiji-LLM. */
@Plugin(type = AgentGuide.class)
public class ExtensionContributionGuide extends AbstractAgentGuide {

	public static final String ID = "extension-contribution";

	private static final String CONTENT = """
			# Extending and Contributing to Fiji-LLM

			Fiji-LLM is a SciJava extension, not a separate application layered beside Fiji.
			Its tools, context, guidance, model providers, and chat integrations are discovered
			through shared extension points and can be reused by both integrated chat and MCP
			clients.

			The important contribution decision is therefore architectural: add the behavior to
			the existing extension point that owns it. Avoid creating a parallel registry,
			client-specific implementation, or new workflow format. Reusing the shared
			infrastructure keeps discovery, lifecycle, application state, safety boundaries, and
			documentation consistent across Fiji interfaces. It also means one focused extension
			can serve more than one client instead of making the same capability separately for
			chat, MCP, or a particular model.

			## Find the right extension point

			| Need | Extension point |
			|---|---|
			| Let an agent inspect or change Fiji state | `AiToolPlugin` |
			| Supply current domain state as model context | `ContextItem` and `ContextItemSupplier` |
			| Explain a stable workflow or concept | `AgentGuide` |
			| Connect another model service | `LLMProvider` |
			| Change the human chat integration | `ChatbotService` |

			When the need crosses boundaries, keep the responsibilities separate: model
			connectivity should not contain Fiji tools, tools should not become a second context
			system, and guidance should route agents rather than duplicate tool descriptions or
			general API documentation.

			## Read next

			For project conventions, nearby implementation links, tool-design guidance, testing,
			and documentation requirements, read the [Developers: Adding Functionality section
			of the README](https://github.com/fiji/fiji-llm#developers-adding-functionality).
			For the broader architecture, read the [technical summary](https://github.com/fiji/fiji-llm/blob/main/doc/TECHNICAL_SUMMARY.md).

			For general Fiji contribution and plugin-development practices, see the [Fiji
			contribution guide](https://imagej.net/contribute/fiji) and [ImageJ plugin
			development guide](https://imagej.net/develop/plugins).
			""".strip();

	public ExtensionContributionGuide() {
		super(ID, "Fiji Extension and Contribution", AgentGuide.topics(
			Topic.APPLICATION, Topic.DEVELOPMENT, Topic.EXTENSIONS, Topic.CONTRIBUTION, Topic.LLM),
			Authority.PROJECT_AUTHORED, List.of(OnboardingGuide.ID, MCPServerGuide.ID,
				IntegratedChatGuide.ID, ServicesAndContextGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
