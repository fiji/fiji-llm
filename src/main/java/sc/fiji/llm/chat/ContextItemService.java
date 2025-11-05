package sc.fiji.llm.chat;

import org.scijava.plugin.SingletonService;

/**
 * SciJava service for discovering and managing context item supplier plugins.
 * This service is a registry for all available ContextItemSupplier implementations
 * and provides lookup methods to discover suppliers.
 */
public interface ContextItemService extends SingletonService<ContextItemSupplier> {
}
