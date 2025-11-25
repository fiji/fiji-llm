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

package sc.fiji.llm.script;

import java.io.File;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.ScriptEditor;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolContext;
import sc.fiji.llm.ui.TextEditorUtils;

/**
 * AI tool that allows the LLM to interact with the Fiji script editor. Provides
 * capabilities to open the editor and create/update scripts.
 */
@Plugin(type = AiToolPlugin.class)
public class ScriptEditorTool extends AbstractAiToolPlugin {

	@Parameter
	private CommandService commandService;

	@Override
	public String getName() {
		return "Script Editor Tools";
	}

	public ScriptEditorTool() {
		super(ScriptEditorTool.class);
	}

	@Override
	public String getToolContext() {
		return ToolContext.SCRIPT;
	}

	@Override
	public String getUsage() {
		return """
Scripts are user-facing single file programs used to build reproducible workflows.
Fiji users have a text editing interface supporting multiple editors open at once.
Each editor can have multiple script files open at once.
A script's file_name extension determines its programming language (e.g., .py, .ijm, .groovy).
Tools to interact with scripts have a "fiji.script" prefix.
Tools will either reference scripts by script_id, or operate on the active script.
""";
	}

	@Tool(value = { "Open a script editor UI if it's not currently open" }, name = "fiji.script.start_editor")
	public String startEditor() {
		try {
			TextEditor textEditor = TextEditorUtils.getMostRecentVisibleEditor();

			if (textEditor == null) {
				// Open the editor on EDT
				if (SwingUtilities.isEventDispatchThread()) {
					commandService.run(ScriptEditor.class, true);
				}
				else {
					SwingUtilities.invokeAndWait(() -> commandService.run(
						ScriptEditor.class, true));
				}

				// Poll for up to 5 seconds for editor to become available
				final long startTime = System.currentTimeMillis();
				final long timeoutMs = 5000;
				while (System.currentTimeMillis() - startTime < timeoutMs) {
					textEditor = TextEditorUtils.getMostRecentVisibleEditor();
					if (textEditor != null) {
						break;
					}
					Thread.sleep(100);
				}

				if (textEditor == null) {
					return jsonError("Failed to open script editor");
				}
			}

			// Get the editor and default tab indices
			int editorIndex = TextEditor.instances.indexOf(textEditor);
			int tabIndex = 0; // Default to the first tab 0

			// Return indication of active tab
			return activeTabJson(textEditor.getTab(tabIndex), editorIndex, tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to start script editor");
		}
	}

	@Tool(value = { "Set the active script by its script_id." }, name = "fiji.script.set_active_script")
	public String setActiveScript(@P("script_id") final String scriptId)
	{
		try {
			// Validate input
			if (scriptId == null || scriptId.trim().isEmpty()) {
				return jsonError("script_id cannot be null or empty");
			}

			// Parse the id string
			final ScriptID scriptID = parseID(scriptId);
			if (scriptID == null) {
				return jsonError("Invalid script_id format. Expected e.g., 0:1");
			}

			// Validate instance index
			if (scriptID.editorIndex < 0 ||
				scriptID.editorIndex >= TextEditor.instances.size())
			{
				return jsonError("Invalid script_id. No text editor at index: " + scriptID.editorIndex);
			}

			final TextEditor textEditor = TextEditor.instances.get(
				scriptID.editorIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				try {
					// Validate tab index
					final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

					if (tab == null) {
						result[0] = jsonError("Invalid script_id. No script tab found at index " + scriptID.tabIndex);
						return;
					}

					// Switch to the specified tab
					textEditor.switchTo(scriptID.tabIndex);

					// Return indication of active tab
					result[0] = activeTabJson(tab, scriptID.editorIndex, scriptID.tabIndex);
				}
				catch (Exception e) {
					result[0] = jsonError("Failed to activate script: " + scriptID.toString());
				}
			});
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to activate script: " + scriptId);
		}
	}

	@Tool(value = { "Get information about the script currently active in the editor." }, name = "fiji.script.get_active_script")
	public String getActiveScript()
	{
		try {
			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found.", "fiji.script.start_editor");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			return activeTabJson(tab, scriptID.editorIndex, scriptID.tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to get active script");
		}
	}

	@Tool(value = { "Create a new script tab in the script editor." }, name = "fiji.script.create_script")
	public String createScript() {
		try {
			// Check if editor is open
			final TextEditor textEditor = TextEditorUtils
				.getMostRecentVisibleEditor();
			if (textEditor == null) {
				return jsonError("Script editor is not open", "fiji.script.start_editor");
			}

			// Create new tab with default empty content
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performCreateNewTab(textEditor);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performCreateNewTab(textEditor);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to create script");
		}
	}

	@Tool(value = { "Completely replace the content of the active script." }, name = "fiji.script.replace_script")
	public String replaceScript(@P("content") final String content)
	{
		try {
			// Validate content
			if (content == null) {
				return jsonError("New script content cannot be null");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				try {

					// Update the tab content
					final EditorPane editorPane = (EditorPane) tab.getEditorPane();
					editorPane.setText(content);

					JsonElement updateScript = ScriptContextUtilities.getTabJson(tab, scriptID);
					result[0] = jsonProp("replaced_script_content", updateScript);
				}
				catch (Exception e) {
					result[0] = jsonError("Failed to update script_id: " + scriptID.toString());
				}
			});
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to update active script.");
		}
	}

	@Tool(value = { "Rename the active script file. Changing its extension will change its script language." },
		name = "fiji.script.rename_script")
	public String renameScript(@P("file_name") final String filename)
	{
		try {
			// Validate filename
			if (filename == null || filename.isEmpty()) {
				return jsonError("New script name cannot be null or empty");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				performRenameScript(scriptID, filename, result);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					performRenameScript(scriptID, filename, result);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to rename script");
		}
	}

	private ScriptID parseID(final String idString) {
		if (idString == null || idString.trim().isEmpty()) {
			return null;
		}
		try {
			// Expected format: "editorIndex:tabIndex"
			final String trimmed = idString.trim();
			final String[] parts = trimmed.split(":");
			if (parts.length != 2) {
				return null;
			}
			final int editorIndex = Integer.parseInt(parts[0].trim());
			final int tabIndex = Integer.parseInt(parts[1].trim());
			return new ScriptID(editorIndex, tabIndex);
		}
		catch (Exception e) {
			return null;
		}
	}

	private String performCreateNewTab(final TextEditor textEditor) {
		try {
			// Create new tab with default empty content and no extension
			final TextEditorTab tab = textEditor.newTab("", "");

			// Get the editor and tab indices
			int editorIndex = TextEditor.instances.indexOf(textEditor);
			int tabIndex = TextEditorUtils.getTabIndex(textEditor, tab);

			return activeTabJson(tab, editorIndex, tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to create new tab");
		}
	}

	private void performRenameScript(final ScriptID scriptID, final String name,
		final String[] result)
	{
		try {
			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			String oldName = tab.getEditorPane().getName();
			textEditor.setEditorPaneFileName(new File(name));
			textEditor.stateChanged(new ChangeEvent(tab));

			JsonObject renameState = new JsonObject();
			renameState.addProperty(ScriptContextItem.SCRIPT_ID_KEY, scriptID.toString());
			renameState.addProperty("old_name", oldName);
			renameState.addProperty("new_name", name);
			result[0] = renameState.toString();
		}
		catch (Exception e) {
			result[0] = jsonError("Failed to rename tab at index " + scriptID.tabIndex);
		}
	}

    private String activeTabJson(TextEditorTab tab, int editorIndex, int tabIndex) {
		JsonElement tabJson = ScriptContextUtilities.getTabJson(tab, editorIndex, tabIndex);
		return jsonProp("active_script", tabJson);
    }

	private String jsonProp(String key, JsonElement element) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.add(key, element);
		return jsonObject.toString();

	}
}
