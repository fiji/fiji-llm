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
package sc.fiji.llm.guidance.workflows;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;

/** Guidance for creating ImageJ macros. */
@Plugin(type = AgentGuide.class)
public class CreatingMacrosGuide extends AbstractAgentGuide {

	public static final String ID = "creating-macros";

	private static final String CONTENT = """
			# ImageJ Macro use in Fiji

			## Diagnostic Guidance

			- Use `fiji_macro_run_status` with the returned `run_id` for an asynchronous macro run.
			- Treat `running`, `success`, `finished_with_warnings`, `finished_with_errors`, `blocked_by_dialog`, and `infrastructure_error` as distinct outcomes. A completed macro with only known non-fatal diagnostics reports `finished_with_warnings` and includes them in `warnings`.
			- A `running` result with `wait_expired: true` is still active; poll its run status instead of retrying.
			- A modal `showMessage` dialog is an interaction point, not by itself a macro error. Genuine macro-error dialogs and structured error output should be reported as errors.
			- Prefer the per-run output, errors, console streams, and ImageJ/SciJava logs in the execution result for diagnosing that run. `fiji_script_read_logs` returns cumulative logs retained by the active Script Editor across runs.
			- If a desired command is unavailable, report the infrastructure limitation instead of repeatedly changing the macro.

			## Reporting

			Summarize the recorded commands, the created `.ijm` script name, any edits made after recording, the execution status, diagnostics, and the observable verification result. Distinguish clearly between:

			- macro recorded and transferred successfully;
			- macro executed successfully and verified;
			- macro execution completed with non-fatal warnings;
			- macro execution completed with actionable errors;
			- macro execution timed out or could not be terminated; and
			- Fiji-MCP or Fiji infrastructure failure

			## Additional resources
			
			Core macro documentation can be found on the ImageJ wiki: https://imagej.net/scripting/macro
			""".strip();

	public CreatingMacrosGuide() {
		super(ID, "Creating Macros",
			"Create, record, edit, run, and diagnose ImageJ macros in Fiji.",
			AgentGuide.topics(Topic.WORKFLOWS, Topic.MACROS, Topic.COMMANDS, Topic.SCRIPTS),
			Authority.PROJECT_AUTHORED);
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
