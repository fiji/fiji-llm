package sc.fiji.llm.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import sc.fiji.llm.chat.AbstractContextItem;
import sc.fiji.llm.chat.ContextItem;

/**
 * Represents a script context item that can be added to the chat.
 */
public class ScriptContextItem extends AbstractContextItem {

	private final String scriptName;
	private final String scriptBody;
	private final ScriptID id;
	private final String errorOutput;
	private final List<LineRange> selectedRanges;

	public ScriptContextItem(String scriptName, String content, ScriptID id) {
		this(scriptName, content, id, "", new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptID id, String errorOutput) {
		this(scriptName, content, id, errorOutput, new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptID id, String errorOutput,
			int selectionStartLine, int selectionEndLine) {
		this(scriptName, content, id, errorOutput,
				selectionStartLine != -1 && selectionEndLine != -1
					? List.of(new LineRange(selectionStartLine, selectionEndLine))
					: new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptID id, String errorOutput,
			List<LineRange> selectedRanges) {
		super("Script", scriptName);
		this.scriptName = scriptName;
		this.scriptBody = content;
		this.id = Objects.requireNonNull(id, "id cannot be null");
		this.errorOutput = errorOutput != null ? errorOutput : "";
		this.selectedRanges = new ArrayList<>(selectedRanges);
	}

	// Backward compatibility constructors
	public ScriptContextItem(String scriptName, String content, int editorIndex, int tabIndex) {
		this(scriptName, content, new ScriptID(editorIndex, tabIndex), "", new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, int editorIndex, int tabIndex, String errorOutput) {
		this(scriptName, content, new ScriptID(editorIndex, tabIndex), errorOutput, new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, int editorIndex, int tabIndex, String errorOutput,
			int selectionStartLine, int selectionEndLine) {
		this(scriptName, content, new ScriptID(editorIndex, tabIndex), errorOutput, selectionStartLine, selectionEndLine);
	}

	@Override
	public String getLabel() {
		StringBuilder sb = new StringBuilder();
		if (hasSelection()) {
			sb.append(formatRangesAsString(selectedRanges));
			sb.append(" ");
		}
		sb.append(scriptName);
		return sb.toString();
	}

	public String getScriptName() {
		return scriptName;
	}

	public String getScriptBody() {
		return scriptBody;
	}

	public ScriptID getId() {
		return id;
	}

	public int getEditorIndex() {
		return id.editorIndex;
	}

	public int getTabIndex() {
		return id.tabIndex;
	}

	public String getErrorOutput() {
		return errorOutput;
	}

	public List<LineRange> getSelectedRanges() {
		return new ArrayList<>(selectedRanges);
	}

	public boolean hasSelection() {
		return !selectedRanges.isEmpty();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"type\": \"Script\",\n");
		sb.append("  \"name\": \"").append(escapeJson(scriptName)).append("\",\n");
		sb.append("  \"id\": \"").append(id).append("\",\n");
		if (hasSelection()) {
			sb.append("  \"selectedLines\": ").append(formatRangesAsJson(selectedRanges)).append(",\n");
		}
		sb.append("  \"content\": \"").append(escapeJson(scriptBody)).append("\"");
		if (!errorOutput.isEmpty()) {
			sb.append(",\n");
			sb.append("  \"errors\": \"").append(escapeJson(errorOutput)).append("\"");
		}
		sb.append("\n}\n");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final ScriptContextItem other = (ScriptContextItem) obj;
		return Objects.equals(scriptName, other.scriptName) &&
				Objects.equals(id, other.id) &&
				Objects.equals(getScriptBody(), other.getScriptBody()) &&
				Objects.equals(errorOutput, other.errorOutput) &&
				Objects.equals(selectedRanges, other.selectedRanges);
	}

	@Override
	public int hashCode() {
		return Objects.hash(scriptName, id, getScriptBody(), errorOutput, selectedRanges);
	}

	@Override
	public String getMergeKey() {
		return "script:" + id;
	}

	@Override
	public ContextItem mergeWith(final java.util.List<ContextItem> others) {
		// Collect all line ranges from this item and all others
		final List<LineRange> ranges = new ArrayList<>(selectedRanges);

		for (final ContextItem item : others) {
			if (item instanceof ScriptContextItem scriptItem) {
				ranges.addAll(scriptItem.getSelectedRanges());
			}
		}

		// Sort and merge overlapping ranges
		ranges.sort((a, b) -> Integer.compare(a.getStart(), b.getStart()));
		final List<LineRange> mergedRanges = LineRange.mergeRanges(ranges);

		// Create a new merged item with merged ranges
		return new ScriptContextItem(scriptName, getScriptBody(), id, errorOutput, mergedRanges);
	}
	/**
	 * Escapes special characters for JSON strings.
	 */
	private static String escapeJson(final String str) {
		if (str == null) {
			return "";
		}
		return str.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	/**
	 * Formats a list of line ranges as JSON array with range notation.
	 */
	private static String formatRangesAsJson(final java.util.List<LineRange> ranges) {
		final StringJoiner joiner = new StringJoiner(", ", "[", "]");
		for (final LineRange r : ranges) {
			joiner.add("\"" + r.getStart() + "-" + r.getEnd() + "\"");
		}
		return joiner.toString();
	}

	private static String formatRangesAsString(final java.util.List<LineRange> ranges) {
		final StringJoiner joiner = new StringJoiner(", ", "[", "]");
		for (final LineRange r : ranges) {
			joiner.add(r.getStart() + "-" + r.getEnd());
		}
		return joiner.toString();
	}
}
