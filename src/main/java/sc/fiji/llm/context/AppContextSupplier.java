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
package sc.fiji.llm.context;

import org.scijava.plugin.SingletonPlugin;

import com.google.gson.JsonElement;

public interface AppContextSupplier extends SingletonPlugin {
	public static final String TYPE_KEY = "type";
	
	/**
	 * Note: all elements should start with a {@link #TYPE_KEY}
	 * property describing the context.
	 *
	 * @return A Json description of this slice of context.
	 */
	public JsonElement appConext();
}
