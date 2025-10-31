package sc.fiji.llm.ui;

import org.scijava.command.CommandService;
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.swing.script.TextEditorTab;

import dev.langchain4j.agent.tool.Tool;

/**
 * Tools that allow the LLM to interact with the Fiji script editor.
 * Provides capabilities to open the editor and create/update scripts.
 */
public class ScriptEditorTool {
    private final CommandService commandService;

    public ScriptEditorTool(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Opens the Fiji script editor window.
     * 
     * @return a message indicating success or failure
     */
    @Tool("Opens the Fiji script editor window. " +
          "Use this before creating scripts. " +
          "The editor will open with an empty tab ready for use.")
    public String openScriptEditor() {
        try {
            if (!TextEditor.instances.isEmpty()) {
                return "Script editor is already open and ready for use.";
            }
            commandService.run(org.scijava.ui.swing.script.ScriptEditor.class, true);
            
            return "Script editor started...";
        } catch (Exception e) {
            return "Failed to open script editor: " + e.getMessage();
        }
    }
    
    /**
     * Creates a new script or updates an existing one in the script editor.
     *
     * @param scriptName the name of the script (e.g., "MyScript.groovy")
     * @param content the script content/code
     * @return a message indicating success or failure
     */
    @Tool("Creates or updates a script in the Fiji script editor with the provided code. " +
          "Use this when you want to provide executable code to the user. " +
          "The scriptName should include the file extension (e.g., 'example.groovy'). " +
          "Valid extensions include: py, groovy, js, java, rb, bsh " +
          "IMPORTANT: The script editor must be open first. If you get an error that the editor is not open, " +
          "call openScriptEditor first, then call this function again.")
    public String createOrUpdateScript(String scriptName, String content) {
        // TODO could use scriptService.getLanguages()
        // Check if the script editor is open - fail fast if not
        if (TextEditor.instances.isEmpty()) {
            return "Script editor is not open. Please call openScriptEditor first, then try again.";
        }
        
        TextEditor textEditor = TextEditor.instances.get(0);

        // Check if a tab with this name already exists
        TextEditorTab existingTab = findTabByName(textEditor, scriptName);

        if (existingTab != null) {
            // Update existing tab content
            existingTab.getEditorPane().setText(content);
            // Make sure this tab is selected
            for (int i = 0; i < getTabCount(textEditor); i++) {
                if (textEditor.getTab(i) == existingTab) {
                    textEditor.switchTo(i);
                    break;
                }
            }
        } else {
            // Extract extension from scriptName
            String extension = "";
            int lastDot = scriptName.lastIndexOf('.');
            if (lastDot > 0 && lastDot < scriptName.length() - 1) {
                extension = scriptName.substring(lastDot + 1);
            }
            
            // Create new tab - newTab() expects just the extension
            TextEditorTab tab = textEditor.newTab(content, extension);
            
            // Set the filename using a File object - this will trigger language detection
            // We create a File object (doesn't need to exist on disk) so that
            // setFileName(File) is called, which internally calls setLanguageByFileName
            org.scijava.ui.swing.script.EditorPane editorPane = 
                (org.scijava.ui.swing.script.EditorPane) tab.getEditorPane();
            editorPane.setFileName(new java.io.File(scriptName));
        }
        
        return "Successfully created/updated script: " + scriptName;
    }

    private TextEditorTab findTabByName(TextEditor textEditor, String scriptName) {
        // Count tabs by iterating until getTab returns null or throws
        int i = 0;
        while (true) {
            try {
                TextEditorTab tab = textEditor.getTab(i);
                if (tab == null) break;
                if (tab.getTitle().equals(scriptName)) {
                    return tab;
                }
                i++;
            } catch (Exception e) {
                break;
            }
        }
        return null;
    }
    
    // Helper method to get tab count
    private int getTabCount(TextEditor textEditor) {
        int count = 0;
        while (true) {
            try {
                if (textEditor.getTab(count) == null) break;
                count++;
            } catch (Exception e) {
                break;
            }
        }
        return count;
    }
}
