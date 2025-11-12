package sc.fiji.llm.provider;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

/**
 * Default implementation of ProviderService.
 */
@Plugin(type = Service.class)
public class DefaultProviderService extends AbstractSingletonService<LLMProvider> implements ProviderService {

	@Override
	public LLMProvider getProvider(final String providerName) {
		return getInstances().stream()
			.filter(p -> p.getName().equals(providerName))
			.findFirst()
			.orElse(null);
	}

    @Override
    public Class<LLMProvider> getPluginType() {
		return LLMProvider.class;
    }

	@Override
	public void initialize() {
		getInstances().stream().forEach(LLMProvider::initialize);
	}

	@Override
	public void dispose() {
		getInstances().stream().forEach(LLMProvider::dispose);
	}
}
