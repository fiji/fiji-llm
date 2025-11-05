package sc.fiji.llm.chat;

import java.util.List;

import org.scijava.plugin.SingletonPlugin;

/**
 * Plugin interface for supplying context items to the chat UI.
 * Implementations provide metadata about available context items and create them on demand.
 */
public interface ContextItemSupplier extends SingletonPlugin {
	/**
	 * Gets a human-readable display name for this supplier.
	 * Used in UI labels and menus.
	 *
	 * @return the display name
	 */
	String getDisplayName();

	/**
	 * Lists all available context items that can be supplied.
	 * For example, a script supplier would list all open scripts.
	 * This method is called frequently to populate UI elements, so it should be efficient.
	 *
	 * @return a list of available context items, or empty list if none available
	 */
	List<ContextItem> listAvailable();

	/**
	 * Creates an active context item from this supplier.
	 * For example, a script supplier would create a context item for the currently active script.
	 *
	 * @return the created ContextItem, or null if one could not be created
	 */
	ContextItem createActiveContextItem();
}
