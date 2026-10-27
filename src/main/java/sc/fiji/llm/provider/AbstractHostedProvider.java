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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base class for hosted providers with a curated set of model aliases.
 */
public abstract class AbstractHostedProvider extends AbstractLLMProvider {

	private final List<String> modelAliases;
	private final Map<String, HostedModel> modelsByAlias;
	private final Map<String, HostedModel> modelsByName;

	/**
	 * Immutable metadata for one hosted model.
	 *
	 * @param model the raw provider model name
	 * @param alias the user-facing model alias
	 * @param cost the model's standard input and output token pricing
	 */
	protected record HostedModel(String model, String alias, ModelCost cost) {

		protected HostedModel(final String model, final String alias,
			final double inputPerMillionTokens, final double outputPerMillionTokens)
		{
			this(model, alias, new ModelCost(inputPerMillionTokens,
				outputPerMillionTokens));
		}
	}

	/**
	 * Creates a hosted provider.
	 *
	 * @param hostedModels the models to present to users, in display order
	 */
	protected AbstractHostedProvider(final HostedModel... hostedModels) {
		final List<HostedModel> models = List.of(hostedModels);
		modelAliases = models.stream().map(HostedModel::alias).toList();
		modelsByAlias = models.stream().collect(Collectors.toUnmodifiableMap(
			HostedModel::alias, Function.identity()));
		modelsByName = models.stream().collect(Collectors.toUnmodifiableMap(
			HostedModel::model, Function.identity()));
	}

	@Override
	public List<String> getAvailableModels() {
		return modelAliases;
	}

	@Override
	public VisionSupport getVisionSupport(final String modelName) {
		return modelName != null && modelsByName.containsKey(resolveModelName(
			modelName)) ? VisionSupport.SUPPORTED : VisionSupport.UNKNOWN;
	}

	@Override
	public Optional<ModelCost> getCost(final String modelName) {
		if (modelName == null) return Optional.empty();
		final HostedModel hostedModel = modelsByName.get(resolveModelName(modelName));
		return hostedModel == null ? Optional.empty() : Optional.of(hostedModel.cost());
	}

	/**
	 * Resolves a user-facing alias to its provider model name.
	 *
	 * @param modelName an alias or raw provider model name
	 * @return the corresponding raw provider model name
	 */
	protected final String resolveModelName(final String modelName) {
		if (modelName == null) return null;
		final HostedModel hostedModel = modelsByAlias.get(modelName);
		return hostedModel == null ? modelName : hostedModel.model();
	}
}
