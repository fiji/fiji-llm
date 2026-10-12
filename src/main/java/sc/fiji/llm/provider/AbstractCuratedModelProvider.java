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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base class for Ollama providers with a curated set of model aliases.
 */
public abstract class AbstractCuratedModelProvider extends AbstractOllamaProvider {

	private final List<String> modelAliases;
	private final Map<String, CuratedModel> modelsByAlias;
	private final Map<String, CuratedModel> modelsByName;

	/**
	 * Immutable metadata for one curated Ollama model.
	 *
	 * @param model the raw Ollama model name
	 * @param alias the user-facing model alias
	 * @param downloadSize the expected download size in bytes, or {@code null}
	 *            when unknown
	 * @param baseGiB estimated memory demand before context storage
	 * @param ctxGiBPerToken estimated additional memory per context token
	 */
	protected record CuratedModel(String model, String alias, Long downloadSize,
		double baseGiB, double ctxGiBPerToken)
	{}

	/**
	 * Creates a curated provider.
	 *
	 * @param curatedModels the models to present to users, in display order
	 */
	protected AbstractCuratedModelProvider(final CuratedModel... curatedModels)
	{
		final List<CuratedModel> models = List.of(curatedModels);
		modelAliases = models.stream().map(CuratedModel::alias).toList();
		modelsByAlias = models.stream().collect(Collectors.toUnmodifiableMap(
			CuratedModel::alias, Function.identity()));
		modelsByName = models.stream().collect(Collectors.toUnmodifiableMap(
			CuratedModel::model, Function.identity()));
	}

	@Override
	public List<String> getAvailableModels() {
		final List<String> installedModels = getAvailableLocalModels();
		final List<String> models = modelAliases.stream().map(alias -> {
			final String modelName = modelsByAlias.get(alias).model();
			return installedModels.contains(modelName) ? alias : appendRemoteString(
				alias);
		}).toList();
		return prioritizeInstalledModels(models);
	}

	/**
	 * Gets the raw Ollama model names configured by this curated provider.
	 *
	 * @return the configured raw model names
	 */
	final Set<String> getRawModelNames() {
		return Set.copyOf(modelsByName.keySet());
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
		final CuratedModel curatedModel = modelsByAlias.get(alias);
		return curatedModel == null ? alias : curatedModel.model();
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
		final CuratedModel curatedModel = modelsByName.get(resolvedModelName);
		return curatedModel == null ? isRemoteModel(modelName) ? removeRemoteString(
			modelName) : modelName : curatedModel.alias();
	}

	@Override
	protected Optional<Long> getRemoteModelDownloadSize(final String modelName) {
		final CuratedModel curatedModel = modelsByName.get(modelName);
		return curatedModel == null ? Optional.empty() : Optional.ofNullable(
			curatedModel.downloadSize());
	}

	@Override
	public Optional<ModelDemand> getDemand(final String modelName) {
		if (modelName == null) return Optional.empty();
		final CuratedModel curatedModel = modelsByName.get(resolveModelName(modelName));
		return curatedModel == null ? Optional.empty() : Optional.of(new ModelDemand(
			curatedModel.baseGiB(), curatedModel.ctxGiBPerToken()));
	}

	@Override
	public Optional<ValidatedModel> validateModel(final String modelToValidate) {
		if (modelToValidate == null || modelToValidate.isBlank()) {
			return Optional.empty();
		}
		final boolean remote = isRemoteModel(modelToValidate);
		final String modelName = resolveModelName(modelToValidate);
		if (!remote) {
			return Optional.of(new ValidatedModel(resolveModelAlias(modelToValidate),
				modelName));
		}
		return super.validateModel(appendRemoteString(modelName)).map(validated ->
			new ValidatedModel(resolveModelAlias(modelToValidate), validated.modelName()));
	}
}