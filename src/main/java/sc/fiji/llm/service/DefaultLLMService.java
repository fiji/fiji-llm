package sc.fiji.llm.service;

import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import sc.fiji.llm.provider.LLMProviderPlugin;

/**
 * Default implementation of LLMService.
 */
@Plugin(type = Service.class)
public class DefaultLLMService extends AbstractService implements LLMService {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private APIKeyService apiKeyService;

	@Override
	public List<LLMProviderPlugin> getAvailableProviders() {
		return pluginService.createInstancesOfType(LLMProviderPlugin.class);
	}

	@Override
	public LLMProviderPlugin getProvider(final String providerName) {
		return getAvailableProviders().stream()
			.filter(p -> p.getName().equals(providerName))
			.findFirst()
			.orElse(null);
	}

	@Override
	public ChatModel createChatModel(final String providerName, final String modelName) {
		final LLMProviderPlugin provider = getProvider(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Provider not found: " + providerName);
		}

		final String apiKey = apiKeyService.getApiKey(providerName);
		if (apiKey == null) {
			throw new IllegalStateException("No API key configured for provider: " + providerName);
		}

		return provider.createChatModel(apiKey, modelName);
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String providerName, final String modelName) {
		final LLMProviderPlugin provider = getProvider(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Provider not found: " + providerName);
		}

		final String apiKey = apiKeyService.getApiKey(providerName);
		if (apiKey == null) {
			throw new IllegalStateException("No API key configured for provider: " + providerName);
		}

		return provider.createStreamingChatModel(apiKey, modelName);
	}

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface, final ChatModel model) {
		return AiServices.builder(assistantInterface)
			.chatModel(model)
			.build();
	}
}
