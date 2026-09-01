# Fiji Large Language Model (LLM) Integration

This project brings extensible, reproducible AI assistance into Fiji, helping scientists discover tools, build image analysis workflows, and connect with both local and external language models.

## Core Goals

* **Make Fiji More Accessible:** Help scientists discover relevant tools, learn unfamiliar workflows, and create reusable scripts through guided natural-language interactions.

* **Enable Reproducible Agentic Workflows:** Give AI agents structured access to Fiji’s application context and core capabilities, with an emphasis on generating familiar scripts and macros rather than de novo workflow formats.

* **Provide an Extensible AI Foundation:** Establish shared extension points for agentic tools and model connectivity so that developers can add new capabilities without creating isolated or incompatible integrations.

* **Support Private and Equitable Model Access:** Make local, open-weight models a first-class option while retaining flexible connectivity to external model providers and AI clients.

## Key Architecture and Components

* **In-App Chat Interface:** Provides scientists with guided AI assistance directly inside Fiji.

* **Model Context Protocol (MCP) Server:** Exposes Fiji’s agentic tools to compatible external assistants, development environments, and other MCP clients.

* **Annotated Tool Registry:** Uses the SciJava plugin framework to dynamically discover and register capabilities that developers expose to AI agents.

* **Extensible Model Engine:** Separates model connectivity from agentic functionality, allowing new local or remote model providers to be added through plugins.

* **Context-Aware Analysis Tools:** Give agents structured access to Fiji’s environment, including installed commands, open images, analysis metadata, the Script Editor, and macro recorder.

## Table of Contents
- [Quick Start](#quick-start)
- [MCP Server](#mcp-server)
  - [VS Code](#vs-code)
- [User Guide](#user-guide)
  - [Basic Concepts](#basic-concepts)
  - [Supported AI Providers](#supported-ai-providers)
  - [General Work Flow](#general-work-flow)
  - [Using Tools](#using-tools)
  - [Tips for Better Results](#tips-for-better-results)
  - [Getting Help](#getting-help)
- [Developers: Adding Functionality](#developers-adding-functionality)
  - [LLMProvider](#llmprovider)
  - [ContextItemSupplier](#contextitemsupplier)
  - [AiToolPlugin](#aitoolplugin)
  - [ChatbotService](#chatbotservice)

## Quick Start

1. **Install Fiji**: Download the `Latest` version from [imagej.net/software/fiji](https://imagej.net/software/fiji/)

2. **Add the Fiji-chat Update Site**:
   - See [instructions on adding unlisted update sites](https://imagej.net/update-sites/following#adding-unlisted-sites).
   - Add the (*currently unlisted*) `Fiji-chat` site: `https://sites.imagej.net/Fiji-chat/`
   - Restart Fiji afterwards

3. **Open the chat**: Run `Help > Assistants > Fiji Chat...` (shortcut: `ctrl + 0`)
	- The first time you run `Fiji Chat...` you will see a landing page where you select an AI Service and model.
    - Future `Fiji Chat...` runs will go right to chatting with your last selected service and model.

4. **Select a model**:

	**Option A: Local Models (Recommended for beginners)**
	- These are open models which run on your machine.
	- Download and install [Ollama](https://ollama.com/download) now (no Fiji restart needed)
	- We recommend starting with the curated `Gemma4 - small (Ollama)` service, which auto-selects a model aimed at maximizing hardware compatibility.
	- The general `Ollama` service allows for full model exploration.
	- Click `OK` to start chatting. You will need to wait for the selected model to download (only on first chat with a model)

	**Option B: Cloud Models**
	- If you have an account with an AI service (Gemini, Claude, ChatGPT, etc.) you can use it with Fiji chat. Most services require paid subscriptions or credits for this function.
	- Select your provider, then choose an available model. Different models have different pricing schemes.
	- Click `OK` to start chatting. You will be prompted for an API key and provided with a link to your provider's key page (only on first chat with a remote provider)

	**Want to switch later?** Use the ⚙️ button in chat to change models and/or providers.

## MCP Server

All LLM tools in Fiji are accessed via an [MCP Server](https://en.wikipedia.org/wiki/Model_Context_Protocol). While we provide a basic, integrated chat interface, this also allows external applications ("harnesses") to interact with Fiji through this local server.

Currently, the MCP server is tied to a running Fiji application - which is where any tools will execute. When Fiji and the MCP server are running, it can be accessed at `http://localhost:9090/mcp` (note the default port 9090)

**Available Configuration**
- **Set Port**: Use `Help > Assistants > Manage MCP Server...` or preferences key `sc.fiji.mcp.port`
- **Start Manually**: Click "Start Server" in the Manage MCP Server dialog
- **Auto-Launch**: Enable `Launch MCP on Startup` in the Manage MCP Server dialog, or set preferences key `sc.fiji.mcp.launchOnStartup` to true

### VS Code

You can connect your VS Code LLMs to the Fiji MCP server! This allows your agents to run tasks in a local Fiji. Edit your `mcp.json` and add the following entry:

```json
		"fiji-mcp": {
			"type": "http",
			"url": "http://localhost:9090/mcp",
			"startupMode": "onDemand"
		},
```

In the `Configure Tools` dialog, you should see a new `fiji-mcp-server` option that you can toggle on or off.

**NB**: Update the port in `mcp.json` as necessary
**NB**: Your local Fiji application must be running first for the MCP server to be findable by VS Code. For best results, (re)start the server from `mcp.json` after launching Fiji.

## User Guide

### Basic Concepts

**AI Service Providers** - Companies that provide cloud-based access to trained language models.

**Models** - Specific language models offered by a provider (e.g., GPT-4o, Claude 3.5 Sonnet). Different models have different capabilities and costs.

**Tokens** - The unit of operation within an LLM: messages to and from the chatbot are encoded as a series of "tokens". Longer messages require more tokens. Importantly, any *actions taken* by the LLM in response to your message will use tokens. (such as editing a script or running a command)

**API Keys** - Credentials that authenticate you with an AI service provider. Often require per-token pay-as-you-go or a subscription plan.

**Conversations** - Your chat history with an AI assistant, independent of model. Conversations are saved locally and loaded when Fiji starts, so you can continue working where you left off. In long conversations, the model may not "see" the whole chat history.

**Context Items** - Information you can attach to chat message that helps the assistant understand your Fiji environment. For example, you could attach an open image or script.

### Supported AI Providers

#### Google (Gemini)
- **Note:** Gemini is currently the only supported provider that provides API Keys at no charge.
- Using a "free" API Key is subject to Google's rate limits and availability. It is suitable for testing and assessment, but not regular use.
- **Getting an API Key**:
  1. Visit [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
  2. Click **Create API key** and copy it

#### Ollama (Local Models Only)
- **Note:** Ollama is a general gateway to pretrained models. Using local models bypasses the need for API keys or token considerations. *However*, running a local LLM can require significant resources (RAM, GPU, hard drive, power).
- Models typically come in varieants (`7b`, `20b`, etc...), indicating the number of model parameters (in billions). More parameters means a better ability to conceptualize solutions, but also more resource use.-
- Fiji-chat is intended for use with models that support [Tool Use](https://ollama.com/search?c=tools).
- **Installation**:
  1. Download and install [Ollama](https://ollama.com/download)
  2. (Optionally) Use the ollama UI or command line tool to download a model of interest.
  3. When you can start a new chat you can choose from compatible models, which will be downloaded as needed.
- **Recommended model(s)**:
  * `Gemma4` - Depending on your available video memory, select `small` (8GB), `medium` (16GB), or `large` (24GB or more).

#### Anthropic (Claude)
- **Getting an API Key**:
  1. Create an account at [console.anthropic.com](https://console.anthropic.com)
  2. Go to **Account settings > API keys** (or [click here](https://console.anthropic.com/settings/keys))
  3. Click **Create Key** and copy it

#### OpenAI (ChatGPT)
- **Getting an API Key**:
  1. Create an account at [platform.openai.com](https://platform.openai.com)
  2. Go to **Account settings > API keys** (or [click here](https://platform.openai.com/api-keys))
  3. Click **Create new secret key** and copy it

### General Work Flow

1. **Launch the Chat**: Run `Help > Assistants > Fiji Chat...`
2. **Select a Provider**: Choose your preferred AI service
3. **Select a Model**: Pick a specific model. If an API Key is required and not found, you will be prompted automatically.
4. **Start Chatting**: Type your question or request in the input box
5. **Attach Context** (optional): Use the context buttons to provide relevant information from your Fiji environment

You can use `Help > Assistants > Manage API Keys...` to manage your key(s) at any time.

### Using Tools

The benefit of having an assistant integrated into Fiji is that it can *perform actions*, beyond just conversation:

**Script Writing** - Ask the assistant to write scripts in Python, Groovy, JavaScript, or other SciJava-compatible languages. Describe the context of your analysis task and the assistant can generate executable scripts.

**Script Editing** - Attach scripts as context and ask the assistant to improve, debug, or adapt them for your specific needs.

**Macro Recording** - Ask the assistant for help creating ImageJ macros for guidance to relevant commands and plugins.

**General Information** - Describe your image analysis goals and discuss options available in your Fiji environment.

### Tips for Better Results

- **Be Specific**: Describe your task in detail. Include what you're trying to analyze, what tools you've already tried, and what's not working.
- **Provide Context**: Use the context buttons to share relevant images, open scripts, or previous conversation history.
- **Iterate**: LLM responses aren't always perfect on the first try. Review the output, provide feedback, and ask follow-up questions.

### Getting Help

A help button `( ? )` in the chat window provides in-app explanations of the UI and how to use each feature.

For questions, bug reports, and feature requests, visit the [Image.sc Forum](https://forum.image.sc/tag/llm). The Fiji community is active there and happy to help!

## Developers: Adding Functionality

This project brings provides [langchain4j](https://docs.langchain4j.dev/) integration to the SciJava's plugin framework. There are several key points of extension:

### [LLMProvider](src/main/java/sc/fiji/llm/provider/LLMProvider.java)

Determine which AI Services are available in chat.

### [ContextItemSupplier](src/main/java/sc/fiji/llm/chat/ContextItemSupplier.java)

Provide a mapping from the Fiji application environment to [`ContextItems`](src/main/java/sc/fiji/llm/chat/ContextItem.java), facilitating deeper understanding by the LLM.

### [AiToolPlugin](src/main/java/sc/fiji/llm/tools/AiToolPlugin.java)

These plugins contain methods annotated with `langchain4j`'s [`@Tool`](https://github.com/langchain4j/langchain4j/blob/main/langchain4j-core/src/main/java/dev/langchain4j/agent/tool/Tool.java) annotation. New tools enable code to be run by the AI assistants.

### [ChatbotService](src/main/java/sc/fiji/llm/ui/ChatbotService.java)

For developing chatbots in particular UI environments.

### [MCPService](src/main/java/sc/fiji/llm/mcp/MCPService.java)

An MCP (Model Context Protocol) server exposes all registered `AiToolPlugin` implementations via local HTTP, making them accessible to external clients.

