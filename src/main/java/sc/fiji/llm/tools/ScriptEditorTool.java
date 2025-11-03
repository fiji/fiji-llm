package sc.fiji.llm.tools;

import java.io.File;

import javax.swing.SwingUtilities;

import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.ScriptEditor;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import dev.langchain4j.agent.tool.Tool;
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
		return "Create and update scripts in the Fiji script editor. " +
			"Script names must have a file extension, which determines language automatically. " +
			"Supported languages: Python (.py), Groovy (.groovy), JavaScript (.js), BeanShell (.bsh), ImageJ Macro (.ijm). " +
			"Use createScript for new scripts. " +
			"Use updateScript with ScriptContext indices to modify existing scripts.";
	}

	@Tool(name = "createScript", value = {
		"Creates a new script in the Fiji script editor.",
		"Arg 1 - scriptName: The name of the script file including extension (e.g., 'example.py', 'analysis.groovy', 'macro.ijm').",
		"Arg 2 - content: The complete source code you wrote for the script.",
		"Returns: Success message with script name, or ERROR message if creation failed."
	})
	public String createScript(final String scriptName, final String content) {
		try {
			// Strip line numbers from content if present
			final String cleanContent = TextEditorUtils.stripLineNumbers(content);

			// Check if there's an open script editor instance
			TextEditor textEditor = TextEditorUtils.getMostRecentVisibleEditor();
			
			if (textEditor == null) {
				// Open a new editor on EDT
				SwingUtilities.invokeAndWait(() -> commandService.run(ScriptEditor.class, true));
				
				// Wait briefly for editor to initialize
				Thread.sleep(500);
				
				textEditor = TextEditorUtils.getMostRecentVisibleEditor();
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
		"Arg 1 - instanceIndex: The script editor instance index, from ScriptContextItem.",
		"Arg 2 - tabIndex: The tab index within the editor instance, from ScriptContextItem.",
		"Arg 3 - content: The new content for the indicated script.",
		"Returns: Success message with indices, or ERROR message if update failed."
	})
	public String updateScript(final int instanceIndex, final int tabIndex, final String content) {
		try {
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
}
