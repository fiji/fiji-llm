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

package sc.fiji.llm.provider;

import org.scijava.plugin.SingletonService;

import net.imagej.ImageJService;

/**
 * SciJava service for discovering and managing LLM provider plugins. This
 * service is stateless and provides access to available LLM providers.
 */
public interface ProviderService extends SingletonService<LLMProvider>, ImageJService {

	/**
	 * Get the particular provider plugin for the given name.
	 *
	 * @param providerName the name of the desired provider
	 * @return the corresponding {@link LLMProvider}, or null if not found
	 */
	LLMProvider getProvider(String providerName);
}
