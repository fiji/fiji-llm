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
import sc.fiji.llm.guidance.workflows.ScriptsAndMacrosGuide;

/** Guidance for inspecting the Fiji environment. */
@Plugin(type = AgentGuide.class)
public class EnvironmentInspectionGuide extends AbstractAgentGuide {

	public static final String ID = "environment-inspection";

	private static final String CONTENT = """
			# Environment inspection

			Fiji state can change between requests and between tool calls. Inspect the
			narrowest relevant state before modifying an image, script, command, dialog,
			or other workspace object. Prefer current tool results over older application
			context when the two disagree.

			## Communication channels

			Different channels answer different questions. Do not treat an empty channel as
			proof that an operation did nothing, and do not treat one channel as a complete
			record of Fiji state.

			- **The operation's tool result:** Start here. It is the most direct evidence
			  about what that tool attempted and whether it reported an error.
			- **Host and installation state:** Use `fiji_system_read` for broad runtime
			  facts and `fiji_system_list_update_sites` for update-site configuration. Use
			  dedicated image, ROI, table, or display tools for workspace state.
			- **ImageJ's legacy log:** Use `fiji_log_imagej_read` for messages in the
			  ImageJ 1.x Log window. Treat it as cumulative history, not as a scoped record
			  of the latest operation.
			- **SciJava and console output:** Use the multi-step SciJava capture workflow
			  below when diagnosing messages emitted during a particular operation. It is
			  useful even when no console window is visible.
			- **Visible UI:** Use `fiji_ui_windows_read` or `fiji_ui_dialogs_read` when a
			  dialog or window may be blocking progress. Use `fiji_ui_controls_read` to
			  inspect one identified window and `fiji_ui_screenshot` only when visual context
			  is needed. A screenshot is visual evidence, not authoritative state.
			- **Execution-specific diagnostics:** Preserve the result from script, macro,
			  and command tools. Their status and diagnostics are often more relevant than
			  a global log.

			## System inspection

			Call the system tools when installation or host configuration could affect the
			answer, and call them again after a meaningful environment change. Do not use a
			previous system result as proof of current state.

			## SciJava logging workflow

			SciJava capture is interval-based and is not retroactive. To diagnose one
			operation:

			1. Call `fiji_log_scijava_start_capture` immediately before the operation.
			2. Run the operation while the capture is active. Keep the interval narrow because
			   messages from other activity during that interval are captured too.
			3. Optionally call `fiji_log_scijava_read` while a long-running operation is in
			   progress or to inspect an interim snapshot. This does not stop capture.
			4. Call `fiji_log_scijava_stop_capture` after the operation to get the final
			   capture and close it.

			Only one SciJava capture can be active at a time. Starting a second capture is
			an error, and reading or stopping without an active capture is also an error.
			Always stop a capture after the operation, including after a failure, so later
			operations do not become mixed into its diagnostics. Use
			`fiji_log_imagej_read` separately when the suspected evidence is in ImageJ's
			legacy Log window; starting SciJava capture does not make that window a scoped
			operation log.

			## Interpreting evidence

			Correlate channels by timing and operation. A SciJava error, console stderr,
			ImageJ Log message, visible dialog, or tool error may describe the same failure
			from a different layer, while a successful tool return does not guarantee that
			an asynchronous operation has finished. Inspect current UI and application state
			after operations that may open dialogs, change images, or mutate shared tables,
			ROIs, or displays.
			""".trim();

	public EnvironmentInspectionGuide() {
		super(ID, "Environment Inspection", List.of("application",
			"environment", "print-stream", "logs"), Authority.PROJECT_AUTHORED,
			List.of(RunningCommandsGuide.ID, ScriptsAndMacrosGuide.ID, OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
