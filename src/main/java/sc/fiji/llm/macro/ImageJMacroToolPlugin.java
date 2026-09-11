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

package sc.fiji.llm.macro;

import java.awt.Frame;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;

/**
 * AI tool collection that provides macro recording capabilities for the LLM.
 * Allows the assistant to open the macro recorder and capture user actions.
 */
@Plugin(type = AiToolPlugin.class)
public class ImageJMacroToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private LegacyService legacyService;

	public ImageJMacroToolPlugin() {
		super(ImageJMacroToolPlugin.class);
	}

	@Override
	public String getToolScope() {
		return ToolScope.MACRO;
	}

	@Override
	public String getName() {
		return "Macro Writing Tools";
	}

	@Override
	public String getUsage() {
		return """
The fiji_macro_* tools support creation of ImageJ macros: a custom script format where a sequence of functions can be saved, adapted, and replayed.
Macro creation follows an intuitive workflow: 1) open the macro recorder to start recording; 2) the user executes commands (or agent via fiji_command_* tools) which are recorded in the order they are run; 3) stop recording and create an .ijm script; 4) use fiji_script_* tools to edit the created macro
""";
	}

	@Tool(value = { "List the built-in ImageJ macro function categories. Use fiji_macro_list_functions with one of these categories to see its functions." }, name = "fiji_macro_list_categories")
	public String listMacroCategories() {
		try {
			JsonArray categories = new JsonArray();
			for (String category : MacroFunctionRegistry.getCategories()) {
				categories.add(category);
			}
			return jsonProp("categories", categories).toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_macro_list_categories: " + e.getMessage());
		}
	}

	@Tool(value = { "List the built-in ImageJ macro functions for a category. Use fiji_macro_list_categories first to find valid categories; each result includes the function signature and description" }, name = "fiji_macro_list_functions")
	public String listMacroFunctionsByCategory(@P("category") String category) {
		try {
			if (category == null || category.trim().isEmpty()) {
				return jsonError("Category cannot be empty");
			}

			JsonArray functions = new JsonArray();
			for (MacroFunctionRegistry.MacroFunction function : MacroFunctionRegistry
				.getByCategory(category))
			{
				JsonObject functionJson = new JsonObject();
				functionJson.addProperty("name", function.getName());
				functionJson.addProperty("description", function.getDescription());
				functions.add(functionJson);
			}

			JsonObject result = new JsonObject();
			result.addProperty("category", category);
			result.add("functions", functions);
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_macro_list_functions: " + e.getMessage());
		}
	}

	@Tool(value = { "Start the ImageJ macro recorder, or bring the existing recorder to the front. When the recorder is open, ALL commands will be recorded, in the order they run" }, name = "fiji_macro_start_recorder" )
	public String startRecorder() {
		try {
			// Run the macro recorder command through ImageJ
			// The Recorder class will automatically handle bringing the existing
			// instance to front if it's already open (see ij.plugin.frame.Recorder
			// constructor)
			if (SwingUtilities.isEventDispatchThread()) {
				legacyService.runLegacyCommand("ij.plugin.frame.Recorder", "");
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					legacyService.runLegacyCommand("ij.plugin.frame.Recorder", "");
				});
			}
			JsonObject result = new JsonObject();
			result.addProperty("recorder_started", true);
			return result.toString();
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_macro_start_recorder: " + e.getMessage());
		}
	}

	@Tool(value = { "Check whether the ImageJ macro recorder is currently open" }, name = "fiji_macro_recorder-state")
	public String getMacroRecorderState() {
		try {
			boolean recorderOpen = false;
			for (Frame frame : Frame.getFrames()) {
				if (frame.toString().startsWith("ij.plugin.frame.Recorder")) {
					recorderOpen = frame.isVisible();
					break;
				}
			}
			JsonObject result = new JsonObject();
			result.addProperty("recorder_is_open", recorderOpen);
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_macro_recorder-state: " + e.getMessage());
		}
	}
}
