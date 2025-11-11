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
			"To modify an existing script, use updateScript.\n" +
			"To change the language or name of an existing script, use renameScript.";
	}

	@Tool(value = {
		"Create a new script in the script editor.",
		"Args: scriptName - The name of the script with language extension (e.g., 'example.py', 'macro.ijm'); content - The source code for the script.",
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
		"Updates an existing script with new content.",
		"Args: editorIndex - The target script's instance index; tabIndex - The target script's tab index; content - The new content for the script."
	})
	public String updateScript(@P("editorIndex") final int editorIndex, @P("tabIndex") final int tabIndex, @P("content") final String content) {
		try {
			// Validate content
			if (content == null) {
				return "ERROR: Script content cannot be null";
			}

			// Strip line numbers from content if present
			final String cleanContent = TextEditorUtils.stripLineNumbers(content);

			// Validate instance index
			if (editorIndex < 0 || editorIndex >= TextEditor.instances.size()) {
				return "ERROR: Invalid instance index " + editorIndex;
			}
			
			final TextEditor textEditor = TextEditor.instances.get(editorIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at instance index " + editorIndex;
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
					
					result[0] = "Successfully updated script at instance " + editorIndex + ", tab " + tabIndex;
				} catch (Exception e) {
					result[0] = "ERROR: Invalid tab index " + tabIndex + ": " + e.getMessage();
				}
			});
			return result[0];
		} catch (Exception e) {
			return "ERROR: Failed to update script: " + e.getMessage();
		}
	}

	@Tool(value = {
		"Renames an existing script. Changing its extension will change its script language.",
		"Args: editorIndex - The target script's instance index; tabIndex - The target script's tab index; name - New script name with language extension (e.g., 'example.py', 'macro.ijm').",
	})
	public String renameScript(@P("editorIndex") final int editorIndex, @P("tabIndex") final int tabIndex, @P("name") final String name) {
		try {
			// Validate instance index
			if (editorIndex < 0 || editorIndex >= TextEditor.instances.size()) {
				return "ERROR: Invalid instance index " + editorIndex;
			}

			final TextEditor textEditor = TextEditor.instances.get(editorIndex);
			if (textEditor == null) {
				return "ERROR: No editor found at instance index " + editorIndex;
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

					result[0] = "Successfully renamed script at instance " + editorIndex + ", tab " + tabIndex + " to " + name;
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

	private String performCreateNewTab(final TextEditor textEditor, final String scriptName, final String extension, final String content) {
		try {
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

@PARAMETER SYNTAX
-----------------
• PREFERRED way to get user inputs and create parameterized commands
• ALL PARAMETER LINES MUST APPEAR FIRST (even before imports!)
• Written as language-specific comments (e.g., # in Python)

Syntax: # @Type variableName (property=value, ...)
Output: #@output Type outputName (MUST THEN DEFINE in script)

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

KEY PROPERTIES
--------------
• label="text" → UI display name
• description="text" → Tooltip
• value=X → Default value
• required=true|false → Optional parameter
• min=X, max=X, stepSize=X → Numeric constraints
• style="slider|file|save|directory|files|directories" → Widget/mode
• visibility=NORMAL|TRANSIENT|INVISIBLE|MESSAGE

EXAMPLE
-------
#@ ImagePlus img
#@output inverted

inverted = img.duplicate()
inverted.getProcessor().invert()
inverted.updateAndDraw()
	""";
	}
}
