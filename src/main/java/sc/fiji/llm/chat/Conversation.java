package sc.fiji.llm.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

/**
 * Manages a conversation as a list of messages with a system prompt.
 * Provides methods to add user and assistant messages, and retrieve the full conversation history.
 */
public class Conversation {
    private final List<ChatMessage> messages;
    private final List<ContextItem> contextItems;

    /**
     * Creates a new conversation with the given system message.
     *
     * @param systemPrompt the system prompt to initialize the conversation
     */
    public Conversation(final String systemPrompt) {
        this.messages = new ArrayList<>();
        this.contextItems = new ArrayList<>();
        this.messages.add(new SystemMessage(systemPrompt));
    }

    /**
     * Adds a context item to be included in the next user message.
     *
     * @param item the context item to add
     */
    public void addContextItem(final ContextItem item) {
        contextItems.add(item);
    }

    /**
     * Removes a context item.
     *
     * @param item the context item to remove
     */
    public void removeContextItem(final ContextItem item) {
        contextItems.remove(item);
    }

    /**
     * Gets an unmodifiable list of context items.
     *
     * @return a list of context items
     */
    public List<ContextItem> getContextItems() {
        return Collections.unmodifiableList(contextItems);
    }

    /**
     * Adds a user message to the conversation, including any context items.
     * Context items are merged where applicable before being added to the message.
     * After adding the message, clears the context items collection.
     *
     * @param userMessage the user message text
     */
    public void addUserMessage(final String userMessage) {
        final StringBuilder fullMessage = new StringBuilder(userMessage);

        if (!contextItems.isEmpty()) {
            final List<ContextItem> itemsToInclude = mergeContextItems(contextItems);

            fullMessage.append("\n\n===Start of Context Items===\n");
            for (final ContextItem item : itemsToInclude) {
                fullMessage.append(item);
            }
            fullMessage.append("===End of Context Items===\n");

            // Clear context items after they've been added to the message
            contextItems.clear();
        }

        messages.add(new UserMessage(fullMessage.toString()));
    }

    /**
     * Merges context items that share the same merge key.
     *
     * @param items the context items to merge
     * @return a list of items with mergeable items combined
     */
    private List<ContextItem> mergeContextItems(final List<ContextItem> items) {
        final Map<String, List<ContextItem>> groupedByMergeKey = new LinkedHashMap<>();
        final List<ContextItem> result = new ArrayList<>();

        // Group items by merge key
        for (final ContextItem item : items) {
            final String mergeKey = item.getMergeKey();
            if (mergeKey != null) {
                groupedByMergeKey.computeIfAbsent(mergeKey, k -> new ArrayList<>()).add(item);
            } else {
                result.add(item);
            }
        }

        // Build result list with merged items
        for (final List<ContextItem> group : groupedByMergeKey.values()) {
            if (group.size() == 1) {
                result.add(group.get(0));
            } else {
                // Merge multiple items with the same key
                final ContextItem merged = group.get(0).mergeWith(group.subList(1, group.size()));
                result.add(merged);
            }
        }

        return result;
    }

    /**
     * Adds an assistant message to the conversation.
     *
     * @param assistantMessage the assistant message text
     */
    public void addAssistantMessage(final String assistantMessage) {
        messages.add(new AiMessage(assistantMessage));
    }

    /**
     * Gets an unmodifiable list of all messages in the conversation.
     *
     * @return a list of all messages
     */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Gets a mutable list of messages for building chat requests.
     * This is useful for APIs that expect a modifiable list.
     *
     * @return a mutable copy of the messages list
     */
    public List<ChatMessage> getMessagesCopy() {
        return new ArrayList<>(messages);
    }

    /**
     * Builds a ChatRequest with all messages in this conversation.
     *
     * @return a ChatRequest ready to be sent to an LLM
     */
    public ChatRequest buildChatRequest() {
        return ChatRequest.builder()
            .messages(new ArrayList<>(messages))
            .build();
    }
}
