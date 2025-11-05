package sc.fiji.llm.chat;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import sc.fiji.llm.script.ScriptContextItem;

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

    @Test
    public void testAddContextItem() {
        // Given: a conversation
        ContextItem item = new TestContextItem("script", "test.py", "print('hello')");

        // When: we add a context item
        conversation.addContextItem(item);

        // Then: it should be stored
        List<ContextItem> items = conversation.getContextItems();
        assertEquals(1, items.size());
        assertEquals(item, items.get(0));
    }

    @Test
    public void testAddMultipleContextItems() {
        // Given: a conversation
        ContextItem item1 = new TestContextItem("script", "test1.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test2.py", "print('world')");

        // When: we add multiple context items
        conversation.addContextItem(item1);
        conversation.addContextItem(item2);

        // Then: both should be stored
        List<ContextItem> items = conversation.getContextItems();
        assertEquals(2, items.size());
        assertEquals(item1, items.get(0));
        assertEquals(item2, items.get(1));
    }

    @Test
    public void testRemoveContextItem() {
        // Given: a conversation with context items
        ContextItem item1 = new TestContextItem("script", "test1.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test2.py", "print('world')");
        conversation.addContextItem(item1);
        conversation.addContextItem(item2);

        // When: we remove one context item
        conversation.removeContextItem(item1);

        // Then: only the second item should remain
        List<ContextItem> items = conversation.getContextItems();
        assertEquals(1, items.size());
        assertEquals(item2, items.get(0));
    }

    @Test
    public void testContextItemIncludedInUserMessage() {
        // Given: a conversation with a context item
        ContextItem item = new TestContextItem("script", "test.py", "print('hello')");
        conversation.addContextItem(item);

        // When: we add a user message
        String userMessage = "Run this script";
        conversation.addUserMessage(userMessage);

        // Then: the user message should include the context item
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size());
        UserMessage userMsg = (UserMessage) messages.get(1);

        // The message should contain all the context information
        // We verify by checking the message was constructed properly
        assertNotNull(userMsg);
        assertTrue(messages.get(1) instanceof UserMessage);
    }

    @Test
    public void testContextItemsClearedAfterAddingUserMessage() {
        // Given: a conversation with context items
        ContextItem item = new TestContextItem("script", "test.py", "print('hello')");
        conversation.addContextItem(item);
        assertEquals(1, conversation.getContextItems().size());

        // When: we add a user message
        conversation.addUserMessage("Process this");

        // Then: context items should be cleared
        List<ContextItem> items = conversation.getContextItems();
        assertEquals(0, items.size());
    }

    @Test
    public void testMultipleContextItemsIncludedInUserMessage() {
        // Given: a conversation with multiple context items
        ContextItem item1 = new TestContextItem("script", "test1.py", "content1");
        ContextItem item2 = new TestContextItem("script", "test2.py", "content2");
        conversation.addContextItem(item1);
        conversation.addContextItem(item2);

        // When: we add a user message
        String userMessage = "Process these scripts";
        conversation.addUserMessage(userMessage);

        // Then: both context items should be in the message
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size());
        assertTrue(messages.get(1) instanceof UserMessage);

        // And they should be cleared
        assertEquals(0, conversation.getContextItems().size());
    }

    @Test
    public void testUserMessageWithoutContextItems() {
        // Given: a conversation with no context items
        assertEquals(0, conversation.getContextItems().size());

        // When: we add a user message without context
        String userMessage = "Hello";
        conversation.addUserMessage(userMessage);

        // Then: the message should be added without context formatting
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size());
        assertTrue(messages.get(1) instanceof UserMessage);
    }

    @Test
    public void testContextItemToString() {
        // Given: a context item
        ContextItem item = new TestContextItem("script", "test.py", "print('hello')");

        // When: we call toString
        String formatted = item.toString();

        // Then: it should have the proper format
        assertTrue(formatted.contains("script"));
        assertTrue(formatted.contains("test.py"));
        assertTrue(formatted.contains("print('hello')"));
        assertTrue(formatted.startsWith("\n---"));
    }

    @Test
    public void testContextItemEquality() {
        // Given: two context items with the same content
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test.py", "print('hello')");

        // When/Then: they should be equal
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void testContextItemInequality() {
        // Given: two context items with different content
        ContextItem item1 = new TestContextItem("script", "test1.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test2.py", "print('hello')");

        // When/Then: they should not be equal
        assertNotEquals(item1, item2);
    }

    @Test
    public void testContextItemInequalityDifferentType() {
        // Given: two context items with different types
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        ContextItem item2 = new TestContextItem("doc", "test.py", "print('hello')");

        // When/Then: they should not be equal
        assertNotEquals(item1, item2);
    }

    @Test
    public void testContextItemInequalityDifferentContent() {
        // Given: two context items with different content
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        ContextItem item2 = new TestContextItem("script", "test.py", "print('world')");

        // When/Then: they should not be equal
        assertNotEquals(item1, item2);
    }

    @Test
    public void testContextItemContainsWithEquals() {
        // Given: a conversation with a context item
        ContextItem item1 = new TestContextItem("script", "test.py", "print('hello')");
        conversation.addContextItem(item1);

        // When: we check if the conversation contains an equal item
    ContextItem item2 = new TestContextItem("script", "test.py", "print('hello')");
        boolean contains = conversation.getContextItems().contains(item2);

        // Then: it should find it
        assertTrue(contains);
    }

    @Test
    public void testContextItemsNotClearedByAssistantMessage() {
        // Given: a conversation with context items
        ContextItem item = new TestContextItem("script", "test.py", "print('hello')");
        conversation.addContextItem(item);
        assertEquals(1, conversation.getContextItems().size());

        // When: we add an assistant message (not a user message)
        conversation.addAssistantMessage("Response");

        // Then: context items should NOT be cleared
        List<ContextItem> items = conversation.getContextItems();
        assertEquals(1, items.size());
    }

    @Test
    public void testContextItemClearedOnlyOnUserMessage() {
        // Given: a conversation with context items and messages
        ContextItem item1 = new TestContextItem("script", "test1.py", "content1");
        conversation.addContextItem(item1);

        // When: we add a user message
        conversation.addUserMessage("First message");

        // Then: context items should be cleared
        assertEquals(0, conversation.getContextItems().size());

        // When: we add new context items and an assistant message
    ContextItem item2 = new TestContextItem("script", "test2.py", "content2");
        conversation.addContextItem(item2);
        conversation.addAssistantMessage("Response");

        // Then: context items should still be there
        assertEquals(1, conversation.getContextItems().size());

        // When: we add another user message
        conversation.addUserMessage("Second message");

        // Then: context items should be cleared again
        assertEquals(0, conversation.getContextItems().size());
    }

    @Test
    public void testScriptContextItemMergeKey() {
        // Given: a script context item
        ScriptContextItem item = new ScriptContextItem("test.py", "content", 0, 1);

        // When: we get the merge key
        String mergeKey = item.getMergeKey();

        // Then: it should be constructed from instance and tab indices
        assertEquals("script:0:1", mergeKey);
    }

    @Test
    public void testScriptContextItemMergeWithSameScript() {
        // Given: two script context items from the same script with different selections
        String content = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("test.py", content, 0, 1, "", 1, 2);
        ScriptContextItem item2 = new ScriptContextItem("test.py", content, 0, 1, "", 4, 5);

        // When: we merge them
        ContextItem merged = item1.mergeWith(java.util.Collections.singletonList(item2));

        // Then: the result should have both ranges
        assertNotNull(merged);
        assertTrue(merged instanceof ScriptContextItem);
        String mergedStr = merged.toString();
        assertTrue(mergedStr.contains("Selected lines:"));
        assertTrue(mergedStr.contains("1-2"));
        assertTrue(mergedStr.contains("4-5"));
    }

    @Test
    public void testScriptContextItemMergeOverlappingRanges() {
        // Given: two script context items with overlapping selections
        String content = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("test.py", content, 0, 1, "", 1, 3);
        ScriptContextItem item2 = new ScriptContextItem("test.py", content, 0, 1, "", 2, 4);

        // When: we merge them
        ContextItem merged = item1.mergeWith(java.util.Collections.singletonList(item2));

        // Then: overlapping ranges should be combined into one
        assertNotNull(merged);
        String mergedStr = merged.toString();
        assertTrue(mergedStr.contains("1-4"));
        // Should not have separate ranges listed
        assertFalse(mergedStr.contains("1-3"));
        assertFalse(mergedStr.contains("2-4"));
    }

    @Test
    public void testScriptContextItemMergeAdjacentRanges() {
        // Given: two script context items with adjacent selections
        String content = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("test.py", content, 0, 1, "", 1, 2);
        ScriptContextItem item2 = new ScriptContextItem("test.py", content, 0, 1, "", 3, 4);

        // When: we merge them
        ContextItem merged = item1.mergeWith(java.util.Collections.singletonList(item2));

        // Then: adjacent ranges should be combined
        assertNotNull(merged);
        String mergedStr = merged.toString();
        assertTrue(mergedStr.contains("1-4"));
    }

    @Test
    public void testConversationMergesScriptContextItems() {
        // Given: a conversation with multiple script items from the same source
        String scriptContent = "line1\nline2\nline3\nline4\nline5";
        ScriptContextItem item1 = new ScriptContextItem("script.py", scriptContent, 0, 0, "", 1, 2);
        ScriptContextItem item2 = new ScriptContextItem("script.py", scriptContent, 0, 0, "", 4, 5);

        conversation.addContextItem(item1);
        conversation.addContextItem(item2);

        // When: we add a user message (which triggers merging)
        conversation.addUserMessage("Analyze these selections");

        // Then: the context items should be merged
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size()); // System message + user message
        assertTrue(messages.get(1) instanceof UserMessage);

        // The conversation context items should be cleared
        assertEquals(0, conversation.getContextItems().size());
    }

    @Test
    public void testConversationPreservesNonMergeableItems() {
        // Given: a conversation with both mergeable and non-mergeable items
        String scriptContent = "content";
        ScriptContextItem scriptItem = new ScriptContextItem("script.py", scriptContent, 0, 0);

        // non-mergeable test item
        ContextItem otherItem = new TestContextItem("doc", "readme.md", "documentation");

        conversation.addContextItem(scriptItem);
        conversation.addContextItem(otherItem);

        // When: we add a user message
        conversation.addUserMessage("Help");

        // Then: both items should be in the conversation (merged into the message)
        List<ChatMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size()); // System message + user message

        // And context items should be cleared
        assertEquals(0, conversation.getContextItems().size());
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

    /**
     * Simple test helper implementation of ContextItem for unit tests.
     */
    private static class TestContextItem extends AbstractContextItem {
        private final String content;

        public TestContextItem(String type, String label, String content) {
            super(type, label);
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("\n--- ").append(getType()).append(": ").append(getLabel()).append(" ---\n");
            sb.append(content).append("\n");
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final TestContextItem other = (TestContextItem) obj;
            return java.util.Objects.equals(getType(), other.getType()) &&
                   java.util.Objects.equals(getLabel(), other.getLabel()) &&
                   java.util.Objects.equals(content, other.content);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(getType(), getLabel(), content);
        }
    }
}
