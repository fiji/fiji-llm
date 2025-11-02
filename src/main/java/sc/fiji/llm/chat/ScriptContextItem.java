package sc.fiji.llm.chat;

import java.util.Objects;

/**
 * Represents a script context item that can be added to the chat.
 */
public class ScriptContextItem extends ContextItem {
	/** Constant indicating that the instance/tab index is not yet set (creating new script) */
	public static final int NEW_INDEX = -1;

	private final String scriptName;
	private final int instanceIndex;
	private final int tabIndex;
	private final String errorOutput;

	public ScriptContextItem(String scriptName, String content) {
		this(scriptName, content, NEW_INDEX, NEW_INDEX, "");
	}

	public ScriptContextItem(String scriptName, String content, int instanceIndex, int tabIndex) {
		this(scriptName, content, instanceIndex, tabIndex, "");
	}

	public ScriptContextItem(String scriptName, String content, int instanceIndex, int tabIndex, String errorOutput) {
		super("Script", "📜 " + scriptName, content);
		this.scriptName = scriptName;
		this.instanceIndex = instanceIndex;
		this.tabIndex = tabIndex;
		this.errorOutput = errorOutput != null ? errorOutput : "";
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("\n--- Script: ").append(scriptName).append(" ---\n");
		sb.append("Editor index: ").append(instanceIndex).append(" | Tab index: ").append(tabIndex).append("\n");
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
				Objects.equals(errorOutput, other.errorOutput);
	}

	@Override
	public int hashCode() {
		return Objects.hash(scriptName, instanceIndex, tabIndex, getContent(), errorOutput);
	}
}
