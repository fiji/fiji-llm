package sc.fiji.llm.script;

import java.util.Objects;

import sc.fiji.llm.chat.AbstractContextItem;
import sc.fiji.llm.chat.ContextItem;

/**
 * Represents a script context item that can be added to the chat.
 */
public class ScriptContextItem extends AbstractContextItem {
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
		super("Script", scriptName, content);
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

	@Override
	public String getMergeKey() {
		return "script:" + instanceIndex + ":" + tabIndex;
	}

	@Override
	public ContextItem mergeWith(final java.util.List<ContextItem> others) {
		// Collect all line ranges from this item and all others
		final java.util.List<LineRange> ranges = new java.util.ArrayList<>();

		if (hasSelection()) {
			ranges.add(new LineRange(selectionStartLine, selectionEndLine));
		}

		for (final ContextItem item : others) {
			if (item instanceof ScriptContextItem scriptItem) {
				if (scriptItem.hasSelection()) {
					ranges.add(new LineRange(scriptItem.selectionStartLine, scriptItem.selectionEndLine));
				}
			}
		}

		// Sort and merge overlapping ranges
		ranges.sort((a, b) -> Integer.compare(a.start, b.start));
		final java.util.List<LineRange> mergedRanges = mergeRanges(ranges);

		// Create a new merged item
		return new MergedScriptContextItem(scriptName, getContent(), instanceIndex, tabIndex, errorOutput, mergedRanges);
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
	 * Formats a list of line ranges into a readable string.
	 */
	private static String formatRanges(final java.util.List<LineRange> ranges) {
		if (ranges.isEmpty()) {
			return "";
		}

		final StringBuilder sb = new StringBuilder(" | Selected lines: ");
		for (int i = 0; i < ranges.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(ranges.get(i).start).append("-").append(ranges.get(i).end);
		}
		return sb.toString();
	}

	/**
	 * Represents a range of lines.
	 */
	private static class LineRange {
		final int start;
		final int end;

		LineRange(final int start, final int end) {
			this.start = start;
			this.end = end;
		}
	}

	/**
	 * A script context item that has been merged from multiple selections.
	 */
	private static class MergedScriptContextItem extends ScriptContextItem {
		private final String mergedRangesLabel;

		MergedScriptContextItem(final String scriptName, final String content, final int instanceIndex,
				final int tabIndex, final String errorOutput, final java.util.List<LineRange> lineRanges) {
			super(scriptName, content, instanceIndex, tabIndex, errorOutput, NO_SELECTION, NO_SELECTION);
			this.mergedRangesLabel = formatRanges(lineRanges);
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("\n--- Script: ").append(getScriptName()).append(" ---\n");
			sb.append("Editor index: ").append(getInstanceIndex()).append(" | Tab index: ").append(getTabIndex());
			sb.append(mergedRangesLabel).append("\n");
			sb.append("\n").append(getContent()).append("\n");
			if (!getErrorOutput().isEmpty()) {
				sb.append("\n--- Errors ---\n");
				sb.append(getErrorOutput()).append("\n");
			}
			return sb.toString();
		}
	}
}
