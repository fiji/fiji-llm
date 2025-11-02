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
	public List<LLMProvider> getAvailableProviders() {
		return pluginService.createInstancesOfType(LLMProvider.class);
	}

	@Override
	public LLMProvider getProvider(final String providerName) {
		return getAvailableProviders().stream()
			.filter(p -> p.getName().equals(providerName))
			.findFirst()
			.orElse(null);
	}
}
