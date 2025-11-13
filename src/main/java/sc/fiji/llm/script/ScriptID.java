package sc.fiji.llm.script;

import java.util.Objects;

/**
 * Represents a script within the editor.
 * Combines editor instance index and tab index into a single ID.
 */
public class ScriptID {
	public final int editorIndex;
	public final int tabIndex;

	public ScriptID(final int editorIndex, final int tabIndex) {
		this.editorIndex = editorIndex;
		this.tabIndex = tabIndex;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final ScriptID other = (ScriptID) obj;
		return editorIndex == other.editorIndex && tabIndex == other.tabIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(editorIndex, tabIndex);
	}

	@Override
	public String toString() {
		return editorIndex + ":" + tabIndex;
	}
}
