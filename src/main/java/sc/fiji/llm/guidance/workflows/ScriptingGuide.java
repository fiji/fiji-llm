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
import sc.fiji.llm.guidance.workflows.scriptlanguages.ScriptingGroovyGuide;
import sc.fiji.llm.guidance.workflows.scriptlanguages.ScriptingJythonGuide;
import sc.fiji.llm.guidance.workflows.scriptlanguages.ScriptingPythonGuide;

/** Guidance for writing and running scripts in Fiji. */
@Plugin(type = AgentGuide.class)
public class ScriptingGuide extends AbstractAgentGuide {

	public static final String ID = "scripting";

	private static final String CONTENT = """
			# Writing and Running Scripts in Fiji

			## Script Parameters
			All scripts can declare **Parameters**, typed inputs and outputs, with a universal
			`#@ <type> <variable>` syntax. Put one Parameter declaration per line at the top of
			a script before code, comments, or whitespace. Parameter types do not need fully
			qualified packages.

			An input declaration does two things: it creates the script variable and describes
			how Fiji should obtain its value. DO NOT re-define inputs in the script body.

			For outputs a variable MUST be defined in the script body with a name matching the
			declaration.

			### Key concepts
			In the following tables, <image> is any of: `Dataset`, `ImagePlus`, `ImgPlus`,
			`ImageDisplay`, `DatasetView`, `DataView`, `Overlay`, Position`, or `ChannelCollection`
			DO NOT use <image> as a literal type. Use `ImagePlus` for ImageJ 1.x API, or `Dataset`
			for ImageJ2 API. The rest are highly situational.

			DO NOT put package prefixes on Parameter types.

			#### Auto-filled
			| Type(s) | Notes |
			|---|---|
			| <image> | If single unresolved, auto-fillable image input: populate with active |
			| Any SciJava `Service`, `Gateway`, or `Context` | Injected from app `Context` |

			#### Parameter properties
			These are optional; set by `key=value` in a comma-separated, parenthetical list between
			a Parameter's type and name.

			| Property | type | notes |
			|---|---|---|
			| value | same as Parameter | Default value |
			| required | boolean | If param must be non-null |
			| persist | boolean | If last-used values are saved |
			| autoFill | boolean | Whether Fiji may populate the value automatically |
			| min | string | Numeric types only |
			| max | string | Numeric types only |
			| stepSize | string | Numeric types only |
			| columns | int | Text fields only |
			| choices | string[] | Multiple choice text fields only |
			| label | string | Label text in UI |
			| visibility | string | values: NORMAL, TRANSIENT, INVISIBLE, MESSAGE |
			| style | string | UI widget style |

			#### Auto-generated widgets
			| Type(s) | Widget | `style` options |
			|---|---|---|
			| `Boolean` | checkbox | |
			| `Byte`, `Short`, `Integer`, `Long` | numeric field | `slider`, `spinner`, `scroll bar` |
			| `Float`, `Double` | numeric field | `slider`, `spinner`, `scroll bar` |
			| `BigInteger`, `BigDecimal` | numeric field | `slider`, `spinner`, `scroll bar` |
			| `Character`, `String` | text field | `text field`, `text area`, `password` |
			| <image> | If multiple unresolved image inputs: create a dropdown chooser | |
			| `ColorRGB` | color chooser | |
			| `Date` | date chooser | |
			| `File` | file chooser | `open`, `save`, `directory`, `extensions:x/y/z` |
			| `File[]` | multi-file chooser | `files`, `directories`, `both`, `extensions:x/y/z` |

			### Sample usage
			```python
			#@ Dataset dataset
			#@ Integer (label="Threshold", min=0, max=255, value=128) threshold
			#@output Dataset result

			# dataset var (input) auto-filled; available here
			# threshold var (input) filled by UI; available here
			# result var (output) still needs to be defined
			result = ...
			```

			### Impact
			Parameter declarations also make scripts usable without a UI. For example, a saved script can be
			run headlessly with `fiji --headless --run path/to/script.py 'name="value",count=3'`, using
			parameter names as the keys.

			So a script is really a reusable module, without hard-coding GUI creation or relying on
			global state.

			Script parameters completely replace ImageJ 1.x GenericDialog usage: NEVER use
			`ij.gui.GenericDialog` to gather script or macro inputs.

			## Script development workflow
			1. Inspect the current Fiji script state with `fiji_script_list`.
			2. Do not overwrite an existing script unless the user asks for it; create a new editor or script otherwise.
			3. Use `fiji_script_rename` to set the filename extension to the requested language; use your judgement if not specified
			4. Use the appropriate `fiji_script_*` tools to replace, edit, delete or read the complete script or selected line ranges, as needed.
			5. Start a script run with `fiji_script_run`. Poll with `fiji_script_run_status` using the returned `run_id` until the run reaches a terminal state.
			6. Verify script behavior using observable Fiji results appropriate to the script's goal, such as image, ROI, or Results Table changes.
			7. If a concrete failure is identified, attempt repair: use the smallest script edit that addresses the diagnostic, then rerun from (5) above.
			8. Stop and report the blocker if success cannot be attained after two focused repair attempts, or if the same infrastructure failure persists.

			## Diagnostic guidance
			- Treat `success`, `finished_with_errors`, `timed_out`, and `infrastructure_error` as distinct outcomes.
			- If an error is indicated, classify the `errors` field:
				- Syntax or compilation failure: parser, compiler, expected-token, or source-location messages.
				- Runtime failure: an exception raised after parsing, usually with a script path and line number.
				- Infrastructure failure: missing engine, class-loading failure, uninitialized Fiji service, or an MCP/tool error.
			- If the result reports failed termination, inspect the script and logs before retrying.

			## Reporting
			Summarize the final script name, language, actions taken, diagnostic classification, repair attempts, and verification result. Distinguish clearly between:
			- confirmed success,
			- execution completed but not verified,
			- script failure with actionable diagnostics, and
			- Tool or Fiji infrastructure failure.

			## Recommended script languages
			All script languages have access to Fiji's Java classpath. Pick the language you know best, and best fits your needs.

			| Language | Reason to use | Guide ID |
			|---|---|---|
			| Jython | Python mode is disabled (`Edit > Options > Python...`) | `%2$s` |
			| Python | Connect to load external Python libraries (Python mode required) |`%3$s` |
			| Groovy | Java-like syntax | `%1$s` |

			## Additional resources
			Scripting documentation can be found on the ImageJ wiki: https://imagej.net/scripting/
			""".formatted(ScriptingGroovyGuide.ID, ScriptingJythonGuide.ID,
				ScriptingPythonGuide.ID).strip();

	public ScriptingGuide() {
		super(ID, "Writing and Running Scripts", AgentGuide.topics(Topic.WORKFLOWS, Topic.SCRIPTS),
			Authority.PROJECT_AUTHORED, List.of(
				ScriptingPythonGuide.ID, ScriptingJythonGuide.ID, ScriptingGroovyGuide.ID,
				ScriptsAndMacrosGuide.ID));
	}

	@Override
	public String content() {
		return CONTENT;
	}
}
