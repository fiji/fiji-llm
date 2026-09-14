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

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.script.ScriptContextItem;
import sc.fiji.llm.script.ScriptID;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;
import sc.fiji.llm.ui.TextEditorUtils;

/**
 * AI tool collection that provides macro recording capabilities for the LLM.
 * Allows the assistant to open the macro recorder and capture user actions.
 */
@Plugin(type = AiToolPlugin.class)
public class ImageJMacroToolPlugin extends AbstractAiToolPlugin {

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private LogService logService;

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

	@Tool(value = { "Close the ImageJ macro recorder and stop recording. Use fiji_macro_create_script first if the current macro should be transferred to the script editor." }, name = "fiji_macro_close_recorder")
	public String closeRecorder() {
		try {
			String[] errors = new String[1];
			Runnable closeAction = () -> {
				Frame recorder = findRecorderFrame();
				if (recorder == null) {
					errors[0] = jsonError("ImageJ macro recorder is not open",
						"fiji_macro_start_recorder");
					return;
				}

				recorder.dispatchEvent(new WindowEvent(recorder,
					WindowEvent.WINDOW_CLOSING));
			};

			if (SwingUtilities.isEventDispatchThread()) {
				closeAction.run();
			}
			else {
				SwingUtilities.invokeAndWait(closeAction);
			}

			if (errors[0] != null) {
				return errors[0];
			}

			JsonObject result = new JsonObject();
			result.addProperty("recorder_closed", true);
			return result.toString();
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_macro_close_recorder: " + e
				.getMessage());
		}
	}

	@Tool(value = { "Transfer the current macro recorder state to the script editor. Use fiji_script_* tools for script interaction" }, name = "fiji_macro_create_script")
	public String createScript() {
		try {
			String[] errors = new String[1];
			Runnable createAction = () -> {
				Frame recorder = findRecorderFrame();
				if (recorder == null) {
					errors[0] = jsonError("ImageJ macro recorder is not open", "fiji_macro_start_recorder");
					return;
				}

				Button createButton = findButton(recorder, "Create");
				if (createButton == null) {
					logService.debug("fiji_macro_create_script failure: The macro recorder does not have a Create button");
					errors[0] = jsonError("Could not locate the Macro Recorder's Create button. Please instruct user to click Create manually.");
					return;
				}

				createButton.dispatchEvent(new ActionEvent(createButton,
					ActionEvent.ACTION_PERFORMED, createButton.getActionCommand()));
			};

			if (SwingUtilities.isEventDispatchThread()) {
				createAction.run();
			}
			else {
				SwingUtilities.invokeAndWait(createAction);
			}

			if (errors[0] != null) {
				return errors[0];
			}

			ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError( "Macro was created, but no script editor is active", "fiji_script_list");
			}

			JsonObject result = new JsonObject();
			result.addProperty("macro_transferred", true);
			result.addProperty(ScriptContextItem.SCRIPT_ID_KEY, scriptID.toString());
			return result.toString();
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_macro_create_script: " + e
				.getMessage());
		}
	}

	private static Frame findRecorderFrame() {
		for (Frame frame : Frame.getFrames()) {
			if (frame.isDisplayable() && "ij.plugin.frame.Recorder".equals(frame
				.getClass().getName()))
			{
				return frame;
			}
		}
		return null;
	}

	private static Button findButton(final Container container,
		final String label)
	{
		for (Component component : container.getComponents()) {
			if (component instanceof Button && label.equals(((Button) component)
				.getLabel()))
			{
				return (Button) component;
			}
			if (component instanceof Container) {
				Button button = findButton((Container) component, label);
				if (button != null) return button;
			}
		}
		return null;
	}

	@Tool(value = { "Check whether the ImageJ macro recorder is currently open" }, name = "fiji_macro_recorder-state")
	public String getMacroRecorderState() {
		try {
			JsonObject result = new JsonObject();
			result.addProperty("recorder_is_open", findRecorderFrame() != null);
			return result.toString();
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_macro_recorder-state: " + e.getMessage());
		}
	}
}
