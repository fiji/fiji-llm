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

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for Ollama providers with a single model.
 */
public abstract class AbstractSingletonOllamaProvider extends AbstractOllamaProvider {

	private final String modelName;

	protected AbstractSingletonOllamaProvider(String modelName) {
		this.modelName = modelName;
	}

	@Override
	public List<String> getAvailableModels() {
		List<String> localModels = getAvailableLocalModels();
		if (localModels.contains(modelName)) {
			return Collections.singletonList(modelName);
		}
		// Model not installed, mark as remote for download
		return Collections.singletonList(appendRemoteString(modelName));
	}
}