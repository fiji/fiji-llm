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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.execution.ExecutionEnvironmentSnapshotService;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;

/**
 * AI tool collection for LLM discovery and execution of available commands.
 */
@Plugin(type = AiToolPlugin.class)
public class CommandUseToolPlugin extends AbstractAiToolPlugin {

	private static final int MAX_RESULTS = 10;

	@Parameter
	private SearchService searchService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private ExecutionEnvironmentSnapshotService environmentSnapshotService;

	public CommandUseToolPlugin() {
		super(CommandUseToolPlugin.class);
	}

	@Override
	public String getToolScope() {
		return ToolScope.MACRO;
	}

	@Override
	public String getName() {
		return "Command Interaction Tools";
	}

	@Override
	public String getUsage() {
		return """
The fiji_command_* tools discover and execute ImageJ commands. Available may commands vary based on installed plugins, so verify a command's presence before attempting to run it.
""";
	}

	@Tool(value = { "Execute a command using its full menu path and return its status, before/after environment impact, and ImageJ/SciJava logs produced by this command. Use fiji_command_search to find a command's menu path." },
		name = "fiji_command_run" )
	public String runCommand(@P("menu_path") String menuPath) {
		ExecutionEnvironmentSnapshotService.EnvironmentCapture capture = null;
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

			capture = environmentSnapshotService.capture();
			// Run the module - this goes through the same path as the search panel
			// and includes automatic recorder integration
			moduleService.run(moduleInfo, true);

			JsonObject command = new JsonObject();
			command.addProperty("name", moduleInfo.getName());
			command.addProperty("menu_path", menuString);
			JsonObject result = new JsonObject();
			result.add("executed_command", command);
			result.addProperty("status", "success");
			result.add("environment", capture.finish().toJson());
			return result.toString();
		}
		catch (RuntimeException e) {
			if (capture != null) {
				return commandError(menuPath, capture.finish(), e.getMessage());
			}
			return jsonError("Failed to run fiji_command_run: " + e.getMessage());
		}
	}

	private String commandError(final String menuPath,
		final ExecutionEnvironmentSnapshotService.EnvironmentImpact impact,
		final String diagnostic)
	{
		final JsonObject command = new JsonObject();
		command.addProperty("menu_path", menuPath);
		final JsonObject result = new JsonObject();
		result.add("executed_command", command);
		result.addProperty("status", "infrastructure_error");
		result.addProperty("diagnostic", diagnostic == null ? "" : diagnostic);
		result.add("environment", impact.toJson());
		return result.toString();
	}

	@Tool(value = { "Search for available ImageJ commands whose name matches the given term, sorted by descending relevance. The returned menu_path can be used with fiji_command_run" },
		name = "fiji_command_search" )
	public String searchCommands(@P("search_name") String searchName) {
		try {
			if (searchName == null || searchName.trim().isEmpty()) {
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
				operation.search(searchName);

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

			JsonArray commands = new JsonArray();
			for (SearchResult result : results) {
				if (result instanceof ModuleSearchResult msr) {
					commands.add(formatModuleResult(msr));
				}
			}

			JsonObject searchResult = new JsonObject();
			searchResult.addProperty("search_name", searchName);
			searchResult.add("commands", commands);
			return searchResult.toString();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return jsonError("Failed to run fiji_command_search: Search interrupted");
		}
		catch (RuntimeException e) {
			return jsonError("Failed to run fiji_command_search: " + e.getMessage());
		}
	}

	private JsonObject formatModuleResult(ModuleSearchResult msr) {
		JsonObject command = new JsonObject();
		ModuleInfo info = msr.info();

		command.addProperty("name", msr.name());

		if (info.getMenuPath() != null && !info.getMenuPath().isEmpty()) {
			command.addProperty("menu_path", info.getMenuPath().getMenuString(true));
		}

		if (info.getMenuPath() != null && info.getMenuPath().getLeaf() != null &&
			info.getMenuPath().getLeaf().getAccelerator() != null)
		{
			command.addProperty("shortcut", info.getMenuPath().getLeaf()
				.getAccelerator().toString());
		}

		return command;
	}
}
