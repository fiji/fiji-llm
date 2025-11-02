package sc.fiji.llm.provider;

import java.util.List;

import org.scijava.service.SciJavaService;

/**
 * SciJava service for discovering and managing LLM provider plugins.
 * This service is stateless and provides access to available LLM providers.
 */
public interface ProviderService extends SciJavaService {

	/**
	 * Get all available LLM provider plugins discovered in the SciJava context.
	 *
	 * @return list of available LLM providers
	 */
	List<LLMProviderPlugin> getAvailableProviders();

	/**
	 * Get the particular provider plugin for the given name.
	 *
	 * @param providerName the name of the desired provider
	 * @return the corresponding {@link LLMProviderPlugin}, or null if not found
	 */
	LLMProviderPlugin getProvider(String providerName);
}
