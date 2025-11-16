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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

/**
 * LLM provider plugin for OpenAI (ChatGPT).
 */
@Plugin(type = LLMProvider.class, name = "ChatGPT")
public class OpenAIProvider extends AbstractLLMProvider {

	@Override
	public String getName() {
		return "ChatGPT";
	}

	@Override
	public String getDescription() {
		return "ChatGPT models by OpenAI";
	}

	@Override
	public List<String> getAvailableModels() {
		// Use the models from langchain4j's OpenAiChatModelName enum
		// Filter to show only the main/latest models to avoid overwhelming users
		return Stream.of(OpenAiChatModelName.values()).map(
			OpenAiChatModelName::toString).filter(this::isMainChatModel).collect(
				Collectors.toList());
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://platform.openai.com/docs/models/";
	}

	/**
	 * Check if a model ID represents a main chat model we want to show. Filters
	 * out preview models and dated versions, keeping only the main model names.
	 */
	private boolean isMainChatModel(final String modelId) {
		// Exclude preview models
		if (modelId.contains("preview")) {
			return false;
		}
		// Exclude dated versions (models with dates like YYYY-MM-DD or YYYY_MM_DD)
		return !modelId.matches(".*\\d{4}[_-]\\d{2}[_-]\\d{2}.*");
	}

	@Override
	public String getApiKeyUrl() {
		return "https://platform.openai.com/api-keys";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(8000,
			new OpenAiTokenCountEstimator(modelName));
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return OpenAiChatModel.builder().apiKey(apiKey()).modelName(modelName)
			.maxRetries(DEFAULT_MAX_RETRIES).timeout(DEFAULT_TIMEOUT).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return OpenAiStreamingChatModel.builder().apiKey(apiKey()).modelName(
			modelName).timeout(DEFAULT_TIMEOUT).build();
	}
}
