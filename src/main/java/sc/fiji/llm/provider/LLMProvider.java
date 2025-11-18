/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
	 * @return The base {@link ChatRequestParameters} recommended for this provider
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
