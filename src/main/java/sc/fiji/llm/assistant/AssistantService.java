package sc.fiji.llm.assistant;

import org.scijava.service.SciJavaService;

import dev.langchain4j.memory.ChatMemory;

/**
 * SciJava service for creating LLM-powered assistants.
 * This service creates AiService instances (LangChain4j assistants) with
 * specified providers, models, and optional tools.
 */
public interface AssistantService extends SciJavaService {

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given interface,
	 * chat model, optional memory, and all available tools.
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant methods
	 * @param providerName the name of the LLM provider
	 * @param modelName the name of the model within that provider
	 * @param chatMemory optional chat memory to persist conversation history
	 * @return an implementation of the assistant interface
	 * @throws IllegalArgumentException if the provider is not found
	 * @throws IllegalStateException if no API key is configured for the provider
	 */
	<T> T createAssistant(Class<T> assistantInterface, String providerName, String modelName, ChatMemory chatMemory);
}
