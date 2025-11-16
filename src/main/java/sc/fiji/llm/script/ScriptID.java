/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package sc.fiji.llm.script;

import java.util.Objects;

/**
 * Represents a script within the editor. Combines editor instance index and tab
 * index into a single ID.
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
