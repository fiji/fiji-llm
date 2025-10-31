package sc.fiji.llm.service;

import java.util.List;

import org.scijava.service.SciJavaService;

import sc.fiji.llm.provider.LLMProviderPlugin;

/**
 * SciJava service for managing LLM providers and creating chat models.
 * This service is stateless and acts as a factory for LLM instances.
 */
public interface LLMService extends SciJavaService {

	/**
	 * Get all available LLM provider plugins discovered in the SciJava context.
	 *
	 * @return list of available LLM providers
	 */
	List<LLMProviderPlugin> getAvailableProviders();

	/**
	 * Get the particular provider plugin for the given name
	 *
	 * @param providerName Desired provider
	 * @return Corresponding {@link LLMProviderPlugin}
	 */
	LLMProviderPlugin getProvider(final String providerName);

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given interface,
	 * chat model, and optional memory.
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant methods
	 * @param providerName Desired provider
	 * @param modelName Desired model for that provider
	 * @return an implementation of the assistant interface
	 */
	<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName);

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given interface,
	 * chat model, and tools.
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant methods
	 * @param providerName Desired provider
	 * @param modelName Desired model for that provider
	 * @param tools the tools (objects with @Tool annotated methods) to make available to the assistant
	 * @return an implementation of the assistant interface
	 */
	<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName, Object... tools);
}
