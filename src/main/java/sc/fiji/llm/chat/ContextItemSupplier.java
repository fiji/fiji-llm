/*-
 * #%L
 * A Fiji plugin for integrating large language models.
 * %%
 * Copyright (C) 2025 ImageJ Developers
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
