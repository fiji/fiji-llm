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

import org.scijava.plugin.Plugin;

/**
 * LLM provider plugin for Ollama qwen3-coder:30b model.
 * Specialized provider for large-code-generation tasks.
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Qwen3-Coder)")
public class Qwen3CoderProvider extends AbstractSingletonOllamaProvider {

	private static final String MODEL_NAME = "qwen3-coder:30b";

	public Qwen3CoderProvider() {
		super(MODEL_NAME);
	}

	@Override
	public String getName() {
		return "Ollama (Qwen3-Coder)";
	}

	@Override
	public String getDescription() {
		return "Local Qwen3-Coder - a sensible first-try for general use.";
	}
}