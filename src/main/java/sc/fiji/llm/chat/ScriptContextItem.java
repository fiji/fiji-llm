package sc.fiji.llm.chat;

import java.util.Objects;

/**
 * Represents a script context item that can be added to the chat.
 */
public class ScriptContextItem extends ContextItem {
	/** Constant indicating that the instance/tab index is not yet set (creating new script) */
	public static final int NEW_INDEX = -1;
	/** Constant indicating that selection line numbers are not set */
	public static final int NO_SELECTION = -1;

	private final String scriptName;
	private final int instanceIndex;
	private final int tabIndex;
	private final String errorOutput;
	private final int selectionStartLine;
	private final int selectionEndLine;

	public ScriptContextItem(String scriptName, String content) {
		this(scriptName, content, NEW_INDEX, NEW_INDEX, "", NO_SELECTION, NO_SELECTION);
	}

	public ScriptContextItem(String scriptName, String content, int instanceIndex, int tabIndex) {
		this(scriptName, content, instanceIndex, tabIndex, "", NO_SELECTION, NO_SELECTION);
	}

	public ScriptContextItem(String scriptName, String content, int instanceIndex, int tabIndex, String errorOutput) {
		this(scriptName, content, instanceIndex, tabIndex, errorOutput, NO_SELECTION, NO_SELECTION);
	}

	public ScriptContextItem(String scriptName, String content, int instanceIndex, int tabIndex, String errorOutput,
			int selectionStartLine, int selectionEndLine) {
		super("Script", "📜 " + scriptName, content);
		this.scriptName = scriptName;
		this.instanceIndex = instanceIndex;
		this.tabIndex = tabIndex;
		this.errorOutput = errorOutput != null ? errorOutput : "";
		this.selectionStartLine = selectionStartLine;
		this.selectionEndLine = selectionEndLine;
	}

	public String getScriptName() {
		return scriptName;
	}

	public int getInstanceIndex() {
		return instanceIndex;
	}

	public int getTabIndex() {
		return tabIndex;
	}

	public String getErrorOutput() {
		return errorOutput;
	}

	public int getSelectionStartLine() {
		return selectionStartLine;
	}

	public int getSelectionEndLine() {
		return selectionEndLine;
	}

	public boolean hasSelection() {
		return selectionStartLine != NO_SELECTION && selectionEndLine != NO_SELECTION;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("\n--- Script: ").append(scriptName).append(" ---\n");
		sb.append("Editor index: ").append(instanceIndex).append(" | Tab index: ").append(tabIndex);
		if (hasSelection()) {
			sb.append(" | Selected lines: ").append(selectionStartLine).append("-").append(selectionEndLine);
		}
		sb.append("\n");
		sb.append("\n").append(getContent()).append("\n");
		if (!errorOutput.isEmpty()) {
			sb.append("\n--- Errors ---\n");
			sb.append(errorOutput).append("\n");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final ScriptContextItem other = (ScriptContextItem) obj;
		return Objects.equals(scriptName, other.scriptName) &&
				instanceIndex == other.instanceIndex &&
				tabIndex == other.tabIndex &&
				Objects.equals(getContent(), other.getContent()) &&
				Objects.equals(errorOutput, other.errorOutput) &&
				selectionStartLine == other.selectionStartLine &&
				selectionEndLine == other.selectionEndLine;
	}

	@Override
	public int hashCode() {
		return Objects.hash(scriptName, instanceIndex, tabIndex, getContent(), errorOutput, selectionStartLine, selectionEndLine);
	}
}
