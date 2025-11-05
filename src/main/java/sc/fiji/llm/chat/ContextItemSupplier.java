package sc.fiji.llm.chat;

import java.util.Set;

import javax.swing.ImageIcon;

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
	 * Gets an icon for this supplier.
	 * Used to visually represent the supplier in the UI.
	 *
	 * @return the icon, or null if no icon is available
	 */
	ImageIcon getIcon();

	/**
	 * Lists all available context items that can be supplied.
	 * For example, a script supplier would list all open scripts.
	 * This method is called frequently to populate UI elements, so it should be efficient.
	 *
	 * @return a set of available context items, or empty set if none available
	 */
	Set<ContextItem> listAvailable();

	/**
	 * Creates an active context item from this supplier.
	 * For example, a script supplier would create a context item for the currently active script.
	 *
	 * @return the created ContextItem, or null if one could not be created
	 */
	ContextItem createActiveContextItem();
}
