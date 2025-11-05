package sc.fiji.llm.chat;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

/**
 * Default implementation of ContextItemSupplierService.
 * Provides registry and lookup functionality for ContextItemSupplier plugins.
 */
@Plugin(type = Service.class)
public class DefaultContextItemService extends AbstractSingletonService<ContextItemSupplier>
		implements ContextItemService {

	@Override
	public Class<ContextItemSupplier> getPluginType() {
		return ContextItemSupplier.class;
	}
}
