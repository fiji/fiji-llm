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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.scijava.MenuPath;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.search.SearchListener;
import org.scijava.search.SearchOperation;
import org.scijava.search.SearchResult;
import org.scijava.search.SearchService;
import org.scijava.search.Searcher;
import org.scijava.search.module.ModuleSearchResult;
import org.scijava.search.module.ModuleSearcher;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolContext;

/**
 * AI tool for LLM agentic discovery and execution of available commands.
 */
@Plugin(type = AiToolPlugin.class)
public class CommandInteractionTool extends AbstractAiToolPlugin {

	private static final int MAX_RESULTS = 10;

	@Parameter
	private SearchService searchService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private PluginService pluginService;

	public CommandInteractionTool() {
		super(CommandInteractionTool.class);
	}

	@Override
	public String getToolContext() {
		return ToolContext.MACRO;
	}

	@Override
	public String getName() {
		return "Command Interaction Tools";
	}

	@Override
	public String getUsage() {
		return """
Commands are reusable functions. They vary by runtime (e.g. installed plugins).
To find available commands, use searchCommands. Always search for a command first to verify it exists before suggesting it to the user.
To run a command, use: 1) searchCommands, then 2) runCommand with the desired menuPath.
""";
	}

	@Tool(value = {
		"Run a command that requires user input (contains \"...\") or is in the \"Open Samples\" menu",
		"Args: menuPath - formatted menu path (e.g., \"File > Open Samples > Blobs\")", })
	public String runCommand(@P("menuPath") String menuPath) {
		try {
			if (menuPath == null || menuPath.isEmpty()) {
				return jsonError("Menu path cannot be empty");
			}

			// Create a MenuPath from the components by joining them
			// MenuPath constructor takes a string like "Plugins > Samples > Blobs"
			MenuPath path = new MenuPath(menuPath);
			String menuString = path.getMenuString();

			// Find the module with this menu path
			ModuleInfo moduleInfo = moduleService.getModules().stream()
				// NB: MenuPath uses object equality
				.filter(info -> menuString.equals(info.getMenuPath().getMenuString()))
				.findFirst().orElse(null);

			if (moduleInfo == null) {
				return jsonError("Command not found at path: " + menuPath);
			}

			// Validate that this command is allowed for agentic use
			// Check for interactive commands (with "..." in the name)
			boolean permittedCommand = false;

			String leafName = path.getLeaf().getName();
			permittedCommand = permittedCommand || leafName.contains("...");

			permittedCommand = permittedCommand || menuString.contains(
				"Open Samples") && !leafName.equals("Open Samples");

			if (!permittedCommand) {
				return jsonError("This command is not allowed for agentic use. Instruct user to run it.");
			}

			// Run the module - this goes through the same path as the search panel
			// and includes automatic recorder integration
			moduleService.run(moduleInfo, true);
			return "Command executed: " + moduleInfo.getName();
		}
		catch (RuntimeException e) {
			return jsonError(e.getMessage());
		}
	}

	@Tool(value = { "Search for available commands",
		"Args: commandName - command to search for (name only, no menu info, e.g., 'Blur', 'Threshold', 'Open')",
		"Returns: info for top commands, most relevant first" })
	public String searchCommands(@P("commandName") String commandName) {
		try {
			if (commandName == null || commandName.trim().isEmpty()) {
				return jsonError("Command name cannot be empty");
			}

			// Collect results with a timeout
			List<SearchResult> results = Collections.synchronizedList(
				new ArrayList<>());
			CountDownLatch searchComplete = new CountDownLatch(1);

			SearchListener listener = event -> {
				// Process results from this search event
				for (SearchResult result : event.results()) {
					if (results.size() < MAX_RESULTS) {
						results.add(result);
					}
					searchComplete.countDown();
				}
			};

			// Save state of all searchers by class
			Map<Searcher, Boolean> originalState = new HashMap<>();
			List<Searcher> allSearchers = pluginService.createInstancesOfType(
				Searcher.class);
			for (Searcher searcher : allSearchers) {
				originalState.put(searcher, searchService.enabled(searcher));
			}

			try {
				// Disable all except ModuleSearcher
				for (Searcher searcher : allSearchers) {
					searchService.setEnabled(searcher,
						searcher instanceof ModuleSearcher);
				}

				// Start the search operation
				SearchOperation operation = searchService.search(listener);
				operation.search(commandName);

				// Wait for results with timeout (2 seconds should be plenty)
				searchComplete.await(2, TimeUnit.SECONDS);
				operation.terminate();

			}
			finally {
				// Restore original state
				for (Map.Entry<Searcher, Boolean> entry : originalState.entrySet()) {
					searchService.setEnabled(entry.getKey(), entry.getValue());
				}
			}

			if (results.isEmpty()) {
				return "No commands found matching: " + commandName;
			}

			// Format results as JSON-like string for LLM consumption
			StringJoiner sb = new StringJoiner(",\n", "[\n", "\n]");
			for (SearchResult result : results) {
				if (result instanceof ModuleSearchResult msr) {
					sb.add(formatModuleResult(msr));
				}
				// Ignore non-module results
			}
			return sb.toString();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return jsonError("Search interrupted");
		}
		catch (RuntimeException e) {
			return jsonError("Search failed");
		}
	}

	/**
	 * Formats a simplified single module search result as a JSON object string.
	 * Includes: name, menu path, shortcut. Not included: identifier, description,
	 * label, additional properties
	 */
	private String formatModuleResult(ModuleSearchResult msr) {
		StringJoiner props = new StringJoiner(", ", "{", "}");
		ModuleInfo info = msr.info();

		// Add name and basic info
		props.add("\"name\": \"" + escapeJson(msr.name()) + "\"");

		// Include menu path if available
		if (info.getMenuPath() != null && !info.getMenuPath().isEmpty()) {
			props.add("\"menuPath\": \"" + escapeJson(info.getMenuPath()
				.getMenuString(true)) + "\"");
		}

		// Include shortcut if available
		if (info.getMenuPath() != null && info.getMenuPath().getLeaf() != null &&
			info.getMenuPath().getLeaf().getAccelerator() != null)
		{
			props.add("\"shortcut\": \"" + escapeJson(info.getMenuPath().getLeaf()
				.getAccelerator().toString()) + "\"");
		}

		return "  " + props;
	}

	/**
	 * Escapes special characters for JSON string representation.
	 */
	private String escapeJson(String value) {
		if (value == null) return "";
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n",
			"\\n").replace("\r", "\\r").replace("\t", "\\t");
	}
}
