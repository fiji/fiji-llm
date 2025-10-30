# SciJava Script Editor - Usage Guide

> **Related files**: Start with `scijava-foundation.md` for shared SciJava concepts (plugin system, dependency injection). See also `scijava-common.md` for detailed service architecture.

## Overview

The SciJava Script Editor is a Swing-based script editor and interpreter for SciJava applications. It provides syntax highlighting, autocompletion, and execution capabilities for multiple scripting languages (Java, Groovy, JavaScript, Python, etc.).

**Key Technologies:**
- SciJava plugin framework (context-based dependency injection)
- RSyntaxTextArea (FifeSoft) for syntax highlighting and language support
- Java 8+ compatible

## Architecture

### SciJava Plugin System

The editor uses SciJava's annotation-based plugin architecture:

```java
@Plugin(type = Command.class, menu = {@Menu(label = "File"), @Menu(label = "New")})
public class ScriptEditor implements Command {
    @Parameter
    private Context context;  // Auto-injected
    
    @Override
    public void run() { /* ... */ }
}
```

**Key patterns:**
- `@Plugin(type = X.class)` - Registers classes as discoverable plugins
- `@Parameter` - Marks fields for context injection
- `context.inject(this)` - Manual injection when needed
- Plugin types include: `Command`, `Service`, `LanguageSupportPlugin`, `SyntaxHighlighter`, `AutoImporter`, `SearchActionFactory`

### Core Components

1. **TextEditor** - Main editor window
   - Manages tabbed interface, menu bar, file operations
   - Coordinates EditorPane, OutputPane, VarsPane
   - Entry point for most editor functionality

2. **EditorPane** - Individual text editing pane
   - Extends RSyntaxTextArea
   - Handles syntax highlighting per language
   - Manages bookmarks and error highlighting

3. **InterpreterPane/Window** - REPL interface for interactive scripting

4. **LanguageSupportService** - Manages language-specific features
   - Discovers `LanguageSupportPlugin` instances
   - Provides autocompletion via RSyntaxTextArea's LanguageSupport

### Service Access Pattern

Services are accessed via the Context:

```java
ScriptService scriptService = context.getService(ScriptService.class);
PluginService pluginService = context.getService(PluginService.class);
```

Common services: `ScriptService`, `PluginService`, `LogService`, `ModuleService`, `PrefService`

## Using the Script Editor

### Launching the Editor

Create a Context and TextEditor instance:

```java
Context context = new Context();
TextEditor editor = new TextEditor(context);
editor.setVisible(true);
```

### Language Support

The editor automatically detects and provides language-specific features based on file extension or syntax:
- Java, Groovy, JavaScript, Python, BeanShell, MATLAB, and more
- Syntax highlighting via `SyntaxHighlighter` plugins
- Autocompletion via `LanguageSupportPlugin` implementations

### Auto-Imports (Legacy Feature)

`AutoImporter` plugins provide default imports for scripts. This is a deprecated but maintained feature:

```java
@Plugin(type = AutoImporter.class)
public class MyAutoImporter implements AutoImporter {
    public Map<String, List<String>> getDefaultImports() {
        // Return map of package -> class list
    }
}
```

## Context Lifecycle Management

Always dispose contexts when windows close to prevent resource leaks:

```java
editor.addWindowListener(new WindowAdapter() {
    public void windowClosed(WindowEvent e) {
        context.dispose();
    }
});
```

## Integration Points

### SciJava Search Framework

`ScriptSourceSearchActionFactory` provides "View Source" actions for script modules in the SciJava search UI.

### OpenAI Integration

Experimental OpenAI features available in `PromptPane` and `OpenAIOptions`. API key configured via SciJava preferences system (`PrefService`).

### External Documentation

`ClassUtil.findDocumentationForClass()` provides access to javadoc URLs from:
- SciJava javadoc site (https://javadoc.scijava.org/)
- JAR manifest files (pom.xml URLs)
- GitHub/GitLab source repositories

## Dependencies

Key dependencies:
- `scijava-common` - Core SciJava framework
- `scijava-search` - Search integration
- `scripting-java` - Java scripting backend
- `rsyntaxtextarea` + `languagesupport` - Editor UI components
- `openai-gpt3-java` - OpenAI integration (experimental)

