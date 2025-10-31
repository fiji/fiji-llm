package sc.fiji.llm.ui;

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
}
