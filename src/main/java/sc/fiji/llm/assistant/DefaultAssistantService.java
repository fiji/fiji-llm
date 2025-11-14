package sc.fiji.llm.assistant;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest.Builder;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.service.AiServices;
import sc.fiji.llm.auth.APIKeyService;
import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;
import sc.fiji.llm.tools.AiToolService;

/**
 * Default implementation of AssistantService.
 */
@Plugin(type = Service.class)
public class DefaultAssistantService extends AbstractService implements AssistantService {

	@Parameter
	private ProviderService providerService;

	@Parameter
	private APIKeyService apiKeyService;

	@Parameter
	private AiToolService toolService;

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface, final String providerName, final String modelName, final ChatMemory chatMemory, final ChatRequestParameters defaultChatParameters) {
		final LLMProvider provider = providerService.getProvider(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Provider not found: " + providerName);
		}

		final String apiKey = apiKeyService.getApiKey(providerName);
		if (provider.requiresApiKey() && apiKey == null) {
			throw new IllegalStateException("No API key configured for provider: " + providerName);
		}

		final var builder = AiServices.builder(assistantInterface)
			.streamingChatModel(provider.createStreamingChatModel(apiKey, modelName))
			.chatModel(provider.createChatModel(apiKey, modelName))
			.tools(toolService.getInstances().toArray());

		// Apply request parameters at AiServices level where they'll be used
		if (defaultChatParameters != null) {
			builder.chatRequestTransformer(chatRequest -> {
				Builder chatTransformBuilder = chatRequest.toBuilder();
				chatTransformBuilder.parameters(defaultChatParameters.overrideWith(chatRequest.parameters()));
				return chatTransformBuilder.build();
			});
		}
		if (chatMemory != null) {
			builder.chatMemory(chatMemory);
		}

		return builder.build();
	}
}
