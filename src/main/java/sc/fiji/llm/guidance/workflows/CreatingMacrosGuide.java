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
package sc.fiji.llm.guidance.workflows;

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;

/** Guidance for creating ImageJ macros. */
@Plugin(type = AgentGuide.class)
public class CreatingMacrosGuide extends AbstractAgentGuide {

	public static final String ID = "creating-macros";

	private static final String CONTENT = """
			# ImageJ Macro use in Jiji

			## Workflow

			1. Inspect the current recorder and script state with `fiji_macro_recorder_state` and `fiji_script_list`. Preserve existing recorder contents and scripts; do not overwrite user content unless asked.
			2. Start the recorder with `fiji_macro_start_recorder`, or bring the existing recorder to the front. While the recorder is open, each recordable command produces a parameterized macro invocation.
			3. Run the relevant commands. Commands may be executed by the user or through `fiji_command_*` tools. Only commands supported by the ImageJ recorder produce macro invocations.
			4. Read the recorder buffer with `fiji_macro_recorder_state` and verify that the expected invocations were captured.
			5. To finalize the recorded macro, transfer to the Script Editor with `fiji_macro_create_script`. This tool requires the recorder window and its Create button to be visible.
			6. To stop recording after transferring to a script, close the recorder with `fiji_macro_close_recorder`. Use `fiji_macro_recorder_state` to confirm as needed.
			7. Manual editing can continue in the script editor. Common editing needs include removing unintended command invocations and parameterization to generalize the workflow.
			8. Use the `fiji_script_*` tools to inspect or edit the created `.ijm` script. Keep this extension so that the file is identified as an ImageJ macro.
			9. ImageJ has a number of built-in macro functions for use in editing. Use `fiji_macro_list_categories` to discover available function categories, then `fiji_macro_list_functions` with a category to inspect available function signatures and descriptions.
			10. Use `fiji_macro_run` to run the active `.ijm` script. Poll with `fiji_macro_run_status` until the run reaches a terminal state. If execution pauses on a modal dialog, inspect it with `fiji_ui_dialogs_read`, respond with `fiji_ui_dialog_respond` and the exact title and button text.
			11. Verify the run's result using a observable Fiji state appropriate to the macro's goal, such as image, ROI, or Results Table changes. Do not claim a macro is successful based on a started or transferred response alone.
			12. If a concrete failure is identified, attempt repair: use the smallest edit that addresses the diagnostic, then rerun from (10) above.
			13. Stop if success cannot be attained after a small number of focused repair attempts, or if the same infrastructure failure persists. Report the blocker rather than repeatedly changing macro code.

			## Diagnostic Guidance

			- Use `fiji_macro_run_status` with the returned `run_id` for an asynchronous macro run.
			- Treat `success`, `finished_with_errors`, `timed_out`, and `infrastructure_error` as distinct outcomes.
			- A modal `showMessage` dialog is an interaction point, not by itself a macro error. Genuine macro-error dialogs and structured error output should be reported as errors.
			- Timeouts are requested but not guaranteed. If the result reports failed termination, inspect the script and logs before retrying.
			- Prefer the per-run output, errors, console streams, and ImageJ/SciJava logs in the execution result for diagnosing that run. `fiji_script_read_logs` returns cumulative logs retained by the active Script Editor across runs.
			- If a desired command is unavailable, report the infrastructure limitation instead of repeatedly changing the macro.

			## Reporting

			Summarize the recorded commands, the created `.ijm` script name, any edits made after recording, the execution status, diagnostics, and the observable verification result. Distinguish clearly between:

			- macro recorded and transferred successfully;
			- macro executed successfully and verified;
			- macro execution completed with actionable errors;
			- macro execution timed out or could not be terminated; and
			- Fiji-MCP or Fiji infrastructure failure

			## Additional resources
			
			Core macro documentation can be found on the ImageJ wiki: https://imagej.net/scripting/macro
			""";

	public CreatingMacrosGuide() {
		super(ID, "Creating Macros", List.of(
			"workflows", "macros", "commands", "scripts"), Authority.PROJECT_AUTHORED, List.of(
				RunningCommandsGuide.ID, WritingScriptsGuide.ID, ScriptsAndMacrosGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
