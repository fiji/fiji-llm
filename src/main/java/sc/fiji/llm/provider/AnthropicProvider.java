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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.scijava.plugin.Plugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicTokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * LLM provider plugin for Anthropic (Claude).
 */
@Plugin(type = LLMProvider.class, name = "Claude")
public class AnthropicProvider extends AbstractLLMProvider {

	@Override
	public String getName() {
		return "Claude";
	}

	@Override
	public String getDescription() {
		return "Claude models by Anthropic";
	}

	@Override
	public List<String> getAvailableModels() {
		// Use the models from langchain4j's AnthropicChatModelName enum
		// Filter to show only the main/latest models to avoid overwhelming users
		return Stream.of(AnthropicChatModelName.values()).map(
			AnthropicChatModelName::toString).collect(Collectors.toList());
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://docs.anthropic.com/en/docs/about-claude/models";
	}

	@Override
	public String getApiKeyUrl() {
		return "https://console.anthropic.com/settings/keys";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(8000,
			AnthropicTokenCountEstimator.builder().apiKey(apiKey()).modelName(
				modelName).build());
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return AnthropicChatModel.builder().apiKey(apiKey()).modelName(modelName)
			.maxRetries(DEFAULT_MAX_RETRIES).timeout(DEFAULT_TIMEOUT).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return AnthropicStreamingChatModel.builder().apiKey(apiKey()).modelName(
			modelName).timeout(DEFAULT_TIMEOUT).build();
	}
}
