package sc.fiji.llm.assistant;

import org.scijava.service.SciJavaService;

import sc.fiji.llm.tools.AiToolService;

/**
 * SciJava service for creating LLM-powered assistants.
 * This service creates AiService instances (LangChain4j assistants) with
 * specified providers, models, and optional tools.
 */
public interface AssistantService extends SciJavaService {

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given interface
	 * and chat model, using all available tools in {@link AiToolService}
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant methods
	 * @param providerName the name of the LLM provider
	 * @param modelName the name of the model within that provider
	 * @return an implementation of the assistant interface
	 * @throws IllegalArgumentException if the provider is not found
	 * @throws IllegalStateException if no API key is configured for the provider
	 */
	<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName);
}
