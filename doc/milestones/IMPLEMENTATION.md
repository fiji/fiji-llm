# Fiji LLM Assistant - Initial Implementation

## What Was Created

This initial implementation establishes the foundation for integrating LLMs into Fiji using the SciJava framework and LangChain4j.

### Package Structure

```
sc.fiji.llm/
├── service/          - SciJava services (stateless singletons)
│   ├── LLMService              - Manages LLM providers and creates chat models
│   ├── DefaultLLMService       - Default implementation
│   ├── APIKeyService           - Secure API key storage
│   ├── DefaultAPIKeyService    - Encrypted preferences storage
│   ├── LLMContextService       - Builds context from Fiji/ImageJ
│   └── DefaultLLMContextService - Default implementation
│
├── provider/         - LLM provider plugins
│   ├── LLMProviderPlugin       - Provider plugin interface
│   ├── OpenAIProviderPlugin    - OpenAI/ChatGPT integration
│   ├── AnthropicProviderPlugin - Anthropic/Claude integration
│   └── GeminiProviderPlugin    - Google Gemini integration
│
├── assistant/        - LangChain4j AI service interfaces
│   └── FijiAssistant           - Main assistant interface with chat, script generation, etc.
│
└── ui/              - UI components and examples
    └── LLMExample              - Example usage demonstrating the API
```

## Architecture

### SciJava Services (Stateless)
- **LLMService**: Factory for creating chat models and assistants
- **APIKeyService**: Encrypted storage of API keys
- **LLMContextService**: Gathers context from Fiji/ImageJ environment

### SciJava Plugins (Multiple Instances)
- **LLMProviderPlugin**: One plugin per LLM provider (OpenAI, Anthropic, Google)

### LangChain4j Integration
- **FijiAssistant**: Interface defining LLM capabilities
- LangChain4j generates the implementation automatically
- Supports: chat, streaming, script generation, code explanation, debugging

### Stateful Components (User-Created)
- UI code creates instances with conversation memory
- Each chat window has its own ChatMemory and assistant instance

## Usage Example

```java
// In your UI or command code
@Parameter
private LLMService llmService;

@Parameter
private LLMContextService contextService;

// Initialize
ChatLanguageModel model = llmService.createChatModel("OpenAI", "gpt-4o");
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
FijiAssistant assistant = llmService.createAssistant(FijiAssistant.class, model);

// Chat
String context = contextService.buildPluginContext();
String response = assistant.chat(context, "How do I segment cells?");

// Generate script
String script = assistant.generateScript("Groovy", "Open an image and apply Gaussian blur", context);

// Explain code
String explanation = assistant.explainCode(someCode, "Groovy");
```

## Next Steps

1. **Build the project**: Run `mvn clean install` to download LangChain4j dependencies
2. **Test the services**: Create unit tests for the services
3. **Create UI**: Build a chat window using the LLMExample as a guide
4. **Add context providers**: Enhance LLMContextService to gather more context
5. **Implement API key UI**: Create dialog for entering/managing API keys

## Notes

- Compilation errors are expected until `mvn install` downloads dependencies
- API key encryption is basic - consider platform-specific credential storage for production
- Context building is minimal - enhance as needed
- Currently uses Java 8 for SciJava compatibility
