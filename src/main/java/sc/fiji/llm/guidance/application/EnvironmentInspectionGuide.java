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

			Fiji state can change between requests and between tool calls.  Prefer current
			queries over older application context when the two disagree.

			## Communication channels

			Different channels answer different questions.

			- **Executed tool results:** Start here. It is the most direct evidence
			  about what that tool attempted and whether it reported an error.
			- **ImageJ's legacy log:** Use `fiji_log_imagej_read` for messages in the
			  ImageJ 1.x Log window. Provides a cumulative history.
			- **Script logs:** Stored per-script. Output and error deltas are attached
			  when run via `fiji_script_run`. `fiji_script_read_logs` provides a
			  cumulative history.
			- **LogService and console streams:** Require the multi-step SciJava logging
			  workflow below when.
			- **Host and installation state:** Use `fiji_system_read` for broad runtime
			  facts.

			## SciJava logging workflow

			SciJava capture is interval-based and is not retroactive. If an operation requires
			diagnosis:

			1. Call `fiji_log_scijava_start_capture` immediately before the operation.
			2. Run the operation while the capture is active. Keep the interval narrow because
			   messages from other activity during that interval are captured too.
			3. Optionally call `fiji_log_scijava_read` while a long-running operation is in
			   progress or to inspect an interim snapshot. This does not stop capture.
			4. Call `fiji_log_scijava_stop_capture` after the operation to get the final
			   capture and close it.

			Only one SciJava capture can be active at a time. Always stop a capture after the
			target operation, including after a failure, to avoid future confusion.

			## Interpreting evidence

			Correlate channels by timing and operation. A SciJava error, console stderr,
			ImageJ Log message, visible dialog, or tool error may describe the same failure
			from a different layer, while a successful tool return does not guarantee that
			an asynchronous operation has finished.
			""".strip();

	public EnvironmentInspectionGuide() {
		super(ID, "Environment Inspection", AgentGuide.topics(Topic.APPLICATION,
			Topic.ENVIRONMENT, Topic.PRINT_STREAM, Topic.LOGS), Authority.PROJECT_AUTHORED,
			List.of(RunningCommandsGuide.ID, ScriptsAndMacrosGuide.ID, OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
