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
package sc.fiji.llm.guidance;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;
import sc.fiji.llm.guidance.application.EnvironmentInspectionGuide;
import sc.fiji.llm.guidance.application.ExtensionContributionGuide;
import sc.fiji.llm.guidance.application.IntegratedChatGuide;
import sc.fiji.llm.guidance.application.MCPServerGuide;
import sc.fiji.llm.guidance.application.SupportAndCommunityGuide;
import sc.fiji.llm.guidance.application.UIInteractionGuide;
import sc.fiji.llm.guidance.application.UpdateSitesGuide;
import sc.fiji.llm.guidance.workflows.RunningCommandsGuide;
import sc.fiji.llm.guidance.workflows.ScriptsAndMacrosGuide;

/** Onboarding guidance for agents operating in Fiji. */
@Plugin(type = AgentGuide.class)
public class OnboardingGuide extends AbstractAgentGuide {

	public static final String ID = "onboarding";

	private static final String CONTENT = """
		# Fiji Onboarding

		Fiji is an application for scientific image analysis.

		Built on the SciJava plugin framework, Fiji bundles ImageJ (1.x), ImageJ2, and a large collection of curated plugins commonly used in bioimage analysis.

		Fiji is an extensible, open source project commonly used for research and publication: reproducibilty and collaboration are core tenets.

		## Agentic Tools
		Tools are named by functional group, including:
		- `fiji_system_*`: report application and host-environment information.
		- `fiji_ui_*`: inspect and interact with visible UI components.
		- `fiji_command_*`: make functional changes to the application state.
		- `fiji_script_*`: create, edit, and run scripts.
		- `fiji_macro_*`: create, edit, and run macros.
		- `fiji_image_*`: queries and view open images.
		- `fiji_rois_*`: interact with regions of interest (ROIs).
		- `fiji_results_*`: reads the ImageJ Results Table.
		- `fiji_log_*`: capture a variety of diagnostic data.

		Tool descriptions contain information relevant to usage.

		## Guidance Tools
		The `fiji_guidance_*` tools provide on-demand access to information essential for effective Fiji interaction.

		1. Use `fiji_guidance_topics` to see the exact topic keywords available.
		2. Use `fiji_guidance_search` to see the documents available for one exact topic keyword.
		3. Use `fiji_guidance_read` to read an exact document ID.

		### Agent Goals
		The provided tools, and therefore guides, focus on several core agentic goals within Fiji.
		Before using an agentic tool for the first time, determine which goal best applies, and ensure you have
		read the corresponding initial guide, while following situational directions to further related guides
		that apply to the current use-case.

		| Goal | Initial guide ID(s) |
		|---|---|
		| Perform image analysis | '%1$s' |
		| Create reproducible workflows | '%2$s' |
		| Query application information | `%3$s`, `%4$s`, `%5$s` |

		### Additional topics
		These topics are not directly related to tool use. Read them when a user raises a related question.

		| Content | Guide ID(s) |
		|---|---|
		| LLM capabilities | `%6$s`, `%7$s` |
		| Support and community resources | `%8$s` |
		| Extending and contributing | `%9$s` |
		""".formatted(RunningCommandsGuide.ID, ScriptsAndMacrosGuide.ID,
			EnvironmentInspectionGuide.ID, UpdateSitesGuide.ID, UIInteractionGuide.ID,
			IntegratedChatGuide.ID, MCPServerGuide.ID, SupportAndCommunityGuide.ID,
			ExtensionContributionGuide.ID).strip();

	public OnboardingGuide() {
		super(ID, "Fiji Agent Onboarding", AgentGuide.topics(Topic.ONBOARDING, Topic.FIJI,
			Topic.TOOLS),
			Authority.PROJECT_AUTHORED, List.of(RunningCommandsGuide.ID, ScriptsAndMacrosGuide.ID,
			EnvironmentInspectionGuide.ID, UIInteractionGuide.ID, UpdateSitesGuide.ID,
			IntegratedChatGuide.ID, MCPServerGuide.ID, SupportAndCommunityGuide.ID,
			ExtensionContributionGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
