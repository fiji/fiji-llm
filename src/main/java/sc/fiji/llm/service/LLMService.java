package sc.fiji.llm.service;

import java.util.List;

import org.scijava.service.SciJavaService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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
	 * Find a specific LLM provider by name.
	 *
	 * @param providerName the name of the provider (e.g., "OpenAI", "Anthropic")
	 * @return the provider plugin, or null if not found
	 */
	LLMProviderPlugin getProvider(String providerName);

	/**
	 * Create a chat language model for the specified provider and model.
	 *
	 * @param providerName the name of the provider
	 * @param modelName the name of the model (e.g., "gpt-4o", "claude-3-5-sonnet")
	 * @return a configured chat language model
	 */
	ChatModel createChatModel(String providerName, String modelName);

	/**
	 * Create a streaming chat language model for the specified provider and model.
	 *
	 * @param providerName the name of the provider
	 * @param modelName the name of the model
	 * @return a configured streaming chat language model
	 */
	StreamingChatModel createStreamingChatModel(String providerName, String modelName);

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given interface,
	 * chat model, and optional memory.
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant methods
	 * @param model the chat language model to use
	 * @return an implementation of the assistant interface
	 */
	<T> T createAssistant(Class<T> assistantInterface, ChatModel model);
}
