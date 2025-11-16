package sc.fiji.llm.chat;

import java.util.Objects;

/**
 * Serializable adapter for LangChain4j ChatMessage objects.
 * Stores the message type, content, and metadata for JSON serialization.
 */
public class SerializedMessage {
    private String type; // SYSTEM, USER, AI, TOOL_EXECUTION_RESULT
    private String content;

    // No-arg constructor for GSON deserialization
    public SerializedMessage() {
    }

    public SerializedMessage(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializedMessage that = (SerializedMessage) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, content);
    }

    @Override
    public String toString() {
        return "SerializedMessage{" +
               "type='" + type + '\'' +
               ", content='" + content + '\'' +
               '}';
    }
}
