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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.scijava.plugin.Plugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

/**
 * LLM provider plugin for Anthropic (Claude).
 */
@Plugin(type = LLMProvider.class, name = "Claude", priority = ProviderPriority.ANTHROPIC)
public class AnthropicProvider extends AbstractLLMProvider {

	private static final List<String> MODEL_ALIASES = List.of("Haiku", "Sonnet", "Opus");
	private static final Map<String, String> MODEL_NAMES = Map.of(
		"Fable", "claude-fable-5-1",
		"Opus", "claude-opus-5-5",
		"Sonnet", "claude-sonnet-5-5",
		"Haiku", "claude-haiku-4-5-20251001");
	private static final Map<String, ModelCost> MODEL_COSTS = Map.of(
		"claude-fable-5-1", new ModelCost(10.0, 50.0),
		"claude-opus-5-5", new ModelCost(4.0, 20.0),
		"claude-sonnet-5-5", new ModelCost(2.0, 10.0),
		"claude-haiku-4-5-20251001", new ModelCost(1.0, 5.0));

	@Override
	public String getName() {
		return "Claude";
	}

	@Override
	public String getDescription() {
		return "Claude models by Anthropic";
	}

	@Override
	public VisionSupport getVisionSupport(final String modelName) {
		return MODEL_NAMES.containsValue(resolveModelName(modelName)) ?
			VisionSupport.SUPPORTED : VisionSupport.UNKNOWN;
	}

	@Override
	public ChatRequestParameters defaultChatRequestParameters(
		final String modelName)
	{
		return ChatRequestParameters.builder().build();
	}

	@Override
	public List<String> getAvailableModels() {
		return MODEL_ALIASES;
	}

	@Override
	public Optional<ModelCost> getCost(final String modelName) {
		if (modelName == null) return Optional.empty();
		return Optional.ofNullable(MODEL_COSTS.get(resolveModelName(modelName)));
	}

	private String resolveModelName(final String modelName) {
		return modelName == null ? null : MODEL_NAMES.getOrDefault(modelName, modelName);
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://platform.claude.com/docs/en/models/overview";
	}

	@Override
	public String getModelsDocumentationContentSelector() {
		return "article#content-container";
	}

	@Override
	public LocalDate getModelsDocumentationLastModified() {
		return LocalDate.of(2026, 9, 30);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://platform.claude.com/settings/keys";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return null;
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return AnthropicChatModel.builder().apiKey(apiKey()).modelName(
			resolveModelName(modelName)).maxRetries(DEFAULT_MAX_RETRIES).timeout(DEFAULT_TIMEOUT)
			.listeners(listeners()).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return AnthropicStreamingChatModel.builder().apiKey(apiKey()).modelName(
			resolveModelName(modelName)).timeout(DEFAULT_TIMEOUT)
			.listeners(listeners()).build();
	}
}
