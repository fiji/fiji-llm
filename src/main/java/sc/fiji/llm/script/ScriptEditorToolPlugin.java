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

package sc.fiji.llm.script;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.ScriptEditor;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.log.TextLogs;
import sc.fiji.llm.tools.AbstractAiToolPlugin;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.tools.ToolScope;
import sc.fiji.llm.ui.TextEditorUtils;

/**
 * AI tool collection that allows the LLM to interact with the Fiji script editor.
 * Provides capabilities to open the editor and create/update scripts.
 */
@Plugin(type = AiToolPlugin.class)
public class ScriptEditorToolPlugin extends AbstractAiToolPlugin {

	private static final String IS_ACTIVE_KEY = "is_active";
	private static final String ERROR_KEY = "errors";
	private static final String OUTPUT_KEY = "output";

	@Parameter
	private CommandService commandService;

	@Parameter
	private ScriptService scriptService;

	@Parameter
	private ScriptExecutionService scriptExecutionService;

	@Override
	public String getName() {
		return "Script Editor Tools";
	}

	public ScriptEditorToolPlugin() {
		super(ScriptEditorToolPlugin.class);
	}

	@Override
	public String getToolScope() {
		return ToolScope.SCRIPT;
	}

	@Tool(value = { "Open a script editor UI with an active blank script, if no editor is currently open; no-op if an editor is open" }, name = "fiji_script_open_editor")
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
					return jsonError("Failed to run fiji_script_open_editor");
				}
			}

			// Get the editor and default tab indices
			int editorIndex = TextEditor.instances.indexOf(textEditor);
			int tabIndex = 0; // Default to the first tab 0

			// Return indication of active tab
			return activeScriptString(editorIndex, tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_open_editor: " + e.getMessage());
		}
	}

	@Tool(value = { "Use this tool first, as needed, to set the active script by its script_id" }, name = "fiji_script_activate")
	public String setActiveScript(@P(name = "script_id", value = "ID of the script to activate") final String scriptId)
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

			// Validate editor index
			if (scriptID.editorIndex < 0 ||
				scriptID.editorIndex >= TextEditor.instances.size())
			{
				return jsonError("Invalid script_id. No editor found at index: " + scriptID.editorIndex, "fiji_script_list");
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
			return jsonError("Failed to run fiji_script_activate: " + e.getMessage());
		}
	}

	private String performSetActiveScript(final TextEditor textEditor, final ScriptID scriptID)
	{
		try {
			TextEditorTab tab = null;

			try {
				// Validate tab index
				tab = textEditor.getTab(scriptID.tabIndex);
			} catch (ArrayIndexOutOfBoundsException e) {
				// No-op, handled below
			}

			if (tab == null) {
				return jsonError("Invalid script_id. No script found at index " + scriptID.tabIndex, "fiji_script_list");
			}

			// Switch to the specified tab
			textEditor.switchTo(scriptID.tabIndex);
			TextEditorUtils.recordLastFocusedEditor(textEditor);
			textEditor.toFront();
			textEditor.requestFocus();

			// Return indication of active tab
			return activeScriptString(scriptID.editorIndex, scriptID.tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to perform fiji_script_activate: " + e.getMessage());
		}
	}

	@Tool(value = { "List all scripts in visible Script Editor windows, including their script_id values, grouped by editor" }, name = "fiji_script_list")
	public String listOpenScripts() {
		try {
			JsonArray editors = new JsonArray();
			List<TextEditor> instances = TextEditor.instances;
			if (instances != null) {
				final ScriptID activeScriptID = TextEditorUtils.getActiveScriptID();

				for (int editorIndex = 0; editorIndex < instances.size(); editorIndex++) {
					TextEditor textEditor = instances.get(editorIndex);
					if (!textEditor.isVisible()) continue;
					JsonObject editorJson = new JsonObject();
					editorJson.addProperty("editor_id", editorIndex);
					JsonArray tabs = new JsonArray();
					int tabIndex = 0;
					try {
						while (true) {
							JsonObject tabJson = getTabJson(editorIndex, tabIndex);
							tabJson.addProperty(IS_ACTIVE_KEY, activeScriptID != null && editorIndex == activeScriptID.editorIndex && tabIndex == activeScriptID.tabIndex);
							tabs.add(tabJson);
							tabIndex++;
						}
					}
					catch (IndexOutOfBoundsException e) {
						// all tabs collected
					}
					editorJson.add("scripts", tabs);

					editorJson.addProperty(IS_ACTIVE_KEY, activeScriptID != null && editorIndex == activeScriptID.editorIndex);

					editors.add(editorJson);
				}
			}
			JsonObject result = new JsonObject();
			result.add("editors", editors);
			return result.toString();
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_list: " + e.getMessage());
		}
	}

	@Tool(value = { "Return all available script languages and their file extensions" }, name = "fiji_script_list_languages")
	public String listScriptLanguages() {
		try {
			final JsonArray languages = new JsonArray();
			for (final ScriptLanguage language : scriptService.getLanguages()) {
				final JsonObject languageJson = new JsonObject();
				languageJson.addProperty("name", language.getLanguageName());

				final JsonArray extensions = new JsonArray();
				for (final String extension : language.getExtensions()) {
					extensions.add(extension);
				}
				languageJson.add("extensions", extensions);
				languages.add(languageJson);
			}

			final JsonObject result = new JsonObject();
			result.add("languages", languages);
			return result.toString();
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_list_languages: " + e
				.getMessage());
		}
	}

	@Tool(value = { "Create and activate a new blank script; no-op if an unmodified blank script is currently active" }, name = "fiji_script_create")
	public String createScript() {
		try {
			// Check if editor is open
			final TextEditor textEditor = TextEditorUtils
				.getLastFocusedVisibleEditor();
			if (textEditor == null) {
				return jsonError("Script editor is not open", "fiji_script_open_editor");
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
			return jsonError("Failed to run fiji_script_create: " + e.getMessage());
		}
	}

	private String performCreateNewTab(final TextEditor textEditor) {
		try {
			// Create new tab with default empty content and no extension
			final TextEditorTab tab = textEditor.newTab("", "");
			TextEditorUtils.recordLastFocusedEditor(textEditor);

			// Get the editor and tab indices
			int editorIndex = TextEditor.instances.indexOf(textEditor);
			int tabIndex = TextEditorUtils.getTabIndex(textEditor, tab);

			return activeScriptString(editorIndex, tabIndex);
		}
		catch (Exception e) {
			return jsonError("Failed to perform fiji_script_create: " + e
				.getMessage());
		}
	}

	@Tool(value = { "Start the active non-macro script through the visible Script Editor and return per-run output, errors, console stdout/stderr, and ImageJ/SciJava logs. This call returns when the script finishes or pauses on a new modal dialog. Use the run_id with fiji_script_run_status to poll, then fiji_ui_dialog_respond to continue. Timeouts are requested, but not guaranteed, if runtime exceeds 30 seconds" }, name = "fiji_script_run")
	public String runScript() {
		try {
			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			final String scriptName = ScriptContextUtilities.buildScriptContextItem(
				scriptID.editorIndex, scriptID.tabIndex).getScriptName();
			if (ScriptExecutionService.isMacroScript(scriptName)) {
				return jsonError("The active script is an ImageJ macro (.ijm) and must be run with fiji_macro_run",
					"fiji_macro_run");
			}

			final ScriptExecutionService.ExecutionResult execution = scriptExecutionService
				.run(scriptID, ScriptExecutionService.RunKind.SCRIPT, true);
			return stringProp("ran_script", execution.toJson());
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_run: " + e.getMessage());
		}
	}

	@Tool(value = { "Poll an asynchronous non-macro script run by run_id. This action is read-only, returning its status and current per-run output, errors, logs, and any blocking dialog. Use fiji_ui_dialog_respond with the exact title and button, then poll again." }, name = "fiji_script_run_status")
	public String scriptRunStatus(@P(name = "run_id", value = "Run ID returned when the run was started") final String runID) {
		if (runID == null || runID.isBlank()) {
			return jsonError("run_id cannot be null or blank");
		}

		try {
			final ScriptExecutionService.ExecutionResult execution = scriptExecutionService
				.status(runID, ScriptExecutionService.RunKind.SCRIPT);
			if (execution == null) {
				return jsonError("No script run found for run_id: " + runID);
			}
			return execution.toJson().toString();
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_run_status: " + e.getMessage());
		}
	}

	@Tool(value = { "Rename the active script. A script's programming language is determined by its name ending in a recognized extension (e.g., .py, .ijm, .groovy). Changing a script's extension will change its language" },
		name = "fiji_script_rename")
	public String renameScript(@P(name = "script_name", value = "New script name, including its file extension") final String scriptName)
	{
		try {
			// Validate filename
			if (scriptName == null || scriptName.isEmpty()) {
				return jsonError("New script_name cannot be null or empty");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performRenameScript(scriptID, scriptName);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performRenameScript(scriptID, scriptName);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_rename: " + e.getMessage());
		}
	}

	private String performRenameScript(final ScriptID scriptID, final String name)
	{
		try {
			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			String oldName = ScriptContextUtilities.buildScriptContextItem(scriptID.editorIndex, scriptID.tabIndex).getScriptName();
			textEditor.setEditorPaneFileName(new File(name));
			textEditor.stateChanged(new ChangeEvent(tab));

			JsonObject renameState = new JsonObject();
			renameState.addProperty(ScriptContextItem.SCRIPT_ID_KEY, scriptID.toString());
			renameState.addProperty("old_name", oldName);
			renameState.addProperty("new_name", name);
			return stringProp("renamed_script", renameState);
		}
		catch (Exception e) {
			return jsonError("Failed to perform fiji_script_rename: " + e
				.getMessage());
		}
	}

	@Tool(value = { "Return the content of the active script" }, name = "fiji_script_read_content")
	public String readScript() {
		try {
			ScriptContextItem scriptContext = ScriptContextUtilities.getActiveScriptContext();
			if (scriptContext == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			JsonObject readScript = getTabJson(scriptContext.getEditorIndex(), scriptContext.getTabIndex());
			readScript.addProperty(ScriptContextItem.CONTENT_KEY, scriptContext.getScriptBody());
			return stringProp("read_content", readScript);
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_read_content: " + e.getMessage());
		}
	}

	@Tool(value = { "Return lines from the active script between (inclusive) the given start_line and end_line indices (1-indexed)" }, name = "fiji_script_read_lines")
	public String readLines(@P(name = "start_line", value = "First line, 1-indexed and inclusive") final int startLine, @P(name = "end_line", value = "Last line, 1-indexed and inclusive") final int endLine)
	{
		try {
			// Validate line numbers
			if (startLine < 1 || endLine < 1 || startLine > endLine) {
				return jsonError("Invalid line range. Lines must be >= 1 and start_line <= end_line");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

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
			}

			JsonObject readState = getTabJson(scriptID);
			readState.addProperty("start_line", startLine);
			readState.addProperty("end_line", actualEndLine);
			readState.addProperty("content", extractedContent.toString());
			return stringProp("read_lines", readState);
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_read_lines: " + e.getMessage());
		}
	}

	@Tool(value = { "Return the cumulative output and error logs retained by the active Script Editor across runs" }, name = "fiji_script_read_logs")
	public String readLogs() {
		try {
			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);
			final TextLogs[] logs = new TextLogs[1];
			final Runnable readLogs = () -> logs[0] = TextEditorUtils.getLogs(textEditor, tab);
			if (SwingUtilities.isEventDispatchThread()) {
				readLogs.run();
			}
			else {
				SwingUtilities.invokeAndWait(readLogs);
			}

			final TextLogs cleanedLogs = logs[0].withoutStartedBanners();
			JsonObject scriptLog = getTabJson(scriptID);
			scriptLog.addProperty(ERROR_KEY, cleanedLogs.getErrors());
			scriptLog.addProperty(OUTPUT_KEY, cleanedLogs.getOutput());
			return stringProp("read_logs", scriptLog);
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_read_logs: " + e.getMessage());
		}
	}

	@Tool(value = { "Delete lines from the active script between (inclusive) the given start_line and end_line indices (1-indexed)" }, name = "fiji_script_delete_lines")
	public String deleteLines(@P(name = "start_line", value = "First line, 1-indexed and inclusive") final Integer startLine, @P(name = "end_line", value = "Last line, 1-indexed and inclusive") final Integer endLine)
	{
		try {
			// Validate line numbers
			if (startLine < 1 || endLine < 1 || startLine > endLine) {
				return jsonError("Invalid line range. Lines must be >= 1 and start_line <= end_line");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
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
			return jsonError("Failed to run fiji_script_delete_lines: " + e.getMessage());
		}
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
			final StringJoiner newContent = new StringJoiner("\n");
			for (int i = 0; i < lines.length; i++) {
				if (i < startLine - 1 || i >= actualEndLine) {
					newContent.add(lines[i]);
				}
			}

			editorPane.setText(newContent.toString());

			JsonObject deleteState = getTabJson(scriptID);
			deleteState.addProperty("deleted_start_line", startLine);
			deleteState.addProperty("deleted_end_line", actualEndLine);
			deleteState.addProperty("total_lines_remaining", lines.length - (actualEndLine - startLine + 1));
			return stringProp("deleted_lines", deleteState);
		}
		catch (Exception e) {
			return jsonError("Failed to perform fiji_script_delete_lines: " + e.getMessage());
		}
	}

	@Tool(value = { "Insert content in the active script before the specified line number (1-indexed)" }, name = "fiji_script_insert_content")
	public String insertAt(@P(name = "content", value = "Content to insert") final String content, @P(name = "before_line", value = "Line number, 1-indexed, before which to insert") final Integer beforeLine)
	{
		try {
			// Validate content
			if (content == null) {
				return jsonError("Content cannot be null");
			}

			// Validate line number
			if (beforeLine < 1) {
				return jsonError("before_line must be >= 1");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performInsert(tab, scriptID, content, beforeLine);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performInsert(tab, scriptID, content, beforeLine);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_insert_content: " + e.getMessage());
		}
	}

	private String performInsert(final TextEditorTab tab, final ScriptID scriptID, final String content, final int beforeLine)
	{
		try {
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String[] existingLines = editorPane.getText().split("\n", -1);
			final String[] newLines = content.split("\n", -1);

			StringJoiner newContent = new StringJoiner("\n");

			// Validate that insertion line is valid (allow inserting at end)
			if (beforeLine > existingLines.length + 1) {
				return jsonError("before_line exceeds total number of lines + 1 (" + (existingLines.length + 1) + ")");
			}

			for (int i=0; i<existingLines.length; i++) {
				if (i+1 == beforeLine) {
					for (int j=0; j<newLines.length; j++) {
						newContent.add(newLines[j]);
					}
				}
				newContent.add(existingLines[i]);
			}
			if (beforeLine > existingLines.length) {
				for (final String line : newLines) {
					newContent.add(line);
				}
			}

			editorPane.setText(newContent.toString());

			JsonObject insertState = getTabJson(scriptID);
			insertState.addProperty("inserted_before_line", beforeLine);
			insertState.addProperty("new_total_lines", newLines.length + existingLines.length);
			return stringProp("inserted_content", insertState);
		}
		catch (Exception e) {
			return jsonError("Failed to perform fiji_script_insert_content: " + e.getMessage());
		}
	}

	@Tool(value = { "Completely replace the content of the active script" }, name = "fiji_script_replace_content")
	public String replaceScript(@P(name = "content", value = "New script content") final String content)
	{
		try {
			// Validate content
			if (content == null) {
				return jsonError("New script content cannot be null");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performReplaceScript(tab, scriptID, content);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performReplaceScript(tab, scriptID, content);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_replace_content: " + e.getMessage());
		}
	}

	private String performReplaceScript(TextEditorTab tab, ScriptID scriptID, String content )
	{
		int oldLines =
			ScriptContextUtilities.buildScriptContextItem(scriptID.editorIndex,
			scriptID.tabIndex).getScriptBody().split("\n", -1).length;

		// Update the tab content
		final EditorPane editorPane = (EditorPane) tab.getEditorPane();
		editorPane.setText(content);

		JsonObject replaceState = getTabJson(scriptID);
		replaceState.addProperty("old_total_lines", oldLines);
		replaceState.addProperty("new_total_lines", content.split("\n", -1).length);
		return stringProp("replaced_content", replaceState);
	}

	@Tool(value = { "Each line in new_content replaces a line in the active script, beginning at start_line (1-indexed), adding lines to extend the script if needed" }, name = "fiji_script_replace_lines")
	public String replaceLines(@P(name = "new_content", value = "Replacement lines") final String newContent, @P(name = "start_line", value = "First line to replace, 1-indexed") final Integer startLine)
	{
		try {
			// Validate content
			if (newContent == null) {
				return jsonError("New content cannot be null");
			}

			// Validate line number
			if (startLine < 1) {
				return jsonError("start_line must be >= 1");
			}

			final ScriptID scriptID = TextEditorUtils.getActiveScriptID();
			if (scriptID == null) {
				return jsonError("No active script found", "fiji_script_create");
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptID.editorIndex);
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performReplaceLines(tab, scriptID, newContent, startLine);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performReplaceLines(tab, scriptID, newContent, startLine);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return jsonError("Failed to run fiji_script_replace_lines: " + e.getMessage());
		}
	}

	private String performReplaceLines(final TextEditorTab tab, final ScriptID scriptID, final String newContent, final int startLine)
	{
		try {
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			final String[] existingLines = editorPane.getText().split("\n", -1);

			// Validate that requested lines exist
			if (startLine > existingLines.length) {
				return jsonError("start_line exceeds total number of lines (" + existingLines.length + ")");
			}

			final String[] newLines = newContent.split("\n", -1);
			final int actualEndLine = Math.min(startLine + newLines.length - 1, existingLines.length);

			final StringJoiner updatedContent = new StringJoiner("\n");
			for (int i = 0; i < startLine - 1; i++) {
				updatedContent.add(existingLines[i]);
			}
			for (final String line : newLines) {
				updatedContent.add(line);
			}
			for (int i = actualEndLine; i < existingLines.length; i++) {
				updatedContent.add(existingLines[i]);
			}

			editorPane.setText(updatedContent.toString());

			JsonObject replaceState = getTabJson(scriptID);
			replaceState.addProperty("replace_start_line", startLine);
			replaceState.addProperty("new_total_lines", updatedContent.toString().split("\n", -1).length);
			return stringProp("replaced_lines", replaceState);
		}
		catch (Exception e) {
			return jsonError("Failed to perform fiji_script_replace_lines: " + e.getMessage());
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

	private JsonObject getTabJson(ScriptID scriptID) {
		return getTabJson(scriptID.editorIndex, scriptID.tabIndex);
	}

	private JsonObject getTabJson(int editorIndex, int tabIndex) {
		ScriptContextItem scriptContext = ScriptContextUtilities.buildScriptContextItem(editorIndex, tabIndex);
        JsonObject tabJson = new JsonObject();
        tabJson.addProperty(ScriptContextItem.NAME_KEY, scriptContext.getScriptName());
        tabJson.addProperty(ScriptContextItem.SCRIPT_ID_KEY, scriptContext.getId().toString());
		return tabJson;
	}

	private String activeScriptString(int editorIndex, int tabIndex) {
		JsonObject activeTabJson = getTabJson(editorIndex, tabIndex);
		activeTabJson.addProperty(IS_ACTIVE_KEY, true);
		return activeTabJson.toString();
	}

}
