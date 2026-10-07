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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
@Plugin(type = LLMProvider.class, name = "Gemini", priority = ProviderPriority.GEMINI)
public class GeminiProvider extends AbstractLLMProvider {

	private static final int MAX_INPUT_TOKENS = 1_048_576;
	private static final int MEMORY_CONTEXT_PERCENTAGE = 75;
	private static final List<String> AVAILABLE_MODELS = List.of("gemini-3.5-flash-lite",
		"gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3.6-flash", "gemini-3.7-flash",
		"gemini-3.8-flash");

	private static final Set<String> VISION_MODELS = new HashSet<>(AVAILABLE_MODELS);

	@Override
	public String getName() {
		return "Gemini";
	}

	@Override
	public String getDescription() {
		return "Gemini models by Google";
	}

	@Override
	public VisionSupport getVisionSupport(final String modelName) {
		return modelName != null && VISION_MODELS.contains(modelName) ?
			VisionSupport.SUPPORTED : VisionSupport.UNKNOWN;
	}

	@Override
	public List<String> getAvailableModels() {
		// Google AI doesn't provide a public API endpoint to list models
		// Fall back to hard-coded list, ordered by most to least permissive
		// Taken from https://aistudio.google.com/docs/models
		return AVAILABLE_MODELS;
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://ai.google.dev/gemini-api/docs/models";
	}

	@Override
	public String getModelsDocumentationContentHash(final String html)
		throws IOException
	{
		final Document document = Jsoup.parse(html, getModelsDocumentationUrl());
		final Set<String> facts = new TreeSet<>();
		String lifecycle = "unknown";
		for (final Element element : document.select("h2, h3, h4, table")) {
			if (!"table".equals(element.tagName())) {
				lifecycle = lifecycleFromHeading(element.text(), lifecycle);
				continue;
			}
			for (final Element row : element.select("tr")) {
				final Elements cells = row.select("th, td");
				if (cells.size() < 2) continue;
				final String modelId = cells.get(1).text().trim();
				if (modelId.isBlank() || !modelId.contains("-")) continue;
				final String rowLifecycle = cells.first().text().toLowerCase(Locale.ROOT)
					.contains("shut down") ? "shut-down" : lifecycle;
				facts.add(rowLifecycle + "|" + modelId);
			}
		}
		if (facts.isEmpty()) throw new IOException("Gemini model facts not found");
		return LLMProvider.hashNormalizedDocumentationText(String.join("\n", facts));
	}

	private static String lifecycleFromHeading(final String heading,
		final String current)
	{
		final String normalized = heading.toLowerCase(Locale.ROOT);
		if (normalized.contains("stable")) return "stable";
		if (normalized.contains("preview")) return "preview";
		if (normalized.contains("experimental")) return "experimental";
		if (normalized.contains("previous") || normalized.contains("deprecated"))
			return "previous";
		return current;
	}

	@Override
	public boolean shouldCheckModelsDocumentationContent(final String lastModified) {
		if (lastModified == null || lastModified.isBlank()) return true;
		try {
			final LocalDate remoteDate = ZonedDateTime.parse(lastModified,
				DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate();
			return remoteDate.isAfter(getModelsDocumentationLastModified());
		}
		catch (final DateTimeParseException e) {
			return true;
		}
	}

	@Override
	public boolean shouldReviewModelsDocumentationLastModified() {
		return false;
	}

	@Override
	public LocalDate getModelsDocumentationLastModified() {
		return LocalDate.of(2026, 10, 7);
	}

	@Override
	public String getApiKeyUrl() {
		return "https://aistudio.google.com/app/apikey";
	}

	@Override
	public Optional<String> getRecommendedModel() {
		return Optional.of("gemini-3.5-flash-lite");
	}

	@Override
	public TokenWindowChatMemory createTokenChatMemory(String modelName) {
		return TokenWindowChatMemory.withMaxTokens(getMemoryTokenLimit(),
			GoogleAiGeminiTokenCountEstimator.builder().apiKey(apiKey()).modelName(
				modelName).build());
	}

	static int getMemoryTokenLimit() {
		return MAX_INPUT_TOKENS * MEMORY_CONTEXT_PERCENTAGE / 100;
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return GoogleAiGeminiChatModel.builder().apiKey(apiKey()).modelName(
			modelName).timeout(DEFAULT_TIMEOUT).maxRetries(DEFAULT_MAX_RETRIES)
			.returnThinking(true).sendThinking(true)
			.listeners(listeners()).build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return GoogleAiGeminiStreamingChatModel.builder().apiKey(apiKey())
			.modelName(modelName).timeout(DEFAULT_TIMEOUT).returnThinking(true)
			.sendThinking(true).listeners(listeners()).build();
	}
}
