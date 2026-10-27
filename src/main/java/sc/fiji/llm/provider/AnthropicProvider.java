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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scijava.plugin.Plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
public class AnthropicProvider extends AbstractHostedProvider {

	public AnthropicProvider() {
		super(new HostedModel("claude-haiku-4-5-20251001", "Haiku", 1.0, 5.0),
			new HostedModel("claude-sonnet-5-5", "Sonnet", 2.0, 10.0),
			new HostedModel("claude-opus-5-5", "Opus", 4.0, 20.0));
	}

	@Override
	public String getName() {
		return "Claude";
	}

	@Override
	public String getDescription() {
		return "Claude models by Anthropic";
	}

	@Override
	public ChatRequestParameters defaultChatRequestParameters(
		final String modelName)
	{
		return ChatRequestParameters.builder().build();
	}

	@Override
	public String getModelsDocumentationUrl() {
		return "https://platform.claude.com/docs/en/models/overview";
	}

	@Override
	public JsonObject getModelsDocumentationFacts(final String html)
		throws IOException
	{
		final Document document = Jsoup.parse(html, getModelsDocumentationUrl());
		final Element modelTable = document.select("table").stream().filter(table -> {
			final String text = table.text();
			return text.contains("Pricing") && text.contains("Claude API ID") &&
				text.contains("Retirement");
		}).findFirst().orElseThrow(() -> new IOException(
			"Claude model comparison table not found"));

		final JsonObject modelTableFacts = new JsonObject();
		for (final Element row : modelTable.select("tr")) {
			final Elements cells = row.select("th, td");
			if (cells.isEmpty()) continue;
			final String label = cells.first().text();
			if (!"Pricing".equals(label) && !"Claude API ID".equals(label) &&
				!"Retirement".equals(label)) continue;

			final JsonArray values = new JsonArray();
			for (int i = 1; i < cells.size(); i++) values.add(cells.get(i).text());
			modelTableFacts.add(label, values);
		}
		if (modelTableFacts.size() != 3) throw new IOException(
			"Claude model comparison facts are incomplete");
		final JsonObject facts = new JsonObject();
		facts.add("model_table", modelTableFacts);
		return facts;
	}

	@Override
	public LocalDate getModelsDocumentationLastModified() {
		return LocalDate.of(2026, 10, 26);
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
