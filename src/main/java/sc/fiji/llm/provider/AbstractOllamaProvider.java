/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.List;

import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

/**
 * Abstract base class for Ollama-based providers. Handles common functionality
 * like process management, client model creation, and token counting.
 */
public abstract class AbstractOllamaProvider implements LLMProvider {

	protected static final String LOCAL_SERVER_URL = "http://localhost:11434";
	protected static final Double DEFAULT_TEMPERATURE = 0.1;
	protected static final Integer DEFAULT_TOKEN_WINDOW = 40000;
	protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
	protected static final String REMOTE_STRING = "* (remote)";

	protected OllamaProcessManager processManager;

	@Parameter
	protected LogService logService;

	@Parameter
	protected UIService uIService;

	@Parameter
	protected StatusService statusService;

	protected AbstractOllamaProvider() {
		this.processManager = new OllamaProcessManager();
	}

	@Override
	public String getDescription() {
		return "Local Ollama model: " + getName();
	}

	@Override
	public ChatRequestParameters defaultChatRequestParameters() {
		return ChatRequestParameters.builder().temperature(DEFAULT_TEMPERATURE)
			.build();
	}

	@Override
	public boolean requiresApiKey() {
		return false;
	}

	@Override
	public String getApiKeyUrl() {
		return "";
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ollama.com/download";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(DEFAULT_TOKEN_WINDOW,
			new OllamaTokenCountEstimator());
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return OllamaChatModel.builder().baseUrl(LOCAL_SERVER_URL).modelName(
			modelName).timeout(DEFAULT_TIMEOUT).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return OllamaStreamingChatModel.builder().baseUrl(LOCAL_SERVER_URL)
			.modelName(modelName).timeout(DEFAULT_TIMEOUT).build();
	}

	@Override
	public void initialize() {
		if (processManager.isServerRunning()) {
			return;
		}
		processManager.startServer();
	}

	@Override
	public void dispose() {
		processManager.shutdown();
	}

	/**
	 * Check if a model string represents a remote model (not yet downloaded).
	 *
	 * @param model the model name to check
	 * @return true if the model has the remote suffix
	 */
	protected boolean isRemoteModel(String model) {
		return model.endsWith(REMOTE_STRING);
	}

	/**
	 * Append the remote suffix to a model name.
	 *
	 * @param model the model name
	 * @return the model name with remote suffix appended
	 */
	protected String appendRemoteString(String model) {
		return model + REMOTE_STRING;
	}

	/**
	 * Get the list of all available local (installed) models from Ollama.
	 *
	 * @return list of available local model names
	 */
	protected List<String> getAvailableLocalModels() {
		return processManager.getInstalledModels();
	}

	/**
	 * Validate and possibly download a model. If the model string ends with the
	 * remote suffix, prompts the user for confirmation and downloads the model.
	 *
	 * @param modelToValidate the model to validate
	 * @return the model name without remote suffix if successful, VALIDATION_FAILED
	 *         if user cancels, or the original string if not a remote model
	 */
	@Override
	public String validateModel(String modelToValidate) {
		if (!isRemoteModel(modelToValidate)) {
			// Not a remote model, no validation needed
			return modelToValidate;
		}

		if (uIService.showDialog(
			"The selected LLM model will be downloaded. This could take some time.\nProceed?",
			MessageType.WARNING_MESSAGE, OptionType.OK_CANCEL_OPTION).equals(
				Result.OK_OPTION))
		{
			String modelName = modelToValidate.substring(0, modelToValidate
				.length() - REMOTE_STRING.length());
			statusService.showStatus(-1, -1, "Downloading Ollama model: " +
				modelName);
			try {
				processManager.pullModel(modelName);
			}
			catch (Exception e) {
				statusService.clearStatus();
				statusService.showStatus("Download failed: " + modelName);
				// Failed to pull
				return modelToValidate;
			}
			statusService.clearStatus();
			statusService.showStatus("Download complete: " + modelName);
			// Pull successful
			return modelName;
		}
		else {
			return LLMProvider.VALIDATION_FAILED;
		}
	}

	/**
	 * Simplified copy/paste from OpenAiTokenCountEstimator, without consideration
	 * for model name.
	 */
	protected static class OllamaTokenCountEstimator implements
		TokenCountEstimator
	{

		@Override
		public int estimateTokenCountInText(String text) {
			return text.length() / 4;
		}

		@Override
		public int estimateTokenCountInMessage(ChatMessage message) {
			int tokenCount = 1; // 1 token for role
			tokenCount += 3; // extra tokens per each message

			if (message instanceof SystemMessage) {
				tokenCount += estimateTokenCountIn((SystemMessage) message);
			}
			else if (message instanceof UserMessage) {
				tokenCount += estimateTokenCountIn((UserMessage) message);
			}
			else if (message instanceof AiMessage) {
				tokenCount += estimateTokenCountIn((AiMessage) message);
			}
			else if (message instanceof ToolExecutionResultMessage) {
				tokenCount += estimateTokenCountIn(
					(ToolExecutionResultMessage) message);
			}
			else {
				throw new IllegalArgumentException("Unknown message type: " + message);
			}

			return tokenCount;
		}

		private int estimateTokenCountIn(SystemMessage systemMessage) {
			return estimateTokenCountInText(systemMessage.text());
		}

		private int estimateTokenCountIn(UserMessage userMessage) {
			int tokenCount = 0;

			for (Content content : userMessage.contents()) {
				if (content instanceof TextContent) {
					tokenCount += estimateTokenCountInText(((TextContent) content)
						.text());
				}
				else {
					throw new IllegalArgumentException("Unknown content type: " +
						content);
				}
			}

			if (userMessage.name() != null) {
				tokenCount += 1; // extra tokens per name
				tokenCount += estimateTokenCountInText(userMessage.name());
			}

			return tokenCount;
		}

		@Override
		public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
			// see
			// https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb

			int tokenCount = 3; // every reply is primed with
				// <|start|>assistant<|message|>
			for (ChatMessage message : messages) {
				tokenCount += estimateTokenCountInMessage(message);
			}
			return tokenCount;
		}

		private int estimateTokenCountIn(AiMessage aiMessage) {
			int tokenCount = 0;

			if (aiMessage.text() != null) {
				tokenCount += estimateTokenCountInText(aiMessage.text());
			}

			if (aiMessage.hasToolExecutionRequests()) {
				tokenCount += 6;
				if (aiMessage.toolExecutionRequests().size() == 1) {
					tokenCount -= 1;
					ToolExecutionRequest toolExecutionRequest = aiMessage
						.toolExecutionRequests().get(0);
					tokenCount += estimateTokenCountInText(toolExecutionRequest.name()) *
						2;
					tokenCount += estimateTokenCountInText(toolExecutionRequest
						.arguments());
				}
				else {
					tokenCount += 15;
					for (ToolExecutionRequest toolExecutionRequest : aiMessage
						.toolExecutionRequests())
					{
						tokenCount += 7;
						tokenCount += estimateTokenCountInText(toolExecutionRequest.name());

						String arguments = toolExecutionRequest.arguments();
						if (arguments == null || arguments.isEmpty()) {
							continue;
						}
						tokenCount += estimateTokenCountInText(arguments);
					}
				}
			}

			return tokenCount;
		}

		private int estimateTokenCountIn(
			ToolExecutionResultMessage toolExecutionResultMessage)
		{
			return estimateTokenCountInText(toolExecutionResultMessage.text());
		}
	}
}
