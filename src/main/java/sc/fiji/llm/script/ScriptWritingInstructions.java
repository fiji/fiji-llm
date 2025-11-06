package sc.fiji.llm.script;

import org.scijava.plugin.Plugin;

import dev.langchain4j.agent.tool.Tool;
import sc.fiji.llm.tools.AiToolPlugin;

/**
 * Instructional tool that provides comprehensive guidance on script editor usage,
 * parameter declarations, and universal scripting features available across all languages.
 * This tool is language-agnostic and returns formatted reference documentation.
 */
@Plugin(type = AiToolPlugin.class)
public class ScriptWritingInstructions implements AiToolPlugin {

	@Override
	public String getName() {
		return "Script Writing Instructions";
	}

	@Override
	public String getUsage() {
		return "The return values of these tools are for YOU, the LLM, to use when writing scripts. " +
			"ALWAYS ensure you have run baseInstructions before writing scripts.";
	}

	@Tool(value = {
		"Returns: A detailed guide for writing scripts in any language"
	})
	public String baseInstructions() {
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
