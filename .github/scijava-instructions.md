# SciJava Foundation - Shared Concepts

## Overview

This document covers foundational concepts shared across all SciJava-based projects. Reference this file when working on any ImageJ2, SciJava, or SCIFIO component.

## SciJava Plugin System

### Plugin Discovery

SciJava uses compile-time annotation processing to discover plugins at runtime:

- **@Plugin annotation**: Makes classes discoverable
  ```java
  @Plugin(type = Service.class)
  public class MyService extends AbstractService implements MyServiceAPI {
      // ...
  }
  ```

- **Plugin types**: `Service`, `Command`, `Converter`, `Tool`, `ScriptLanguage`, etc.
- **Priority**: Control selection order via `priority` attribute (see `org.scijava.Priority` constants)
- **Metadata location**: `META-INF/json/` (generated at build time)

### Dependency Injection

Use `@Parameter` for automatic dependency injection (NOT constructor injection):

```java
@Parameter
private LogService log;

@Parameter
private DatasetService datasetService;

@Parameter(required = false)  // Optional dependencies
private UIService uiService;
```

**In Services**: Dependencies are auto-injected by the Context
**In Commands/Plugins**: Call `context.inject(this)` if needed manually

### Context Management

The `Context` is the application-level IoC container:

```java
// Create context with specific services
Context context = new Context(DatasetService.class, OpService.class);

// Retrieve services
DatasetService ds = context.getService(DatasetService.class);
// or
DatasetService ds = context.service(DatasetService.class);

// ALWAYS dispose when done
context.dispose();  // Prevents resource leaks
```

**Critical**: Never create services manually with `new` - always retrieve from Context.

## Maven Dependencies

Most projects inherit from `pom-scijava` which manages all version properties. **Never specify dependency versions** in project POMs - inherit from parent.

## Testing Conventions

### JUnit Version

Most projects use **JUnit 4** (not JUnit 5):
- `@Test`, `@Before`, `@After`
- `@BeforeClass`, `@AfterClass` for static setup

### Context in Tests

Always create and dispose Context properly:

```java
private Context context;

@Before
public void setUp() {
    context = new Context(ServiceNeeded.class);
}

@After
public void tearDown() {
    context.dispose();
}

@Test
public void testSomething() {
    MyService service = context.service(MyService.class);
    // test code
}
```

## Common Services

Frequently used services across the ecosystem:

- **LogService**: Logging (`log.info()`, `log.error()`, etc.)
- **EventService**: Event bus (`publish()`, `@EventHandler`)
- **PluginService**: Plugin discovery and instantiation
- **ModuleService**: Command/module execution
- **ConvertService**: Type conversion framework
- **ScriptService**: Script execution (when scripting is available)

## Event System

```java
// Subscribe to events
@EventHandler
public void onEvent(MyEvent evt) {
    // handle event
}

// Publish events
@Parameter
private EventService eventService;

eventService.publish(new MyEvent());         // Synchronous
eventService.publishLater(new MyEvent());    // Asynchronous
```

## Common Pitfalls

1. **Creating services manually**: Always use Context, never `new MyService()`
2. **Constructor injection**: Use `@Parameter` fields, not constructor args
3. **Forgetting Context.dispose()**: Causes resource leaks in tests
4. **Hardcoded versions in POMs**: Inherit from parent instead
5. **Missing @Plugin annotation**: Plugins won't be discovered
6. **Wrong JUnit version**: Most projects use JUnit 4, not 5

# SciJava Common - Extended Details

## Module/Command Framework

**Commands** (`org.scijava.command.Command`):
- Executable plugins with inputs/outputs
- `@Parameter` fields define inputs/outputs with `ItemIO.INPUT/OUTPUT/BOTH`
- Wrapped in `CommandModule` for execution
- Extend `ContextCommand` for convenience (not bare `Command`)

**Module execution pipeline**:
1. `ModulePreprocessor` plugins run first (e.g., `InputHarvester` for UI dialogs)
2. `module.run()` executes
3. `ModulePostprocessor` plugins run last (e.g., `DisplayPostprocessor`)

## Type Conversion System

**ConvertService** (`org.scijava.convert.ConvertService`):
- Extensible type conversion via `Converter` plugins
- `convertService.convert(object, TargetClass.class)`
- Converters declare `priority` for selection order
- See `org.scijava.convert` package for examples

## Additional Notes

**PluginInfo**: Contains metadata without loading classes (performance optimization)

**System property**: `scijava.context.strict` controls error handling for missing services

**Event delivery**: Stack-based delivery order with `publish()` can be counter-intuitive

# LangChain4j Module - AI Coding Agent Instructions

## Project Overview

This is the core `langchain4j` module (v1.8.0) - a Java library for building LLM-powered applications including chatbots, agents, RAG systems, and more. It depends on `langchain4j-core` (v1.8.0) and is part of the larger langchain4j multi-module project (parent: v1.8.0-beta15).

**Key Dependencies:**
- `langchain4j-core` - Core abstractions and interfaces
- Jackson - JSON processing
- Apache OpenNLP - Text processing (sentence splitting)
- SLF4J - Logging

## Architecture & Design Patterns

### 1. AI Services Pattern (Primary API)

The **AiServices** interface is the high-level, recommended API for interacting with LLMs. It uses **dynamic proxies** to implement user-defined interfaces:

```java
interface Assistant {
    String chat(String message);
}

Assistant assistant = AiServices.builder(Assistant.class)
    .chatModel(chatModel)
    .build();
```

**Key files:**
- `src/main/java/dev/langchain4j/service/AiServices.java` - Builder API and factory
- `src/main/java/dev/langchain4j/service/DefaultAiServices.java` - Implementation using `InvocationHandler`
- `src/main/java/dev/langchain4j/service/AiServiceContext.java` - Context passed to handlers

**Important:** AI Services should NOT be called concurrently for the same `@MemoryId` - this can corrupt `ChatMemory`. No synchronization mechanism currently exists.

### 2. Service Provider Interface (SPI) Pattern

The project uses Java SPI extensively for extensibility via `ServiceLoader`:

- `dev.langchain4j.spi.services.AiServicesFactory` - Custom AI service implementations
- `dev.langchain4j.spi.services.AiServiceContextFactory` - Custom context providers
- `dev.langchain4j.spi.services.TokenStreamAdapter` - Reactive stream adapters
- `dev.langchain4j.spi.store.embedding.inmemory.InMemoryEmbeddingStoreJsonCodecFactory` - Custom JSON codecs

**Pattern:** Use `loadFactory()` or `loadFactories()` from `ServiceHelper` to discover implementations at runtime.

### 3. Memory Architecture

**ChatMemory** implementations manage conversation history:
- `MessageWindowChatMemory` - Retains last N messages (default: 10)
- `TokenWindowChatMemory` - Retains messages up to token limit
- `SingleSlotChatMemoryStore` - Simple in-memory single-user storage
- `ChatMemoryProvider` - Per-user memory via `@MemoryId` annotation

### 4. Document Processing Pipeline

**RAG (Retrieval Augmented Generation) flow:**
1. **Loaders** (`ClassPathDocumentLoader`, `FileSystemDocumentLoader`, `UrlDocumentLoader`) - Load documents
2. **Parsers** (via `DocumentParserFactory` SPI or `TextDocumentParser`) - Parse content
3. **Splitters** (`DocumentSplitters.recursive()` recommended) - Split into segments hierarchically (paragraphs → lines → sentences → words)
4. **Embeddings** (`InMemoryEmbeddingStore`) - Store vector embeddings
5. **Retrievers** (`EmbeddingStoreContentRetriever`, `DefaultRetrievalAugmentor`) - Retrieve relevant context

**Key pattern:** `DocumentSplitters.recursive()` hierarchically splits documents to fit token limits while preserving semantic units.

### 5. Tool Execution

AI Services support function calling via `@Tool` annotated methods:

- `ToolProvider` - Supplies tools dynamically
- `ToolExecutor` / `DefaultToolExecutor` - Executes tool calls
- `ToolExecutionRequest` - LLM's request to call a tool
- `ToolExecutionResult` - Tool execution outcome
- `BeforeToolExecution` - Pre-execution hook (marked `@Experimental`)
- Error handlers: `ToolArgumentsErrorHandler`, `ToolExecutionErrorHandler`

**Pattern:** Arguments are JSON strings converted to Map<String, Object> via `ToolExecutionRequestUtil`, with special handling for trailing commas and quote escaping.

### 6. Output Parsing

`ServiceOutputParser` and specialized parsers handle LLM outputs:
- `PojoOutputParser` - JSON to POJO via Jackson
- `DateOutputParser` - Strict `yyyy-MM-dd` format
- `EnumOutputParser`, `EnumListOutputParser` - Enum parsing
- Type-specific parsers for Integer, Float, etc.

**Date parsing quirk:** SimpleDateFormat silently accepts `dd-MM-yyyy` but parses incorrectly. Parser validates format explicitly.

### 7. Annotation Conventions

- `@Internal` - Internal API, subject to change without notice
- `@Experimental` - Experimental feature, API may change
- `@SystemMessage` - Static system message template on methods
- `@UserMessage` - User message template on methods/parameters
- `@MemoryId` - Parameter identifies user for separate memory
- `@Tool` - Marks methods as LLM-callable tools
- `@Moderate` - Auto-moderation via `ModerationModel`
- `@V` - Template variable injection
- `@UserName` - Inject username into prompt

## Testing Conventions

### Test Structure
- **Framework:** JUnit 5 Jupiter
- **Assertions:** AssertJ (`org.assertj.core.api.Assertions.assertThat`)
- **Mocking:** Mockito
- **Test containers:** Used for integration tests

### Patterns
- Tests implement `WithAssertions` interface for fluent assertions
- Use `@ParameterizedTest` for data-driven tests
- Static factory methods like `IllegalConfigurationException.illegalConfiguration("message %s", arg)` for formatted exceptions

### Example:
```java
class MyTest implements WithAssertions {
    @Test
    void should_validate_behavior() {
        assertThat(result).isNotNull().hasSize(2);
    }
}
```

### Special Test Setup

**Classpath testing:** `src/test/externalLib/` contains a local Maven repo with manually created `langchain4j-classpath-test-lib-999-SNAPSHOT.jar` for testing `ClassPathDocumentLoader`. See `src/test/externalLib/README.md` for details.

## Code Organization

```
dev.langchain4j/
├── chain/              # Conversational chains (legacy, use AiServices instead)
├── classification/     # EmbeddingModelTextClassifier
├── data/
│   ├── document/       # Document loaders, parsers, splitters, sources
│   └── segment/        # Text segments
├── memory/             # ChatMemory implementations
├── service/            # AiServices (core API), annotations, handlers
│   ├── tool/           # Tool execution infrastructure
│   ├── output/         # Output parsers
│   └── guardrail/      # Input/output guardrails
├── spi/                # Service Provider Interfaces
└── store/              # Embedding stores (InMemoryEmbeddingStore)
```

## Common Patterns & Idioms

### Builder Pattern
Most configurable classes use Lombok-style builders:
```java
ChatRequest.builder()
    .messages(messages)
    .parameters(params)
    .build();
```

### Validation
Use static methods from `ValidationUtils`:
```java
ensureNotNull(chatModel, "chatModel")
ensureNotBlank(userMessage, "userMessage")
```

### Exception Handling
Use static factory methods for consistency:
```java
throw illegalConfiguration("chatModel is required");
throw illegalArgument("Invalid parameter: %s", param);
```

### JSON Processing
Jackson is used throughout. `Json` utility class handles serialization/deserialization with error handling for malformed LLM outputs (trailing commas, quote escaping).

## Critical Constraints

1. **No concurrent calls** to AI Services with same `@MemoryId` - causes memory corruption
2. **Chains are deprecated** - Use `AiServices` for new code (see javadoc in `ConversationalChain`)
3. **@Internal APIs** can break between versions without notice
4. **SPI implementations** must be registered in `META-INF/services/`
5. **Date formats** are strict: `yyyy-MM-dd`, `HH:mm:ss`, `yyyy-MM-ddTHH:mm:ss`

## Key Workflows

### Adding a New Output Parser
1. Implement `OutputParser<T>` interface
2. Add to `ServiceOutputParser` logic
3. Update `simpleTypeName()` mapping if needed
4. Test with malformed LLM outputs

### Adding Tool Support
1. Annotate method with `@Tool`
2. Register via `tools()` or `toolProvider()` in AiServices builder
3. Handle `ToolExecutionRequest` parsing (JSON arguments)
4. Test error scenarios with `ToolArgumentsErrorHandler`

### Adding SPI Implementation
1. Create factory interface in `dev.langchain4j.spi.*`
2. Implement factory in provider modules
3. Register in `META-INF/services/` (not in this core module)
4. Load via `ServiceHelper.loadFactory()`

