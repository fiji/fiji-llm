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

import org.scijava.plugin.Plugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

/**
 * LLM provider plugin for OpenAI (ChatGPT).
 */
@Plugin(type = LLMProvider.class, name = "ChatGPT", priority = ProviderPriority.OPENAI)
public class OpenAIProvider extends AbstractLLMProvider {

	private static final List<String> MODEL_ALIASES = List.of("Luna 5.6", "Luna 6",
		"Sol 6");
	private static final Map<String, String> MODEL_NAMES = Map.of("Luna 5.6",
		"gpt-5.6-luna", "Luna 6", "gpt-6-luna", "Sol 6", "gpt-6.1-sol");

	@Override
	public String getName() {
		return "ChatGPT";
	}

	@Override
	public String getDescription() {
		return "ChatGPT models by OpenAI";
	}

	@Override
	public ChatRequestParameters defaultChatRequestParameters() {
		return OpenAiChatRequestParameters.builder().frequencyPenalty(0.0)
			.presencePenalty(0.0).temperature(0.1).reasoningEffort("none").build();
	}

	@Override
	public VisionSupport getVisionSupport(final String modelName) {
		return MODEL_NAMES.containsValue(resolveModelName(modelName)) ?
			VisionSupport.SUPPORTED : VisionSupport.UNKNOWN;
	}

	@Override
	public List<String> getAvailableModels() {
		return MODEL_ALIASES;
	}

	private String resolveModelName(final String modelName) {
		return modelName == null ? null : MODEL_NAMES.getOrDefault(modelName, modelName);
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://developers.openai.com/api/docs/models";
	}

	@Override
	public String getModelsDocumentationContentSelector() {
		return "main";
	}

	@Override
	public LocalDate getModelsDocumentationLastModified() {
		return LocalDate.of(2026, 9, 30);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://platform.openai.com/api-keys";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(8000,
			new OpenAiTokenCountEstimator(resolveModelName(modelName)));
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return OpenAiChatModel.builder().apiKey(apiKey()).modelName(resolveModelName(
			modelName))
			.defaultRequestParameters(defaultChatRequestParameters())
			.maxRetries(DEFAULT_MAX_RETRIES).timeout(DEFAULT_TIMEOUT)
			.listeners(listeners()).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return OpenAiStreamingChatModel.builder().apiKey(apiKey()).modelName(
			resolveModelName(modelName)).defaultRequestParameters(
				defaultChatRequestParameters()).timeout(DEFAULT_TIMEOUT).listeners(
					listeners()).build();
	}
}
