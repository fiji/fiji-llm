package sc.fiji.llm.script;

import java.util.Objects;

/**
 * Represents the location of a script within the editor.
 * Combines editor instance index and tab index into a single address.
 */
public class ScriptAddress {
	/** Constant indicating that the address is not yet set (for new scripts) */
	public static final int UNSET = -1;

	public final int editorIndex;
	public final int tabIndex;

	public ScriptAddress(final int editorIndex, final int tabIndex) {
		this.editorIndex = editorIndex;
		this.tabIndex = tabIndex;
	}

	public boolean isSet() {
		return editorIndex != UNSET && tabIndex != UNSET;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final ScriptAddress other = (ScriptAddress) obj;
		return editorIndex == other.editorIndex && tabIndex == other.tabIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(editorIndex, tabIndex);
	}

	@Override
	public String toString() {
		return "[" + editorIndex + ":" + tabIndex + "]";
	}
}
