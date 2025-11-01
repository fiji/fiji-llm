package sc.fiji.llm.chat;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

/**
 * Unit tests for the Conversation class.
 */
public class ConversationTest {
    private static final String SYSTEM_PROMPT = "You are a helpful assistant.";
    private Conversation conversation;

    @Before
    public void setUp() {
        conversation = new Conversation(SYSTEM_PROMPT);
    }

    @Test
    public void testConversationInitialization() {
        // Given: a new conversation
        // When: we get the messages
        List<ChatMessage> messages = conversation.getMessages();

        // Then: it should contain only the system message
        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
    }

    @Test
    public void testAddUserMessage() {
        // Given: a conversation with system message
        String userText = "Hello, assistant!";

        // When: we add a user message
        conversation.addUserMessage(userText);

        // Then: the message should be added
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size());
        assertTrue(messages.get(1) instanceof UserMessage);
    }

    @Test
    public void testAddAssistantMessage() {
        // Given: a conversation with system message
        String assistantText = "I'm here to help!";

        // When: we add an assistant message
        conversation.addAssistantMessage(assistantText);

        // Then: the message should be added
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size());
        assertTrue(messages.get(1) instanceof AiMessage);
    }

    @Test
    public void testMultipleMessages() {
        // Given: a conversation with system message
        // When: we add multiple messages
        conversation.addUserMessage("First message");
        conversation.addAssistantMessage("First response");
        conversation.addUserMessage("Second message");
        conversation.addAssistantMessage("Second response");

        // Then: all messages should be tracked
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(5, messages.size()); // System + 4 messages
        assertTrue(messages.get(1) instanceof UserMessage);
        assertTrue(messages.get(2) instanceof AiMessage);
        assertTrue(messages.get(3) instanceof UserMessage);
        assertTrue(messages.get(4) instanceof AiMessage);
    }

    @Test
    public void testGetMessages() {
        // Given: a conversation with some messages
        conversation.addUserMessage("Test");
        conversation.addAssistantMessage("Response");

        // When: we get the messages
        List<ChatMessage> messages = conversation.getMessages();

        // Then: the list should be unmodifiable
        assertEquals(3, messages.size());
        // Verify it's unmodifiable by checking that add throws
        assertTrue(!canModifyList(messages));
    }

    @Test
    public void testGetMessagesCopy() {
        // Given: a conversation with some messages
        conversation.addUserMessage("Test");
        conversation.addAssistantMessage("Response");

        // When: we get a copy of messages
        List<ChatMessage> messagesCopy = conversation.getMessagesCopy();

        // Then: it should be a modifiable list with the same content
        assertEquals(3, messagesCopy.size());
        // Verify it's modifiable by adding to the copy
        messagesCopy.add(new UserMessage("Extra"));
        assertEquals(4, messagesCopy.size());

        // And the original should remain unchanged
        List<ChatMessage> original = conversation.getMessages();
        assertEquals(3, original.size());
    }

    @Test
    public void testBuildChatRequest() {
        // Given: a conversation with some messages
        conversation.addUserMessage("What is 2+2?");
        conversation.addAssistantMessage("2+2 equals 4");

        // When: we build a chat request
        ChatRequest request = conversation.buildChatRequest();

        // Then: the request should be properly constructed
        assertNotNull(request);
        assertNotNull(request.messages());
        assertEquals(3, request.messages().size());
        assertTrue(request.messages().get(0) instanceof SystemMessage);
        assertTrue(request.messages().get(1) instanceof UserMessage);
        assertTrue(request.messages().get(2) instanceof AiMessage);
    }

    @Test
    public void testBuildChatRequestWithEmptyConversation() {
        // Given: a new conversation with only system message
        // When: we build a chat request
        ChatRequest request = conversation.buildChatRequest();

        // Then: the request should contain only the system message
        assertNotNull(request);
        assertEquals(1, request.messages().size());
        assertTrue(request.messages().get(0) instanceof SystemMessage);
    }

    /**
     * Helper method to check if a list can be modified.
     */
    private boolean canModifyList(List<?> list) {
        try {
            list.add(null);
            list.remove(list.size() - 1);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}
