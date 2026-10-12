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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Generalist LLM provider plugin for Ollama with user-selectable models.
 * Allows users to choose from available installed models and download remote
 * models from the Ollama library.
 */
@Plugin(type = LLMProvider.class, name = "Ollama (Custom)", priority = ProviderPriority.OLLAMA)
public class OllamaProvider extends AbstractOllamaProvider {

	static final String ENTER_NEW_MODEL = "<Enter new model>";

	@Parameter
	private CommandService commandService;

	@Override
	public String getName() {
		return "Ollama (Custom)";
	}

	@Override
	public String getDescription() {
		return "Explore any Ollama model by name.";
	}

	@Override
	protected int getContextSize() {
		return 32 * 1024;
	}

	@Override
	public List<String> getAvailableModels() {
		final ProviderService providerService = getContext() == null ? null : getContext()
			.getService(ProviderService.class);
		final Set<String> curatedModelNames = providerService == null ? Set.of() :
			providerService.getInstances().stream().filter(
				AbstractCuratedModelProvider.class::isInstance).map(
					AbstractCuratedModelProvider.class::cast).flatMap(provider -> provider
						.getRawModelNames().stream()).collect(Collectors.toUnmodifiableSet());
		return filterAvailableModels(getAvailableLocalModels(), curatedModelNames);
	}

	static List<String> filterAvailableModels(final List<String> localModels,
		final Set<String> curatedModelNames)
	{
		final List<String> models = new ArrayList<>();
		localModels.stream().filter(model -> !curatedModelNames.contains(model)).forEach(
			models::add);
		models.add(ENTER_NEW_MODEL);
		return List.copyOf(models);
	}

	@Override
	public Optional<ValidatedModel> validateModel(final String modelToValidate) {
		if (!ENTER_NEW_MODEL.equals(modelToValidate)) {
			return super.validateModel(modelToValidate);
		}

		final String modelName = promptForModelName();
		if (modelName == null) return Optional.empty();
		return super.validateModel(appendRemoteString(modelName));
	}

	private String promptForModelName() {
		if (commandService == null) return null;
		try {
			final CommandModule module = commandService.run(OllamaModelInputCommand.class, true)
				.get();
			final Object value = module == null ? null : module.getInput("model");
			if (!(value instanceof String)) return null;
			final String modelName = ((String) value).trim();
			return modelName.isEmpty() ? null : modelName;
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		catch (final ExecutionException | CancellationException e) {
			return null;
		}
	}
}
