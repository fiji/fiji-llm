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

import org.scijava.Disposable;
import org.scijava.Initializable;
import org.scijava.plugin.SingletonPlugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

/**
 * Plugin interface for LLM providers. Each provider (OpenAI, Anthropic, Google,
 * etc.) implements this interface to provide access to their chat models.
 */
public interface LLMProvider extends SingletonPlugin, Initializable,
	Disposable
{

	/** Default timeout duration for API calls */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

	/** Default maximum number of retries for API calls */
	public static int DEFAULT_MAX_RETRIES = 0;

	public static final String VALIDATION_FAILED =
		"sc.fiji.llm.provider.validation_failed";

	/**
	 * @return True if this model requires an API key (i.e. cloud-based models)
	 */
	default boolean requiresApiKey() {
		return true;
	}

	/**
	 * Hook for when a model requires additional actions. This is a transformative
	 * action, allowing for descriptive identifiers attached to model names that
	 * require validation. (e.g. when downloading a remote model)
	 *
	 * @param modelToValidate Name of the model for validation
	 * @return The validated model name, or {@link #VALIDATION_FAILED} if
	 *         validation wasunsuccessful.
	 */
	default String validateModel(String modelToValidate) {
		return modelToValidate;
	}

	/**
	 * @return The base {@link ChatRequestParameters} recommended for this
	 *         provider
	 */
	default ChatRequestParameters defaultChatRequestParameters() {
		return ChatRequestParameters.builder().frequencyPenalty(0.0)
			.presencePenalty(0.0).temperature(0.1).build();
	}

	/**
	 * Get the name of this provider.
	 *
	 * @return the provider name (e.g., "OpenAI", "Anthropic", "Google")
	 */
	String getName();

	/**
	 * Get a description of this provider.
	 *
	 * @return a human-readable description
	 */
	String getDescription();

	/**
	 * Get the list of available models for this provider.
	 *
	 * @return list of model names
	 */
	List<String> getAvailableModels();

	/**
	 * Get the URL to the provider's models documentation.
	 *
	 * @return URL to the models documentation page
	 */
	String getModelsDocumentationUrl();

	/**
	 * Get the URL where users can obtain an API key for this provider.
	 *
	 * @return URL to the API key page
	 */
	String getApiKeyUrl();

	/**
	 * @param apiKey the API key for authentication
	 * @return A {@link TokenWindowChatMemory} appropriate for the specified
	 *         model, or {@code null} if not supported.
	 */
	TokenWindowChatMemory createTokenChatMemory(String modelName);

	/**
	 * Create a chat language model with the specified API key and model name.
	 *
	 * @param apiKey the API key for authentication
	 * @param modelName the name of the model to use
	 * @return a configured chat language model
	 */
	ChatModel createChatModel(String modelName);

	/**
	 * Create a streaming chat language model with the specified API key and model
	 * name.
	 *
	 * @param apiKey the API key for authentication
	 * @param modelName the name of the model to use
	 * @return a configured streaming chat language model
	 */
	StreamingChatModel createStreamingChatModel(String modelName);
}
