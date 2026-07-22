/*-
 * #%L
 * Fiji software for LLM integration.
 * %%
 * Copyright (C) 2025 - 2026 ImageJ2 Developers
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
