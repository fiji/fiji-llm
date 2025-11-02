package sc.fiji.llm.assistant;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import dev.langchain4j.service.AiServices;
import sc.fiji.llm.auth.APIKeyService;
import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;

/**
 * Default implementation of AssistantService.
 */
@Plugin(type = Service.class)
public class DefaultAssistantService extends AbstractService implements AssistantService {

	@Parameter
	private ProviderService providerService;

	@Parameter
	private APIKeyService apiKeyService;

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface, final String providerName, final String modelName) {
		return createAssistant(assistantInterface, providerName, modelName, new Object[0]);
	}

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface, final String providerName, final String modelName, final Object... tools) {
		final LLMProvider provider = providerService.getProvider(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Provider not found: " + providerName);
		}

		final String apiKey = apiKeyService.getApiKey(providerName);
		if (apiKey == null) {
			throw new IllegalStateException("No API key configured for provider: " + providerName);
		}

		return AiServices.builder(assistantInterface)
			.streamingChatModel(provider.createStreamingChatModel(apiKey, modelName))
			.chatModel(provider.createChatModel(apiKey, modelName))
			.tools(tools)
			.build();
	}
}
