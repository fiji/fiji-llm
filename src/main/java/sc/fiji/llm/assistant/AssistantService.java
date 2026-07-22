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

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import net.imagej.ImageJService;

/**
 * SciJava service for creating LLM-powered assistants. This service creates
 * AiService instances (LangChain4j assistants) with specified providers,
 * models, and optional tools.
 */
public interface AssistantService extends ImageJService {

	/**
	 * Create an AI service instance (LangChain4j assistant) with the given
	 * interface, chat model, optional memory, request parameters, and all
	 * available tools.
	 *
	 * @param <T> the assistant interface type
	 * @param assistantInterface the interface class defining the assistant
	 *          methods
	 * @param providerName the name of the LLM provider
	 * @param modelName the name of the model within that provider
	 * @param chatMemory optional chat memory to persist conversation history
	 * @param defaultChatParameters optional request parameters (temperature,
	 *          top_p, etc.)
	 * @return an implementation of the assistant interface
	 * @throws IllegalArgumentException if the provider is not found
	 * @throws IllegalStateException if no API key is configured for the provider
	 */
	<T> T createAssistant(Class<T> assistantInterface, String providerName,
		String modelName, ChatMemory chatMemory,
		ChatRequestParameters defaultChatParameters);
}
