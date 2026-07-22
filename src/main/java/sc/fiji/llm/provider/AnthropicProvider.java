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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.scijava.plugin.Plugin;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

/**
 * LLM provider plugin for Anthropic (Claude).
 */
@Plugin(type = LLMProvider.class, name = "Claude")
public class AnthropicProvider extends AbstractLLMProvider {

	private Map<String, AnthropicChatModelName> models = null;
	private List<String> modelList;

	@Override
	public String getName() {
		return "Claude";
	}

	@Override
	public String getDescription() {
		return "Claude models by Anthropic";
	}

	@Override
	public ChatRequestParameters defaultChatRequestParameters() {
		return ChatRequestParameters.builder().temperature(0.1).build();
	}

	@Override
	public List<String> getAvailableModels() {
		if (models == null) initModelMap();
		return modelList;
	}

	private synchronized void initModelMap() {
		if (models == null) {
			Map<String, AnthropicChatModelName> tmpModels = new HashMap<>();
			List<String> modelNames = new ArrayList<>();
			Stream.of(AnthropicChatModelName.values()).forEach(n -> {
				String s = sanitize(n);
				tmpModels.put(s, n);
				modelNames.add(s);
			});
			modelList = Collections.unmodifiableList(modelNames);
			models = tmpModels;
		}
	}

	private String sanitize(AnthropicChatModelName name) {
		String n = name.toString();
		// Remove the date stamp
		n = n.substring(0, n.lastIndexOf('-'));
		// Replace #-# with #.#
		n = n.replaceAll("(\\d)-(\\d)", "$1.$2");
		// Replace remaining '-' with spaces
		n = n.replace('-', ' ');
		return n;
	}

	private AnthropicChatModelName getModel(final String sanitized) {
		if (models == null) {
			initModelMap();
		}
		return models.get(sanitized);
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
		return null;
	}

	@Override
	public ChatModel createChatModel(final String modelName) {
		return AnthropicChatModel.builder().apiKey(apiKey()).modelName(getModel(
			modelName)).maxRetries(DEFAULT_MAX_RETRIES).timeout(DEFAULT_TIMEOUT)
			.build();
	}

	@Override
	public StreamingChatModel createStreamingChatModel(final String modelName) {
		return AnthropicStreamingChatModel.builder().apiKey(apiKey()).modelName(
			getModel(modelName)).timeout(DEFAULT_TIMEOUT).build();
	}
}
