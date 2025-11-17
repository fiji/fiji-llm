# Fiji Large Language Model (LLM) Integration

Chat with AI assistants directly in Fiji to get help with image analysis, scripting, and general questions.

## Quick Start

1. **Install Fiji**: Download the `Latest` version from [imagej.net/software/fiji](https://imagej.net/software/fiji/)

2. **Add the Fiji-chat Update Site**:

   - See [instructions on adding update sites](https://imagej.net/update-sites/following#adding-unlisted-sites).
   - Add the (*currently unlisted*) `Fiji-chat` site: `https://sites.imagej.net/Fiji-chat/`
   - Restart Fiji afterwards

3. **Start chatting**: Use `Help > Assistants > Fiji Chat...` (shortcut: `ctrl + 0`)

## Users Guide

### Basic Concepts

**AI Service Providers** - Companies that provide cloud-based access to trained language models.

**Models** - Specific language models offered by a provider (e.g., GPT-4o, Claude 3.5 Sonnet). Different models have different capabilities and costs.

**Tokens** - The unit of exchange with a LLM: messages are sent and received as a series of "tokens".

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
  1. Download and install Ollama from [ollama.ai](https://ollama.com/download)
  2. (Optionally) Use the ollama UI or command line tool to download a model of interest.
  3. When you can start a new chat you can choose from compatible models, which will be downloaded as needed.
- **Recommended model(s)**:
  * `gpt-oss:20b`

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

For questions, bug reports, and feature requests, visit the [Image.sc Forum LLM tag](https://forum.image.sc/tag/llm). The Fiji community is active there and happy to help!

## Developers: Adding Functionality

Fiji LLM is built on SciJava's plugin architecture and has several key points of extension:

### LLMProvider

These plugins determine which AI Services are available in chat.

### ContextItemSupplier

`ContextItems` allow representation of the application environment to the LLM. Supplier plugins serve as a map from Fiji to a new context item.

### AiToolPlugin

These classes contain methods annotated with `langchain4j`'s `@Tool` annotation. New tools enable new functionality by the assistants.

### Chatbot Service

For developing chatbots in particular UI environments.