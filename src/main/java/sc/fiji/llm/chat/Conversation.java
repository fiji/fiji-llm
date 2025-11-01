package sc.fiji.llm.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * Creates a new conversation with the given system message.
     *
     * @param systemPrompt the system prompt to initialize the conversation
     */
    public Conversation(final String systemPrompt) {
        this.messages = new ArrayList<>();
        this.messages.add(new SystemMessage(systemPrompt));
    }

    /**
     * Adds a user message to the conversation.
     *
     * @param userMessage the user message text
     */
    public void addUserMessage(final String userMessage) {
        messages.add(new UserMessage(userMessage));
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
