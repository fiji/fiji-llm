package sc.fiji.llm.chat;

import java.util.Objects;

/**
 * Represents a context item that can be added to the chat.
 */
public class ContextItem {
    private final String type;
    private final String label;
    private final String content;

    public ContextItem(String type, String label, String content) {
        this.type = type;
        this.label = label;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getContent() {
        return content;
    }

    /**
     * Returns a nicely formatted string representation of this context item
     * for inclusion in chat messages.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n--- ").append(type).append(": ").append(label).append(" ---\n");
        sb.append(content).append("\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final ContextItem other = (ContextItem) obj;
        return Objects.equals(type, other.type) &&
               Objects.equals(label, other.label) &&
               Objects.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, label, content);
    }
}
