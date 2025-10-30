package sc.fiji.llm.provider;

import java.util.List;

import org.scijava.plugin.SciJavaPlugin;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Plugin interface for LLM providers.
 * Each provider (OpenAI, Anthropic, Google, etc.) implements this interface
 * to provide access to their chat models.
 */
public interface LLMProviderPlugin extends SciJavaPlugin {

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
	 * This method will attempt to query the provider's API for the current list of models.
	 * If the query fails or is not supported, it falls back to a hard-coded list.
	 *
	 * @param apiKey optional API key for authentication (may be null if not needed)
	 * @return list of model names
	 */
	List<String> getAvailableModels(String apiKey);

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
	 * Create a chat language model with the specified API key and model name.
	 *
	 * @param apiKey the API key for authentication
	 * @param modelName the name of the model to use
	 * @return a configured chat language model
	 */
	ChatModel createChatModel(String apiKey, String modelName);

	/**
	 * Create a streaming chat language model with the specified API key and model name.
	 *
	 * @param apiKey the API key for authentication
	 * @param modelName the name of the model to use
	 * @return a configured streaming chat language model
	 */
	StreamingChatModel createStreamingChatModel(String apiKey, String modelName);
}
