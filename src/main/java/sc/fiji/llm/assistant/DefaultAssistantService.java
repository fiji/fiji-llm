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

package sc.fiji.llm.assistant;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest.Builder;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.service.AiServices;
import sc.fiji.llm.mcp.MCPService;
import sc.fiji.llm.provider.LLMProvider;
import sc.fiji.llm.provider.ProviderService;
import sc.fiji.llm.tools.AiToolService;

/**
 * Default implementation of AssistantService.
 */
@Plugin(type = Service.class)
public class DefaultAssistantService extends AbstractService implements
	AssistantService
{

	@Parameter
	private ProviderService providerService;

	@Parameter
	private MCPService mcpService;

	@Parameter
	private AiToolService aiToolService;

	@Override
	public <T> T createAssistant(final Class<T> assistantInterface,
		final String providerName, final String modelName,
		final ChatMemory chatMemory,
		final ChatRequestParameters defaultChatParameters)
	{
		final LLMProvider provider = providerService.getProvider(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Provider not found: " + providerName);
		}

		final var builder = AiServices.builder(assistantInterface)
			.streamingChatModel(provider.createStreamingChatModel(modelName))
			.toolProvider(mcpService.getToolProvider())
			.toolExecutionErrorHandler(aiToolService::handleExecutionError)
			.toolArgumentsErrorHandler(aiToolService::handleArgumentError)
			.chatModel(provider.createChatModel(modelName));

		// Apply request parameters at AiServices level where they'll be used
		if (defaultChatParameters != null) {
			builder.chatRequestTransformer(chatRequest -> {
				Builder chatTransformBuilder = chatRequest.toBuilder();
				chatTransformBuilder.parameters(defaultChatParameters.overrideWith(
					chatRequest.parameters()));
				return chatTransformBuilder.build();
			});
		}
		if (chatMemory != null) {
			builder.chatMemory(chatMemory);
		}

		return builder.build();
	}
}
