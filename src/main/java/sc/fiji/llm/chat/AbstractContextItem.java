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
     * Returns a key for grouping mergeable items.
     * Items with the same merge key can be combined into a single representation.
     *
     * @return a key identifying this item's merge group, or null if this item doesn't merge
     */
    @Override
    public String getMergeKey() {
        return null;
    }

    /**
     * Merges this item with others that share the same merge key.
     * Only called if {@link #getMergeKey()} is non-null.
     *
     * @param others context items that share the same merge key as this item
     * @return a new merged context item
     * @throws UnsupportedOperationException if this item type doesn't support merging
     */
    @Override
    public ContextItem mergeWith(final List<ContextItem> others) {
        throw new UnsupportedOperationException("Merging not supported for " + getClass().getSimpleName());
    }

    /**
     * Returns a nicely formatted string representation of this context item
     * for inclusion in chat messages.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n--- ").append(type).append(": ").append(label).append(" ---\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final AbstractContextItem other = (AbstractContextItem) obj;
        return Objects.equals(type, other.type) &&
               Objects.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, label);
    }
}
