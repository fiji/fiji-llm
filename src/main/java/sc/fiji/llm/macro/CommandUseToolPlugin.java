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
import sc.fiji.llm.execution.EnvironmentSnapshotService;
import sc.fiji.llm.execution.EnvironmentSnapshotService.PixelChangeTracking;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;

/**
 * AI tool collection for LLM discovery and execution of available commands.
 */
@Plugin(type = AiToolPlugin.class)
public class CommandUseToolPlugin extends AbstractAiToolPlugin {

	private static final int MAX_RESULTS = 10;
	private static final long UI_SETTLE_DELAY_MS = 250;

	@Parameter
	private SearchService searchService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private EnvironmentSnapshotService environmentSnapshotService;

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

	@Tool(value = { "Execute an ImageJ command. Commands that need an image generally use Fiji's active image, so verify the intended image is active before running when multiple images are open. Returns command status, environment impact, and log output observed during the operation." },
		name = "fiji_command_run" )
	public String runCommand(@P(name = "menu_path", value = "Exact menu path from fiji_command_search") String menuPath) {
		EnvironmentSnapshotService.EnvironmentCapture capture = null;
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

			capture = environmentSnapshotService.capture(PixelChangeTracking.FINAL_SHA256);
			// Run the module - this goes through the same path as the search panel
			// and includes automatic recorder integration.
			moduleService.run(moduleInfo, true);
			waitForUiToSettle();

			JsonObject command = new JsonObject();
			command.addProperty("name", moduleInfo.getTitle());
			command.addProperty("menu_path", menuString);
			JsonObject result = new JsonObject();
			result.add("executed_command", command);
			result.addProperty("status", "success");
			final JsonObject environment = capture.finish().toJson();
			if (environment.size() > 0) result.add("environment_impact", environment);
			if (hasOpenedDialogs(environment)) addToolRecommendations(
				result, "fiji_ui_dialog_respond", "fiji_ui_dialog_close");
			return result.toString();
		}
		catch (RuntimeException e) {
			if (capture != null) {
				return commandError(menuPath, capture.finish(), e.getMessage());
			}
			return jsonError("Failed to run fiji_command_run: " + e.getMessage());
		}
	}

	private static boolean hasOpenedDialogs(final JsonObject environment) {
		return environment.has("changes") && environment.getAsJsonObject("changes")
			.has("dialogs_opened");
	}

	private void waitForUiToSettle() {
		try {
			Thread.sleep(UI_SETTLE_DELAY_MS);
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for UI to settle", e);
		}
	}

	private String commandError(final String menuPath,
		final EnvironmentSnapshotService.EnvironmentImpact impact,
		final String diagnostic)
	{
		final JsonObject command = new JsonObject();
		command.addProperty("menu_path", menuPath);
		final JsonObject result = new JsonObject();
		result.add("executed_command", command);
		result.addProperty("status", "infrastructure_error");
		if (diagnostic != null && !diagnostic.isBlank()) result.addProperty("diagnostic",
			diagnostic);
		final JsonObject environment = impact.toJson();
		if (environment.size() > 0) result.add("environment_impact", environment);
		return result.toString();
	}

	@Tool(value = { "Search for available ImageJ commands, sorted by descending relevance. Returns matching command names and menu paths." },
		name = "fiji_command_search" )
	public String searchCommands(@P(name = "query", value = "Term to match against command names") String query) {
		try {
			if (query == null || query.trim().isEmpty()) {
				return jsonError("Search query cannot be empty");
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
				operation.search(query);

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
			boolean hasRunnableCommand = false;
			for (SearchResult result : results) {
				if (result instanceof ModuleSearchResult msr) {
					final JsonObject command = formatModuleResult(msr);
					commands.add(command);
					if (command.has("menu_path")) hasRunnableCommand = true;
				}
			}

			JsonObject searchResult = new JsonObject();
			searchResult.addProperty("search_name", query);
			searchResult.add("commands", commands);
			if (hasRunnableCommand) addToolRecommendations(searchResult,
				"fiji_command_run");
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
