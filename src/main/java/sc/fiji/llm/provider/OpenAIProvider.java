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

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.scijava.plugin.Plugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiResponsesStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

/**
 * LLM provider plugin for OpenAI (ChatGPT).
 */
@Plugin(type = LLMProvider.class, name = "ChatGPT", priority = ProviderPriority.OPENAI)
public class OpenAIProvider extends AbstractLLMProvider {

	private static final int MAX_INPUT_TOKENS = 922_000;
	private static final int MEMORY_CONTEXT_PERCENTAGE = 75;
	private static final List<String> MODEL_ALIASES = List.of("Luna 5.6", "Luna 6",
		"Sol 6");
	private static final Map<String, String> MODEL_NAMES = Map.of("Luna 5.6",
		"gpt-5.6-luna", "Luna 6", "gpt-6-luna", "Sol 6", "gpt-6.1-sol");
	private static final Map<String, ModelCost> MODEL_COSTS = Map.of(
		"gpt-5.6-luna", new ModelCost(0.20, 1.20),
		"gpt-6-luna", new ModelCost(0.10, 0.50),
		"gpt-6.1-sol", new ModelCost(2.00, 10.00));

	@Override
	public String getName() {
		return "ChatGPT";
	}

	@Override
	public String getDescription() {
		return "ChatGPT models by OpenAI";
	}

	@Override
	public ChatRequestParameters defaultChatRequestParameters(final String modelName)
	{
		final String reasoningEffort = "gpt-6.1-sol".equals(resolveModelName(
			modelName)) ? "medium" : "none";
		return OpenAiResponsesChatRequestParameters.builder()
			.reasoningEffort(reasoningEffort).build();
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
		return "https://developers.openai.com/api/docs/models";
	}

	@Override
	public String getModelsDocumentationContentHash(final String html)
		throws IOException
	{
		final Document document = Jsoup.parse(html, getModelsDocumentationUrl());
		final List<String> facts = new ArrayList<>();
		for (final Element modelIdLabel : document.select("div").stream().filter(
			element -> "Model ID".equals(element.ownText().trim())).toList())
		{
			final String modelId = fieldValue(modelIdLabel);
			if (modelId.isBlank()) continue;
			final Element pricingScope = findPricingScope(modelIdLabel);
			final String inputPrice = pricingScope == null ? "" : fieldValue(
				pricingScope, "Input price");
			final String outputPrice = pricingScope == null ? "" : fieldValue(
				pricingScope, "Output price");
			final String scopeText = pricingScope == null ? modelIdLabel.parent()
				.parent().text() : pricingScope.text();
			final String lifecycle = scopeText.toLowerCase(Locale.ROOT).contains("deprecated") ?
				"deprecated" : "active";
			facts.add(modelId + "|input=" + inputPrice + "|output=" + outputPrice +
				"|lifecycle=" + lifecycle);
		}
		if (facts.isEmpty()) throw new IOException("OpenAI model facts not found");
		facts.sort(String::compareTo);
		return LLMProvider.hashNormalizedDocumentationText(String.join("\n",
			facts));
	}

	private static Element findPricingScope(final Element modelIdLabel) {
		Element fallback = null;
		for (final Element ancestor : modelIdLabel.parents()) {
			if (!hasField(ancestor, "Input price") || !hasField(ancestor,
				"Output price")) continue;
			if (fallback == null) fallback = ancestor;
			if (ancestor.select("a[href*='/api/docs/models/']").size() == 1) return ancestor;
		}
		return fallback;
	}

	private static boolean hasField(final Element scope, final String label) {
		return scope.select("div").stream().anyMatch(element -> label.equals(
			element.ownText().trim()));
	}

	private static String fieldValue(final Element scope, final String label) {
		for (final Element field : scope.select("div")) {
			if (label.equals(field.ownText().trim())) return fieldValue(field);
		}
		return "";
	}

	private static String fieldValue(final Element field) {
		final String label = field.ownText().trim();
		final String rowText = field.parent().text().trim();
		return rowText.startsWith(label) ? rowText.substring(label.length()).trim() :
			"";
	}

	@Override
	public LocalDate getModelsDocumentationLastModified() {
		return LocalDate.of(2026, 10, 06);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://platform.openai.com/api-keys";
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(getMemoryTokenLimit(),
			new OpenAiTokenCountEstimator(resolveModelName(modelName)));
	}

	static int getMemoryTokenLimit() {
		return MAX_INPUT_TOKENS * MEMORY_CONTEXT_PERCENTAGE / 100;
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return OpenAiResponsesChatModel.builder().apiKey(apiKey()).modelName(resolveModelName(
			modelName))
			.defaultRequestParameters(defaultChatRequestParameters(modelName))
			.listeners(listeners()).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return OpenAiResponsesStreamingChatModel.builder().apiKey(apiKey()).modelName(
			resolveModelName(modelName)).defaultRequestParameters(
				defaultChatRequestParameters(modelName)).listeners(
					listeners()).build();
	}
}
