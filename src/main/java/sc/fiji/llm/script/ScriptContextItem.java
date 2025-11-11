package sc.fiji.llm.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sc.fiji.llm.chat.AbstractContextItem;
import sc.fiji.llm.chat.ContextItem;

/**
 * Represents a script context item that can be added to the chat.
 */
public class ScriptContextItem extends AbstractContextItem {

	private final String scriptName;
	private final String scriptBody;
	private final ScriptAddress address;
	private final String errorOutput;
	private final List<LineRange> selectedRanges;

	public ScriptContextItem(String scriptName, String content) {
		this(scriptName, content, new ScriptAddress(ScriptAddress.UNSET, ScriptAddress.UNSET), "", new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptAddress address) {
		this(scriptName, content, address, "", new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptAddress address, String errorOutput) {
		this(scriptName, content, address, errorOutput, new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptAddress address, String errorOutput,
			int selectionStartLine, int selectionEndLine) {
		this(scriptName, content, address, errorOutput,
				selectionStartLine != ScriptAddress.UNSET && selectionEndLine != ScriptAddress.UNSET
					? List.of(new LineRange(selectionStartLine, selectionEndLine))
					: new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, ScriptAddress address, String errorOutput,
			List<LineRange> selectedRanges) {
		super("Script", scriptName);
		this.scriptName = scriptName;
		this.scriptBody = content;
		this.address = Objects.requireNonNull(address, "address cannot be null");
		this.errorOutput = errorOutput != null ? errorOutput : "";
		this.selectedRanges = new ArrayList<>(selectedRanges);
	}

	// Backward compatibility constructors
	public ScriptContextItem(String scriptName, String content, int editorIndex, int tabIndex) {
		this(scriptName, content, new ScriptAddress(editorIndex, tabIndex), "", new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, int editorIndex, int tabIndex, String errorOutput) {
		this(scriptName, content, new ScriptAddress(editorIndex, tabIndex), errorOutput, new ArrayList<>());
	}

	public ScriptContextItem(String scriptName, String content, int editorIndex, int tabIndex, String errorOutput,
			int selectionStartLine, int selectionEndLine) {
		this(scriptName, content, new ScriptAddress(editorIndex, tabIndex), errorOutput, selectionStartLine, selectionEndLine);
	}

	public String getScriptName() {
		return scriptName;
	}

	public String getScriptBody() {
		return scriptBody;
	}

	public ScriptAddress getAddress() {
		return address;
	}

	public int getEditorIndex() {
		return address.editorIndex;
	}

	public int getTabIndex() {
		return address.tabIndex;
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
		sb.append("  \"address\": \"").append(address).append("\",\n");
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
				Objects.equals(address, other.address) &&
				Objects.equals(getScriptBody(), other.getScriptBody()) &&
				Objects.equals(errorOutput, other.errorOutput) &&
				Objects.equals(selectedRanges, other.selectedRanges);
	}

	@Override
	public int hashCode() {
		return Objects.hash(scriptName, address, getScriptBody(), errorOutput, selectedRanges);
	}

	@Override
	public String getMergeKey() {
		return "script:" + address;
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
		ranges.sort((a, b) -> Integer.compare(a.start, b.start));
		final List<LineRange> mergedRanges = mergeRanges(ranges);

		// Create a new merged item with merged ranges
		return new ScriptContextItem(scriptName, getScriptBody(), address, errorOutput, mergedRanges);
	}

	/**
	 * Merges overlapping or adjacent line ranges.
	 */
	private static java.util.List<LineRange> mergeRanges(final java.util.List<LineRange> ranges) {
		if (ranges.isEmpty()) {
			return ranges;
		}

		final java.util.List<LineRange> merged = new java.util.ArrayList<>();
		LineRange current = ranges.get(0);

		for (int i = 1; i < ranges.size(); i++) {
			final LineRange next = ranges.get(i);
			if (current.end >= next.start - 1) {
				// Overlapping or adjacent - merge them
				current = new LineRange(current.start, Math.max(current.end, next.end));
			} else {
				// Gap - save current and start new
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);
		return merged;
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
		final StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < ranges.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			final LineRange r = ranges.get(i);
			sb.append("\"").append(r.start).append("-").append(r.end).append("\"");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Represents a range of lines.
	 */
	public static class LineRange {
		public final int start;
		public final int end;

		public LineRange(final int start, final int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			final LineRange other = (LineRange) obj;
			return start == other.start && end == other.end;
		}

		@Override
		public int hashCode() {
			return Objects.hash(start, end);
		}

		@Override
		public String toString() {
			return start + "-" + end;
		}
	}
}
