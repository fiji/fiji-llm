/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.llm.ui;

import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.SingletonPlugin;

import sc.fiji.llm.context.ContextItem;

/**
 * Plugin interface for supplying context items to the chat UI. Implementations
 * provide metadata about available context items and create them on demand.
 */
public interface ContextItemSupplier extends SingletonPlugin {

	/**
	 * Gets a human-readable display name for this supplier. Used in UI labels and
	 * menus.
	 *
	 * @return the display name
	 */
	String getDisplayName();

	/**
	 * Gets an icon for this supplier. Used to visually represent the supplier in
	 * the UI.
	 *
	 * @return the icon, or null if no icon is available
	 */
	ImageIcon getIcon();

	/**
	 * Lists all available context items that can be supplied. For example, a
	 * script supplier would list all open scripts. This method is called
	 * frequently to populate UI elements, so it should be efficient.
	 *
	 * @return a set of available context items, or empty set if none available
	 */
	Set<ContextItem> listAvailable();

	/**
	 * Creates an active context item from this supplier. For example, a script
	 * supplier would create a context item for the currently active script.
	 *
	 * @return the created ContextItem, or null if one could not be created
	 */
	ContextItem createActiveContextItem();
}
