package sc.fiji.llm.chat;

import java.util.List;

import org.scijava.service.SciJavaService;

/**
 * {@link SciJavaService} for managing {@link Conversation}s.
 */
public interface ConversationService extends SciJavaService {

    /**
     * @return Names of all saved conversations
     */
    List<String> getConversationNames();

    /**
     * Gets a saved conversation by name.
     *
     * @param name The conversation name
     * @return The conversation, or null if not found
     */
    Conversation getConversation(String name);

    /**
     * Creates and registers a new conversation.
     *
     * @param name The conversation name
     * @param systemMessage The system message for the conversation
     * @return The created conversation
     */
    Conversation createConversation(String name, dev.langchain4j.data.message.SystemMessage systemMessage);

    /**
     * Adds/updates a conversation.
     *
     * @param newConversation The conversation to add
     * @return true if successful
     */
    boolean addConversation(Conversation newConversation);

    /**
     * Removes a conversation.
     *
     * @param name The conversation name to remove
     * @return true if the conversation was found and removed
     */
    boolean removeConversation(String name);

    /**
     * Deletes a conversation permanently from disk and memory.
     *
     * @param name The conversation name to delete
     * @return true if the conversation was found and deleted
     */
    boolean deleteConversation(String name);
}

