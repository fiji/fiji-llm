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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
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

/**
 * Abstract base class for Ollama providers with a curated set of model aliases.
 */
public abstract class AbstractCuratedModelProvider extends AbstractOllamaProvider {

	private final List<String> modelAliases;
	private final Map<String, String> modelNames;

	/**
	 * Creates a curated provider.
	 *
	 * @param modelAliases the aliases to present to users, in display order
	 * @param modelNames the mapping from aliases to Ollama model names
	 */
	protected AbstractCuratedModelProvider(final List<String> modelAliases,
		final Map<String, String> modelNames)
	{
		this.modelAliases = List.copyOf(modelAliases);
		this.modelNames = Map.copyOf(modelNames);
	}

	@Override
	public List<String> getAvailableModels() {
		final List<String> installedModels = getAvailableLocalModels();
		final List<String> models = modelAliases.stream().map(alias -> {
			final String modelName = modelNames.get(alias);
			return installedModels.contains(modelName) ? alias : appendRemoteString(
				alias);
		}).toList();
		return prioritizeInstalledModels(models);
	}

	/**
	 * Resolves an alias or remote alias to its Ollama model name.
	 *
	 * @param modelName an alias, model name, or remote alias
	 * @return the corresponding Ollama model name
	 */
	protected final String resolveModelName(final String modelName) {
		if (modelName == null) return null;
		final String alias = isRemoteModel(modelName) ? removeRemoteString(modelName) :
			modelName;
		return modelNames.getOrDefault(alias, alias);
	}

	/**
	 * Resolves a model name to the configured alias used by the user interface.
	 *
	 * @param modelName an alias, model name, or remote alias
	 * @return the corresponding alias, or the supplied name when it is unknown
	 */
	protected final String resolveModelAlias(final String modelName) {
		if (modelName == null) return null;
		final String resolvedModelName = resolveModelName(modelName);
		return modelNames.entrySet().stream().filter(entry -> entry.getValue().equals(
			resolvedModelName)).map(Map.Entry::getKey).findFirst().orElse(
			isRemoteModel(modelName) ? removeRemoteString(modelName) : modelName);
	}

	@Override
	public String validateModel(final String modelToValidate) {
		final boolean remote = modelToValidate != null && isRemoteModel(
			modelToValidate);
		final String modelName = resolveModelName(modelToValidate);
		return remote ? super.validateModel(appendRemoteString(modelName)) : modelName;
	}
}