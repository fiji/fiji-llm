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
import java.util.List;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;

/**
 * AI tool that provides macro recording capabilities for the LLM. Allows the
 * assistant to open the macro recorder and capture user actions.
 */
// @Plugin(type = AiToolPlugin.class)
public class ImageJMacroTool extends AbstractAiToolPlugin {

	@Parameter
	private LegacyService legacyService;

	public ImageJMacroTool() {
		super(ImageJMacroTool.class);
	}

	@Override
	public String getToolScope() {
		return ToolScope.MACRO;
	}

	@Override
	public String getName() {
		// legacyService.getIJ1Helper().getIJ().getWindows();
		return "Macro Writing Tools";
	}

	@Override
	public String getUsage() {
		return "We use ImageJ Macros to build reproducible workflows. These tools support macro creation and editing.\n" +
			"To start recording a macro, use startRecorder.\n" +
			"To find macro functions, use: 1) listMacroCategories(), 2) listMacroFunctionsByCategory(category).";
	}

	@Tool(value = { "Lists the built-in ImageJ macro function categories. Use fiji_macro_list_functions to list the functions for a particular category." }, name = "fiji_macro_list_categories")
	public String listMacroCategories() {
		List<String> categories = MacroFunctionRegistry.getCategories();
		if (categories.isEmpty()) {
			return "No categories found";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Available macro function categories:\n\n");
		for (String category : categories) {
			sb.append("• ").append(category).append("\n");
		}
		return sb.toString();
	}

	@Tool(value = { "Lists the built-in ImageJ macro functions for the given category. Use fiji_macro_list_categories first to find categories." }, name = "fiji_macro_list_functions")
	public String listMacroFunctionsByCategory(@P("category") String category) {
		if (category == null || category.trim().isEmpty()) {
			return jsonError("Category cannot be empty");
		}

		List<MacroFunctionRegistry.MacroFunction> functions = MacroFunctionRegistry
			.getByCategory(category);

		if (functions.isEmpty()) {
			return "No functions found in category: " + category;
		}

		StringBuilder sb = new StringBuilder();

		for (MacroFunctionRegistry.MacroFunction func : functions) {
			sb.append("• **").append(func.simpleString()).append("**\n");
		}

		return sb.toString();
	}

	@Tool(value = { "Start the macro recorder" }, name = "fiji_macro_start_recorder" )
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
				SwingUtilities.invokeLater(() -> {
					legacyService.runLegacyCommand("ij.plugin.frame.Recorder", "");
				});
			}
			return "Macro recorder is now active.";
		}
		catch (RuntimeException e) {
			return jsonError("Failed to open macro recorder");
		}
	}

	@Tool(value = { "Check whether the ImageJ macro recorder is currently open." }, name = "fiji_macro_recorder-state")
	public String getMacroRecorderState() {
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
}
