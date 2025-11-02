package sc.fiji.llm.provider;

import java.util.List;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default implementation of ProviderService.
 */
@Plugin(type = Service.class)
public class DefaultProviderService extends AbstractService implements ProviderService {

	@Parameter
	private PluginService pluginService;

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
}
