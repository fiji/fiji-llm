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

import java.util.List;

import org.scijava.plugin.Plugin;

import sc.fiji.llm.guidance.AbstractAgentGuide;
import sc.fiji.llm.guidance.AgentGuide;
import sc.fiji.llm.guidance.AgentGuideMetadata.Authority;
import sc.fiji.llm.guidance.OnboardingGuide;
import sc.fiji.llm.guidance.data.DataTypesGuide;

/** Guidance for choosing between scripts and macros in Fiji. */
@Plugin(type = AgentGuide.class)
public class ScriptsAndMacrosGuide extends AbstractAgentGuide {

	public static final String ID = "scripts-and-macros";

	private static final String CONTENT = """
			# Scripts and Macros in Fiji
			
			As a tool for scientific analysis, the FAIR principles are useful guides at all levels of operation:
			- Findable
			- Accessible
			- Interoperable
			- Reusable
			
			To this end, workflows that can be persisted, distributed, and reused across host environments are
			the default goal in Fiji. One of your largest impacts is in ensuring any solutions you support a user in
			creating are recorded in one of the key workflow formats of Fiji: scripts and macros.

			## Scripts

			Scripts are standalone programs. Most supported script languages do not need to be compiled, allowing rapid
			iteration and exploration of functionality without restarting Fiji. Scripts expose many core programming
			features of the Fiji ecosystem, making them incredibly powerful. This also presents a barrier for less technical
			biologist users, and thus is a key area where LLM agents can provide impact.

			As we are running in Fiji, a Java host application, each script language may have its own particulars that could
			differ from coding in the same language "natively." Many languages are connected via the JSR223 script API,
			but there are exceptions.
			
			Before writing a script in Fiji, read Guide ID `%1$s`, and at least one language-specific Guide
			appropriate to your particular use.

			## Macros

			Macros originated in ImageJ 1.x as a custom workflow format. They are generated in an intuitive, user-friendly
			manner: when the Macro Recorder is open, every time a recordable command is run its execution string is "recorded,"
			including used parameters. This allows an image analysis workflow to be run once manually, then saved and generalized
			for repetition.

			In Fiji, the resulting portable ImageJ macro files (*.ijm) have been absorbed into the Script Editor's umbrella. Therefore,
			before creating a Macro in Fiji, read both Guide IDs `%2$s` and `%3$s`. A language-specific script
			guide is not necessary for this use.

			## Shared concepts

			The functionality underlying both scripts and macros is directly tied to available Commands (Guide ID: `%4$s`).
			Both scripts and macros run asynchronously; if a poll return of `blocked_by_dialog` is an interaction point:
			inspect the dialog with `fiji_ui_dialogs_read`, respond with `fiji_ui_dialog_respond` using the exact title
			and button text.
			Finally, you should be familiar with the data types in Guide ID: `%5$s`.
			""".formatted(ScriptingGuide.ID, ScriptingGuide.ID, CreatingMacrosGuide.ID,
				RunningCommandsGuide.ID, DataTypesGuide.ID).strip();

	public ScriptsAndMacrosGuide() {
		super(ID, "Scripts and Macros in Fiji", AgentGuide.topics(Topic.WORKFLOWS, Topic.SCRIPTS,
			Topic.MACROS),
			Authority.PROJECT_AUTHORED, List.of(RunningCommandsGuide.ID, DataTypesGuide.ID,
				ScriptingGuide.ID, CreatingMacrosGuide.ID, OnboardingGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
