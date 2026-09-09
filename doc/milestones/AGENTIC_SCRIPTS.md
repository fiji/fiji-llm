# Agentic Script Support in Fiji Chat

## Overview

The Fiji Chat assistant now has agentic capabilities to create and modify scripts directly in the Fiji Script Editor. This is implemented using LangChain4j's **Tools** feature (also known as function calling).

## Architecture

### Components

1. **ScriptEditorTool** - The tool class that exposes script manipulation capabilities to the LLM
   - Annotated with `@Tool` to define the functions the LLM can call
   - Provides two main methods:
     - `openScriptEditor()` - Opens the script editor window
     - `createOrUpdateScript()` - Creates or modifies scripts with automatic language detection

2. **CommandService Integration** - Uses SciJava's CommandService to launch the editor
   - Runs `org.scijava.ui.swing.script.ScriptEditor` command
   - Checks `TextEditor.instances` to verify if editor is already open

3. **Direct TextEditor Integration** - Direct interaction with the Script Editor
   - Accesses TextEditor instances for editor state
   - Creates/updates tabs programmatically
   - Handles language detection through file extension

4. **LLMService Updates** - Enhanced to support tools
   - New `createAssistant()` overload that accepts tool objects
   - Tools are registered with the LangChain4j `AiServices` builder

5. **FijiAssistant Updates** - Enhanced system message
   - Instructs the LLM to use the tools when generating scripts
   - Ensures consistent behavior across conversations

## How It Works

### LLM Tool Calling Flow

1. **User Request**: User asks for a script (e.g., "Create a Python script that opens an image")

2. **LLM Decision**: The LLM recognizes this requires executable code and decides to use the tools:
   - First calls `openScriptEditor()` if needed
   - Then calls `createOrUpdateScript()` with the script content

3. **Tool Invocation**: LangChain4j intercepts the tool calls and invokes the Java methods:
   ```java
   scriptEditorTool.openScriptEditor()
   scriptEditorTool.createOrUpdateScript("OpenImage.py", scriptContent)
   ```

4. **Script Editor Update**: ScriptEditorTool:
   - Opens the script editor using CommandService if not already open
   - Extracts the file extension from the script name (e.g., "py" from "OpenImage.py")
   - Creates a new tab with `textEditor.newTab(content, extension)`
   - Sets the filename using `setFileName(new File(scriptName))` which triggers automatic language detection
   - For updates, finds the existing tab by name, updates content, and switches to that tab

5. **Response**: The tool returns a success/failure message that the LLM incorporates into its response

### User Experience

From the user's perspective:
- They ask for a script in natural language
- The assistant responds AND automatically creates/opens the script in the editor
- The script has proper syntax highlighting based on the file extension
- The user can immediately run, modify, or save the generated script
- No copy-pasting required!

## Example Interaction

**User**: "I need a Python script to threshold all open images"

**Assistant**: "I'll create a Python script for you that thresholds all open images..."
*[Tool calls happen in background: openScriptEditor(), createOrUpdateScript()]*
"I've created the script `ThresholdImages.py` in the script editor. You can run it directly or modify the threshold value as needed."

**Result**: A new tab appears in the Script Editor with the complete, runnable Python script with Python syntax highlighting.

## Implementation Details

### Tool Definitions

The `@Tool` annotations provide the LLM with information about when and how to use the tools:

```java
@Tool("Opens the Fiji script editor window. " +
      "Use this before creating scripts. " +
      "The editor will open with an empty tab ready for use.")
public String openScriptEditor()

@Tool("Creates or updates a script in the Fiji script editor with the provided code. " +
      "Use this when you want to provide executable code to the user. " +
      "The scriptName should include the file extension (e.g., 'example.groovy'). " +
      "Valid extensions include: py, groovy, js, java, rb, bsh " +
      "IMPORTANT: The script editor must be open first. If you get an error that the editor is not open, " +
      "call openScriptEditor first, then call this function again.")
public String createOrUpdateScript(String scriptName, String content)
```

### Language Support

Supported scripting languages (based on file extension):
- **Python** (`.py`) - Jython
- **Groovy** (`.groovy`)
- **JavaScript** (`.js`)
- **Java** (`.java`)
- **Ruby** (`.rb`) - JRuby
- **BeanShell** (`.bsh`)

Language detection is automatic based on the file extension in the script name.

### Script Editor Interaction

The implementation directly accesses the SciJava Script Editor:

1. **Opening the Editor**:
   - Checks `TextEditor.instances` static field to see if editor is already open
   - Uses CommandService to run `org.scijava.ui.swing.script.ScriptEditor` command

2. **Creating/Updating Scripts**:
   - Extracts file extension from script name (e.g., "hello.py" → "py")
   - For new scripts:
     - Calls `textEditor.newTab(content, extension)` with just the extension
     - Creates a temporary `File` object with the full script name
     - Calls `editorPane.setFileName(File)` which triggers `setLanguageByFileName()` internally
   - For existing scripts:
     - Finds the tab by matching title with script name
     - Updates the editor pane text with `setText()`
     - Switches to the updated tab using `switchTo(index)`

3. **Tab Management**:
   - `findTabByName()` iterates through tabs to find matches
   - `getTabCount()` safely counts tabs by iterating until null/exception
   - Tab titles are matched exactly against the provided script name

### Key Implementation Details

**Language Detection**: The trick to proper language detection is using the two-argument version of `setFileName`:
- `setFileName(String)` only sets the base name, doesn't trigger language detection
- `setFileName(File)` triggers `setLanguageByFileName()` internally, enabling proper syntax highlighting

**Extension Extraction**: Script names must include the extension:
```java
String extension = "";
int lastDot = scriptName.lastIndexOf('.');
if (lastDot > 0 && lastDot < scriptName.length() - 1) {
    extension = scriptName.substring(lastDot + 1);
}
```

**Error Handling**: The tool provides clear error messages:
- If editor isn't open: "Script editor is not open. Please call openScriptEditor first, then try again."
- The LLM learns to call `openScriptEditor()` first, then retry `createOrUpdateScript()`

## Future Enhancements

Potential improvements:
1. **Script Execution Tool** - Allow the LLM to run scripts and see output
2. **Script Reading Tool** - Allow the LLM to read existing script content from open tabs
3. **Error Handling Tool** - Allow the LLM to help debug script errors by reading console output
4. **Multi-file Support** - Create multiple related scripts in one operation (e.g., utils + main)
5. **Template Support** - Provide script templates based on common tasks
6. **Script History** - Track and reference previously created scripts in the conversation

## Best Practices for LLM Interaction

When using the script tools, the LLM should:
1. **Always call `openScriptEditor()` first** if uncertain about editor state
2. **Include file extension** in script names (e.g., "SegmentNuclei.py" not "SegmentNuclei")
3. **Use descriptive names** (e.g., "ThresholdAllImages.groovy" not "script1.groovy")
4. **Include comments** in generated scripts explaining what the code does
5. **Prefer complete, runnable scripts** over fragments that require modification
6. **Inform the user** about what was created and how to use it
7. **Handle updates gracefully** - if updating an existing script, mention that

## Troubleshooting

### Script Editor Doesn't Open
- The tool automatically attempts to open it via CommandService
- Check that `org.scijava.ui.swing.script.ScriptEditor` command is available
- Verify CommandService is properly injected

### Language Not Detected / "Select Language" Prompt Appears
- Ensure the script name includes a valid file extension
- The extension must match one of: py, groovy, js, java, rb, bsh
- Check that `setFileName(File)` is being called (not `setFileName(String)`)
- Verify the ScriptService has language plugins available

### Updates Don't Appear
- The tool matches tabs by exact title (including extension)
- Tab title must exactly match the script name parameter
- Check for whitespace differences or case sensitivity issues

### Script Content is Empty
- Verify `newTab(content, extension)` receives non-null content
- Check that `setText()` is being called for updates
- Ensure content parameter is passed correctly from LLM

### Multiple Tabs with Same Name
- Currently, only the first match is updated
- Consider using unique, descriptive names for each script
- Future enhancement could handle duplicates more gracefully
