package sc.fiji.llm.service;

import org.scijava.service.SciJavaService;

/**
 * SciJava service for managing encrypted API keys for LLM providers.
 * This service handles secure storage and retrieval of API keys.
 */
public interface APIKeyService extends SciJavaService {

	/**
	 * Get the API key for the specified provider.
	 *
	 * @param providerName the name of the provider (e.g., "OpenAI", "Anthropic")
	 * @return the API key, or null if not set
	 */
	String getApiKey(String providerName);

	/**
	 * Set the API key for the specified provider.
	 *
	 * @param providerName the name of the provider
	 * @param apiKey the API key to store
	 */
	void setApiKey(String providerName, String apiKey);

	/**
	 * Check if an API key is configured for the specified provider.
	 *
	 * @param providerName the name of the provider
	 * @return true if an API key is configured, false otherwise
	 */
	boolean hasApiKey(String providerName);

	/**
	 * Remove the API key for the specified provider.
	 *
	 * @param providerName the name of the provider
	 */
	void removeApiKey(String providerName);

	/**
	 * Validate the API key for the specified provider by attempting a test connection.
	 *
	 * @param providerName the name of the provider
	 * @param apiKey the API key to validate
	 * @return true if the API key is valid, false otherwise
	 */
	boolean validateApiKey(String providerName, String apiKey);
}
