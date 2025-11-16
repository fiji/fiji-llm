package sc.fiji.llm.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Serializable container for a Conversation, used for JSON persistence.
 */
public class SerializedConversation {
    private String name;
    private String systemMessage;
    private List<SerializedConversationMessage> messages = new ArrayList<>();

    // No-arg constructor for GSON
    public SerializedConversation() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    public List<SerializedConversationMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<SerializedConversationMessage> messages) {
        this.messages = messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializedConversation that = (SerializedConversation) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(systemMessage, that.systemMessage) &&
               Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, systemMessage, messages);
    }

    /**
     * Represents a single message in a serialized conversation.
     */
    public static class SerializedConversationMessage {
        private String displayMessage;
        private SerializedMessage memoryMessage;

        public SerializedConversationMessage() {
        }

        public String getDisplayMessage() {
            return displayMessage;
        }

        public void setDisplayMessage(String displayMessage) {
            this.displayMessage = displayMessage;
        }

        public SerializedMessage getMemoryMessage() {
            return memoryMessage;
        }

        public void setMemoryMessage(SerializedMessage memoryMessage) {
            this.memoryMessage = memoryMessage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SerializedConversationMessage that = (SerializedConversationMessage) o;
            return Objects.equals(displayMessage, that.displayMessage) &&
                   Objects.equals(memoryMessage, that.memoryMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(displayMessage, memoryMessage);
        }
    }
}
