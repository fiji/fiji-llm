package sc.fiji.llm.provider;

import org.scijava.plugin.SingletonService;

/**
 * SciJava service for discovering and managing LLM provider plugins.
 * This service is stateless and provides access to available LLM providers.
 */
public interface ProviderService extends SingletonService<LLMProvider> {

	/**
	 * Get the particular provider plugin for the given name.
	 *
	 * @param providerName the name of the desired provider
	 * @return the corresponding {@link LLMProvider}, or null if not found
	 */
	LLMProvider getProvider(String providerName);
}
