package sc.fiji.llm.service;

import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

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
	public <T> T createAssistant(final Class<T> assistantInterface, final String providerName, final String modelName) {
			return createAssistant(assistantInterface, providerName, modelName, new Object[0]);
	}

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface, final String providerName, final String modelName, final Object... tools) {
		final LLMProviderPlugin provider = getProvider(providerName);
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
