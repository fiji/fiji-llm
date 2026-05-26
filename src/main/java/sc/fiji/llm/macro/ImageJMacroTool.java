/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji.llm.macro;

import java.util.List;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.imagej.legacy.LegacyService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolContext;

/**
 * AI tool that provides macro recording capabilities for the LLM. Allows the
 * assistant to open the macro recorder and capture user actions.
 */
@Plugin(type = AiToolPlugin.class)
public class ImageJMacroTool extends AbstractAiToolPlugin {

	@Parameter
	private LegacyService legacyService;

	public ImageJMacroTool() {
		super(ImageJMacroTool.class);
	}

	@Override
	public String getToolContext() {
		return ToolContext.MACRO;
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
}
