package sc.fiji.llm.chat;

import java.util.List;

/**
 * Represents a context item that can be added to the chat.
 */
public interface ContextItem {

    public String getType();
    public String getLabel();
    public String getContent();
    public String getMergeKey();
    public ContextItem mergeWith(final List<ContextItem> others);
}
