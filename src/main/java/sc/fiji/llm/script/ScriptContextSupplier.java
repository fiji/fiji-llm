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

package sc.fiji.llm.script;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;


import sc.fiji.llm.context.ContextItem;
import sc.fiji.llm.ui.ContextItemSupplier;

/**
 * ContextItemSupplier implementation for script context items. Provides
 * available scripts from open TextEditor instances and creates
 * ScriptContextItem objects.
 */
@Plugin(type = ContextItemSupplier.class, priority = Priority.EXTREMELY_HIGH)
public class ScriptContextSupplier implements ContextItemSupplier {

	@Override
	public String getDisplayName() {
		return "Script";
	}

	@Override
	public ImageIcon getIcon() {
		final URL iconUrl = getClass().getResource("/icons/petition-noun-32.png");
		if (iconUrl != null) {
			return new ImageIcon(iconUrl);
		}
		return null;
	}

	@Override
	public Set<ContextItem> listAvailable() {
		final Set<ContextItem> items = new LinkedHashSet<>();
		items.addAll(ScriptContextUtilities.getAvailableScripts());
		return items;
	}

	@Override
	public ContextItem createActiveContextItem() {
		return ScriptContextUtilities.getActiveScriptContext();
	}

}
