/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 Fiji developers.
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
package sc.fiji.llm.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.scijava.plugin.Plugin;

/**
 * LLM provider plugin for the locally hosted Gemma4 Ollama models.
 * See: https://huggingface.co/collections/google/gemma-4-69c2f5b8d6a5f
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Gemma4)", priority = ProviderPriority.GEMMA4)
public class Gemma4Provider extends AbstractOllamaProvider {

	private static final String XS_MODEL =
		"hf.co/google/gemma-4-E4B-it-qat-q4_0-gguf:latest";
	private static final String S_MODEL =
		"hf.co/google/gemma-4-12B-it-qat-q4_0-gguf:latest";
	private static final String M_MODEL =
		"hf.co/google/gemma-4-26B-A4B-it-qat-q4_0-gguf:latest";
	private static final String L_MODEL =
		"hf.co/google/gemma-4-31B-it-qat-q4_0-gguf:latest";

	private static final List<String> MODEL_ALIASES = List.of("XS", "S", "M", "L");
	private static final Map<String, String> MODEL_NAMES = Map.of(
		"XS", XS_MODEL,
		"S", S_MODEL,
		"M", M_MODEL,
		"L", L_MODEL);

	@Override
	public String getName() {
		return "Gemma4 (Ollama)";
	}

	@Override
	public boolean isCurated() {
		return true;
	}

	@Override
	public String getDescription() {
		return "Local Gemma4 models with selectable parameter counts and memory footprints.";
	}

	@Override
	public List<String> getAvailableModels() {
		final List<String> installedModels = getAvailableLocalModels();
		return MODEL_ALIASES.stream().map(alias -> {
			final String modelName = MODEL_NAMES.get(alias);
			return installedModels.contains(modelName) ? alias : appendRemoteString(alias);
		}).toList();
	}

	@Override
	public String validateModel(String modelToValidate) {
		final boolean remote = isRemoteModel(modelToValidate);
		final String alias = remote ? removeRemoteString(modelToValidate) :
			modelToValidate;
		final String modelName = MODEL_NAMES.getOrDefault(alias, alias);
		return remote ? super.validateModel(appendRemoteString(modelName)) : modelName;
	}

	@Override
	public Optional<String> getRecommendedModel() {
		return Optional.of("S");
	}

	@Override
	protected int getContextSize() {
		return 32 * 1024;
	}

	@Override
	protected int getContextSize(String modelName) {
		if (XS_MODEL.equals(modelName)) return 24 * 1024;
		if (L_MODEL.equals(modelName)) return 64 * 1024;
		return getContextSize();
	}
}
