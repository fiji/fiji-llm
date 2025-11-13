package sc.fiji.llm.provider;

import java.time.Duration;
import java.util.List;

import org.scijava.Disposable;
import org.scijava.Initializable;
import org.scijava.plugin.SingletonPlugin;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Plugin interface for LLM providers.
 * Each provider (OpenAI, Anthropic, Google, etc.) implements this interface
 * to provide access to their chat models.
 */
public interface LLMProvider extends SingletonPlugin, Initializable, Disposable {

	/** Default timeout duration for API calls */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

	/** Default maximum number of retries for API calls */
	public static int DEFAULT_MAX_RETRIES = 0;

	public static final String VALIDATION_FAILED = "sc.fiji.llm.provider.validation_failed";

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
	 * @return True if this model requires an API key (i.e. cloud-based models)
	 */
	default boolean requiresApiKey() {
		return true;
	}

	/**
	 * Get the URL where users can obtain an API key for this provider.
	 *
	 * @return URL to the API key page
	 */
	String getApiKeyUrl();

	/**
	 * Hook for when a model requires additional actions.
	 * This is a transformative action, allowing for descriptive identifiers attached
	 * to model names that require validation. (e.g. when downloading a remote model)
	 * 
	 * @param modelToValidate Name of the model for validation
	 * @return The validated model name, or {@link #VALIDATION_FAILED} if validation wasunsuccessful.
	 */
	default String validateModel(String modelToValidate) {
		return modelToValidate;
	}

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
