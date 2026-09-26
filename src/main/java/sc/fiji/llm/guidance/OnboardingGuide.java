/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
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
import sc.fiji.llm.guidance.data.DataTypesGuide;
import sc.fiji.llm.guidance.data.ImagesInFiji;
import sc.fiji.llm.guidance.data.ResultsTableGuide;
import sc.fiji.llm.guidance.data.RoisGuide;
import sc.fiji.llm.guidance.workflows.CreatingMacrosGuide;
import sc.fiji.llm.guidance.workflows.RunningCommandsGuide;
import sc.fiji.llm.guidance.workflows.ScriptingGuide;
import sc.fiji.llm.guidance.workflows.ScriptsAndMacrosGuide;

/** Onboarding guidance for agents operating in Fiji. */
@Plugin(type = AgentGuide.class)
public class OnboardingGuide extends AbstractAgentGuide {

	public static final String ID = "onboarding";

	private static final String CONTENT = """
		# Fiji Onboarding

		Fiji is an application for scientific image analysis. Built on the SciJava plugin framework,
		Fiji bundles ImageJ (1.x), ImageJ2, and curated plugins commonly used in bioimage analysis.

		Fiji is an extensible, open source project commonly used for research and publication:
		reproducibility is essential.

		## Reading Guides
		The `fiji_guide_*` tools provide on-demand access to information essential for effective Fiji
		interaction.

		Use `fiji_guide_read` with an exact document ID to read that guide.

		Additional `fiji_guide_*` tools allow exploration of guides by topic.

		## Tools
		Tools are named by functional category. Ensure you have read the corresponding guide(s)
		before calling tools in the following categories.

		| Tool category | Read Guide ID | Tool function |
		|---|---|---|
		| `fiji_script_*` | `%1$s` | create, edit, and run scripts |
		| `fiji_macro_*` | `%2$s` | create, edit, and run macros |
		| `fiji_image_*` | `%3$s` | query and view open images |
		| `fiji_rois_*` | `%4$s` | interact with regions of interest (ROIs) |
		| `fiji_results_*` | `%5$s` | read the ImageJ Results Table |
		| `fiji_ui_*` | `%6$s` | inspect and interact with visible UI components |
		| `fiji_log_*` | `%7$s` | capture runtime diagnostic data |
		| `fiji_system_*` | `%7$s`, `%8$s` | report application and host-environment information |
		| `fiji_command_*` | `%9$s` | perform functions in Fiji |

		Read individual tool descriptions for information relevant to their usage.

		## Decision Guides
		These guides are aimed at helping you make appropriate decisions.

		| Decision Point | Guide ID |
		|---|---|
		| Write a script or macro? | `%10$s` |
		| What data type to use? | `%11$s` |

		## Additional Guides
		These are not directly related to tool use. Read them when a user raises a related question.

		| Content | Guide ID(s) |
		|---|---|
		| LLM capabilities | `%12$s`, `%13$s` |
		| Support and community resources | `%14$s` |
		| Extending and contributing | `%15$s` |
		""".formatted(ScriptingGuide.ID, CreatingMacrosGuide.ID, ImagesInFiji.ID, RoisGuide.ID,
			ResultsTableGuide.ID, UIInteractionGuide.ID, EnvironmentInspectionGuide.ID, 
			UpdateSitesGuide.ID, RunningCommandsGuide.ID, ScriptsAndMacrosGuide.ID,
			DataTypesGuide.ID, IntegratedChatGuide.ID, MCPServerGuide.ID,
			SupportAndCommunityGuide.ID, ExtensionContributionGuide.ID).strip();

	public OnboardingGuide() {
		super(ID, "Fiji Agent Onboarding", AgentGuide.topics(Topic.ONBOARDING, Topic.FIJI,
			Topic.TOOLS),
			Authority.PROJECT_AUTHORED, List.of(ScriptingGuide.ID, CreatingMacrosGuide.ID, ImagesInFiji.ID, RoisGuide.ID,
			ResultsTableGuide.ID, UIInteractionGuide.ID, EnvironmentInspectionGuide.ID, 
			UpdateSitesGuide.ID, RunningCommandsGuide.ID, ScriptsAndMacrosGuide.ID,
			DataTypesGuide.ID, IntegratedChatGuide.ID, MCPServerGuide.ID,
			SupportAndCommunityGuide.ID, ExtensionContributionGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
