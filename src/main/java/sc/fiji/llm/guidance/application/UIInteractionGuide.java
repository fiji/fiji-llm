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
import sc.fiji.llm.guidance.workflows.RunningCommandsGuide;

/** Guidance for interacting with the Fiji UI. */
@Plugin(type = AgentGuide.class)
public class UIInteractionGuide extends AbstractAgentGuide {

	public static final String ID = "ui-interaction";

	private static final String CONTENT = """
			# Fiji UI Interaction

			The `fiji_ui_*` tools inspect and, in limited cases, interact with visible Fiji
			windows and dialogs.
			
			Use them to support image analysis workflow development: when application state
			or a blocked workflow is exposed only through the UI. Remember that scripts and
			macros are artifacts that can be saved, edited, shared, and re-run. Clicks in a
			UI are transient.

			## Choosing a UI tool

			- Use `fiji_ui_windows_read` to discover visible windows when the target or its
			  exact title is unknown.
			- Use `fiji_ui_controls_read` to inspect the controls in one identified window.
			  Select the window using its exact title and, when needed, its class name.
			- Use `fiji_ui_dialogs_read` when a dialog may be blocking an operation or when
			  its message and available buttons must be identified.
			- Use `fiji_ui_dialog_respond` only after reading the dialogs. Provide the exact
			  dialog title and button text; never guess which dialog or button to use. Check
			  post-action state afterward.
			- Use `fiji_ui_screenshot` when visual context is useful for a vision-capable
			  client.
			""".strip();

	public UIInteractionGuide() {
		super(ID, "UI Interaction", AgentGuide.topics(Topic.APPLICATION, Topic.UI, Topic.DIALOGS,
			Topic.INPUT, Topic.ERRORS), Authority.PROJECT_AUTHORED,
			List.of(RunningCommandsGuide.ID, OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
