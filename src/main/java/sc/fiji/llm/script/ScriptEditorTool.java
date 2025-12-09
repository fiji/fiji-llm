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
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performSetActiveScript(textEditor, scriptID);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performSetActiveScript(textEditor, scriptID);
				});
			}
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

			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performReplaceScript(scriptID, tab, content);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performReplaceScript(scriptID, tab, content);
				});
			}
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
				result[0] = performRenameScript(scriptID, filename);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performRenameScript(scriptID, filename);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to rename active script");
		}
	}

	@Tool(value = { "Read the complete content of the active script." }, name = "fiji.script.read_script")
	public String readScript()
	{
		try {
			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String content = editorPane.getText();

			JsonObject readState = ScriptContextUtilities.getTabJson(tab, scriptID).getAsJsonObject();
			readState.addProperty(ScriptContextItem.CONTENT_KEY, content);
			return jsonProp("read_script", readState);
		}
		catch (Exception e) {
			return jsonError("Failed to read active script");
		}
	}

	@Tool(value = { "Read lines from the active script between the specified start and end lines (inclusive)" }, name = "fiji.script.read_lines")
	public String readLines(@P("start_line") final int startLine, @P("end_line") final int endLine)
	{
		try {
			// Validate line numbers
			if (startLine < 1 || endLine < 1 || startLine > endLine) {
				return jsonError("Invalid line range. Lines must be >= 1 and start_line <= end_line");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String content = editorPane.getText();
			final String[] lines = content.split("\n", -1);

			// Validate that requested lines exist
			if (startLine > lines.length) {
				return jsonError("start_line exceeds total number of lines (" + lines.length + ")");
			}

			// Extract the requested lines (1-indexed)
			final int actualEndLine = Math.min(endLine, lines.length);
			final StringBuilder extractedContent = new StringBuilder();
			for (int i = startLine - 1; i < actualEndLine; i++) {
				extractedContent.append(lines[i]);
				if (i < actualEndLine - 1) {
					extractedContent.append("\n");
				}
			}

			JsonObject readState = ScriptContextUtilities.getTabJson(tab, scriptID).getAsJsonObject();
			readState.addProperty("start_line", startLine);
			readState.addProperty("end_line", actualEndLine);
			readState.addProperty("content", extractedContent.toString());
			return jsonProp("read_lines", readState);
		}
		catch (Exception e) {
			return jsonError("Failed to read lines from active script");
		}
	}

	@Tool(value = { "Delete lines from the active script within a specified range." }, name = "fiji.script.delete_lines")
	public String deleteLines(@P("start_line") final int startLine, @P("end_line") final int endLine)
	{
		try {
			// Validate line numbers
			if (startLine < 1 || endLine < 1 || startLine > endLine) {
				return jsonError("Invalid line range. Lines must be >= 1 and start_line <= end_line");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performDeleteLines(tab, scriptID, startLine, endLine);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performDeleteLines(tab, scriptID, startLine, endLine);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to delete lines from active script");
		}
	}

	@Tool(value = { "Insert content at a specific line in the active script." }, name = "fiji.script.insert_at")
	public String insertAt(@P("content") final String content, @P("start_line") final int startLine)
	{
		try {
			// Validate content
			if (content == null) {
				return jsonError("Content cannot be null");
			}

			// Validate line number
			if (startLine < 1) {
				return jsonError("start_line must be >= 1");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performInsertAt(tab, scriptID, content, startLine);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performInsertAt(tab, scriptID, content, startLine);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to insert content into active script");
		}
	}

	@Tool(value = { "Replace lines in the active script within a specified range." }, name = "fiji.script.replace_lines")
	public String replaceLines(@P("new_content") final String newContent, @P("start_line") final int startLine, @P("end_line") final int endLine)
	{
		try {
			// Validate content
			if (newContent == null) {
				return jsonError("New content cannot be null");
			}

			// Validate line numbers
			if (startLine < 1 || endLine < 1 || startLine > endLine) {
				return jsonError("Invalid line range. Lines must be >= 1 and start_line <= end_line");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji.script.create_script");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performReplaceLines(tab, scriptID, newContent, startLine, endLine);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performReplaceLines(tab, scriptID, newContent, startLine, endLine);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to replace lines in active script");
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

	private String performSetActiveScript(final TextEditor textEditor, final ScriptID scriptID)
	{
		try {
			// Validate tab index
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			if (tab == null) {
				return jsonError("Invalid script_id. No script tab found at index " + scriptID.tabIndex);
			}

			// Switch to the specified tab
			textEditor.switchTo(scriptID.tabIndex);

			// Return indication of active tab
			return activeTabJson(tab, scriptID.editorIndex, scriptID.tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to activate script: " + scriptID.toString());
		}
	}

	private String performReplaceScript( ScriptID scriptID, TextEditorTab tab, String content )
	{
		// Update the tab content
		final EditorPane editorPane = (EditorPane) tab.getEditorPane();
		editorPane.setText(content);

		JsonElement updateScript = ScriptContextUtilities.getTabJson(tab, scriptID);
		return jsonProp("replaced_script_content", updateScript);
	}

	private String performDeleteLines(final TextEditorTab tab, final ScriptID scriptID, final int startLine, final int endLine)
	{
		try {
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String content = editorPane.getText();
			final String[] lines = content.split("\n", -1);

			// Validate that requested lines exist
			if (startLine > lines.length) {
				return jsonError("start_line exceeds total number of lines (" + lines.length + ")");
			}

			// Delete the specified lines (1-indexed)
			final int actualEndLine = Math.min(endLine, lines.length);
			final StringBuilder newContent = new StringBuilder();
			for (int i = 0; i < lines.length; i++) {
				if (i < startLine - 1 || i >= actualEndLine) {
					newContent.append(lines[i]);
					if (i < lines.length - 1) {
						newContent.append("\n");
					}
				}
			}

			editorPane.setText(newContent.toString());

			JsonObject deleteState = ScriptContextUtilities.getTabJson(tab, scriptID).getAsJsonObject();
			deleteState.addProperty("deleted_start_line", startLine);
			deleteState.addProperty("deleted_end_line", actualEndLine);
			deleteState.addProperty("total_lines_remaining", lines.length - (actualEndLine - startLine + 1));
			return jsonProp("deleted_lines", deleteState);
		}
		catch (Exception e) {
			return jsonError("Failed to delete lines from script");
		}
	}

	private String performInsertAt(final TextEditorTab tab, final ScriptID scriptID, final String content, final int startLine)
	{
		try {
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String currentContent = editorPane.getText();
			final String[] lines = currentContent.split("\n", -1);

			// Validate that insertion line is valid (allow inserting at end)
			if (startLine > lines.length + 1) {
				return jsonError("start_line exceeds total number of lines + 1 (" + (lines.length + 1) + ")");
			}

			// Build new content with insertion
			final StringBuilder newContent = new StringBuilder();
			for (int i = 0; i < lines.length; i++) {
				if (i == startLine - 1) {
					newContent.append(content);
					if (!content.endsWith("\n")) {
						newContent.append("\n");
					}
				}
				newContent.append(lines[i]);
				if (i < lines.length - 1) {
					newContent.append("\n");
				}
			}

			// Handle insertion at end of file
			if (startLine > lines.length) {
				if (newContent.length() > 0 && !currentContent.endsWith("\n")) {
					newContent.append("\n");
				}
				newContent.append(content);
			}

			editorPane.setText(newContent.toString());

			JsonObject insertState = ScriptContextUtilities.getTabJson(tab, scriptID).getAsJsonObject();
			insertState.addProperty("inserted_at_line", startLine);
			insertState.addProperty("new_total_lines", newContent.toString().split("\n", -1).length);
			return jsonProp("inserted_content", insertState);
		}
		catch (Exception e) {
			return jsonError("Failed to insert content into script");
		}
	}

	private String performReplaceLines(final TextEditorTab tab, final ScriptID scriptID, final String newContent, final int startLine, final int endLine)
	{
		try {
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String content = editorPane.getText();
			final String[] lines = content.split("\n", -1);

			// Validate that requested lines exist
			if (startLine > lines.length) {
				return jsonError("start_line exceeds total number of lines (" + lines.length + ")");
			}

			// Replace the specified lines (1-indexed)
			final int actualEndLine = Math.min(endLine, lines.length);
			final StringBuilder updatedContent = new StringBuilder();
			for (int i = 0; i < lines.length; i++) {
				if (i == startLine - 1) {
					updatedContent.append(newContent);
					if (!newContent.endsWith("\n")) {
						updatedContent.append("\n");
					}
					i = actualEndLine - 1; // Skip to after the replaced lines
				}
				else if (i > startLine - 1 && i <= actualEndLine - 1) {
					// Skip lines being replaced
					continue;
				}
				else {
					updatedContent.append(lines[i]);
					if (i < lines.length - 1) {
						updatedContent.append("\n");
					}
				}
			}

			editorPane.setText(updatedContent.toString());

			JsonObject replaceState = ScriptContextUtilities.getTabJson(tab, scriptID).getAsJsonObject();
			replaceState.addProperty("replaced_start_line", startLine);
			replaceState.addProperty("replaced_end_line", actualEndLine);
			replaceState.addProperty("new_total_lines", updatedContent.toString().split("\n", -1).length);
			return jsonProp("replaced_lines", replaceState);
		}
		catch (Exception e) {
			return jsonError("Failed to replace lines in script");
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

	private String performRenameScript(final ScriptID scriptID, final String name)
	{
		final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
		final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

		String oldName = tab.getEditorPane().getName();
		textEditor.setEditorPaneFileName(new File(name));
		textEditor.stateChanged(new ChangeEvent(tab));

		JsonObject renameState = new JsonObject();
		renameState.addProperty(ScriptContextItem.SCRIPT_ID_KEY, scriptID.toString());
		renameState.addProperty("old_name", oldName);
		renameState.addProperty("new_name", name);
		return jsonProp("renamed_script", renameState);
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
