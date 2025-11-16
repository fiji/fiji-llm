package sc.fiji.llm.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Utility class for converting between ChatMessage and SerializedMessage.
 */
public class ChatMessageConverter {

    /**
     * Converts a ChatMessage to a SerializedMessage for JSON persistence.
     */
    public static SerializedMessage toSerialized(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return new SerializedMessage("SYSTEM", ((SystemMessage) message).text());
        } else if (message instanceof UserMessage) {
            return new SerializedMessage("USER", ((UserMessage) message).singleText());
        } else if (message instanceof AiMessage) {
            return new SerializedMessage("AI", ((AiMessage) message).text());
        } else if (message instanceof ToolExecutionResultMessage) {
            return new SerializedMessage("TOOL_EXECUTION_RESULT",
                ((ToolExecutionResultMessage) message).text());
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
        }
    }

    /**
     * Converts a SerializedMessage back to a ChatMessage.
     */
    public static ChatMessage fromSerialized(SerializedMessage serialized) {
        switch (serialized.getType()) {
            case "SYSTEM":
                return new SystemMessage(serialized.getContent());
            case "USER":
                return new UserMessage(serialized.getContent());
            case "AI":
                return new AiMessage(serialized.getContent());
            case "TOOL_EXECUTION_RESULT":
                // For now, treat as AI message. This may need enhancement
                // if you need to preserve tool execution metadata
                return new AiMessage(serialized.getContent());
            default:
                throw new IllegalArgumentException("Unknown message type: " + serialized.getType());
        }
    }
}
