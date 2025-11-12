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
		return "Script Editor Tools";
	}

	@Override
	public String getUsage() {
		return "We use scripts to build reproducible workflows. These tools support script creation and editing.\n" +
			"BEFORE using any other Script Editor tool, use scriptGuide if it's not in your context.\n" +
			"To make a new script, use createScript.\n" +
			"To modify a script in your context, use editScriptContent.\n" +
			"To change the language or name of a script in your context, use renameScript.";
	}

	@Tool(value = {
		"Create a new script in the script editor.",
		"Args: scriptName - The name of the script with language extension (e.g., 'example.py', 'macro.ijm'); content - The source code for the script.",
		"Returns: JSON with script name and address for future editing"
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
			
			// Create new tab with the script content
			return createNewTab(textEditor, scriptName, cleanContent);
		} catch (Exception e) {
			return "ERROR: Failed to create script: " + e.getMessage();
		}
	}

	@Tool(value = {
		"Update an existing script with new content. REQUIRES: A \"Script\" entry in your context.",
		"Args: address - The script's address from context (e.g., [0:1]); content - New content for the script."
	})
	public String editScriptContent(@P("address") final String address, @P("content") final String content) {
		try {
			// Validate inputs
			if (address == null || address.trim().isEmpty()) {
				return "ERROR: Script address cannot be null or empty";
			}
			if (content == null) {
				return "ERROR: Script content cannot be null";
			}

			// Parse the address string
			final ScriptAddress scriptAddress = parseAddress(address);
			if (scriptAddress == null) {
				return "ERROR: Invalid address format. Expected [editorIndex:tabIndex] (e.g., [0:1])";
			}

			// Strip line numbers from content if present
			final String cleanContent = TextEditorUtils.stripLineNumbers(content);

			// Validate instance index
			if (scriptAddress.editorIndex < 0 || scriptAddress.editorIndex >= TextEditor.instances.size()) {
				return "ERROR: Invalid editor index " + scriptAddress.editorIndex;
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptAddress.editorIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at index " + scriptAddress.editorIndex;
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				try {
					// Validate tab index
					final TextEditorTab tab = textEditor.getTab(scriptAddress.tabIndex);

					if (tab == null) {
						result[0] = "ERROR: No tab found at index " + scriptAddress.tabIndex;
						return;
					}

					// Update the tab content
					final EditorPane editorPane = (EditorPane) tab.getEditorPane();
					editorPane.setText(cleanContent);

					// Switch to the updated tab
					textEditor.switchTo(scriptAddress.tabIndex);

					result[0] = "Successfully updated script at " + scriptAddress;
				} catch (Exception e) {
					result[0] = "ERROR: Failed to update tab at index " + scriptAddress.tabIndex + ": " + e.getMessage();
				}
			});
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to update script: " + e.getMessage();
		}
	}

	@Tool(value = {
		"Rename an existing script. Changing its extension will change its script language. REQUIRES: A \"Script\" entry in context.",
		"Args: address - The script's address from context (e.g., [0:1]); name - New script name with language extension (e.g., 'example.py', 'macro.ijm').",
	})
	public String renameScript(@P("address") final String address, @P("name") final String name) {
		try {
			// Validate inputs
			if (address == null || address.trim().isEmpty()) {
				return "ERROR: Script address cannot be null or empty";
			}
			if (name == null || name.trim().isEmpty()) {
				return "ERROR: Script name cannot be null or empty";
			}

			// Parse the address string
			final ScriptAddress scriptAddress = parseAddress(address);
			if (scriptAddress == null) {
				return "ERROR: Invalid address format. Expected [editorIndex:tabIndex] (e.g., [0:1])";
			}

			// Validate instance index
			if (scriptAddress.editorIndex < 0 || scriptAddress.editorIndex >= TextEditor.instances.size()) {
				return "ERROR: Invalid editor index " + scriptAddress.editorIndex;
			}

			final TextEditor textEditor = TextEditor.instances.get(scriptAddress.editorIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at index " + scriptAddress.editorIndex;
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				performRenameScript(textEditor, scriptAddress, name, result);
			} else {
				SwingUtilities.invokeAndWait(() -> {
					performRenameScript(textEditor, scriptAddress, name, result);
				});
			}
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to rename script: " + e.getMessage();
		}
	}

	private String createNewTab(final TextEditor textEditor, final String scriptName, final String content) {
		try {
			// Extract extension from scriptName
			final String extension;
			final int lastDot = scriptName.lastIndexOf('.');
			if (lastDot > 0 && lastDot < scriptName.length() - 1) {
				extension = scriptName.substring(lastDot + 1);
			} else {
				extension = "";
			}

			// Perform UI operations on EDT
			final String[] result = new String[1];
			if (SwingUtilities.isEventDispatchThread()) {
				result[0] = performCreateNewTab(textEditor, scriptName, extension, content);
			} else {
				SwingUtilities.invokeAndWait(() -> {
					result[0] = performCreateNewTab(textEditor, scriptName, extension, content);
				});
			}
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to create new tab: " + e.getMessage();
		}
	}

	private ScriptAddress parseAddress(final String addressString) {
		if (addressString == null || addressString.trim().isEmpty()) {
			return null;
		}
		try {
			// Expected format: "[editorIndex:tabIndex]"
			final String trimmed = addressString.trim();
			if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
				return null;
			}
			final String inner = trimmed.substring(1, trimmed.length() - 1);
			final String[] parts = inner.split(":");
			if (parts.length != 2) {
				return null;
			}
			final int editorIndex = Integer.parseInt(parts[0].trim());
			final int tabIndex = Integer.parseInt(parts[1].trim());
			return new ScriptAddress(editorIndex, tabIndex);
		} catch (Exception e) {
			return null;
		}
	}

	private String performCreateNewTab(final TextEditor textEditor, final String scriptName, final String extension, final String content) {
		try {
			// Create new tab - newTab() expects just the extension
			final TextEditorTab tab = textEditor.newTab(content, extension);

			// Set the filename using a File object - this will trigger language detection
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			editorPane.setFileName(new File(scriptName));

			// Get the editor and tab indices
			int editorIndex = TextEditor.instances.indexOf(textEditor);
			int tabIndex = findTabIndex(textEditor, tab);

			// Return formatted JSON response
			final StringBuilder sb = new StringBuilder();
			sb.append("{\n");
			sb.append("  \"type\": \"Script\",\n");
			sb.append("  \"name\": \"").append(scriptName).append("\",\n");
			sb.append("  \"address\": \"[").append(editorIndex).append(":").append(tabIndex).append("]\"\n");
			sb.append("}\n");
			return sb.toString();
		} catch (Exception e) {
			return "ERROR: Failed to create new tab: " + e.getMessage();
		}
	}

	private int findTabIndex(final TextEditor textEditor, final TextEditorTab targetTab) {
		for (int i = 0; ; i++) {
			try {
				final TextEditorTab currentTab = textEditor.getTab(i);
				if (currentTab == null) {
					break;
				}
				if (currentTab == targetTab) {
					return i;
				}
			} catch (Exception e) {
				break;
			}
		}
		return -1; // Not found
	}

	private void performRenameScript(final TextEditor textEditor, final ScriptAddress scriptAddress, final String name, final String[] result) {
		try {
			// Validate tab index
			final TextEditorTab tab = textEditor.getTab(scriptAddress.tabIndex);

			if (tab == null) {
				result[0] = "ERROR: No tab found at index " + scriptAddress.tabIndex;
				return;
			}

			// Update the filename
			final EditorPane editorPane = (EditorPane) tab.getEditorPane();
			editorPane.setFileName(new File(name));

			// Switch to the tab
			textEditor.switchTo(scriptAddress.tabIndex);

			result[0] = "Successfully renamed script at " + scriptAddress + " to " + name;
		} catch (Exception e) {
			result[0] = "ERROR: Failed to rename tab at index " + scriptAddress.tabIndex + ": " + e.getMessage();
		}
	}

	@Tool(value = {
		"Returns: A script syntax guide for YOU, the LLM."
	})
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
