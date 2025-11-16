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

package sc.fiji.llm.chat;

import java.util.List;
import java.util.Objects;

/**
 * Represents a context item that can be added to the chat.
 */
public abstract class AbstractContextItem implements ContextItem {

	private final String type;
	private final String label;

	public AbstractContextItem(String type, String label) {
		this.type = type;
		this.label = label;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getLabel() {
		return label;
	}

	/**
	 * Returns a key for grouping mergeable items. Items with the same merge key
	 * can be combined into a single representation.
	 *
	 * @return a key identifying this item's merge group, or null if this item
	 *         doesn't merge
	 */
	@Override
	public String getMergeKey() {
		return null;
	}

	/**
	 * Merges this item with others that share the same merge key. Only called if
	 * {@link #getMergeKey()} is non-null.
	 *
	 * @param others context items that share the same merge key as this item
	 * @return a new merged context item
	 * @throws UnsupportedOperationException if this item type doesn't support
	 *           merging
	 */
	@Override
	public ContextItem mergeWith(final List<ContextItem> others) {
		throw new UnsupportedOperationException("Merging not supported for " +
			getClass().getSimpleName());
	}

	/**
	 * Returns a nicely formatted string representation of this context item for
	 * inclusion in chat messages.
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("\n--- ").append(type).append(": ").append(label).append(
			" ---\n");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		final AbstractContextItem other = (AbstractContextItem) obj;
		return Objects.equals(type, other.type) && Objects.equals(label,
			other.label);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(type, label);
	}
}
