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
 * AI tool that allows the LLM to interact with the Fiji script editor.
 * Provides capabilities to open the editor and create/update scripts.
 */
@Plugin(type = AiToolPlugin.class)
public class ScriptEditorTool implements AiToolPlugin {

	@Parameter
	private CommandService commandService;

	@Override
	public String getName() {
		return "Script Editor Toolset";
	}

	@Override
	public String getUsage() {
		return "Tools for interacting with the Fiji script editor. " +
			"BEFORE using any other script tool, IF scriptWritingGuide **is not** in your context, USE IT FIRST to load the guide. " +
			"Use createScript to make a new script. " +
			"Use updateScript to modify the content of an existing script. " +
			"Use renameScript to change the language of (or rename) an existing script.";
	}

	@Tool(name = "createScript", value = {
		"Create a new script in the Fiji script editor",
		"Args:",
		"	scriptName (arg0) - The name of the script file including extension (e.g., 'example.py', 'macro.ijm')",
		"	content (arg1) - The complete source code you wrote for the script. Do NOT wrap in markdown code fences (```). Provide raw source code only.",
		"Returns - Success message with script name, or ERROR message if creation failed"
	})
	public String createScript(@P("scriptName") final String scriptName, @P("content") final String content) {
		try {
			// Validate inputs
			if (scriptName == null || scriptName.trim().isEmpty()) {
				return "ERROR: Script name cannot be null or empty";
			}
			if (content == null) {
				return "ERROR: Script content cannot be null";
			}

			// Strip line numbers from content if present
			final String cleanContent = TextEditorUtils.stripLineNumbers(content);

			// Check if there's an open script editor instance
			TextEditor textEditor = TextEditorUtils.getMostRecentVisibleEditor();
			
			if (textEditor == null) {
				// Open a new editor, checking if we're already on EDT
				if (SwingUtilities.isEventDispatchThread()) {
					commandService.run(ScriptEditor.class, true);
				} else {
					SwingUtilities.invokeAndWait(() -> commandService.run(ScriptEditor.class, true));
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
					return "ERROR: Failed to open script editor";
				}
			}
			
			// Create new tab with the script content on EDT
			final TextEditor editor = textEditor;
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				result[0] = createNewTab(editor, scriptName, cleanContent);
			});
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to create script: " + e.getMessage();
		}
	}

	@Tool(name = "updateScript", value = {
		"Updates an existing open script with new content.",
		"Args:",
		"	instanceIndex (arg0) - The script editor instance index, from ScriptContextItem.",
		"	tabIndex (arg1) - The tab index within the editor instance, from ScriptContextItem.",
		"	content (arg2) - The new content for the indicated script. Do NOT wrap in markdown code fences (```). Provide raw source code only.",
		"Returns: Success message with indices, or ERROR message if update failed."
	})
	public String updateScript(@P("instanceIndex") final int instanceIndex, @P("tabIndex") final int tabIndex, @P("content") final String content) {
		try {
			// Validate content
			if (content == null) {
				return "ERROR: Script content cannot be null";
			}

			// Strip line numbers from content if present
			final String cleanContent = TextEditorUtils.stripLineNumbers(content);

			// Validate instance index
			if (instanceIndex < 0 || instanceIndex >= TextEditor.instances.size()) {
				return "ERROR: Invalid instance index " + instanceIndex;
			}
			
			final TextEditor textEditor = TextEditor.instances.get(instanceIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at instance index " + instanceIndex;
			}
			
			// Perform UI operations on EDT
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				try {
					// Validate tab index
					final TextEditorTab tab = textEditor.getTab(tabIndex);
					
					if (tab == null) {
						result[0] = "ERROR: No tab found at index " + tabIndex;
						return;
					}
					
					// Update the tab content
					final EditorPane editorPane = (EditorPane) tab.getEditorPane();
					editorPane.setText(cleanContent);

					// Switch to the updated tab
					textEditor.switchTo(tabIndex);
					
					result[0] = "Successfully updated script at instance " + instanceIndex + ", tab " + tabIndex;
				} catch (Exception e) {
					result[0] = "ERROR: Invalid tab index " + tabIndex + ": " + e.getMessage();
				}
			});
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to update script: " + e.getMessage();
		}
	}

	@Tool(name = "renameScript", value = {
		"Renames an existing open script. Changing its extension will change its script language.",
		"Args:",
		"	instanceIndex (arg0) - The script editor instance index, from ScriptContextItem.",
		"	tabIndex (arg1) - The tab index within the editor instance, from ScriptContextItem.",
		"	name (arg2) - New script name with extension (e.g., 'renamed.ijm') to change the script language.",
		"Returns: Success message with indices, or ERROR message if rename failed."
	})
	public String renameScript(@P("instanceIndex") final int instanceIndex, @P("tabIndex") final int tabIndex, @P("name") final String name) {
		try {
			// Validate instance index
			if (instanceIndex < 0 || instanceIndex >= TextEditor.instances.size()) {
				return "ERROR: Invalid instance index " + instanceIndex;
			}

			final TextEditor textEditor = TextEditor.instances.get(instanceIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at instance index " + instanceIndex;
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				try {
					// Validate tab index
					final TextEditorTab tab = textEditor.getTab(tabIndex);

					if (tab == null) {
						result[0] = "ERROR: No tab found at index " + tabIndex;
						return;
					}

					// Update the filename
					final EditorPane editorPane = (EditorPane) tab.getEditorPane();
					editorPane.setFileName(new File(name));

					// Switch to the tab
					textEditor.switchTo(tabIndex);

					result[0] = "Successfully renamed script at instance " + instanceIndex + ", tab " + tabIndex + " to " + name;
				} catch (Exception e) {
					result[0] = "ERROR: Failed to rename tab at index " + tabIndex + ": " + e.getMessage();
				}
			});
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to rename script: " + e.getMessage();
		}
	}

	private String createNewTab(final TextEditor textEditor, final String scriptName, final String content) {
		try {
			// Extract extension from scriptName
			String extension = "";
			final int lastDot = scriptName.lastIndexOf('.');
			if (lastDot > 0 && lastDot < scriptName.length() - 1) {
				extension = scriptName.substring(lastDot + 1);
			}

			// Create new tab - newTab() expects just the extension
			final TextEditorTab tab = textEditor.newTab(content, extension);

			// Set the filename using a File object - this will trigger language detection
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			editorPane.setFileName(new File(scriptName));

			return "Successfully created script: " + scriptName;
		} catch (Exception e) {
			return "ERROR: Failed to create new tab: " + e.getMessage();
		}
	}

@Tool(value = {
		"Returns: A script syntax guide for YOU, the LLM."
	})
	public String scriptWritingGuide() {
		return """
SciJava Scripting Guide
=======================

LANGUAGE DETECTION
------------------
Script language is detected based on file extension in the name.
Ensure the script name includes the correct extension (e.g., 'myScript.py', 'analysis.ijm').

RECOMMENDED LANGUAGES
----------------------
• .py - Python
• .ijm - ImageJ Macro Language
• .groovy - Groovy

ALSO SUPPORTED
--------------
• .js - JavaScript (Nashorn)
• .bsh - BeanShell
• .java - Java

@Parameters
============
• Turn scripts into parameterized SciJava commands, enabling use in other environments (e.g., headlessly).
• The best way to get input from users
• No restrictions on number of parameters

BASIC SYNTAX
------------
• ALL PARAMETER LINES MUST APPEAR FIRST IN THE FILE (even before imports!)
• ALWAYS WRITTEN AS LANGUAGE-SPECIFIC COMMENT LINE

In Python (# for comments):
#@ Type variableName (propKey=propVal,...) → Declare input variable, available for use in the script. Properties are optional.
#@output Type outputName → Declare output variable, which MUST BE DEFINED in the script. Type optional, default: Object.

SUPPORTED TYPES
---------------
• SCRIPT PARAMETERS ARE THE PREFERRED WAY TO GATHER INPUTS!
• If you need a variable of one of these types, you usually should use an @Parameter

Support automatic UI creation:
  • Dataset, ImagePlus, ImgPlus → Automatically use the active image. Selector created if multiple images open.
  • Boolean → Checkbox widget
  • Byte, Short, Long, Integer, Float, Double → Numeric input 
  • String → Text field or text area
  • Character → Single character input
  • File → File chooser widget (supports open/save/directory modes)
  • File[] → Multiple files/folders selector
  • Date → Date chooser widget
  • ColorRGB → Color chooser widget

Injected without UI:
  • SciJavaService implementations → e.g. UIService, CommandService

PARAMETER PROPERTIES (OPTIONAL)
-------------------------------
Universal:
•  label="Custom Label" → Alternative UI display name
•  description="Help text" → UI tooltip
•  value=defaultValue → Default value
•  persist=true|false → Remember last value (default: true)
•  required=true|false → Whether parameter must be satisfied (default: true)
•  visibility=NORMAL|TRANSIENT|INVISIBLE|MESSAGE
    • NORMAL: Included in history and macro recording
    • TRANSIENT: Excluded from history, included in recording
    • INVISIBLE: Excluded from both history and recording
    • MESSAGE: Read-only documentation message (forces required=false)

Numeric-specific:
• min=value, max=value → Numeric range
• stepSize=amount → Increment step (default: 1)
• style="slider" → Widget style (slider, spinner, scroll bar)
• style="format:#.##" → Decimal formatting

File-specific:
• style="file" → Single file open
• style="save" → File save dialog
• style="directory" → Directory selection
• style="files" → Multiple files (for File[])
• style="directories" → Multiple directories (for File[])
• style="both" → Multiple files or directories (for File[])

COMMON SERVICES
---------------
  • UIService → User interface actions (e.g., ui.show())
  • CommandService → Run SciJava commands/scripts (e.g., cs.run())
  • LogService → Logging output (e.g., log.info(), log.warn(), log.error())
  • StatusService → Update progress/status messages

OUTPUT HANDLING
---------------
The framework tries to display any outputs. Some have specific handlers:

• Dataset, ImagePlus → Displayed as image
• String → Printed as text
• numerics → Displayed in table

EXAMPLE SCRIPT: Invert_image.py
===============================
#@ ImagePlus img
#@output inverted

inverted = img.duplicate()
ip = inverted.getProcessor()

ip.invert()
inverted.updateAndDraw()

COMMON ERRORS
=============
• TypeError: 'org.scijava.plugin.PluginInfo' object is not callable → Runtime error during script execution
	""";
	}

}
