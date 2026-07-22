/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.context;

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
		return toJson().toString();
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
