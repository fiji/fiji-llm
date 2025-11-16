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

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.ScriptEditor;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AiToolPlugin;
import sc.fiji.llm.ui.TextEditorUtils;

/**
 * AI tool that allows the LLM to interact with the Fiji script editor. Provides
 * capabilities to open the editor and create/update scripts.
 */
@Plugin(type = AiToolPlugin.class)
public class ScriptEditorTool implements AiToolPlugin {

	@Parameter
	private CommandService commandService;

	@Override
	public String getName() {
		return "Script Editor Tools";
	}

	@Override
	public String getUsage() {
		return "Scripts are runnable programs, used to make reproducible workflows. These tools facilitate script creation and editing.\n" +
			"Scripts are referenced by id, which may be provided by the user or returned by tool calls.\n" +
			"A script's file extension determines its language (e.g., .py, .ijm, .groovy).\n" +
			"BEFORE using any other Script Editor tool, use scriptGuide if it's not in your context.\n" +
			"USAGE:\n" +
			"• When you have a script id to edit: set the content using writeScript, set the display name and language extension using setScriptFilename.\n" +
			"• When you want to make a new script id: first check isEditorOpen, then if false use startEditor, or if true use createScript.";
	}

	@Tool(value = { "Check if the script editor is open.",
		"Returns: true if is open, false otherwise" })
	public boolean isEditorOpen() {
		return TextEditorUtils.getMostRecentVisibleEditor() != null;
	}

	@Tool(value = { "Start the script editor if it's not currently open",
		"Returns: Info with id for the default script" })
	public String startEditor() {
		try {
			// Check if editor is already open
			if (isEditorOpen()) {
				return "ERROR: Script editor is already open. Use createScript instead.";
			}

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
			TextEditor textEditor = null;
			while (System.currentTimeMillis() - startTime < timeoutMs) {
				textEditor = TextEditorUtils.getMostRecentVisibleEditor();
				if (textEditor != null) {
					break;
				}
				Thread.sleep(100);
			}

			if (textEditor == null) {
				return "ERROR: Failed to open script editor";
			}

			// Get the editor and default tab indices
			int editorIndex = TextEditor.instances.indexOf(textEditor);
			int tabIndex = 0; // The default tab is at index 0

			// Return formatted JSON response
			return getTabJson(textEditor.getTab(tabIndex), editorIndex, tabIndex);
		}
		catch (Exception e) {
			return "ERROR: Failed to start script editor: " + e.getMessage();
		}
	}

	@Tool(value = {
		"Create a new script tab in the script editor. REQUIRES: Script editor must already be open (use startEditor if needed).",
		"Returns: Script info with id of the new tab" })
	public String createScript() {
		try {
			// Check if editor is open
			final TextEditor textEditor = TextEditorUtils
				.getMostRecentVisibleEditor();
			if (textEditor == null) {
				return "ERROR: Script editor is not open. Use startEditor to open it first.";
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
			return "ERROR: Failed to create script: " + e.getMessage();
		}
	}

	@Tool(value = {
		"Update an existing script with new content. REQUIRES: A \"Script\" entry in your context.",
		"Args: id - The script's id from context (e.g., 0:1); content - New content for the script." })
	public String writeScript(@P("id") final String id,
		@P("content") final String content)
	{
		try {
			// Validate inputs
			if (id == null || id.trim().isEmpty()) {
				return "ERROR: Script id cannot be null or empty";
			}
			if (content == null) {
				return "ERROR: Script content cannot be null";
			}

			// Parse the id string
			final ScriptID scriptID = parseID(id);
			if (scriptID == null) {
				return "ERROR: Invalid id format. Expected e.g., 0:1";
			}

			// Strip line numbers from content if present
			final String cleanContent = TextEditorUtils.stripLineNumbers(content);

			// Validate instance index
			if (scriptID.editorIndex < 0 ||
				scriptID.editorIndex >= TextEditor.instances.size())
			{
				return "ERROR: Invalid editor index " + scriptID.editorIndex;
			}

			final TextEditor textEditor = TextEditor.instances.get(
				scriptID.editorIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at index " + scriptID.editorIndex;
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				try {
					// Validate tab index
					final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

					if (tab == null) {
						result[0] = "ERROR: No tab found at index " + scriptID.tabIndex;
						return;
					}

					// Update the tab content
					final EditorPane editorPane = (EditorPane) tab.getEditorPane();
					editorPane.setText(cleanContent);

					// Switch to the updated tab
					textEditor.switchTo(scriptID.tabIndex);

					result[0] = "Successfully updated script at " + scriptID;
				}
				catch (Exception e) {
					result[0] = "ERROR: Failed to update tab at index " +
						scriptID.tabIndex + ": " + e.getMessage();
				}
			});
			return result[0];
		}
		catch (Exception e) {
			return "ERROR: Failed to update script: " + e.getMessage();
		}
	}

	@Tool(value = {
		"Rename an existing script. Changing its extension will change its script language. REQUIRES: A \"Script\" entry in context.",
		"Args: id - The script's id from context (e.g., 0:1); filename - New script name with language extension (e.g., 'example.py', 'macro.ijm').", })
	public String setScriptFilename(@P("id") final String id,
		@P("filename") final String filename)
	{
		try {
			// Validate inputs
			if (id == null || id.trim().isEmpty()) {
				return "ERROR: Script id cannot be null or empty";
			}
			if (filename == null || filename.trim().isEmpty()) {
				return "ERROR: Script name cannot be null or empty";
			}

			// Parse the id string
			final ScriptID scriptID = parseID(id);
			if (scriptID == null) {
				return "ERROR: Invalid id format. Expected e.g., 0:1";
			}

			// Validate instance index
			if (scriptID.editorIndex < 0 ||
				scriptID.editorIndex >= TextEditor.instances.size())
			{
				return "ERROR: Invalid editor index " + scriptID.editorIndex;
			}

			final TextEditor textEditor = TextEditor.instances.get(
				scriptID.editorIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at index " + scriptID.editorIndex;
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				performRenameScript(textEditor, scriptID, filename, result);
			}
			else {
				SwingUtilities.invokeAndWait(() -> {
					performRenameScript(textEditor, scriptID, filename, result);
				});
			}
			return result[0];
		}
		catch (Exception e) {
			return "ERROR: Failed to rename script: " + e.getMessage();
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
			int tabIndex = findTabIndex(textEditor, tab);

			return getTabJson(tab, editorIndex, tabIndex);
		}
		catch (Exception e) {
			return "ERROR: Failed to create new tab: " + e.getMessage();
		}
	}

	private String getTabJson(TextEditorTab tab, int editorIndex, int tabIndex) {
		// Return formatted JSON response
		final StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"type\": \"Script\",\n");
		sb.append("  \"name\": \"").append(tab.getName()).append("\",\n");
		sb.append("  \"id\": \"").append(editorIndex).append(":").append(tabIndex)
			.append("\"\n");
		sb.append("}\n");
		return sb.toString();
	}

	private int findTabIndex(final TextEditor textEditor,
		final TextEditorTab targetTab)
	{
		for (int i = 0;; i++) {
			try {
				final TextEditorTab currentTab = textEditor.getTab(i);
				if (currentTab == null) {
					break;
				}
				if (currentTab == targetTab) {
					return i;
				}
			}
			catch (Exception e) {
				break;
			}
		}
		return -1; // Not found
	}

	private void performRenameScript(final TextEditor textEditor,
		final ScriptID scriptID, final String name, final String[] result)
	{
		try {
			// Validate tab index
			final TextEditorTab tab = textEditor.getTab(scriptID.tabIndex);

			if (tab == null) {
				result[0] = "ERROR: No tab found at index " + scriptID.tabIndex;
				return;
			}

			// Update the filename
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			editorPane.setFileName(new File(name));

			// Switch to the tab
			textEditor.switchTo(scriptID.tabIndex);

			result[0] = "Successfully renamed script at " + scriptID + " to " + name;
		}
		catch (Exception e) {
			result[0] = "ERROR: Failed to rename tab at index " + scriptID.tabIndex +
				": " + e.getMessage();
		}
	}

	@Tool(value = { "Returns: A script syntax guide for YOU, the LLM." })
	public String scriptGuide() {
		return """
				SciJava Scripting Guide
				=======================

				LANGUAGES (by extension)
				-------------------------
				Recommended: .py (Python), .ijm (ImageJ Macro), .groovy (Groovy)
				Also: .js (JavaScript), .bsh (BeanShell), .java (Java)

				NOTE: ImageJ Macros (.ijm) are typically created using the Macro Recorder.
				Use available ImageJ Macro Tools to start, then edit the resulting script as needed.

				@ PARAMETERS
				------------
				• A special magic syntax for script inputs and outputs
				• ALL PARAMETER LINES MUST APPEAR FIRST (even before imports!)
				• WRITTEN AS COMMENTS (e.g., # in Python, // in Groovy)
				• Inputs Parameters are created automatically
				* Output Parameters must be defined in the script.


				Input: # @Type variableName (property=value, ...)
				Output: #@output Type outputName

				PARAMETER TYPES
				---------------
				UI-enabled (automatic widgets):
				  • Dataset, ImagePlus, ImgPlus → Image selector (uses active image)
				  • Boolean → Checkbox
				  • Byte, Short, Long, Integer, Float, Double → Numeric input
				  • String → Text field
				  • Character → Single character
				  • File, File[] → File/folder chooser
				  • Date → Date picker
				  • ColorRGB → Color picker

				Injected (no UI): SciJavaServices (UIService, CommandService, etc.)

				OPTIONAL PROPERTIES
				-------------------
				• label="text" → UI display name
				• description="text" → Tooltip
				• value=X → Default value
				• required=true|false → Optional parameter
				• min=X, max=X, stepSize=X → Numeric constraints
				• style="slider|file|save|directory|files|directories" → Widget/mode
				• visibility=NORMAL|TRANSIENT|INVISIBLE|MESSAGE

				EXAMPLE
				-------
				# @ImagePlus img
				#@output inverted

				inverted = img.duplicate()
				inverted.getProcessor().invert()
				inverted.updateAndDraw()
					""";
	}
}
