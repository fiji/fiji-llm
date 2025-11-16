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

import java.util.Arrays;
import java.util.List;

import org.scijava.plugin.Plugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;

/**
 * LLM provider plugin for Google AI (Gemini).
 */
@Plugin(type = LLMProvider.class, name = "Gemini")
public class GeminiProvider extends AbstractLLMProvider {

	@Override
	public String getName() {
		return "Gemini";
	}

	@Override
	public String getDescription() {
		return "Gemini models by Google";
	}

	@Override
	public List<String> getAvailableModels() {
		// Google AI doesn't provide a public API endpoint to list models
		// Fall back to hard-coded list
		return Arrays.asList("gemini-2.5-pro", "gemini-2.5-flash",
			"gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-2.0-flash-lite");
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ai.google.dev/gemini-api/docs/models";
	}

	@Override
	public String getApiKeyUrl() {
		return "https://aistudio.google.com/app/apikey";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(8000,
			GoogleAiGeminiTokenCountEstimator.builder().apiKey(apiKey()).modelName(
				modelName).build());
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return GoogleAiGeminiChatModel.builder().apiKey(apiKey()).modelName(
			modelName).timeout(DEFAULT_TIMEOUT).maxRetries(DEFAULT_MAX_RETRIES)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return GoogleAiGeminiStreamingChatModel.builder().apiKey(apiKey())
			.modelName(modelName).timeout(DEFAULT_TIMEOUT).build();
	}
}
