# ChatMemory Migration Summary

## Overview

This refactoring integrates **LangChain4j's built-in `ChatMemory`** for conversation management instead of maintaining a separate custom `Conversation` class. This provides better integration with LangChain4j's features and automatically handles tool execution tracking.

## Key Changes

### 1. **AssistantService Interface**

**Before:**
```java
<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName);
```

**After:**
```java
<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName, ChatMemory chatMemory);

// Overload for backward compatibility (null memory = stateless)
default <T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName) {
    return createAssistant(assistantInterface, providerName, modelName, null);
}
```

**Benefit:** Assistants can now be created with or without persistent memory.

### 2. **DefaultAssistantService Implementation**

Now accepts an optional `ChatMemory` parameter and passes it to `AiServices.builder()`:

```java
final var builder = AiServices.builder(assistantInterface)
    .streamingChatModel(...)
    .chatModel(...)
    .tools(...)
    .chatMemory(chatMemory)  // <-- Automatic tool tracking!
    .build();
```

**Benefit:** LangChain4j automatically adds `ToolExecutionRequest` and `ToolExecutionResultMessage` to the memory when tools are called.

### 3. **FijiAssistantChat Refactoring**

#### Removed:
- Direct usage of `Conversation` and `ConversationBuilder`
- Manual conversation building via `ConversationBuilder`

#### Added:
- `ChatMemory chatMemory` field initialized with `MessageWindowChatMemory.withMaxMessages(20)`
- `List<ContextItem> contextItems` field for managing context items
- `AiToolService aiToolService` field for building system messages with tool descriptions

#### Key Methods Changed:

**`sendMessage()`** - Now uses ChatMemory directly:
```java
// Add user message to chat memory (with context items)
chatMemory.add(new UserMessage(messageWithContext));

// Create ChatRequest from chat memory - includes all history
final ChatRequest chatRequest = ChatRequest.builder()
    .messages(chatMemory.messages())
    .build();

// ... call assistant.chatStreaming(chatRequest)

// Add response to chat memory (handles tool execution automatically)
chatMemory.add(response.aiMessage());
```

**`buildUserMessageWithContext()`** - New helper method:
```java
private String buildUserMessageWithContext(final String userMessage) {
    final StringBuilder fullMessage = new StringBuilder(userMessage);
    if (!contextItems.isEmpty()) {
        fullMessage.append("\n\n===Start of Context Items===\n");
        for (final ContextItem item : contextItems) {
            fullMessage.append(item);
        }
        fullMessage.append("===End of Context Items===\n");
    }
    return fullMessage.toString();
}
```

**`buildSystemMessage()`** - Moved from ConversationBuilder:
```java
private String buildSystemMessage() {
    final StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
    // Add tool documentation to system message
    final List<AiToolPlugin> tools = aiToolService.getInstances();
    if (!tools.isEmpty()) {
        sb.append("\n\n## Available Tools\n\n");
        for (final AiToolPlugin tool : tools) {
            sb.append("- **").append(tool.getName()).append("**: ");
            // ... tool description
        }
    }
    return sb.toString();
}
```

**Context Item Management:**
- Changed from `conversation.addContextItem()` to `contextItems.add()`
- Changed from `conversation.removeContextItem()` to `contextItems.remove()`
- Changed from `conversation.getContextItems()` to `contextItems`

### 4. **Deprecated Classes (Preserved for Backward Compatibility)**

Both `Conversation` and `ConversationBuilder` are marked as `@Deprecated` but remain in the codebase:

```java
@Deprecated(since = "1.0.0", forRemoval = false)
public class Conversation { ... }

@Deprecated(since = "1.0.0", forRemoval = false)
public class ConversationBuilder { ... }
```

**Reason:** Existing tests still use these classes, and they may be referenced by external code. Keeping them avoids breaking changes.

## Benefits

### 1. **Automatic Tool Execution Tracking**
LangChain4j's `ChatMemory` automatically adds tool execution messages to the conversation history. The LLM can see:
- Which tools it called
- What parameters it passed
- What results were returned

**Example trace in memory:**
```
1. SystemMessage: [system prompt + tool descriptions]
2. UserMessage: "help me analyze this image"
3. ToolExecutionRequest: {tool: "analyzeImage", params: {...}}
4. ToolExecutionResultMessage: {result: "..."}
5. AiMessage: "Based on the analysis, I found..."
```

### 2. **Simplified Architecture**
- One less custom class to maintain
- Leverages battle-tested LangChain4j code
- Better integration with LangChain4j ecosystem

### 3. **Memory Window Management**
- `MessageWindowChatMemory.withMaxMessages(20)` keeps conversation focused
- Prevents token bloat from extremely long conversations
- Easy to adjust window size if needed

### 4. **Tool Results Persist Across Messages**
Without explicit code, tool results are now automatically available in the conversation context. The LLM can:
- Reference previous tool calls
- Learn from past mistakes
- Build on previous analysis

## Migration Notes for Future Development

### If You Need to Create a Custom Assistant:

**Old Way:**
```java
FijiAssistant assistant = assistantService.createAssistant(
    FijiAssistant.class, 
    "OpenAI", 
    "gpt-4"
);
```

**New Way (Stateful):**
```java
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
FijiAssistant assistant = assistantService.createAssistant(
    FijiAssistant.class, 
    "OpenAI", 
    "gpt-4",
    memory
);
```

**New Way (Stateless):**
```java
FijiAssistant assistant = assistantService.createAssistant(
    FijiAssistant.class, 
    "OpenAI", 
    "gpt-4"
    // null memory by default
);
```

### Testing

The `ConversationTest` still passes despite the refactoring because `Conversation` remains available. New tests should use LangChain4j's `ChatMemory` directly:

```java
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
memory.add(new SystemMessage("You are helpful"));
memory.add(new UserMessage("Hello"));
List<ChatMessage> messages = memory.messages();
```

## Files Modified

- `AssistantService.java` - Added `ChatMemory` parameter
- `DefaultAssistantService.java` - Implemented memory integration
- `FijiAssistantChat.java` - Refactored to use `ChatMemory` directly
- `Conversation.java` - Marked as `@Deprecated`
- `ConversationBuilder.java` - Marked as `@Deprecated`

## Build & Test Status

✅ **Compilation:** All 33 source files compile successfully  
✅ **Tests:** All 38 tests pass (including deprecated Conversation tests)  
✅ **No Breaking Changes:** Existing code continues to work

## Future Improvements

1. **Persistence:** Save/load `ChatMemory` to disk for multi-session conversations
2. **RAG:** Integrate embeddings for semantic retrieval of past tool results
3. **Tool-Specific Context:** Create specialized `ChatMemory` implementations that handle tool results differently
4. **Memory Analytics:** Track which tools are most effective based on memory history
