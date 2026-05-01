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

import org.scijava.plugin.Plugin;

/**
 * LLM provider plugin for Ollama qwen2.5-coder:7b model.
 * Specialized provider for code generation and analysis tasks.
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Qwen2.5-Coder:7B)")
public class Qwen25CoderProvider extends AbstractOllamaProvider {

	private static final String MODEL_NAME = "qwen2.5-coder:7b";

	@Override
	public String getName() {
		return "Ollama (Qwen2.5-Coder:7B)";
	}

	@Override
	public String getDescription() {
		return "Qwen2.5-Coder 7B model optimized for code generation and analysis";
	}

	@Override
	public List<String> getAvailableModels() {
		List<String> localModels = getAvailableLocalModels();
		if (localModels.contains(MODEL_NAME)) {
			return Collections.singletonList(MODEL_NAME);
		}
		// Model not installed, mark as remote for download
		return Collections.singletonList(appendRemoteString(MODEL_NAME));
	}
}
